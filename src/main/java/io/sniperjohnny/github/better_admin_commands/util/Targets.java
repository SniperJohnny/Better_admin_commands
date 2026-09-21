package io.sniperjohnny.github.better_admin_commands.util;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Helpers for resolving player arguments and building tab completions.
 */
public final class Targets {

    private Targets() {
    }

    /** Resolves an online player, returning {@code null} and messaging the sender when unknown. */
    public static Player online(CommandSender sender, String name) {
        Player player = Bukkit.getPlayerExact(name);
        if (player == null) {
            Msg.unknownPlayer(sender, name);
        }
        return player;
    }

    /** Resolves an online player, or {@code null}, without messaging the sender. */
    public static Player onlineOrNull(String name) {
        return name == null ? null : Bukkit.getPlayerExact(name);
    }

    /** Resolves an offline player by name, returning {@code null} and messaging the sender when unknown. */
    public static OfflinePlayer offline(CommandSender sender, String name) {
        OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(name);
        if (player == null) {
            player = Bukkit.getOfflinePlayer(name);
        }
        if (player == null || (!player.hasPlayedBefore() && !player.isOnline())) {
            Msg.unknownOfflinePlayer(sender, name);
            return null;
        }
        return player;
    }

    /** Resolves a UUID from a name without messaging. Returns {@code null} when unknown. */
    public static UUID uuidOf(String name) {
        OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(name);
        if (player == null) {
            return null;
        }
        return player.getUniqueId();
    }

    public static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    /** Returns online player names matching the given prefix. */
    public static List<String> complete(String prefix) {
        List<String> out = new ArrayList<>();
        for (String name : onlineNames()) {
            if (name.toLowerCase().startsWith(prefix.toLowerCase())) {
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
