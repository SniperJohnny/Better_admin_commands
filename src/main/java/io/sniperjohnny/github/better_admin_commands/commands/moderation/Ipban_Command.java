package io.sniperjohnny.github.better_admin_commands.commands.moderation;

import io.sniperjohnny.github.better_admin_commands.notify.NotificationService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Bans the IP address of an online player. */
public class Ipban_Command implements TabExecutor {

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        Player target = Targets.online(sender, args[0]);
        if (target == null) {
            return true;
        }
        if (target.getAddress() == null) {
            Msg.error(sender, "The IP address of " + target.getName() + " could not be read.");
            return true;
        }
        String ip = target.getAddress().getAddress().getHostAddress();
        String reason = args.length >= 2
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "IP banned by an operator";

        BanList banList = Bukkit.getBanList(BanList.Type.IP);
        banList.addBan(ip, reason, (Date) null, sender.getName());
        target.kick(Component.text(Msg.color("&cYour IP is banned.\n&7Reason: &f" + reason)));

        NotificationService.staffBroadcast("ban", "betteradmincommands.ban.notify",
                "&f" + target.getName() + " &7was IP banned by &f" + sender.getName() + "&7.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.complete(args[0]);
        }
        return Collections.emptyList();
    }
}
