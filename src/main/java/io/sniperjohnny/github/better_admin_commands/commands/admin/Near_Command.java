package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Lists all players in a certain radius. */
public class Near_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player self)) {
            Msg.playerOnly(sender);
            return true;
        }
        double radius = 200;
        if (args.length >= 1) {
            var parsed = io.sniperjohnny.github.better_admin_commands.util.Targets.parseDouble(args[0]);
            if (parsed == null) {
                Msg.error(self, "The radius has to be a number.");
                return true;
            }
            radius = Math.max(1, parsed);
        }

        List<Player> nearby = new ArrayList<>();
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(self) || !other.getWorld().equals(self.getWorld())) {
                continue;
            }
            if (other.getLocation().distance(self.getLocation()) <= radius) {
                nearby.add(other);
            }
        }
        if (nearby.isEmpty()) {
            Msg.send(self, "&7There are no players within " + (int) radius + " blocks.");
            return true;
        }
        nearby.sort(Comparator.comparingDouble(player -> player.getLocation().distance(self.getLocation())));
        Msg.raw(self, "&6Players within " + (int) radius + " blocks:");
        for (Player other : nearby) {
            Msg.raw(self, String.format(Locale.ROOT, " &8- &f%s &7(%.0f blocks)",
                    other.getName(), other.getLocation().distance(self.getLocation())));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> options = List.of("50", "100", "200", "500");
            List<String> result = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], options, result);
            return result;
        }
        return Collections.emptyList();
    }
}
