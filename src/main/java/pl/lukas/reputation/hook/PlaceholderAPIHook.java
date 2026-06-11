package pl.lukas.reputation.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.lukas.reputation.ReputationPlugin;
import pl.lukas.reputation.db.DatabaseManager;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final ReputationPlugin plugin;

    public PlaceholderAPIHook(ReputationPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "reputation";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getPluginMeta().getAuthors().isEmpty() ? "Lukas" : plugin.getPluginMeta().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        DatabaseManager.PlayerRepData data = plugin.getReputationManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            return "";
        }

        if (params.equalsIgnoreCase("score")) {
            return String.format("%.2f", data.getScore());
        }

        if (params.equalsIgnoreCase("score_int")) {
            return String.valueOf(plugin.getReputationManager().getRoundedScore(data.getScore()));
        }

        if (params.equalsIgnoreCase("heart")) {
            int rounded = plugin.getReputationManager().getRoundedScore(data.getScore());
            return plugin.getReputationManager().getHeartEmoji(rounded);
        }

        if (params.equalsIgnoreCase("prefix")) {
            return plugin.getReputationManager().getPrefixString(player.getUniqueId());
        }

        return null;
    }
}
