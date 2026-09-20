package io.sniperjohnny.github.better_admin_commands.commands.info;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Finds the player behind a nickname. */
public class Realname_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Realname_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        String query = args[0];
        List<String> matches = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String nickname = plugin.preferences().nickname(player.getUniqueId());
            if (nickname != null && nickname.equalsIgnoreCase(query)) {
                matches.add(player.getName());
            }
        }
        if (matches.isEmpty()) {
            Msg.send(sender, "&7Nobody is using the nickname &f" + query + "&7.");
            return true;
        }
        Msg.send(sender, "&f" + query + " &7is &f" + String.join("&7, &f", matches) + "&7.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> nicknames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                String nickname = plugin.preferences().nickname(player.getUniqueId());
                if (nickname != null) {
                    nicknames.add(nickname);
                }
            }
            return io.sniperjohnny.github.better_admin_commands.util.Targets
                    .completeFrom(args[0].toLowerCase(), nicknames);
        }
        return Collections.emptyList();
    }
}
