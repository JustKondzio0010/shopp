package pl.konrad.vipshop.shop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import pl.konrad.vipshop.VipShop;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DynamicShopManager {

    public static final class SalesData {
        public double salesCount;
        public long lastUpdate;

        public SalesData(double salesCount, long lastUpdate) {
            this.salesCount = salesCount;
            this.lastUpdate = lastUpdate;
        }
    }

    public static final class ShopItemInfo {
        public final String materialName;
        public final Material material;
        public final String categoryId;
        public final double baseBuy;
        public final double baseSell;
        public final double threshold;

        public ShopItemInfo(String materialName, Material material, String categoryId, double baseBuy, double baseSell, double threshold) {
            this.materialName = materialName;
            this.material = material;
            this.categoryId = categoryId;
            this.baseBuy = baseBuy;
            this.baseSell = baseSell;
            this.threshold = threshold;
        }
    }

    public static final class CategoryInfo {
        public final String id;
        public final String title;
        public final Material icon;
        public final List<ShopItemInfo> items = new ArrayList<>();

        public CategoryInfo(String id, String title, Material icon) {
            this.id = id;
            this.title = title;
            this.icon = icon;
        }
    }

    private final VipShop plugin;
    private final Map<String, SalesData> salesCache = new ConcurrentHashMap<>();
    private final Map<String, CategoryInfo> categories = new LinkedHashMap<>();
    private final Map<String, ShopItemInfo> itemsByMaterialName = new HashMap<>();

    public DynamicShopManager(VipShop plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        loadConfig();
        loadDatabaseSales();
    }

    private void loadConfig() {
        categories.clear();
        itemsByMaterialName.clear();

        ConfigurationSection shopSec = plugin.getConfig().getConfigurationSection("dynamic-shop.categories");
        if (shopSec == null) {
            plugin.getLogger().warning("Brak sekcji dynamic-shop.categories w config.yml!");
            return;
        }

        for (String catKey : shopSec.getKeys(false)) {
            ConfigurationSection catSec = shopSec.getConfigurationSection(catKey);
            if (catSec == null) continue;

            String title = catSec.getString("title", catKey);
            String iconStr = catSec.getString("icon", "STONE");
            Material icon = Material.matchMaterial(iconStr);
            if (icon == null) {
                icon = Material.STONE;
                plugin.getLogger().warning("Niepoprawna ikona kategorii " + catKey + ": " + iconStr + ". Uzyto STONE.");
            }

            CategoryInfo category = new CategoryInfo(catKey, title, icon);

            ConfigurationSection itemsSec = catSec.getConfigurationSection("items");
            if (itemsSec != null) {
                for (String itemKey : itemsSec.getKeys(false)) {
                    ConfigurationSection itemSec = itemsSec.getConfigurationSection(itemKey);
                    if (itemSec == null) continue;

                    Material mat = Material.matchMaterial(itemKey);
                    if (mat == null) {
                        plugin.getLogger().warning("Ignorowanie nieznanego materialu w sklepie: " + itemKey);
                        continue;
                    }

                    double buy = itemSec.getDouble("base-buy", 10.0);
                    double sell = itemSec.getDouble("base-sell", 1.0);
                    double threshold = itemSec.getDouble("threshold", 500.0);

                    ShopItemInfo itemInfo = new ShopItemInfo(itemKey, mat, catKey, buy, sell, threshold);
                    category.items.add(itemInfo);
                    itemsByMaterialName.put(itemKey, itemInfo);
                }
            }

            categories.put(catKey, category);
        }
        plugin.getLogger().info("Zaladowano " + categories.size() + " kategorii sklepu oraz " + itemsByMaterialName.size() + " przedmiotow.");
    }

    private void loadDatabaseSales() {
        salesCache.clear();
        salesCache.putAll(plugin.getDatabaseManager().loadAllSalesData());
        plugin.getLogger().info("Wczytano statystyki sprzedazy dla " + salesCache.size() + " przedmiotow z bazy SQLite.");
    }

    /**
     * Lazily applies decay to salesCount based on time elapsed since lastUpdate, then returns it.
     */
    public synchronized SalesData getSalesData(String materialName) {
        SalesData data = salesCache.get(materialName);
        if (data == null) {
            data = new SalesData(0.0, System.currentTimeMillis());
            salesCache.put(materialName, data);
            return data;
        }

        long now = System.currentTimeMillis();
        long elapsedMillis = now - data.lastUpdate;
        if (elapsedMillis > 1000) { // Only calculate decay if at least 1 second passed
            double minutes = elapsedMillis / 60000.0;
            double decayRate = plugin.getConfig().getDouble("dynamic-shop.pricing.decay-rate-per-minute", 0.98);
            double decayedSales = data.salesCount * Math.pow(decayRate, minutes);
            
            // Prevent keeping tiny values
            if (decayedSales < 0.01) {
                decayedSales = 0.0;
            }

            data.salesCount = decayedSales;
            data.lastUpdate = now;

            // Persist lazy decay to database asynchronously
            saveSalesDataAsync(materialName, data.salesCount, data.lastUpdate);
        }
        return data;
    }

    /**
     * Get current price drop percentage (0.0 to 1.0)
     */
    public double getPriceDropPercent(String materialName) {
        ShopItemInfo item = itemsByMaterialName.get(materialName);
        if (item == null) return 0.0;

        SalesData salesData = getSalesData(materialName);
        double sales = salesData.salesCount;

        double minSales = plugin.getConfig().getDouble("dynamic-shop.pricing.min-sales-to-drop", 64.0);
        double minDrop = plugin.getConfig().getDouble("dynamic-shop.pricing.min-price-drop", 0.20);
        double maxDrop = plugin.getConfig().getDouble("dynamic-shop.pricing.max-price-drop", 0.60);

        if (sales < minSales) {
            return 0.0;
        }

        double scale = (sales - minSales) / item.threshold;
        double progress = scale / (scale + 1.0); // ranges smoothly from 0.0 to 1.0
        return minDrop + (maxDrop - minDrop) * progress;
    }

    public double getCurrentBuyPrice(String materialName) {
        ShopItemInfo item = itemsByMaterialName.get(materialName);
        if (item == null) return 0.0;
        return item.baseBuy * (1.0 - getPriceDropPercent(materialName));
    }

    public double getCurrentSellPrice(String materialName) {
        ShopItemInfo item = itemsByMaterialName.get(materialName);
        if (item == null) return 0.0;
        return item.baseSell * (1.0 - getPriceDropPercent(materialName));
    }

    /**
     * Adds sold quantity to sales history
     */
    public synchronized void registerSale(String materialName, double quantity) {
        SalesData data = getSalesData(materialName); // Applies decay first
        data.salesCount += quantity;
        data.lastUpdate = System.currentTimeMillis();

        // Save new state asynchronously
        saveSalesDataAsync(materialName, data.salesCount, data.lastUpdate);
    }

    private void saveSalesDataAsync(String materialName, double salesCount, long lastUpdate) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().saveSalesData(materialName, salesCount, lastUpdate);
        });
    }

    public Collection<CategoryInfo> getCategories() {
        return categories.values();
    }

    public CategoryInfo getCategory(String categoryId) {
        return categories.get(categoryId);
    }

    public ShopItemInfo getItemInfo(String materialName) {
        return itemsByMaterialName.get(materialName);
    }
}
