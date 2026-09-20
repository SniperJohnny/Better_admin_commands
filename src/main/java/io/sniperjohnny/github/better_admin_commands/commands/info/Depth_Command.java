package io.sniperjohnny.github.better_admin_commands.commands.info;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Shows how deep the sender is, both as a Y value and below sea level. */
public class Depth_Command implements TabExecutor {

    /** Sea level, the reference used for the "depth below sea" value. */
    private static final int SEA_LEVEL = 63;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        int y = player.getLocation().getBlockY();
        int belowSea = SEA_LEVEL - y;
        Msg.send(player, "&7You are at &fY " + y + "&7, which is &f"
                + Math.abs(belowSea) + " &7blocks " + (belowSea > 0 ? "below" : "above") + " sea level.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
