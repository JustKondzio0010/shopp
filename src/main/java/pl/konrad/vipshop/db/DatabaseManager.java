package pl.konrad.vipshop.db;

import pl.konrad.vipshop.VipShop;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public final class DatabaseManager {

    private final VipShop plugin;
    private Connection connection;

    public DatabaseManager(VipShop plugin) {
        this.plugin = plugin;
    }

    public synchronized void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "database.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            createTables();
            plugin.getLogger().info("Polaczenie z baza SQLite zostalo pomyslnie otwarte.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Nie znaleziono sterownika JDBC SQLite!", e);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Blad podczas laczenia z baza SQLite!", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Table for backpack inventories (using TEXT for unlimited capacity base64 string)
            stmt.execute("CREATE TABLE IF NOT EXISTS backpack_inventories (" +
                         "backpack_uuid TEXT PRIMARY KEY, " +
                         "contents TEXT NOT NULL" +
                         ");");

            // Table for frozen players
            stmt.execute("CREATE TABLE IF NOT EXISTS frozen_players (" +
                         "player_uuid TEXT PRIMARY KEY, " +
                         "reason TEXT, " +
                         "timestamp INTEGER NOT NULL" +
                         ");");

            // Table for auditor logs
            stmt.execute("CREATE TABLE IF NOT EXISTS auditor_logs (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                         "player_uuid TEXT NOT NULL, " +
                         "alert_type TEXT NOT NULL, " +
                         "details TEXT NOT NULL, " +
                         "timestamp INTEGER NOT NULL" +
                         ");");

            // Table for dynamic shop prices
            stmt.execute("CREATE TABLE IF NOT EXISTS shop_dynamic_prices (" +
                         "item_material TEXT PRIMARY KEY, " +
                         "sales_count DOUBLE NOT NULL, " +
                         "last_update INTEGER NOT NULL" +
                         ");");
        }
    }

    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("Polaczenie z baza SQLite zostalo zamkniete.");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Blad podczas zamykania bazy SQLite!", e);
            }
        }
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            initialize();
        }
        return connection;
    }

    // Load backpack contents
    public synchronized String getBackpackContents(String uuid) {
        String query = "SELECT contents FROM backpack_inventories WHERE backpack_uuid = ?;";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("contents");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Blad podczas wczytywania plecaka: " + uuid, e);
        }
        return null;
    }

    // Save backpack contents
    public synchronized void saveBackpackContents(String uuid, String contents) {
        String query = "INSERT INTO backpack_inventories (backpack_uuid, contents) VALUES (?, ?) " +
                       "ON CONFLICT(backpack_uuid) DO UPDATE SET contents = excluded.contents;";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid);
            ps.setString(2, contents);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Blad podczas zapisu plecaka: " + uuid, e);
        }
    }

    // Set frozen state
    public synchronized void setPlayerFrozen(String uuid, boolean freeze, String reason) {
        if (freeze) {
            String query = "INSERT INTO frozen_players (player_uuid, reason, timestamp) VALUES (?, ?, ?) " +
                           "ON CONFLICT(player_uuid) DO NOTHING;";
            try (PreparedStatement ps = getConnection().prepareStatement(query)) {
                ps.setString(1, uuid);
                ps.setString(2, reason);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Blad podczas zamrazania gracza: " + uuid, e);
            }
        } else {
            String query = "DELETE FROM frozen_players WHERE player_uuid = ?;";
            try (PreparedStatement ps = getConnection().prepareStatement(query)) {
                ps.setString(1, uuid);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Blad podczas odmrazania gracza: " + uuid, e);
            }
        }
    }

    // Check if player is frozen
    public synchronized boolean isPlayerFrozen(String uuid) {
        String query = "SELECT 1 FROM frozen_players WHERE player_uuid = ?;";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Blad podczas sprawdzania zamrozenia gracza: " + uuid, e);
        }
        return false;
    }

    // Insert alert log
    public synchronized void logAlert(String uuid, String type, String details) {
        String query = "INSERT INTO auditor_logs (player_uuid, alert_type, details, timestamp) VALUES (?, ?, ?, ?);";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, uuid);
            ps.setString(2, type);
            ps.setString(3, details);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Blad podczas zapisywania logu audytora dla gracza: " + uuid, e);
        }
    }

    // Load all dynamic shop sales data on startup
    public synchronized java.util.Map<String, pl.konrad.vipshop.shop.DynamicShopManager.SalesData> loadAllSalesData() {
        java.util.Map<String, pl.konrad.vipshop.shop.DynamicShopManager.SalesData> map = new java.util.HashMap<>();
        String query = "SELECT item_material, sales_count, last_update FROM shop_dynamic_prices;";
        try (PreparedStatement ps = getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String material = rs.getString("item_material");
                double count = rs.getDouble("sales_count");
                long lastUpdate = rs.getLong("last_update");
                map.put(material, new pl.konrad.vipshop.shop.DynamicShopManager.SalesData(count, lastUpdate));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Blad wczytywania dynamicznych cen z bazy!", e);
        }
        return map;
    }

    // Save sales data for an item
    public synchronized void saveSalesData(String material, double salesCount, long lastUpdate) {
        String query = "INSERT INTO shop_dynamic_prices (item_material, sales_count, last_update) VALUES (?, ?, ?) " +
                       "ON CONFLICT(item_material) DO UPDATE SET sales_count = excluded.sales_count, last_update = excluded.last_update;";
        try (PreparedStatement ps = getConnection().prepareStatement(query)) {
            ps.setString(1, material);
            ps.setDouble(2, salesCount);
            ps.setLong(3, lastUpdate);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Blad zapisu dynamicznych cen dla " + material, e);
        }
    }
}
