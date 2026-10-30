package org.example.quranteacher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * Préférences globales : trois langues indépendantes (interface, professeur,
 * reconnaissance vocale) + réglages de la voix.
 *
 * Fichier volontairement sans dépendance AndroidX : l'application doit rester
 * légère pour un téléphone de 2 Go de RAM sous Android 9.
 */
public final class AppPrefs {

    private static final String FILE = "quran_prefs_v1";

    public static final String KEY_UI_LANG = "ui_lang";
    public static final String KEY_TEACHER_LANG = "teacher_lang";
    public static final String KEY_TEACHER_ON = "teacher_on";
    public static final String KEY_TEACHER_RATE = "teacher_rate";
    public static final String KEY_RECOG_LANG = "recog_lang";

    /** L'interface est en français par défaut : l'utilisatrice ne comprend pas l'arabe d'interface. */
    public static final String DEFAULT_UI_LANG = "fr";
    public static final String DEFAULT_TEACHER_LANG = "ar";
    public static final String DEFAULT_RECOG_LANG = "ar-SA";

    private AppPrefs() {
    }

    public static SharedPreferences get(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String uiLang(Context c) {
        return get(c).getString(KEY_UI_LANG, DEFAULT_UI_LANG);
    }

    public static void setUiLang(Context c, String tag) {
        get(c).edit().putString(KEY_UI_LANG, tag).commit();
    }

    public static String teacherLang(Context c) {
        return get(c).getString(KEY_TEACHER_LANG, DEFAULT_TEACHER_LANG);
    }

    public static void setTeacherLang(Context c, String tag) {
        get(c).edit().putString(KEY_TEACHER_LANG, tag).commit();
    }

    public static boolean isTeacherOn(Context c) {
        return get(c).getBoolean(KEY_TEACHER_ON, true);
    }

    public static void setTeacherOn(Context c, boolean on) {
        get(c).edit().putBoolean(KEY_TEACHER_ON, on).commit();
    }

    /** 0 = lente, 1 = normale, 2 = rapide. */
    public static int teacherRateIndex(Context c) {
        return get(c).getInt(KEY_TEACHER_RATE, 1);
    }

    public static void setTeacherRateIndex(Context c, int index) {
        get(c).edit().putInt(KEY_TEACHER_RATE, index).commit();
    }

    public static float teacherRate(Context c) {
        switch (teacherRateIndex(c)) {
            case 0:
                return 0.8f;
            case 2:
                return 1.15f;
            default:
                return 1.0f;
        }
    }

    public static String recogLang(Context c) {
        return get(c).getString(KEY_RECOG_LANG, DEFAULT_RECOG_LANG);
    }

    public static void setRecogLang(Context c, String tag) {
        get(c).edit().putString(KEY_RECOG_LANG, tag).commit();
    }

    /** Applique la langue d'interface à une Activity (appelé depuis attachBaseContext). */
    public static Context wrap(Context base) {
        return withLocale(base, Locale.forLanguageTag(uiLang(base)));
    }

    /** Contexte configuré dans la langue du professeur, pour ses phrases. */
    public static Context teacherContext(Context base) {
        return withLocale(base, Locale.forLanguageTag(teacherLang(base)));
    }

    private static Context withLocale(Context base, Locale locale) {
        Configuration config = new Configuration(base.getResources().getConfiguration());
        config.setLocale(locale);
        return base.createConfigurationContext(config);
    }
}
