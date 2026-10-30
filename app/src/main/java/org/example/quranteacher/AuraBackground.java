package org.example.quranteacher;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Fond « aurore » : un dégradé crème + quelques halos verts et beige.
 *
 * C'est ce qui donne de la matière derrière les boutons en verre. Dessiné à la
 * main plutôt qu'en XML : sur Android 9 les dégradés radiaux XML n'acceptent
 * pas de centre, et il faut pouvoir placer les halos.
 */
public final class AuraBackground extends Drawable {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int[] base = {0xFFFBF9F3, 0xFFF2EFE5, 0xFFE7EEE3};

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        paint.setShader(new LinearGradient(
                bounds.left, bounds.top, bounds.right, bounds.bottom,
                base, null, Shader.TileMode.CLAMP));
        canvas.drawRect(bounds, paint);

        halo(canvas, bounds, 0.06f, 0.03f, 0.95f, 0xBFD3B8, 190);
        halo(canvas, bounds, 1.02f, 0.18f, 0.80f, 0xF1DFC4, 165);
        halo(canvas, bounds, 0.18f, 1.04f, 1.00f, 0xCBDCC6, 200);
        halo(canvas, bounds, 0.96f, 0.85f, 0.62f, 0xE9D9EA, 110);
    }

    private void halo(Canvas canvas, Rect b, float cx, float cy, float rel,
                      int rgb, int alpha) {
        float radius = Math.max(b.width(), b.height()) * rel;
        float x = b.left + b.width() * cx;
        float y = b.top + b.height() * cy;
        int solid = Color.argb(alpha, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
        int clear = Color.argb(0, Color.red(rgb), Color.green(rgb), Color.blue(rgb));
        paint.setShader(new RadialGradient(x, y, radius, solid, clear, Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, radius, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.OPAQUE;
    }
}
