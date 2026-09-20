package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Gives a kit defined in config.yml to the sender. */
public class Kit_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Kit_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length < 1) {
            Msg.send(player, "&7Available kits: &f" + String.join(", ", plugin.kits().names()));
            Msg.usage(sender, command);
            return true;
        }
        String name = args[0].toLowerCase();
        if (!plugin.kits().exists(name)) {
            Msg.error(player, "There is no kit called " + name + ".");
            return true;
        }
        String permission = plugin.kits().permission(name);
        if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) {
            Msg.noPermission(player);
            return true;
        }

        long remaining = plugin.kits().remainingMillis(player, name);
        if (remaining > 0) {
            Msg.error(player, "You have to wait " + Targets.formatDuration(remaining / 1000L)
                    + " before using this kit again.");
            return true;
        }

        List<ItemStack> items = plugin.kits().items(name);
        if (items.isEmpty()) {
            Msg.error(player, "This kit does not contain any items.");
            return true;
        }
        for (ItemStack stack : items) {
            player.getInventory().addItem(stack).forEach((index, leftover) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
        plugin.kits().markUsed(player, name);
        Msg.success(player, "You received the kit " + name + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], plugin.kits().names());
        }
        return Collections.emptyList();
    }
}
