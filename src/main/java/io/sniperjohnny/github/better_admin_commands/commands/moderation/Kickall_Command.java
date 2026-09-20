package io.sniperjohnny.github.better_admin_commands.commands.moderation;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Kicks every player from the server. */
public class Kickall_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        String reason = args.length >= 1 ? String.join(" ", args) : "The server was cleared";
        int kicked = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(sender)) {
                player.kick(Component.text(Msg.color("&cYou were kicked.\n&7Reason: &f" + reason)));
                kicked++;
            }
        }
        Msg.success(sender, "Kicked " + kicked + " player(s).");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
