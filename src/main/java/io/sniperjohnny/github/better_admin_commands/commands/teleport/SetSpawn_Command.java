package io.sniperjohnny.github.better_admin_commands.commands.teleport;

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

/** Saves the current coordinates of the sender as the server spawn. */
public class SetSpawn_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public SetSpawn_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        Location location = player.getLocation();
        plugin.spawns().setSpawn(location);

        String coords = String.format(Locale.ROOT, "%.2f, %.2f, %.2f", location.getX(), location.getY(), location.getZ());
        Msg.success(player, "Spawn set to your current position (" + coords + ") in world "
                + location.getWorld().getName() + ".");
        Msg.send(player, "&7Saved to &fspawn.yml&7. Players can now use &f/spawn&7.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
