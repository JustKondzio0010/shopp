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
        if (plugin.getEconomyAuditor().isFrozen(player.getUniqueId())) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.showTitle(Title.title(
                mm.deserialize("<red><bold>LOCKED!</bold></red>"),
                mm.deserialize("<gray>Your account is frozen by the Auditor.</gray>"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            player.closeInventory();
            return;
        }

        // Vault check
        if (plugin.getEconomy() == null) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(mm.deserialize("<red>Error: Vault economy system not found on the server!</red>"));
            return;
        }

        // ==========================================
        // 1. MAIN MENU NAVIGATION
        // ==========================================
        if (menuType.equals("MAIN")) {
            List<CategoryInfo> list = new ArrayList<>(plugin.getDynamicShopManager().getCategories());
            CategoryInfo selected = null;
            int[] slots = {19, 20, 21, 22, 23, 24, 25};
            for (int i = 0; i < slots.length; i++) {
                if (slot == slots[i] && list.size() > i) {
                    selected = list.get(i);
                    break;
                }
            }

            if (selected != null) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new VIPShopCommand(plugin).openCategoryMenu(player, selected.id);
                return;
            }

            // VIP submenus
            if (slot == 30) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new VIPShopCommand(plugin).openEquipmentCategory(player);
            } else if (slot == 32) {
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
                    mm.deserialize("<red><bold>INSUFFICIENT FUNDS!</bold></red>"),
                    mm.deserialize("<gray>You need <yellow>$" + String.format(Locale.US, "%,.0f", price) + "</yellow></gray>"),
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
                mm.deserialize("<green><bold>PURCHASED!</bold></green>"),
                mm.deserialize("<gray>Successfully purchased VIP item.</gray>"),
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

            // Previous page (Lime dye at slot 45)
            if (slot == 45) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && clicked.getType() == Material.LIME_DYE) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new VIPShopCommand(plugin).openCategoryMenu(player, holder.getCategoryId(), holder.getPage() - 1);
                    return;
                }
            }

            // Next page (Lime dye at slot 53)
            if (slot == 53) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && clicked.getType() == Material.LIME_DYE) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new VIPShopCommand(plugin).openCategoryMenu(player, holder.getCategoryId(), holder.getPage() + 1);
                    return;
                }
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

            // Instead of buying/selling immediately, open the Quantity Selection GUI
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            new VIPShopCommand(plugin).openQuantitySelectionMenu(player, itemInfo);
            return;
        }

        // ==========================================
        // 4. QUANTITY SELECTION MENU
        // ==========================================
        if (menuType.equals("QUANTITY")) {
            // Back button
            if (slot == 22) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                new VIPShopCommand(plugin).openCategoryMenu(player, holder.getCategoryId());
                return;
            }

            ShopItemInfo itemInfo = holder.getItemInfo();
            if (itemInfo == null) return;

            // Handle Quantity adjustments (-64, -16, -1, +1, +16, +64)
            // Slots: 9, 10, 11, 15, 16, 17
            if (slot == 9 || slot == 10 || slot == 11 || slot == 15 || slot == 16 || slot == 17) {
                int currentQuantity = holder.getPage(); // current quantity is stored in the holder's page property
                int diff = 0;
                if (slot == 9) diff = -64;
                else if (slot == 10) diff = -16;
                else if (slot == 11) diff = -1;
                else if (slot == 15) diff = 1;
                else if (slot == 16) diff = 16;
                else if (slot == 17) diff = 64;

                int newQuantity = currentQuantity + diff;
                int maxStack = Math.max(1, itemInfo.material.getMaxStackSize());
                if (newQuantity < 1) newQuantity = 1;
                if (newQuantity > maxStack) newQuantity = maxStack;

                if (newQuantity != currentQuantity) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    new VIPShopCommand(plugin).openQuantitySelectionMenu(player, itemInfo, newQuantity);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.5f);
                }
                return;
            }

            // Confirm Buy (slot 20)
            if (slot == 20) {
                if (itemInfo.baseBuy <= 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(mm.deserialize("<red>Tego przedmiotu nie można kupić!</red>"));
                    return;
                }

                int quantity = holder.getPage();
                double unitBuyPrice = plugin.getDynamicShopManager().getCurrentBuyPrice(itemInfo.materialName);
                double totalCost = unitBuyPrice * quantity;

                // Check money balance
                if (!plugin.getEconomy().has(player, totalCost)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(mm.deserialize("<red>Insufficient funds! You need $" + String.format(Locale.US, "%.2f", totalCost) + "</red>"));
                    return;
                }

                // Check inventory capacity precisely
                int freeSpace = 0;
                for (ItemStack invItem : player.getInventory().getStorageContents()) {
                    if (invItem == null || invItem.getType() == Material.AIR) {
                        freeSpace += Math.max(1, itemInfo.material.getMaxStackSize());
                    } else if (invItem.getType() == itemInfo.material) {
                        freeSpace += (Math.max(1, invItem.getMaxStackSize()) - invItem.getAmount());
                    }
                }

                if (freeSpace < quantity) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(mm.deserialize("<red>Your inventory is full!</red>"));
                    return;
                }

                // Complete buy transaction
                plugin.getEconomy().withdrawPlayer(player, totalCost);
                player.getInventory().addItem(new ItemStack(itemInfo.material, quantity));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                player.sendMessage(mm.deserialize("<green>Successfully purchased " + quantity + "x for $" + String.format(Locale.US, "%.2f", totalCost) + "</green>"));

                // Refresh GUI to show updated balance and details
                new VIPShopCommand(plugin).openQuantitySelectionMenu(player, itemInfo, quantity);
                return;
            }

            // Confirm Sell (slot 24)
            if (slot == 24) {
                int quantity = holder.getPage();

                // Count how many items of this type the player has
                int playerAmount = 0;
                for (ItemStack item : player.getInventory().getStorageContents()) {
                    if (item != null && item.getType() == itemInfo.material) {
                        playerAmount += item.getAmount();
                    }
                }

                if (playerAmount < quantity) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(mm.deserialize("<red>You do not have enough of this item in your inventory!</red>"));
                    return;
                }

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

                // Complete sell transaction & register sale
                plugin.getEconomy().depositPlayer(player, totalEarnings);
                plugin.getDynamicShopManager().registerSale(itemInfo.materialName, quantity);

                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);
                
                double newPrice = plugin.getDynamicShopManager().getCurrentSellPrice(itemInfo.materialName);
                player.sendMessage(mm.deserialize("<gold>Sold " + quantity + "x for $" + String.format(Locale.US, "%.2f", totalEarnings) + " <gray>(New unit price: $" + String.format(Locale.US, "%.2f", newPrice) + ")</gray></gold>"));

                // Refresh GUI (clamp quantity if player has fewer items now)
                int newPlayerAmount = playerAmount - quantity;
                int newQuantity = Math.max(1, Math.min(quantity, newPlayerAmount));
                new VIPShopCommand(plugin).openQuantitySelectionMenu(player, itemInfo, newQuantity);
                return;
            }

            // Sell All (slot 25)
            if (slot == 25) {
                // Count how many items of this type the player has
                int playerAmount = 0;
                for (ItemStack item : player.getInventory().getStorageContents()) {
                    if (item != null && item.getType() == itemInfo.material) {
                        playerAmount += item.getAmount();
                    }
                }

                if (playerAmount <= 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    player.sendMessage(mm.deserialize("<red>You do not have any of this item in your inventory!</red>"));
                    return;
                }

                double unitSellPrice = plugin.getDynamicShopManager().getCurrentSellPrice(itemInfo.materialName);
                double totalEarnings = unitSellPrice * playerAmount;

                // Remove items from player inventory
                ItemStack[] contents = player.getInventory().getStorageContents();
                for (int i = 0; i < contents.length; i++) {
                    ItemStack invItem = contents[i];
                    if (invItem != null && invItem.getType() == itemInfo.material) {
                        contents[i] = null;
                    }
                }
                player.getInventory().setStorageContents(contents);

                // Complete sell transaction & register sale
                plugin.getEconomy().depositPlayer(player, totalEarnings);
                plugin.getDynamicShopManager().registerSale(itemInfo.materialName, playerAmount);

                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.8f);
                
                double newPrice = plugin.getDynamicShopManager().getCurrentSellPrice(itemInfo.materialName);
                player.sendMessage(mm.deserialize("<gold>Sold " + playerAmount + "x for $" + String.format(Locale.US, "%.2f", totalEarnings) + " <gray>(New unit price: $" + String.format(Locale.US, "%.2f", newPrice) + ")</gray></gold>"));

                // Refresh GUI, resetting quantity to 1
                new VIPShopCommand(plugin).openQuantitySelectionMenu(player, itemInfo, 1);
                return;
            }
        }
    }
}
