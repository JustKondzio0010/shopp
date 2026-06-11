package pl.lukas.reputation.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pl.lukas.reputation.ReputationPlugin;
import pl.lukas.reputation.db.DatabaseManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReputationManager {
    private final ReputationPlugin plugin;
    private final Map<UUID, DatabaseManager.PlayerRepData> cache = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public enum VoteResult {
        SUCCESS,
        ALREADY_VOTED,
        SELF_VOTE,
        SAME_IP,
        IP_ALREADY_VOTED
    }

    public ReputationManager(ReputationPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadPlayer(UUID uuid, String username) {
        DatabaseManager.PlayerRepData data = plugin.getDatabaseManager().loadPlayerData(uuid, username);
        cache.put(uuid, data);
    }

    public void unloadPlayer(UUID uuid, String username) {
        DatabaseManager.PlayerRepData data = cache.remove(uuid);
        if (data != null) {
            plugin.getDatabaseManager().savePlayerData(uuid, username, data);
        }
        
        // Remove scoreboard team to prevent memory leaks
        org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "r_" + uuid.toString().substring(0, 14);
        org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
        if (team != null) {
            team.unregister();
        }
    }

    public void cleanupTeams() {
        org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (org.bukkit.scoreboard.Team team : new java.util.ArrayList<>(scoreboard.getTeams())) {
            if (team.getName().startsWith("r_")) {
                team.unregister();
            }
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, DatabaseManager.PlayerRepData> entry : cache.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            String username = player != null ? player.getName() : "Unknown";
            plugin.getDatabaseManager().savePlayerData(entry.getKey(), username, entry.getValue());
        }
    }

    public DatabaseManager.PlayerRepData getPlayerData(UUID uuid) {
        DatabaseManager.PlayerRepData data = cache.get(uuid);
        if (data == null) {
            // Load if not in cache (e.g. offline player checked)
            data = plugin.getDatabaseManager().loadPlayerData(uuid, "Unknown");
        }
        return data;
    }

    public double getReputationScore(UUID uuid) {
        return getPlayerData(uuid).getScore();
    }

    public int getRoundedScore(double score) {
        int rounded = (int) Math.round(score);
        return Math.max(1, Math.min(10, rounded));
    }

    public String getHeartEmoji(int roundedScore) {
        String emoji = plugin.getConfig().getString("hearts." + roundedScore);
        if (emoji == null) {
            return switch (roundedScore) {
                case 1 -> "🖤";
                case 2 -> "🩶";
                case 3 -> "🤍";
                case 4 -> "🤎";
                case 5 -> "💙";
                case 6 -> "💚";
                case 7 -> "💛";
                case 8 -> "🧡";
                case 9 -> "❤️";
                case 10 -> "💖";
                default -> "💙";
            };
        }
        return emoji;
    }

    public String getPrefixString(UUID uuid) {
        double score = getReputationScore(uuid);
        int rounded = getRoundedScore(score);
        String heart = getHeartEmoji(rounded);
        
        String format = plugin.getConfig().getString("settings.prefix-format", "<gray>[</gray>%heart% <gray>%score_int%]</gray> ");
        return format.replace("%heart%", heart)
                     .replace("%score_int%", String.valueOf(rounded));
    }

    public Component getPrefixComponent(UUID uuid) {
        return miniMessage.deserialize(getPrefixString(uuid));
    }

    public void updatePrefixes(Player player) {
        UUID uuid = player.getUniqueId();
        Component prefix = getPrefixComponent(uuid);
        
        if (plugin.getConfig().getBoolean("settings.enable-chat-prefix", true)) {
            player.displayName(prefix.append(Component.text(player.getName())));
        } else {
            player.displayName(Component.text(player.getName()));
        }

        if (plugin.getConfig().getBoolean("settings.enable-tab-prefix", true)) {
            player.playerListName(prefix.append(Component.text(player.getName())));
        } else {
            player.playerListName(Component.text(player.getName()));
        }

        if (plugin.getConfig().getBoolean("settings.enable-nametag-prefix", true)) {
            org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            String teamName = "r_" + uuid.toString().substring(0, 14);
            org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            team.prefix(prefix);
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
        } else {
            org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            String teamName = "r_" + uuid.toString().substring(0, 14);
            org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.unregister();
            }
        }
    }

    public synchronized VoteResult votePlus(UUID voter, String voterName, String voterIp, UUID target, String targetName) {
        if (voter.equals(target)) {
            return VoteResult.SELF_VOTE;
        }

        // Same IP check
        if (plugin.getConfig().getBoolean("settings.prevent-same-ip-voting", true)) {
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer != null && voterIp != null && !voterIp.isEmpty()) {
                String targetIp = targetPlayer.getAddress().getAddress().getHostAddress();
                if (voterIp.equals(targetIp)) {
                    return VoteResult.SAME_IP;
                }
            }
        }

        // Multi-account IP check
        if (plugin.getConfig().getBoolean("settings.prevent-multi-account-voting", true)) {
            if (voterIp != null && !voterIp.isEmpty()) {
                if (plugin.getDatabaseManager().hasIpVoted(voterIp, target, voter)) {
                    return VoteResult.IP_ALREADY_VOTED;
                }
            }
        }

        String existingVote = plugin.getDatabaseManager().getVote(voter, target);
        if ("PLUS".equals(existingVote)) {
            return VoteResult.ALREADY_VOTED;
        }

        DatabaseManager.PlayerRepData targetData = getPlayerData(target);
        double oldScore = targetData.getScore();

        if ("MINUS".equals(existingVote)) {
            targetData.setScore(Math.max(1.0, Math.min(10.0, oldScore + 1.0)));
            targetData.setPosVotes(targetData.getPosVotes() + 1);
            targetData.setNegVotes(Math.max(0, targetData.getNegVotes() - 1));
        } else {
            targetData.setScore(Math.max(1.0, Math.min(10.0, oldScore + 0.5)));
            targetData.setPosVotes(targetData.getPosVotes() + 1);
        }

        plugin.getDatabaseManager().saveVote(voter, target, "PLUS", voterIp);
        plugin.getDatabaseManager().savePlayerData(target, targetName, targetData);

        // Update online player prefix
        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer != null) {
            updatePrefixes(targetPlayer);
        }

        return VoteResult.SUCCESS;
    }

    public synchronized VoteResult voteMinus(UUID voter, String voterName, String voterIp, UUID target, String targetName) {
        if (voter.equals(target)) {
            return VoteResult.SELF_VOTE;
        }

        // Same IP check
        if (plugin.getConfig().getBoolean("settings.prevent-same-ip-voting", true)) {
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer != null && voterIp != null && !voterIp.isEmpty()) {
                String targetIp = targetPlayer.getAddress().getAddress().getHostAddress();
                if (voterIp.equals(targetIp)) {
                    return VoteResult.SAME_IP;
                }
            }
        }

        // Multi-account IP check
        if (plugin.getConfig().getBoolean("settings.prevent-multi-account-voting", true)) {
            if (voterIp != null && !voterIp.isEmpty()) {
                if (plugin.getDatabaseManager().hasIpVoted(voterIp, target, voter)) {
                    return VoteResult.IP_ALREADY_VOTED;
                }
            }
        }

        String existingVote = plugin.getDatabaseManager().getVote(voter, target);
        if ("MINUS".equals(existingVote)) {
            return VoteResult.ALREADY_VOTED;
        }

        DatabaseManager.PlayerRepData targetData = getPlayerData(target);
        double oldScore = targetData.getScore();

        if ("PLUS".equals(existingVote)) {
            targetData.setScore(Math.max(1.0, Math.min(10.0, oldScore - 1.0)));
            targetData.setPosVotes(Math.max(0, targetData.getPosVotes() - 1));
            targetData.setNegVotes(targetData.getNegVotes() + 1);
        } else {
            targetData.setScore(Math.max(1.0, Math.min(10.0, oldScore - 0.5)));
            targetData.setNegVotes(targetData.getNegVotes() + 1);
        }

        plugin.getDatabaseManager().saveVote(voter, target, "MINUS", voterIp);
        plugin.getDatabaseManager().savePlayerData(target, targetName, targetData);

        // Update online player prefix
        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer != null) {
            updatePrefixes(targetPlayer);
        }

        return VoteResult.SUCCESS;
    }

    public synchronized void deductKillPenalty(UUID killer, String killerName, UUID victim, String victimName) {
        if (!plugin.getConfig().getBoolean("settings.enable-kill-penalty", true)) {
            return;
        }

        double penalty = plugin.getConfig().getDouble("settings.kill-penalty", 0.2);
        DatabaseManager.PlayerRepData killerData = getPlayerData(killer);
        double newScore = Math.max(1.0, killerData.getScore() - penalty);
        killerData.setScore(newScore);

        plugin.getDatabaseManager().savePlayerData(killer, killerName, killerData);

        Player killerPlayer = Bukkit.getPlayer(killer);
        if (killerPlayer != null) {
            updatePrefixes(killerPlayer);
            
            // Notify killer in English
            String messageFormat = plugin.getConfig().getString("messages.kill-penalty", "<red>You lost <yellow>%penalty%</yellow> reputation for killing player <yellow>%victim%</yellow>!</red>");
            String msg = messageFormat.replace("%penalty%", String.valueOf(penalty))
                                      .replace("%victim%", victimName);
            killerPlayer.sendMessage(miniMessage.deserialize(msg));
        }
    }

    public synchronized void adminSet(UUID target, String targetName, double score) {
        double clampedScore = Math.max(1.0, Math.min(10.0, score));
        DatabaseManager.PlayerRepData targetData = getPlayerData(target);
        targetData.setScore(clampedScore);

        plugin.getDatabaseManager().savePlayerData(target, targetName, targetData);

        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer != null) {
            updatePrefixes(targetPlayer);
        }
    }
}
