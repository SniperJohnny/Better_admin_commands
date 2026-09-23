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

/** Lists every online player, each with their rank prefix and nickname. */
public class List_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public List_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(plugin.preferences().displayName(player.getUniqueId(), player.getName()));
        }
        names.sort(String::compareToIgnoreCase);

        Msg.raw(sender, "&6Online players &7(" + names.size() + "/" + Bukkit.getMaxPlayers() + ")&6:");
        Msg.raw(sender, " " + String.join("&7, ", names));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
