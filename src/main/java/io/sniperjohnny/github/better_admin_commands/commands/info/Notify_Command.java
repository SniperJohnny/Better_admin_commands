package io.sniperjohnny.github.better_admin_commands.commands.info;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.notify.NotificationService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Lets a player pick which notifications they want:
 *
 * <pre>
 *   /notify                 list them all with their state
 *   /notify &lt;name&gt;          flip one on or off
 *   /notify &lt;name&gt; on|off   set one outright
 *   /notify all on|off      set every one at once
 *   /notify reset           back to the server defaults
 * </pre>
 *
 * <p>A notification only ever arrives when the player also holds its permission,
 * so the toggles narrow things down rather than granting them.</p>
 */
public class Notify_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Notify_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        NotificationService notifications = plugin.notifications();
        if (notifications == null || notifications.categories().isEmpty()) {
            Msg.error(sender, "There are no notifications to configure.");
            return true;
        }
        if (args.length == 0) {
            list(player, label);
            return true;
        }

        String name = args[0].toLowerCase(Locale.ROOT);
        Boolean force = args.length >= 2 ? parse(args[1]) : null;
        if (args.length >= 2 && force == null) {
            Msg.error(sender, "Use on or off.");
            return true;
        }

        if (name.equals("all")) {
            if (force == null) {
                Msg.error(sender, "Use /" + label + " all on|off.");
                return true;
            }
            for (NotificationService.Category category : notifications.categories()) {
                notifications.toggle(player, category.id(), force);
            }
            Msg.success(sender, "Every notification is now " + (force ? "on" : "off") + ".");
            list(player, label);
            return true;
        }
        if (name.equals("reset")) {
            for (NotificationService.Category category : notifications.categories()) {
                notifications.reset(player, category.id());
            }
            Msg.success(sender, "Your notifications are back to the server defaults.");
            list(player, label);
            return true;
        }

        NotificationService.Category category = notifications.category(name);
        if (category == null) {
            Msg.error(sender, "There is no notification called '" + args[0] + "'.");
            return true;
        }
        boolean now = notifications.toggle(player, category.id(), force);
        Msg.success(sender, Msg.color(category.display()) + " &7is now "
                + (now ? "&aon" : "&coff") + "&7.");
        return true;
    }

    private Boolean parse(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "on", "true", "yes", "enable", "enabled" -> Boolean.TRUE;
            case "off", "false", "no", "disable", "disabled" -> Boolean.FALSE;
            default -> null;
        };
    }

    /** Prints every notification with its state and a clickable toggle. */
    private void list(Player player, String label) {
        NotificationService notifications = plugin.notifications();
        Msg.raw(player, "&6Your notifications &7(click to toggle)");
        for (NotificationService.Category category : notifications.categories()) {
            boolean wants = notifications.toggleEnabled(player, category);
            boolean allowed = category.permission().isBlank()
                    || plugin.permissions().has(player, category.permission());
            String state = !allowed ? "&8(no permission)"
                    : wants ? "&aon" : "&coff";
            player.sendMessage(Msg.component(" &8\u2022 ").append(
                    Msg.button(category.display() + " &7- " + state,
                            "/" + label + " " + category.id(),
                            "&7Click to turn this " + (wants ? "off" : "on") + ".")));
        }
        Msg.raw(player, " &7Change one with &f/" + label + " <name> [on|off]&7,");
        Msg.raw(player, " &7or all at once with &f/" + label + " all on|off&7.");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> choices = new ArrayList<>();
            if (plugin.notifications() != null) {
                for (NotificationService.Category category : plugin.notifications().categories()) {
                    choices.add(category.id());
                }
            }
            choices.add("all");
            choices.add("reset");
            return Targets.completeFrom(args[0].toLowerCase(Locale.ROOT), choices);
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("reset")) {
            return Targets.completeFrom(args[1].toLowerCase(Locale.ROOT), "on", "off");
        }
        return Collections.emptyList();
    }
}
