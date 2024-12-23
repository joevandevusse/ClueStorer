package org.storer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.storer.meta.Clue;

import java.util.ArrayList;
import java.util.List;

public class Scraper {
    protected void scrapeGame(String url) {
        try {
            // Connect to the website and get the HTML document
            Document doc = Jsoup.connect(url).get();

            // Extract the title of the page
            // TODO: Use this to get the date
            String title = doc.title();
            System.out.println("Title: " + title);

            // Get categories
            List<String> categories = getCategories(doc);
            System.out.println("Categories: " + categories);

            // Get clues
            List<Clue> clues = getClues(doc, categories);
            //System.out.println("Clues: " + clues);
            System.out.println("Clue Count: " + clues.size());
            System.out.println("----------------------------------------");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private List<String> getCategories(Document doc) {
        List<String> categories = new ArrayList<>();
        // Select all elements with class "category_name"
        Elements categoryElements = doc.select(".category_name");

        // Iterate and print each element's content
        for (Element element : categoryElements) {
            categories.add(element.text());
            //System.out.println("Element Text: " + element.text());
            //System.out.println("Element HTML: " + element.html());
        }
        return categories;
    }

    private List<Clue> getClues(Document doc, List<String> categories) {
        List<Clue> clues = new ArrayList<>();

        // Select all td elements with class "clue"
        Elements clueElements = doc.select("td.clue");
        System.out.println("Clue Element Count: " + clueElements.size());

        // Iterate through each clue element
        for (Element clue : clueElements) {
            Element valueElement = clue.selectFirst("td.clue_value");
            String clueValue = valueElement != null ? valueElement.text() : "$0";
            Element textElement = clue.selectFirst("td.clue_text:not([style*='display:none'])");
            String clueText = textElement != null ? textElement.text() : "Default Clue Text";
            String clueId = textElement != null ? textElement.attr("id") : "N/A";
            Element responseElement = clue.selectFirst("td[style*='display:none;'] em.correct_response");
            String correctResponse = responseElement != null ? responseElement.text() : "Default Correct Response";

            if (valueElement != null) {
                Clue clueObj = constructClue(categories, clueValue, clueText, clueId, correctResponse);
                clues.add(clueObj);

                System.out.println("Clue ID: " + clueId);
                System.out.println("Category: " + clueObj.getCategory());
                System.out.println("Round: " + clueObj.getRound());
                System.out.println("Category Number: " + clueObj.getCategoryNumber());
                System.out.println("Clue Value: " + clueObj.getValue());
                System.out.println("Clue Text: " + clueObj.getQuestion());
                System.out.println("Correct Response: " + clueObj.getAnswer());
                System.out.println("----------------------------------------");
            } else {
                System.out.println("CLUE NOT READ");
            }
        }

        return clues;
    }

    private Clue constructClue(
            List<String> categories, String clueValue, String clueText,
            String clueId, String correctResponse) {
        System.out.println("Clue ID: " + clueId);
        String[] clueIdParts = clueId.split("_");
        String round = clueIdParts[1];
        int categoryNumber = -1;
        if (round.equals("J")) {
            categoryNumber = Integer.parseInt(clueIdParts[2]) - 1;
        } else if (round.equals("DJ")) {
            // women 6, drama 7
            categoryNumber = Integer.parseInt(clueIdParts[2]) + 6 - 1;
        }
        String category = categories.get(categoryNumber);
        return new Clue(category, round, categoryNumber, clueValue, clueText, correctResponse);
    }

    protected List<String> scrapeSeason(String url) {
        List<String> gameIds = new ArrayList<>();
        try {
            // Connect to the website and get the HTML document
            Document doc = Jsoup.connect(url).get();

            // Extract the title of the page
            String title = doc.title();
            System.out.println("Title: " + title);

            // Select elements using CSS selectors
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String childUrl = link.absUrl("href");
                if (childUrl.contains("game_id")) {
                    gameIds.add(childUrl.split("=")[1]);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return gameIds;
    }
}
