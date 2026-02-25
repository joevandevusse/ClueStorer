package org.storer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.storer.meta.Clue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Scraper {

    private final ScraperHelper scraperHelper;
    private boolean logging = false;

    public Scraper(ScraperHelper scraperHelper) {
        this.scraperHelper = scraperHelper;
    }

    List<Clue> scrapeGame(Document doc, int gameNumber) throws Exception {
        String title = doc.title();
        if (logging) System.out.println("Title: " + title);

        String gameDate = title.split("aired")[1].trim();
        System.out.println("Scraping Game: " + gameNumber + " for date: " + gameDate);

        List<String> categories = getCategories(doc);
        if (logging) System.out.println("Categories: " + categories);

        List<Clue> clues = getClues(doc, categories, gameNumber, gameDate);
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
        Elements categoryElements = doc.select(".category_name");
        for (Element element : categoryElements) {
            categories.add(element.text());
        }
        return categories;
    }

    private List<Clue> getClues(Document doc, List<String> categories,
                                int gameId, String gameDate) {
        List<Clue> clues = new ArrayList<>();

        Elements clueElements = doc.select("td.clue");
        if (logging) System.out.println("Clue Element Count: " + clueElements.size());

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
                clueObj = constructFjClue(categories, clueText, correctResponse, gameId, gameDate);
            } else {
                clueObj = constructClue(categories, clueValue, clueText,
                        clueId, correctResponse, isDailyDouble, gameId, gameDate);
            }
            clues.add(clueObj);
            if (logging) printClues(clueObj, clueId);
        }
        return clues;
    }

    private Clue constructClue(
            List<String> categories, String clueValue, String clueText,
            String clueId, String correctResponse, boolean isDailyDouble,
            int gameId, String gameDate) {
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
                                 String correctResponse, int gameId, String gameDate) {
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
            Document doc = Jsoup.connect(url).get();
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String childUrl = link.absUrl("href");
                if (childUrl.contains("game_id")) {
                    for (String param : new URI(childUrl).getQuery().split("&")) {
                        if (param.startsWith("game_id=")) {
                            gameIds.add(param.substring("game_id=".length()));
                            break;
                        }
                    }
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
