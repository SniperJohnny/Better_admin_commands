package io.sniperjohnny.github.better_admin_commands.teleport;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every teleport in the plugin goes through here so that warm-ups, movement
 * cancellation, cooldowns and /back all behave the same way.
 */
public class TeleportService {

    private final Better_Admin_Commands plugin;
    private final Map<UUID, Location> backLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> backCooldowns = new ConcurrentHashMap<>();

    private final int warmupSeconds;
    private final boolean backEnabled;
    private final long backCooldownMillis;

    public TeleportService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
        this.warmupSeconds = Math.max(0, plugin.getConfig().getInt("teleport.warmup-seconds", 3));
        this.backEnabled = plugin.getConfig().getBoolean("teleport.back-enabled", true);
        this.backCooldownMillis = Math.max(0, plugin.getConfig().getLong("teleport.back-cooldown-seconds", 0)) * 1000L;
    }

    /** Stores the current position so /back can return to it later. */
    public void remember(Player player) {
        if (backEnabled) {
            backLocations.put(player.getUniqueId(), player.getLocation().clone());
        }
    }

    public Location getBackLocation(UUID uuid) {
        Location location = backLocations.get(uuid);
        return location == null ? null : location.clone();
    }

    public boolean hasBackLocation(UUID uuid) {
        return backLocations.containsKey(uuid);
    }

    /** Teleports with the configured warm-up, remembering the previous location. */
    public void requestTeleport(Player player, Location destination) {
        if (destination == null || destination.getWorld() == null) {
            Msg.error(player, "That destination is not valid any more.");
            return;
        }
        if (plugin.jails().isJailed(player.getUniqueId())) {
            Msg.error(player, "You cannot teleport while you are jailed.");
            return;
        }
        remember(player);

        if (warmupSeconds <= 0) {
            player.teleport(destination);
            return;
        }

        Location start = player.getLocation().clone();
        Msg.send(player, "&7Teleporting in &f" + warmupSeconds + "&7 seconds, don't move.");
        new BukkitRunnable() {
            int remaining = warmupSeconds;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (player.getLocation().distanceSquared(start) > 1.0) {
                    Msg.error(player, "Teleport cancelled because you moved.");
                    cancel();
                    return;
                }
                remaining--;
                if (remaining <= 0) {
                    cancel();
                    player.teleport(destination);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /** Teleports without warm-up or messages, still remembering the old position. */
    public void teleportNow(Player player, Location destination) {
        remember(player);
        if (destination != null) {
            player.teleport(destination);
        }
    }

    /** Teleports a player to their previous location. */
    public boolean back(Player player) {
        if (!backEnabled) {
            Msg.error(player, "/back is disabled on this server.");
            return false;
        }
        Long until = backCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (until != null && until > now) {
            Msg.error(player, "You have to wait " + ((until - now) / 1000L) + "s before using /back again.");
            return false;
        }
        Location destination = getBackLocation(player.getUniqueId());
        if (destination == null) {
            Msg.error(player, "There is no previous location to go back to.");
            return false;
        }
        backCooldowns.put(player.getUniqueId(), now + backCooldownMillis);
        player.teleport(destination);
        Msg.success(player, "Teleported back.");
        return true;
    }
}
