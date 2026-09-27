package io.sniperjohnny.github.better_admin_commands.util;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.player.PlayerPreferences;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Helpers for resolving player arguments and building tab completions.
 *
 * <p>Every command that takes a player name goes through here, so a nickname set
 * with {@code /nick} works anywhere a real name does - {@code /msg nick hello}
 * or {@code /gm 1 nick}. The real name of a nicked player is still accepted when
 * it is typed out, but it is hidden from tab completion, which only offers the
 * nickname. The nickname of an offline player is not known (nicknames are only
 * read while a player is online), so offline lookups still use the real name.</p>
 */
public final class Targets {

    private Targets() {
    }

    /* ------------------------------------------------------ nickname ------------------------------ */

    /** The colour-stripped nickname of a player, or {@code null} when none is set. */
    private static String nicknameOf(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        Better_Admin_Commands plugin = Better_Admin_Commands.get_Instance();
        if (plugin == null || plugin.preferences() == null) {
            return null;
        }
        return plugin.preferences().plainNickname(uuid);
    }

    /**
     * Resolves an online player by nickname, ignoring case and colour codes.
     * Returns {@code null} when no online player wears that nickname.
     */
    public static Player byNickname(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String needle = PlayerPreferences.stripColor(name).toLowerCase(Locale.ROOT);
        for (Player player : Bukkit.getOnlinePlayers()) {
            String nickname = nicknameOf(player.getUniqueId());
            if (nickname != null
                    && PlayerPreferences.stripColor(nickname).toLowerCase(Locale.ROOT).equals(needle)) {
                return player;
            }
        }
        return null;
    }

    /**
     * The name a player is known by in command arguments: the nickname when one
     * is set, otherwise the real name.
     */
    public static String nameOf(Player player) {
        if (player == null) {
            return "";
        }
        String nickname = nicknameOf(player.getUniqueId());
        return nickname == null ? player.getName() : nickname;
    }

    /**
     * The full display name of a player - the rank prefix plus the nickname, or
     * the real name when no nickname is set. Used in command output, so a nicked
     * player is never referred to by their real name.
     */
    public static String displayName(Player player) {
        if (player == null) {
            return "";
        }
        Better_Admin_Commands plugin = Better_Admin_Commands.get_Instance();
        if (plugin == null || plugin.preferences() == null) {
            return player.getName();
        }
        return plugin.preferences().displayName(player.getUniqueId(), player.getName());
    }

    /**
     * The same for a player who may be offline: the nickname while they are
     * online, otherwise the real name (an offline nickname is not known).
     */
    public static String displayName(OfflinePlayer player) {
        if (player == null) {
            return "";
        }
        Player online = player.getPlayer();
        if (online != null) {
            return displayName(online);
        }
        return player.getName() == null ? "" : player.getName();
    }

    /* -------------------------------------------------------- resolving --------------------------- */

    /**
     * Resolves an online player by real name or nickname, returning {@code null}
     * and messaging the sender when neither matches.
     */
    public static Player online(CommandSender sender, String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            player = byNickname(name);
        }
        if (player == null) {
            Msg.unknownPlayer(sender, name);
        }
        return player;
    }

    /**
     * Resolves an online player by real name or nickname, or {@code null},
     * without messaging the sender.
     */
    public static Player onlineOrNull(String name) {
        if (name == null) {
            return null;
        }
        Player player = Bukkit.getPlayerExact(name);
        return player != null ? player : byNickname(name);
    }

    /**
     * Resolves an offline player by name, returning {@code null} and messaging
     * the sender when unknown. A nickname is resolved as a last resort, so a
     * nicked player who is online can be named by their nickname even on a
     * command that normally takes an offline player.
     */
    public static OfflinePlayer offline(CommandSender sender, String name) {
        OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(name);
        if (player == null) {
            player = Bukkit.getOfflinePlayer(name);
        }
        if (player == null || (!player.hasPlayedBefore() && !player.isOnline())) {
            Player nicked = byNickname(name);
            if (nicked != null) {
                return nicked;
            }
            Msg.unknownOfflinePlayer(sender, name);
            return null;
        }
        return player;
    }

    /** Resolves a UUID from a name (real name or nickname) without messaging. */
    public static UUID uuidOf(String name) {
        Player online = onlineOrNull(name);
        if (online != null) {
            return online.getUniqueId();
        }
        OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(name);
        return player == null ? null : player.getUniqueId();
    }

    /* ------------------------------------------------------- completion --------------------------- */

    /**
     * The names offered by tab completion: the nickname for a nicked player, the
     * real name for everyone else. Real names of nicked players are hidden.
     */
    public static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(nameOf(player));
        }
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    /** Returns online player names (or nicknames) matching the given prefix. */
    public static List<String> complete(String prefix) {
        List<String> out = new ArrayList<>();
        for (String name : onlineNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                out.add(name);
            }
        }
        return out;
    }

    /** Returns the given suggestions matching the given prefix. */
    public static List<String> completeFrom(String prefix, String... suggestions) {
        List<String> out = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().startsWith(prefix.toLowerCase())) {
                out.add(suggestion);
            }
        }
        return out;
    }

    public static List<String> completeFrom(String prefix, Iterable<String> suggestions) {
        List<String> out = new ArrayList<>();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().startsWith(prefix.toLowerCase())) {
                out.add(suggestion);
            }
        }
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    /** Parses a double, returning {@code null} when the input is not a number. */
    public static Double parseDouble(String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parses an integer, returning {@code null} when the input is not a number. */
    public static Integer parseInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses a duration like {@code 30s}, {@code 10m}, {@code 2h}, {@code 7d} or
     * {@code perm} into seconds. Returns {@code -1} for permanent and
     * {@code null} when the format is not recognised.
     */
    public static Long parseDuration(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String lower = input.toLowerCase();
        if (lower.equals("perm") || lower.equals("permanent") || lower.equals("forever")) {
            return -1L;
        }
        char unitChar = lower.charAt(lower.length() - 1);
        long multiplier;
        String number = lower;
        if (!Character.isDigit(unitChar)) {
            number = lower.substring(0, lower.length() - 1);
            multiplier = switch (unitChar) {
                case 's' -> 1L;
                case 'm' -> 60L;
                case 'h' -> 3600L;
                case 'd' -> 86400L;
                case 'w' -> 604800L;
                default -> -1L;
            };
            if (multiplier < 0) {
                return null;
            }
        } else {
            multiplier = 1L;
        }
        try {
            return Long.parseLong(number) * multiplier;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Formats a duration in seconds as a short human readable string. */
    public static String formatDuration(long seconds) {
        if (seconds < 0) {
            return "permanent";
        }
        if (seconds < 60) {
            return seconds + "s";
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m");
        }
        return sb.toString().trim();
    }
}
