package io.sniperjohnny.github.better_admin_commands.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class Root_Command extends Command {

    private final Plugin_Command delegate;

    public Root_Command(String name, List<String> aliases, Plugin_Command delegate) {
        super(name, "manages the plugin",
                "/" + name + " <reload|backup|reconnect|database|modules|info|disable|enable>",
                aliases == null ? Collections.emptyList() : aliases);
        this.delegate = delegate;
        setPermission("betteradmincommands.command");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel,
                           @NotNull String @NotNull[] args) {
        return delegate.onCommand(sender, this, commandLabel, args);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias,
                                             @NotNull String @NotNull[] args) {
        List<String> suggestions = delegate.onTabComplete(sender, this, alias, args);
        return suggestions == null ? Collections.emptyList() : suggestions;
    }
}
