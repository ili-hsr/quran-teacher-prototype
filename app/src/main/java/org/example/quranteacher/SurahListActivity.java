package org.example.quranteacher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

/**
 * Liste des 114 sourates.
 *
 * Chaque sourate est un bouton de pleine largeur, lu par TalkBack dans l'ordre :
 * « 2, Al-Baqara, 286 versets ». C'est le seul moyen simple de parcourir tout le
 * Coran sans vision, et sans arbre de navigation à plusieurs niveaux.
 */
public class SurahListActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.screen_surahs));
        buildScreen();
    }

    private void buildScreen() {
        LinearLayout column = Ui.column(this);
        column.addView(Ui.heading(this, getString(R.string.screen_surahs)));
        column.addView(Ui.meta(this, getString(R.string.surah_list_help)));

        for (int i = 0; i < QuranData.surahs().length; i++) {
            final int index = i;
            QuranData.Surah surah = QuranData.surahs()[i];
            String label = getString(R.string.surah_item, surah.latinLabel(), surah.verseCount());
            column.addView(Ui.button(this, label, v -> open(index)));
        }

        addNavigation(column);
        setScreen(column, teacherCtx.getString(R.string.teacher_surah_list_intro,
                QuranData.totalVerses()));
    }

    private void open(int surahIndex) {
        Intent intent = new Intent(this, LessonActivity.class);
        intent.putExtra(LessonActivity.EXTRA_SURAH, surahIndex);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onScreenCommand(String action) {
        if (CommandKeywords.HOME.equals(action)) {
            goHome();
        } else {
            super.onScreenCommand(action);
        }
    }
}
