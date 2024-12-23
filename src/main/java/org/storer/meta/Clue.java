package org.storer.meta;

public class Clue {
    private final String category;
    private final String round;
    private final int categoryNumber;
    private final String value;
    private final String question;
    private final String answer;

    public Clue(
            String category, String round, int categoryNumber,
            String value, String question, String answer) {
        this.category = category;
        this.round = round;
        this.categoryNumber = categoryNumber;
        this.value = value;
        this.question = question;
        this.answer = answer;
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

    @Override
    public String toString() {
        return "Clue{" +
                "category='" + category + '\'' +
                ", question='" + question + '\'' +
                ", round='" + question + '\'' +
                ", round='" + question + '\'' +
                ", answer='" + answer + '\'' +
                '}';
    }
}
