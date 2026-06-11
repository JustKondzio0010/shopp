package pl.lukas.reputation.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.lukas.reputation.ReputationPlugin;
import pl.lukas.reputation.hook.WorldGuardHook;

public class ReputationListener implements Listener {
    private final ReputationPlugin plugin;

    public ReputationListener(ReputationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Load data on thread-safe Bukkit scheduler if needed, or synchronously if it's fast. 
        // Synchronous loading on Join is standard for SQLite to ensure data is loaded before first chat/interaction.
        plugin.getReputationManager().loadPlayer(player.getUniqueId(), player.getName());
        plugin.getReputationManager().updatePrefixes(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getReputationManager().unloadPlayer(player.getUniqueId(), player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();

        if (killer != null && !killer.equals(victim)) {
            // Check if world is disabled for kill penalty
            String worldName = victim.getWorld().getName();
            java.util.List<String> disabledWorlds = plugin.getConfig().getStringList("settings.kill-penalty-disabled-worlds");
            if (disabledWorlds != null && !disabledWorlds.isEmpty()) {
                for (String w : disabledWorlds) {
                    if (w.equalsIgnoreCase(worldName)) {
                        return;
                    }
                }
            }

            // Check if region is disabled for kill penalty (requires WorldGuard)
            java.util.List<String> disabledRegions = plugin.getConfig().getStringList("settings.kill-penalty-disabled-regions");
            if (disabledRegions != null && !disabledRegions.isEmpty()) {
                if (WorldGuardHook.isInRegion(victim.getLocation(), disabledRegions)) {
                    return;
                }
            }

            plugin.getReputationManager().deductKillPenalty(
                    killer.getUniqueId(),
                    killer.getName(),
                    victim.getUniqueId(),
                    victim.getName()
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("settings.enable-chat-prefix", true)) {
            return;
        }

        // Set the custom chat renderer
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            Component prefix = plugin.getReputationManager().getPrefixComponent(source.getUniqueId());
            return prefix.append(sourceDisplayName)
                         .append(Component.text(": "))
                         .append(message);
        });
    }
}
