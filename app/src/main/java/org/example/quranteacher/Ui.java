package org.example.quranteacher;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Point unique pour le style visuel ET l'accessibilité.
 *
 * Règles appliquées ici :
 * - texte jamais plus petit que 20sp ;
 * - boutons pleine largeur, hauteur minimale 72dp, libellé en texte ET en
 *   contentDescription (annonce fiable par TalkBack) ;
 * - titres marqués comme titres (setAccessibilityHeading) pour la navigation
 *   par titres dans TalkBack ;
 * - texte arabe toujours rendu de droite à gauche, même dans une interface
 *   française (TEXT_DIRECTION_FIRST_STRONG_RTL).
 */
public final class Ui {

    public static final int CREAM = 0xFFFAF8F1;
    public static final int BEIGE = 0xFFEDE6D8;
    public static final int SAGE = 0xFF8A9B83;
    public static final int DEEP_GREEN = 0xFF355B42;
    public static final int INK = 0xFF252B25;

    private Ui() {
    }

    public static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 20), dp(c, 28), dp(c, 20), dp(c, 32));
        l.setBackgroundColor(CREAM);
        return l;
    }

    public static ScrollView wrap(LinearLayout column) {
        ScrollView s = new ScrollView(column.getContext());
        s.setFillViewport(true);
        s.setBackgroundColor(CREAM);
        s.setContentDescription(null);
        s.addView(column);
        return s;
    }

    public static TextView heading(Context c, CharSequence value) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(30);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setTextColor(DEEP_GREEN);
        t.setGravity(Gravity.CENTER);
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        t.setAccessibilityHeading(true);
        t.setPadding(0, dp(c, 8), 0, dp(c, 12));
        return t;
    }

    public static TextView subheading(Context c, CharSequence value) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(22);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setTextColor(INK);
        t.setGravity(Gravity.CENTER);
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        t.setPadding(0, dp(c, 6), 0, dp(c, 6));
        return t;
    }

    public static TextView body(Context c, CharSequence value) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(20);
        t.setTextColor(INK);
        t.setGravity(Gravity.CENTER);
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        t.setPadding(0, dp(c, 4), 0, dp(c, 4));
        return t;
    }

    /** Texte coranique : toujours RTL, même si la phrase commence par un chiffre. */
    public static TextView verse(Context c, CharSequence value) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(28);
        t.setTextColor(INK);
        t.setGravity(Gravity.CENTER);
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        t.setLineSpacing(dp(c, 8), 1.2f);
        t.setPadding(dp(c, 8), dp(c, 20), dp(c, 8), dp(c, 20));
        return t;
    }

    public static Button button(Context c, CharSequence label, View.OnClickListener click) {
        Button b = new Button(c);
        b.setText(label);
        b.setTextSize(21);
        b.setAllCaps(false);
        b.setTextColor(0xFFFFFFFF);
        b.setBackgroundTintList(ColorStateList.valueOf(DEEP_GREEN));
        b.setContentDescription(label);
        b.setMinHeight(dp(c, 72));
        b.setMinimumHeight(dp(c, 72));
        b.setPadding(dp(c, 16), dp(c, 16), dp(c, 16), dp(c, 16));
        b.setOnClickListener(click);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(c, 6), 0, dp(c, 6));
        b.setLayoutParams(p);
        return b;
    }

    /** Petit espace avant une nouvelle section (paramètres). */
    public static void section(Context c, LinearLayout parent, CharSequence label) {
        TextView t = subheading(c, label);
        t.setPadding(0, dp(c, 20), 0, dp(c, 4));
        t.setAccessibilityHeading(true);
        parent.addView(t);
    }
}
