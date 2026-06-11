package pl.lukas.reputation;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import pl.lukas.reputation.command.ReputationCommands;
import pl.lukas.reputation.db.DatabaseManager;
import pl.lukas.reputation.hook.PlaceholderAPIHook;
import pl.lukas.reputation.listener.ReputationListener;
import pl.lukas.reputation.manager.ReputationManager;

public final class ReputationPlugin extends JavaPlugin {
    private DatabaseManager databaseManager;
    private ReputationManager reputationManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize SQLite Database Manager
        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        // Initialize Reputation Manager
        reputationManager = new ReputationManager(this);

        // Register Command Executor and Tab Completer
        ReputationCommands commands = new ReputationCommands(this);
        if (getCommand("reputation") != null) {
            getCommand("reputation").setExecutor(commands);
            getCommand("reputation").setTabCompleter(commands);
        }
        if (getCommand("rep+") != null) {
            getCommand("rep+").setExecutor(commands);
            getCommand("rep+").setTabCompleter(commands);
        }
        if (getCommand("rep-") != null) {
            getCommand("rep-").setExecutor(commands);
            getCommand("rep-").setTabCompleter(commands);
        }

        // Register Listeners
        getServer().getPluginManager().registerEvents(new ReputationListener(this), this);

        // Register PlaceholderAPI hook if present
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI!");
        }

        // If reloading, load data for already online players
        for (Player online : Bukkit.getOnlinePlayers()) {
            reputationManager.loadPlayer(online.getUniqueId(), online.getName());
            reputationManager.updatePrefixes(online);
        }

        getLogger().info("ReputationPlugin has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Save all data and disconnect SQLite
        if (reputationManager != null) {
            reputationManager.saveAll();
            reputationManager.cleanupTeams();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("ReputationPlugin has been disabled successfully!");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ReputationManager getReputationManager() {
        return reputationManager;
    }
}
