package pl.lukas.legitcheck;

import org.bukkit.plugin.java.JavaPlugin;
import pl.lukas.legitcheck.command.CheckCommand;
import pl.lukas.legitcheck.listener.CheckListener;
import pl.lukas.legitcheck.listener.GuiListener;
import pl.lukas.legitcheck.manager.CheckManager;

public final class LegitCheckPlugin extends JavaPlugin {
    private CheckManager checkManager;

    @Override
    public void onEnable() {
        // Save default config if not present
        saveDefaultConfig();

        // Initialize Manager
        this.checkManager = new CheckManager(this);

        // Register Command
        if (getCommand("check") != null) {
            CheckCommand checkCommand = new CheckCommand(this);
            getCommand("check").setExecutor(checkCommand);
            getCommand("check").setTabCompleter(checkCommand);
        }

        // Register Listeners
        getServer().getPluginManager().registerEvents(new CheckListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        getLogger().info("LegitCheck Plugin has been enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("LegitCheck Plugin has been disabled.");
    }

    public CheckManager getCheckManager() {
        return checkManager;
    }
}
