package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of vanished players and hides them from everyone else.
 */
public class VanishService {

    private final Better_Admin_Commands plugin;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public VanishService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    /** Toggles vanish and returns the new state. */
    public boolean toggle(Player player) {
        if (isVanished(player)) {
            setVanished(player, false);
            return false;
        }
        setVanished(player, true);
        return true;
    }

    public void setVanished(Player player, boolean value) {
        if (value) {
            vanished.add(player.getUniqueId());
            for (Player other : plugin.getServer().getOnlinePlayers()) {
                if (!other.equals(player) && !other.hasPermission("betteradmincommands.vanish.see")) {
                    other.hidePlayer(plugin, player);
                }
            }
        } else {
            vanished.remove(player.getUniqueId());
            show(player);
        }
    }

    private void show(Player player) {
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            other.showPlayer(plugin, player);
        }
    }

    /** Hides every vanished player from a player who just joined. */
    public void applyToJoining(Player joiner) {
        if (joiner.hasPermission("betteradmincommands.vanish.see")) {
            return;
        }
        for (UUID uuid : vanished) {
            Player vanishedPlayer = plugin.getServer().getPlayer(uuid);
            if (vanishedPlayer != null && !vanishedPlayer.equals(joiner)) {
                joiner.hidePlayer(plugin, vanishedPlayer);
            }
        }
    }

    public Set<UUID> vanishedPlayers() {
        return Set.copyOf(vanished);
    }
}
