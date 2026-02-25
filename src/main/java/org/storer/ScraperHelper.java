package org.storer;

public class ScraperHelper {
    protected String getDailyDoubleValue(String clueId) {
        int row = Integer.parseInt(clueId.split("_")[3]);
        int value = row * 200;

        if (clueId.contains("DJ")) {
            value *= 2;
        }

        return "$" + value;
    }
}
