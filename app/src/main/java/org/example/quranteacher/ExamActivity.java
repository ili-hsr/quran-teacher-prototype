package org.example.quranteacher;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Examen oral.
 *
 * Honnêteté technique : la reconnaissance vocale du téléphone ne permet pas de
 * corriger le tajwid ni de garantir une correction parfaite. L'application
 * demande donc à l'utilisatrice de réciter, écoute (plus tard, avec un moteur
 * local), puis c'est elle qui valide « juste » ou « à revoir ». Aucun score
 * n'est présenté comme une certification.
 */
public class ExamActivity extends BaseActivity {

    private static final int QUESTION_COUNT = 5;

    private final List<int[]> questions = new ArrayList<>();
    private int questionIndex = 0;
    private int correctAnswers = 0;
    private boolean finished = false;

    private TextView questionView;
    private TextView progressView;
    private TextView feedbackView;
    private LinearLayout answerRow;
    private LinearLayout finishedRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.screen_exam));
        buildQuestions();
        buildScreen();
        showQuestion(true);
    }

    private void buildQuestions() {
        List<int[]> pool = new ArrayList<>();
        for (int s = 0; s < QuranData.SURAHS.length; s++) {
            for (int v = 0; v < QuranData.SURAHS[s].verseCount(); v++) {
                pool.add(new int[]{s, v});
            }
        }
        Collections.shuffle(pool, new Random());
        int count = Math.min(QUESTION_COUNT, pool.size());
        questions.clear();
        questions.addAll(pool.subList(0, count));
    }

    private void buildScreen() {
        LinearLayout column = Ui.column(this);
        column.addView(Ui.heading(this, getString(R.string.screen_exam)));

        questionView = Ui.subheading(this, "");
        progressView = Ui.body(this, "");
        feedbackView = Ui.body(this, getString(R.string.exam_instruction));

        column.addView(questionView);
        column.addView(progressView);
        column.addView(feedbackView);

        answerRow = new LinearLayout(this);
        answerRow.setOrientation(LinearLayout.VERTICAL);
        answerRow.addView(Ui.primaryButton(this, getString(R.string.btn_correct), v -> answer(true)));
        answerRow.addView(Ui.button(this, getString(R.string.btn_review), v -> answer(false)));
        column.addView(answerRow);

        finishedRow = new LinearLayout(this);
        finishedRow.setOrientation(LinearLayout.VERTICAL);
        finishedRow.addView(Ui.button(this, getString(R.string.btn_new_exam), v -> restart()));
        finishedRow.setVisibility(LinearLayout.GONE);
        column.addView(finishedRow);

        column.addView(Ui.button(this, getString(R.string.btn_finish_exam), v -> finishExam()));
        addNavigation(column);
        setScreen(column, null);
    }

    private void showQuestion(boolean speak) {
        if (questionIndex >= questions.size()) {
            finishExam();
            return;
        }
        int[] position = questions.get(questionIndex);
        QuranData.Surah surah = QuranData.SURAHS[position[0]];
        questionView.setText(getString(R.string.verse_label,
                getString(surah.nameRes), position[1] + 1));
        progressView.setText(getString(R.string.verse_counter,
                questionIndex + 1, questions.size()));
        if (speak) {
            announce(teacherCtx.getString(R.string.teacher_exam_question,
                    teacherCtx.getString(surah.nameRes), position[1] + 1));
        }
    }

    private void answer(boolean correct) {
        if (finished) {
            return;
        }
        if (correct) {
            correctAnswers++;
            progress.addRepetition(currentKey());
            progress.setLearned(currentKey(), true);
        } else {
            progress.markReviewed(currentKey());
        }
        questionIndex++;
        if (questionIndex >= questions.size()) {
            finishExam();
        } else {
            showQuestion(true);
        }
    }

    private String currentKey() {
        if (questionIndex >= questions.size()) {
            return "fatiha_1";
        }
        int[] position = questions.get(questionIndex);
        return QuranData.SURAHS[position[0]].verseKey(position[1]);
    }

    private void finishExam() {
        finished = true;
        int answered = Math.min(questionIndex, questions.size());
        progress.addExamResult(correctAnswers, Math.max(answered, 1));

        questionView.setText(getString(R.string.progress_average,
                answered == 0 ? 0 : Math.round(100f * correctAnswers / answered)));
        progressView.setText(getString(R.string.teacher_exam_score,
                correctAnswers, answered));
        feedbackView.setText(getString(R.string.teacher_recognition_note));
        answerRow.setVisibility(LinearLayout.GONE);
        finishedRow.setVisibility(LinearLayout.VISIBLE);

        announce(teacherCtx.getString(R.string.teacher_exam_score, correctAnswers, answered)
                + " " + teacherCtx.getString(R.string.teacher_encouragement));
    }

    private void restart() {
        finished = false;
        questionIndex = 0;
        correctAnswers = 0;
        buildQuestions();
        answerRow.setVisibility(LinearLayout.VISIBLE);
        finishedRow.setVisibility(LinearLayout.GONE);
        feedbackView.setText(getString(R.string.exam_instruction));
        showQuestion(true);
    }

    @Override
    protected void onScreenCommand(String action) {
        if (CommandKeywords.YES.equals(action)) {
            answer(true);
        } else if (CommandKeywords.NO.equals(action)) {
            answer(false);
        } else if (CommandKeywords.STOP.equals(action)) {
            finishExam();
        } else if (CommandKeywords.NEXT.equals(action)) {
            showQuestion(true);
        } else {
            super.onScreenCommand(action);
        }
    }
}
