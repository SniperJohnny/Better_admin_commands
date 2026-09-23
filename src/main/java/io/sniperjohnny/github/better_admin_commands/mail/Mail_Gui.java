package io.sniperjohnny.github.better_admin_commands.mail;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.gui.Items;
import io.sniperjohnny.github.better_admin_commands.gui.Menu;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The mailbox behind {@code /mail}: the inbox newest first, one head per sender.
 * Opening a message marks it read; from there it can be answered or deleted.
 * Composing picks the receiver from the online players, or by typing a name, so
 * offline players can still be written to.
 *
 * <p>The mailbox is read <strong>off the main thread</strong> and the window is
 * drawn afterwards, so a slow database delays the window instead of freezing the
 * server. A redraw after a write waits for that write to finish, so a deleted or
 * newly sent message never shows up stale.</p>
 */
public class Mail_Gui {

    private static final SimpleDateFormat STAMP = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final Better_Admin_Commands plugin;

    public Mail_Gui(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    /* ----------------------------------------------------------- loading -- */

    /** Opens the mailbox, reading it in the background first. */
    public void open(Player player, int page) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<MailService.Mail> box;
            try {
                box = new ArrayList<>(plugin.mail().inbox(player.getUniqueId(), false));
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not read mail: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> Msg.error(player, "Your mailbox could not be read, please tell an administrator."));
                return;
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // They may have left while the read was running.
                if (!player.isOnline()) {
                    return;
                }
                render(player, box, page);
            });
        });
    }

    /** Redraws once an async write has finished, back on the server thread. */
    private void redrawAfter(CompletableFuture<Void> write, Player player, int page) {
        write.whenComplete((ignored, failure) ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        open(player, page);
                    }
                }));
    }

    /* ------------------------------------------------------------- inbox -- */

    private void render(Player player, List<MailService.Mail> mailbox, int page) {
        // Newest first is what people expect from a mailbox.
        List<MailService.Mail> box = new ArrayList<>(mailbox);
        Collections.reverse(box);
        int unread = 0;
        for (MailService.Mail mail : box) {
            if (!mail.read()) {
                unread++;
            }
        }

        int rows = Math.max(3, Math.min(6, plugin.getConfig().getInt("mail.gui-rows", 6)));
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(box.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Mailbox &7(" + unread + " new)", rows);
        menu.frame();

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < box.size(); index++) {
            MailService.Mail mail = box.get(start + index);
            List<String> lore = new ArrayList<>();
            if (!mail.read()) {
                lore.add("&cNew message");
            }
            lore.add("&7From: &f" + mail.senderName());
            lore.add("&7When: &f" + STAMP.format(new Date(mail.sentAt())));
            lore.add("");
            for (String line : wrap(mail.message())) {
                lore.add("&7" + line);
            }
            lore.add("");
            lore.add("&eClick to open");
            menu.button(index, Items.head(mail.senderUuid(),
                    (mail.read() ? "&f" : "&6") + mail.senderName(), lore),
                    event -> openMessage(player, mail, current));
        }
        if (box.isEmpty()) {
            menu.button((rows - 1) / 2 * 9 + 4, Items.of(Material.BOOK, "&7Your mailbox is empty",
                    "&7Use the button below to write to someone."));
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> open(player, current - 1));
        }
        menu.button(nav + 1, Items.of(Material.WRITABLE_BOOK, "&aWrite a message",
                        "&7Pick a player and type your message.",
                        "",
                        "&eClick to write"),
                event -> openCompose(player, 0));
        if (!box.isEmpty()) {
            menu.button(nav + 2, Items.of(Material.LIME_DYE, "&aMark all as read",
                            "&7Everything will show as read.",
                            "",
                            "&eClick to mark"),
                    event -> {
                        redrawAfter(plugin.mail().markAllRead(player.getUniqueId()), player, current);
                        Msg.success(player, "All messages were marked as read.");
                    });
        }
        menu.button(nav + 4, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages,
                "&7Messages: &f" + box.size(),
                "&7Unread: &f" + unread));
        if (!box.isEmpty()) {
            menu.button(nav + 6, Items.of(Material.LAVA_BUCKET, "&cClear mailbox",
                            "&7Deletes every message.",
                            "",
                            "&eClick to confirm"),
                    event -> openClear(player, current));
        }
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> open(player, current + 1));
        }
        menu.open(player);
    }

    /* -------------------------------------------------------- one message - */

    private void openMessage(Player player, MailService.Mail mail, int returnPage) {
        // Opening a message is what marks it read, so the list keeps showing which
        // ones are new until they are actually looked at.
        if (!mail.read()) {
            plugin.mail().markRead(player.getUniqueId(), mail);
        }

        Menu menu = new Menu("&8Mail from " + mail.senderName(), 3);
        menu.frame();

        List<String> lore = new ArrayList<>();
        lore.add("&7From: &f" + mail.senderName());
        lore.add("&7When: &f" + STAMP.format(new Date(mail.sentAt())));
        lore.add("");
        for (String line : wrap(mail.message())) {
            lore.add("&f" + line);
        }
        menu.button(13, Items.of(Material.PAPER, "&6Message", lore));

        Player onlineSender = plugin.getServer().getPlayer(mail.senderUuid());
        menu.button(11, Items.of(Material.WRITABLE_BOOK, "&aReply",
                        "&7Write back to &f" + mail.senderName() + "&7.",
                        "",
                        "&eClick to reply"),
                event -> plugin.chatPrompts().request(player,
                        "&7Your reply to &f" + mail.senderName() + "&7:", answer -> {
                            if (answer.equalsIgnoreCase("cancel")) {
                                Msg.send(player, "&7Reply cancelled.");
                                open(player, returnPage);
                                return;
                            }
                            // The sender may well be offline; mail is exactly for that.
                            redrawAfter(plugin.mail().send(player, mail.senderUuid(), answer), player, returnPage);
                            Msg.success(player, "Reply sent to " + mail.senderName() + ".");
                            if (onlineSender != null) {
                                Msg.send(onlineSender, "&7You received new mail from &f"
                                        + player.getName() + "&7. Use &f/mail&7.");
                            }
                        }));

        menu.button(15, Items.of(Material.BARRIER, "&cDelete",
                        "&7Removes this message for good.",
                        "",
                        "&eClick to delete"),
                event -> {
                    redrawAfter(plugin.mail().delete(player.getUniqueId(), mail), player, returnPage);
                    Msg.success(player, "Message deleted.");
                });
        menu.button(22, Items.of(Material.ARROW, "&7Back to the mailbox"), event -> open(player, returnPage));
        menu.open(player);
    }

    private void openClear(Player player, int returnPage) {
        Menu menu = new Menu("&8Clear mailbox", 3);
        menu.frame();
        menu.button(13, Items.of(Material.LAVA_BUCKET, "&cDelete every message",
                "&7This cannot be undone."));
        menu.button(11, Items.of(Material.LIME_CONCRETE, "&aKeep them"), event -> open(player, returnPage));
        menu.button(15, Items.of(Material.RED_CONCRETE, "&cDelete everything"), event -> {
            redrawAfter(plugin.mail().clear(player.getUniqueId()), player, 0);
            Msg.success(player, "Your mailbox was cleared.");
        });
        menu.open(player);
    }

    /* ---------------------------------------------------------- compose --- */

    private void openCompose(Player player, int page) {
        List<Player> receivers = new ArrayList<>();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.getUniqueId().equals(player.getUniqueId())) {
                receivers.add(online);
            }
        }
        receivers.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));

        int rows = 5;
        int pageSize = (rows - 1) * 9;
        int pages = Math.max(1, (int) Math.ceil(receivers.size() / (double) pageSize));
        int current = Math.max(0, Math.min(page, pages - 1));

        Menu menu = new Menu("&8Write to...", rows);
        menu.frame();

        int start = current * pageSize;
        for (int index = 0; index < pageSize && start + index < receivers.size(); index++) {
            Player receiver = receivers.get(start + index);
            menu.button(index, Items.head(receiver.getUniqueId(), "&f" + receiver.getName(),
                            "&7Write a message to them.",
                            "",
                            "&eClick to write"),
                    event -> promptMessage(player, receiver.getUniqueId(), receiver.getName()));
        }
        if (receivers.isEmpty()) {
            menu.button((rows - 1) / 2 * 9 + 4, Items.of(Material.GRAY_DYE, "&7Nobody else is online",
                    "&7Use the button below to write to someone by name."));
        }

        int nav = (rows - 1) * 9;
        if (current > 0) {
            menu.button(nav, Items.arrow(true, true), event -> openCompose(player, current - 1));
        }
        menu.button(nav + 3, Items.of(Material.NAME_TAG, "&bWrite by name",
                        "&7For someone who is not online.",
                        "",
                        "&eClick to type a name"),
                event -> plugin.chatPrompts().request(player,
                        "&7Who do you want to write to? &8(type a player name)", answer -> {
                            if (answer.equalsIgnoreCase("cancel")) {
                                Msg.send(player, "&7Cancelled.");
                                open(player, 0);
                                return;
                            }
                            var target = Targets.offline(player, answer);
                            if (target == null) {
                                openCompose(player, current);
                                return;
                            }
                            promptMessage(player, target.getUniqueId(),
                                    target.getName() == null ? answer : target.getName());
                        }));
        menu.button(nav + 5, Items.of(Material.BARRIER, "&cBack"), event -> open(player, 0));
        menu.button(nav + 7, Items.of(Material.PAPER, "&7Page &f" + (current + 1) + "&7/&f" + pages));
        if (current < pages - 1) {
            menu.button(nav + 8, Items.arrow(false, true), event -> openCompose(player, current + 1));
        }
        menu.open(player);
    }

    private void promptMessage(Player player, UUID target, String targetName) {
        plugin.chatPrompts().request(player, "&7Your message to &f" + targetName + "&7:", answer -> {
            if (answer.equalsIgnoreCase("cancel")) {
                Msg.send(player, "&7Cancelled.");
                open(player, 0);
                return;
            }
            if (answer.isBlank()) {
                Msg.error(player, "An empty message was not sent.");
                open(player, 0);
                return;
            }
            redrawAfter(plugin.mail().send(player, target, answer), player, 0);
            Msg.success(player, "Mail sent to " + targetName + ".");
            Player online = plugin.getServer().getPlayer(target);
            if (online != null) {
                Msg.send(online, "&7You received new mail from &f" + player.getName()
                        + "&7. Use &f/mail&7.");
            }
        });
    }

    /* ---------------------------------------------------------- helpers --- */

    /** Splits a message into lore-sized lines so a long message stays readable. */
    static List<String> wrap(String message) {
        List<String> lines = new ArrayList<>();
        if (message == null || message.isBlank()) {
            return List.of("");
        }
        StringBuilder line = new StringBuilder();
        for (String word : message.trim().split("\\s+")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > 42) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}
