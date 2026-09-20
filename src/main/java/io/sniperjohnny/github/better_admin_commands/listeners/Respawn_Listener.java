package io.sniperjohnny.github.better_admin_commands.listeners;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/** Moves the respawn point to the configured spawn when enabled. */
public class Respawn_Listener implements Listener {

    private final Better_Admin_Commands plugin;

    public Respawn_Listener(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfig().getBoolean("spawn.respawn-at-spawn", false)) {
            return;
        }
        Location spawn = plugin.spawns().getSpawn();
        if (spawn != null) {
            event.setRespawnLocation(spawn);
        }
    }
}
