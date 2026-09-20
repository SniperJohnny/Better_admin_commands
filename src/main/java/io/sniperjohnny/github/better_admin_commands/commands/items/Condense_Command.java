package io.sniperjohnny.github.better_admin_commands.commands.items;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
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
import java.util.Map;

/**
 * Combines loose materials into their block form, e.g. nine iron ingots
 * become one iron block.
 */
public class Condense_Command implements TabExecutor {

    /** Material to block mapping for everything that condenses 9:1. */
    private static final Map<Material, Material> NINE_TO_ONE = Map.ofEntries(
            Map.entry(Material.IRON_INGOT, Material.IRON_BLOCK),
            Map.entry(Material.GOLD_INGOT, Material.GOLD_BLOCK),
            Map.entry(Material.DIAMOND, Material.DIAMOND_BLOCK),
            Map.entry(Material.EMERALD, Material.EMERALD_BLOCK),
            Map.entry(Material.REDSTONE, Material.REDSTONE_BLOCK),
            Map.entry(Material.COAL, Material.COAL_BLOCK),
            Map.entry(Material.LAPIS_LAZULI, Material.LAPIS_BLOCK),
            Map.entry(Material.NETHERITE_INGOT, Material.NETHERITE_BLOCK),
            Map.entry(Material.COPPER_INGOT, Material.COPPER_BLOCK),
            Map.entry(Material.WHEAT, Material.HAY_BLOCK),
            Map.entry(Material.SLIME_BALL, Material.SLIME_BLOCK),
            Map.entry(Material.BONE_MEAL, Material.BONE_BLOCK),
            Map.entry(Material.DRIED_KELP, Material.DRIED_KELP_BLOCK),
            Map.entry(Material.IRON_NUGGET, Material.IRON_INGOT),
            Map.entry(Material.GOLD_NUGGET, Material.GOLD_INGOT));

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        ItemStack[] contents = player.getInventory().getStorageContents();
        int converted = 0;

        for (int index = 0; index < contents.length; index++) {
            ItemStack stack = contents[index];
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            Material block = NINE_TO_ONE.get(stack.getType());
            if (block == null || stack.getAmount() < 9) {
                continue;
            }
            int groups = stack.getAmount() / 9;
            int rest = stack.getAmount() % 9;
            converted += groups;

            ItemStack residual = rest == 0 ? null : new ItemStack(stack.getType(), rest);
            contents[index] = residual;

            int remaining = groups;
            while (remaining > 0) {
                int size = Math.min(remaining, block.getMaxStackSize());
                ItemStack blockStack = new ItemStack(block, size);
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(blockStack);
                leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                remaining -= size;
            }
        }
        player.getInventory().setStorageContents(contents);

        if (converted == 0) {
            Msg.error(player, "You have nothing that can be condensed.");
            return true;
        }
        Msg.success(player, "Condensed " + converted + " stack(s).");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }

    static List<Material> condenseable() {
        return new ArrayList<>(NINE_TO_ONE.keySet());
    }
}
