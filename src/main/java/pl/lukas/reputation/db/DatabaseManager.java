package pl.lukas.reputation.db;

import pl.lukas.reputation.ReputationPlugin;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {
    private final ReputationPlugin plugin;
    private Connection connection;
    private final File dbFile;

    public DatabaseManager(ReputationPlugin plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "reputation.db");
    }

    public synchronized void connect() {
        if (connection != null) {
            return;
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to SQLite database!", e);
        }
    }

    public synchronized void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not close SQLite database connection!", e);
            }
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS reputation_players (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "username VARCHAR(16) NOT NULL," +
                    "reputation DOUBLE DEFAULT 5.0," +
                    "pos_votes INT DEFAULT 0," +
                    "neg_votes INT DEFAULT 0" +
                    ");");
            stmt.execute("CREATE TABLE IF NOT EXISTS reputation_votes (" +
                    "voter_uuid VARCHAR(36)," +
                    "target_uuid VARCHAR(36)," +
                    "vote_type VARCHAR(10)," +
                    "PRIMARY KEY (voter_uuid, target_uuid)" +
                    ");");
            
            // Alter reputation_votes to add voter_ip if it doesn't exist
            try {
                stmt.execute("ALTER TABLE reputation_votes ADD COLUMN voter_ip VARCHAR(45);");
            } catch (SQLException e) {
                // Column probably already exists, ignore
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not create database tables!", e);
        }
    }

    public synchronized PlayerRepData loadPlayerData(UUID uuid, String defaultUsername) {
        if (connection == null) connect();
        
        String selectSql = "SELECT reputation, pos_votes, neg_votes FROM reputation_players WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double reputation = rs.getDouble("reputation");
                    int posVotes = rs.getInt("pos_votes");
                    int negVotes = rs.getInt("neg_votes");
                    return new PlayerRepData(reputation, posVotes, negVotes);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading player data for " + uuid, e);
        }
        
        // If not found, create new record and return defaults
        PlayerRepData defaultData = new PlayerRepData(5.0, 0, 0);
        savePlayerData(uuid, defaultUsername, defaultData);
        return defaultData;
    }

    public synchronized void savePlayerData(UUID uuid, String username, PlayerRepData data) {
        if (connection == null) connect();
        
        String replaceSql = "REPLACE INTO reputation_players (uuid, username, reputation, pos_votes, neg_votes) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(replaceSql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.setDouble(3, data.getScore());
            ps.setInt(4, data.getPosVotes());
            ps.setInt(5, data.getNegVotes());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving player data for " + username, e);
        }
    }

    public synchronized String getVote(UUID voterUuid, UUID targetUuid) {
        if (connection == null) connect();
        
        String sql = "SELECT vote_type FROM reputation_votes WHERE voter_uuid = ? AND target_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, voterUuid.toString());
            ps.setString(2, targetUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("vote_type");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting vote", e);
        }
        return null;
    }

    public synchronized void saveVote(UUID voterUuid, UUID targetUuid, String voteType, String voterIp) {
        if (connection == null) connect();
        
        String sql = "REPLACE INTO reputation_votes (voter_uuid, target_uuid, vote_type, voter_ip) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, voterUuid.toString());
            ps.setString(2, targetUuid.toString());
            ps.setString(3, voteType);
            ps.setString(4, voterIp != null ? voterIp : "");
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving vote", e);
        }
    }

    public synchronized boolean hasIpVoted(String ip, UUID targetUuid, UUID voterUuid) {
        if (connection == null) connect();
        if (ip == null || ip.isEmpty()) return false;
        
        String sql = "SELECT 1 FROM reputation_votes WHERE voter_ip = ? AND target_uuid = ? AND voter_uuid != ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ip);
            ps.setString(2, targetUuid.toString());
            ps.setString(3, voterUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking if IP has voted", e);
        }
        return false;
    }

    public static class PlayerRepData {
        private double score;
        private int posVotes;
        private int negVotes;

        public PlayerRepData(double score, int posVotes, int negVotes) {
            this.score = score;
            this.posVotes = posVotes;
            this.negVotes = negVotes;
        }

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public int getPosVotes() { return posVotes; }
        public void setPosVotes(int posVotes) { this.posVotes = posVotes; }
        public int getNegVotes() { return negVotes; }
        public void setNegVotes(int negVotes) { this.negVotes = negVotes; }
    }
}
