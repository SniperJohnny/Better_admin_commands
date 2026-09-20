package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Spawns one or more mobs at a player. */
public class Spawnmob_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        EntityType type = EntityType.fromName(args[0].toLowerCase(Locale.ROOT));
        if (type == null || type.getEntityClass() == null) {
            Msg.error(sender, "There is no mob called " + args[0] + ".");
            return true;
        }

        int amount = 1;
        if (args.length >= 2) {
            Integer parsed = Targets.parseInt(args[1]);
            if (parsed == null || parsed < 1) {
                Msg.error(sender, "The amount has to be a positive number.");
                return true;
            }
            amount = Math.min(parsed, 100);
        }

        Player target;
        if (args.length >= 3) {
            target = Targets.online(sender, args[2]);
            if (target == null) {
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            Msg.playerOnly(sender);
            return true;
        }

        Location location = target.getLocation();
        for (int index = 0; index < amount; index++) {
            target.getWorld().spawnEntity(location, type);
        }
        Msg.success(sender, "Spawned " + amount + "x " + args[0].toLowerCase(Locale.ROOT)
                + " at " + target.getName() + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (EntityType type : EntityType.values()) {
                if (!type.isSpawnable() || type.getEntityClass() == null) {
                    continue;
                }
                names.add(type.name().toLowerCase(Locale.ROOT));
            }
            return Targets.completeFrom(args[0], names);
        }
        if (args.length == 2) {
            return Targets.completeFrom(args[1], "1", "5", "10");
        }
        if (args.length == 3) {
            return Targets.complete(args[2]);
        }
        return Collections.emptyList();
    }
}
