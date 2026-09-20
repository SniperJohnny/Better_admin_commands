package io.sniperjohnny.github.better_admin_commands.commands.info;

import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Shows runtime and memory information. */
public class Gc_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory() / 1024L / 1024L;
        long total = runtime.totalMemory() / 1024L / 1024L;
        long free = runtime.freeMemory() / 1024L / 1024L;
        long used = total - free;

        Msg.raw(sender, "&6Server:");
        Msg.raw(sender, " &7Online: &f" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        Msg.raw(sender, " &7Worlds: &f" + Bukkit.getWorlds().size());
        for (World world : Bukkit.getWorlds()) {
            Msg.raw(sender, "  &8- &f" + world.getName() + " &7(" + world.getLoadedChunks().length
                    + " chunks, " + world.getEntities().size() + " entities)");
        }
        Msg.raw(sender, "&6Memory:");
        Msg.raw(sender, " &7Used: &f" + used + "MB &7/ Total: &f" + total + "MB &7/ Max: &f" + max + "MB");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
