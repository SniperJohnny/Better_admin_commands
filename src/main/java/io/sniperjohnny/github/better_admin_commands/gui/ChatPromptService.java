package io.sniperjohnny.github.better_admin_commands.gui;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatPromptService {

    private static final long LIFETIME_MILLIS = 120_000L;

    private record Pending(Consumer<String> answer, long expiresAt) {
    }

    private final Better_Admin_Commands plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ChatPromptService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void request(Player player, String prompt, Consumer<String> onAnswer) {
        request(player, prompt, onAnswer, true);
    }

    public void request(Player player, String prompt, Consumer<String> onAnswer, boolean closeWindow) {
        arm(player.getUniqueId(), onAnswer);
        if (closeWindow) {
            player.closeInventory();
        }
        Msg.send(player, prompt);
        Msg.send(player, "&7Type &fcancel &7to abort.");
    }

    public boolean isWaiting(UUID uuid) {
        return live(uuid) != null;
    }

    public boolean handle(UUID uuid, String message) {
        Pending entry = live(uuid);
        if (entry == null) {
            return false;
        }
        pending.remove(uuid, entry);
        String text = message == null ? "" : message.trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> entry.answer().accept(text));
        return true;
    }

    public void clear(UUID uuid) {
        pending.remove(uuid);
    }

    public void arm(UUID uuid, Consumer<String> onAnswer) {
        pending.put(uuid, new Pending(onAnswer, System.currentTimeMillis() + LIFETIME_MILLIS));
    }

    private Pending live(UUID uuid) {
        Pending entry = pending.get(uuid);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expiresAt()) {
            pending.remove(uuid, entry);
            return null;
        }
        return entry;
    }
}
