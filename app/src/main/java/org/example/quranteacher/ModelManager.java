package org.example.quranteacher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Installe les modèles Vosk de reconnaissance vocale hors ligne.
 *
 * Rien n'est livré dans l'APK : les modèles pèsent 39 à 99 Mo. Ils sont
 * téléchargés une seule fois puis stockés sur la carte mémoire.
 */
public final class ModelManager {

    public interface Listener {
        void onProgress(int percent);
        void onFinished(boolean ok, String path, String error);
    }

    public static final class ModelInfo {
        public final String tag;
        public final String url;
        public final long sizeMb;

        ModelInfo(String tag, String url, long sizeMb) {
            this.tag = tag;
            this.url = url;
            this.sizeMb = sizeMb;
        }
    }

    public static final ModelInfo FR = new ModelInfo("fr",
            "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.22.zip", 40);
    public static final ModelInfo AR = new ModelInfo("ar",
            "https://alphacephei.com/vosk/models/vosk-model-small-ar-0.3.zip", 99);
    public static final ModelInfo EN = new ModelInfo("en",
            "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip", 39);

    public static final ModelInfo[] MODELS = {FR, AR, EN};

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ModelManager() {
    }

    /** Associe la langue de reconnaissance choisie au modèle correspondant. */
    public static ModelInfo infoFor(String recogLang) {
        String tag = tagOf(recogLang);
        for (ModelInfo info : MODELS) {
            if (info.tag.equals(tag)) return info;
        }
        return FR;
    }

    public static String tagOf(String recogLang) {
        if (recogLang == null) return "fr";
        if (recogLang.startsWith("ar")) return "ar";
        if (recogLang.startsWith("en")) return "en";
        return "fr";
    }

    public static File modelsRoot(Context context) {
        File card = AudioInstaller.sdRoot(context);
        File base = card != null ? new File(card, "QuranTeacher") : context.getExternalFilesDir(null);
        if (base == null) base = context.getFilesDir();
        File dir = new File(base, "models");
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        return dir;
    }

    public static File modelDir(Context context, String tag) {
        return new File(modelsRoot(context), tag);
    }

    public static boolean isInstalled(Context context, String tag) {
        File dir = modelDir(context, tag);
        return new File(dir, "am/final.mdl").exists();
    }

    public static void delete(Context context, String tag) {
        deleteRecursive(modelDir(context, tag));
    }

    public static void download(final Context context, final String tag, final Listener listener) {
        final Context app = context.getApplicationContext();
        Thread worker = new Thread(() -> {
            ModelInfo info = infoFor(tag);
            File zip = new File(app.getCacheDir(), "model-" + tag + ".zip");
            File target = modelDir(app, tag);
            String error = null;
            try {
                fetch(info, zip, listener);
                unzip(zip, target, listener);
                if (!new File(target, "am/final.mdl").exists()) {
                    throw new IllegalStateException("modèle incomplet");
                }
            } catch (Exception e) {
                error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            } finally {
                //noinspection ResultOfMethodCallIgnored
                zip.delete();
            }
            final boolean ok = error == null;
            final String path = target.getAbsolutePath();
            final String message = error;
            MAIN.post(() -> {
                if (listener != null) listener.onFinished(ok, path, message);
            });
        }, "modele-vosk");
        worker.start();
    }

    private static void fetch(ModelInfo info, File zip, Listener listener) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(info.url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            connection.connect();
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("serveur " + code);
            }
            long total = connection.getContentLengthLong();
            try (InputStream in = new BufferedInputStream(connection.getInputStream());
                 OutputStream out = new FileOutputStream(zip)) {
                byte[] buffer = new byte[65536];
                long done = 0L;
                int last = -1;
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                    done += read;
                    if (total > 0 && listener != null) {
                        int percent = (int) (done * 50 / total);
                        if (percent != last) {
                            last = percent;
                            notifyProgress(listener, percent);
                        }
                    }
                }
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static void unzip(File zip, File target, Listener listener) throws Exception {
        deleteRecursive(target);
        if (!target.mkdirs()) {
            throw new IllegalStateException("dossier non créé");
        }
        long total = zip.length();
        long done = 0L;
        try (ZipInputStream in = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)))) {
            ZipEntry entry = in.getNextEntry();
            String prefix = "";
            if (entry != null && entry.isDirectory()) {
                prefix = entry.getName();
            }
            int last = -1;
            while (entry != null) {
                String name = entry.getName();
                if (!prefix.isEmpty() && name.startsWith(prefix)) {
                    name = name.substring(prefix.length());
                }
                if (!name.isEmpty()) {
                    File out = new File(target, name);
                    if (entry.isDirectory()) {
                        //noinspection ResultOfMethodCallIgnored
                        out.mkdirs();
                    } else {
                        File parent = out.getParentFile();
                        if (parent != null) {
                            //noinspection ResultOfMethodCallIgnored
                            parent.mkdirs();
                        }
                        try (OutputStream os = new FileOutputStream(out)) {
                            byte[] buffer = new byte[65536];
                            int read;
                            while ((read = in.read(buffer)) > 0) {
                                os.write(buffer, 0, read);
                                done += read;
                            }
                        }
                    }
                }
                in.closeEntry();
                entry = in.getNextEntry();
                if (listener != null && total > 0) {
                    int percent = 50 + (int) (Math.min(done, total) * 50 / total);
                    if (percent != last) {
                        last = percent;
                        notifyProgress(listener, percent);
                    }
                }
            }
        }
    }

    private static void notifyProgress(Listener listener, int percent) {
        MAIN.post(() -> listener.onProgress(percent));
    }

    static void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) deleteRecursive(child);
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
