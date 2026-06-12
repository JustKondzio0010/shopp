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
import org.bukkit.inventory.meta.SkullMeta;
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
        private final String menuType; // "MAIN", "CATEGORY", "QUANTITY", "EQUIPMENT", "AMULETS"
        private final String categoryId; // dynamic category ID if menuType is CATEGORY
        private final ShopItemInfo itemInfo; // item info if menuType is QUANTITY
        private final int page;
        
        public VIPShopHolder(String menuType, String categoryId) {
            this(menuType, categoryId, null, 1);
        }

        public VIPShopHolder(String menuType, String categoryId, ShopItemInfo itemInfo) {
            this(menuType, categoryId, itemInfo, 1);
        }

        public VIPShopHolder(String menuType, String categoryId, ShopItemInfo itemInfo, int page) {
            this.menuType = menuType;
            this.categoryId = categoryId;
            this.itemInfo = itemInfo;
            this.page = page;
        }
        
        public String getMenuType() {
            return menuType;
        }
        
        public String getCategoryId() {
            return categoryId;
        }

        public ShopItemInfo getItemInfo() {
            return itemInfo;
        }

        public int getPage() {
            return page;
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
            sender.sendMessage("§cThis command can only be executed by a player!");
            return true;
        }

        if (!player.hasPermission("vipshop.use")) {
            Component msg = mm.deserialize("<red>You do not have permission to use the shop!</red>");
            player.sendMessage(msg);
            return true;
        }

        openMainMenu(player);
        return true;
    }

    private ItemStack getPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.displayName(mm.deserialize("<gradient:#ffe066:#f5b041><bold>" + player.getName() + "</bold></gradient>")
                    .decoration(TextDecoration.ITALIC, false));
            
            double balance = 0;
            if (plugin.getEconomy() != null) {
                balance = plugin.getEconomy().getBalance(player);
            }
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<gray>Your Balance: </gray><green>$" + String.format(Locale.US, "%,.2f", balance) + "</green>").decoration(TextDecoration.ITALIC, false));
            lore.add(mm.deserialize("<gray>Status: </gray><gold>VIP Member</gold>").decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    public void openMainMenu(Player player) {
        Component title = mm.deserialize("<gradient:#4df2f2:#3a7bd5>✦ Shop Categories ✦</gradient>").decorate(TextDecoration.BOLD);
        Inventory inv = Bukkit.createInventory(new VIPShopHolder("MAIN", null), 54, title);

        ItemStack filler = createFillerGlass(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Add Player head in top middle slot (slot 4)
        inv.setItem(4, getPlayerHead(player));

        // Center row layout slots for categories
        // We will lay out the dynamic categories in row 3 (slots 20, 21, 22, 23, 24, 25)
        int[] slots = {19, 20, 21, 22, 23, 24, 25};
        int index = 0;
        for (CategoryInfo cat : plugin.getDynamicShopManager().getCategories()) {
            if (index >= slots.length) break;

            ItemStack catIcon = new ItemStack(cat.icon);
            ItemMeta meta = catIcon.getItemMeta();
            if (meta != null) {
                meta.displayName(mm.deserialize(cat.title).decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.add(mm.deserialize("<gray>Click to open this category.</gray>").decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
                catIcon.setItemMeta(meta);
            }
            inv.setItem(slots[index], catIcon);
            index++;
        }

        // Add 2 VIP static category icons in row 4 (slots 30, 32)
        // Category: Armor & Tools (Slot 30)
        ItemStack equipmentCategory = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta eqMeta = equipmentCategory.getItemMeta();
        if (eqMeta != null) {
            eqMeta.displayName(mm.deserialize("<gradient:#00aaff:#00ffff><bold>Armor, Weapons & Tools (VIP)</bold></gradient>").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<gray>Click to open the VIP armory.</gray>").decoration(TextDecoration.ITALIC, false));
            eqMeta.lore(lore);
            eqMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            equipmentCategory.setItemMeta(eqMeta);
        }
        inv.setItem(30, equipmentCategory);

        // Category: Magic Amulets (Slot 32)
        ItemStack amuletsCategory = new ItemStack(Material.NETHER_STAR);
        ItemMeta amMeta = amuletsCategory.getItemMeta();
        if (amMeta != null) {
            amMeta.displayName(mm.deserialize("<gradient:#aa00ff:#ff00aa><bold>Magic Amulets (VIP)</bold></gradient>").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<gray>Click to view VIP amulets.</gray>").decoration(TextDecoration.ITALIC, false));
            amMeta.lore(lore);
            amMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            amuletsCategory.setItemMeta(amMeta);
        }
        inv.setItem(32, amuletsCategory);

        player.openInventory(inv);
    }

    public void openCategoryMenu(Player player, String categoryId) {
        openCategoryMenu(player, categoryId, 1);
    }

    public void openCategoryMenu(Player player, String categoryId, int page) {
        CategoryInfo cat = plugin.getDynamicShopManager().getCategory(categoryId);
        if (cat == null) {
            openMainMenu(player);
            return;
        }

        int itemsPerPage = 36;
        List<ShopItemInfo> items = cat.items;
        int totalItems = items.size();
        int maxPage = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (maxPage == 0) maxPage = 1;
        if (page < 1) page = 1;
        if (page > maxPage) page = maxPage;

        Component title = mm.deserialize(cat.title + " <gray>(Strona " + page + "/" + maxPage + ")</gray>").decorate(TextDecoration.BOLD);
        Inventory inv = Bukkit.createInventory(new VIPShopHolder("CATEGORY", categoryId, null, page), 54, title);

        // Gray filler for boundaries
        ItemStack filler = createFillerGlass(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Add Player head in top middle slot (slot 4)
        inv.setItem(4, getPlayerHead(player));

        // Grid slots for items: slots 9 to 44 (4 rows of 9 slots = 36 items)
        int[] itemSlots = new int[36];
        for (int i = 0; i < 36; i++) {
            itemSlots[i] = 9 + i;
        }

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            inv.setItem(itemSlots[slotIndex], createShopItem(items.get(i)));
            slotIndex++;
        }

        // Previous Page button in slot 45
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.LIME_DYE);
            ItemMeta meta = prev.getItemMeta();
            if (meta != null) {
                meta.displayName(mm.deserialize("<green><bold>← Poprzednia Strona</bold></green>").decoration(TextDecoration.ITALIC, false));
                prev.setItemMeta(meta);
            }
            inv.setItem(45, prev);
        }

        // Next Page button in slot 53
        if (page < maxPage) {
            ItemStack next = new ItemStack(Material.LIME_DYE);
            ItemMeta meta = next.getItemMeta();
            if (meta != null) {
                meta.displayName(mm.deserialize("<green><bold>Następna Strona →</bold></green>").decoration(TextDecoration.ITALIC, false));
                next.setItemMeta(meta);
            }
            inv.setItem(53, next);
        }

        // Back button in slot 49
        ItemStack back = new ItemStack(Material.RED_DYE);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize("<red><bold>Wróć do Menu</bold></red>").decoration(TextDecoration.ITALIC, false));
            back.setItemMeta(meta);
        }
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    public void openQuantitySelectionMenu(Player player, ShopItemInfo item) {
        openQuantitySelectionMenu(player, item, 1);
    }

    public void openQuantitySelectionMenu(Player player, ShopItemInfo item, int quantity) {
        String englishName = getEnglishName(item.material);
        Component title = mm.deserialize("<gradient:#4df2f2:#3a7bd5>Ilość: " + englishName + " (" + quantity + " szt)</gradient>").decorate(TextDecoration.BOLD);
        Inventory inv = Bukkit.createInventory(new VIPShopHolder("QUANTITY", item.categoryId, item, quantity), 27, title);

        // Fill with border glass
        ItemStack filler = createFillerGlass(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Player Head at top middle
        inv.setItem(4, getPlayerHead(player));

        // Adjust quantity buttons on the left
        inv.setItem(9, createAmountAdjustButton(Material.RED_STAINED_GLASS_PANE, "<red><bold>-64</bold></red>"));
        inv.setItem(10, createAmountAdjustButton(Material.RED_STAINED_GLASS, "<red><bold>-16</bold></red>"));
        inv.setItem(11, createAmountAdjustButton(Material.RED_DYE, "<red><bold>-1</bold></red>"));

        // Center item
        inv.setItem(13, createQuantityDisplayItem(item, quantity));

        // Adjust quantity buttons on the right
        inv.setItem(15, createAmountAdjustButton(Material.LIME_DYE, "<green><bold>+1</bold></green>"));
        inv.setItem(16, createAmountAdjustButton(Material.LIME_STAINED_GLASS, "<green><bold>+16</bold></green>"));
        inv.setItem(17, createAmountAdjustButton(Material.LIME_STAINED_GLASS_PANE, "<green><bold>+64</bold></green>"));

        // Confirm buttons at the bottom row (slots 18-26)
        double currentBuy = plugin.getDynamicShopManager().getCurrentBuyPrice(item.materialName);
        double currentSell = plugin.getDynamicShopManager().getCurrentSellPrice(item.materialName);

        // Confirm Buy button (slot 20)
        if (item.baseBuy > 0) {
            double totalCost = currentBuy * quantity;
            inv.setItem(20, createConfirmButton(Material.LIME_WOOL, "<green><bold>Potwierdź Zakup</bold></green>", totalCost, true));
        } else {
            inv.setItem(20, createConfirmButton(Material.BARRIER, "<red>Zakup Zablokowany</red>", 0, true));
        }

        // Back button (slot 22)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize("<red><bold>Wróć do Sklepu</bold></red>").decoration(TextDecoration.ITALIC, false));
            back.setItemMeta(meta);
        }
        inv.setItem(22, back);

        // Confirm Sell button (slot 24)
        double totalEarnings = currentSell * quantity;
        inv.setItem(24, createConfirmButton(Material.RED_WOOL, "<red><bold>Potwierdź Sprzedaż</bold></red>", totalEarnings, false));

        // Sell All button (slot 25)
        int totalInInventory = 0;
        for (ItemStack invItem : player.getInventory().getStorageContents()) {
            if (invItem != null && invItem.getType() == item.material) {
                totalInInventory += invItem.getAmount();
            }
        }
        double totalSellAllEarnings = currentSell * totalInInventory;
        inv.setItem(25, createSellAllButton(totalInInventory, totalSellAllEarnings));

        player.openInventory(inv);
    }

    private ItemStack createAmountAdjustButton(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize(name).decoration(TextDecoration.ITALIC, false));
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createConfirmButton(Material mat, String name, double total, boolean isBuy) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize(name).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            if (mat != Material.BARRIER) {
                String label = isBuy ? "Koszt całkowity: " : "Zarobek całkowity: ";
                lore.add(mm.deserialize("<gray>" + label + "</gray><green>$" + String.format(Locale.US, "%,.2f", total) + "</green>").decoration(TextDecoration.ITALIC, false));
                lore.add(Component.empty());
                lore.add(mm.deserialize("<gray>Kliknij, aby zatwierdzić transakcję.</gray>").decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(mm.deserialize("<gray>Tego przedmiotu nie można kupić.</gray>").decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSellAllButton(int count, double totalEarnings) {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize("<red><bold>Sprzedaj Wszystko</bold></red>").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<gray>Posiadasz w eq: </gray><aqua>" + count + " szt</aqua>").decoration(TextDecoration.ITALIC, false));
            lore.add(mm.deserialize("<gray>Łączny zarobek: </gray><green>$" + String.format(Locale.US, "%,.2f", totalEarnings) + "</green>").decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(mm.deserialize("<gray>Kliknij, aby sprzedać wszystkie sztuki z eq.</gray>").decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createQuantityDisplayItem(ShopItemInfo item, int quantity) {
        ItemStack stack = new ItemStack(item.material, quantity);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        double currentBuy = plugin.getDynamicShopManager().getCurrentBuyPrice(item.materialName);
        double currentSell = plugin.getDynamicShopManager().getCurrentSellPrice(item.materialName);

        String displayName = getEnglishName(item.material);
        meta.displayName(mm.deserialize("<gradient:#ffe066:#f5b041><bold>" + displayName + "</bold></gradient>")
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(mm.deserialize("<gray>Wybrana ilość: </gray><yellow><b>" + quantity + " szt</b></yellow>").decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        
        lore.add(mm.deserialize("<yellow>Ceny jednostkowe:</yellow>").decoration(TextDecoration.ITALIC, false));
        if (item.baseBuy > 0) {
            lore.add(mm.deserialize(" <gray>• Kupno: </gray><green>$" + String.format(Locale.US, "%.2f", currentBuy) + "</green>").decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(mm.deserialize(" <gray>• Kupno: </gray><red>Zablokowane</red>").decoration(TextDecoration.ITALIC, false));
        }
        lore.add(mm.deserialize(" <gray>• Sprzedaż: </gray><green>$" + String.format(Locale.US, "%.2f", currentSell) + "</green>").decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(meta);
        return stack;
    }

    // ==========================================
    // VIP Shop Submenus (Original static VIP items)
    // ==========================================

    public void openEquipmentCategory(Player player) {
        Component title = mm.deserialize("<gradient:#ffaa00:#ffff55>✦ VIP Armory ✦</gradient>").decorate(TextDecoration.BOLD);
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
            meta.displayName(mm.deserialize("<red><bold>Go Back</bold></red>").decoration(TextDecoration.ITALIC, false));
            back.setItemMeta(meta);
        }
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    public void openAmuletsCategory(Player player) {
        Component title = mm.deserialize("<gradient:#aa00ff:#ff00aa>✦ VIP Amulets ✦</gradient>").decorate(TextDecoration.BOLD);
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
            meta.displayName(mm.deserialize("<red><bold>Go Back</bold></red>").decoration(TextDecoration.ITALIC, false));
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
        Component priceComponent = mm.deserialize("<yellow><bold>Price: $" + String.format(Locale.US, "%,.0f", price) + "</bold></yellow>").decoration(TextDecoration.ITALIC, false);
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

        String displayName = getEnglishName(item.material);
        meta.displayName(mm.deserialize("<gradient:#ffe066:#f5b041><bold>" + displayName + "</bold></gradient>")
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        
        // Buy Section
        lore.add(mm.deserialize("<yellow><b>Buy:</b></yellow>").decoration(TextDecoration.ITALIC, false));
        if (item.baseBuy > 0) {
            lore.add(mm.deserialize(" <gray>• 1x: </gray><green>$" + String.format(Locale.US, "%.2f", currentBuy) + "</green>").decoration(TextDecoration.ITALIC, false));
            lore.add(mm.deserialize(" <gray>• 64x: </gray><green>$" + String.format(Locale.US, "%.2f", currentBuy * 64) + "</green>").decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(mm.deserialize(" <gray>• Kupno: </gray><red>Zablokowane</red>").decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());

        // Sell Section
        lore.add(mm.deserialize("<gold><b>Sell:</b></gold>").decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize(" <gray>• 1x: </gray><green>$" + String.format(Locale.US, "%.2f", currentSell) + "</green>").decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize(" <gray>• 64x: </gray><green>$" + String.format(Locale.US, "%.2f", currentSell * 64) + "</green>").decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        // Dynamic pricing status
        lore.add(mm.deserialize("<blue><b>Market Demand:</b></blue>").decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize(" <gray>• Recent Sales: </gray><aqua>" + String.format(Locale.US, "%.0f", salesCount) + "</aqua>").decoration(TextDecoration.ITALIC, false));
        
        if (dropPercent > 0.1) {
            lore.add(mm.deserialize(" <gray>• Price Status: </gray><red>-" + String.format(Locale.US, "%.0f", dropPercent) + "% (Overproduction)</red>").decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(mm.deserialize(" <gray>• Price Status: </gray><green>Stable (100%)</green>").decoration(TextDecoration.ITALIC, false));
        }
        
        lore.add(Component.empty());
        lore.add(mm.deserialize("<dark_gray>-------------------------</dark_gray>").decoration(TextDecoration.ITALIC, false));
        lore.add(mm.deserialize("<yellow>[Click]</yellow> <gray>Open Transaction Menu</gray>").decoration(TextDecoration.ITALIC, false));

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

    private String getEnglishName(Material material) {
        String name = material.name().replace("_", " ").toLowerCase();
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
