package pl.lukas.legitcheck.command;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.lukas.legitcheck.LegitCheckPlugin;
import pl.lukas.legitcheck.manager.CheckManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CheckCommand implements CommandExecutor, TabCompleter {
    private final LegitCheckPlugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CheckCommand(LegitCheckPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage(mm.deserialize("<red>Only players can execute this command.</red>"));
            return true;
        }

        if (!admin.hasPermission("legitcheck.use")) {
            admin.sendMessage(mm.deserialize("<red>You do not have permission to use this command.</red>"));
            return true;
        }

        CheckManager manager = plugin.getCheckManager();

        if (args.length == 0) {
            sendHelpMessage(admin);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("setloc")) {
            manager.saveCheckLocation(admin.getLocation());
            admin.sendMessage(mm.deserialize("<green>Check location has been successfully set to your current position.</green>"));
            return true;
        }

        if (sub.equals("reload")) {
            plugin.reloadConfig();
            manager.loadCheckLocation();
            admin.sendMessage(mm.deserialize("<green>LegitCheck configuration has been reloaded.</green>"));
            return true;
        }

        if (sub.equals("clean") || sub.equals("clear")) {
            if (args.length < 2) {
                admin.sendMessage(mm.deserialize("<red>Usage: /check clean <player></red>"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                admin.sendMessage(mm.deserialize("<red>Player " + args[1] + " is not online.</red>"));
                return true;
            }
            if (!manager.isBeingChecked(target.getUniqueId())) {
                admin.sendMessage(mm.deserialize("<red>This player is not being checked.</red>"));
                return true;
            }
            manager.cleanPlayer(target, admin);
            return true;
        }

        // Default behavior: check a player
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            admin.sendMessage(mm.deserialize("<red>Player " + args[0] + " is not online.</red>"));
            return true;
        }

        if (target.getUniqueId().equals(admin.getUniqueId())) {
            admin.sendMessage(mm.deserialize("<red>You cannot check yourself.</red>"));
            return true;
        }

        if (manager.isBeingChecked(target.getUniqueId())) {
            // Player is already being checked - open the GUI for the admin
            manager.openCheckGui(admin, target.getName());
            return true;
        }

        if (manager.getCheckLocation() == null) {
            admin.sendMessage(mm.deserialize("<yellow>Warning: Check location is not set! Set it first using </yellow><gold>/check setloc</gold><yellow>.</yellow>"));
        }

        manager.startChecking(admin, target);
        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(mm.deserialize("<gold><bold>=== LegitCheck Help ===</bold></gold>"));
        player.sendMessage(mm.deserialize("<yellow>/check <player></yellow> - <gray>Start checking a player (opens GUI if already checking).</gray>"));
        player.sendMessage(mm.deserialize("<yellow>/check clean <player></yellow> - <gray>Release a checked player as clean.</gray>"));
        player.sendMessage(mm.deserialize("<yellow>/check setloc</yellow> - <gray>Set the check location to your current position.</gray>"));
        player.sendMessage(mm.deserialize("<yellow>/check reload</yellow> - <gray>Reload configuration.</gray>"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("legitcheck.use")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("setloc");
            list.add("clean");
            list.add("clear");
            list.add("reload");
            for (Player player : Bukkit.getOnlinePlayers()) {
                list.add(player.getName());
            }
            return list.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("clean") || args[0].equalsIgnoreCase("clear"))) {
            List<String> list = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.getCheckManager().isBeingChecked(player.getUniqueId())) {
                    list.add(player.getName());
                }
            }
            return list.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
