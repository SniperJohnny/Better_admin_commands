package io.sniperjohnny.github.better_admin_commands.commands;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Answer that every command hands out while the plugin is switched off with
 * {@code /betteradmincommands disable}. Only the management command itself
 * keeps working, so the plugin can be switched back on.
 */
public class Disabled_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Disabled_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        Msg.error(sender, "The plugin is currently disabled. Use /" + plugin.rootLabel()
                + " enable to switch it back on.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
