package org.storer;

import org.storer.meta.Clue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Storer {

    private static final String URL = "jdbc:postgresql://localhost:5432/joevandevusse";
    private static final String USER = "joevandevusse";
    private static final String PASSWORD = "whombovb2508";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    protected void storeClues(List<Clue> clues) {
        String insertQuery =
            "INSERT INTO clues_java " +
            "(id, category, round, category_number, value, question, answer, is_daily_double, " +
            "game_id, game_date, date_added) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {

            clues.forEach(clue -> {
                try {
                    preparedStatement.setString(1, UUID.randomUUID().toString());
                    preparedStatement.setString(2, clue.getCategory());
                    preparedStatement.setString(3, clue.getRound());
                    preparedStatement.setInt(4, clue.getCategoryNumber());
                    preparedStatement.setString(5, clue.getClueValue());
                    preparedStatement.setString(6, clue.getQuestion());
                    preparedStatement.setString(7, clue.getAnswer());
                    preparedStatement.setBoolean(8, clue.getIsDailyDouble());
                    preparedStatement.setInt(9, clue.getGameId());
                    preparedStatement.setString(10, clue.getGameDate());
                    preparedStatement.setString(11, LocalDate.now().format(formatter));

                    preparedStatement.addBatch();
                } catch (SQLException e) {
                    System.out.println(Arrays.toString(e.getStackTrace()));
                }
            });
            // Execute the query
            preparedStatement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
