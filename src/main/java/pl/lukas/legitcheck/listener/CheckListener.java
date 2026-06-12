package pl.lukas.legitcheck.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import pl.lukas.legitcheck.LegitCheckPlugin;
import pl.lukas.legitcheck.manager.CheckManager;
import pl.lukas.legitcheck.model.CheckSession;

import java.util.UUID;

public class CheckListener implements Listener {
    private final LegitCheckPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CheckListener(LegitCheckPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        CheckManager manager = plugin.getCheckManager();

        if (manager.isBeingChecked(player.getUniqueId())) {
            Location from = event.getFrom();
            Location to = event.getTo();

            // Freeze the player's coordinate movement but allow looking around
            if (from.getX() != to.getX() || from.getZ() != to.getZ() || from.getY() != to.getY()) {
                Location newLoc = from.clone();
                newLoc.setYaw(to.getYaw());
                newLoc.setPitch(to.getPitch());
                event.setTo(newLoc);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CheckManager manager = plugin.getCheckManager();

        if (manager.isBeingChecked(player.getUniqueId())) {
            CheckSession session = manager.getSessionByPlayer(player.getUniqueId());
            if (session != null) {
                // Execute command from console to ban player permanently
                String banCommand = "ban " + player.getName() + " Leaving during investigation";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), banCommand);

                // Notify server/staff
                Bukkit.broadcast(
                        mm.deserialize("<gray>[<red>LegitCheck</red>] Player <gold>" + player.getName() + "</gold> logged out during investigation and has been <red>permanently banned</red>.</gray>"),
                        "legitcheck.notify"
                );

                // Clean up session (without teleporting back since they quit)
                manager.removeSessionWithoutTeleport(player.getUniqueId());

                // Inform the checking admin
                Player admin = Bukkit.getPlayer(session.getAdminUuid());
                if (admin != null && admin.isOnline()) {
                    admin.sendMessage(mm.deserialize("<red>The player has logged out and has been banned.</red>"));
                    // Teleport admin back
                    admin.teleport(session.getOriginalAdminLocation());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        CheckManager manager = plugin.getCheckManager();
        UUID senderUuid = sender.getUniqueId();

        // 1. If sender is the player being checked
        if (manager.isBeingChecked(senderUuid)) {
            event.setCancelled(true);
            CheckSession session = manager.getSessionByPlayer(senderUuid);
            if (session != null) {
                String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
                Component customMessage = mm.deserialize("<gray>[<red>LegitCheck</red>] [<red>Player</red>] <gold>" + sender.getName() + "</gold>: " + rawMessage);

                // Send to player
                sender.sendMessage(customMessage);

                // Send to checking admin
                Player admin = Bukkit.getPlayer(session.getAdminUuid());
                if (admin != null && admin.isOnline()) {
                    admin.sendMessage(customMessage);
                }

                // Send to other staff with notification permission
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission("legitcheck.notify") && !online.getUniqueId().equals(senderUuid) && !online.getUniqueId().equals(session.getAdminUuid())) {
                        online.sendMessage(customMessage);
                    }
                }
            }
            return;
        }

        // 2. If sender is the checking admin
        if (manager.isChecking(senderUuid)) {
            event.setCancelled(true);
            CheckSession session = manager.getSessionByAdmin(senderUuid);
            if (session != null) {
                String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
                Component customMessage = mm.deserialize("<gray>[<red>LegitCheck</red>] [<green>Admin</green>] <gold>" + sender.getName() + "</gold>: " + rawMessage);

                // Send to admin
                sender.sendMessage(customMessage);

                // Send to checked player
                Player target = Bukkit.getPlayer(session.getPlayerUuid());
                if (target != null && target.isOnline()) {
                    target.sendMessage(customMessage);
                }

                // Send to other staff with notification permission
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.hasPermission("legitcheck.notify") && !online.getUniqueId().equals(senderUuid) && !online.getUniqueId().equals(session.getPlayerUuid())) {
                        online.sendMessage(customMessage);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        CheckManager manager = plugin.getCheckManager();

        if (manager.isBeingChecked(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (manager.isChecking(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(mm.deserialize("<red>You cannot break blocks while checking players.</red>"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        CheckManager manager = plugin.getCheckManager();

        if (manager.isBeingChecked(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (manager.isChecking(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(mm.deserialize("<red>You cannot place blocks while checking players.</red>"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        CheckManager manager = plugin.getCheckManager();

        // Prevent checking admin from teleporting away via commands/other plugins (allow PLUGIN cause for system teleports)
        if (manager.isChecking(player.getUniqueId())) {
            if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
                event.setCancelled(true);
                player.sendMessage(mm.deserialize("<red>Teleportation is blocked while checking players.</red>"));
            }
        }

        // Prevent checked player from teleporting away (allow PLUGIN cause for system teleports)
        if (manager.isBeingChecked(player.getUniqueId())) {
            if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
                event.setCancelled(true);
            }
        }
    }
}
