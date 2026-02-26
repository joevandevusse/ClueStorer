package org.storer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.storer.meta.Clue;

import java.util.List;

public class ClueStorage {

  private static final Logger log = LoggerFactory.getLogger(ClueStorage.class);
  private static final Scraper scraper = new Scraper(new ScraperHelper());

  private static void storeClues(int season, boolean dryRun) {
    String seasonUrl = "https://www.j-archive.com/showseason.php?season=" + season;
    List<String> gameIds = scraper.scrapeSeason(seasonUrl);
    System.out.println(gameIds);

    Storer storer = dryRun ? null : new Storer();

    gameIds.forEach(gameId -> {
      String gameUrl = "https://j-archive.com/showgame.php?game_id=" + gameId;
      List<Clue> clues = scraper.scrapeGame(gameUrl, Integer.parseInt(gameId));
      String date = clues.isEmpty() ? "unknown" : clues.getFirst().gameDate();
      if (dryRun) {
        log.info("[DRY RUN] Game {} ({}): {} clues would be stored", gameId, date, clues.size());
      } else {
        log.info("Storing game {} ({}): {} clues", gameId, date, clues.size());
        storer.storeClues(clues);
      }
    });
  }

  public static void main(String[] args) {
    boolean dryRun = args.length > 0 && args[0].equals("--dry-run");
    int seasonArgIndex = dryRun ? 1 : 0;

    if (seasonArgIndex >= args.length) {
      System.err.println("Usage: ClueStorage [--dry-run] <season>");
      System.exit(1);
    }

    String seasonArg = args[seasonArgIndex];
    try {
      storeClues(Integer.parseInt(seasonArg), dryRun);
    } catch (NumberFormatException e) {
      System.err.println("Error: season must be a number, got: " + seasonArg);
      System.exit(1);
    }
  }
}
