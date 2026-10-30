package org.example.quranteacher;

import android.os.Bundle;
import android.widget.LinearLayout;

/** Écran d'accueil : tout est atteignable au clavier TalkBack ou à la voix. */
public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.screen_home));
        buildScreen();
        // Premier lancement : les récitations livrées partent sur la carte.
        if (!AudioInstaller.isInstalled(this) && AudioInstaller.hasBundle(this)) {
            installBundledAudio();
        }
    }

    private void buildScreen() {
        LinearLayout column = Ui.column(this);

        column.addView(Ui.heading(this, getString(R.string.app_name)));
        column.addView(Ui.subheading(this, getString(R.string.home_greeting)));
        column.addView(Ui.body(this, getString(R.string.app_tagline)));

        column.addView(Ui.primaryButton(this, getString(R.string.btn_start_session),
                v -> openScreen(LessonActivity.class)));
        column.addView(Ui.button(this, getString(R.string.btn_lessons),
                v -> openScreen(LessonActivity.class)));
        column.addView(Ui.button(this, getString(R.string.btn_memory),
                v -> openScreen(MemoryActivity.class)));
        column.addView(Ui.button(this, getString(R.string.btn_exam),
                v -> openScreen(ExamActivity.class)));
        column.addView(Ui.button(this, getString(R.string.btn_progress),
                v -> openScreen(ProgressActivity.class)));
        column.addView(Ui.button(this, getString(R.string.btn_settings),
                v -> openScreen(SettingsActivity.class)));

        column.addView(Ui.button(this, getString(R.string.btn_voice),
                v -> startVoiceCommand()));

        int learned = progress.learnedCount();
        CharSequence welcome = learned > 0
                ? teacherCtx.getString(R.string.teacher_home_with_progress, learned)
                : teacherCtx.getString(R.string.teacher_home);

        setScreen(column, welcome);
    }
}
