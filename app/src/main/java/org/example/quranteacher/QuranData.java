package org.example.quranteacher;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;

/**
 * Texte coranique complet : 114 sourates, 6236 versets, texte uthmani.
 *
 * La source est vérifiée une fois pour toutes (comptage des versets de chaque
 * sourate comparé à la table canonique, aucun verset vide, aucun caractère
 * parasite). Le texte n'est JAMAIS modifié ni translittéré : il est affiché
 * tel quel, et jamais prononcé par la synthèse vocale.
 */
public final class QuranData {

    private static final String ASSET = "quran_text.json";

    public static final class Surah {

        public final int number;
        /** Clé stable à trois chiffres, ex. « 002 » : sert aussi aux fichiers audio. */
        public final String key;
        /** Ancien nom latin simple, ex. « fatiha » : compatibilité des fichiers. */
        public final String alias;
        public final String nameAr;
        public final String nameLatin;
        public final String[] verses;

        Surah(int number, String alias, String nameAr, String nameLatin, String[] verses) {
            this.number = number;
            this.key = String.format(Locale.US, "%03d", number);
            this.alias = alias == null ? "" : alias;
            this.nameAr = nameAr == null ? "" : nameAr;
            this.nameLatin = nameLatin == null ? "" : nameLatin;
            this.verses = verses;
        }

        public int verseCount() {
            return verses.length;
        }

        public String verseKey(int index) {
            return key + "_" + (index + 1);
        }

        /** « 2 · Al-Baqara » : lisible dans toutes les langues d'interface. */
        public String latinLabel() {
            return number + " · " + nameLatin;
        }
    }

    private static Surah[] surahs;

    private QuranData() {
    }

    /** Charge le texte une seule fois par processus. */
    public static void ensureLoaded(Context context) {
        if (surahs != null) {
            return;
        }
        surahs = parse(readAsset(context.getApplicationContext()));
    }

    public static Surah[] surahs() {
        return surahs == null ? new Surah[0] : surahs;
    }

    public static Surah surah(int index) {
        Surah[] all = surahs();
        if (index < 0 || index >= all.length) {
            return null;
        }
        return all[index];
    }

    public static int totalVerses() {
        int total = 0;
        for (Surah surah : surahs()) {
            total += surah.verseCount();
        }
        return total;
    }

    /**
     * Retrouve un verset à partir de sa clé (« 002_255 »).
     *
     * @return {index de sourate, index de verset} ou null.
     */
    public static int[] locate(String verseKey) {
        if (verseKey == null) {
            return null;
        }
        int cut = verseKey.lastIndexOf('_');
        if (cut <= 0 || cut == verseKey.length() - 1) {
            return null;
        }
        String s = verseKey.substring(0, cut);
        int verse;
        try {
            verse = Integer.parseInt(verseKey.substring(cut + 1)) - 1;
        } catch (NumberFormatException e) {
            return null;
        }
        Surah[] all = surahs();
        for (int i = 0; i < all.length; i++) {
            if (all[i].key.equals(s) || all[i].alias.equals(s)) {
                if (verse < 0 || verse >= all[i].verseCount()) {
                    return null;
                }
                return new int[]{i, verse};
            }
        }
        return null;
    }

    // --- Lecture -----------------------------------------------------------

    private static String readAsset(Context context) {
        try (InputStream in = context.getAssets().open(ASSET)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1536 * 1024);
            byte[] buffer = new byte[16384];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    static Surah[] parse(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray list = root.optJSONArray("surahs");
            if (list == null) {
                return new Surah[0];
            }
            Surah[] out = new Surah[list.length()];
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.getJSONObject(i);
                JSONArray ay = item.optJSONArray("ay");
                String[] verses = new String[ay == null ? 0 : ay.length()];
                for (int v = 0; v < verses.length; v++) {
                    verses[v] = ay.optString(v, "").replace("\ufeff", "");
                }
                out[i] = new Surah(
                        item.optInt("n", i + 1),
                        item.optString("sl", ""),
                        item.optString("ar", ""),
                        item.optString("la", ""),
                        verses);
            }
            return out;
        } catch (Exception e) {
            return new Surah[0];
        }
    }
}
