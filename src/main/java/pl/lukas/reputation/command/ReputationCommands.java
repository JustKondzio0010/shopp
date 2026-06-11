package pl.lukas.reputation.command;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.lukas.reputation.ReputationPlugin;
import pl.lukas.reputation.db.DatabaseManager;
import pl.lukas.reputation.manager.ReputationManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReputationCommands implements CommandExecutor, TabCompleter {
    private final ReputationPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ReputationCommands(ReputationPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("rep+")) {
            handleVoteCommand(sender, args, true);
            return true;
        } else if (cmdName.equals("rep-")) {
            handleVoteCommand(sender, args, false);
            return true;
        } else if (cmdName.equals("reputation")) {
            if (args.length == 0) {
                sendHelp(sender);
                return true;
            }

            String sub = args[0].toLowerCase();
            if (sub.equals("+") || sub.equals("plus")) {
                String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
                handleVoteCommand(sender, newArgs, true);
                return true;
            } else if (sub.equals("-") || sub.equals("minus")) {
                String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
                handleVoteCommand(sender, newArgs, false);
                return true;
            } else if (sub.equals("check")) {
                handleCheckCommand(sender, args);
                return true;
            } else if (sub.equals("set")) {
                handleSetCommand(sender, args);
                return true;
            } else if (sub.equals("reload")) {
                handleReloadCommand(sender);
                return true;
            } else {
                sendHelp(sender);
                return true;
            }
        }

        return false;
    }

    private void handleVoteCommand(CommandSender sender, String[] args, boolean isPlus) {
        if (!(sender instanceof Player voter)) {
            sender.sendMessage(miniMessage.deserialize(getPrefix() + plugin.getConfig().getString("messages.only-players")));
            return;
        }

        if (args.length == 0) {
            String usage = isPlus ? "<red>Usage: /rep+ <player> or /rep+ give <player></red>" : "<red>Usage: /rep- <player> or /rep- give <player></red>";
            voter.sendMessage(miniMessage.deserialize(getPrefix() + usage));
            return;
        }

        String targetName = args[0];
        if (targetName.equalsIgnoreCase("give") && args.length > 1) {
            targetName = args[1];
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            String notFound = plugin.getConfig().getString("messages.player-not-found", "<red>Player %player% was not found.</red>")
                    .replace("%player%", targetName);
            voter.sendMessage(miniMessage.deserialize(getPrefix() + notFound));
            return;
        }

        String voterIp = voter.getAddress() != null ? voter.getAddress().getAddress().getHostAddress() : "";

        ReputationManager.VoteResult result;
        if (isPlus) {
            result = plugin.getReputationManager().votePlus(voter.getUniqueId(), voter.getName(), voterIp, target.getUniqueId(), target.getName());
        } else {
            result = plugin.getReputationManager().voteMinus(voter.getUniqueId(), voter.getName(), voterIp, target.getUniqueId(), target.getName());
        }

        switch (result) {
            case SELF_VOTE -> {
                String msg = plugin.getConfig().getString("messages.cannot-rate-self", "<red>You cannot rate your own reputation!</red>");
                voter.sendMessage(miniMessage.deserialize(getPrefix() + msg));
            }
            case SAME_IP -> {
                String msg = plugin.getConfig().getString("messages.cannot-rate-same-ip", "<red>You cannot rate a player with the same IP address!</red>");
                voter.sendMessage(miniMessage.deserialize(getPrefix() + msg));
            }
            case IP_ALREADY_VOTED -> {
                String msg = plugin.getConfig().getString("messages.ip-already-voted", "<red>Someone from your IP address has already rated this player!</red>");
                voter.sendMessage(miniMessage.deserialize(getPrefix() + msg));
            }
            case ALREADY_VOTED -> {
                String key = isPlus ? "messages.already-voted-plus" : "messages.already-voted-minus";
                String fallback = isPlus ? "<red>You already gave positive reputation to %player%.</red>" : "<red>You already gave negative reputation to %player%.</red>";
                String msg = plugin.getConfig().getString(key, fallback).replace("%player%", target.getName());
                voter.sendMessage(miniMessage.deserialize(getPrefix() + msg));
            }
            case SUCCESS -> {
                String key = isPlus ? "messages.vote-plus-success" : "messages.vote-minus-success";
                String fallback = isPlus ? "<green>You gave positive reputation (+0.5) to %player%!</green>" : "<red>You gave negative reputation (-0.5) to %player%!</red>";
                String msg = plugin.getConfig().getString(key, fallback).replace("%player%", target.getName());
                voter.sendMessage(miniMessage.deserialize(getPrefix() + msg));
                
                // Optional: Notify target if they are online
                Player onlineTarget = target.getPlayer();
                if (onlineTarget != null) {
                    double currentScore = plugin.getReputationManager().getReputationScore(target.getUniqueId());
                    String alert = isPlus ? "<green>You received positive reputation (+0.5) from <yellow>" + voter.getName() + "</yellow>! (Current: " + String.format("%.1f", currentScore) + ")</green>" 
                                          : "<red>You received negative reputation (-0.5) from <yellow>" + voter.getName() + "</yellow>! (Current: " + String.format("%.1f", currentScore) + ")</red>";
                    onlineTarget.sendMessage(miniMessage.deserialize(getPrefix() + alert));
                }
            }
        }
    }

    private void handleCheckCommand(CommandSender sender, String[] args) {
        OfflinePlayer target;
        String targetName;

        if (args.length < 2) {
            if (sender instanceof Player player) {
                target = player;
                targetName = player.getName();
            } else {
                sender.sendMessage(miniMessage.deserialize(getPrefix() + "<red>Usage: /rep check <player></red>"));
                return;
            }
        } else {
            targetName = args[1];
            target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                String notFound = plugin.getConfig().getString("messages.player-not-found", "<red>Player %player% was not found.</red>")
                        .replace("%player%", targetName);
                sender.sendMessage(miniMessage.deserialize(getPrefix() + notFound));
                return;
            }
        }

        DatabaseManager.PlayerRepData data = plugin.getReputationManager().getPlayerData(target.getUniqueId());
        double score = data.getScore();
        int scoreInt = plugin.getReputationManager().getRoundedScore(score);
        String heart = plugin.getReputationManager().getHeartEmoji(scoreInt);

        List<String> format = plugin.getConfig().getStringList("messages.check-format");
        if (format.isEmpty()) {
            format = Arrays.asList(
                "<gold>=============== <yellow>%player%'s Reputation</yellow> ===============</gold>",
                "<gray>Score: </gray>%heart% <yellow>%score%</yellow> <gray>(Rounded: %score_int%)</gray>",
                "<gray>Positive votes (+0.5): </gray><green>%pos_votes%</green>",
                "<gray>Negative votes (-0.5): </gray><red>%neg_votes%</red>",
                "<gold>===================================================</gold>"
            );
        }

        for (String line : format) {
            String formatted = line.replace("%player%", target.getName() != null ? target.getName() : targetName)
                                   .replace("%score%", String.format("%.2f", score)) // Keep precision in check command!
                                   .replace("%score_int%", String.valueOf(scoreInt))
                                   .replace("%heart%", heart)
                                   .replace("%pos_votes%", String.valueOf(data.getPosVotes()))
                                   .replace("%neg_votes%", String.valueOf(data.getNegVotes()));
            sender.sendMessage(miniMessage.deserialize(formatted));
        }
    }

    private void handleSetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("reputation.admin")) {
            sender.sendMessage(miniMessage.deserialize(getPrefix() + plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(miniMessage.deserialize(getPrefix() + plugin.getConfig().getString("messages.admin-set-usage")));
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            String notFound = plugin.getConfig().getString("messages.player-not-found", "<red>Player %player% was not found.</red>")
                    .replace("%player%", targetName);
            sender.sendMessage(miniMessage.deserialize(getPrefix() + notFound));
            return;
        }

        double score;
        try {
            score = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(miniMessage.deserialize(getPrefix() + "<red>Invalid score. Must be a number between 1 and 10.</red>"));
            return;
        }

        if (score < 1.0 || score > 10.0) {
            sender.sendMessage(miniMessage.deserialize(getPrefix() + "<red>Score must be between 1.0 and 10.0.</red>"));
            return;
        }

        plugin.getReputationManager().adminSet(target.getUniqueId(), target.getName(), score);
        
        String successMsg = plugin.getConfig().getString("messages.admin-set-success", "<green>Successfully set %player%'s reputation to %score%.</green>")
                .replace("%player%", target.getName())
                .replace("%score%", String.format("%.1f", score));
        sender.sendMessage(miniMessage.deserialize(getPrefix() + successMsg));
    }

    private void handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("reputation.admin")) {
            sender.sendMessage(miniMessage.deserialize(getPrefix() + plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        plugin.reloadConfig();
        
        // Refresh all prefixes
        for (Player online : Bukkit.getOnlinePlayers()) {
            plugin.getReputationManager().updatePrefixes(online);
        }

        sender.sendMessage(miniMessage.deserialize(getPrefix() + "<green>Configuration reloaded successfully!</green>"));
    }

    private void sendHelp(CommandSender sender) {
        List<String> help = plugin.getConfig().getStringList("messages.help");
        if (help.isEmpty()) {
            help = new ArrayList<>(Arrays.asList(
                "<gold>=== <yellow>Reputation Commands</yellow> ===</gold>",
                "<yellow>/rep+ <player></yellow> <gray>- Give positive reputation (+0.5)</gray>",
                "<yellow>/rep- <player></yellow> <gray>- Give negative reputation (-0.5)</gray>",
                "<yellow>/rep check <player></yellow> <gray>- View detailed reputation statistics</gray>"
            ));
            if (sender.hasPermission("reputation.admin")) {
                help.add("<red>/rep set <player> <1-10></red> <gray>- (Admin) Set reputation score</gray>");
            }
        }

        for (String line : help) {
            // Hide admin commands from players who don't have permission
            if (line.toLowerCase().contains("set") && !sender.hasPermission("reputation.admin")) {
                continue;
            }
            if (line.toLowerCase().contains("reload") && !sender.hasPermission("reputation.admin")) {
                continue;
            }
            sender.sendMessage(miniMessage.deserialize(line));
        }
    }

    private String getPrefix() {
        return plugin.getConfig().getString("messages.prefix", "<gold>[Reputation]</gold> ");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase();
        List<String> completions = new ArrayList<>();

        if (cmdName.equals("rep+") || cmdName.equals("rep-")) {
            if (args.length == 1) {
                completions.add("give");
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            }
        } else if (cmdName.equals("reputation")) {
            if (args.length == 1) {
                completions.addAll(new ArrayList<>(Arrays.asList("+", "-", "check")));
                if (sender.hasPermission("reputation.admin")) {
                    completions.add("set");
                    completions.add("reload");
                }
            } else if (args.length == 2) {
                String sub = args[0].toLowerCase();
                if (sub.equals("+") || sub.equals("-") || sub.equals("check") || (sub.equals("set") && sender.hasPermission("reputation.admin"))) {
                    completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("set") && sender.hasPermission("reputation.admin")) {
                for (int i = 1; i <= 10; i++) {
                    completions.add(String.valueOf(i));
                }
            }
        }

        // Filter based on input
        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .sorted()
                .collect(Collectors.toList());
    }
}
