package org.example.quranteacher;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Tableau de bord : lecture seule, tout vient du stockage local. */
public class ProgressActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.screen_progress));
        buildScreen();
    }

    private void buildScreen() {
        LinearLayout column = Ui.column(this);
        column.addView(Ui.heading(this, getString(R.string.screen_progress)));

        int learned = progress.learnedCount();
        int repetitions = progress.totalRepetitions();
        int today = progress.repsToday();
        int goal = progress.dailyGoal();
        int exams = progress.examCount();
        int average = progress.examAverage();
        int minutes = progress.learningMinutes();

        column.addView(Ui.body(this, getString(R.string.progress_learned, learned)));
        column.addView(Ui.body(this, getString(R.string.progress_repetitions, repetitions)));
        column.addView(Ui.body(this, getString(R.string.progress_today, today, goal)));
        column.addView(Ui.body(this, getString(R.string.progress_exams, exams)));
        column.addView(Ui.body(this, getString(R.string.progress_average, average)));
        column.addView(Ui.body(this, getString(R.string.progress_time, minutes)));
        column.addView(Ui.body(this, getString(R.string.progress_local_note)));

        if (learned == 0 && repetitions == 0 && exams == 0) {
            column.addView(Ui.body(this, getString(R.string.progress_none)));
        }

        addNavigation(column);

        String summary = teacherCtx.getString(R.string.teacher_progress_intro)
                + " " + teacherCtx.getString(R.string.progress_learned, learned)
                + " " + teacherCtx.getString(R.string.progress_repetitions, repetitions)
                + " " + teacherCtx.getString(R.string.progress_today, today, goal);
        setScreen(column, summary);
    }
}
