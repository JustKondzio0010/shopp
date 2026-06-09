package pl.konrad.vipshop.auditor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import pl.konrad.vipshop.VipShop;

public class UnfreezeCommand implements CommandExecutor {

    private final VipShop plugin;

    public UnfreezeCommand(VipShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("vipshop.admin.unfreeze")) {
            sender.sendMessage(Component.text("Brak uprawnień!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("Użycie: /unfreeze <nick>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Nie znaleziono gracza online o nicku " + args[0], NamedTextColor.RED));
            return true;
        }

        if (!plugin.getEconomyAuditor().isFrozen(target.getUniqueId())) {
            sender.sendMessage(Component.text("Ten gracz nie jest zamrożony!", NamedTextColor.YELLOW));
            return true;
        }

        plugin.getEconomyAuditor().unfreezePlayer(target.getUniqueId());
        
        sender.sendMessage(Component.text("Pomyślnie odblokowano gracza " + target.getName() + "!", NamedTextColor.GREEN));
        target.sendMessage(Component.text("Twoje konto zostało pomyślnie odblokowane przez administratora.", NamedTextColor.GREEN));
        
        return true;
    }
}
