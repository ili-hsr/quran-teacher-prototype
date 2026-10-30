package org.example.quranteacher;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;

import java.util.ArrayList;

/**
 * Base commune à tous les écrans.
 *
 * Elle corrige trois défauts de la version précédente :
 * 1. la langue d'interface est appliquée avant la création des vues ;
 * 2. l'écran est construit UNE fois ; les mises à jour modifient les textes en
 *    place, donc TalkBack ne perd plus le focus à chaque appui ;
 * 3. le bouton Retour du téléphone ramène à l'accueil au lieu de quitter
 *    l'application.
 */
public abstract class BaseActivity extends Activity {

    private static final int REQ_MIC = 210;
    private static final int REQ_TREE = 220;
    /** Laisse la consigne se terminer avant d'ouvrir le micro. */
    private static final long LISTEN_DELAY_MS = 1100L;

    protected TeacherVoice teacher;
    protected Context teacherCtx;
    protected ProgressStore progress;
    protected RecitationStore recitations;
    protected RecitationPlayer player;

    private SpeechRecognizer recognizer;
    private long learningStart = 0L;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppPrefs.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progress = new ProgressStore(this);
        recitations = new RecitationStore(this);
        player = new RecitationPlayer(this);
        // Fond « aurore » : c'est lui qui donne sa matière au verre des boutons.
        getWindow().setBackgroundDrawable(new AuraBackground());
        teacherCtx = AppPrefs.teacherContext(this);
        teacher = new TeacherVoice(
                getApplicationContext(),
                AppPrefs.teacherLang(this),
                AppPrefs.teacherRate(this),
                AppPrefs.isTeacherOn(this));

        teacher.setOnReady(() -> {
            if (!teacher.isLanguageAvailable()) {
                // La voix de la langue du professeur n'est pas installée :
                // on le dit clairement, dans la langue de l'interface.
                teacher.speak(getString(R.string.teacher_voice_missing));
            }
        });
    }

    // --- Voix du professeur ------------------------------------------------

    /** Prononce une phrase du professeur dans SA langue (indépendante de l'interface). */
    protected void say(int resId, Object... args) {
        if (teacher == null) {
            return;
        }
        Context ctx = teacher.isLanguageAvailable() ? teacherCtx : this;
        teacher.speak(ctx.getString(resId, args));
    }

    protected void sayText(String text) {
        if (teacher != null) {
            teacher.speak(text);
        }
    }

    protected void repeatLast() {
        if (teacher != null) {
            teacher.repeat();
        }
    }

    // --- Affichage ---------------------------------------------------------

    /**
     * Affiche l'écran. À n'appeler qu'une fois par Activity : pour la suite,
     * modifier les TextView existants et appeler {@link #announce(CharSequence)}.
     */
    protected void setScreen(LinearLayout column, CharSequence announcement) {
        setContentView(Ui.wrap(column));
        View root = getWindow().getDecorView();
        root.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
        if (announcement != null && announcement.length() > 0) {
            announce(announcement);
        }
    }

    /**
     * Annonce un changement d'état sans reconstruire l'écran.
     * Voix du professeur si elle est active, sinon TalkBack : jamais les deux,
     * pour éviter deux voix qui parlent en même temps.
     */
    protected void announce(CharSequence text) {
        if (text == null || text.length() == 0) {
            return;
        }
        if (teacher != null && teacher.isEnabled()) {
            teacher.speak(text.toString());
        } else {
            getWindow().getDecorView().announceForAccessibility(text);
        }
    }

    /** Boutons communs en bas de chaque écran. */
    protected void addNavigation(LinearLayout column) {
        column.addView(Ui.button(this, getString(R.string.btn_repeat_instruction),
                v -> repeatLast()));
        column.addView(Ui.button(this, getString(R.string.btn_voice),
                v -> startVoiceCommand()));
        column.addView(Ui.button(this, getString(R.string.btn_home),
                v -> goHome()));
    }

    // --- Navigation --------------------------------------------------------

    protected void openScreen(Class<? extends Activity> target) {
        startActivity(new Intent(this, target));
        if (!(this instanceof MainActivity)) {
            finish();
        }
    }

    protected void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        // CLEAR_TOP recrée l'accueil : la langue d'interface y est réappliquée.
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (this instanceof MainActivity) {
            super.onBackPressed();
        } else {
            goHome();
        }
    }

    // --- Commandes vocales -------------------------------------------------

    protected void startVoiceCommand() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            say(R.string.teacher_mic_unavailable);
            return;
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(recognitionListener);
        }
        say(R.string.teacher_listening);
        new Handler(Looper.getMainLooper()).postDelayed(this::startListening, LISTEN_DELAY_MS);
    }

    private void startListening() {
        if (recognizer == null) {
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, AppPrefs.recogLang(this));
        // Priorité au moteur hors ligne : l'application ne dépend pas d'Internet.
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        try {
            recognizer.startListening(intent);
        } catch (Exception e) {
            say(R.string.teacher_mic_unavailable);
        }
    }

    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onError(int error) {
            say(R.string.teacher_not_understood);
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches == null || matches.isEmpty()) {
                say(R.string.teacher_not_understood);
                return;
            }
            String action = null;
            for (String candidate : matches) {
                action = CommandKeywords.match(BaseActivity.this, candidate);
                if (action != null) {
                    break;
                }
            }
            if (action == null) {
                say(R.string.teacher_not_understood);
            } else {
                onCommand(action);
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_MIC) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoiceCommand();
        } else {
            say(R.string.teacher_mic_denied);
        }
    }

    /** Une commande a été reconnue. */
    protected void onCommand(String action) {
        if (handleGlobalCommand(action)) {
            return;
        }
        onScreenCommand(action);
    }

    /** Commandes valables partout. @return true si l'action a été traitée. */
    protected boolean handleGlobalCommand(String action) {
        if (action == null) {
            return false;
        }
        switch (action) {
            case CommandKeywords.HOME:
                goHome();
                return true;
            case CommandKeywords.LESSONS:
                openScreen(LessonActivity.class);
                return true;
            case CommandKeywords.MEMORY:
                openScreen(MemoryActivity.class);
                return true;
            case CommandKeywords.EXAM:
                openScreen(ExamActivity.class);
                return true;
            case CommandKeywords.PROGRESS:
                openScreen(ProgressActivity.class);
                return true;
            case CommandKeywords.SETTINGS:
                openScreen(SettingsActivity.class);
                return true;
            case CommandKeywords.HELP:
                say(R.string.teacher_help);
                return true;
            case CommandKeywords.REPEAT:
                repeatLast();
                return true;
            case CommandKeywords.STOP:
                teacher.stop();
                return true;
            default:
                return false;
        }
    }

    /** Commandes propres à l'écran courant (suivant, précédent, montre...). */
    protected void onScreenCommand(String action) {
        say(R.string.teacher_not_understood);
    }

    // --- Récitations audio (fichiers authentiques, jamais de synthèse) -----

    /** Joue la récitation d'un verset si le fichier existe. */
    protected void playVerse(QuranData.Surah surah, int verseIndex) {
        Uri uri = recitations.uriFor(surah.key, verseIndex);
        if (uri == null) {
            say(R.string.teacher_audio_not_found);
            return;
        }
        say(R.string.teacher_audio_playing);
        player.play(uri, new RecitationPlayer.Listener() {
            @Override
            public void onFinished() {
                onRecitationFinished();
            }

            @Override
            public void onFailed() {
                say(R.string.teacher_audio_not_found);
            }
        });
    }

    protected void stopRecitation() {
        if (player != null) {
            player.stop();
        }
    }

    /** Appelé quand une récitation se termine (surchargé au besoin). */
    protected void onRecitationFinished() {
    }

    /** Choix du dossier contenant les fichiers audio (une seule fois). */
    protected void chooseAudioFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQ_TREE);
        } catch (Exception e) {
            say(R.string.teacher_audio_not_found);
        }
    }

    protected void onAudioFolderChosen() {
        say(R.string.audio_folder_count, recitations.availableCount());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_TREE || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri tree = data.getData();
        if (tree == null) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(
                    tree, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
            // Certains fournisseurs ne persistent pas : lecture tant que l'app vit.
        }
        recitations.setTreeUri(tree.toString());
        onAudioFolderChosen();
    }

    // --- Cycle de vie ------------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();
        learningStart = SystemClock.elapsedRealtime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (learningStart > 0L) {
            progress.addLearningSeconds((SystemClock.elapsedRealtime() - learningStart) / 1000L);
            learningStart = 0L;
        }
        if (teacher != null) {
            teacher.stop();
        }
        if (player != null) {
            player.stop();
        }
        if (recognizer != null) {
            try {
                recognizer.cancel();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.stop();
        }
        if (teacher != null) {
            teacher.shutdown();
            teacher = null;
        }
        if (recognizer != null) {
            try {
                recognizer.destroy();
            } catch (Exception ignored) {
            }
            recognizer = null;
        }
        super.onDestroy();
    }
}
