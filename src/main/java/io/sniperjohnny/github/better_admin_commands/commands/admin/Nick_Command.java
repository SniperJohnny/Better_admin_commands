package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.player.PlayerPreferences;
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
 * Changes the nickname of the player running the command, stored in the
 * database. The nickname is used in the tab list, above the player's head and in
 * chat.
 *
 * <p>The optional group argument borrows the prefix of a LuckPerms group, so a
 * nickname can be shown with a rank tag. No permission or group of the player is
 * ever changed.</p>
 *
 * <ul>
 *   <li>{@code /nick <nickname> [group]} - your own nickname</li>
 *   <li>{@code /nick <nickname> off} keeps the nickname but drops the prefix</li>
 *   <li>{@code /nick off} - removes the nickname entirely</li>
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
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }

        String nickname = args[0];
        String group = args.length >= 2 ? args[1] : null;

        if (isReset(nickname)) {
            plugin.preferences().setNickname(player, null, null);
            Msg.success(sender, "Your nickname was removed.");
            return true;
        }
        if (nickname.length() > 32) {
            Msg.error(sender, "Nicknames can be at most 32 characters long.");
            return true;
        }
        if (!mayUse(sender, nickname, group)) {
            return true;
        }
        if (nickname.contains("&") && !plugin.permissions().has(sender, "betteradmincommands.nick.color")) {
            nickname = nickname.replace("&", "");
        }

        // "off" for the group means "no prefix"; no group at all means "use my
        // own rank", which is what most people expect from /nick.
        if (isReset(group)) {
            group = PlayerPreferences.PREFIX_NONE;
        } else if (group == null && !plugin.getConfig().getBoolean("nick.show-rank-prefix", true)) {
            // No group named and rank prefixes are switched off: show the
            // nickname bare instead of borrowing the player's own rank.
            group = PlayerPreferences.PREFIX_NONE;
        }
        if (group != null && !isNoPrefix(group)
                && (!plugin.nicks().available() || !plugin.nicks().hasGroup(group))) {
            // Still rename the player - a prefix that cannot be resolved is no
            // reason to leave the nickname unset.
            Msg.send(sender, plugin.nicks().available()
                    ? "&7There is no LuckPerms group called &f" + group
                            + "&7 - using your own rank prefix instead."
                    : "&7LuckPerms is not installed - setting the nickname without a prefix.");
            group = plugin.nicks().available() ? null : PlayerPreferences.PREFIX_NONE;
        }

        plugin.preferences().setNickname(player, nickname, group);
        plugin.getLogger().info("Nickname of " + player.getName() + " is now '" + nickname + "'"
                + describePrefix(group) + " - applied to the tab list, the name tag and chat.");
        String prefix = plugin.preferences().nicknamePrefix(player.getUniqueId());
        Msg.success(sender, "Your nickname is now " + prefix + nickname + "&r.");
        if (plugin.nicks().available() && prefix.isEmpty() && !isNoPrefix(group)) {
            Msg.send(sender, "&7Your rank has no prefix set, or you have no rank yet.");
        }
        return true;
    }

    private static boolean isNoPrefix(String group) {
        return PlayerPreferences.PREFIX_NONE.equals(group);
    }

    private static String describePrefix(String group) {
        if (group == null) {
            return " with the player's own rank prefix";
        }
        return isNoPrefix(group) ? " without a prefix" : " with the " + group + " prefix";
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
        if (args.length == 1) {
            return Targets.completeFrom(args[0].toLowerCase(Locale.ROOT), "off");
        }
        if (args.length == 2) {
            return Targets.completeFrom(args[1].toLowerCase(Locale.ROOT), groupChoices());
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
