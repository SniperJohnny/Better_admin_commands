package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
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
 * Changes the name of a player, stored in the database. The nickname is used in
 * the tab list, above the player's head and in chat.
 *
 * <p>The optional group argument borrows the prefix of a LuckPerms group, so a
 * nickname can be shown with a rank tag. No permission or group of the player is
 * ever changed.</p>
 *
 * <ul>
 *   <li>{@code /nick <nickname> [group]} - your own nickname</li>
 *   <li>{@code /nick <player> <nickname> [group]} - someone else, needs
 *       {@code betteradmincommands.nick.others}</li>
 *   <li>{@code /nick <nickname> off} keeps the nickname but drops the prefix</li>
 * </ul>
 */
public class Nick_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Nick_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }

        // Decide who is renamed and which group prefix is borrowed. Someone who
        // may nickname others gets the two argument form when the first argument
        // names an online player, and the own-nickname form otherwise.
        Player target;
        String nickname;
        String group;
        Player named = sender.hasPermission("betteradmincommands.nick.others")
                ? Targets.onlineOrNull(args[0]) : null;
        if (named != null && args.length >= 2) {
            target = named;
            nickname = args[1];
            group = args.length >= 3 ? args[2] : null;
        } else {
            if (!(sender instanceof Player self)) {
                Msg.playerOnly(sender);
                return true;
            }
            target = self;
            nickname = args[0];
            group = args.length >= 2 ? args[1] : null;
        }

        if (isReset(nickname)) {
            plugin.preferences().setNickname(target, null, null);
            Msg.success(sender, target.equals(sender)
                    ? "Your nickname was removed."
                    : "Removed the nickname of " + target.getName() + ".");
            return true;
        }
        if (nickname.length() > 32) {
            Msg.error(sender, "Nicknames can be at most 32 characters long.");
            return true;
        }
        if (!mayUse(sender, nickname, group)) {
            return true;
        }
        if (nickname.contains("&") && !sender.hasPermission("betteradmincommands.nick.color")) {
            nickname = nickname.replace("&", "");
        }

        if (isReset(group)) {
            group = null;
        }
        if (group != null && (!plugin.nicks().available() || !plugin.nicks().hasGroup(group))) {
            // Still rename the player - a prefix that cannot be resolved is no
            // reason to leave the nickname unset.
            Msg.send(sender, plugin.nicks().available()
                    ? "&7There is no LuckPerms group called &f" + group
                            + "&7 - setting the nickname without a prefix."
                    : "&7LuckPerms is not installed - setting the nickname without a prefix.");
            group = null;
        }

        plugin.preferences().setNickname(target, nickname, group);
        plugin.getLogger().info("Nickname of " + target.getName() + " is now '" + nickname + "'"
                + (group == null ? "" : " with the " + group + " prefix")
                + " - applied to the display name and the tab list.");
        String shown = group == null ? nickname : plugin.nicks().prefix(group) + nickname;
        Msg.success(sender, target.equals(sender)
                ? "Your nickname is now " + shown + "&r."
                : target.getName() + " is now called " + shown + "&r.");
        return true;
    }

    private static boolean isReset(String value) {
        return value != null && (value.equalsIgnoreCase("off") || value.equalsIgnoreCase("reset")
                || value.equalsIgnoreCase("clear"));
    }

    /**
     * The nicks and groups listed under {@code nick.restricted} are reserved for
     * server operators, so a player cannot dress up as staff.
     */
    private boolean mayUse(CommandSender sender, String nickname, String group) {
        if (!(sender instanceof Player) || sender.isOp()) {
            return true; // console counts as an operator
        }
        for (String reserved : plugin.getConfig().getStringList("nick.restricted")) {
            if (plain(reserved).equalsIgnoreCase(plain(nickname))
                    || (group != null && plain(reserved).equalsIgnoreCase(plain(group)))) {
                Msg.error(sender, "The nick and group " + reserved + " are reserved for server operators.");
                return false;
            }
        }
        return true;
    }

    /** Strips colour codes, so "&4dev" cannot slip past the reserved list. */
    private static String plain(String value) {
        return value == null ? "" : value.replaceAll("(?i)&[0-9a-fk-or]", "").trim();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        boolean others = sender.hasPermission("betteradmincommands.nick.others");
        if (args.length == 1) {
            if (!others) {
                return Targets.completeFrom(args[0].toLowerCase(Locale.ROOT), "off");
            }
            List<String> names = new ArrayList<>(Targets.complete(args[0]));
            names.addAll(Targets.completeFrom(args[0].toLowerCase(Locale.ROOT), "off"));
            return names;
        }
        if (args.length == 2) {
            // Someone else's nickname, or the group for your own.
            if (others && Targets.onlineOrNull(args[0]) != null) {
                return Targets.completeFrom(args[1].toLowerCase(Locale.ROOT), "off");
            }
            return Targets.completeFrom(args[1].toLowerCase(Locale.ROOT), groupChoices());
        }
        if (args.length == 3) {
            return Targets.completeFrom(args[2].toLowerCase(Locale.ROOT), groupChoices());
        }
        return Collections.emptyList();
    }

    /** Every LuckPerms group plus the words that clear the prefix. */
    private List<String> groupChoices() {
        List<String> choices = new ArrayList<>(plugin.nicks().groupNames());
        choices.add("off");
        return choices;
    }
}
