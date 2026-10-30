package org.example.quranteacher;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Mémorisation : le verset est caché, l'utilisatrice récite de mémoire, puis
 * vérifie en l'affichant. Chaque récitation est enregistrée.
 */
public class MemoryActivity extends BaseActivity {

    private static final String K_VERSE = "memory_verse";

    private int verseIndex = 0;
    private boolean visible = false;

    private TextView labelView;
    private TextView verseView;
    private TextView hintView;
    private TextView repsView;
    private android.widget.Button toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.screen_memory));
        if (savedInstanceState != null) {
            verseIndex = savedInstanceState.getInt(K_VERSE, 0);
            visible = savedInstanceState.getBoolean("memory_visible", false);
        }
        buildScreen();
        render(true);
    }

    private void buildScreen() {
        LinearLayout column = Ui.column(this);
        column.addView(Ui.heading(this, getString(R.string.screen_memory)));

        labelView = Ui.subheading(this, "");
        verseView = Ui.verse(this, "");
        hintView = Ui.body(this, getString(R.string.memory_instruction));
        repsView = Ui.body(this, "");

        column.addView(labelView);
        column.addView(hintView);

        LinearLayout verseCard = Ui.card(this);
        verseCard.addView(verseView);
        column.addView(verseCard);
        column.addView(repsView);

        column.addView(Ui.button(this, getString(R.string.btn_listen_verse),
                v -> listen()));
        column.addView(Ui.button(this, getString(R.string.btn_stop_audio),
                v -> stopRecitation()));
        toggleButton = Ui.primaryButton(this, getString(R.string.btn_show_verse), v -> toggle());
        column.addView(toggleButton);
        column.addView(Ui.button(this, getString(R.string.btn_recited), v -> markRecited()));
        column.addView(Ui.button(this, getString(R.string.btn_next_verse), v -> move(1)));
        column.addView(Ui.button(this, getString(R.string.btn_prev_verse), v -> move(-1)));

        addNavigation(column);
        setScreen(column, null);
    }

    private void render(boolean speak) {
        QuranData.Surah surah = QuranData.SURAHS[0];
        labelView.setText(getString(R.string.verse_label,
                getString(surah.nameRes), verseIndex + 1));
        verseView.setText(visible ? surah.verses[verseIndex] : "•••");
        toggleButton.setText(visible
                ? getString(R.string.btn_hide_verse)
                : getString(R.string.btn_show_verse));
        toggleButton.setContentDescription(toggleButton.getText());
        repsView.setText(getString(R.string.repetitions_count,
                progress.repetitions(surah.verseKey(verseIndex))));

        if (speak) {
            announce(teacherCtx.getString(R.string.teacher_verse_intro,
                    teacherCtx.getString(surah.nameRes), verseIndex + 1));
        }
    }

    private void listen() {
        playVerse(QuranData.SURAHS[0], verseIndex);
    }

    private void toggle() {
        visible = !visible;
        render(false);
        say(visible ? R.string.teacher_verse_shown : R.string.teacher_verse_hidden);
    }

    private void markRecited() {
        QuranData.Surah surah = QuranData.SURAHS[0];
        String key = surah.verseKey(verseIndex);
        progress.addRepetition(key);
        progress.markReviewed(key);
        if (visible) {
            progress.setLearned(key, true);
        }
        say(R.string.teacher_review_saved);
        render(false);
    }

    private void move(int delta) {
        QuranData.Surah surah = QuranData.SURAHS[0];
        int target = verseIndex + delta;
        if (target < 0 || target >= surah.verseCount()) {
            say(delta > 0 ? R.string.teacher_verse_last : R.string.teacher_verse_first);
            return;
        }
        verseIndex = target;
        visible = false;
        render(true);
    }

    @Override
    protected void onScreenCommand(String action) {
        if (CommandKeywords.NEXT.equals(action)) {
            move(1);
        } else if (CommandKeywords.PREV.equals(action)) {
            move(-1);
        } else if (CommandKeywords.SHOW.equals(action)) {
            if (!visible) {
                toggle();
            } else {
                say(R.string.teacher_verse_shown);
            }
        } else if (CommandKeywords.HIDE.equals(action)) {
            if (visible) {
                toggle();
            } else {
                say(R.string.teacher_verse_hidden);
            }
        } else if (CommandKeywords.YES.equals(action)) {
            markRecited();
        } else {
            super.onScreenCommand(action);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(K_VERSE, verseIndex);
        outState.putBoolean("memory_visible", visible);
    }
}
