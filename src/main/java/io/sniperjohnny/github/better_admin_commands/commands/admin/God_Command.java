package io.sniperjohnny.github.better_admin_commands.commands.admin;

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

/** Toggles invulnerability for a player. */
public class God_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        Player target;
        if (args.length >= 1) {
            target = Targets.online(sender, args[0]);
            if (target == null) {
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            Msg.playerOnly(sender);
            return true;
        }

        boolean enabled = !target.isInvulnerable();
        target.setInvulnerable(enabled);
        Msg.success(sender, target.equals(sender)
                ? (enabled ? "God mode enabled." : "God mode disabled.")
                : target.getName() + (enabled ? " is now invulnerable." : " is no longer invulnerable."));
        if (!target.equals(sender)) {
            Msg.send(target, enabled ? "&7You are now invulnerable." : "&7You are no longer invulnerable.");
        }
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
