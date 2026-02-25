package org.storer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.storer.meta.Clue;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScraperTest {

    private List<Clue> clues;

    @BeforeEach
    void setUp() throws Exception {
        Scraper scraper = new Scraper(new ScraperHelper());
        File file = new File("src/test/java/testFiles/game9036.html");
        Document doc = Jsoup.parse(file, "UTF-8");
        clues = scraper.scrapeGame(doc, 9036);
    }

    @Test
    void testCluesAreReturned() {
        assertFalse(clues.isEmpty());
    }

    @Test
    void testTotalClueCount() {
        // This game has 60 revealed clues (1 was unrevealed before time expired)
        assertEquals(60, clues.size());
    }

    @Test
    void testGameDateParsedFromTitle() {
        assertEquals("2024-10-29", clues.get(0).gameDate());
    }

    @Test
    void testGameIdStoredOnClues() {
        assertEquals(9036, clues.get(0).gameId());
    }

    @Test
    void testAllRoundsPresent() {
        assertTrue(clues.stream().anyMatch(c -> c.round().equals("J")));
        assertTrue(clues.stream().anyMatch(c -> c.round().equals("DJ")));
        assertTrue(clues.stream().anyMatch(c -> c.round().equals("FJ")));
    }

    @Test
    void testJeopardyClue() {
        // clue_J_1_1: ALASKAN CITIES $200
        Clue clue = clues.stream()
                .filter(c -> c.round().equals("J")
                        && c.category().equals("ALASKAN CITIES")
                        && c.clueValue().equals("$200"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected J clue not found"));

        assertEquals("Nome in the west of the state is the main city on Norton Sound, an arm of this sea",
                clue.question());
        assertEquals("the Bering Sea", clue.answer());
        assertFalse(clue.isDailyDouble());
    }

    @Test
    void testDoubleJeopardyClue() {
        // clue_DJ_1_1: HISTORIC WOMEN $400
        Clue clue = clues.stream()
                .filter(c -> c.round().equals("DJ")
                        && c.category().equals("HISTORIC WOMEN")
                        && c.clueValue().equals("$400"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected DJ clue not found"));

        assertEquals("fission", clue.answer());
        assertFalse(clue.isDailyDouble());
    }

    @Test
    void testDailyDoubleCount() {
        // Standard game has exactly 3 Daily Doubles (1 in J!, 2 in DJ!)
        long ddCount = clues.stream().filter(Clue::isDailyDouble).count();
        assertEquals(3, ddCount);
    }

    @Test
    void testDailyDoubleNominalValue() {
        // clue_J_2_4: row 4 in J! round -> nominal value $800
        Clue dd = clues.stream()
                .filter(c -> c.isDailyDouble() && c.round().equals("J"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected J! Daily Double not found"));

        assertEquals("$800", dd.clueValue());
    }

    @Test
    void testFinalJeopardyClue() {
        Clue fj = clues.stream()
                .filter(c -> c.round().equals("FJ"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected FJ clue not found"));

        assertEquals("NEWS FROM THE STORK", fj.category());
        assertEquals("Antarctica", fj.answer());
        assertFalse(fj.isDailyDouble());
    }
}
