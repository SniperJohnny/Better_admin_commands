package io.sniperjohnny.github.better_admin_commands.listeners;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Registers the player account in the economy, loads their settings, applies the
 * nickname and borrowed skin, and handles spawn and vanish rules when someone
 * joins.
 */
public class Join_Listener implements Listener {

    private final Better_Admin_Commands plugin;

    public Join_Listener(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        plugin.economy().touch(player);
        plugin.preferences().load(player);
        plugin.preferences().applyNickname(player);
        plugin.skins().applyStored(player);
        plugin.afk().forget(player.getUniqueId());
        plugin.vanish().applyToJoining(player);
        plugin.playtime().onJoin(player);

        if (plugin.jails().isJailed(player.getUniqueId())) {
            plugin.jails().sendToCell(player);
            Msg.send(player, "&cYou are jailed.");
        }

        int unread = plugin.mail().unreadCount(player.getUniqueId());
        if (unread > 0) {
            Msg.send(player, "&7You have &f" + unread + " &7unread mail message(s). Use &f/mail read&7.");
        }

        if (plugin.jails().isJailed(player.getUniqueId())) {
            return;
        }
        boolean firstJoin = !player.hasPlayedBefore();
        boolean shouldTeleport = plugin.getConfig().getBoolean("spawn.teleport-on-join", false)
                || (firstJoin && plugin.getConfig().getBoolean("spawn.teleport-on-first-join", false));
        if (!shouldTeleport || !plugin.spawns().hasSpawn()) {
            return;
        }
        Location spawn = plugin.spawns().getSpawn();
        if (spawn != null) {
            player.teleport(spawn);
            Msg.send(player, "&7Welcome! You were teleported to the server spawn.");
        }
    }
}
