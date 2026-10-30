package org.example.quranteacher;

import android.content.Context;
import android.content.res.Configuration;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Reconnaît une commande vocale quel que soit le moteur de reconnaissance.
 *
 * Astuce utile sur le terrain : les mots-clés des QUATRE langues livrées sont
 * acceptés en même temps. Une utilisatrice qui parle français peut donc dire
 * « suivant » alors que la reconnaissance est réglée en arabe, et inversement.
 * Aucun Internet n'est nécessaire : tout est local.
 */
public final class CommandKeywords {

    public static final String HOME = "home";
    public static final String LESSONS = "lessons";
    public static final String MEMORY = "memory";
    public static final String EXAM = "exam";
    public static final String PROGRESS = "progress";
    public static final String SETTINGS = "settings";
    public static final String NEXT = "next";
    public static final String PREV = "prev";
    public static final String SHOW = "show";
    public static final String HIDE = "hide";
    public static final String REPEAT = "repeat";
    public static final String HELP = "help";
    public static final String STOP = "stop";
    public static final String YES = "yes";
    public static final String NO = "no";

    private static final String[] LOCALES = {"fr", "ar", "en", "nl"};

    private static final String[] ACTIONS = {
            HOME, LESSONS, MEMORY, EXAM, PROGRESS, SETTINGS,
            NEXT, PREV, SHOW, HIDE, REPEAT, HELP, STOP, YES, NO
    };

    private static final int[] ARRAYS = {
            R.array.cmd_home, R.array.cmd_lessons, R.array.cmd_memory,
            R.array.cmd_exam, R.array.cmd_progress, R.array.cmd_settings,
            R.array.cmd_next, R.array.cmd_prev, R.array.cmd_show,
            R.array.cmd_hide, R.array.cmd_repeat, R.array.cmd_help,
            R.array.cmd_stop, R.array.cmd_yes, R.array.cmd_no
    };

    private CommandKeywords() {
    }

    /** @return l'action reconnue, ou null si la phrase ne contient aucune commande. */
    public static String match(Context base, String heard) {
        if (heard == null) {
            return null;
        }
        String said = normalize(heard);
        if (said.isEmpty()) {
            return null;
        }
        for (String tag : LOCALES) {
            Context localized = localized(base, tag);
            for (int i = 0; i < ARRAYS.length; i++) {
                String[] keywords;
                try {
                    keywords = localized.getResources().getStringArray(ARRAYS[i]);
                } catch (Exception e) {
                    continue;
                }
                for (String keyword : keywords) {
                    if (containsKeyword(said, normalize(keyword))) {
                        return ACTIONS[i];
                    }
                }
            }
        }
        return null;
    }

    private static Context localized(Context base, String tag) {
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(Locale.forLanguageTag(tag));
        return base.createConfigurationContext(config);
    }

    /** Un mot-clé de 3 lettres ou moins doit apparaître comme mot entier. */
    private static boolean containsKeyword(String said, String keyword) {
        if (keyword.isEmpty()) {
            return false;
        }
        if (keyword.length() <= 3) {
            for (String word : said.split(" ")) {
                if (word.equals(keyword)) {
                    return true;
                }
            }
            return false;
        }
        return said.contains(keyword);
    }

    /**
     * Minuscules, sans accents latins, sans harakat arabes, alef/ya/ta marbuta
     * unifiés : « Répète » et « رَبِّ » reviennent à une forme comparable.
     */
    static String normalize(String input) {
        String text = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]", "")
                .replace('\u0623', '\u0627')
                .replace('\u0625', '\u0627')
                .replace('\u0622', '\u0627')
                .replace('\u0649', '\u064A')
                .replace('\u0629', '\u0647')
                .replaceAll("[\\p{Punct}«»؟،]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return text;
    }
}
