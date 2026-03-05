GRANT ALL PRIVILEGES ON TABLE clues_java TO your_user;
GRANT ALL PRIVILEGES ON TABLE cluster_assignments TO your_user;
GRANT ALL PRIVILEGES ON TABLE category_mappings TO your_user;
GRANT ALL PRIVILEGES ON TABLE user_stats TO your_user;
GRANT USAGE, SELECT ON SEQUENCE user_stats_id_seq TO your_user;

--truncate category_mappings;

SELECT DISTINCT canonical_topic FROM category_mappings
                                ORDER BY canonical_topic;
SELECT COUNT(DISTINCT canonical_topic) FROM category_mappings;

UPDATE clues_java SET answer   = REPLACE(answer,   E'\\"', '"')
                  WHERE answer   LIKE E'%\\"%';
UPDATE clues_java SET question = REPLACE(question, E'\\"', '"')
                  WHERE question LIKE E'%\\"%';
UPDATE clues_java SET category   = REPLACE(category,   E'\\"', '"')
                  WHERE category   LIKE E'%\\"%';

SELECT COUNT(*) FROM clues_java
WHERE POSITION(E'\\"' IN answer)   > 0
   OR POSITION(E'\\"' IN question) > 0;