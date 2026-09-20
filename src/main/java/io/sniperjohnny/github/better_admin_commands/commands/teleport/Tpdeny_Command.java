package io.sniperjohnny.github.better_admin_commands.commands.teleport;

import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.teleport.TpaService;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Denies a pending teleport request. */
public class Tpdeny_Command implements TabExecutor {

    private final Better_Admin_Commands plugin;

    public Tpdeny_Command(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (!(sender instanceof Player self)) {
            Msg.playerOnly(sender);
            return true;
        }
        List<TpaService.Request> requests = plugin.tpa().forTarget(self.getUniqueId());
        if (requests.isEmpty()) {
            Msg.error(self, "You have no pending teleport requests.");
            return true;
        }

        TpaService.Request request;
        if (args.length >= 1) {
            Player who = Targets.online(sender, args[0]);
            if (who == null) {
                return true;
            }
            Optional<TpaService.Request> found = plugin.tpa().poll(self.getUniqueId(), who.getUniqueId());
            if (found.isEmpty()) {
                Msg.error(self, who.getName() + " has not sent you a teleport request.");
                return true;
            }
            request = found.get();
        } else {
            request = requests.get(requests.size() - 1);
            plugin.tpa().poll(self.getUniqueId(), request.sender());
        }

        Msg.success(self, "Teleport request denied.");
        Player other = Bukkit.getPlayer(request.sender());
        if (other != null) {
            Msg.send(other, "&7Your teleport request was denied by &f" + self.getName() + "&7.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            List<String> names = new ArrayList<>();
            for (TpaService.Request request : plugin.tpa().forTarget(player.getUniqueId())) {
                Player other = Bukkit.getPlayer(request.sender());
                if (other != null) {
                    names.add(other.getName());
                }
            }
            return Targets.completeFrom(args[0], names);
        }
        return Collections.emptyList();
    }
}
