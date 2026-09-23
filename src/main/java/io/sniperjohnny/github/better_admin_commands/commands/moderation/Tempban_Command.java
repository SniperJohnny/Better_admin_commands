package io.sniperjohnny.github.better_admin_commands.commands.moderation;

import io.sniperjohnny.github.better_admin_commands.notify.NotificationService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

/** Temporarily bans a player. */
public class Tempban_Command implements TabExecutor {

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 2) {
            Msg.usage(sender, command);
            return true;
        }
        OfflinePlayer target = Targets.offline(sender, args[0]);
        if (target == null) {
            return true;
        }
        Long seconds = Targets.parseDuration(args[1]);
        if (seconds == null || seconds <= 0) {
            Msg.error(sender, "The duration has to look like 30m, 2h or 7d.");
            return true;
        }
        String name = target.getName() == null ? args[0] : target.getName();
        String reason = args.length >= 3
                ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : "Temporarily banned by an operator";

        Date expires = new Date(System.currentTimeMillis() + (seconds * 1000L));
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        banList.addBan(name, reason, expires, sender.getName());

        Player online = target.getPlayer();
        if (online != null) {
            online.kick(Component.text(Msg.color("&cYou are banned for "
                    + Targets.formatDuration(seconds) + ".\n&7Reason: &f" + reason)));
        }
        NotificationService.staffBroadcast("ban", "betteradmincommands.ban.notify",
                "&f" + name + " &7was banned for &f" + Targets.formatDuration(seconds)
                        + " &7by &f" + sender.getName() + "&7.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.complete(args[0]);
        }
        if (args.length == 2) {
            return Targets.completeFrom(args[1], "1h", "1d", "7d", "30d");
        }
        return Collections.emptyList();
    }
}
