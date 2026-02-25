package org.storer.meta;

public record Clue(
        String category,
        String round,
        int categoryNumber,
        String clueValue,
        String question,
        String answer,
        boolean isDailyDouble,
        int gameId,
        String gameDate
) {}
