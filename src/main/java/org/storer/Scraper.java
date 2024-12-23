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
            String title = doc.title();
            System.out.println("Title: " + title);

            // Select elements using CSS selectors
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                System.out.println("Link: " + link.attr("href"));
                System.out.println("Text: " + link.text());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
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
                System.out.println(childUrl);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return gameIds;
    }
}
