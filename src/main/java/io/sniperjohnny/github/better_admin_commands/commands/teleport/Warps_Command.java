package io.sniperjohnny.github.better_admin_commands.commands.teleport;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Lists all available warps. */
public class Warps_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Warps_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (plugin.warps().size() == 0) {
            Msg.error(sender, "There are no warps yet.");
            return true;
        }
        Msg.raw(sender, "&6Warps &7(" + plugin.warps().size() + ")&6:");
        for (String name : plugin.warps().names()) {
            Msg.raw(sender, " &8- &f" + name);
        }
        Msg.raw(sender, "&7Use &f/warp <name> &7to teleport.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
