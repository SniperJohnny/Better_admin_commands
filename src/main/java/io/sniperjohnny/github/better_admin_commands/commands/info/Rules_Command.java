package io.sniperjohnny.github.better_admin_commands.commands.info;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Shows the server rules from config.yml. */
public class Rules_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Rules_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        List<String> rules = plugin.getConfig().getStringList("messages.rules");
        if (rules.isEmpty()) {
            Msg.send(sender, "&7No rules have been configured.");
            return true;
        }
        Msg.raw(sender, "&6Server rules:");
        int number = 1;
        for (String rule : rules) {
            Msg.raw(sender, " &7" + (number++) + ". &f" + rule);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
