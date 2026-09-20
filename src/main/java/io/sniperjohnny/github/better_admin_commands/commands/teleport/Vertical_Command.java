package io.sniperjohnny.github.better_admin_commands.commands.teleport;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Vertical movement helpers, registered for {@code /top}, {@code /bottom} and
 * {@code /descend}.
 */
public class Vertical_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Vertical_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        World world = player.getWorld();
        Location current = player.getLocation();
        int x = current.getBlockX();
        int z = current.getBlockZ();

        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "top" -> {
                int y = world.getHighestBlockYAt(x, z) + 1;
                if (y <= world.getMinHeight() + 1) {
                    Msg.error(player, "There is no safe spot above you.");
                    return true;
                }
                plugin.teleports().teleportNow(player, stand(world, x, y, z, current));
                Msg.success(player, "Teleported to the top.");
            }
            case "bottom" -> {
                Location destination = scan(world, x, z, world.getMinHeight() + 1, world.getMaxHeight() - 2, 1, current);
                if (destination == null) {
                    Msg.error(player, "There is no safe spot below you.");
                    return true;
                }
                plugin.teleports().teleportNow(player, destination);
                Msg.success(player, "Teleported to the bottom.");
            }
            case "descend" -> {
                Location destination = scan(world, x, z, current.getBlockY() - 1, world.getMinHeight() + 1, -1, current);
                if (destination == null) {
                    Msg.error(player, "There is no safe spot below you.");
                    return true;
                }
                plugin.teleports().teleportNow(player, destination);
                Msg.success(player, "Teleported down.");
            }
            default -> Msg.usage(sender, command);
        }
        return true;
    }

    /** Walks from start to end looking for a two block high pocket. */
    private Location scan(World world, int x, int z, int start, int end, int step, Location original) {
        for (int y = start; step > 0 ? y <= end : y >= end; y += step) {
            Block below = world.getBlockAt(x, y - 1, z);
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            if (below.getType().isSolid() && !below.isLiquid()
                    && feet.isPassable() && head.isPassable()) {
                return stand(world, x, y, z, original);
            }
        }
        return null;
    }

    private Location stand(World world, int x, int y, int z, Location original) {
        return new Location(world, x + 0.5, y, z + 0.5, original.getYaw(), original.getPitch());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
