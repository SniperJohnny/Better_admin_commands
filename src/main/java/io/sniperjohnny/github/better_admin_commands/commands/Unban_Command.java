package io.sniperjohnny.github.better_admin_commands.commands;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Removes a name ban. */
public class Unban_Command implements TabExecutor {

    @Override
    @SuppressWarnings("rawtypes")
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        String playerName = args[0];
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        if (!banList.isBanned(playerName)) {
            Msg.error(sender, playerName + " is not banned.");
            return true;
        }
        banList.pardon(playerName);
        Msg.success(sender, playerName + " was unbanned.");
        Bukkit.broadcast(Msg.color("&8[&6BetterAdmin&8] &f" + playerName + " &7was unbanned by &f"
                + sender.getName() + "&7."), "betteradmincommands.unban.notify");
        return true;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> bannedNames = new ArrayList<>();
            for (Object entry : Bukkit.getBanList(BanList.Type.NAME).getEntries()) {
                if (entry instanceof org.bukkit.BanEntry banEntry) {
                    bannedNames.add(String.valueOf(banEntry.getTarget()));
                }
            }
            return Targets.completeFrom(args[0], bannedNames);
        }
        return Collections.emptyList();
    }
}
