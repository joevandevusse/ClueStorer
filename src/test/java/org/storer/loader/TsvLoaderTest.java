package org.storer.loader;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TsvLoaderTest {

    private static final String FIXTURE = "src/test/java/testFiles/sample_clues.tsv";
    private static HikariDataSource dataSource;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        config.setUsername("sa");
        config.setPassword("");
        dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS clues_java (
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
                )
            """);
        }
    }

    @AfterAll
    static void tearDown() {
        dataSource.close();
    }

    @BeforeEach
    void clearTable() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM clues_java");
        }
    }

    private void load() {
        new TsvLoader(dataSource).load(FIXTURE);
    }

    @Test
    void testLoadsAllRows() throws Exception {
        load();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT COUNT(*) FROM clues_java")) {
            rs.next();
            assertEquals(4, rs.getInt(1));
        }
    }

    @Test
    void testRoundMapping() throws Exception {
        load();
        Set<String> rounds = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT DISTINCT round FROM clues_java")) {
            while (rs.next()) rounds.add(rs.getString("round"));
        }
        assertTrue(rounds.contains("J"));
        assertTrue(rounds.contains("DJ"));
        assertTrue(rounds.contains("FJ"));
    }

    @Test
    void testClueValueFormatting() throws Exception {
        load();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT clue_value FROM clues_java WHERE category = 'GEOGRAPHY'")) {
            assertTrue(rs.next());
            assertEquals("$200", rs.getString("clue_value"));
        }
    }

    @Test
    void testFjClueValue() throws Exception {
        load();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT clue_value FROM clues_java WHERE round = 'FJ'")) {
            assertTrue(rs.next());
            assertEquals("$0", rs.getString("clue_value"));
        }
    }

    @Test
    void testDailyDoubleDetected() throws Exception {
        load();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT is_daily_double FROM clues_java WHERE category = 'HISTORY'")) {
            assertTrue(rs.next());
            assertTrue(rs.getBoolean("is_daily_double"));
        }
    }

    @Test
    void testNonDailyDoubleNotFlagged() throws Exception {
        load();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT is_daily_double FROM clues_java WHERE category = 'GEOGRAPHY'")) {
            assertTrue(rs.next());
            assertFalse(rs.getBoolean("is_daily_double"));
        }
    }

    @Test
    void testGameIdDerivedFromAirDate() throws Exception {
        load();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT game_id FROM clues_java WHERE category = 'GEOGRAPHY'")) {
            assertTrue(rs.next());
            assertEquals(19840910, rs.getInt("game_id"));
        }
    }

    @Test
    void testIdempotency() throws Exception {
        load();
        load(); // run twice
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT COUNT(*) FROM clues_java")) {
            rs.next();
            assertEquals(4, rs.getInt(1)); // still 4, not 8
        }
    }

    @Test
    void testFieldMapping() throws Exception {
        load();
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.createStatement()
                     .executeQuery("SELECT * FROM clues_java WHERE category = 'GEOGRAPHY'")) {
            assertTrue(rs.next());
            assertEquals("J",                          rs.getString("round"));
            assertEquals("This country's capital is Paris", rs.getString("question"));
            assertEquals("France",                     rs.getString("answer"));
            assertEquals("1984-09-10",                 rs.getString("game_date"));
            assertEquals(-1,                           rs.getInt("category_number"));
        }
    }
}
