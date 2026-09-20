package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Shows how an item is crafted or smelted. */
public class Recipe_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        Material material;
        if (args.length >= 1) {
            material = Material.matchMaterial(args[0].toUpperCase(Locale.ROOT));
            if (material == null || material.isAir()) {
                Msg.error(sender, "There is no item called " + args[0] + ".");
                return true;
            }
        } else if (sender instanceof Player player) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType().isAir()) {
                Msg.error(player, "You have to hold an item or name one: /recipe <item>.");
                return true;
            }
            material = held.getType();
        } else {
            Msg.usage(sender, command);
            return true;
        }

        List<Recipe> recipes = Bukkit.getRecipesFor(new ItemStack(material));
        if (recipes.isEmpty()) {
            Msg.send(sender, "&7There is no recipe for &f"
                    + material.name().toLowerCase(Locale.ROOT) + "&7.");
            return true;
        }

        Msg.raw(sender, "&6Recipes for " + material.name().toLowerCase(Locale.ROOT) + ":");
        int index = 0;
        for (Recipe recipe : recipes) {
            if (++index > 5 && recipes.size() > 5) {
                Msg.raw(sender, " &7... and " + (recipes.size() - 5) + " more.");
                break;
            }
            describe(sender, recipe);
        }
        return true;
    }

    private void describe(CommandSender sender, Recipe recipe) {
        if (recipe instanceof ShapedRecipe shaped) {
            Msg.raw(sender, " &8- &7crafting table:");
            for (String row : shaped.getShape()) {
                StringBuilder builder = new StringBuilder("    ");
                for (char symbol : row.toCharArray()) {
                    ItemStack ingredient = shaped.getIngredientMap().get(symbol);
                    builder.append(ingredient == null ? "[ ]" : "[" + shortName(ingredient.getType()) + "]");
                }
                Msg.raw(sender, builder.toString());
            }
            return;
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            List<String> names = new ArrayList<>();
            for (ItemStack ingredient : shapeless.getIngredientList()) {
                if (ingredient != null) {
                    names.add(shortName(ingredient.getType()));
                }
            }
            Msg.raw(sender, " &8- &7shapeless: &f" + String.join(", ", names));
            return;
        }
        if (recipe instanceof FurnaceRecipe furnace) {
            Msg.raw(sender, " &8- &7smelting &f" + shortName(furnace.getInput().getType()));
            return;
        }
        Msg.raw(sender, " &8- &7" + recipe.getClass().getSimpleName());
    }

    private String shortName(Material material) {
        return material.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
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
