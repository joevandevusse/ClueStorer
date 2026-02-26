package org.storer.loader;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.storer.meta.Clue;

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
        boolean dryRun = args.length > 0 && args[0].equals("--dry-run");
        int fileArgIndex = dryRun ? 1 : 0;

        if (fileArgIndex >= args.length) {
            System.err.println("Usage: TsvLoader [--dry-run] <path-to-tsv-file>");
            System.exit(1);
        }

        String filePath = args[fileArgIndex];

        if (dryRun) {
            new TsvLoader().load(filePath);
            return;
        }

        String url      = System.getenv("DB_URL");
        String user     = System.getenv("DB_USER");
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
            new TsvLoader(dataSource).load(filePath);
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private final HikariDataSource dataSource;
    private final boolean dryRun;

    /** Dry-run constructor — no database connection required. */
    TsvLoader() {
        this.dataSource = null;
        this.dryRun = true;
    }

    TsvLoader(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.dryRun = false;
    }

    void load(String filePath) {
        CSVFormat format = CSVFormat.TDF.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        try (Reader reader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, format)) {

            if (dryRun) {
                long count = parser.stream().count();
                log.info("[DRY RUN] {} rows would be inserted.", count);
                return;
            }

            String insertQuery =
                    "INSERT INTO " + TABLE +
                    " (id, category, round, category_number, clue_value, question, answer," +
                    " is_daily_double, game_id, game_date, date_added)" +
                    " SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
                    " WHERE NOT EXISTS (SELECT 1 FROM " + TABLE + " WHERE id = ?)";

            String dateAdded = LocalDate.now().format(DATE_ADDED_FORMATTER);

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(insertQuery)) {

                int count = 0;

                for (CSVRecord record : parser) {
                    Clue clue = parseRecord(record);

                    String id = UUID.nameUUIDFromBytes(
                            (clue.round() + "|" + clue.category() + "|" + clue.question() + "|" + clue.gameDate())
                                    .getBytes(StandardCharsets.UTF_8)).toString();

                    ps.setString(1, id);
                    ps.setString(2, clue.category());
                    ps.setString(3, clue.round());
                    ps.setInt(4, clue.categoryNumber());
                    ps.setString(5, clue.clueValue());
                    ps.setString(6, clue.question());
                    ps.setString(7, clue.answer());
                    ps.setBoolean(8, clue.isDailyDouble());
                    ps.setInt(9, clue.gameId());
                    ps.setString(10, clue.gameDate());
                    ps.setString(11, dateAdded);
                    ps.setString(12, id);   // id for the NOT EXISTS check
                    ps.addBatch();

                    count++;
                    if (count % BATCH_SIZE == 0) {
                        ps.executeBatch();
                        log.info("Inserted {} rows...", count);
                    }
                }

                ps.executeBatch();
                log.info("Load complete. Total rows processed: {}", count);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load TSV file: " + filePath, e);
        }
    }

    private Clue parseRecord(CSVRecord record) {
        String airDate    = record.get("air_date").trim();
        String category   = record.get("category").trim();
        String question   = record.get("question").trim();
        String answer     = record.get("answer").trim();
        String rawRound   = record.get("round").trim();
        String rawValue   = record.get("clue_value").trim();
        String rawDdValue = record.get("daily_double_value").trim();

        String  round         = mapRound(rawRound);
        boolean isDailyDouble = !rawDdValue.isEmpty() && Integer.parseInt(rawDdValue) > 0;
        String  clueValue     = (rawValue.isEmpty() || rawValue.equals("0")) ? "$0" : "$" + rawValue;
        int     gameId        = airDate.isEmpty() ? 0 : Integer.parseInt(airDate.replace("-", ""));

        return new Clue(category, round, -1, clueValue, question, answer, isDailyDouble, gameId, airDate);
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
