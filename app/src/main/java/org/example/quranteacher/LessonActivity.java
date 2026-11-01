package org.example.quranteacher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Mode apprentissage : on avance verset par verset.
 *
 * Le professeur annonce le verset et son numéro, mais ne lit JAMAIS le texte
 * coranique avec la synthèse vocale : l'écoute doit venir d'un enregistrement
 * authentique (étape « Coran audio »).
 */
public class LessonActivity extends BaseActivity {

    /** Index de sourate à ouvrir, transmis par la liste des sourates. */
    public static final String EXTRA_SURAH = "lesson_surah_index";
    public static final String EXTRA_VERSE = "lesson_verse_index";

    private static final String K_SURAH = "lesson_surah";
    private static final String K_VERSE = "lesson_verse";

    private int surahIndex = 0;
    private int verseIndex = 0;

    private TextView labelView;
    private TextView counterView;
    private TextView verseView;
    private TextView statusView;
    private TextView repsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.screen_lessons));
        if (savedInstanceState != null) {
            surahIndex = savedInstanceState.getInt(K_SURAH, 0);
            verseIndex = savedInstanceState.getInt(K_VERSE, 0);
        } else {
            surahIndex = getIntent().getIntExtra(EXTRA_SURAH, 0);
        }
        if (QuranData.surah(surahIndex) == null) {
            surahIndex = 0;
            verseIndex = 0;
        }
        buildScreen();
        render(true);
    }

    private void buildScreen() {
        LinearLayout column = Ui.column(this);
        column.addView(Ui.heading(this, getString(R.string.screen_lessons)));

        labelView = Ui.subheading(this, "");
        counterView = Ui.body(this, "");
        verseView = Ui.verse(this, "");
        statusView = Ui.body(this, "");
        repsView = Ui.body(this, "");

        column.addView(labelView);
        column.addView(counterView);

        LinearLayout verseCard = Ui.card(this);
        verseCard.addView(verseView);
        column.addView(verseCard);
        column.addView(statusView);
        column.addView(repsView);

        column.addView(Ui.button(this, getString(R.string.btn_listen_verse),
                v -> listen()));
        column.addView(Ui.button(this, getString(R.string.btn_recite),
                v -> openRecite()));
        column.addView(Ui.button(this, getString(R.string.btn_stop_audio),
                v -> stopRecitation()));
        column.addView(Ui.primaryButton(this, getString(R.string.btn_next_verse),
                v -> move(1)));
        column.addView(Ui.button(this, getString(R.string.btn_prev_verse),
                v -> move(-1)));
        column.addView(Ui.button(this, getString(R.string.btn_learned),
                v -> markLearned()));

        addNavigation(column);
        setScreen(column, null);
    }

    private void render(boolean speak) {
        QuranData.Surah surah = QuranData.surah(surahIndex);
        String surahName = surah.latinLabel();

        labelView.setText(getString(R.string.verse_label, surahName, verseIndex + 1));
        counterView.setText(getString(R.string.verse_counter, verseIndex + 1, surah.verseCount()));
        verseView.setText(surah.verses[verseIndex]);
        statusView.setText(progress.isLearned(surah.verseKey(verseIndex))
                ? getString(R.string.status_learned)
                : getString(R.string.status_not_learned));
        repsView.setText(getString(R.string.repetitions_count,
                progress.repetitions(surah.verseKey(verseIndex))));

        if (speak) {
            announce(teacherCtx.getString(R.string.teacher_verse_intro,
                    surah.latinLabel(), verseIndex + 1));
        }
    }

    private void move(int delta) {
        QuranData.Surah surah = QuranData.surah(surahIndex);
        int target = verseIndex + delta;
        if (target < 0) {
            say(R.string.teacher_verse_first);
            return;
        }
        if (target >= surah.verseCount()) {
            say(R.string.teacher_verse_last);
            return;
        }
        verseIndex = target;
        render(true);
    }

    private void openRecite() {
        Intent intent = new Intent(this, ReciteActivity.class);
        intent.putExtra(EXTRA_SURAH, surahIndex);
        intent.putExtra(EXTRA_VERSE, verseIndex);
        startActivity(intent);
    }

    private void listen() {
        playVerse(QuranData.surah(surahIndex), verseIndex);
    }

    private void markLearned() {
        QuranData.Surah surah = QuranData.surah(surahIndex);
        String key = surah.verseKey(verseIndex);
        progress.setLearned(key, true);
        progress.addRepetition(key);
        progress.markReviewed(key);
        say(R.string.teacher_learned_saved);
        render(false);
    }

    @Override
    protected void onScreenCommand(String action) {
        if (CommandKeywords.NEXT.equals(action)) {
            move(1);
        } else if (CommandKeywords.PREV.equals(action)) {
            move(-1);
        } else if (CommandKeywords.YES.equals(action)) {
            markLearned();
        } else {
            super.onScreenCommand(action);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(K_SURAH, surahIndex);
        outState.putInt(K_VERSE, verseIndex);
    }
}
