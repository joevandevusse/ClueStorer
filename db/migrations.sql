-- NOTE: After creating tables, grant access to your application user:
--   GRANT ALL PRIVILEGES ON TABLE clues_java TO your_user;
--   GRANT ALL PRIVILEGES ON TABLE cluster_assignments TO your_user;
--   GRANT ALL PRIVILEGES ON TABLE category_mappings TO your_user;
--   GRANT ALL PRIVILEGES ON TABLE user_stats TO your_user;

-- Migration 1: Core clues table
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

-- Migration 2: Index for date-based filtering
CREATE INDEX idx_clues_game_date ON clues_java (game_date);

-- Migration 3: Cluster assignments from TF-IDF/K-Means pipeline (exploratory)
CREATE TABLE cluster_assignments (
    clue_id VARCHAR PRIMARY KEY REFERENCES clues_java(id),
    cluster_id INT NOT NULL,
    cluster_label VARCHAR NOT NULL
);

-- Migration 4: Canonical topic mappings from LLM normalization pipeline
CREATE TABLE category_mappings (
    jeopardy_category VARCHAR PRIMARY KEY,
    canonical_topic VARCHAR NOT NULL
);

CREATE INDEX idx_category_mappings_topic ON category_mappings (canonical_topic);

-- Migration 5: User study session results (drives Phase 4 bubble chart Y-axis)
CREATE TABLE user_stats (
    id SERIAL PRIMARY KEY,
    canonical_topic VARCHAR NOT NULL,
    passed BOOLEAN NOT NULL,
    recorded_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_stats_topic ON user_stats (canonical_topic);
