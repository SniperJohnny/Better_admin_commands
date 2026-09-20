package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
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

/** Changes the type of a mob spawner. */
public class Spawner_Command implements TabExecutor {

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
        EntityType type = EntityType.fromName(args[0].toLowerCase(Locale.ROOT));
        if (type == null) {
            Msg.error(player, "There is no mob called " + args[0] + ".");
            return true;
        }

        Block target = player.getTargetBlockExact(10);
        if (target == null || target.getType() != Material.SPAWNER) {
            Msg.error(player, "You have to look at a mob spawner (within 10 blocks).");
            return true;
        }
        if (target.getState() instanceof CreatureSpawner spawner) {
            spawner.setSpawnedType(type);
            spawner.update(true);
            Msg.success(player, "This spawner now spawns " + args[0].toLowerCase(Locale.ROOT) + ".");
        } else {
            Msg.error(player, "That block is not a mob spawner.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (EntityType type : EntityType.values()) {
                if (type.isSpawnable() && type.getEntityClass() != null) {
                    names.add(type.name().toLowerCase(Locale.ROOT));
                }
            }
            return Targets.completeFrom(args[0], names);
        }
        return Collections.emptyList();
    }
}
