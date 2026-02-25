package org.storer;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.storer.meta.Clue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class Storer {

    private static final String TABLE = "clues_java";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final HikariDataSource dataSource;

    public Storer() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || user == null || password == null) {
            throw new IllegalStateException(
                "Missing required environment variables: DB_URL, DB_USER, DB_PASSWORD");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        dataSource = new HikariDataSource(config);
    }

    protected void storeClues(List<Clue> clues) {
        String insertQuery =
            "INSERT INTO " + TABLE +
            " (id, category, round, category_number, clue_value, question, answer, is_daily_double," +
            " game_id, game_date, date_added)" +
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(insertQuery)) {

            for (Clue clue : clues) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, clue.category());
                ps.setString(3, clue.round());
                ps.setInt(4, clue.categoryNumber());
                ps.setString(5, clue.clueValue());
                ps.setString(6, clue.question());
                ps.setString(7, clue.answer());
                ps.setBoolean(8, clue.isDailyDouble());
                ps.setInt(9, clue.gameId());
                ps.setString(10, clue.gameDate());
                ps.setString(11, LocalDate.now().format(FORMATTER));
                ps.addBatch();
            }
            ps.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
