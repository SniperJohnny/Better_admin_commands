package io.sniperjohnny.github.better_admin_commands.commands.teleport;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Opens the warp menu, where every warp is a button. {@code /warps list} keeps
 * the plain text listing, and the console always gets the text form.
 */
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
        boolean wantsList = args.length >= 1 && args[0].equalsIgnoreCase("list");
        if (sender instanceof Player player && !wantsList) {
            plugin.warpGui().open(player, 0);
            return true;
        }
        list(sender);
        return true;
    }

    private void list(CommandSender sender) {
        Msg.raw(sender, "&6Warps &7(" + plugin.warps().size() + ")&6:");
        for (String name : plugin.warps().names()) {
            Msg.raw(sender, " &8- &f" + name);
        }
        Msg.raw(sender, "&7Use &f/warp <name> &7to teleport.");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return "list".startsWith(args[0].toLowerCase()) ? List.of("list") : Collections.emptyList();
        }
        return Collections.emptyList();
    }
}
