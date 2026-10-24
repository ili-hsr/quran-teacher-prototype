
package org.example.quranteacher;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private final int bg = Color.rgb(250, 248, 241);
    private final int green = Color.rgb(78, 119, 83);
    private final int ink = Color.rgb(43, 51, 42);
    private static final int MIC_PERMISSION = 100;

    private SharedPreferences prefs;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String currentScreen = "home";

    private int verseIndex = 0;
    private int memoryIndex = 0;
    private boolean verseVisible = false;
    private int quizQuestion = 0;

    private final String[] verses = {
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
        "الرَّحْمَٰنِ الرَّحِيمِ",
        "مَالِكِ يَوْمِ الدِّينِ",
        "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ",
        "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
        "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ"
    };

    private final String[] verseNumbers = {
        "الفاتحة، الآية ١",
        "الفاتحة، الآية ٢",
        "الفاتحة، الآية ٣",
        "الفاتحة، الآية ٤",
        "الفاتحة، الآية ٥",
        "الفاتحة، الآية ٦",
        "الفاتحة، الآية ٧"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("quran_progress", MODE_PRIVATE);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("ar"));
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        showHome();
    }

    private void speak(String message) {
        if (ttsReady && tts != null) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "quran_voice");
        }
    }

    private LinearLayout makeLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(24, 24, 24, 24);
        layout.setBackgroundColor(bg);
        return layout;
    }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(ink);
        t.setGravity(Gravity.CENTER);
        t.setPadding(12, 14, 12, 14);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(18);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(green));
        b.setPadding(12, 14, 12, 14);
        b.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 8, 0, 8);
        b.setLayoutParams(p);
        return b;
    }

    private void show(LinearLayout layout) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(layout);
        setContentView(scroll);
    }

    private void title(LinearLayout layout, String value) {
        layout.addView(text(value, 27, true));
    }

    private void addVoiceButton(LinearLayout layout) {
        layout.addView(button("🎙 التحدث بالأوامر", this::startVoice));
    }

    private void addBack(LinearLayout layout) {
        addVoiceButton(layout);
        layout.addView(button("العودة إلى الرئيسية", this::showHome));
    }

    private void showHome() {
        currentScreen = "home";
        LinearLayout l = makeLayout();
        title(l, "مُعَلِّمُ القُرْآن");
        l.addView(text("مرحباً بكِ", 22, true));
        l.addView(text("تعلّمي القرآن خطوة بخطوة", 18, false));

        addVoiceButton(l);
        l.addView(button("الدروس", this::showLesson));
        l.addView(button("الحفظ والمراجعة", this::showMemory));
        l.addView(button("الاختبار", this::showQuiz));
        l.addView(button("التقدم", this::showProgress));

        l.addView(text("نسخة تجريبية", 15, false));
        show(l);
    }

    private void showLesson() {
        currentScreen = "lesson";
        LinearLayout l = makeLayout();
        title(l, "درس سورة الفاتحة");
        l.addView(text(verseNumbers[verseIndex], 20, true));
        l.addView(text(verses[verseIndex], 27, true));

        l.addView(button("الآية السابقة", () -> {
            if (verseIndex > 0) verseIndex--;
            showLesson();
        }));

        l.addView(button("الآية التالية", () -> {
            if (verseIndex < verses.length - 1) {
                verseIndex++;
                showLesson();
            } else {
                prefs.edit().putBoolean("fatiha_done", true).apply();
                speak("انتهى الدرس، تم حفظ التقدم");
                showLesson();
            }
        }));

        l.addView(button("إنهاء الدرس وحفظ التقدم", () -> {
            prefs.edit().putBoolean("fatiha_done", true).apply();
            speak("تم حفظ تقدم الدرس");
        }));

        addBack(l);
        show(l);
    }

    private void showMemory() {
        currentScreen = "memory";
        LinearLayout l = makeLayout();
        title(l, "الحفظ والمراجعة");
        l.addView(text(verseNumbers[memoryIndex], 20, true));

        if (verseVisible) {
            l.addView(text(verses[memoryIndex], 27, true));
        } else {
            l.addView(text("الآية مخفية، حاولي تذكّرها", 21, true));
        }

        l.addView(button(
            verseVisible ? "إخفاء الآية" : "إظهار الآية",
            () -> {
                verseVisible = !verseVisible;
                showMemory();
            }));

        l.addView(button("الآية التالية", () -> {
            memoryIndex = (memoryIndex + 1) % verses.length;
            verseVisible = false;
            showMemory();
        }));

        l.addView(button("لقد راجعت هذه الآية", () -> {
            int count = prefs.getInt("reviews", 0) + 1;
            prefs.edit().putInt("reviews", count).apply();
            speak("تم تسجيل المراجعة");
        }));

        addBack(l);
        show(l);
    }

    private void showQuiz() {
        currentScreen = "quiz";
        LinearLayout l = makeLayout();
        title(l, "اختبار القرآن");

        if (quizQuestion == 0) {
            l.addView(text(
                "ما اسم السورة التي تبدأ بالحمد لله رب العالمين؟",
                22, true));
            l.addView(button("سورة الفاتحة", () -> answerQuiz(true)));
            l.addView(button("سورة الإخلاص", () -> answerQuiz(false)));
        } else {
            l.addView(text(
                "انتهى الاختبار. عدد الإجابات الصحيحة: "
                + prefs.getInt("quiz_correct", 0),
                21, true));
            l.addView(button("إعادة الاختبار", () -> {
                quizQuestion = 0;
                showQuiz();
            }));
        }

        addBack(l);
        show(l);
    }

    private void answerQuiz(boolean correct) {
        if (correct) {
            int score = prefs.getInt("quiz_correct", 0) + 1;
            prefs.edit().putInt("quiz_correct", score).apply();
            speak("إجابة صحيحة");
        } else {
            speak("ليست الإجابة الصحيحة، حاولي مرة أخرى");
        }
        quizQuestion = 1;
        showQuiz();
    }

    private void showProgress() {
        currentScreen = "progress";
        LinearLayout l = makeLayout();
        title(l, "التقدم");

        boolean done = prefs.getBoolean("fatiha_done", false);
        int reviews = prefs.getInt("reviews", 0);
        int score = prefs.getInt("quiz_correct", 0);

        l.addView(text(
            "درس الفاتحة: " + (done ? "مكتمل" : "لم يكتمل"),
            20, true));
        l.addView(text("عدد المراجعات: " + reviews, 20, false));
        l.addView(text("الإجابات الصحيحة: " + score, 20, false));

        addBack(l);
        show(l);
    }

    private void startVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO},
                MIC_PERMISSION);
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("التعرف على الصوت غير متوفر على هذا الجهاز");
            Toast.makeText(this,
                "Le service de reconnaissance vocale n'est pas disponible.",
                Toast.LENGTH_LONG).show();
            return;
        }

        try {
            if (recognizer != null) recognizer.destroy();
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);

            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    speak("تفضلي، قولي الأمر");
                }

                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}

                @Override
                public void onError(int error) {
                    speak("لم أفهم الأمر، حاولي مرة أخرى");
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches =
                        results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION);

                    if (matches != null && !matches.isEmpty()) {
                        handleCommand(matches.get(0));
                    } else {
                        speak("لم أسمع الأمر بوضوح");
                    }
                }

                @Override public void onPartialResults(Bundle results) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);

            recognizer.startListening(intent);

        } catch (Exception e) {
            speak("تعذر تشغيل الميكروفون");
        }
    }

    private String normalizeArabic(String input) {
        return input
            .replaceAll("[\\u064B-\\u065F\\u0670]", "")
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ى", "ي")
            .trim();
    }

    private void handleCommand(String rawCommand) {
        String c = normalizeArabic(rawCommand);

        if (c.contains("الدروس") || c.contains("درس")) {
            speak("فتح الدروس");
            showLesson();
        } else if (c.contains("الحفظ") || c.contains("المراجعة")) {
            speak("فتح الحفظ والمراجعة");
            showMemory();
        } else if (c.contains("الاختبار") || c.contains("اختبار")) {
            speak("فتح الاختبار");
            quizQuestion = 0;
            showQuiz();
        } else if (c.contains("التقدم") || c.contains("التقدم")) {
            speak("فتح التقدم");
            showProgress();
        } else if (c.contains("الرئيسية") || c.contains("العودة")) {
            speak("العودة إلى الرئيسية");
            showHome();
        } else if (c.contains("التالية") || c.contains("التالي")) {
            if ("lesson".equals(currentScreen)) {
                if (verseIndex < verses.length - 1) verseIndex++;
                showLesson();
                speak(verseNumbers[verseIndex]);
            } else if ("memory".equals(currentScreen)) {
                memoryIndex = (memoryIndex + 1) % verses.length;
                verseVisible = false;
                showMemory();
                speak(verseNumbers[memoryIndex]);
            } else {
                speak("هذا الأمر متاح داخل الدرس أو المراجعة");
            }
        } else if (c.contains("السابقة") || c.contains("السابق")) {
            if ("lesson".equals(currentScreen) && verseIndex > 0) {
                verseIndex--;
                showLesson();
                speak(verseNumbers[verseIndex]);
            } else {
                speak("لا توجد آية سابقة هنا");
            }
        } else if (c.contains("اظهر") || c.contains("اكشف")) {
            if ("memory".equals(currentScreen)) {
                verseVisible = true;
                showMemory();
                speak("تم إظهار الآية");
            } else {
                speak("افتحي الحفظ أولاً");
            }
        } else if (c.contains("اخف")) {
            if ("memory".equals(currentScreen)) {
                verseVisible = false;
                showMemory();
                speak("تم إخفاء الآية");
            } else {
                speak("افتحي الحفظ أولاً");
            }
        } else if (c.contains("مساعدة") || c.contains("الاوامر")) {
            speak("قولي: افتح الدروس، افتح الحفظ، افتح الاختبار، افتح التقدم، الآية التالية، الآية السابقة، أو العودة إلى الرئيسية");
        } else {
            speak("لم أفهم الأمر. قولي: مساعدة، لسماع الأوامر المتاحة");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MIC_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoice();
        } else if (requestCode == MIC_PERMISSION) {
            speak("يجب السماح باستخدام الميكروفون");
        }
    }

    @Override
    protected void onDestroy() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }
}
