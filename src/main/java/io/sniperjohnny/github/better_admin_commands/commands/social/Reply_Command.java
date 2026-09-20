package io.sniperjohnny.github.better_admin_commands.commands.social;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
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
import java.util.UUID;

/** Replies to the last private message. */
public class Reply_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Reply_Command(Better_Admin_Commands plugin) {
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
        UUID lastUuid = plugin.preferences().lastReplyTarget(self.getUniqueId());
        if (lastUuid == null) {
            Msg.error(self, "You have nobody to reply to.");
            return true;
        }
        Player target = Bukkit.getPlayer(lastUuid);
        if (target == null) {
            Msg.error(self, "That player is no longer online.");
            return true;
        }
        if (plugin.preferences().isIgnoring(target.getUniqueId(), self.getName())) {
            Msg.error(self, target.getName() + " is ignoring you.");
            return true;
        }

        String message = String.join(" ", args);
        Msg.raw(self, "&7[&fme &7-> &f" + target.getName() + "&7] &f" + message);
        Msg.raw(target, "&7[&f" + self.getName() + " &7-> &fme&7] &f" + message);

        plugin.preferences().setLastReplyTarget(target.getUniqueId(), self.getUniqueId());
        Msg_Command.notifySocialSpies(self, target, message);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        return Collections.emptyList();
    }
}
