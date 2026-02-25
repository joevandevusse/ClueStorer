package org.storer.loader;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TsvLoader {

    private static final Logger log = LoggerFactory.getLogger(TsvLoader.class);
    private static final String TABLE = "clues_java";
    private static final int BATCH_SIZE = 500;
    private static final DateTimeFormatter DATE_ADDED_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: TsvLoader <path-to-tsv-file>");
            System.exit(1);
        }

        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || user == null || password == null) {
            System.err.println("Missing required environment variables: DB_URL, DB_USER, DB_PASSWORD");
            System.exit(1);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);

        try (HikariDataSource dataSource = new HikariDataSource(config)) {
            new TsvLoader(dataSource).load(args[0]);
        }
    }

    private final HikariDataSource dataSource;

    TsvLoader(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    void load(String filePath) {
        String insertQuery =
                "INSERT INTO " + TABLE +
                " (id, category, round, category_number, clue_value, question, answer," +
                " is_daily_double, game_id, game_date, date_added)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" +
                " ON CONFLICT (id) DO NOTHING";

        String dateAdded = LocalDate.now().format(DATE_ADDED_FORMATTER);

        CSVFormat format = CSVFormat.TDF.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (Reader reader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format);
             Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(insertQuery)) {

            int count = 0;

            for (CSVRecord record : parser) {
                String airDate    = record.get("air_date").trim();
                String category   = record.get("category").trim();
                String question   = record.get("question").trim();
                String answer     = record.get("answer").trim();
                String rawRound   = record.get("round").trim();
                String rawValue   = record.get("clue_value").trim();
                String rawDdValue = record.get("daily_double_value").trim();

                String  round          = mapRound(rawRound);
                boolean isDailyDouble  = !rawDdValue.isEmpty() && Integer.parseInt(rawDdValue) > 0;
                String  clueValue      = (rawValue.isEmpty() || rawValue.equals("0")) ? "$0" : "$" + rawValue;
                int     gameId         = airDate.isEmpty() ? 0 : Integer.parseInt(airDate.replace("-", ""));

                // Deterministic ID — makes the loader idempotent on re-runs
                String deterministicKey = round + "|" + category + "|" + question + "|" + airDate;
                String id = UUID.nameUUIDFromBytes(
                        deterministicKey.getBytes(StandardCharsets.UTF_8)).toString();

                ps.setString(1, id);
                ps.setString(2, category);
                ps.setString(3, round);
                ps.setInt(4, -1);       // category_number not available in TSV
                ps.setString(5, clueValue);
                ps.setString(6, question);
                ps.setString(7, answer);
                ps.setBoolean(8, isDailyDouble);
                ps.setInt(9, gameId);
                ps.setString(10, airDate);
                ps.setString(11, dateAdded);
                ps.addBatch();

                count++;
                if (count % BATCH_SIZE == 0) {
                    ps.executeBatch();
                    log.info("Inserted {} rows...", count);
                }
            }

            ps.executeBatch(); // flush remaining rows
            log.info("Load complete. Total rows processed: {}", count);

        } catch (Exception e) {
            log.error("Failed to load TSV file: {}", filePath, e);
            System.exit(1);
        }
    }

    private String mapRound(String raw) {
        return switch (raw) {
            case "1" -> "J";
            case "2" -> "DJ";
            case "3" -> "FJ";
            default  -> raw;
        };
    }
}
