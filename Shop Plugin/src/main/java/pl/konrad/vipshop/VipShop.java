package pl.konrad.vipshop;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import pl.konrad.vipshop.auditor.EconomyAuditor;
import pl.konrad.vipshop.db.DatabaseManager;
import pl.konrad.vipshop.items.CustomItemListener;
import pl.konrad.vipshop.items.CustomItemManager;
import pl.konrad.vipshop.shop.VIPShopCommand;
import pl.konrad.vipshop.shop.VIPShopListener;

public final class VipShop extends JavaPlugin {

    private static VipShop instance;

    private Economy economy;
    private DatabaseManager databaseManager;
    private EconomyAuditor economyAuditor;
    private CustomItemManager customItemManager;
    private VIPShopCommand vipShopCommand;
    private pl.konrad.vipshop.shop.DynamicShopManager dynamicShopManager;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        // 1. Initialize SQLite Database
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // 2. Setup Vault Economy connection
        if (!setupEconomy()) {
            getLogger().severe("========================================");
            getLogger().severe("BLAD: Vault lub plugin od ekonomii (np. EssentialsX) nie zostal znaleziony!");
            getLogger().severe("Ekonomia w Sklepie VIP nie bedzie dzialac!");
            getLogger().severe("Upewnij sie ze zainstalowales oba pluginy na serwerze.");
            getLogger().severe("========================================");
        } else {
            getLogger().info("Polaczono z systemem ekonomii Vault.");
        }

        // 3. Initialize Custom Items
        customItemManager = new CustomItemManager(this);

        // 4. Initialize Economy Auditor
        economyAuditor = new EconomyAuditor(this);
        economyAuditor.startAuditing();

        // 4b. Initialize Dynamic Shop Manager
        dynamicShopManager = new pl.konrad.vipshop.shop.DynamicShopManager(this);
        dynamicShopManager.initialize();

        // 5. Register Commands
        PluginCommand shopCmd = getCommand("shop");
        if (shopCmd != null) {
            vipShopCommand = new VIPShopCommand(this);
            shopCmd.setExecutor(vipShopCommand);
        }
        
        PluginCommand unfreezeCmd = getCommand("unfreeze");
        if (unfreezeCmd != null) {
            unfreezeCmd.setExecutor(new pl.konrad.vipshop.auditor.UnfreezeCommand(this));
        }

        // 6. Register Listeners
        Bukkit.getPluginManager().registerEvents(economyAuditor, this);
        Bukkit.getPluginManager().registerEvents(new CustomItemListener(this), this);
        Bukkit.getPluginManager().registerEvents(new VIPShopListener(this), this);
        Bukkit.getPluginManager().registerEvents(new pl.konrad.vipshop.items.AmuletListener(this), this);

        getLogger().info("VipShop v" + getDescription().getVersion() + " zostal wlaczony!");
    }

    @Override
    public void onDisable() {
        // Cancel any running tasks
        Bukkit.getScheduler().cancelTasks(this);

        // Close SQLite database safely
        // Note: databaseManager's synchronized write methods will block onDisable thread
        // until active writes are flushed, preventing DB corruption.
        if (databaseManager != null) {
            databaseManager.close();
        }

        instance = null;
        getLogger().info("VipShop zostal wylaczony.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static VipShop getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        if (economy == null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
            }
        }
        return economy;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public EconomyAuditor getEconomyAuditor() {
        return economyAuditor;
    }

    public CustomItemManager getCustomItemManager() {
        return customItemManager;
    }

    public VIPShopCommand getVIPShopCommand() {
        return vipShopCommand;
    }

    public pl.konrad.vipshop.shop.DynamicShopManager getDynamicShopManager() {
        return dynamicShopManager;
    }
}
