package io.sniperjohnny.github.better_admin_commands.util;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.player.PlayerPreferences;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class Targets {

    private Targets() {
    }

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

    public static String nameOf(Player player) {
        if (player == null) {
            return "";
        }
        String nickname = nicknameOf(player.getUniqueId());
        return nickname == null ? player.getName() : nickname;
    }

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

    public static Player onlineOrNull(String name) {
        if (name == null) {
            return null;
        }
        Player player = Bukkit.getPlayerExact(name);
        return player != null ? player : byNickname(name);
    }

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

    public static UUID uuidOf(String name) {
        Player online = onlineOrNull(name);
        if (online != null) {
            return online.getUniqueId();
        }
        OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(name);
        return player == null ? null : player.getUniqueId();
    }

    public static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(nameOf(player));
        }
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    public static List<String> complete(String prefix) {
        List<String> out = new ArrayList<>();
        for (String name : onlineNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                out.add(name);
            }
        }
        return out;
    }

    private static final int MAX_OFFLINE_SUGGESTIONS = 100;

    public static List<String> completeIncludingOffline(String prefix) {
        Set<String> names = new LinkedHashSet<>(onlineNames());
        Better_Admin_Commands plugin = Better_Admin_Commands.get_Instance();
        if (plugin != null && plugin.economy() != null) {
            for (UUID uuid : plugin.economy().accountIds()) {
                String name = plugin.economy().nameOf(uuid);
                if (name != null && !name.isBlank()) {
                    names.add(name);
                }
            }
        }
        String needle = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String name : names) {
            if (name.toLowerCase(Locale.ROOT).startsWith(needle)) {
                out.add(name);
            }
        }
        out.sort(String::compareToIgnoreCase);
        return out.size() <= MAX_OFFLINE_SUGGESTIONS
                ? out : out.subList(0, MAX_OFFLINE_SUGGESTIONS);
    }

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

    public static Double parseDouble(String input) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer parseInt(String input) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

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
