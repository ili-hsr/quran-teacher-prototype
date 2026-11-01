package org.example.quranteacher;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

import java.io.File;

/** Moteur de reconnaissance hors ligne (Vosk). */
public final class VoskAsr implements RecognitionListener {

    public interface Callback {
        void onText(String text);
        void onFailed(String message);
    }

    public interface Stream {
        void onPartial(String text);
        void onFinal(String text);
        void onFailed(String message);
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private Model model;
    private Recognizer recognizer;
    private SpeechService service;
    private Callback callback;
    private Stream stream;
    private boolean running;
    private String last = "";

    public boolean isBusy() {
        return running;
    }

    public void listen(final Context context, final String tag, final Callback cb) {
        this.callback = cb;
        this.stream = null;
        startEngine(context, tag);
    }

    public void listenStream(final Context context, final String tag, final Stream st) {
        this.stream = st;
        this.callback = null;
        startEngine(context, tag);
    }

    private void startEngine(final Context context, final String tag) {
        if (running) {
            return;
        }
        running = true;
        last = "";
        if (service != null) {
            main.post(this::beginListening);
            return;
        }
        new Thread(() -> {
            try {
                File dir = ModelManager.modelDir(context, tag);
                model = new Model(dir.getAbsolutePath());
                recognizer = new Recognizer(model, 16000.0f);
                service = new SpeechService(recognizer, 16000.0f);
                main.post(this::beginListening);
            } catch (Throwable t) {
                running = false;
                main.post(() -> fail(String.valueOf(t.getMessage())));
            }
        }, "vosk-init").start();
    }

    private void beginListening() {
        try {
            if (service != null) {
                service.startListening(this);
            }
        } catch (Throwable t) {
            fail(String.valueOf(t.getMessage()));
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        String partial = field(hypothesis, "partial");
        if (partial == null || partial.isEmpty()) {
            return;
        }
        last = partial;
        Stream s = stream;
        if (s != null) {
            s.onPartial(partial);
        }
    }

    @Override
    public void onResult(String hypothesis) {
        String text = field(hypothesis, "text");
        if (text != null && !text.isEmpty()) {
            last = text;
        }
    }

    @Override
    public void onFinalResult(String hypothesis) {
        String text = field(hypothesis, "text");
        if (text == null || text.isEmpty()) {
            text = last;
        }
        final String out = text == null ? "" : text;
        Stream s = stream;
        if (s != null) {
            s.onFinal(out);
            last = "";
            if (running) {
                main.post(this::resume);
            }
            return;
        }
        running = false;
        stopService();
        main.post(() -> {
            Callback cb = callback;
            if (cb != null) {
                cb.onText(out);
            }
        });
    }

    @Override
    public void onError(Exception e) {
        fail(e == null ? "erreur" : String.valueOf(e.getMessage()));
    }

    @Override
    public void onTimeout() {
        onFinalResult(null);
    }

    private void resume() {
        try {
            if (service != null && running) {
                service.startListening(this);
            }
        } catch (Throwable ignored) {
        }
    }

    private void fail(String message) {
        running = false;
        String msg = message == null ? "" : message;
        stopService();
        Stream s = stream;
        Callback cb = callback;
        main.post(() -> {
            if (s != null) {
                s.onFailed(msg);
            } else if (cb != null) {
                cb.onFailed(msg);
            }
        });
    }

    public void stop() {
        running = false;
        stopService();
    }

    private void stopService() {
        try {
            if (service != null) {
                service.stop();
            }
        } catch (Throwable ignored) {
        }
    }

    public void shutdown() {
        running = false;
        stopService();
        try {
            if (service != null) {
                service.shutdown();
            }
        } catch (Throwable ignored) {
        }
        service = null;
        try {
            if (recognizer != null) {
                recognizer.close();
            }
        } catch (Throwable ignored) {
        }
        recognizer = null;
        try {
            if (model != null) {
                model.close();
            }
        } catch (Throwable ignored) {
        }
        model = null;
    }

    private static String field(String json, String name) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JSONObject object = new JSONObject(json);
            if (object.has(name)) {
                return object.getString(name).trim();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
