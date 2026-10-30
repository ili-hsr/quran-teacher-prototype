package org.example.quranteacher;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Index des récitations audio disponibles sur le téléphone.
 *
 * Aucun verset n'est jamais prononcé par la synthèse vocale : le son vient
 * uniquement de fichiers posés par la famille, sur la carte SD ou dans un
 * dossier choisi une seule fois.
 *
 * Convention de nommage : <sourate>_<numéro>.<ext>
 *   fatiha_1.mp3, fatiha_2.mp3 ... fatiha_7.mp3
 * Extensions reconnues : mp3, ogg, m4a, aac, wav, opus.
 */
public final class RecitationStore {

    private static final String KEY_TREE = "audio_tree_uri";
    private static final String[] EXTENSIONS = {"mp3", "ogg", "m4a", "aac", "wav", "opus"};

    private final Context context;
    private final SharedPreferences prefs;

    public RecitationStore(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = AppPrefs.get(context);
    }

    public boolean hasFolder() {
        return !treeUri().isEmpty();
    }

    public String treeUri() {
        return prefs.getString(KEY_TREE, "");
    }

    public void setTreeUri(String uri) {
        prefs.edit().putString(KEY_TREE, uri == null ? "" : uri).commit();
    }

    public void clearFolder() {
        prefs.edit().remove(KEY_TREE).commit();
    }

    /** Dossier par défaut, utilisable sans permission : Android/data/.../files/quran */
    public File defaultFolder() {
        File dir = context.getExternalFilesDir(null);
        return dir == null ? new File(context.getFilesDir(), "quran") : new File(dir, "quran");
    }

    /** Clé attendue pour un verset, ex. « fatiha_3 ». */
    public static String key(String surahKey, int verseIndex) {
        return surahKey + "_" + (verseIndex + 1);
    }

    /** @return tous les versets audio trouvés, indexés par clé. */
    public Map<String, Uri> index() {
        Map<String, Uri> found = new HashMap<>();
        scanTree(found);
        scanDefaultFolder(found);
        return found;
    }

    public Uri uriFor(String surahKey, int verseIndex) {
        return index().get(key(surahKey, verseIndex));
    }

    public int availableCount() {
        return index().size();
    }

    private void scanDefaultFolder(Map<String, Uri> out) {
        File dir = defaultFolder();
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String name = baseName(file.getName());
            if (name != null && !out.containsKey(name)) {
                out.put(name, Uri.fromFile(file));
            }
        }
    }

    private void scanTree(Map<String, Uri> out) {
        String tree = treeUri();
        if (tree.isEmpty()) {
            return;
        }
        Uri treeUri = Uri.parse(tree);
        try {
            String rootId = DocumentsContract.getTreeDocumentId(treeUri);
            Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootId);
            Cursor cursor = context.getContentResolver().query(
                    children,
                    new String[]{
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    },
                    null, null, null);
            if (cursor == null) {
                return;
            }
            try {
                while (cursor.moveToNext()) {
                    String docId = cursor.getString(0);
                    String displayName = cursor.getString(1);
                    String name = baseName(displayName);
                    if (name == null || out.containsKey(name)) {
                        continue;
                    }
                    out.put(name,
                            DocumentsContract.buildDocumentUriUsingTree(treeUri, docId));
                }
            } finally {
                cursor.close();
            }
        } catch (Exception ignored) {
            // Dossier inaccessible (carte retirée, permission expirée) : on ignore.
        }
    }

    /** « Fatiha_3.MP3 » -> « fatiha_3 » ; null si l'extension n'est pas audio. */
    private static String baseName(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return null;
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        boolean known = false;
        for (String candidate : EXTENSIONS) {
            if (candidate.equals(ext)) {
                known = true;
                break;
            }
        }
        if (!known) {
            return null;
        }
        return fileName.substring(0, dot).toLowerCase(Locale.ROOT).replace(' ', '_');
    }
}
