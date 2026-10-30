package org.example.quranteacher;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Sauvegarde locale des progrès.
 *
 * Tout est écrit avec commit() : en cas de coupure brutale (batterie, arrêt
 * forcé), la donnée est déjà sur le disque. Sur un téléphone de 2 Go, ce coût
 * est négligeable et la fiabilité prime.
 */
public final class ProgressStore {

    private static final String FILE = "quran_progress_v2";

    private static final String K_TOTAL_REPS = "total_reps";
    private static final String K_SECONDS = "learning_seconds";
    private static final String K_EXAM_COUNT = "exam_count";
    private static final String K_EXAM_SUM = "exam_percent_sum";
    private static final String K_EXAM_BEST = "exam_best";
    private static final String K_DAILY_GOAL = "daily_goal";

    public static final int DEFAULT_DAILY_GOAL = 20;

    private final SharedPreferences prefs;

    public ProgressStore(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // --- Versets -----------------------------------------------------------

    public int repetitions(String verseKey) {
        return prefs.getInt("reps_" + verseKey, 0);
    }

    public void addRepetition(String verseKey) {
        String key = "reps_" + verseKey;
        prefs.edit()
                .putInt(key, prefs.getInt(key, 0) + 1)
                .putInt(K_TOTAL_REPS, totalRepetitions() + 1)
                .putInt("today_" + today(), repsToday() + 1)
                .commit();
    }

    public boolean isLearned(String verseKey) {
        return prefs.getBoolean("learned_" + verseKey, false);
    }

    public void setLearned(String verseKey, boolean learned) {
        prefs.edit().putBoolean("learned_" + verseKey, learned).commit();
    }

    public void markReviewed(String verseKey) {
        prefs.edit().putLong("last_" + verseKey, System.currentTimeMillis()).commit();
    }

    /** Nombre de versets marqués comme mémorisés. */
    public int learnedCount() {
        int count = 0;
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getKey().startsWith("learned_")
                    && Boolean.TRUE.equals(entry.getValue())) {
                count++;
            }
        }
        return count;
    }

    // --- Compteurs globaux -------------------------------------------------

    public int totalRepetitions() {
        return prefs.getInt(K_TOTAL_REPS, 0);
    }

    public int repsToday() {
        return prefs.getInt("today_" + today(), 0);
    }

    public int dailyGoal() {
        return prefs.getInt(K_DAILY_GOAL, DEFAULT_DAILY_GOAL);
    }

    public void setDailyGoal(int goal) {
        prefs.edit().putInt(K_DAILY_GOAL, goal).commit();
    }

    public void addLearningSeconds(long seconds) {
        if (seconds <= 0) {
            return;
        }
        prefs.edit().putLong(K_SECONDS, learningSeconds() + seconds).commit();
    }

    public long learningSeconds() {
        return prefs.getLong(K_SECONDS, 0L);
    }

    public int learningMinutes() {
        return (int) (learningSeconds() / 60L);
    }

    // --- Examens -----------------------------------------------------------

    public void addExamResult(int correct, int total) {
        if (total <= 0) {
            return;
        }
        int percent = Math.round(100f * correct / total);
        int best = Math.max(percent, prefs.getInt(K_EXAM_BEST, 0));
        prefs.edit()
                .putInt(K_EXAM_COUNT, examCount() + 1)
                .putInt(K_EXAM_SUM, prefs.getInt(K_EXAM_SUM, 0) + percent)
                .putInt(K_EXAM_BEST, best)
                .commit();
    }

    public int examCount() {
        return prefs.getInt(K_EXAM_COUNT, 0);
    }

    public int examAverage() {
        int count = examCount();
        return count == 0 ? 0 : prefs.getInt(K_EXAM_SUM, 0) / count;
    }

    public int examBest() {
        return prefs.getInt(K_EXAM_BEST, 0);
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}
