package io.sniperjohnny.github.better_admin_commands.commands.social;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.mail.MailService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Offline messaging: {@code /mail send <player> <message>}, {@code /mail read}
 * and {@code /mail clear}.
 */
public class Mail_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Mail_Command(Better_Admin_Commands plugin) {
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
            int unread = plugin.mail().unreadCount(player.getUniqueId());
            Msg.send(player, "&7You have &f" + unread + " &7unread message(s).");
            Msg.send(player, "&7Use &f/mail read&7, &f/mail send <player> <message> &7or &f/mail clear&7.");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "send" -> {
                if (args.length < 3) {
                    Msg.error(player, "Usage: /mail send <player> <message>");
                    return true;
                }
                OfflinePlayer target = Targets.offline(player, args[1]);
                if (target == null) {
                    return true;
                }
                String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                plugin.mail().send(player, target.getUniqueId(), message);
                Msg.success(player, "Mail sent to " + target.getName() + ".");
                Player online = target.getPlayer();
                if (online != null) {
                    Msg.send(online, "&7You received new mail from &f" + player.getName() + "&7. Use &f/mail read&7.");
                }
            }
            case "read" -> showInbox(player);
            case "clear", "delete" -> {
                plugin.mail().clear(player.getUniqueId());
                Msg.success(player, "Your mailbox was cleared.");
            }
            default -> Msg.error(player, "Usage: /mail <send|read|clear>");
        }
        return true;
    }

    private void showInbox(Player player) {
        List<MailService.Mail> messages;
        try {
            messages = plugin.mail().inbox(player.getUniqueId(), false);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not read mail: " + e.getMessage());
            Msg.error(player, "Your mailbox could not be read, please tell an administrator.");
            return;
        }
        if (messages.isEmpty()) {
            Msg.send(player, "&7Your mailbox is empty.");
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Msg.raw(player, "&6Your mail &7(" + messages.size() + ")&6:");
        for (MailService.Mail mail : messages) {
            Msg.raw(player, " &7[" + format.format(new Date(mail.sentAt())) + "] &f" + mail.senderName()
                    + "&7: &f" + mail.message());
        }
        plugin.mail().markAllRead(player.getUniqueId());
        Msg.send(player, "&7All messages were marked as read.");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "send", "read", "clear");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("send")) {
            return Targets.onlineNames();
        }
        return Collections.emptyList();
    }
}
