package io.sniperjohnny.github.better_admin_commands.player;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VanishService {

    public static final String SEE_PERMISSION = "betteradmincommands.vanish.see";

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
            // A mob that is already chasing the player would keep attacking even
            // though new targets are refused, so the existing targets are cleared.
            if (plugin.getConfig().getBoolean("moderation.vanish-mobs-ignore", true)) {
                clearMobTargets(player);
            }
        } else {
            vanished.remove(player.getUniqueId());
        }
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (other.equals(player)) {
                continue;
            }
            apply(other, player, !value || other.hasPermission(SEE_PERMISSION));
        }
        // The tab list name carries the cue while the player is vanished.
        plugin.preferences().applyNickname(player);
    }

    public void applyToJoining(Player joiner) {
        boolean maySee = joiner.hasPermission(SEE_PERMISSION);
        for (UUID uuid : vanished) {
            Player vanishedPlayer = plugin.getServer().getPlayer(uuid);
            if (vanishedPlayer == null || vanishedPlayer.equals(joiner)) {
                continue;
            }
            apply(joiner, vanishedPlayer, maySee);
        }
    }

    private void apply(Player viewer, Player vanishedPlayer, boolean visible) {
        if (visible) {
            viewer.showEntity(plugin, vanishedPlayer);
            if (viewer.canSee(vanishedPlayer)) {
                viewer.listPlayer(vanishedPlayer);
            }
            return;
        }
        viewer.hideEntity(plugin, vanishedPlayer);
        viewer.unlistPlayer(vanishedPlayer);
    }

    public Set<UUID> vanishedPlayers() {
        return Set.copyOf(vanished);
    }

    private static void clearMobTargets(Player player) {
        // A generous box: anything that could reasonably be following a player.
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 96.0, 96.0, 96.0)) {
            if (entity instanceof Mob mob && mob.getTarget() != null
                    && mob.getTarget().getUniqueId().equals(player.getUniqueId())) {
                mob.setTarget(null);
            }
        }
    }
}
