# ClueStorer — Improvement Backlog

Items are grouped by file and ordered by priority within each group. Check them off as they are completed.

---

## High Priority

- [x] **`Scraper.java` — Fragile game ID parsing in `scrapeSeason`**
  `childUrl.split("=")[1]` breaks if `game_id` is not the only query parameter (e.g., `?game_id=123&foo=bar` yields `123&foo`). Replace with proper URL parsing via `URI` or `URLDecoder`.

- [x] **`ScrapeTest.java` — Test file does not compile and has no real tests**
  `url` on line 8 is undefined. `testGetCategories()` is empty. Add JUnit to `pom.xml` and write real tests using the local `game9036.html` fixture.

- [x] **`Storer.java` — New DB connection opened per game**
  `storeClues` is called once per game in a loop, and each call opens a fresh `DriverManager.getConnection`. Replace with a connection pool (e.g., HikariCP).

---

## Medium Priority

- [x] **`ClueStorage.java` / `pom.xml` — Guice is wired but never used**
  `@Inject` is on `Scraper`'s constructor but objects are instantiated manually. Either wire up a real `Injector` (and un-comment `StorageModule`) or remove Guice entirely and simplify.

- [x] **`Scraper.java` — Mutable `gameId` / `gameDate` instance state**
  These fields are set during `scrapeGame()`, making `Scraper` stateful and non-reentrant. Pass them as parameters or return a wrapper object alongside the clue list.

- [ ] **`ScraperHelper.java` — Switch statement is just multiplication**
  Rows 1–5 map to 200, 400, 600, 800, 1000. The entire switch can be replaced with `int value = row * 200;`.

- [x] **`Storer.java` — Inconsistent and lossy error handling**
  The inner `catch` in the `forEach` lambda uses `Arrays.toString(e.getStackTrace())` and silently swallows errors. Use consistent logging and consider failing fast or collecting errors rather than continuing a broken batch.

- [x] **`Storer.java` — No validation of environment variables**
  If `DB_URL`, `DB_USER`, or `DB_PASSWORD` are unset, the app throws a cryptic NPE. Add an explicit startup check with a clear error message.

- [ ] **`ClueStorage.java` — No input validation on CLI args**
  Missing or non-numeric `args[0]` throws an unhelpful `ArrayIndexOutOfBoundsException` or `NumberFormatException`. Add a guard with a usage message.

- [ ] **`ClueStorage.java` — Inconsistent URL schemes**
  `getSeasonGames` uses `https://` but `getGameClues` uses `http://` (line 28). Standardize to `https://`.

---

## Low Priority

- [ ] **`Clue.java` — Boolean getter naming**
  `getIsDailyDouble()` violates Java conventions. Rename to `isDailyDouble()`.

- [x] **`pom.xml` — Upgrade Java target from 8 to 21**
  Java 8 is 12 years old. Upgrading to Java 21 (current LTS) unlocks records, text blocks, pattern matching, and more.

- [ ] **`Clue.java` — Convert to a `record` (after Java upgrade)**
  `Clue` is purely immutable data. A `record` eliminates the constructor, all getters, and `toString` boilerplate.

- [x] **`Storer.java` — `DateTimeFormatter` should be `static final`**
  It is immutable and thread-safe; no need to create a new instance per `Storer` object.

- [x] **`Storer.java` — Magic string table name**
  `"clues_java"` should be a `private static final String` constant.

- [ ] **`Scraper.java` — `logging` flag**
  The hardcoded `private boolean logging = false` field should be replaced with a proper logging framework (SLF4J + Logback).

- [ ] **`Scraper.java` — Wasted initial list assignment**
  `List<Clue> clues = new ArrayList<>()` on line 29 is immediately overwritten by `clues = getClues(...)`. Remove the first assignment.

- [ ] **`Scraper.java` — Unused `ddValueElement` variable**
  Declared in the daily double branch but the line that uses it is commented out. Remove it.

- [ ] **`Scraper.java` — Fragile title date parsing**
  `title.split("aired")[1]` throws `ArrayIndexOutOfBoundsException` if the title doesn't contain "aired". Add a defensive check.

- [ ] **`ClueStorage.java` — Remove thin wrapper methods**
  `getSeasonGames`, `getGameClues`, and `persistClues` just delegate with identical arguments and add no value. Call scraper/storer methods directly.

- [ ] **`ClueStorage.java` — Remove leftover debug comment**
  Lines 17–18 contain a commented-out single-game invocation left over from debugging.

- [x] **`StorageModule.java` — Remove or implement**
  The file is entirely commented out. Either implement it (if keeping Guice) or delete it.

- [ ] **`pom.xml` — Fix placeholder `groupId`**
  `org.example` is the Maven default. Update to match the actual package (`org.storer`).

- [ ] **`pom.xml` — Add executable JAR configuration**
  Add a `maven-jar-plugin` entry with a `mainClass` manifest so the jar is directly runnable.
