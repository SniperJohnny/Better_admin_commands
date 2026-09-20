package io.sniperjohnny.github.better_admin_commands.commands.items;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Edits the lore of the item in the main hand. */
public class Lore_Command implements TabExecutor {

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
        ItemMeta meta = held.getItemMeta();
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                if (args.length < 2) {
                    Msg.usage(sender, command);
                    return true;
                }
                lore.add(Msg.component(String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))));
            }
            case "set" -> {
                if (args.length < 3) {
                    Msg.usage(sender, command);
                    return true;
                }
                Integer index = Targets.parseInt(args[1]);
                if (index == null || index < 1) {
                    Msg.error(player, "The line number has to be 1 or higher.");
                    return true;
                }
                String text = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                while (lore.size() < index) {
                    lore.add(Component.empty());
                }
                lore.set(index - 1, Msg.component(text));
            }
            case "clear", "reset" -> lore.clear();
            default -> {
                Msg.error(player, "Use /lore <add|set|clear> [line] [text].");
                return true;
            }
        }

        meta.lore(lore.isEmpty() ? null : lore);
        held.setItemMeta(meta);
        Msg.success(player, "Lore updated.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "add", "set", "clear");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Targets.completeFrom(args[1], "1", "2", "3");
        }
        return Collections.emptyList();
    }
}
