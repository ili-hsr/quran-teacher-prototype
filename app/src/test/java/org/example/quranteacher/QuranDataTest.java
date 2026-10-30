package org.example.quranteacher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Vérifie l'analyse du fichier de texte coranique. */
public class QuranDataTest {

    private static final String FIXTURE =
            "{\"surahs\":["
            + "{\"n\":1,\"ar\":\"سُورَةُ ٱلْفَاتِحَةِ\",\"la\":\"Al-Faatiha\",\"sl\":\"fatiha\","
            + "\"ay\":[\"بِسْمِ ٱللَّهِ\",\"ٱلْحَمْدُ لِلَّهِ\"]},"
            + "{\"n\":2,\"ar\":\"سُورَةُ البَقَرَةِ\",\"la\":\"Al-Baqara\",\"sl\":\"albaqara\","
            + "\"ay\":[\"الٓمٓ\"]}"
            + "]}";

    @Test
    public void analyseDeuxSourates() {
        QuranData.Surah[] surahs = QuranData.parse(FIXTURE);
        assertEquals(2, surahs.length);
        assertEquals(1, surahs[0].number);
        assertEquals("001", surahs[0].key);
        assertEquals("fatiha", surahs[0].alias);
        assertEquals(2, surahs[0].verseCount());
        assertEquals("001_2", surahs[0].verseKey(1));
        assertEquals("2 · Al-Baqara", surahs[1].latinLabel());
        assertEquals("002", surahs[1].key);
    }

    @Test
    public void leTexteEstConserveTelQuel() {
        QuranData.Surah[] surahs = QuranData.parse(FIXTURE);
        assertEquals("بِسْمِ ٱللَّهِ", surahs[0].verses[0]);
        assertEquals("الٓمٓ", surahs[1].verses[0]);
    }

    @Test
    public void unFichierIllisibleNePlantePas() {
        assertEquals(0, QuranData.parse("").length);
        assertEquals(0, QuranData.parse("{}").length);
        assertEquals(0, QuranData.parse("pas du json").length);
        assertTrue(QuranData.totalVerses() >= 0);
    }
}
