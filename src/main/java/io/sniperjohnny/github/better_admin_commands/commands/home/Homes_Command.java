package io.sniperjohnny.github.better_admin_commands.commands.home;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Opens the home menu, where a home can be teleported to or deleted. {@code
 * /homes list} keeps the plain text listing.
 */
public class Homes_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Homes_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        Map<String, Location> homes = plugin.homes().homesOf(player.getUniqueId());
        if (homes.isEmpty()) {
            Msg.error(player, "You have no homes yet. Use /sethome to create one.");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
            Msg.raw(player, "&6Your homes &7(" + homes.size() + "/" + plugin.homes().limitFor(player) + ")&6:");
            for (Map.Entry<String, Location> entry : homes.entrySet()) {
                Location location = entry.getValue();
                Msg.raw(player, String.format(Locale.ROOT, " &8- &f%s &7(%s %.0f, %.0f, %.0f)",
                        entry.getKey(), location.getWorld().getName(),
                        location.getX(), location.getY(), location.getZ()));
            }
            Msg.raw(player, "&7Use &f/home <name> &7to teleport.");
            return true;
        }
        plugin.homeGui().open(player, 0);
        return true;
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
