package org.storer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
            System.out.println("Categories: " + getCategories(doc));

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
                //System.out.println(childUrl);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return gameIds;
    }
}
