package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
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
import java.util.Locale;

/** Changes the display name of a player, stored in the database. */
public class Nick_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Nick_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }

        Player target;
        String nickname;
        if (args.length == 1) {
            if (!(sender instanceof Player self)) {
                Msg.playerOnly(sender);
                return true;
            }
            target = self;
            nickname = args[0];
        } else {
            if (!sender.hasPermission("betteradmincommands.nick.others")) {
                Msg.noPermission(sender);
                return true;
            }
            target = Targets.online(sender, args[0]);
            if (target == null) {
                return true;
            }
            nickname = args[1];
        }

        if (nickname.equalsIgnoreCase("off") || nickname.equalsIgnoreCase("reset")
                || nickname.equalsIgnoreCase("clear")) {
            plugin.preferences().setNickname(target, null);
            Msg.success(sender, target.equals(sender) ? "Your nickname was removed."
                    : "Removed the nickname of " + target.getName() + ".");
            return true;
        }
        if (nickname.length() > 32) {
            Msg.error(sender, "Nicknames can be at most 32 characters long.");
            return true;
        }
        if (nickname.contains("&") && !sender.hasPermission("betteradmincommands.nick.color")) {
            nickname = nickname.replace("&", "");
        }

        plugin.preferences().setNickname(target, nickname);
        Msg.success(sender, target.equals(sender) ? "Your nickname is now " + nickname + "."
                : target.getName() + " is now called " + nickname + ".");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            if (sender.hasPermission("betteradmincommands.nick.others")) {
                return Targets.complete(args[0]);
            }
            return Targets.completeFrom(args[0].toLowerCase(Locale.ROOT), "off");
        }
        if (args.length == 2 && sender.hasPermission("betteradmincommands.nick.others")) {
            return Targets.completeFrom(args[1].toLowerCase(Locale.ROOT), "off");
        }
        return Collections.emptyList();
    }
}
