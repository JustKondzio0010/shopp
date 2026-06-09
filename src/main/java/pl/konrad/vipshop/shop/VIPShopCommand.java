package pl.konrad.vipshop.shop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import pl.konrad.vipshop.VipShop;
import pl.konrad.vipshop.shop.DynamicShopManager.CategoryInfo;
import pl.konrad.vipshop.shop.DynamicShopManager.ShopItemInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VIPShopCommand implements CommandExecutor {

    private final VipShop plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public static final class VIPShopHolder implements InventoryHolder {
        private final String menuType; // "MAIN", "CATEGORY", "EQUIPMENT", "AMULETS"
        private final String categoryId; // dynamic category ID if menuType is CATEGORY
        
        public VIPShopHolder(String menuType, String categoryId) {
            this.menuType = menuType;
            this.categoryId = categoryId;
        }
        
        public String getMenuType() {
            return menuType;
        }
        
        public String getCategoryId() {
            return categoryId;
        }
        
        @Override
        public @NotNull Inventory getInventory() {
            return null;
        }
    }

    public VIPShopCommand(VipShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cTa komenda moze byc wykonana tylko przez gracza!");
            return true;
        }

        if (!player.hasPermission("vipshop.use")) {
            Component msg = mm.deserialize("<red>Nie masz uprawnień do korzystania ze sklepu!</red>");
            player.sendMessage(msg);
            return true;
        }

        openMainMenu(player);
        return true;
    }

    public void openMainMenu(Player player) {
        Component title = mm.deserialize("<gradient:#4df2f2:#3a7bd5>✦ Kategorie Sklepu ✦</gradient>").decorate(TextDecoration.BOLD);
        Inventory inv = Bukkit.createInventory(new VIPShopHolder("MAIN", null), 27, title);

        ItemStack filler = createFillerGlass(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Add 4 Dynamic Category icons (slots 10, 12, 14, 16)
        int[] slots = {10, 12, 14, 16};
        int index = 0;
        for (CategoryInfo cat : plugin.getDynamicShopManager().getCategories()) {
            if (index >= slots.length) break;

            ItemStack catIcon = new ItemStack(cat.icon);
            ItemMeta meta = catIcon.getItemMeta();
            if (meta != null) {
                meta.displayName(mm.deserialize(cat.title).decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.add(mm.deserialize("<gray>Kliknij, aby otworzyć tę kategorię.</gray>").decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
                catIcon.setItemMeta(meta);
            }
            inv.setItem(slots[index], catIcon);
            index++;
        }

        // Add 2 VIP static category icons (slots 21, 23)
        // Kategoria: Zbroje i Bronie VIP (Slot 21)
        ItemStack equipmentCategory = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta eqMeta = equipmentCategory.getItemMeta();
        if (eqMeta != null) {
            eqMeta.displayName(mm.deserialize("<gradient:#00aaff:#00ffff><bold>Zbroje, Bronie i Narzędzia (VIP)</bold></gradient>").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<gray>Kliknij, aby otworzyć zbrojownię VIP.</gray>").decoration(TextDecoration.ITALIC, false));
            eqMeta.lore(lore);
            eqMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            equipmentCategory.setItemMeta(eqMeta);
        }
        inv.setItem(21, equipmentCategory);

        // Kategoria: Magiczne Amulety VIP (Slot 23)
        ItemStack amuletsCategory = new ItemStack(Material.NETHER_STAR);
        ItemMeta amMeta = amuletsCategory.getItemMeta();
        if (amMeta != null) {
            amMeta.displayName(mm.deserialize("<gradient:#aa00ff:#ff00aa><bold>Magiczne Amulety (VIP)</bold></gradient>").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<gray>Kliknij, aby zobaczyć amulety VIP.</gray>").decoration(TextDecoration.ITALIC, false));
            amMeta.lore(lore);
            amMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            amuletsCategory.setItemMeta(amMeta);
        }
        inv.setItem(23, amuletsCategory);

        player.openInventory(inv);
    }

    public void openCategoryMenu(Player player, String categoryId) {
        CategoryInfo cat = plugin.getDynamicShopManager().getCategory(categoryId);
        if (cat == null) {
            openMainMenu(player);
            return;
        }

        Component title = mm.deserialize(cat.title).decorate(TextDecoration.BOLD);
        Inventory inv = Bukkit.createInventory(new VIPShopHolder("CATEGORY", categoryId), 54, title);

        // Gray filler for boundaries
        ItemStack filler = createFillerGlass(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Centered grid slots for items
        int[] itemSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int index = 0;
        for (ShopItemInfo item : cat.items) {
            if (index >= itemSlots.length) break;
            inv.setItem(itemSlots[index], createShopItem(item));
            index++;
        }

        // Back arrow button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize("<red><bold>Wróć do Kategorii</bold></red>").decoration(TextDecoration.ITALIC, false));
            back.setItemMeta(meta);
        }
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    // ==========================================
    // VIP Shop Submenus (Original static VIP items)
    // ==========================================

    public void openEquipmentCategory(Player player) {
        Component title = mm.deserialize("<gradient:#ffaa00:#ffff55>✦ Zbrojownia VIP ✦</gradient>").decorate(TextDecoration.BOLD);
        Inventory inv = Bukkit.createInventory(new VIPShopHolder("EQUIPMENT", null), 54, title);

        ItemStack filler = createFillerGlass(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Silhouette - Armors
        setItemInSlot(inv, 2, "titan_helmet");
        setItemInSlot(inv, 3, "nightvision_helmet");
        setItemInSlot(inv, 11, "titan_chestplate");
        setItemInSlot(inv, 12, "magma_chestplate");
        setItemInSlot(inv, 20, "titan_leggings");
        setItemInSlot(inv, 21, "bottomless_backpack");
        setItemInSlot(inv, 29, "titan_boots");
        setItemInSlot(inv, 30, "hermes_boots");

        // Tools
        setItemInSlot(inv, 14, "miner_pickaxe");
        setItemInSlot(inv, 15, "titan_pickaxe");
        setItemInSlot(inv, 16, "gold_vein_pickaxe");
        setItemInSlot(inv, 17, "smelt_drill");
        setItemInSlot(inv, 23, "compact_hammer");
        setItemInSlot(inv, 24, "tunnel_destroyer");
        setItemInSlot(inv, 25, "titan_excavator");
        setItemInSlot(inv, 26, "greed_artifact");
        setItemInSlot(inv, 33, "lumberjack_axe");
        setItemInSlot(inv, 34, "harvest_hoe");
        setItemInSlot(inv, 35, "magnetic_rod");

        // Swords
        setItemInSlot(inv, 37, "collector_sword");
        setItemInSlot(inv, 38, "butcher_sword");
        setItemInSlot(inv, 39, "titan_sword");
        setItemInSlot(inv, 40, "reaper_scythe");
        setItemInSlot(inv, 41, "vampire_dagger");
        setItemInSlot(inv, 42, "headhunter_sword");

        // Back arrow button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize("<red><bold>Wróć do Kategorii</bold></red>").decoration(TextDecoration.ITALIC, false));
            back.setItemMeta(meta);
        }
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    public void openAmuletsCategory(Player player) {
        Component title = mm.deserialize("<gradient:#aa00ff:#ff00aa>✦ Amulety VIP ✦</gradient>").decorate(TextDecoration.BOLD);
        Inventory inv = Bukkit.createInventory(new VIPShopHolder("AMULETS", null), 54, title);

        ItemStack filler = createFillerGlass(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Amulets centered 7x2
        setItemInSlot(inv, 19, "amulet_berserker");
        setItemInSlot(inv, 20, "amulet_phantom");
        setItemInSlot(inv, 21, "amulet_vampire");
        setItemInSlot(inv, 22, "amulet_seismic");
        setItemInSlot(inv, 23, "amulet_gold_vein");
        setItemInSlot(inv, 24, "amulet_wind");
        setItemInSlot(inv, 25, "amulet_shadow");

        setItemInSlot(inv, 28, "amulet_greed");
        setItemInSlot(inv, 29, "amulet_necromancer");
        setItemInSlot(inv, 30, "amulet_time");
        setItemInSlot(inv, 31, "amulet_volcano");
        setItemInSlot(inv, 32, "amulet_tide");
        setItemInSlot(inv, 33, "amulet_illusion");
        setItemInSlot(inv, 34, "amulet_tsunami");

        // Back arrow button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize("<red><bold>Wróć do Kategorii</bold></red>").decoration(TextDecoration.ITALIC, false));
            back.setItemMeta(meta);
        }
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    private void setItemInSlot(Inventory inv, int slot, String id) {
        ItemStack item = plugin.getCustomItemManager().createItem(id);
        if (item != null) {
            updatePremiumTooltip(item, id);
            inv.setItem(slot, item);
        }
    }

    private void updatePremiumTooltip(ItemStack item, String id) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String gradientName = plugin.getCustomItemManager().getGradientName(id);
        Component nameComponent = mm.deserialize(gradientName).decoration(TextDecoration.ITALIC, false);
        meta.displayName(nameComponent);

        List<Component> newLore = new ArrayList<>();
        String description = getFancyDescription(id);
        if (!description.isEmpty()) {
            newLore.add(mm.deserialize("<gray><i>" + description + "</i></gray>"));
            newLore.add(Component.empty());
        }

        List<String> stats = getFancyStats(id);
        for (String stat : stats) {
            newLore.add(mm.deserialize(stat).decoration(TextDecoration.ITALIC, false));
        }
        newLore.add(Component.empty());

        double price = plugin.getConfig().getDouble("shop." + id, 10000.0);
        Component priceComponent = mm.deserialize("<yellow><bold>Cena: $" + String.format(Locale.US, "%,.0f", price) + "</bold></yellow>").decoration(TextDecoration.ITALIC, false);
        newLore.add(priceComponent);

        meta.lore(newLore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private String getFancyDescription(String id) {
        return "";
    }

    private List<String> getFancyStats(String id) {
        pl.konrad.vipshop.items.CustomItemInfo info = plugin.getCustomItemManager().getItemInfo(id);
        if (info == null) return List.of();
        
        List<String> stats = new ArrayList<>();
        for (String loreLine : info.lore) {
            String line = loreLine.replace("§0", "<black>")
                                  .replace("§1", "<dark_blue>")
                                  .replace("§2", "<dark_green>")
                                  .replace("§3", "<dark_aqua>")
                                  .replace("§4", "<dark_red>")
                                  .replace("§5", "<dark_purple>")
                                  .replace("§6", "<gold>")
                                  .replace("§7", "<gray>")
                                  .replace("§8", "<dark_gray>")
                                  .replace("§9", "<blue>")
                                  .replace("§a", "<green>")
                                  .replace("§b", "<aqua>")
                                  .replace("§c", "<red>")
                                  .replace("§d", "<light_purple>")
                                  .replace("§e", "<yellow>")
                                  .replace("§f", "<white>")
                                  .replace("§l", "<bold>")
                                  .replace("§o", "<italic>")
                                  .replace("§n", "<underlined>")
                                  .replace("§m", "<strikethrough>")
                                  .replace("§k", "<obfuscated>")
                                  .replace("§r", "<reset>");
            stats.add(line);
        }
        return stats;
    }

    // ==========================================
    // Dynamic Pricing Lore Formatting
    // ==========================================

    public ItemStack createShopItem(ShopItemInfo item) {
        ItemStack stack = new ItemStack(item.material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        double currentBuy = plugin.getDynamicShopManager().getCurrentBuyPrice(item.materialName);
        double currentSell = plugin.getDynamicShopManager().getCurrentSellPrice(item.materialName);
        double dropPercent = plugin.getDynamicShopManager().getPriceDropPercent(item.materialName) * 100.0;
        double salesCount = plugin.getDynamicShopManager().getSalesData(item.materialName).salesCount;

        String displayName = getPolishName(item.material);
        meta.displayName(mm.deserialize("<gradient:#ffe066:#f5b041><bold>" + displayName + "</bold></gradient>")
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        
        // Buy Section
        lore.add(mm.deserialize("<yellow><b>Kupno:</b></yellow>").decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize(" <gray>• 1x: </gray><green>$" + String.format(Locale.US, "%.2f", currentBuy) + "</green>").decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize(" <gray>• 64x: </gray><green>$" + String.format(Locale.US, "%.2f", currentBuy * 64) + "</green>").decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        // Sell Section
        lore.add(mm.deserialize("<gold><b>Sprzedaż:</b></gold>").decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize(" <gray>• 1x: </gray><green>$" + String.format(Locale.US, "%.2f", currentSell) + "</green>").decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize(" <gray>• 64x: </gray><green>$" + String.format(Locale.US, "%.2f", currentSell * 64) + "</green>").decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        // Dynamic pricing status
        lore.add(mm.deserialize("<blue><b>Popyt rynkowy:</b></blue>").decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize(" <gray>• Ostatnia sprzedaż: </gray><aqua>" + String.format(Locale.US, "%.1f", salesCount) + " / " + String.format(Locale.US, "%.0f", item.threshold) + " szt.</aqua>").decoration(TextDecoration.ITALIC, false));
        
        if (dropPercent > 0.1) {
            lore.add(mm.deserialize(" <gray>• Status ceny: </gray><red>-" + String.format(Locale.US, "%.0f", dropPercent) + "% (Nadprodukcja)</red>").decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(mm.deserialize(" <gray>• Status ceny: </gray><green>Stabilna (100%)</green>").decoration(TextDecoration.ITALIC, false));
        }
        
        lore.add(Component.empty());
        lore.add(mm.deserialize("<dark_gray>-------------------------</dark_gray>").decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize("<yellow>[LPM]</yellow> <gray>Kup 1</gray>  <yellow>[Shift+LPM]</yellow> <gray>Kup 64</gray>").decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize("<gold>[PPM]</gold> <gray>Sprzedaj 1</gray>  <gold>[Shift+PPM]</gold> <gray>Sprzedaj all</gray>").decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS, org.bukkit.inventory.ItemFlag.HIDE_POTION_EFFECTS);
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createFillerGlass(Material mat) {
        ItemStack glass = new ItemStack(mat);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private String getPolishName(Material material) {
        switch (material) {
            case STONE: return "Kamień";
            case COBBLESTONE: return "Bruk (Cobblestone)";
            case DIRT: return "Ziemia";
            case SAND: return "Piasek";
            case GRAVEL: return "Żwir";
            case OAK_LOG: return "Dębowe drewno";
            case SPRUCE_LOG: return "Świerkowe drewno";
            case BIRCH_LOG: return "Brzozowe drewno";
            case OAK_PLANKS: return "Dębowe deski";
            case GLASS: return "Szkło";
            case SMOOTH_STONE: return "Gładki kamień";
            case STONE_BRICKS: return "Kamienne cegły";
            case WHEAT: return "Pszenica";
            case POTATO: return "Ziemniak";
            case CARROT: return "Marchewka";
            case SUGAR_CANE: return "Trzcina cukrowa";
            case CACTUS: return "Kaktus";
            case MELON: return "Arbuz";
            case PUMPKIN: return "Dynia";
            case APPLE: return "Jabłko";
            case LEATHER: return "Skóra";
            case FEATHER: return "Pióro";
            case ROTTEN_FLESH: return "Zgniłe mięso";
            case BONE: return "Kość";
            case GUNPOWDER: return "Proch strzelniczy";
            case STRING: return "Nić";
            case SPIDER_EYE: return "Oko pająka";
            case ENDER_PEARL: return "Perła Endu";
            case SLIME_BALL: return "Kula szlamu";
            case BLAZE_ROD: return "Płomienna różdżka";
            case COAL: return "Węgiel";
            case COPPER_INGOT: return "Sztabka miedzi";
            case IRON_INGOT: return "Sztabka żelaza";
            case GOLD_INGOT: return "Sztabka złota";
            case REDSTONE: return "Czerwony proszek";
            case LAPIS_LAZULI: return "Lapis Lazuli";
            case EMERALD: return "Szmaragd";
            case DIAMOND: return "Diament";
            default: return material.name().replace("_", " ").toLowerCase();
        }
    }
}
