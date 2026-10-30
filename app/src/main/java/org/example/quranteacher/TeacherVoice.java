package org.example.quranteacher;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/**
 * La voix du professeur (synthèse vocale).
 *
 * Règles importantes :
 * - cette voix ne lit JAMAIS un verset du Coran : seules les consignes passent
 *   par ici, les versets doivent venir d'enregistrements authentiques ;
 * - la synthèse est asynchrone : un texte demandé avant l'initialisation est
 *   mis en attente puis prononcé dès que le moteur est prêt (sinon la première
 *   consigne était perdue) ;
 * - si la langue demandée n'existe pas sur le téléphone, on retombe sur la
 *   langue du système et on prévient l'utilisatrice.
 */
public final class TeacherVoice {

    private static final int STATE_UNKNOWN = 0;
    private static final int STATE_OK = 1;
    private static final int STATE_MISSING = 2;

    private TextToSpeech tts;
    private boolean engineReady = false;
    private int languageState = STATE_UNKNOWN;
    private final boolean enabled;
    private final Locale locale;
    private final float rate;

    private String pending = null;
    private String lastSpoken = null;
    private Runnable onReady = null;

    public TeacherVoice(Context appContext, String langTag, float rate, boolean enabled) {
        this.locale = Locale.forLanguageTag(langTag);
        this.rate = rate;
        this.enabled = enabled;
        this.tts = new TextToSpeech(appContext, status -> {
            engineReady = status == TextToSpeech.SUCCESS && tts != null;
            if (engineReady) {
                configureLanguage();
            }
            Runnable ready = onReady;
            onReady = null;
            if (ready != null) {
                ready.run();
            }
            String queued = pending;
            pending = null;
            if (queued != null) {
                speakNow(queued);
            }
        });
    }

    /** Appelé quand le moteur vient de finir de s'initialiser (ou tout de suite s'il l'était déjà). */
    public void setOnReady(Runnable action) {
        if (engineReady) {
            action.run();
        } else {
            onReady = action;
        }
    }

    private void configureLanguage() {
        try {
            int result = tts.setLanguage(locale);
            boolean ok = result != TextToSpeech.LANG_MISSING_DATA
                    && result != TextToSpeech.LANG_NOT_SUPPORTED;
            languageState = ok ? STATE_OK : STATE_MISSING;
            if (!ok) {
                // Repli : la voix du système, pour au moins prévenir l'utilisatrice.
                tts.setLanguage(Locale.getDefault());
            }
            tts.setSpeechRate(rate);
        } catch (Exception e) {
            languageState = STATE_MISSING;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isReady() {
        return engineReady;
    }

    /**
     * Optimiste tant que le moteur n'a pas répondu : la première consigne part
     * dans la bonne langue sur un téléphone correctement configuré.
     */
    public boolean isLanguageAvailable() {
        return languageState != STATE_MISSING;
    }

    public void speak(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        lastSpoken = text;
        if (!enabled) {
            return;
        }
        if (engineReady) {
            speakNow(text);
        } else {
            pending = text;
        }
    }

    private void speakNow(String text) {
        try {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, (Bundle) null, "teacher");
        } catch (Exception ignored) {
            // Un moteur TTS qui tombe ne doit pas faire planter l'application.
        }
    }

    /** Relit la dernière consigne, pour la commande vocale « répète ». */
    public void repeat() {
        if (lastSpoken != null) {
            speak(lastSpoken);
        }
    }

    public void stop() {
        if (tts != null) {
            try {
                tts.stop();
            } catch (Exception ignored) {
            }
        }
    }

    public void shutdown() {
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }
            tts = null;
            engineReady = false;
        }
    }
}
