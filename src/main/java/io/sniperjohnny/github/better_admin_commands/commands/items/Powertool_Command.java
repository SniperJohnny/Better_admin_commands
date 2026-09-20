package io.sniperjohnny.github.better_admin_commands.commands.items;

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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Binds a command to the item in your hand. */
public class Powertool_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Powertool_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
            Map<String, String> bindings = plugin.powerTools().bindings(player);
            if (bindings.isEmpty()) {
                Msg.send(player, "&7You have no powertools bound.");
                return true;
            }
            Msg.raw(player, "&6Your powertools:");
            bindings.forEach((material, bound) -> Msg.raw(player, " &8- &f" + material + " &7-> &f" + bound));
            return true;
        }

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            Msg.error(player, "You have to hold an item.");
            return true;
        }

        if (args.length >= 1 && (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("off"))) {
            plugin.powerTools().clear(player, held.getType());
            Msg.success(player, "The powertool for " + held.getType().name().toLowerCase(Locale.ROOT) + " was removed.");
            return true;
        }
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }

        String binding = String.join(" ", args);
        plugin.powerTools().bind(player, held.getType(), binding);
        Msg.success(player, "Bound &f/" + binding + " &ato "
                + held.getType().name().toLowerCase(Locale.ROOT) + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "clear", "list");
        }
        return Collections.emptyList();
    }
}
