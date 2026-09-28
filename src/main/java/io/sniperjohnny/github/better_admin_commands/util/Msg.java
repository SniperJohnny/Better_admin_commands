package io.sniperjohnny.github.better_admin_commands.util;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

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

    public static net.kyori.adventure.text.Component component(String message) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(color(message));
    }

    public static net.kyori.adventure.text.Component prefixed(net.kyori.adventure.text.Component message) {
        return component(prefix).append(message);
    }

    public static void send(CommandSender to, String message) {
        to.sendMessage(color(prefix + message));
    }

    public static net.kyori.adventure.text.Component button(String label, String command, String hover) {
        return component(label)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(command))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(component(hover)));
    }

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
