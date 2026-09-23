package io.sniperjohnny.github.better_admin_commands.gui;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Asks a player for one chat line and hands the answer to a callback.
 *
 * <p>Used where a GUI alone cannot collect input (a price, a report description).
 * The message is swallowed by {@code Chat_Listener} so it never reaches public
 * chat, and the callback always runs on the server thread.</p>
 */
public class ChatPromptService {

    private final Better_Admin_Commands plugin;
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public ChatPromptService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    /** Asks for a value. The window is closed so the player can type. */
    public void request(Player player, String prompt, Consumer<String> onAnswer) {
        request(player, prompt, onAnswer, true);
    }

    /**
     * Asks for a value while keeping the open window open. Used where closing it
     * would be wrong, for example the trade money button: the trade window has
     * to stay on screen while the amount is typed in chat.
     */
    public void request(Player player, String prompt, Consumer<String> onAnswer, boolean closeWindow) {
        pending.put(player.getUniqueId(), onAnswer);
        if (closeWindow) {
            player.closeInventory();
        }
        Msg.send(player, prompt);
        Msg.send(player, "&7Type &fcancel &7to abort.");
    }

    public boolean isWaiting(UUID uuid) {
        return pending.containsKey(uuid);
    }

    /**
     * Consumes the next chat line of a player. Called from the async chat event,
     * so the callback is scheduled back onto the server thread.
     *
     * @return whether the message was consumed by a prompt
     */
    public boolean handle(UUID uuid, String message) {
        Consumer<String> answer = pending.remove(uuid);
        if (answer == null) {
            return false;
        }
        String text = message == null ? "" : message.trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> answer.accept(text));
        return true;
    }

    /** Drops a pending prompt, for example when a player disconnects. */
    public void clear(UUID uuid) {
        pending.remove(uuid);
    }

    /**
     * Arms a chat answer without printing a question of its own. Used by
     * {@link DialogPromptService} as the fallback for clients that cannot show
     * a dialog: the question is already in chat, this only picks up the line.
     * The message is still swallowed by {@code Chat_Listener}, and the callback
     * still runs on the server thread.
     */
    public void arm(UUID uuid, Consumer<String> onAnswer) {
        pending.put(uuid, onAnswer);
    }
}
