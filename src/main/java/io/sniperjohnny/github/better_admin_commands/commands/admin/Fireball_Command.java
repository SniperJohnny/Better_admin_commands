package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Launches a fireball in the direction you are looking. */
public class Fireball_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        boolean small = args.length >= 1 && args[0].equalsIgnoreCase("small");
        float power = 1f;
        if (args.length >= 2) {
            Double parsed = Targets.parseDouble(args[1]);
            if (parsed != null) {
                power = Math.max(1f, Math.min(10f, parsed.floatValue()));
            }
        } else if (args.length == 1 && !small) {
            Double parsed = Targets.parseDouble(args[0]);
            if (parsed != null) {
                power = Math.max(1f, Math.min(10f, parsed.floatValue()));
            }
        }

        if (small) {
            player.launchProjectile(SmallFireball.class).setYield(power);
        } else {
            LargeFireball fireball = player.launchProjectile(LargeFireball.class);
            fireball.setYield(power);
            fireball.setIsIncendiary(false);
        }
        Msg.success(player, "Fireball launched.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0].toLowerCase(Locale.ROOT), "small", "1", "2", "3");
        }
        if (args.length == 2) {
            return Targets.completeFrom(args[1], "1", "2", "3");
        }
        return Collections.emptyList();
    }
}
