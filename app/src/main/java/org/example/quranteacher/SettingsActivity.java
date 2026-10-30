package org.example.quranteacher;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;

/**
 * Paramètres : dix langues d'interface et dix langues de professeur, réglages
 * de la voix, et le dossier des récitations audio.
 *
 * Chaque option est un gros bouton en verre ; l'option active est affichée en
 * verre vert et annoncée comme « choix actuel » par TalkBack.
 */
public class SettingsActivity extends BaseActivity {

    private static final long RECREATE_DELAY_MS = 1200L;

    private static final String[] LANG_TAGS = {
            "fr", "ar", "en", "nl", "es", "de", "it", "pt", "tr", "ur"
    };
    private static final int[] LANG_LABELS = {
            R.string.lang_fr, R.string.lang_ar, R.string.lang_en, R.string.lang_nl,
            R.string.lang_es, R.string.lang_de, R.string.lang_it, R.string.lang_pt,
            R.string.lang_tr, R.string.lang_ur
    };

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
        for (int i = 0; i < LANG_TAGS.length; i++) {
            final String tag = LANG_TAGS[i];
            addOption(column, getString(LANG_LABELS[i]), tag.equals(ui), () -> setUiLang(tag));
        }

        Ui.section(this, column, getString(R.string.settings_teacher_language));
        String teacherLang = AppPrefs.teacherLang(this);
        for (int i = 0; i < LANG_TAGS.length; i++) {
            final String tag = LANG_TAGS[i];
            addOption(column, getString(LANG_LABELS[i]), tag.equals(teacherLang),
                    () -> setTeacherLang(tag));
        }

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
        column.addView(Ui.meta(this, getString(R.string.settings_recognition_note)));

        Ui.section(this, column, getString(R.string.audio_section));
        int count = recitations.availableCount();
        column.addView(Ui.body(this, getString(R.string.audio_folder_count, count)));
        column.addView(Ui.primaryButton(this, getString(R.string.audio_install_button),
                v -> installBundledAudio()));
        column.addView(Ui.button(this, getString(R.string.audio_choose_folder),
                v -> chooseAudioFolder()));
        column.addView(Ui.button(this, getString(R.string.btn_stop_audio),
                v -> stopRecitation()));
        column.addView(Ui.meta(this, getString(R.string.audio_folder_help)));
        column.addView(Ui.meta(this,
                getString(R.string.audio_folder_default, recitations.defaultFolder().getAbsolutePath())));

        column.addView(Ui.button(this, getString(R.string.btn_test_voice),
                v -> say(R.string.teacher_voice_test)));

        addNavigation(column);
        setScreen(column, teacherCtx.getString(R.string.teacher_settings_intro));
    }

    private void addOption(LinearLayout column, String label, boolean current, Runnable action) {
        CharSequence text = current ? getString(R.string.current_choice, label) : label;
        if (current) {
            column.addView(Ui.primaryButton(this, text, v -> action.run()));
        } else {
            column.addView(Ui.button(this, text, v -> action.run()));
        }
    }

    @Override
    protected void onAudioInstalled(AudioInstaller.Result result) {
        super.onAudioInstalled(result);
        new Handler(Looper.getMainLooper()).postDelayed(this::rebuildQuietly, RECREATE_DELAY_MS);
    }

    private void rebuildQuietly() {
        if (!isFinishing()) {
            recreate();
        }
    }

    @Override
    protected void onAudioFolderChosen() {
        say(R.string.audio_folder_count, recitations.availableCount());
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                recreate();
            }
        }, RECREATE_DELAY_MS);
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
