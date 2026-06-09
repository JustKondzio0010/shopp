package pl.konrad.vipshop.shop;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import pl.konrad.vipshop.VipShop;
import pl.konrad.vipshop.items.CustomItemInfo;
import pl.konrad.vipshop.shop.DynamicShopManager.CategoryInfo;
import pl.konrad.vipshop.shop.DynamicShopManager.ShopItemInfo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VIPShopListener implements Listener {

    private final VipShop plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public VIPShopListener(VipShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShopClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof VIPShopCommand.VIPShopHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        String menuType = holder.getMenuType();

        // Security check: is player frozen by economy auditor?
        // (Applies to all shop menus to prevent frozen player exploitation)
        if (plugin.getEconomyAuditor().isFrozen(player.getUniqueId())) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.showTitle(Title.title(
                mm.deserialize("<red><bold>ZABLOKOWANE!</bold></red>"),
                mm.deserialize("<gray>Twoje konto jest zamrożone przez Audytora.</gray>"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            player.closeInventory();
            return;
        }

        // Vault check
        if (plugin.getEconomy() == null) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(mm.deserialize("<red>Błąd: Brak systemu ekonomii Vault na serwerze!</red>"));
            return;
        }

        // ==========================================
        // 1. MAIN MENU NAVIGATION
        // ==========================================
        if (menuType.equals("MAIN")) {
            List<CategoryInfo> list = new ArrayList<>(plugin.getDynamicShopManager().getCategories());
            CategoryInfo selected = null;
            if (slot == 10 && list.size() > 0) selected = list.get(0);
            else if (slot == 12 && list.size() > 1) selected = list.get(1);
            else if (slot == 14 && list.size() > 2) selected = list.get(2);
            else if (slot == 16 && list.size() > 3) selected = list.get(3);

            if (selected != null) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new VIPShopCommand(plugin).openCategoryMenu(player, selected.id);
                return;
            }

            // VIP submenus
            if (slot == 21) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new VIPShopCommand(plugin).openEquipmentCategory(player);
            } else if (slot == 23) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new VIPShopCommand(plugin).openAmuletsCategory(player);
            }
            return;
        }

        // ==========================================
        // 2. VIP STATIC SHOP MENU (EQUIPMENT & AMULETS)
        // ==========================================
        if (menuType.equals("EQUIPMENT") || menuType.equals("AMULETS")) {
            // Return arrow
            if (slot == 49) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new VIPShopCommand(plugin).openMainMenu(player);
                return;
            }

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().name().contains("GLASS_PANE")) {
                return;
            }

            // Retrieve custom item ID
            String itemId = plugin.getCustomItemManager().getItemId(clicked);
            if (itemId == null) {
                return;
            }

            CustomItemInfo info = plugin.getCustomItemManager().getItemInfo(itemId);
            if (info == null) {
                return;
            }

            double price = plugin.getConfig().getDouble("shop." + itemId, 10000.0);
            if (!plugin.getEconomy().has(player, price)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.showTitle(Title.title(
                    mm.deserialize("<red><bold>BRAK ŚRODKÓW!</bold></red>"),
                    mm.deserialize("<gray>Potrzebujesz <yellow>$" + String.format(Locale.US, "%,.0f", price) + "</yellow></gray>"),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
                ));
                return;
            }

            // Deduct funds
            plugin.getEconomy().withdrawPlayer(player, price);
            
            ItemStack purchasedItem = plugin.getCustomItemManager().createItem(itemId);
            if (purchasedItem == null) return;
            
            // Deliver item
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItemNaturally(player.getLocation(), purchasedItem);
            } else {
                player.getInventory().addItem(purchasedItem);
            }

            // Success feedback
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            
            player.showTitle(Title.title(
                mm.deserialize("<green><bold>ZAKUPIONO!</bold></green>"),
                mm.deserialize("<gray>Pomyślnie kupiono przedmiot VIP.</gray>"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            return;
        }

        // ==========================================
        // 3. DYNAMIC ECONOMY CATEGORY MENU
        // ==========================================
        if (menuType.equals("CATEGORY")) {
            // Return arrow
            if (slot == 49) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new VIPShopCommand(plugin).openMainMenu(player);
                return;
            }

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || clicked.getType().name().contains("GLASS_PANE")) {
                return;
            }

            // Identify dynamic item info
            ShopItemInfo itemInfo = plugin.getDynamicShopManager().getItemInfo(clicked.getType().name());
            if (itemInfo == null || !itemInfo.categoryId.equals(holder.getCategoryId())) {
                return;
            }

            ClickType clickType = event.getClick();

            // Handle Buying (Left Click / Shift Left Click)
            if (clickType == ClickType.LEFT || clickType == ClickType.SHIFT_LEFT) {
                int quantity = (clickType == ClickType.SHIFT_LEFT) ? 64 : 1;
                double unitBuyPrice = plugin.getDynamicShopManager().getCurrentBuyPrice(itemInfo.materialName);
                double totalCost = unitBuyPrice * quantity;

                // Check money balance
                if (!plugin.getEconomy().has(player, totalCost)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendActionBar(mm.deserialize("<red>Brak środków! Potrzebujesz $" + String.format(Locale.US, "%.2f", totalCost) + "</red>"));
                    return;
                }

                // Check inventory capacity
                if (player.getInventory().firstEmpty() == -1) {
                    boolean hasSpace = false;
                    for (ItemStack item : player.getInventory().getStorageContents()) {
                        if (item != null && item.getType() == itemInfo.material && item.getAmount() < item.getMaxStackSize()) {
                            hasSpace = true;
                            break;
                        }
                    }
                    if (!hasSpace) {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        player.sendActionBar(mm.deserialize("<red>Brak wolnego miejsca w ekwipunku!</red>"));
                        return;
                    }
                }

                // Complete buy transaction
                plugin.getEconomy().withdrawPlayer(player, totalCost);
                player.getInventory().addItem(new ItemStack(itemInfo.material, quantity));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                player.sendActionBar(mm.deserialize("<green>Zakupiono " + quantity + " szt. za $" + String.format(Locale.US, "%.2f", totalCost) + "</green>"));

                // Refresh GUI
                new VIPShopCommand(plugin).openCategoryMenu(player, holder.getCategoryId());
            }

            // Handle Selling (Right Click / Shift Right Click)
            else if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
                // Count how many items of this type the player has
                int playerAmount = 0;
                for (ItemStack item : player.getInventory().getStorageContents()) {
                    if (item != null && item.getType() == itemInfo.material) {
                        playerAmount += item.getAmount();
                    }
                }

                if (playerAmount <= 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendActionBar(mm.deserialize("<red>Nie posiadasz tego przedmiotu w ekwipunku!</red>"));
                    return;
                }

                int quantity = (clickType == ClickType.SHIFT_RIGHT) ? playerAmount : 1;
                double unitSellPrice = plugin.getDynamicShopManager().getCurrentSellPrice(itemInfo.materialName);
                double totalEarnings = unitSellPrice * quantity;

                // Remove items from player inventory
                int remainingToRemove = quantity;
                ItemStack[] contents = player.getInventory().getStorageContents();
                for (int i = 0; i < contents.length; i++) {
                    ItemStack invItem = contents[i];
                    if (invItem != null && invItem.getType() == itemInfo.material) {
                        int amt = invItem.getAmount();
                        if (amt <= remainingToRemove) {
                            remainingToRemove -= amt;
                            contents[i] = null;
                        } else {
                            invItem.setAmount(amt - remainingToRemove);
                            remainingToRemove = 0;
                            break;
                        }
                    }
                }
                player.getInventory().setStorageContents(contents);

                // Complete sell transaction & register to increase supply (dropping price)
                plugin.getEconomy().depositPlayer(player, totalEarnings);
                plugin.getDynamicShopManager().registerSale(itemInfo.materialName, quantity);

                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);
                
                double newPrice = plugin.getDynamicShopManager().getCurrentSellPrice(itemInfo.materialName);
                player.sendActionBar(mm.deserialize("<gold>Sprzedano " + quantity + " szt. za $" + String.format(Locale.US, "%.2f", totalEarnings) + " <gray>(Nowa cena: $" + String.format(Locale.US, "%.2f", newPrice) + "/szt.)</gray></gold>"));

                // Refresh GUI
                new VIPShopCommand(plugin).openCategoryMenu(player, holder.getCategoryId());
            }
        }
    }
}
