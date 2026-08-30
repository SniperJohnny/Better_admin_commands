package io.sniperjohnny.github.custom_smp_plugin.quality_of_life.listeners;

import io.sniperjohnny.github.custom_smp_plugin.Custom_SMP_Plugin;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class Leave_Listener implements Listener {
    Custom_SMP_Plugin plugin;
    public Leave_Listener (Custom_SMP_Plugin instance) {
        plugin = instance;
    }
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        String quitmessagebluprint = this.plugin.getConfig().getString("leave.message");
        String quit_message = quitmessagebluprint.replace("&player&", e.getPlayer().getDisplayName());
        if (e.getPlayer().isOp()) {
            e.getPlayer().sendMessage(ChatColor.RED + "Welcome " + e.getPlayer().getDisplayName() + ", thanks for joining and downloading this plugin!");
        }
        if (quit_message != null) {
            if (quit_message.equalsIgnoreCase("0")) {
                e.setQuitMessage("");
            } else if (quit_message.equalsIgnoreCase("")) {
                e.setQuitMessage(e.getPlayer().getDisplayName() + " has left the game."+ ChatColor.YELLOW);
            } else {
                e.setQuitMessage(ChatColor.YELLOW + quit_message);
            }
        }
        
        // Save player data when they leave
        plugin.getYamlPlayerCreator().createPlayerFileIfNotExists(e.getPlayer());
    }
}