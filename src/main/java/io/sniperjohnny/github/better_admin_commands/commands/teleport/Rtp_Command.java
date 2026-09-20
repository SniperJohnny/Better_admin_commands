package io.sniperjohnny.github.better_admin_commands.commands.teleport;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
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
import java.util.Random;

/** Randomly teleports you to a safe spot in your world. */
public class Rtp_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;
    private final Random random = new Random();

    public Rtp_Command(Better_Admin_Commands plugin) {
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
        if (plugin.getConfig().getStringList("rtp.disabled-worlds").contains(world.getName())) {
            Msg.error(player, "Random teleporting is disabled in this world.");
            return true;
        }

        int max = Math.max(100, plugin.getConfig().getInt("rtp.max-radius", 2000));
        int min = Math.max(0, plugin.getConfig().getInt("rtp.min-radius", 200));
        int attempts = Math.max(1, plugin.getConfig().getInt("rtp.max-attempts", 30));
        Location original = player.getLocation();

        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = random.nextInt(max * 2 + 1) - max;
            int z = random.nextInt(max * 2 + 1) - max;
            if (Math.abs(x) < min && Math.abs(z) < min) {
                continue;
            }
            int y = world.getHighestBlockYAt(x, z);
            if (y <= world.getMinHeight() + 1) {
                continue;
            }
            Block ground = world.getBlockAt(x, y, z);
            if (!ground.getType().isSolid() || ground.isLiquid()) {
                continue;
            }
            if (!world.getBlockAt(x, y + 1, z).isPassable() || !world.getBlockAt(x, y + 2, z).isPassable()) {
                continue;
            }
            Location destination = new Location(world, x + 0.5, y + 1, z + 0.5,
                    original.getYaw(), original.getPitch());
            plugin.teleports().teleportNow(player, destination);
            Msg.success(player, String.format("Teleported to &f%d&a, &f%d&a.", x, z));
            return true;
        }
        Msg.error(player, "No safe spot was found, please try again.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }

    static double distanceHint(Location from, Location to) {
        return from.distance(to);
    }
}
