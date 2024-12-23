package org.storer;

//import com.google.inject.Guice;
//import com.google.inject.Injector;
//import jakarta.inject.Inject;

import java.util.List;

public class ClueStorage {
    // Need to find an equivalent BeautifulSoup package for Java

    //@Inject
    private static final Scraper scraper = new Scraper();

    private static void storeClues(int season) {
        List<String> gameIds = getSeasonGames(season);
        System.out.println(gameIds);
        //gameIds.forEach(gameId -> getGame(Integer.parseInt(gameId)));
        getGame(9036);
    }

    private static List<String> getSeasonGames(int season) {
        String url = "https://www.j-archive.com/showseason.php?season=" + season;
        return scraper.scrapeSeason(url);
    }

    private static void getGame(int gameNumber) {
        // Use BeautifulSoup type API to get game
        String url = "http://j-archive.com/showgame.php?game_id=" + gameNumber;
        scraper.scrapeGame(url, gameNumber);
    }

    public static void main(String[] args) {
        storeClues(Integer.parseInt(args[0]));
    }
}
