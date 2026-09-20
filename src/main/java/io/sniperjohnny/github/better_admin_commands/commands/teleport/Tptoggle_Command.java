package io.sniperjohnny.github.better_admin_commands.commands.teleport;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.player.PlayerPreferences;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Toggles whether other players may send you teleport requests. */
public class Tptoggle_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Tptoggle_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        boolean blocked = !plugin.preferences()
                .getBoolean(player.getUniqueId(), PlayerPreferences.TELEPORT_TOGGLE, true);
        plugin.preferences().setBoolean(player.getUniqueId(), PlayerPreferences.TELEPORT_TOGGLE, blocked);
        Msg.success(player, blocked
                ? "Other players may no longer send you teleport requests."
                : "Other players may send you teleport requests again.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
