package org.example.quranteacher;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Récitation guidée : le professeur fait écouter le verset, écoute la
 * récitation, et coupe à la première erreur pour corriger tout de suite.
 */
public class ReciteActivity extends BaseActivity {

    private final Handler ui = new Handler(Looper.getMainLooper());

    private int surahIndex;
    private int verseIndex;
    private QuranData.Surah surah;
    private RecitationTracker tracker;
    private VoskAsr asr;

    private TextView labelView;
    private TextView statusView;
    private TextView progressView;

    private boolean listening;
    private boolean pendingResume;
    private int resumeIndex;

    private final VoskAsr.Stream streamCallback = new VoskAsr.Stream() {
        @Override
        public void onPartial(String text) {
            handle(text);
        }

        @Override
        public void onFinal(String text) {
            handle(text);
            if (tracker != null) {
                tracker.endUtterance();
            }
        }

        @Override
        public void onFailed(String message) {
            listening = false;
            setStatus(getString(R.string.teacher_mic_unavailable));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.screen_recite));
        surahIndex = getIntent().getIntExtra(LessonActivity.EXTRA_SURAH, 0);
        verseIndex = getIntent().getIntExtra(LessonActivity.EXTRA_VERSE, 0);
        surah = QuranData.surah(surahIndex);
        if (surah == null) {
            surahIndex = 0;
            surah = QuranData.surah(0);
        }
        if (surah != null && verseIndex >= surah.verseCount()) {
            verseIndex = 0;
        }
        buildScreen();
        startFlow();
    }

    private void buildScreen() {
        LinearLayout column = Ui.column(this);
        column.addView(Ui.heading(this, getString(R.string.screen_recite)));

        labelView = Ui.subheading(this, "");
        statusView = Ui.body(this, "");
        progressView = Ui.meta(this, "");

        column.addView(labelView);
        column.addView(statusView);
        column.addView(progressView);

        column.addView(Ui.primaryButton(this, getString(R.string.btn_restart_verse),
                v -> startFlow()));
        column.addView(Ui.button(this, getString(R.string.btn_next_verse),
                v -> nextVerse()));
        column.addView(Ui.button(this, getString(R.string.btn_stop_audio),
                v -> stopEverything()));
        addNavigation(column);

        setScreen(column, null);
        refreshLabel();
    }

    private void refreshLabel() {
        if (surah == null) {
            return;
        }
        labelView.setText(getString(R.string.verse_label, surah.latinLabel(), verseIndex + 1));
    }

    private void startFlow() {
        if (surah == null) {
            return;
        }
        listening = false;
        pendingResume = false;
        stopEverything(false);
        tracker = new RecitationTracker(surah.verses[verseIndex]);
        refreshLabel();
        setStatus(getString(R.string.recite_intro));
        say(R.string.recite_intro);
        progressView.setText("");
        ui.postDelayed(this::playModelOrListen, 3800);
    }

    private void playModelOrListen() {
        if (recitations != null && recitations.uriFor(surah, verseIndex) != null) {
            playVerse(surah, verseIndex);
        } else {
            beginListening();
        }
    }

    @Override
    protected void onRecitationFinished() {
        if (pendingResume) {
            pendingResume = false;
            beginListening();
        } else {
            beginListening();
        }
    }

    private void beginListening() {
        final String tag = ModelManager.tagOf(AppPrefs.recogLang(this));
        if (!ModelManager.isInstalled(this, tag)) {
            setStatus(getString(R.string.teacher_no_model));
            say(R.string.teacher_no_model);
            return;
        }
        if (tracker == null) {
            tracker = new RecitationTracker(surah.verses[verseIndex]);
        }
        if (asr == null) {
            asr = new VoskAsr();
        }
        listening = true;
        setStatus(getString(R.string.recite_listening));
        say(R.string.recite_listening);
        ui.postDelayed(() -> {
            if (teacher != null) {
                teacher.stop();
            }
            asr.listenStream(ReciteActivity.this, tag, streamCallback);
        }, 1300);
    }

    private void handle(String text) {
        if (!listening || tracker == null || text == null) {
            return;
        }
        RecitationTracker.Step step = tracker.feed(text);
        progressView.setText(getString(R.string.recite_progress, step.cursor, tracker.length()));
        if (step.hasError()) {
            onMistake(step);
            return;
        }
        if (step.finished) {
            onComplete();
        }
    }

    private void onMistake(RecitationTracker.Step step) {
        listening = false;
        if (asr != null) {
            asr.stop();
        }
        resumeIndex = step.errorIndex;
        int word = step.errorIndex + 1;
        int message = step.omission ? R.string.recite_omission : R.string.recite_wrong;
        setStatus(getString(message, word));
        say(message, word);
        pendingResume = true;
        ui.postDelayed(() -> {
            if (recitations != null && recitations.uriFor(surah, verseIndex) != null) {
                playVerse(surah, verseIndex);
            } else {
                pendingResume = false;
                resumeAfterMistake();
            }
        }, 3200);
    }

    private void resumeAfterMistake() {
        if (tracker != null) {
            tracker.resumeFrom(resumeIndex);
        }
        final String tag = ModelManager.tagOf(AppPrefs.recogLang(this));
        if (asr == null) {
            asr = new VoskAsr();
        }
        listening = true;
        say(R.string.recite_resume);
        setStatus(getString(R.string.recite_resume));
        ui.postDelayed(() -> {
            if (teacher != null) {
                teacher.stop();
            }
            asr.listenStream(ReciteActivity.this, tag, streamCallback);
        }, 1300);
    }

    private void onComplete() {
        listening = false;
        if (asr != null) {
            asr.stop();
        }
        String key = surah.verseKey(verseIndex);
        progress.addRepetition(key);
        progress.markReviewed(key);
        progress.setLearned(key, true);
        setStatus(getString(R.string.recite_done));
        say(R.string.recite_done);
        say(R.string.teacher_learned_saved);
    }

    private void nextVerse() {
        if (surah == null) {
            return;
        }
        if (verseIndex + 1 < surah.verseCount()) {
            verseIndex++;
            startFlow();
        } else {
            say(R.string.teacher_verse_last);
        }
    }

    private void setStatus(String text) {
        statusView.setText(text);
    }

    private void stopEverything() {
        stopEverything(true);
    }

    private void stopEverything(boolean speakNothing) {
        listening = false;
        if (asr != null) {
            asr.stop();
        }
        stopRecitation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        listening = false;
        if (asr != null) {
            asr.stop();
        }
    }

    @Override
    protected void onDestroy() {
        if (asr != null) {
            asr.shutdown();
            asr = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onSpecificCommand(String action) {
        if (CommandKeywords.NEXT.equals(action)) {
            nextVerse();
        } else if (CommandKeywords.PREV.equals(action)) {
            if (verseIndex > 0) {
                verseIndex--;
            }
            startFlow();
        } else if (CommandKeywords.REPEAT.equals(action)) {
            startFlow();
        } else if (CommandKeywords.STOP.equals(action)) {
            stopEverything();
        } else {
            super.onSpecificCommand(action);
        }
    }
}
