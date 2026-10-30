package org.example.quranteacher;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Point unique pour le style ET l'accessibilité.
 *
 * Style « verre » : boutons en verre dépoli (dégradé translucide, liseré blanc,
 * ombre douce) posés sur le fond aurore. Sur Android 9 il n'y a pas de vrai
 * flou d'arrière-plan (RenderEffect n'existe qu'à partir d'Android 12) : le
 * rendu est obtenu par transparence, liseré et ombre, ce qui reste très proche
 * à l'œil et ne coûte rien en performance sur un téléphone de 2 Go.
 *
 * Accessibilité maintenue : texte jamais sous 20sp, boutons de 72dp de haut,
 * pleine largeur, libellé + contentDescription, contrastes forts.
 */
public final class Ui {

    public static final int INK = 0xFF1E2B22;
    public static final int INK_SOFT = 0xFF546458;
    public static final int DEEP_GREEN = 0xFF2C4A36;
    public static final int WHITE = 0xFFFFFFFF;

    private Ui() {
    }

    public static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    // --- Conteneurs --------------------------------------------------------

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 18), dp(c, 26), dp(c, 18), dp(c, 30));
        l.setBackgroundColor(0x00000000);
        return l;
    }

    public static ScrollView wrap(LinearLayout column) {
        ScrollView s = new ScrollView(column.getContext());
        s.setFillViewport(true);
        s.setBackgroundColor(0x00000000);
        s.addView(column);
        return s;
    }

    /** Carte en verre : pour le texte coranique et les blocs d'information. */
    public static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        l.setBackground(glassPanel(c, 0xB3FFFFFF, 0x4DFFFFFF, 0xB3FFFFFF, 22));
        l.setElevation(dp(c, 4));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(c, 8), 0, dp(c, 8));
        l.setLayoutParams(p);
        return l;
    }

    // --- Textes ------------------------------------------------------------

    public static TextView heading(Context c, CharSequence value) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(30);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setTextColor(DEEP_GREEN);
        t.setGravity(Gravity.CENTER);
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        t.setAccessibilityHeading(true);
        t.setShadowLayer(dp(c, 1), 0, dp(c, 1), 0x66FFFFFF);
        t.setPadding(0, dp(c, 10), 0, dp(c, 14));
        return t;
    }

    public static TextView subheading(Context c, CharSequence value) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(22);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setTextColor(DEEP_GREEN);
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

    public static TextView meta(Context c, CharSequence value) {
        TextView t = body(c, value);
        t.setTextSize(16);
        t.setTextColor(INK_SOFT);
        return t;
    }

    /** Texte coranique : toujours RTL, même dans une interface latine. */
    public static TextView verse(Context c, CharSequence value) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextSize(28);
        t.setTextColor(INK);
        t.setGravity(Gravity.CENTER);
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);
        t.setLineSpacing(dp(c, 8), 1.2f);
        t.setPadding(dp(c, 6), dp(c, 16), dp(c, 6), dp(c, 16));
        return t;
    }

    // --- Boutons -----------------------------------------------------------

    public static Button button(Context c, CharSequence label, View.OnClickListener click) {
        Button b = baseButton(c, label, click);
        b.setTextColor(DEEP_GREEN);
        b.setBackground(glassState(c, 0xABFFFFFF, 0x47FFFFFF, 0xC2FFFFFF,
                0xC4FFFFFF, 0x66FFFFFF, 0xD9FFFFFF));
        b.setElevation(dp(c, 6));
        return b;
    }

    /** Action principale : verre vert profond, texte blanc. */
    public static Button primaryButton(Context c, CharSequence label, View.OnClickListener click) {
        Button b = baseButton(c, label, click);
        b.setTextColor(WHITE);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        b.setBackground(glassState(c, 0xF2466E52, 0xF22B4B36, 0x4DFFFFFF,
                0xF23E6448, 0xF2264430, 0x66FFFFFF));
        b.setElevation(dp(c, 10));
        return b;
    }

    private static Button baseButton(Context c, CharSequence label, View.OnClickListener click) {
        Button b = new Button(c);
        b.setText(label);
        b.setTextSize(21);
        b.setAllCaps(false);
        b.setContentDescription(label);
        b.setMinHeight(dp(c, 72));
        b.setMinimumHeight(dp(c, 72));
        b.setPadding(dp(c, 14), dp(c, 14), dp(c, 14), dp(c, 14));
        b.setOnClickListener(click);
        b.setStateListAnimator(null);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dp(c, 7), 0, dp(c, 7));
        b.setLayoutParams(p);
        return b;
    }

    private static StateListDrawable glassState(Context c, int top, int bottom, int stroke,
                                                int topPressed, int bottomPressed,
                                                int strokePressed) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed},
                glassPanel(c, topPressed, bottomPressed, strokePressed, 20));
        states.addState(new int[]{},
                glassPanel(c, top, bottom, stroke, 20));
        return states;
    }

    private static Drawable glassPanel(Context c, int top, int bottom, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        g.setCornerRadius(dp(c, radiusDp));
        g.setStroke(dp(c, 1), stroke);
        return g;
    }

    /** Libellé de section dans les paramètres. */
    public static void section(Context c, LinearLayout parent, CharSequence label) {
        TextView t = subheading(c, label);
        t.setPadding(0, dp(c, 20), 0, dp(c, 4));
        t.setAccessibilityHeading(true);
        parent.addView(t);
    }
}
