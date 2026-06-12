package pl.lukas.legitcheck.listener;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import pl.lukas.legitcheck.LegitCheckPlugin;
import pl.lukas.legitcheck.manager.CheckManager;

public class GuiListener implements Listener {
    private final LegitCheckPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public GuiListener(LegitCheckPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin)) {
            return;
        }

        // Get plain text title of the inventory
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.startsWith("Checking: ")) {
            event.setCancelled(true); // Prevent item taking

            // Determine target player name
            String targetName = title.substring("Checking: ".length()).trim();
            Player target = Bukkit.getPlayer(targetName);

            if (target == null) {
                admin.sendMessage(mm.deserialize("<red>The target player is no longer online.</red>"));
                admin.closeInventory();
                return;
            }

            CheckManager manager = plugin.getCheckManager();
            int slot = event.getRawSlot();

            switch (slot) {
                case 0 -> { // Slot 0: Permanent Ban
                    admin.closeInventory();
                    String cmd = "ban " + targetName + " Cheats - Refused/Detected";
                    manager.banPlayerFromGui(target, admin, cmd);
                }
                case 2 -> { // Slot 2: Temp Ban 21 Days
                    admin.closeInventory();
                    String cmd = "tempban " + targetName + " 21d Cheats";
                    manager.banPlayerFromGui(target, admin, cmd);
                }
                case 4 -> { // Slot 4: Temp Ban 14 Days
                    admin.closeInventory();
                    String cmd = "tempban " + targetName + " 14d Cheats";
                    manager.banPlayerFromGui(target, admin, cmd);
                }
                case 6 -> { // Slot 6: Temp Ban 7 Days
                    admin.closeInventory();
                    String cmd = "tempban " + targetName + " 7d Cheating - Admission of guilt";
                    manager.banPlayerFromGui(target, admin, cmd);
                }
                case 8 -> { // Slot 8: Clean
                    admin.closeInventory();
                    manager.cleanPlayer(target, admin);
                }
            }
        }
    }
}
