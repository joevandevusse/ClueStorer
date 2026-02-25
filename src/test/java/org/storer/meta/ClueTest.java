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
    void testAccessors() {
        Clue clue = buildClue(false);
        assertEquals("SCIENCE", clue.category());
        assertEquals("J", clue.round());
        assertEquals(0, clue.categoryNumber());
        assertEquals("$200", clue.clueValue());
        assertEquals("This is the clue text", clue.question());
        assertEquals("What is the answer", clue.answer());
        assertFalse(clue.isDailyDouble());
        assertEquals(1234, clue.gameId());
        assertEquals("2024-10-29", clue.gameDate());
    }

    @Test
    void testIsDailyDouble() {
        assertTrue(buildClue(true).isDailyDouble());
        assertFalse(buildClue(false).isDailyDouble());
    }

    @Test
    void testToStringContainsKeyFields() {
        String str = buildClue(false).toString();
        assertTrue(str.contains("SCIENCE"));
        assertTrue(str.contains("This is the clue text"));
        assertTrue(str.contains("What is the answer"));
    }
}
