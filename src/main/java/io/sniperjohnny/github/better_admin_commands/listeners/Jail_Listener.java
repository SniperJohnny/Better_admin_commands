package io.sniperjohnny.github.better_admin_commands.listeners;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/** Keeps jailed players inside their cell and reports when their time is up. */
public class Jail_Listener implements Listener {

    private final Better_Admin_Commands plugin;

    public Jail_Listener(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.jails().isJailed(player.getUniqueId())) {
            return;
        }
        var entry = plugin.jails().entryOf(player.getUniqueId());
        if (entry == null) {
            return;
        }
        Location cell = plugin.jails().get(entry.jail());
        Location to = event.getTo();
        if (cell == null || to == null || !cell.getWorld().equals(to.getWorld())) {
            return;
        }
        if (cell.distanceSquared(to) > Math.pow(plugin.jails().radius(), 2)) {
            event.setTo(cell);
            Msg.send(player, "&cYou are jailed and cannot leave this area.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!plugin.jails().isJailed(player.getUniqueId())) {
            return;
        }
        var entry = plugin.jails().entryOf(player.getUniqueId());
        if (entry == null) {
            return;
        }
        Location cell = plugin.jails().get(entry.jail());
        if (cell != null) {
            event.setRespawnLocation(cell);
        }
    }

    /** Releases players whose jail time has run out and reminds the rest. */
    public void checkExpired() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            var entry = plugin.jails().entryOf(player.getUniqueId());
            if (entry == null) {
                continue;
            }
            if (entry.expired()) {
                plugin.jails().unjail(player.getUniqueId());
                Msg.send(player, "&aYour jail sentence is over, you are free.");
                continue;
            }
            if (!entry.permanent() && entry.remainingMillis() < 30_000L && entry.remainingMillis() > 0) {
                Msg.send(player, "&7You will be released in &f"
                        + Targets.formatDuration(entry.remainingMillis() / 1000L) + "&7.");
            }
        }
    }
}
