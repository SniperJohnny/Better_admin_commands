package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class Admin_Audit {

    public static final String CATEGORY = "admin";

    public static final String PERMISSION = "betteradmincommands.admin.notify";

    private Admin_Audit() {
    }

    public static TabExecutor wrap(TabExecutor delegate) {
        return new TabExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                boolean handled = delegate.onCommand(sender, command, label, args);
                report(sender, label, args);
                return handled;
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, Command command,
                                              String label, String[] args) {
                List<String> completions = delegate.onTabComplete(sender, command, label, args);
                return completions == null ? Collections.emptyList() : completions;
            }
        };
    }

    public static CommandExecutor wrap(CommandExecutor delegate) {
        return (sender, command, label, args) -> {
            boolean handled = delegate.onCommand(sender, command, label, args);
            report(sender, label, args);
            return handled;
        };
    }

    private static void report(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return;
        }
        Better_Admin_Commands plugin = Better_Admin_Commands.get_Instance();
        if (plugin == null || plugin.notifications() == null) {
            return;
        }
        StringBuilder message = new StringBuilder("&7")
                .append(Targets.displayName(player))
                .append(" &fused &7/")
                .append(label == null ? "" : label.toLowerCase(Locale.ROOT));
        if (args != null && args.length > 0) {
            message.append(' ').append(String.join(" ", args));
        }
        message.append("&7.");
        plugin.notifications().broadcast(CATEGORY, PERMISSION, message.toString(), player);
    }
}
