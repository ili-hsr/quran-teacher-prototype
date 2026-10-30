package org.example.quranteacher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Transfert des récitations vers la carte mémoire, avec vérification.
 *
 * Principe demandé par Iliass : les versets sont livrés avec l'application, ils
 * partent sur la carte mémoire à la première ouverture, on VÉRIFIE que la copie
 * est intacte (empreinte SHA-256 du fichier écrit comparée à l'empreinte de la
 * source), et seulement ensuite l'application ne garde plus rien.
 *
 * Précision honnête : un APK installé ne peut pas se vider lui-même. La source
 * reste dans l'APK (lecture seule), mais l'application ne conserve AUCUNE copie
 * dans sa mémoire interne : les octets vont directement de l'APK vers la carte,
 * et un fichier dont l'empreinte ne correspond pas est supprimé et signalé.
 */
public final class AudioInstaller {

    public interface Listener {
        void onFinished(Result result);
    }

    public static final class Result {
        public final boolean ok;
        public final int prepared;
        public final int verified;
        public final String path;
        public final String error;

        Result(boolean ok, int prepared, int verified, String path, String error) {
            this.ok = ok;
            this.prepared = prepared;
            this.verified = verified;
            this.path = path == null ? "" : path;
            this.error = error == null ? "" : error;
        }
    }

    private static final String ASSET_DIR = "recitations";
    private static final String PREFS = "quran_audio_install";
    private static final String K_INSTALLED = "installed_v1";
    private static final String K_PATH = "path";
    private static final String K_COUNT = "count";
    private static final String TARGET_FOLDER = "QuranTeacher";

    private AudioInstaller() {
    }

    // --- État --------------------------------------------------------------

    public static boolean isInstalled(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(K_INSTALLED, false);
    }

    public static int installedCount(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(K_COUNT, 0);
    }

    public static String installedPath(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(K_PATH, "");
    }

    /** Titres livrés dans l'APK. */
    public static List<String> bundleTracks(Context context) {
        try {
            String[] names = context.getAssets().list(ASSET_DIR);
            List<String> tracks = new ArrayList<>();
            if (names != null) {
                for (String name : names) {
                    String lower = name.toLowerCase(java.util.Locale.ROOT);
                    if (lower.endsWith(".mp3") || lower.endsWith(".ogg") || lower.endsWith(".m4a")) {
                        tracks.add(name);
                    }
                }
            }
            return tracks;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static boolean hasBundle(Context context) {
        return !bundleTracks(context).isEmpty();
    }

    // --- Cible -------------------------------------------------------------

    /** Racine de la carte mémoire amovible, ou null si absente. */
    public static File sdCard(Context context) {
        File[] dirs = context.getExternalFilesDirs(null);
        if (dirs == null || dirs.length < 2 || dirs[1] == null) {
            return null;
        }
        File card = dirs[1];
        for (int i = 0; i < 4 && card != null; i++) {
            card = card.getParentFile();
        }
        return card;
    }

    /** Dossier de destination : la carte si possible, sinon la mémoire de l'app. */
    public static File targetFolder(Context context) {
        File card = sdCard(context);
        if (card != null) {
            File dir = new File(card, TARGET_FOLDER + "/recitations");
            if (dir.exists() || dir.mkdirs()) {
                if (dir.canWrite()) {
                    return dir;
                }
            }
        }
        File base = context.getExternalFilesDir(null);
        if (base == null) {
            base = context.getFilesDir();
        }
        File dir = new File(base, "quran");
        if (dir.exists() || dir.mkdirs()) {
            return dir;
        }
        return null;
    }

    // --- Installation ------------------------------------------------------

    public static void install(final Context context, final Listener listener) {
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            final Result result = run(app);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (listener != null) {
                    listener.onFinished(result);
                }
            });
        }, "audio-install").start();
    }

    private static Result run(Context context) {
        List<String> tracks = bundleTracks(context);
        if (tracks.isEmpty()) {
            return new Result(false, 0, 0, "", "aucune piste livrée");
        }
        File target = targetFolder(context);
        if (target == null) {
            return new Result(false, tracks.size(), 0, "", "destination indisponible");
        }

        int verified = 0;
        for (String name : tracks) {
            File out = new File(target, name);
            String sourceHash = copyAndHash(context, name, out);
            if (sourceHash == null) {
                continue;
            }
            String writtenHash = hash(out);
            if (sourceHash.equals(writtenHash)) {
                verified++;
            } else {
                // Copie douteuse : on la retire plutôt que de laisser un fichier faux.
                out.delete();
            }
        }

        boolean ok = verified == tracks.size();
        if (ok) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putBoolean(K_INSTALLED, true)
                    .putString(K_PATH, target.getAbsolutePath())
                    .putInt(K_COUNT, verified)
                    .commit();
        }
        return new Result(ok, tracks.size(), verified, target.getAbsolutePath(),
                ok ? null : "vérification incomplète");
    }

    /** Copie un asset vers la carte et renvoie l'empreinte de la source. */
    private static String copyAndHash(Context context, String assetName, File out) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            return null;
        }
        File tmp = new File(out.getParentFile(), assetName + ".part");
        try (InputStream in = context.getAssets().open(ASSET_DIR + "/" + assetName);
             FileOutputStream os = new FileOutputStream(tmp)) {
            byte[] buffer = new byte[16384];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
                os.write(buffer, 0, read);
            }
            os.flush();
            os.getFD().sync();
        } catch (Exception e) {
            tmp.delete();
            return null;
        }
        if (!tmp.renameTo(out)) {
            tmp.delete();
            return null;
        }
        return toHex(digest.digest());
    }

    private static String hash(File file) {
        if (file == null || !file.isFile()) {
            return "";
        }
        try (InputStream in = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[16384];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            return toHex(digest.digest());
        } catch (Exception e) {
            return "";
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
