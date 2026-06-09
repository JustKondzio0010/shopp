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

import java.util.ArrayList;
import java.util.List;

public final class VIPShopCommand implements CommandExecutor {

    private final VipShop plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public static final class VIPShopHolder implements InventoryHolder {
        private final String menuType; // "MAIN", "EQUIPMENT", "AMULETS"
        
        public VIPShopHolder(String menuType) {
            this.menuType = menuType;
        }
        
        public String getMenuType() {
            return menuType;
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
            Component msg = mm.deserialize("<red>Nie masz uprawnień do korzystania ze sklepu VIP!</red>");
            player.sendMessage(msg);
            return true;
        }

        openMainMenu(player);
        return true;
    }

    public void openMainMenu(Player player) {
        Component title = mm.deserialize("<gradient:#ffaa00:#ffff55>✦ Kategorie VIP ✦</gradient>").decorate(TextDecoration.BOLD);
        Inventory inv = Bukkit.createInventory(new VIPShopHolder("MAIN"), 27, title);

        ItemStack filler = createFillerGlass(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Kategoria: Zbroje i Bronie
        ItemStack equipmentCategory = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta eqMeta = equipmentCategory.getItemMeta();
        if (eqMeta != null) {
            eqMeta.displayName(mm.deserialize("<gradient:#00aaff:#00ffff><bold>Zbroje, Bronie i Narzędzia</bold></gradient>").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<gray>Kliknij, aby otworzyć zbrojownię.</gray>").decoration(TextDecoration.ITALIC, false));
            eqMeta.lore(lore);
            eqMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            equipmentCategory.setItemMeta(eqMeta);
        }
        inv.setItem(11, equipmentCategory);

        // Kategoria: Amulety
        ItemStack amuletsCategory = new ItemStack(Material.NETHER_STAR);
        ItemMeta amMeta = amuletsCategory.getItemMeta();
        if (amMeta != null) {
            amMeta.displayName(mm.deserialize("<gradient:#aa00ff:#ff00aa><bold>Magiczne Amulety</bold></gradient>").decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(mm.deserialize("<gray>Kliknij, aby zobaczyć potężne amulety.</gray>").decoration(TextDecoration.ITALIC, false));
            amMeta.lore(lore);
            amMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            amuletsCategory.setItemMeta(amMeta);
        }
        inv.setItem(15, amuletsCategory);

        player.openInventory(inv);
    }

    public void openEquipmentCategory(Player player) {
        Component title = mm.deserialize("<gradient:#ffaa00:#ffff55>✦ Zbrojownia VIP ✦</gradient>").decorate(TextDecoration.BOLD);
        Inventory inv = Bukkit.createInventory(new VIPShopHolder("EQUIPMENT"), 54, title);

        ItemStack filler = createFillerGlass(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Sylwetka gracza - Zbroje
        setItemInSlot(inv, 2, "titan_helmet");
        setItemInSlot(inv, 3, "nightvision_helmet");
        setItemInSlot(inv, 11, "titan_chestplate");
        setItemInSlot(inv, 12, "magma_chestplate");
        setItemInSlot(inv, 20, "titan_leggings");
        setItemInSlot(inv, 21, "bottomless_backpack");
        setItemInSlot(inv, 29, "titan_boots");
        setItemInSlot(inv, 30, "hermes_boots");

        // Narzędzia
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

        // Miecze
        setItemInSlot(inv, 37, "collector_sword");
        setItemInSlot(inv, 38, "butcher_sword");
        setItemInSlot(inv, 39, "titan_sword");
        setItemInSlot(inv, 40, "reaper_scythe");
        setItemInSlot(inv, 41, "vampire_dagger");
        setItemInSlot(inv, 42, "headhunter_sword");

        // Wróć
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize("<red><bold>Wróć do Menu</bold></red>").decoration(TextDecoration.ITALIC, false));
            back.setItemMeta(meta);
        }
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    public void openAmuletsCategory(Player player) {
        Component title = mm.deserialize("<gradient:#aa00ff:#ff00aa>✦ Amulety VIP ✦</gradient>").decorate(TextDecoration.BOLD);
        Inventory inv = Bukkit.createInventory(new VIPShopHolder("AMULETS"), 54, title);

        ItemStack filler = createFillerGlass(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Amulety wyśrodkowane w siatce 7x2
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

        // Wróć
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize("<red><bold>Wróć do Menu</bold></red>").decoration(TextDecoration.ITALIC, false));
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

    private ItemStack createFillerGlass(Material mat) {
        ItemStack glass = new ItemStack(mat);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            glass.setItemMeta(meta);
        }
        return glass;
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
        Component priceComponent = mm.deserialize("<yellow><bold>Cena: $" + String.format("%,.0f", price) + "</bold></yellow>").decoration(TextDecoration.ITALIC, false);
        newLore.add(priceComponent);

        meta.lore(newLore);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    // Pozostale opisy przedmiotów zostały przeniesione z twardego kodu do CustomItemManager
    // Poniewaz w CustomItemManager już mamy świetne lore (wymagania z dzisiaj),
    // usunąłem nadpisywanie lore w getFancyDescription() aby uzyc natywnych lore z Managera!
    // W zasadzie CustomItemManager juz robi nam swietne lore!



    private String getFancyDescription(String id) {
        // Zwracamy puste, ponieważ pełny lore ładujemy bezpośrednio z CustomItemManager.java. 
        // Zapobiega to podwójnemu kodowaniu statystyk w dwóch miejscach naraz.
        return "";
    }

    private List<String> getFancyStats(String id) {
        // Wszystkie statystyki i opisy pobieramy bezpośrednio z CustomItemManager!
        pl.konrad.vipshop.items.CustomItemInfo info = plugin.getCustomItemManager().getItemInfo(id);
        if (info == null) return List.of();
        
        List<String> stats = new ArrayList<>();
        for (String loreLine : info.lore) {
            // Zamiana natywnych kodów koloru na minimessage dla stylizacji
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
}
