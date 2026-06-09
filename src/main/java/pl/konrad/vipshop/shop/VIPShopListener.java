package pl.konrad.vipshop.shop;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import pl.konrad.vipshop.VipShop;
import pl.konrad.vipshop.items.CustomItemInfo;

import java.time.Duration;

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

        // Obsługa Menu Głównego
        if (menuType.equals("MAIN")) {
            if (slot == 11) {
                VIPShopCommand cmd = new VIPShopCommand(plugin);
                cmd.openEquipmentCategory(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            } else if (slot == 15) {
                VIPShopCommand cmd = new VIPShopCommand(plugin);
                cmd.openAmuletsCategory(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            }
            return; // Nic się nie kupuje w Menu
        }

        // Strzałka Wstecz dla innych podmenu
        if (slot == 49) {
            VIPShopCommand cmd = new VIPShopCommand(plugin);
            cmd.openMainMenu(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            return;
        }

        // Zakup przedmiotu
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().name().contains("GLASS_PANE")) {
            return;
        }

        String itemId = plugin.getCustomItemManager().getItemId(clicked);
        if (itemId == null) {
            return;
        }

        CustomItemInfo info = plugin.getCustomItemManager().getItemInfo(itemId);
        if (info == null) {
            return;
        }

        // Sprawdzenie czy gracz jest zamrożony
        if (plugin.getEconomyAuditor().isFrozen(player.getUniqueId())) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.showTitle(Title.title(
                mm.deserialize("<red><bold>ZABLOKOWANE!</bold></red>"),
                mm.deserialize("<gray>Twoje konto jest zamrożone.</gray>"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            player.closeInventory();
            return;
        }

        // Sprawdzenie integracji z Vault
        if (plugin.getEconomy() == null) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.showTitle(Title.title(
                mm.deserialize("<red><bold>BŁĄD!</bold></red>"),
                mm.deserialize("<gray>Brak systemu ekonomii na serwerze.</gray>"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            return;
        }

        double price = plugin.getConfig().getDouble("shop." + itemId, 10000.0);
        if (!plugin.getEconomy().has(player, price)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.showTitle(Title.title(
                mm.deserialize("<red><bold>BRAK ŚRODKÓW!</bold></red>"),
                mm.deserialize("<gray>Potrzebujesz <yellow>$" + String.format("%,.0f", price) + "</yellow></gray>"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
            return;
        }

        // Transakcja
        plugin.getEconomy().withdrawPlayer(player, price);
        
        ItemStack purchasedItem = plugin.getCustomItemManager().createItem(itemId);
        if (purchasedItem == null) return;
        
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), purchasedItem);
        } else {
            player.getInventory().addItem(purchasedItem);
        }

        // Dźwięk i ekran po zakupie
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        
        player.showTitle(Title.title(
            mm.deserialize("<green><bold>ZAKUPIONO!</bold></green>"),
            mm.deserialize("<gray>Pomyślnie kupiono przedmiot.</gray>"),
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(500))
        ));
    }
}
