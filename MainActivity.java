
package org.example.quranteacher;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    private TextToSpeech tts;
    private boolean ttsReady = false;

    private final int backgroundColor = Color.rgb(250, 248, 241);
    private final int greenColor = Color.rgb(105, 139, 104);
    private final int textColor = Color.rgb(48, 57, 47);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        getWindow().getDecorView().setBackgroundColor(backgroundColor);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("ar"));
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        showHome();
    }

    private TextView makeText(String text, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(textColor);
        view.setGravity(Gravity.CENTER);
        view.setPadding(16, 16, 16, 16);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(19);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(greenColor));
        button.setPadding(18, 18, 18, 18);
        return button;
    }

    private LinearLayout createLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(24, 24, 24, 24);
        layout.setBackgroundColor(backgroundColor);
        return layout;
    }

    private void showHome() {
        LinearLayout layout = createLayout();

        TextView title = makeText("مُعَلِّمُ القُرْآن", 30, true);
        layout.addView(title);

        layout.addView(makeText(
                "مرحباً بكِ\nلنتعلّم القرآن معاً", 22, false));

        Button voiceButton = makeButton("🔊 اختبار الصوت");
        voiceButton.setOnClickListener(v -> speak(
                "السلام عليكم. مرحباً بك في معلم القرآن."));
        layout.addView(voiceButton, buttonParams());

        Button lessonButton = makeButton("📖 درس القرآن");
        lessonButton.setOnClickListener(v ->
                Toast.makeText(this,
                        "سيتم إضافة الدروس القرآنية لاحقاً",
                        Toast.LENGTH_LONG).show());
        layout.addView(lessonButton, buttonParams());

        Button progressButton = makeButton("📊 لوحة التقدّم");
        progressButton.setOnClickListener(v -> showDashboard());
        layout.addView(progressButton, buttonParams());

        layout.addView(makeText(
                "نسخة تجريبية — قيد التطوير", 14, false));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 12, 0, 12);
        return params;
    }

    private void showDashboard() {
        LinearLayout layout = createLayout();
        layout.addView(makeText("لوحة التقدّم", 28, true));
        layout.addView(makeText(
                "ستظهر هنا الدروس المكتملة والحفظ والتقدّم.",
                20, false));

        Button backButton = makeButton("العودة إلى الصفحة الرئيسية");
        backButton.setOnClickListener(v -> showHome());
        layout.addView(backButton, buttonParams());

        setContentView(layout);
    }

    private void speak(String message) {
        if (ttsReady && tts != null) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "quran_voice");
        } else {
            Toast.makeText(this,
                    "الصوت العربي غير متوفر حالياً على الهاتف",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
