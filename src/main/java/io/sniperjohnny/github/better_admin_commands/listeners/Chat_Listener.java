package io.sniperjohnny.github.better_admin_commands.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.moderation.MuteService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/** Blocks chat messages from muted players. */
public class Chat_Listener implements Listener {

    private final Better_Admin_Commands plugin;

    public Chat_Listener(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        MuteService.Mute mute = plugin.mutes().muteOf(event.getPlayer().getUniqueId());
        if (mute == null) {
            return;
        }
        event.setCancelled(true);
        String template = plugin.getConfig().getString("moderation.mute-message",
                "&cYou are muted. &7Reason: &f%reason%");
        String message = template.replace("%reason%", mute.reason() == null ? "No reason given" : mute.reason());
        if (!mute.permanent()) {
            message = message + Msg.color(" &7(&f" + Targets.formatDuration(mute.remainingMillis() / 1000L) + " left&7)");
        }
        event.getPlayer().sendMessage(Msg.color(message));
    }
}
