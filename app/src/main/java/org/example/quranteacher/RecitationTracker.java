package org.example.quranteacher;

/**
 * Suit la récitation mot par mot pendant que l'utilisatrice parle.
 *
 * À chaque hypothèse du moteur, on compare les mots entendus aux mots du
 * verset. Dès qu'un mot ne correspond pas, on renvoie l'erreur avec sa
 * position pour que le professeur corrige tout de suite.
 */
public final class RecitationTracker {

    public static final class Step {
        public final boolean finished;
        public final int cursor;
        public final int errorIndex;
        public final boolean omission;

        Step(boolean finished, int cursor, int errorIndex, boolean omission) {
            this.finished = finished;
            this.cursor = cursor;
            this.errorIndex = errorIndex;
            this.omission = omission;
        }

        public boolean hasError() {
            return errorIndex >= 0;
        }
    }

    private final String[] expected;
    private int cursor;
    private int processed;

    public RecitationTracker(String verse) {
        this.expected = VerseMatcher.words(verse);
    }

    public int length() {
        return expected.length;
    }

    public int cursor() {
        return cursor;
    }

    public Step feed(String hypothesis) {
        String[] heard = VerseMatcher.words(hypothesis);
        while (processed < heard.length) {
            String word = heard[processed++];
            if (cursor >= expected.length) {
                break;
            }
            if (VerseMatcher.similar(expected[cursor], word)) {
                cursor++;
                continue;
            }
            if (cursor + 1 < expected.length && VerseMatcher.similar(expected[cursor + 1], word)) {
                int index = cursor;
                cursor += 2;
                return new Step(false, cursor, index, true);
            }
            return new Step(false, cursor, cursor, false);
        }
        return new Step(cursor >= expected.length, cursor, -1, false);
    }

    /** Reprise après une correction : on réécoute à partir de ce mot. */
    public void resumeFrom(int index) {
        cursor = Math.max(0, Math.min(index, expected.length - 1));
        processed = 0;
    }

    public void endUtterance() {
        processed = 0;
    }

    public void reset() {
        cursor = 0;
        processed = 0;
    }

    public String gap(int index) {
        int from = Math.max(0, index);
        int to = Math.min(expected.length, index + 3);
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(expected[i]);
        }
        return sb.toString();
    }
}
