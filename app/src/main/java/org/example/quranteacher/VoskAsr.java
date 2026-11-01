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

/**
 * Reconnaissance vocale locale (Vosk), sans Internet et sans service Google.
 *
 * Le moteur est chargé à la première utilisation puis relâché quand on quitte
 * l'écran : sur un téléphone de 2 Go, garder le modèle en mémoire n'est pas
 * raisonnable.
 */
public final class VoskAsr implements RecognitionListener {

    public interface Callback {
        void onText(String text);
        void onFailed(String message);
    }

    private final Handler main = new Handler(Looper.getMainLooper());
    private Model model;
    private Recognizer recognizer;
    private SpeechService service;
    private Callback callback;
    private String last = "";
    private boolean loading = false;

    public boolean isBusy() {
        return loading;
    }

    public void listen(final Context context, final String tag, Callback cb) {
        callback = cb;
        if (loading) return;
        loading = true;
        last = "";
        if (service != null) {
            service.startListening(this);
            return;
        }
        new Thread(() -> {
            try {
                File dir = ModelManager.modelDir(context, tag);
                model = new Model(dir.getAbsolutePath());
                recognizer = new Recognizer(model, 16000.0f);
                service = new SpeechService(recognizer, 16000.0f);
                main.post(() -> {
                    try {
                        service.startListening(VoskAsr.this);
                    } catch (Exception e) {
                        loading = false;
                        fail("micro indisponible");
                    }
                });
            } catch (Throwable t) {
                loading = false;
                main.post(() -> fail(String.valueOf(t.getMessage())));
            }
        }, "vosk-init").start();
    }

    private void fail(String message) {
        if (callback != null) callback.onFailed(message);
    }

    private void deliver(String text) {
        loading = false;
        if (service != null) {
            try {
                service.stop();
            } catch (Exception ignored) {
            }
        }
        if (callback != null) callback.onText(text == null ? "" : text);
    }

    private static String field(String json, String name) {
        if (json == null) return null;
        try {
            JSONObject object = new JSONObject(json);
            return object.optString(name, null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        String partial = field(hypothesis, "partial");
        if (partial != null) last = partial;
    }

    @Override
    public void onResult(String hypothesis) {
        String text = field(hypothesis, "text");
        if (text != null && !text.isEmpty()) last = text;
    }

    @Override
    public void onFinalResult(String hypothesis) {
        String text = field(hypothesis, "text");
        if (text == null || text.isEmpty()) text = last;
        String out = text;
        main.post(() -> deliver(out));
    }

    @Override
    public void onError(Exception e) {
        main.post(() -> {
            loading = false;
            fail(e == null ? "erreur" : String.valueOf(e.getMessage()));
        });
    }

    @Override
    public void onTimeout() {
        String out = last;
        main.post(() -> deliver(out));
    }

    public void stop() {
        loading = false;
        if (service != null) {
            try {
                service.stop();
            } catch (Exception ignored) {
            }
        }
    }

    public void shutdown() {
        stop();
        if (service != null) {
            try {
                service.shutdown();
            } catch (Exception ignored) {
            }
            service = null;
        }
        if (recognizer != null) {
            try {
                recognizer.close();
            } catch (Exception ignored) {
            }
            recognizer = null;
        }
        if (model != null) {
            try {
                model.close();
            } catch (Exception ignored) {
            }
            model = null;
        }
    }
}
