# ClueStorer

ClueStorer is a Java-based application designed to scrape Jeopardy! game data from public web archives and store the clues, categories, and metadata into a local database.

## Features

*   **Game Scraping**: Connects to game URLs to extract categories, clues, and correct responses.
*   **Round Handling**: Intelligently parses clues from the Jeopardy!, Double Jeopardy!, and Final Jeopardy! rounds.
*   **Special Clue Detection**: Identifies "Daily Double" clues and calculates their nominal board value (e.g., $800) rather than the wagered amount.
*   **Season Traversal**: Includes functionality to scrape season pages and aggregate game links.
*   **Data Model**: Captures granular details including:
    *   Game ID and Date
    *   Category names
    *   Clue values and text
    *   Correct responses

## Tech Stack

*   **Java**: Core application logic.
*   **Jsoup**: Used for connecting to web pages and parsing HTML DOM elements.
*   **Google Guice**: Used for dependency injection (e.g., injecting `ScraperHelper`).
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

## Usage

The application entry point is `ClueStorage.java`. It accepts a Season ID as a command-line argument.

```bash
# Run the scraper for Season 38
java org.storer.ClueStorage 38
```

## Disclaimer

This project is for educational purposes and personal data archiving. Please ensure you comply with the Terms of Service of any website you scrape.