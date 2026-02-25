package org.storer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.storer.meta.Clue;

import java.util.ArrayList;
//import java.util.Arrays;
import java.util.List;

public class Scraper {

    private int gameId = 0;
    private String gameDate = "1970-0-0";
    //private static final ScraperHelper scraperHelper = new ScraperHelper();
    private final ScraperHelper scraperHelper;
    private boolean logging = false;

    public Scraper(ScraperHelper scraperHelper) {
        this.scraperHelper = scraperHelper;
    }

    List<Clue> scrapeGame(Document doc, int gameNumber) throws Exception {
        gameId = gameNumber;
        String title = doc.title();
        if (logging) System.out.println("Title: " + title);

        gameDate = title.split("aired")[1].trim();
        System.out.println("Scraping Game: " + gameId + " for date: " + gameDate);

        List<String> categories = getCategories(doc);
        if (logging) System.out.println("Categories: " + categories);

        List<Clue> clues = getClues(doc, categories);
        if (logging) System.out.println("Clue Count: " + clues.size());
        if (logging) System.out.println("----------------------------------------");
        return clues;
    }

    protected List<Clue> scrapeGame(String url, int gameNumber) {
        try {
            Document doc = Jsoup.connect(url).get();
            return scrapeGame(doc, gameNumber);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<String> getCategories(Document doc) {
        List<String> categories = new ArrayList<>();
        // Select all elements with class "category_name"
        Elements categoryElements = doc.select(".category_name");

        // Iterate and print each element's content
        for (Element element : categoryElements) {
            categories.add(element.text());
        }
        return categories;
    }

    private List<Clue> getClues(Document doc, List<String> categories) {
        List<Clue> clues = new ArrayList<>();

        // Select all td elements with class "clue"
        Elements clueElements = doc.select("td.clue");
        if (logging) System.out.println("Clue Element Count: " + clueElements.size());

        // Iterate through each clue element
        for (Element clue : clueElements) {
            Element textElement = clue.selectFirst("td.clue_text");
            if (textElement == null) {
                if (logging) System.out.println("CLUE NOT READ");
                continue;
            }

            String clueText = textElement.text();
            String clueId = textElement.attr("id");
            boolean isDailyDouble = false;
            Element valueElement = clue.selectFirst("td.clue_value");
            String clueValue;

            // Check if the clue is a daily double (FJ has no value element but is not a DD)
            if (valueElement == null) {
                if (clueId.contains("FJ")) {
                    clueValue = "$0";
                } else {
                    clueValue = scraperHelper.getDailyDoubleValue(clueId);
                    isDailyDouble = true;
                }
            } else {
                clueValue = valueElement.text();
            }

            Element responseElement = clue.selectFirst("td em.correct_response");
            String correctResponse = responseElement != null ?
                    responseElement.text() : "Default Correct Response";

            Clue clueObj;
            if (clueId.contains("FJ")) {
                clueObj = constructFjClue(categories, clueText, correctResponse);
            } else {
                clueObj = constructClue(categories, clueValue, clueText,
                        clueId, correctResponse, isDailyDouble);
            }
            clues.add(clueObj);
            if (logging) printClues(clueObj, clueId);
        }
        return clues;
    }

    private Clue constructClue(
            List<String> categories, String clueValue, String clueText,
            String clueId, String correctResponse, boolean isDailyDouble) {
        String[] clueIdParts = clueId.split("_");
        String round = clueIdParts[1];
        int categoryNumber = -1;
        if (round.equals("J")) {
            categoryNumber = Integer.parseInt(clueIdParts[2]) - 1;
        } else if (round.equals("DJ")) {
            categoryNumber = Integer.parseInt(clueIdParts[2]) + 6 - 1;
        }
        String category = categories.get(categoryNumber);
        return new Clue(category, round, categoryNumber, clueValue, clueText,
                correctResponse, isDailyDouble, gameId, gameDate);
    }

    private Clue constructFjClue(List<String> categories, String clueText,
                                 String correctResponse) {
        return new Clue(categories.get(categories.size() - 1),
                "FJ",
                categories.size() - 1,
                "$0",
                clueText,
                correctResponse,
                false,
                gameId,
                gameDate);
    }

    protected List<String> scrapeSeason(String url) {
        List<String> gameIds = new ArrayList<>();
        try {
            // Connect to the website and get the HTML document
            Document doc = Jsoup.connect(url).get();

            // Select elements using CSS selectors
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String childUrl = link.absUrl("href");
                if (childUrl.contains("game_id")) {
                    gameIds.add(childUrl.split("=")[1]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameIds;
    }

    private void printClues(Clue clue, String clueId) {
        System.out.println("Clue ID: " + clueId);
        System.out.println("Category: " + clue.getCategory());
        System.out.println("Round: " + clue.getRound());
        System.out.println("Category Number: " + clue.getCategoryNumber());
        System.out.println("Clue Value: " + clue.getClueValue());
        System.out.println("Clue Text: " + clue.getQuestion());
        System.out.println("Correct Response: " + clue.getAnswer());
        System.out.println("Is Daily Double: " + clue.getIsDailyDouble());
        System.out.println("Game ID: " + clue.getGameId());
        System.out.println("Game Date: " + clue.getGameDate());
        System.out.println("----------------------------------------");
    }
}
