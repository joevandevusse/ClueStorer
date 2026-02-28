-- NOTE: After creating tables, grant access to your application user:
--   GRANT ALL PRIVILEGES ON TABLE clues_java TO your_user;
--   GRANT ALL PRIVILEGES ON TABLE cluster_assignments TO your_user;

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

-- Migration 3: Cluster assignments from NLP pipeline
CREATE TABLE cluster_assignments (
    clue_id VARCHAR PRIMARY KEY REFERENCES clues_java(id),
    cluster_id INT NOT NULL,
    cluster_label VARCHAR NOT NULL
);
