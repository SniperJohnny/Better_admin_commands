package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks how long a player has been on the server. The finished seconds are
 * stored in the player settings, the current session is kept in memory.
 */
public class PlaytimeService {

    private static final String SETTING = "playtime";

    private final Better_Admin_Commands plugin;
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();

    public PlaytimeService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public void onJoin(Player player) {
        sessionStart.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /** Stores the session length and forgets it. Called on quit. */
    public void onQuit(Player player) {
        Long start = sessionStart.remove(player.getUniqueId());
        if (start == null) {
            return;
        }
        long stored = storedSeconds(player.getUniqueId());
        long total = stored + Math.max(0, (System.currentTimeMillis() - start) / 1000L);
        plugin.preferences().set(player.getUniqueId(), SETTING, Long.toString(total));
    }

    public long storedSeconds(UUID uuid) {
        String raw = plugin.preferences().get(uuid, SETTING, "0");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** Total playtime including the running session. */
    public long seconds(Player player) {
        long stored = storedSeconds(player.getUniqueId());
        Long start = sessionStart.get(player.getUniqueId());
        if (start == null) {
            return stored;
        }
        return stored + Math.max(0, (System.currentTimeMillis() - start) / 1000L);
    }

    /** Formats seconds as {@code 3d 4h 12m}. */
    public static String format(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("d ");
        }
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        builder.append(minutes).append("m");
        return builder.toString().trim();
    }
}
