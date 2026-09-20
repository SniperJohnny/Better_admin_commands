package io.sniperjohnny.github.better_admin_commands.commands.teleport;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * /tp &lt;player&gt; - teleport yourself to a player.
 * /tp &lt;x&gt; &lt;y&gt; &lt;z&gt; - teleport yourself to coordinates.
 * /tp &lt;player&gt; &lt;target&gt; - teleport a player to another player.
 * /tp &lt;player&gt; &lt;x&gt; &lt;y&gt; &lt;z&gt; - teleport a player to coordinates.
 */
public class Tp_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Tp_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player self)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length == 0) {
            Msg.usage(sender, command);
            return true;
        }
        if (args.length == 1) {
            Player target = Targets.online(sender, args[0]);
            if (target == null) {
                return true;
            }
            plugin.teleports().requestTeleport(self, target.getLocation());
            Msg.success(self, "Teleported to " + target.getName() + ".");
            return true;
        }
        if (args.length == 2) {
            Player who = Targets.online(sender, args[0]);
            Player target = Targets.online(sender, args[1]);
            if (who == null || target == null) {
                return true;
            }
            plugin.teleports().teleportNow(who, target.getLocation());
            Msg.success(sender, "Teleported " + who.getName() + " to " + target.getName() + ".");
            return true;
        }
        if (args.length == 3) {
            Location location = coordinates(self, args, 0);
            if (location == null) {
                Msg.error(sender, "Invalid coordinates.");
                return true;
            }
            plugin.teleports().requestTeleport(self, location);
            Msg.success(self, "Teleported to " + format(location) + ".");
            return true;
        }
        if (args.length == 4) {
            Player who = Targets.online(sender, args[0]);
            if (who == null) {
                return true;
            }
            Location location = coordinates(who, args, 1);
            if (location == null) {
                Msg.error(sender, "Invalid coordinates.");
                return true;
            }
            plugin.teleports().teleportNow(who, location);
            Msg.success(sender, "Teleported " + who.getName() + " to " + format(location) + ".");
            return true;
        }
        Msg.usage(sender, command);
        return true;
    }

    static Location coordinates(Player reference, String[] args, int offset) {
        Double x = Targets.parseDouble(args[offset]);
        Double y = Targets.parseDouble(args[offset + 1]);
        Double z = Targets.parseDouble(args[offset + 2]);
        if (x == null || y == null || z == null) {
            return null;
        }
        return new Location(reference.getWorld(), x, y, z, reference.getLocation().getYaw(),
                reference.getLocation().getPitch());
    }

    static String format(Location location) {
        return String.format(java.util.Locale.ROOT, "%.1f, %.1f, %.1f", location.getX(), location.getY(), location.getZ());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1 || args.length == 2) {
            return Targets.complete(args[args.length - 1]);
        }
        return Collections.emptyList();
    }
}
