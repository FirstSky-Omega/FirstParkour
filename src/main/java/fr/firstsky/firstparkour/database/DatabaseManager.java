package fr.firstsky.firstparkour.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.firstsky.firstparkour.FirstParkour;
import fr.firstsky.firstparkour.model.BlockTheme;
import fr.firstsky.firstparkour.model.DailyEntry;
import fr.firstsky.firstparkour.model.Difficulty;
import fr.firstsky.firstparkour.model.PlayerData;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final FirstParkour plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(FirstParkour plugin) {
        this.plugin = plugin;
    }

    public void init() {
        HikariConfig cfg = new HikariConfig();
        String host = plugin.getConfig().getString("mysql.host", "localhost");
        int port = plugin.getConfig().getInt("mysql.port", 3306);
        String db = plugin.getConfig().getString("mysql.database", "firstparkour");
        cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useSSL=false&autoReconnect=true&characterEncoding=utf8");
        cfg.setUsername(plugin.getConfig().getString("mysql.username", "root"));
        cfg.setPassword(plugin.getConfig().getString("mysql.password", ""));
        cfg.setMaximumPoolSize(plugin.getConfig().getInt("mysql.pool-size", 10));
        cfg.setMinimumIdle(2);
        cfg.setConnectionTimeout(10_000);
        cfg.setPoolName("FirstParkour-Pool");
        cfg.setDriverClassName("fr.firstsky.firstparkour.libs.mysql.cj.jdbc.Driver");

        dataSource = new HikariDataSource(cfg);
        createTables();
        migrateIfNeeded();
    }

    private void createTables() {
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS firstparkour_players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    name VARCHAR(16) NOT NULL,
                    best_score_easy INT DEFAULT 0,
                    best_score_medium INT DEFAULT 0,
                    best_score_hard INT DEFAULT 0,
                    total_jumps BIGINT DEFAULT 0,
                    theme VARCHAR(32) DEFAULT 'default',
                    last_played TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS firstparkour_daily (
                    uuid VARCHAR(36) NOT NULL,
                    name VARCHAR(16) NOT NULL,
                    score INT NOT NULL DEFAULT 0,
                    day DATE NOT NULL,
                    difficulty VARCHAR(16) NOT NULL,
                    PRIMARY KEY (uuid, day, difficulty)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossible de créer les tables MySQL", e);
        }
    }

    /** Ajoute la colonne theme si elle n'existe pas (migration depuis v1.0.0) */
    private void migrateIfNeeded() {
        try (Connection c = dataSource.getConnection()) {
            DatabaseMetaData meta = c.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "firstparkour_players", "theme")) {
                if (!rs.next()) {
                    try (Statement s = c.createStatement()) {
                        s.executeUpdate("ALTER TABLE firstparkour_players ADD COLUMN theme VARCHAR(32) DEFAULT 'default'");
                        plugin.getLogger().info("Migration DB : colonne 'theme' ajoutée.");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erreur migration DB", e);
        }
    }

    public PlayerData loadPlayer(UUID uuid, String name) {
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM firstparkour_players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlayerData(uuid,
                            rs.getString("name"),
                            rs.getInt("best_score_easy"),
                            rs.getInt("best_score_medium"),
                            rs.getInt("best_score_hard"),
                            rs.getLong("total_jumps"),
                            BlockTheme.fromKey(rs.getString("theme")));
                }
            }
            try (PreparedStatement ins = c.prepareStatement(
                    "INSERT IGNORE INTO firstparkour_players (uuid, name) VALUES (?, ?)")) {
                ins.setString(1, uuid.toString());
                ins.setString(2, name);
                ins.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur chargement joueur " + uuid, e);
        }
        return new PlayerData(uuid, name, 0, 0, 0, 0, BlockTheme.DEFAULT);
    }

    public void savePlayer(PlayerData data) {
        String sql = """
            INSERT INTO firstparkour_players (uuid, name, best_score_easy, best_score_medium, best_score_hard, total_jumps, theme)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                best_score_easy   = GREATEST(best_score_easy,   VALUES(best_score_easy)),
                best_score_medium = GREATEST(best_score_medium, VALUES(best_score_medium)),
                best_score_hard   = GREATEST(best_score_hard,   VALUES(best_score_hard)),
                total_jumps = total_jumps + VALUES(total_jumps),
                theme = VALUES(theme),
                last_played = CURRENT_TIMESTAMP
        """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, data.getUuid().toString());
            ps.setString(2, data.getName());
            ps.setInt(3, data.getBestScoreEasy());
            ps.setInt(4, data.getBestScoreMedium());
            ps.setInt(5, data.getBestScoreHard());
            ps.setLong(6, data.getTotalJumps());
            ps.setString(7, data.getTheme().getKey());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur sauvegarde joueur " + data.getUuid(), e);
        }
    }

    /** Classement global : trié par GREATEST(easy, medium, hard) DESC */
    public List<PlayerData> getGlobalLeaderboard(int limit) {
        List<PlayerData> list = new ArrayList<>();
        String sql = "SELECT * FROM firstparkour_players " +
                     "ORDER BY GREATEST(best_score_easy, best_score_medium, best_score_hard) DESC LIMIT ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PlayerData(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("name"),
                            rs.getInt("best_score_easy"),
                            rs.getInt("best_score_medium"),
                            rs.getInt("best_score_hard"),
                            rs.getLong("total_jumps"),
                            BlockTheme.fromKey(rs.getString("theme"))));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur classement global", e);
        }
        return list;
    }

    public List<PlayerData> getLeaderboard(Difficulty difficulty, int limit) {
        String col = difficulty.getScoreColumn();
        List<PlayerData> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM firstparkour_players ORDER BY " + col + " DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PlayerData(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("name"),
                            rs.getInt("best_score_easy"),
                            rs.getInt("best_score_medium"),
                            rs.getInt("best_score_hard"),
                            rs.getLong("total_jumps"),
                            BlockTheme.fromKey(rs.getString("theme"))));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur classement " + difficulty, e);
        }
        return list;
    }

    public List<DailyEntry> getDailyLeaderboard(LocalDate date, int limit) {
        List<DailyEntry> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT uuid, name, score FROM firstparkour_daily WHERE day = ? ORDER BY score DESC LIMIT ?")) {
            ps.setDate(1, java.sql.Date.valueOf(date));
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DailyEntry(UUID.fromString(rs.getString("uuid")),
                            rs.getString("name"), rs.getInt("score")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur classement daily", e);
        }
        return list;
    }

    public void saveDailyScore(UUID uuid, String name, int score, LocalDate date, Difficulty difficulty) {
        String sql = """
            INSERT INTO firstparkour_daily (uuid, name, score, day, difficulty)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                score = GREATEST(score, VALUES(score))
        """;
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setInt(3, score);
            ps.setDate(4, java.sql.Date.valueOf(date));
            ps.setString(5, difficulty.getKey());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erreur sauvegarde daily score " + uuid, e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
