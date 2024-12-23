package org.storer.meta;

public class Clue {
    private final String category;
    private final String round;
    private final int categoryNumber;
    private final String value;
    private final String question;
    private final String answer;
    private final boolean isDailyDouble;
    private final int gameId;
    private final String gameDate;

    public Clue(
            String category,
            String round,
            int categoryNumber,
            String value,
            String question,
            String answer,
            boolean isDailyDouble,
            int gameId,
            String gameDate) {
        this.category = category;
        this.round = round;
        this.categoryNumber = categoryNumber;
        this.value = value;
        this.question = question;
        this.answer = answer;
        this.isDailyDouble = isDailyDouble;
        this.gameId = gameId;
        this.gameDate = gameDate;
    }

    public String getCategory() {
        return category;
    }

    public String getRound() {
        return round;
    }

    public int getCategoryNumber() {
        return categoryNumber;
    }

    public String getValue() {
        return value;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean getIsDailyDouble() {
        return isDailyDouble;
    }

    public int getGameId() {
        return gameId;
    }

    public String getGameDate() {
        return gameDate;
    }

    @Override
    public String toString() {
        return "Clue{" +
                "category='" + category + '\'' +
                ", question='" + question + '\'' +
                ", round='" + round + '\'' +
                ", categoryNumber='" + categoryNumber + '\'' +
                ", answer='" + answer + '\'' +
                ", isDailyDouble='" + isDailyDouble + '\'' +
                ", gameId='" + gameId + '\'' +
                ", gameDate='" + gameDate + '\'' +
                '}';
    }
}
