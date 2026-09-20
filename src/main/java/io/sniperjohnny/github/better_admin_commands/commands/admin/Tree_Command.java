package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Grows a tree at the block you are looking at. Registered for {@code /tree}
 * and {@code /bigtree}.
 */
public class Tree_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        boolean big = command.getName().equalsIgnoreCase("bigtree");
        TreeType type;
        if (args.length >= 1) {
            type = parseType(args[0]);
            if (type == null) {
                Msg.error(player, "Unknown tree type. Try TREE, BIG_TREE, REDWOOD, BIRCH, JUNGLE or MEGA_REDWOOD.");
                return true;
            }
        } else {
            type = big ? TreeType.BIG_TREE : TreeType.TREE;
        }

        Block target = player.getTargetBlockExact(20);
        Location where = target == null ? player.getLocation() : target.getLocation();

        if (!player.getWorld().generateTree(where, type)) {
            Msg.error(player, "A tree of that type cannot grow here, try another spot.");
            return true;
        }
        Msg.success(player, "Grew a " + type.name().toLowerCase(Locale.ROOT) + ".");
        return true;
    }

    private TreeType parseType(String raw) {
        try {
            return TreeType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> types = new ArrayList<>();
            for (TreeType type : TreeType.values()) {
                types.add(type.name().toLowerCase(Locale.ROOT));
            }
            return Targets.completeFrom(args[0], types);
        }
        return Collections.emptyList();
    }
}
