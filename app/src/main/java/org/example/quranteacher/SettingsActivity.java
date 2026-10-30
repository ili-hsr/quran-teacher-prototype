package org.example.quranteacher;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;

/**
 * Paramètres : trois langues indépendantes (interface, professeur,
 * reconnaissance) et réglages de la voix.
 *
 * Chaque option est un gros bouton ; l'option active est annoncée comme
 * « choix actuel » par TalkBack.
 */
public class SettingsActivity extends BaseActivity {

    private static final long RECREATE_DELAY_MS = 1200L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.screen_settings));
        buildScreen();
    }

    private void buildScreen() {
        LinearLayout column = Ui.column(this);
        column.addView(Ui.heading(this, getString(R.string.screen_settings)));

        Ui.section(this, column, getString(R.string.settings_interface_language));
        String ui = AppPrefs.uiLang(this);
        addOption(column, getString(R.string.lang_fr), "fr".equals(ui), () -> setUiLang("fr"));
        addOption(column, getString(R.string.lang_ar), "ar".equals(ui), () -> setUiLang("ar"));
        addOption(column, getString(R.string.lang_en), "en".equals(ui), () -> setUiLang("en"));
        addOption(column, getString(R.string.lang_nl), "nl".equals(ui), () -> setUiLang("nl"));

        Ui.section(this, column, getString(R.string.settings_teacher_language));
        String teacherLang = AppPrefs.teacherLang(this);
        addOption(column, getString(R.string.lang_ar), "ar".equals(teacherLang),
                () -> setTeacherLang("ar"));
        addOption(column, getString(R.string.lang_fr), "fr".equals(teacherLang),
                () -> setTeacherLang("fr"));
        addOption(column, getString(R.string.lang_en), "en".equals(teacherLang),
                () -> setTeacherLang("en"));
        addOption(column, getString(R.string.lang_nl), "nl".equals(teacherLang),
                () -> setTeacherLang("nl"));

        Ui.section(this, column, getString(R.string.settings_teacher_voice));
        boolean voiceOn = AppPrefs.isTeacherOn(this);
        addOption(column, getString(R.string.voice_on), voiceOn, () -> setVoice(true));
        addOption(column, getString(R.string.voice_off), !voiceOn, () -> setVoice(false));

        Ui.section(this, column, getString(R.string.settings_teacher_rate));
        int rate = AppPrefs.teacherRateIndex(this);
        addOption(column, getString(R.string.rate_slow), rate == 0, () -> setRate(0));
        addOption(column, getString(R.string.rate_normal), rate == 1, () -> setRate(1));
        addOption(column, getString(R.string.rate_fast), rate == 2, () -> setRate(2));

        Ui.section(this, column, getString(R.string.settings_recognition_language));
        String recog = AppPrefs.recogLang(this);
        addOption(column, getString(R.string.recog_ar), "ar-SA".equals(recog),
                () -> setRecog("ar-SA"));
        addOption(column, getString(R.string.recog_fr), "fr-FR".equals(recog),
                () -> setRecog("fr-FR"));
        addOption(column, getString(R.string.recog_ma), "ar-MA".equals(recog),
                () -> setRecog("ar-MA"));

        column.addView(Ui.body(this, getString(R.string.settings_recognition_note)));

        column.addView(Ui.button(this, getString(R.string.btn_test_voice),
                v -> say(R.string.teacher_voice_test)));

        addNavigation(column);
        setScreen(column, teacherCtx.getString(R.string.teacher_settings_intro));
    }

    private void addOption(LinearLayout column, String label, boolean current, Runnable action) {
        CharSequence text = current ? getString(R.string.current_choice, label) : label;
        column.addView(Ui.button(this, text, v -> action.run()));
    }

    /** Confirme, puis recrée l'écran pour appliquer la nouvelle langue partout. */
    private void confirmAndRecreate() {
        say(R.string.teacher_saved);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                recreate();
            }
        }, RECREATE_DELAY_MS);
    }

    private void setUiLang(String tag) {
        AppPrefs.setUiLang(this, tag);
        confirmAndRecreate();
    }

    private void setTeacherLang(String tag) {
        AppPrefs.setTeacherLang(this, tag);
        confirmAndRecreate();
    }

    private void setVoice(boolean on) {
        AppPrefs.setTeacherOn(this, on);
        if (on) {
            say(R.string.teacher_voice_test);
        }
        confirmAndRecreate();
    }

    private void setRate(int index) {
        AppPrefs.setTeacherRateIndex(this, index);
        confirmAndRecreate();
    }

    private void setRecog(String tag) {
        AppPrefs.setRecogLang(this, tag);
        confirmAndRecreate();
    }
}
