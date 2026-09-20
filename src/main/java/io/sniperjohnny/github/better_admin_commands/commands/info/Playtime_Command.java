package io.sniperjohnny.github.better_admin_commands.commands.info;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.player.PlaytimeService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Shows how long a player has been on the server. */
public class Playtime_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Playtime_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        long seconds;
        String name;
        if (args.length >= 1) {
            if (!sender.hasPermission("betteradmincommands.playtime.others")) {
                Msg.noPermission(sender);
                return true;
            }
            OfflinePlayer target = Targets.offline(sender, args[0]);
            if (target == null) {
                return true;
            }
            name = target.getName() == null ? args[0] : target.getName();
            Player online = target.getPlayer();
            if (online != null) {
                seconds = plugin.playtime().seconds(online);
            } else {
                // Offline values are only available once the settings of that
                // player have been loaded, so fall back to zero when unknown.
                seconds = 0L;
                Msg.send(sender, "&7Playtime of offline players is only known after they joined once with this version.");
            }
        } else if (sender instanceof Player self) {
            name = self.getName();
            seconds = plugin.playtime().seconds(self);
        } else {
            Msg.playerOnly(sender);
            return true;
        }

        Msg.send(sender, "&f" + name + " &7has played for &f" + PlaytimeService.format(seconds) + "&7.");
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
