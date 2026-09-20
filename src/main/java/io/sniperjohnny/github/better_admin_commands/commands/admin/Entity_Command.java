package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Entity clean up helpers, registered for {@code /killall}, {@code /butcher}
 * and {@code /remove}.
 *
 * <ul>
 *     <li>{@code /killall [type]} kills every matching entity in every world</li>
 *     <li>{@code /butcher [radius]} removes hostile mobs around you</li>
 *     <li>{@code /remove <type> [radius]} removes one entity type around you</li>
 * </ul>
 */
public class Entity_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        return switch (name) {
            case "killall" -> killAll(sender, args);
            case "butcher" -> butcher(sender, args);
            default -> remove(sender, args);
        };
    }

    private boolean killAll(CommandSender sender, String[] args) {
        EntityType filter = null;
        if (args.length >= 1) {
            filter = EntityType.fromName(args[0].toLowerCase(Locale.ROOT));
            if (filter == null) {
                Msg.error(sender, "There is no entity type called " + args[0] + ".");
                return true;
            }
        }
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Player) {
                    continue;
                }
                if (filter != null && entity.getType() != filter) {
                    continue;
                }
                if (filter == null && !(entity instanceof LivingEntity)) {
                    continue;
                }
                entity.remove();
                removed++;
            }
        }
        Msg.success(sender, "Removed " + removed + " entity/entities.");
        return true;
    }

    private boolean butcher(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        double radius = 50;
        if (args.length >= 1) {
            Double parsed = Targets.parseDouble(args[0]);
            if (parsed == null) {
                Msg.error(sender, "The radius has to be a number.");
                return true;
            }
            radius = Math.max(1, parsed);
        }
        int removed = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)) {
                living.remove();
                removed++;
            }
        }
        Msg.success(sender, "Removed " + removed + " mob(s) within " + (int) radius + " blocks.");
        return true;
    }

    private boolean remove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length < 1) {
            Msg.error(sender, "Usage: /remove <type> [radius]");
            return true;
        }
        EntityType type = EntityType.fromName(args[0].toLowerCase(Locale.ROOT));
        if (type == null) {
            Msg.error(sender, "There is no entity type called " + args[0] + ".");
            return true;
        }
        double radius = 50;
        if (args.length >= 2) {
            Double parsed = Targets.parseDouble(args[1]);
            if (parsed != null) {
                radius = Math.max(1, parsed);
            }
        }
        int removed = 0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity.getType() == type) {
                entity.remove();
                removed++;
            }
        }
        Msg.success(sender, "Removed " + removed + "x " + args[0].toLowerCase(Locale.ROOT) + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            if (name.equals("butcher")) {
                return Targets.completeFrom(args[0], "20", "50", "100");
            }
            List<String> names = new ArrayList<>();
            for (EntityType type : EntityType.values()) {
                if (type.getEntityClass() != null) {
                    names.add(type.name().toLowerCase(Locale.ROOT));
                }
            }
            return Targets.completeFrom(args[0].toLowerCase(Locale.ROOT), names);
        }
        if (name.equals("remove") && args.length == 2) {
            return Targets.completeFrom(args[1], "20", "50", "100");
        }
        return Collections.emptyList();
    }
}
