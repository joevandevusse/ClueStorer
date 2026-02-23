package org.storer;

import org.storer.meta.Clue;
import java.util.List;

public class ClueStorage {
    private static final Scraper scraper = new Scraper(new ScraperHelper());
    private static final Storer storer = new Storer();

    private static void storeClues(int season) {
        List<String> gameIds = getSeasonGames(season);
        System.out.println(gameIds);
        gameIds.forEach(gameId -> {
            List<Clue> clues = getGameClues(Integer.parseInt(gameId));
            persistClues(clues);
        });
        //List<Clue> clues = getGameClues(9036);
        //persistClues(clues);
    }

    private static List<String> getSeasonGames(int season) {
        String url = "https://www.j-archive.com/showseason.php?season=" + season;
        return scraper.scrapeSeason(url);
    }

    private static List<Clue> getGameClues(int gameNumber) {
        // Use BeautifulSoup type API to get game (Jsoup)
        String url = "http://j-archive.com/showgame.php?game_id=" + gameNumber;
        return scraper.scrapeGame(url, gameNumber);
    }

    private static void persistClues(List<Clue> clues) {
        storer.storeClues(clues);
    }

    public static void main(String[] args) {
        storeClues(Integer.parseInt(args[0]));
    }
}
