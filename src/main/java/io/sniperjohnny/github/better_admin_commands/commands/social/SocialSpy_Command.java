package io.sniperjohnny.github.better_admin_commands.commands.social;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.player.PlayerPreferences;
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

/** Toggles seeing the private messages of other players. */
public class SocialSpy_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public SocialSpy_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player player)) {
            Msg.playerOnly(sender);
            return true;
        }
        boolean enabled = !plugin.preferences().getBoolean(player.getUniqueId(), PlayerPreferences.SOCIAL_SPY, false);
        plugin.preferences().setBoolean(player.getUniqueId(), PlayerPreferences.SOCIAL_SPY, enabled);
        Msg.success(player, enabled ? "Social spy enabled." : "Social spy disabled.");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "on", "off");
        }
        return Collections.emptyList();
    }
}
