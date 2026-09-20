package io.sniperjohnny.github.better_admin_commands.commands.economy;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Shows what an item is worth. */
public class Worth_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Worth_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        Material material;
        if (args.length >= 1 && !args[0].equalsIgnoreCase("hand")) {
            material = Material.matchMaterial(args[0].toUpperCase(Locale.ROOT));
            if (material == null || material.isAir()) {
                Msg.error(sender, "There is no item called " + args[0] + ".");
                return true;
            }
        } else if (sender instanceof Player player) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType().isAir()) {
                Msg.error(player, "You have to hold an item or name one: /worth <item>.");
                return true;
            }
            material = held.getType();
        } else {
            Msg.usage(sender, command);
            return true;
        }

        double price = plugin.worth().price(material);
        if (price <= 0) {
            Msg.send(sender, "&f" + plugin.worth().nameOf(material) + " &7cannot be sold.");
            return true;
        }
        Msg.send(sender, "&f" + plugin.worth().nameOf(material) + " &7is worth &a"
                + plugin.worth().format(price) + " &7each.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            names.add("hand");
            for (Material material : Material.values()) {
                if (material.isItem()) {
                    names.add(material.name().toLowerCase(Locale.ROOT));
                }
            }
            return Targets.completeFrom(args[0].toLowerCase(Locale.ROOT), names);
        }
        return Collections.emptyList();
    }
}
