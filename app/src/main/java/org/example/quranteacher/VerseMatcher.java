package org.example.quranteacher;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Compare ce que la reconnaissance a entendu avec le texte du verset.
 *
 * La comparaison porte sur les mots, pas sur les voyelles courtes : la
 * synthèse d'un moteur vocal ne restitue jamais les diacritiques du texte
 * uthmani. On tolère donc une petite différence par mot, et on annonce
 * honnêtement un nombre de mots reconnus, jamais une note de tajwid.
 */
public final class VerseMatcher {

    public static final class Score {
        public final int matched;
        public final int expected;
        public final int percent;

        Score(int matched, int expected) {
            this.matched = matched;
            this.expected = expected;
            this.percent = expected == 0 ? 0 : Math.round(matched * 100f / expected);
        }

        public boolean accepted() {
            return percent >= 60;
        }

        public boolean strong() {
            return percent >= 88;
        }
    }

    private VerseMatcher() {
    }

    public static Score compare(String verse, String spoken) {
        String[] wanted = words(verse);
        String[] heard = words(spoken);
        int matched = 0;
        int cursor = 0;
        for (String word : wanted) {
            for (int i = cursor; i < heard.length; i++) {
                if (similar(word, heard[i])) {
                    matched++;
                    cursor = i + 1;
                    break;
                }
            }
        }
        return new Score(matched, wanted.length);
    }

    static String[] words(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return new String[0];
        for (String raw : text.split("\\s+")) {
            String word = normalize(raw);
            if (!word.isEmpty()) out.add(word);
        }
        return out.toArray(new String[0]);
    }

    /** Retire les diacritiques, unifie les formes de lettres et la ponctuation. */
    static String normalize(String raw) {
        if (raw == null) return "";
        String text = Normalizer.normalize(raw, Normalizer.Form.NFD);
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '\u0610' && c <= '\u061A') continue;
            if (c >= '\u064B' && c <= '\u065F') continue;
            if (c >= '\u06D6' && c <= '\u06ED') continue;
            if (c == '\u0670' || c == '\u0640') continue;
            if (Character.getType(c) == Character.NON_SPACING_MARK) continue;
            switch (c) {
                case '\u0622': case '\u0623': case '\u0625': case '\u0671': c = '\u0627'; break;
                case '\u0649': c = '\u064A'; break;
                case '\u0629': c = '\u0647'; break;
                case '\u0624': c = '\u0648'; break;
                case '\u0626': c = '\u064A'; break;
                default: break;
            }
            if (c >= '\u0621' && c <= '\u064A') out.append(c);
            else if (c >= 'a' && c <= 'z') out.append(c);
            else if (c >= 'A' && c <= 'Z') out.append((char) (c + 32));
            else if (c >= '0' && c <= '9') out.append(c);
            else if (c == ' ' || c == '\t') out.append(' ');
        }
        return out.toString().trim();
    }

    static boolean similar(String a, String b) {
        if (a.equals(b)) return true;
        int longest = Math.max(a.length(), b.length());
        if (longest <= 2) return false;
        int allowed = longest >= 6 ? 2 : 1;
        return distance(a, b, allowed) <= allowed;
    }

    private static int distance(String a, String b, int limit) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            int best = current[0];
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
                if (current[j] < best) best = current[j];
            }
            if (best > limit) return limit + 1;
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
