package io.sniperjohnny.github.better_admin_commands.commands.social;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.player.PlayerPreferences;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Sends a private message to another player, with social spy support. */
public class Msg_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Msg_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player self)) {
            Msg.playerOnly(sender);
            return true;
        }
        if (args.length < 2) {
            Msg.usage(sender, command);
            return true;
        }
        Player target = Targets.online(sender, args[0]);
        if (target == null) {
            return true;
        }
        if (target.equals(self)) {
            Msg.error(self, "You cannot message yourself.");
            return true;
        }
        if (plugin.preferences().isIgnoring(target.getUniqueId(), self.getName())) {
            Msg.error(self, target.getName() + " is ignoring you.");
            return true;
        }
        if (plugin.preferences().isIgnoring(self.getUniqueId(), target.getName())) {
            Msg.error(self, "You are ignoring " + target.getName() + ". Use /ignore to change that.");
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Msg.raw(self, "&7[&fme &7-> &f" + target.getName() + "&7] &f" + message);
        Msg.raw(target, "&7[&f" + self.getName() + " &7-> &fme&7] &f" + message);

        plugin.preferences().setLastReplyTarget(self.getUniqueId(), target.getUniqueId());
        plugin.preferences().setLastReplyTarget(target.getUniqueId(), self.getUniqueId());
        notifySocialSpies(self, target, message);
        return true;
    }

    static void notifySocialSpies(Player sender, Player target, String message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(sender) || online.equals(target)) {
                continue;
            }
            if (plugin().preferences().getBoolean(online.getUniqueId(), PlayerPreferences.SOCIAL_SPY, false)) {
                Msg.raw(online, "&8[&5Spy&8] &7" + sender.getName() + " &8-> &7" + target.getName() + "&8: &f" + message);
            }
        }
    }

    private static Better_Admin_Commands plugin() {
        return Better_Admin_Commands.get_Instance();
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
