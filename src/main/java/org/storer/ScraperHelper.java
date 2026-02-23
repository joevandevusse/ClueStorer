package org.storer;

public class ScraperHelper {
    protected String getDailyDoubleValue(String clueId) {
        int row = Integer.parseInt(clueId.split("_")[3]);
        int value = 0;
        switch (row) {
            case 1:
                value = 200;
                break;
            case 2:
                value = 400;
                break;
            case 3:
                value = 600;
                break;
            case 4:
                value = 800;
                break;
            case 5:
                value = 1000;
                break;
            default:
                break;
        }

        if (clueId.contains("DJ")) {
            value *= 2;
        }

        return "$" + value;
    }
}
