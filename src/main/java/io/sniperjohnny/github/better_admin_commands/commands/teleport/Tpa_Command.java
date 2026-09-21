package io.sniperjohnny.github.better_admin_commands.commands.teleport;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.player.PlayerPreferences;
import io.sniperjohnny.github.better_admin_commands.teleport.TpaService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Asks another player for permission to teleport to them. */
public class Tpa_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Tpa_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player self)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        Player target = Targets.online(sender, args[0]);
        if (target == null) {
            return true;
        }
        if (target.equals(self)) {
            Msg.error(self, "You cannot send a request to yourself.");
            return true;
        }
        if (!plugin.preferences().getBoolean(target.getUniqueId(), PlayerPreferences.TELEPORT_TOGGLE, true)) {
            Msg.error(self, target.getName() + " does not accept teleport requests.");
            return true;
        }
        plugin.tpa().add(self.getUniqueId(), target.getUniqueId(), false);
        Msg.success(self, "Teleport request sent to " + target.getName() + ".");
        TpaService.sendNotice(target, self.getName(), false);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.complete(args[0]);
        }
        return Collections.emptyList();
    }
}
