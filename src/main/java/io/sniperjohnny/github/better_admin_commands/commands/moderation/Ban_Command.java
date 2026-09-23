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

/** Bans a player by name, using the vanilla ban list. */
public class Ban_Command implements TabExecutor {

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        String name = args[0];
        String reason = args.length >= 2
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "Banned by an operator";

        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        banList.addBan(name, reason, (Date) null, sender.getName());

        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            online.kick(Component.text(Msg.color("&cYou are banned.\n&7Reason: &f" + reason)));
        }

        NotificationService.staffBroadcast("ban", "betteradmincommands.ban.notify",
                "&f" + name + " &7was banned by &f" + sender.getName()
                        + "&7. Reason: &f" + reason);
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
