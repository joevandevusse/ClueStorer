package org.storer.meta;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClueTest {

    private Clue buildClue(boolean isDailyDouble) {
        return new Clue(
                "SCIENCE",
                "J",
                0,
                "$200",
                "This is the clue text",
                "What is the answer",
                isDailyDouble,
                1234,
                "2024-10-29"
        );
    }

    @Test
    void testGetters() {
        Clue clue = buildClue(false);
        assertEquals("SCIENCE", clue.getCategory());
        assertEquals("J", clue.getRound());
        assertEquals(0, clue.getCategoryNumber());
        assertEquals("$200", clue.getClueValue());
        assertEquals("This is the clue text", clue.getQuestion());
        assertEquals("What is the answer", clue.getAnswer());
        assertFalse(clue.getIsDailyDouble());
        assertEquals(1234, clue.getGameId());
        assertEquals("2024-10-29", clue.getGameDate());
    }

    @Test
    void testIsDailyDouble() {
        assertTrue(buildClue(true).getIsDailyDouble());
        assertFalse(buildClue(false).getIsDailyDouble());
    }

    @Test
    void testToStringContainsKeyFields() {
        String str = buildClue(false).toString();
        assertTrue(str.contains("SCIENCE"));
        assertTrue(str.contains("This is the clue text"));
        assertTrue(str.contains("What is the answer"));
    }
}
