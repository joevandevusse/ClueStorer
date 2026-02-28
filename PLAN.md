# **Design Document: "Moneyball" Trivia Engine**

**Project Lead:** Joe

**Primary Goal:** Build a data-driven trivia training platform to identify knowledge gaps, optimize study time via statistical probability, and improve recall speed for competitive trivia (and secure future wins for Joey & the Pussycats). **Existing Foundation:** To be integrated with or modeled after the existing `jeopardy-react` repository.

[Roger Craig's Video](https://vimeo.com/29001512?&login=true&__cf_chl_tk=haYCnESp6mL_Eo8Wj8ok.2L0CkNkp7FyZN7TKnUpOPo-1771993727-1.0.1.1-qmop0CRZPXWAiI0oUtcuuo2QoJGGr7A3l_.K41F2HJw)

## **1\. System Architecture & Tech Stack**

* **Frontend:** React.js (leveraging existing `jeopardy-react` patterns)
* **Backend/Database:** ~~Google Firebase/Firestore~~ **PostgreSQL** (local, via JDBC/psycopg2). Firebase was deprioritized in favour of a local-first approach — the data model is highly relational and PostgreSQL is a better fit. Firebase can be revisited if multi-user or real-time sync is needed later.
* **Data Processing (NLP):** Python (scikit-learn for exploratory clustering; Anthropic Claude API for production-quality category normalization)
* **Data Visualization:** D3.js or Visx (for the interactive bubble chart)

## **2\. Project Phases & Milestones**

### **Phase 1: Data Acquisition & ETL Pipeline** ✅

* **Objective:** Secure the raw data and push it to a scalable database.
* **Actual implementation (differs from original plan):**
  * Built a Java 21 application (`ClueStorer`) with two data sources rather than a Python ETL script:
    * **`TsvLoader`** — bulk-loads the `jwolle1/jeopardy_clue_dataset` (529k+ clues) from TSV into PostgreSQL. Idempotent via `WHERE NOT EXISTS`. Supports `--dry-run`.
    * **`ClueStorage`** — scrapes live seasons from J! Archive via Jsoup. Supports `--dry-run`.
  * No data cleaning script was needed — the TSV dataset was clean enough to load directly with field mapping.
  * Database is local PostgreSQL (not Firebase). Schema tracked in `migrations.sql`.
  * Connection pooling via HikariCP; logging via SLF4J + Logback.

### **Phase 2: NLP & Text Clustering (The Brain)** ✅

* **Objective:** Categorize the raw clues into distinct, semantic study topics rather than relying on the show's arbitrary categories.
* **Actual implementation (differs from original plan):**
  * **Exploratory pass** (`cluster_clues.py`): Ran TF-IDF vectorization + K-Means (50 clusters) as a proof of concept. Results were directionally useful for geographic topics but produced semantically mixed clusters for name-heavy topics (e.g. Babe Ruth and Ruth Bader Ginsburg in the same bucket; Harry Truman and Harry Potter together). Concluded this approach was insufficient for the bubble chart.
  * **Production pass** (`normalize_categories.py`): Switched strategy entirely. Rather than clustering clue text, we normalize the 56,328 unique Jeopardy category names directly using the Claude API (Haiku model):
    * **Pass 1** — Sample 500 random category names, ask Claude to derive a canonical taxonomy of 74 study topics. Taxonomy saved to `taxonomy.json` for consistency across re-runs.
    * **Pass 2** — Batch-classify all 56,328 categories (30 per API call) against the fixed taxonomy using a positional array format to avoid JSON key escaping issues. Retry logic on failures.
    * Results stored in `category_mappings` table (`jeopardy_category → canonical_topic`), separate from `clues_java` to keep raw and derived data isolated.
  * Final result: 56,328 categories mapped, 0 batch failures, consistent taxonomy across the full dataset.
  * Cluster assignments are joined at query time: `SELECT ... FROM clues_java JOIN category_mappings ON category`.

### **Phase 3: The Game Interface (The Web App)**

* **Objective:** Build the UI to study the categorized data and enforce the 3-second recall rule.
* **Tasks:**
  * Create a "Study Mode" component in React that queries for specific canonical topics.
  * Implement a strict 3-second countdown timer using React hooks (`useEffect`, `useRef`).
  * Build the state management to log a pass/fail. If the timer hits zero before the answer is revealed, it registers as a failure.
  * Persist pass/fail results to a `user_stats` table in PostgreSQL, tracking accuracy percentage per canonical topic.

### **Phase 4: Data Visualization (The Map)**

* **Objective:** Render an interactive X/Y bubble chart to expose high-value knowledge gaps.
* **Tasks:**
  * Integrate D3.js or Visx into the React frontend.
  * Map the axes:
    * **X-Axis:** Mean dollar value per canonical topic (calculated from `clues_java` joined to `category_mappings`).
    * **Y-Axis:** User accuracy % per canonical topic (from Phase 3 stats).
    * **Bubble Radius:** Total volume of clues in that topic.
  * Implement click-events on the bubbles so clicking a weak category immediately launches a Study Mode session for those specific clues.

---

> **Note:** The PM's original plan referenced Firebase/Firestore and a Python ETL script for Phase 1. Both were replaced — see notes in the Tech Stack and Phase 1 sections above.
