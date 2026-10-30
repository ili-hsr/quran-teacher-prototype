package org.example.quranteacher;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.DocumentsContract;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Index des récitations présentes sur le téléphone.
 *
 * Deux façons de nommer, reconnues en même temps :
 *   1. à plat          : 002_255.mp3
 *   2. par récitateur  : Alafasy/002_255.mp3
 *
 * La seconde est celle qui permettra le site : chacun télécharge le dossier de
 * son récitateur préféré, l'application le détecte et propose le choix. Un
 * fichier à la racine reste toujours accepté comme solution de repli.
 *
 * Aucun verset n'est jamais prononcé par la synthèse vocale.
 */
public final class RecitationStore {

    private static final String KEY_TREE = "audio_tree_uri";
    private static final String KEY_RECITER = "reciter";
    private static final String[] EXTENSIONS = {"mp3", "ogg", "m4a", "aac", "wav", "opus"};
    private static final String SEP = "|";
    private static final long CACHE_MS = 20000L;

    private final Context context;
    private final SharedPreferences prefs;
    private Map<String, Uri> cache;
    private long cacheAt;

    public RecitationStore(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = AppPrefs.get(context);
    }

    // --- Dossiers ----------------------------------------------------------

    public String treeUri() {
        return prefs.getString(KEY_TREE, "");
    }

    public void setTreeUri(String uri) {
        prefs.edit().putString(KEY_TREE, uri == null ? "" : uri).commit();
        invalidate();
    }

    public File defaultFolder() {
        File dir = context.getExternalFilesDir(null);
        return dir == null ? new File(context.getFilesDir(), "quran") : new File(dir, "quran");
    }

    // --- Récitateur actif --------------------------------------------------

    public String activeReciter() {
        return prefs.getString(KEY_RECITER, "");
    }

    public void setActiveReciter(String reciter) {
        prefs.edit().putString(KEY_RECITER, reciter == null ? "" : reciter).commit();
    }

    /** Noms des récitateurs trouvés ("" = fichiers à la racine). */
    public List<String> reciters() {
        Set<String> names = new LinkedHashSet<>();
        for (String composite : index().keySet()) {
            names.add(composite.substring(0, composite.indexOf(SEP)));
        }
        List<String> out = new ArrayList<>(names);
        Collections.sort(out);
        return out;
    }

    // --- Index -------------------------------------------------------------

    public Map<String, Uri> index() {
        long now = SystemClock.elapsedRealtime();
        if (cache != null && now - cacheAt < CACHE_MS) {
            return cache;
        }
        Map<String, Uri> found = new HashMap<>();
        scanTree(found);
        File installed = AudioInstaller.targetFolder(context);
        if (installed != null) {
            scanFolder(installed, "", found, 0);
        }
        scanFolder(defaultFolder(), "", found, 0);
        cache = found;
        cacheAt = now;
        return found;
    }

    public void invalidate() {
        cache = null;
        cacheAt = 0L;
    }

    public Uri uriFor(QuranData.Surah surah, int verseIndex) {
        if (surah == null) {
            return null;
        }
        Map<String, Uri> all = index();
        String[] names = {
                surah.key + "_" + (verseIndex + 1),
                surah.alias + "_" + (verseIndex + 1)
        };
        String active = activeReciter();
        for (String name : names) {
            Uri uri = all.get(active + SEP + name);
            if (uri != null) {
                return uri;
            }
        }
        for (String name : names) {
            Uri uri = all.get(SEP + name);
            if (uri != null) {
                return uri;
            }
        }
        for (String name : names) {
            for (Map.Entry<String, Uri> entry : all.entrySet()) {
                if (entry.getKey().endsWith(SEP + name)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    /** Nombre de versets disponibles pour le récitateur actif. */
    public int availableCount() {
        Set<String> names = new HashSet<>();
        String active = activeReciter();
        for (String composite : index().keySet()) {
            int cut = composite.indexOf(SEP);
            String reciter = composite.substring(0, cut);
            if (reciter.isEmpty() || reciter.equals(active)) {
                names.add(composite.substring(cut + 1));
            }
        }
        return names.size();
    }

    // --- Analyse du disque -------------------------------------------------

    private void scanFolder(File dir, String reciter, Map<String, Uri> out, int depth) {
        File[] files = dir == null ? null : dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                // Un sous-dossier = un récitateur (un seul niveau).
                if (depth == 0) {
                    scanFolder(file, file.getName(), out, 1);
                }
                continue;
            }
            String name = baseName(file.getName());
            if (name == null) {
                continue;
            }
            String composite = reciter + SEP + name;
            if (!out.containsKey(composite)) {
                out.put(composite, Uri.fromFile(file));
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
            scanTreeChildren(treeUri, rootId, "", out, 0);
        } catch (Exception ignored) {
            // Dossier inaccessible : carte retirée ou permission expirée.
        }
    }

    private void scanTreeChildren(Uri treeUri, String parentId, String reciter,
                                  Map<String, Uri> out, int depth) {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId);
        Cursor cursor = context.getContentResolver().query(
                children,
                new String[]{
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                },
                null, null, null);
        if (cursor == null) {
            return;
        }
        try {
            while (cursor.moveToNext()) {
                String docId = cursor.getString(0);
                String displayName = cursor.getString(1);
                String mime = cursor.getString(2);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    if (depth == 0) {
                        scanTreeChildren(treeUri, docId, displayName, out, 1);
                    }
                    continue;
                }
                String name = baseName(displayName);
                if (name == null) {
                    continue;
                }
                String composite = reciter + SEP + name;
                if (!out.containsKey(composite)) {
                    out.put(composite, DocumentsContract.buildDocumentUriUsingTree(treeUri, docId));
                }
            }
        } finally {
            cursor.close();
        }
    }

    /** « 001_5.MP3 » -> « 001_5 » ; null si ce n'est pas un fichier audio. */
    static String baseName(String fileName) {
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
