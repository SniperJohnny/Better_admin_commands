package io.sniperjohnny.github.better_admin_commands.commands.items;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Renames the item in the main hand. */
public class Rename_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            Msg.error(player, "You have to hold an item.");
            return true;
        }
        String name = String.join(" ", args);
        ItemMeta meta = held.getItemMeta();
        if (name.equalsIgnoreCase("off") || name.equalsIgnoreCase("clear")) {
            meta.displayName(null);
            held.setItemMeta(meta);
            Msg.success(player, "The item name was removed.");
            return true;
        }
        if (name.contains("&") && !player.hasPermission("betteradmincommands.nick.color")) {
            name = name.replace("&", "");
        }
        meta.displayName(Msg.component(name));
        held.setItemMeta(meta);
        Msg.success(player, "Item renamed.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return List.of("off");
        }
        return Collections.emptyList();
    }

    static String lower(Material material) {
        return material.name().toLowerCase(Locale.ROOT);
    }
}
