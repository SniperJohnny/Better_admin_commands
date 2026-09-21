package io.sniperjohnny.github.better_admin_commands.commands.teleport;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.player.PlayerPreferences;
import io.sniperjohnny.github.better_admin_commands.teleport.TpaService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Asks every online player to teleport to the sender. */
public class Tpaall_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Tpaall_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player self)) {
            Msg.playerOnly(sender);
            return true;
        }
        int sent = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(self)) {
                continue;
            }
            if (!plugin.preferences().getBoolean(online.getUniqueId(), PlayerPreferences.TELEPORT_TOGGLE, true)) {
                continue;
            }
            plugin.tpa().add(self.getUniqueId(), online.getUniqueId(), true);
            TpaService.sendNotice(online, self.getName(), true);
            sent++;
        }
        Msg.success(self, "Sent a teleport request to " + sent + " player(s).");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
