package io.sniperjohnny.github.better_admin_commands.listeners;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Persists economy data and clears per-player state when a player leaves. */
public class Quit_Listener implements Listener {

    private final Better_Admin_Commands plugin;

    public Quit_Listener(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.tpa().clear(player.getUniqueId());
        plugin.permissions().clear(player);
        plugin.economy().saveAsync(player.getUniqueId());
        plugin.afk().forget(player.getUniqueId());
        // Playtime has to be written before the settings cache is dropped.
        plugin.playtime().onQuit(player);
        plugin.preferences().unload(player.getUniqueId());
    }
}
