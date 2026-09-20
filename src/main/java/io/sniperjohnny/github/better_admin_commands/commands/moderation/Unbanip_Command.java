package io.sniperjohnny.github.better_admin_commands.commands.moderation;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Removes an IP ban, accepting either an address or a player name. */
public class Unbanip_Command implements TabExecutor {

    @Override
    @SuppressWarnings("rawtypes")
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        String input = args[0];
        String address = input;
        if (!input.contains(".")) {
            Player online = Bukkit.getPlayerExact(input);
            OfflinePlayer offline = online != null ? online : Bukkit.getOfflinePlayer(input);
            if (online != null && online.getAddress() != null) {
                address = online.getAddress().getAddress().getHostAddress();
            } else if (offline.getPlayer() != null && offline.getPlayer().getAddress() != null) {
                address = offline.getPlayer().getAddress().getAddress().getHostAddress();
            }
        }

        BanList banList = Bukkit.getBanList(BanList.Type.IP);
        if (!banList.isBanned(address)) {
            Msg.error(sender, "The IP " + address + " is not banned.");
            return true;
        }
        banList.pardon(address);
        Msg.success(sender, "The IP " + address + " was unbanned.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return io.sniperjohnny.github.better_admin_commands.util.Targets.complete(args[0]);
        }
        return Collections.emptyList();
    }
}
