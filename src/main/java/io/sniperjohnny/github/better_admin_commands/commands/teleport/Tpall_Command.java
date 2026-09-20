package io.sniperjohnny.github.better_admin_commands.commands.teleport;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Teleports every online player (except the sender) to the sender. */
public class Tpall_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Tpall_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player self)) {
            Msg.playerOnly(sender);
            return true;
        }
        int moved = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(self)) {
                continue;
            }
            plugin.teleports().teleportNow(online, self.getLocation());
            Msg.send(online, "&7You were teleported to &f" + self.getName() + "&7.");
            moved++;
        }
        Msg.success(self, "Teleported " + moved + " player(s) to you.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
