package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks who is away from keyboard, both manually through /afk and
 * automatically after the configured amount of idle time. Activity is recorded
 * from movement events, so no server internal API is needed.
 */
public class AfkService {

    private final Better_Admin_Commands plugin;
    private final Map<UUID, String> afk = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();

    public AfkService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public boolean isAfk(Player player) {
        return afk.containsKey(player.getUniqueId());
    }

    public String reason(UUID uuid) {
        return afk.get(uuid);
    }

    /** Called whenever a player moves to a new block or interacts. */
    public void recordActivity(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
        if (afk.containsKey(player.getUniqueId())) {
            clearSilently(player);
        }
    }

    public void setAfk(Player player, String reason) {
        boolean wasAfk = afk.containsKey(player.getUniqueId());
        afk.put(player.getUniqueId(), reason == null ? "" : reason);
        if (!wasAfk) {
            String suffix = reason == null || reason.isBlank() ? "" : " (" + reason + ")";
            plugin.getServer().broadcast(Msg.component("&7" + player.getName() + " is now away" + suffix));
        }
    }

    public void clear(Player player) {
        if (afk.remove(player.getUniqueId()) != null) {
            plugin.getServer().broadcast(Msg.component("&7" + player.getName() + " is no longer away"));
        }
    }

    private void clearSilently(Player player) {
        afk.remove(player.getUniqueId());
    }

    public void forget(UUID uuid) {
        afk.remove(uuid);
        lastActivity.remove(uuid);
    }

    /** Marks everybody who has been idle long enough as afk. */
    public void checkIdle(int idleMinutes) {
        long threshold = System.currentTimeMillis() - (idleMinutes * 60_000L);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isAfk(player)) {
                continue;
            }
            Long last = lastActivity.get(player.getUniqueId());
            if (last != null && last < threshold) {
                setAfk(player, "idle");
            }
        }
    }
}
