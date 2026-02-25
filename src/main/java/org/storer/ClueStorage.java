package org.storer;

import org.storer.meta.Clue;

import java.util.List;

public class ClueStorage {
    private static final Scraper scraper = new Scraper(new ScraperHelper());
    private static final Storer storer = new Storer();

    private static void storeClues(int season) {
        String seasonUrl = "https://www.j-archive.com/showseason.php?season=" + season;
        List<String> gameIds = scraper.scrapeSeason(seasonUrl);
        System.out.println(gameIds);
        gameIds.forEach(gameId -> {
            String gameUrl = "https://j-archive.com/showgame.php?game_id=" + gameId;
            List<Clue> clues = scraper.scrapeGame(gameUrl, Integer.parseInt(gameId));
            storer.storeClues(clues);
        });
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: ClueStorage <season>");
            System.exit(1);
        }
        try {
            storeClues(Integer.parseInt(args[0]));
        } catch (NumberFormatException e) {
            System.err.println("Error: season must be a number, got: " + args[0]);
            System.exit(1);
        }
    }
}
