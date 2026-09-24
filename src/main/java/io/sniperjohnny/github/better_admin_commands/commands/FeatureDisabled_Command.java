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
 * Answer of a command whose feature module is switched off in {@code config.yml}.
 *
 * <p>One instance serves every switched-off command: which one was called is read
 * from the {@link Command} the server hands in, so the message always names the
 * module and the option that has to be turned back on.</p>
 */
public class FeatureDisabled_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public FeatureDisabled_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        Msg.error(sender, plugin.features().blockReason(command.getName()));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
