package org.example.quranteacher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Tests JVM de la logique pure : reconnaissance des noms de fichiers audio et
 * clés de progression. Aucun émulateur nécessaire, ils tournent dans la CI.
 */
public class RecitationStoreTest {

    @Test
    public void accepteLesExtensionsAudioConnues() {
        assertEquals("fatiha_1", RecitationStore.baseName("fatiha_1.mp3"));
        assertEquals("fatiha_12", RecitationStore.baseName("fatiha_12.ogg"));
        assertEquals("fatiha_3", RecitationStore.baseName("fatiha_3.m4a"));
        assertEquals("fatiha_4", RecitationStore.baseName("fatiha_4.wav"));
    }

    @Test
    public void ignoreLaCasseEtLesEspaces() {
        assertEquals("fatiha_1", RecitationStore.baseName("FATIHA_1.MP3"));
        assertEquals("fatiha_1", RecitationStore.baseName("Fatiha 1.mp3"));
    }

    @Test
    public void rejetteLesFichiersNonAudio() {
        assertNull(RecitationStore.baseName("lisezmoi.txt"));
        assertNull(RecitationStore.baseName("fatiha_1"));
        assertNull(RecitationStore.baseName(null));
        assertNull(RecitationStore.baseName(".mp3"));
    }

    @Test
    public void cleDeVersetCoherente() {
        assertEquals("fatiha_1", QuranData.SURAHS[0].verseKey(0));
        assertEquals("fatiha_7", QuranData.SURAHS[0].verseKey(6));
        assertEquals(7, QuranData.SURAHS[0].verseCount());
    }

    @Test
    public void lesSeptVersetsDeFatihaSontPresents() {
        QuranData.Surah fatiha = QuranData.SURAHS[0];
        assertEquals(7, fatiha.verses.length);
        for (String verse : fatiha.verses) {
            assertEquals(false, verse == null || verse.isEmpty());
        }
        assertEquals(7, QuranData.totalVerses());
    }
}
