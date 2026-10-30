package org.example.quranteacher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Tests JVM de la logique pure : noms de fichiers audio et clés de versets. */
public class RecitationStoreTest {

    @Test
    public void accepteLesExtensionsAudioConnues() {
        assertEquals("001_1", RecitationStore.baseName("001_1.mp3"));
        assertEquals("002_255", RecitationStore.baseName("002_255.ogg"));
        assertEquals("fatiha_3", RecitationStore.baseName("fatiha_3.m4a"));
        assertEquals("001_4", RecitationStore.baseName("001_4.wav"));
    }

    @Test
    public void ignoreLaCasseEtLesEspaces() {
        assertEquals("001_1", RecitationStore.baseName("001_1.MP3"));
        assertEquals("fatiha_1", RecitationStore.baseName("Fatiha 1.mp3"));
    }

    @Test
    public void rejetteLesFichiersNonAudio() {
        assertNull(RecitationStore.baseName("lisezmoi.txt"));
        assertNull(RecitationStore.baseName("quran_text.json"));
        assertNull(RecitationStore.baseName("001_1"));
        assertNull(RecitationStore.baseName(null));
        assertNull(RecitationStore.baseName(".mp3"));
    }

    @Test
    public void cleDeVersetCoherente() {
        QuranData.Surah fatiha = new QuranData.Surah(1, "fatiha", "الفاتحة", "Al-Faatiha",
                new String[]{"a", "b", "c", "d", "e", "f", "g"});
        assertEquals("001", fatiha.key);
        assertEquals("001_1", fatiha.verseKey(0));
        assertEquals("001_7", fatiha.verseKey(6));
        assertEquals(7, fatiha.verseCount());
        assertEquals("1 · Al-Faatiha", fatiha.latinLabel());
    }
}
