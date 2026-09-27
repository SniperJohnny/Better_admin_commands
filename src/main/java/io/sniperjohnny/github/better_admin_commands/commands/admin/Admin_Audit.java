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

/**
 * Reports the use of an administration command to the staff.
 *
 * <p>Every command of the {@code admin} module is wrapped in here, so a
 * {@code /gm}, {@code /enchant}, {@code /give} and the rest announce who used
 * them and with which arguments. The report goes through the {@code /notify}
 * system under the {@code admin} category, so a staff member can switch it off
 * with {@code /notify admin off} and only whoever holds
 * {@link #PERMISSION} receives it. Only players are reported; the console is
 * left out, as there is no name to show and the action is already in the log.</p>
 */
public final class Admin_Audit {

    /** The {@code /notify} category that carries the administration reports. */
    public static final String CATEGORY = "admin";
    /** The node that decides who receives the reports. */
    public static final String PERMISSION = "betteradmincommands.admin.notify";

    private Admin_Audit() {
    }

    /** Wraps a tab-completing administration command. */
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

    /** Wraps an administration command that does not complete arguments. */
    public static CommandExecutor wrap(CommandExecutor delegate) {
        return (sender, command, label, args) -> {
            boolean handled = delegate.onCommand(sender, command, label, args);
            report(sender, label, args);
            return handled;
        };
    }

    /** Sends the report for one command use to every subscribed staff member. */
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
