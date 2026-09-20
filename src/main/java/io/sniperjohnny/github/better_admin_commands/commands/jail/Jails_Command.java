package io.sniperjohnny.github.better_admin_commands.commands.jail;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Lists every jail cell. */
public class Jails_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Jails_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (plugin.jails().size() == 0) {
            Msg.error(sender, "There are no jails yet. Create one with /setjail <name>.");
            return true;
        }
        Msg.raw(sender, "&6Jails &7(" + plugin.jails().size() + ")&6:");
        for (String name : plugin.jails().names()) {
            Location location = plugin.jails().get(name);
            Msg.raw(sender, String.format(Locale.ROOT, " &8- &f%s &7(%s %.0f, %.0f, %.0f)",
                    name, location.getWorld().getName(),
                    location.getX(), location.getY(), location.getZ()));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
