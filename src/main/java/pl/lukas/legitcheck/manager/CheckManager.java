package pl.lukas.legitcheck.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.lukas.legitcheck.LegitCheckPlugin;
import pl.lukas.legitcheck.model.CheckSession;

import java.util.*;

public class CheckManager {
    private final LegitCheckPlugin plugin;
    private final Map<UUID, CheckSession> activeSessionsByPlayer = new HashMap<>();
    private final Map<UUID, CheckSession> activeSessionsByAdmin = new HashMap<>();
    private Location checkLocation;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CheckManager(LegitCheckPlugin plugin) {
        this.plugin = plugin;
        loadCheckLocation();
    }

    public void loadCheckLocation() {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("check-location.world")) {
            String worldName = config.getString("check-location.world");
            double x = config.getDouble("check-location.x");
            double y = config.getDouble("check-location.y");
            double z = config.getDouble("check-location.z");
            float yaw = (float) config.getDouble("check-location.yaw");
            float pitch = (float) config.getDouble("check-location.pitch");
            if (worldName != null && Bukkit.getWorld(worldName) != null) {
                this.checkLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
            }
        }
    }

    public void saveCheckLocation(Location loc) {
        this.checkLocation = loc;
        FileConfiguration config = plugin.getConfig();
        config.set("check-location.world", loc.getWorld().getName());
        config.set("check-location.x", loc.getX());
        config.set("check-location.y", loc.getY());
        config.set("check-location.z", loc.getZ());
        config.set("check-location.yaw", loc.getYaw());
        config.set("check-location.pitch", loc.getPitch());
        plugin.saveConfig();
    }

    public Location getCheckLocation() {
        return checkLocation;
    }

    public boolean isBeingChecked(UUID playerUuid) {
        return activeSessionsByPlayer.containsKey(playerUuid);
    }

    public boolean isChecking(UUID adminUuid) {
        return activeSessionsByAdmin.containsKey(adminUuid);
    }

    public CheckSession getSessionByPlayer(UUID playerUuid) {
        return activeSessionsByPlayer.get(playerUuid);
    }

    public CheckSession getSessionByAdmin(UUID adminUuid) {
        return activeSessionsByAdmin.get(adminUuid);
    }

    public Collection<CheckSession> getSessions() {
        return activeSessionsByPlayer.values();
    }

    public void startChecking(Player admin, Player player) {
        UUID playerUuid = player.getUniqueId();
        UUID adminUuid = admin.getUniqueId();

        CheckSession session = new CheckSession(
                playerUuid,
                player.getName(),
                adminUuid,
                admin.getName(),
                player.getLocation(),
                admin.getLocation()
        );

        activeSessionsByPlayer.put(playerUuid, session);
        activeSessionsByAdmin.put(adminUuid, session);

        // Teleport both to the check location if configured
        if (checkLocation != null) {
            player.teleport(checkLocation);
            admin.teleport(checkLocation);
        }

        // Notify player and server
        player.sendMessage(mm.deserialize("<red><bold>=============================================</bold></red>"));
        player.sendMessage(mm.deserialize("<yellow>You are being checked for cheats by Admin <gold>" + admin.getName() + "</gold>!</yellow>"));
        player.sendMessage(mm.deserialize("<yellow>Do <red>NOT</red> log out. Logging out will result in a permanent ban.</yellow>"));
        player.sendMessage(mm.deserialize("<yellow>Communicate using the chat. Your messages are private.</yellow>"));
        player.sendMessage(mm.deserialize("<red><bold>=============================================</bold></red>"));

        admin.sendMessage(mm.deserialize("<green>Started checking <gold>" + player.getName() + "</gold>. Both teleported to check location.</green>"));
        
        // Open the decision GUI for the admin
        openCheckGui(admin, player.getName());
    }

    public void cleanPlayer(Player player, Player admin) {
        UUID playerUuid = player.getUniqueId();
        CheckSession session = activeSessionsByPlayer.remove(playerUuid);
        if (session != null) {
            activeSessionsByAdmin.remove(session.getAdminUuid());

            // Teleport both back to original locations
            player.teleport(session.getOriginalLocation());
            
            Player originalAdmin = Bukkit.getPlayer(session.getAdminUuid());
            if (originalAdmin != null && originalAdmin.isOnline()) {
                originalAdmin.teleport(session.getOriginalAdminLocation());
                originalAdmin.sendMessage(mm.deserialize("<green>Checking finished. <gold>" + player.getName() + "</gold> is clean.</green>"));
            }

            player.sendMessage(mm.deserialize("<green>Verification complete. You are <gold>CLEAN</gold>! Thank you for cooperating.</green>"));
            
            // Broadcast message to staff / server
            Bukkit.broadcast(
                    mm.deserialize("<gray>[<red>LegitCheck</red>] Player <gold>" + player.getName() + "</gold> has been checked by <gold>" + session.getAdminName() + "</gold> and is <green>CLEAN</green>.</gray>"),
                    "legitcheck.notify"
            );
        }
    }

    public void removeSessionWithoutTeleport(UUID playerUuid) {
        CheckSession session = activeSessionsByPlayer.remove(playerUuid);
        if (session != null) {
            activeSessionsByAdmin.remove(session.getAdminUuid());
        }
    }

    public void banPlayerFromGui(Player player, Player admin, String banCommand) {
        UUID playerUuid = player.getUniqueId();
        CheckSession session = activeSessionsByPlayer.remove(playerUuid);
        if (session != null) {
            activeSessionsByAdmin.remove(session.getAdminUuid());

            // Teleport admin back
            Player originalAdmin = Bukkit.getPlayer(session.getAdminUuid());
            if (originalAdmin != null && originalAdmin.isOnline()) {
                originalAdmin.teleport(session.getOriginalAdminLocation());
                originalAdmin.sendMessage(mm.deserialize("<green>Player has been banned. You have been teleported back.</green>"));
            }

            // Execute the ban command on behalf of console
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), banCommand);
        }
    }


    public void openCheckGui(Player admin, String targetPlayerName) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Checking: " + targetPlayerName));

        // Slot 0: Perm Ban (Red Wool)
        inv.setItem(0, createGuiItem(Material.RED_WOOL, 
                "<red><bold>PERMANENT BAN</bold></red>", 
                Arrays.asList("<gray>Bans the player permanently.</gray>", "<gray>Command: /ban " + targetPlayerName + " Cheats - Refused/Detected</gray>")));

        // Slot 2: Temp Ban 21 Days (Orange Wool)
        inv.setItem(2, createGuiItem(Material.ORANGE_WOOL, 
                "<gold><bold>TEMP BAN - 21 DAYS</bold></gold>", 
                Arrays.asList("<gray>Bans the player for 21 days.</gray>", "<gray>Command: /tempban " + targetPlayerName + " 21d Cheats</gray>")));

        // Slot 4: Temp Ban 14 Days (Yellow Wool)
        inv.setItem(4, createGuiItem(Material.YELLOW_WOOL, 
                "<yellow><bold>TEMP BAN - 14 DAYS</bold></yellow>", 
                Arrays.asList("<gray>Bans the player for 14 days.</gray>", "<gray>Command: /tempban " + targetPlayerName + " 14d Cheats</gray>")));

        // Slot 6: Temp Ban 7 Days (Blue Wool)
        inv.setItem(6, createGuiItem(Material.BLUE_WOOL, 
                "<blue><bold>TEMP BAN - 7 DAYS (Confession)</bold></blue>", 
                Arrays.asList("<gray>Bans the player for 7 days (confessed).</gray>", "<gray>Command: /tempban " + targetPlayerName + " 7d Cheating - Admission of guilt</gray>")));

        // Slot 8: Clean / Free (Green Wool)
        inv.setItem(8, createGuiItem(Material.GREEN_WOOL, 
                "<green><bold>PLAYER IS CLEAN (RELEASE)</bold></green>", 
                Arrays.asList("<gray>Declares the player clean.</gray>", "<gray>Teleports the player back and releases them.</gray>")));

        admin.openInventory(inv);
    }

    private ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize(name));
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(mm.deserialize(line));
            }
            meta.lore(loreComponents);
            item.setItemMeta(meta);
        }
        return item;
    }
}
