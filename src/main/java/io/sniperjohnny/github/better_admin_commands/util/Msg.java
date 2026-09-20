package io.sniperjohnny.github.better_admin_commands.util;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Tiny messaging helper. All strings may use legacy '&amp;' colour codes.
 */
public final class Msg {

    private static String prefix = "&8[&6BetterAdmin&8] &r";

    private Msg() {
    }

    public static void setPrefix(String value) {
        prefix = value == null ? "" : value;
    }

    public static String color(String message) {
        return message == null ? "" : ChatColor.translateAlternateColorCodes('&', message);
    }

    /** Converts a legacy coloured string into an Adventure component. */
    public static net.kyori.adventure.text.Component component(String message) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(color(message));
    }

    /** Sends a message prefixed with the configured plugin prefix. */
    public static void send(CommandSender to, String message) {
        to.sendMessage(color(prefix + message));
    }

    /** Sends a message without the plugin prefix. */
    public static void raw(CommandSender to, String message) {
        to.sendMessage(color(message));
    }

    public static void noPermission(CommandSender to) {
        send(to, "&cYou do not have permission to do that.");
    }

    public static void playerOnly(CommandSender to) {
        send(to, "&cYou have to be a player to use this command.");
    }

    public static void unknownPlayer(CommandSender to, String name) {
        send(to, "&cThe player &f" + name + " &cis not online.");
    }

    public static void unknownOfflinePlayer(CommandSender to, String name) {
        send(to, "&cNo player called &f" + name + " &chas ever played here.");
    }

    public static void usage(CommandSender to, Command command) {
        send(to, "&7Usage: &f" + command.getUsage().replace("<command>", command.getName()));
    }

    public static void error(CommandSender to, String message) {
        send(to, "&c" + message);
    }

    public static void success(CommandSender to, String message) {
        send(to, "&a" + message);
    }
}
