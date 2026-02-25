# ClueStorer

ClueStorer is a Java-based application designed to scrape Jeopardy! game data from public web archives and store the clues, categories, and metadata into a local database. It also includes a bulk loader for pre-existing TSV datasets.

## Features

*   **Game Scraping**: Connects to game URLs to extract categories, clues, and correct responses.
*   **Round Handling**: Intelligently parses clues from the Jeopardy!, Double Jeopardy!, and Final Jeopardy! rounds.
*   **Special Clue Detection**: Identifies "Daily Double" clues and calculates their nominal board value (e.g., $800) rather than the wagered amount.
*   **Season Traversal**: Includes functionality to scrape season pages and aggregate game links.
*   **TSV Bulk Loader**: Loads pre-existing clue datasets in TSV format into the same database.
*   **Data Model**: Captures granular details including:
    *   Game ID and Date
    *   Category names
    *   Clue values and text
    *   Correct responses

## Tech Stack

*   **Java 21**: Core application logic.
*   **Jsoup**: Used for connecting to web pages and parsing HTML DOM elements.
*   **HikariCP**: Connection pooling for efficient database access.
*   **Apache Commons CSV**: TSV file parsing for the bulk loader.
*   **SLF4J + Logback**: Logging.
*   **PostgreSQL**: Local database used for persistence via JDBC.

## Database Setup

The application requires a PostgreSQL database running locally. Ensure you have a table named `clues_java` with the following schema:

```sql
CREATE TABLE clues_java (
    id VARCHAR PRIMARY KEY,
    category VARCHAR,
    round VARCHAR,
    category_number INT,
    clue_value VARCHAR,
    question TEXT,
    answer TEXT,
    is_daily_double BOOLEAN,
    game_id INT,
    game_date VARCHAR,
    date_added VARCHAR
);
```

Set the following environment variables before running either entry point:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/your_db
export DB_USER=your_user
export DB_PASSWORD=your_password
```

## Usage

### Scraper — `ClueStorage`

Scrapes a full season from J! Archive and stores clues. Accepts a Season ID as a command-line argument.

```bash
# Scrape Season 42
java -cp target/ClueStorer-1.0-SNAPSHOT.jar org.storer.ClueStorage 42
```

### TSV Bulk Loader — `TsvLoader`

Loads a pre-existing TSV dataset into the database. The seasons 1–41 dataset used by this project is sourced from [jwolle1/jeopardy_clue_dataset v41](https://github.com/jwolle1/jeopardy_clue_dataset/releases/tag/v41). Expects the standard column layout: `round, clue_value, daily_double_value, category, comments, answer, question, air_date, notes`.

```bash
# Load the seasons 1-41 combined dataset
java -cp target/ClueStorer-1.0-SNAPSHOT.jar org.storer.loader.TsvLoader \
  jeopardy_dataset_seasons_1-41/combined_season1-41.tsv
```

The loader is **idempotent** — re-running it on the same file will not create duplicate rows.

> **Note on `game_id`:** The scraper uses J! Archive's internal game IDs (e.g., `9036`). The TSV loader has no equivalent, so it derives `game_id` from the air date as a plain integer (e.g., `1984-09-10` → `19840910`). These two ID spaces do not overlap, but they are not linked to each other.

> **Note on `category_number`:** The TSV dataset does not include column/category position data. Rows loaded by `TsvLoader` will have `category_number = -1`.

## Disclaimer

This project is for educational purposes and personal data archiving. Please ensure you comply with the Terms of Service of any website you scrape.
