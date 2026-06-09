package pl.konrad.vipshop.auditor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import pl.konrad.vipshop.VipShop;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class EconomyAuditor implements Listener {

    private final VipShop plugin;
    
    // Track balance history: Player UUID -> LinkedList of historical balances (timestamp, amount)
    private final Map<UUID, LinkedList<BalanceRecord>> balanceHistory = new ConcurrentHashMap<>();
    
    // Track item drops: Player UUID -> List of drop records (timestamp, amount)
    private final Map<UUID, LinkedList<DropRecord>> dropHistory = new ConcurrentHashMap<>();

    // Cached frozen players to avoid database queries on every movement/click
    private final Set<UUID> frozenCache = Collections.synchronizedSet(new HashSet<>());

    // Exemptions for players who just received a /pay or /eco give command
    private final Map<UUID, Long> payExemptions = new ConcurrentHashMap<>();

    private static final long FIVE_MINUTES_MS = 5 * 60 * 1000L;
    private static final long ONE_MINUTE_MS = 60 * 1000L;

    public EconomyAuditor(VipShop plugin) {
        this.plugin = plugin;
    }

    public void startAuditing() {
        if (!plugin.getConfig().getBoolean("auditor.enabled", true)) {
            return;
        }

        // Run async task to check balances every 30 seconds to avoid database blocks
        new BukkitRunnable() {
            @Override
            public void run() {
                if (plugin.getEconomy() == null) {
                    return;
                }
                
                long now = System.currentTimeMillis();
                double threshold = plugin.getConfig().getDouble("auditor.wealth-spike-threshold", 100000.0);
                long windowMs = plugin.getConfig().getInt("auditor.wealth-spike-time-minutes", 5) * 60 * 1000L;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    
                    // Skip checking if already frozen, OP, or temporarily exempted
                    if (isFrozen(uuid) || player.isOp()) {
                        continue;
                    }
                    
                    Long exemptUntil = payExemptions.get(uuid);
                    if (exemptUntil != null && now < exemptUntil) {
                        // Skip balance check completely while exempted
                        // Update the baseline history to current balance so we don't trigger spike AFTER exemption ends
                        LinkedList<BalanceRecord> hist = balanceHistory.computeIfAbsent(uuid, k -> new LinkedList<>());
                        hist.clear();
                        hist.addLast(new BalanceRecord(now, plugin.getEconomy().getBalance(player)));
                        continue;
                    }

                    double currentBalance = plugin.getEconomy().getBalance(player);
                    
                    LinkedList<BalanceRecord> history = balanceHistory.computeIfAbsent(uuid, k -> new LinkedList<>());
                    history.addLast(new BalanceRecord(now, currentBalance));

                    // Prune records older than the window size
                    while (!history.isEmpty() && now - history.getFirst().timestamp > windowMs) {
                        history.removeFirst();
                    }

                    // Check for wealth spikes
                    if (history.size() > 1) {
                        double oldestBalance = history.getFirst().balance;
                        double change = currentBalance - oldestBalance;

                        if (change >= threshold) {
                            // Run the freeze and alerts on the main thread
                            Bukkit.getScheduler().runTask(plugin, () -> triggerWealthSpikeAlert(player, change));
                        }
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 100L, 600L); // Start after 5s, run every 30s
    }

    public boolean isFrozen(UUID uuid) {
        // First check the thread-safe memory cache
        if (frozenCache.contains(uuid)) {
            return true;
        }
        
        // If not in cache, query database and populate cache
        boolean isDbFrozen = plugin.getDatabaseManager().isPlayerFrozen(uuid.toString());
        if (isDbFrozen) {
            frozenCache.add(uuid);
        }
        return isDbFrozen;
    }

    public void freezePlayer(Player player, String reason) {
        UUID uuid = player.getUniqueId();
        frozenCache.add(uuid);
        plugin.getDatabaseManager().setPlayerFrozen(uuid.toString(), true, reason);
        plugin.getDatabaseManager().logAlert(uuid.toString(), "FREEZE", reason);

        // Notify the frozen player with titles and sounds
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        player.showTitle(net.kyori.adventure.title.Title.title(
            Component.text("BLOKADA KONTA!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
            Component.text("Twoje konto zostało zamrożone. Wykryto anomalię.").color(NamedTextColor.GRAY)
        ));

        // Alert all online administrators
        Component adminMsg = Component.text("[AUDYTOR] ")
            .color(NamedTextColor.DARK_RED)
            .append(Component.text(player.getName()).color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
            .append(Component.text(" został zamrożony! Powód: " + reason).color(NamedTextColor.GRAY));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("vipshop.admin.alerts")) {
                online.sendMessage(adminMsg);
                online.playSound(online.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.8f, 0.8f);
                online.showTitle(net.kyori.adventure.title.Title.title(
                    Component.text("ALERT AUDYTORA").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD),
                    Component.text("Gracz " + player.getName() + " został zamrożony!").color(NamedTextColor.GRAY)
                ));
            }
        }
    }

    public void unfreezePlayer(UUID uuid) {
        frozenCache.remove(uuid);
        plugin.getDatabaseManager().setPlayerFrozen(uuid.toString(), false, null);
    }

    private void triggerWealthSpikeAlert(Player player, double change) {
        // Re-verify they aren't already frozen
        if (isFrozen(player.getUniqueId())) {
            return;
        }
        
        String reason = String.format("Nagły przyrost gotówki: +$%.2f w ciągu 5 minut", change);
        plugin.getLogger().warning(String.format("Wykryto anomalię ekonomiczną u gracza %s! %s", player.getName(), reason));
        freezePlayer(player, reason);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropValuables(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 1. Cancel if the player is frozen
        if (isFrozen(uuid)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Twój ekwipunek jest zamrożony!", NamedTextColor.RED));
            return;
        }

        // 2. Track drop counts for anti-dupe audits
        Material mat = event.getItemDrop().getItemStack().getType();
        List<String> monitored = plugin.getConfig().getStringList("auditor.valuable-materials");
        if (monitored.contains(mat.name())) {
            long now = System.currentTimeMillis();
            int amount = event.getItemDrop().getItemStack().getAmount();

            LinkedList<DropRecord> history = dropHistory.computeIfAbsent(uuid, k -> new LinkedList<>());
            history.addLast(new DropRecord(now, mat, amount));

            // Prune old drop records
            long windowMs = plugin.getConfig().getInt("auditor.drop-valuable-time-minutes", 1) * 60 * 1000L;
            while (!history.isEmpty() && now - history.getFirst().timestamp > windowMs) {
                history.removeFirst();
            }

            // Calculate total drops
            int totalDropped = history.stream().mapToInt(r -> r.amount).sum();
            int threshold = plugin.getConfig().getInt("auditor.drop-valuable-threshold", 64);

            if (totalDropped >= threshold) {
                String reason = String.format("Nienaturalne wyrzucanie surowców: %d szt. wartościowych bloków/rud w 1 minutę", totalDropped);
                plugin.getLogger().warning(String.format("Wykryto podejrzane wyrzucanie przedmiotów u gracza %s! %s", player.getName(), reason));
                freezePlayer(player, reason);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFrozenPlayerInteract(PlayerInteractEvent event) {
        if (isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Nie możesz wchodzić w interakcję ze światem, gdy Twoje konto jest zamrożone!", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFrozenInventoryClick(InventoryClickEvent event) {
        if (isFrozen(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
            event.getWhoClicked().sendMessage(Component.text("Nie możesz modyfikować ekwipunku, gdy Twoje konto jest zamrożone!", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFrozenCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (isFrozen(player.getUniqueId())) {
            // Block all commands to prevent transfers like /pay or /ah sell
            event.setCancelled(true);
            player.sendMessage(Component.text("Nie możesz wykonywać komend, gdy Twoje konto jest zamrożone!", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPayCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase();
        // Sprawdzamy czy to komenda od przelewów (EssentialsX)
        if (msg.startsWith("/pay ") || msg.startsWith("/eco give ") || msg.startsWith("/economy give ")) {
            String[] args = event.getMessage().split(" ");
            // /pay <nick> <kwota> lub /eco give <nick> <kwota>
            int targetIndex = msg.startsWith("/pay") ? 1 : 2;
            if (args.length > targetIndex) {
                Player target = Bukkit.getPlayer(args[targetIndex]);
                if (target != null && target.isOnline()) {
                    // Dajemy graczowi 15 sekund "odporności" na wykrycie nagłego przyrostu gotówki
                    payExemptions.put(target.getUniqueId(), System.currentTimeMillis() + 15000L);
                }
            }
        }
    }

    public void clearHistory(UUID uuid) {
        balanceHistory.remove(uuid);
        dropHistory.remove(uuid);
        frozenCache.remove(uuid);
    }

    private static final class BalanceRecord {
        final long timestamp;
        final double balance;

        BalanceRecord(long timestamp, double balance) {
            this.timestamp = timestamp;
            this.balance = balance;
        }
    }

    private static final class DropRecord {
        final long timestamp;
        final Material material;
        final int amount;

        DropRecord(long timestamp, Material material, int amount) {
            this.timestamp = timestamp;
            this.material = material;
            this.amount = amount;
        }
    }
}
