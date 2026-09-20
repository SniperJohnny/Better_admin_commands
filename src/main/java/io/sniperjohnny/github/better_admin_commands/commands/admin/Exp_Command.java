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
import java.util.Locale;

/** Manages experience points: /exp &lt;show|give|set&gt; [player] [amount]. */
public class Exp_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);

        Player target;
        int amount = 0;
        if (action.equals("show")) {
            if (args.length >= 2) {
                target = Targets.online(sender, args[1]);
                if (target == null) {
                    return true;
                }
            } else if (sender instanceof Player self) {
                target = self;
            } else {
                Msg.playerOnly(sender);
                return true;
            }
            Msg.send(sender, "&f" + target.getName() + " &7has &f" + target.getLevel()
                    + " &7levels and &f" + target.getTotalExperience() + " &7XP.");
            return true;
        }

        if (!action.equals("give") && !action.equals("set")) {
            Msg.usage(sender, command);
            return true;
        }
        if (args.length < 3) {
            Msg.usage(sender, command);
            return true;
        }
        target = Targets.online(sender, args[1]);
        if (target == null) {
            return true;
        }
        Integer parsed = Targets.parseInt(args[2]);
        if (parsed == null) {
            Msg.error(sender, "The amount has to be a number.");
            return true;
        }

        if (action.equals("give")) {
            target.giveExp(parsed);
        } else {
            target.setLevel(0);
            target.setExp(0f);
            target.setTotalExperience(0);
            target.giveExp(parsed);
        }
        Msg.success(sender, (action.equals("give") ? "Gave " : "Set ") + parsed + " XP for "
                + target.getName() + ". New level: " + target.getLevel() + ".");
        if (!target.equals(sender)) {
            Msg.send(target, "&7Your experience was changed by &f" + sender.getName() + "&7.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            return Targets.completeFrom(args[0], "show", "give", "set");
        }
        if (args.length == 2) {
            return Targets.complete(args[1]);
        }
        if (args.length == 3) {
            return Targets.completeFrom(args[2], "1", "10", "50", "100");
        }
        return Collections.emptyList();
    }
}
