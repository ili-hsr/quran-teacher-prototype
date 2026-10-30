package org.example.quranteacher;

/**
 * Données coraniques embarquées.
 *
 * Le texte arabe est stocké tel quel, sans traduction et sans modification :
 * seuls des enregistrements authentiques doivent être diffusés, jamais une
 * synthèse vocale. Pour l'instant seule Al-Fatiha est présente ; les autres
 * sourates seront importées depuis une source vérifiée avec les fichiers audio
 * (étape « Coran audio »), pour ne pas risquer la moindre erreur de texte.
 */
public final class QuranData {

    public static final class Surah {

        public final String key;
        public final int nameRes;
        public final String[] verses;

        Surah(String key, int nameRes, String[] verses) {
            this.key = key;
            this.nameRes = nameRes;
            this.verses = verses;
        }

        public int verseCount() {
            return verses.length;
        }

        /** Clé stable pour la sauvegarde, ex. « fatiha_3 ». */
        public String verseKey(int index) {
            return key + "_" + (index + 1);
        }
    }

    private static final Surah FATIHA = new Surah(
            "fatiha",
            R.string.surah_fatiha,
            new String[]{
                    "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
                    "الرَّحْمَٰنِ الرَّحِيمِ",
                    "مَالِكِ يَوْمِ الدِّينِ",
                    "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
                    "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
                    "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ"
            });

    public static final Surah[] SURAHS = {FATIHA};

    private QuranData() {
    }

    public static int totalVerses() {
        int total = 0;
        for (Surah surah : SURAHS) {
            total += surah.verseCount();
        }
        return total;
    }
}
