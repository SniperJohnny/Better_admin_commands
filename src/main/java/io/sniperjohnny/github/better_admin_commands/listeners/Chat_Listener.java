package io.sniperjohnny.github.better_admin_commands.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.moderation.MuteService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Handles the chat side of the plugin: GUI prompts are swallowed, muted players
 * are blocked, and messages are rendered with the player's nickname.
 *
 * <p>The renderer runs at {@link EventPriority#LOWEST}, so a chat formatting
 * plugin that sets its own renderer at a later priority replaces this one. When
 * nobody else formats chat, this keeps {@code /nick} and the player's rank
 * prefix working in chat without any extra setup. The real name behind a
 * nickname is never revealed here.</p>
 */
public class Chat_Listener implements Listener {

    /** Lets a player use '&' colour codes in their own chat messages. */
    private static final String COLOR_PERMISSION = "betteradmincommands.chat.color";

    private static final String NICK_TOKEN = "%nickname%";
    private static final String MESSAGE_TOKEN = "%message%";

    private final Better_Admin_Commands plugin;

    public Chat_Listener(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // A GUI (for example the auction house asking for a price) is waiting for
        // the next line: swallow it so it never reaches public chat.
        if (plugin.chatPrompts().isWaiting(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.chatPrompts().handle(player.getUniqueId(),
                    PlainTextComponentSerializer.plainText().serialize(event.message()));
            return;
        }

        MuteService.Mute mute = plugin.mutes().muteOf(event.getPlayer().getUniqueId());
        if (mute != null) {
            event.setCancelled(true);
            String template = plugin.getConfig().getString("moderation.mute-message",
                    "&cYou are muted. &7Reason: &f%reason%");
            String message = template.replace("%reason%", mute.reason() == null ? "No reason given" : mute.reason());
            if (!mute.permanent()) {
                message = message + Msg.color(" &7(&f" + Targets.formatDuration(mute.remainingMillis() / 1000L) + " left&7)");
            }
            event.getPlayer().sendMessage(Msg.color(message));
            return;
        }

        // Colour codes in chat are a permission, so a normal player cannot
        // colour their messages. The message is plain text, so '&c' still shows
        // up literally for everyone else.
        if (plugin.permissions().has(player, COLOR_PERMISSION)) {
            String raw = PlainTextComponentSerializer.plainText().serialize(event.message());
            event.message(Msg.component(raw));
        }

        event.renderer((source, sourceDisplayName, message, viewer) ->
                render(source, message));
    }

    /**
     * Builds one chat line from the player's rank prefix and nickname (or real
     * name when no nickname is set).
     */
    private Component render(Player source, Component message) {
        Component name = plugin.preferences().tabName(source.getUniqueId(), source.getName());
        return format(plugin.getConfig().getString("nick.chat-format", "&f<%nickname%>&r %message%"),
                name, message);
    }

    /** Fills {@code %nickname%} and {@code %message%} into the configured format. */
    private static Component format(String template, Component nickname, Component message) {
        if (template == null || template.isEmpty()) {
            return Component.text("<").append(nickname).append(Component.text("> ")).append(message);
        }
        Component result = Component.empty();
        int index = 0;
        while (index < template.length()) {
            int nick = template.indexOf(NICK_TOKEN, index);
            int text = template.indexOf(MESSAGE_TOKEN, index);
            int next;
            boolean useNickname;
            if (nick >= 0 && (text < 0 || nick < text)) {
                next = nick;
                useNickname = true;
            } else if (text >= 0) {
                next = text;
                useNickname = false;
            } else {
                break;
            }
            result = result.append(Msg.component(template.substring(index, next)));
            result = result.append(useNickname ? nickname : message);
            index = next + (useNickname ? NICK_TOKEN.length() : MESSAGE_TOKEN.length());
        }
        return result.append(Msg.component(template.substring(index)));
    }
}
