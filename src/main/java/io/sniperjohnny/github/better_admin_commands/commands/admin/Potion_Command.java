package io.sniperjohnny.github.better_admin_commands.commands.admin;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.sniperjohnny.github.better_admin_commands.util.Msg;
import io.sniperjohnny.github.better_admin_commands.util.Targets;
import net.kyori.adventure.key.Key;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Applies a potion effect to a player. */
public class Potion_Command implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length < 1) {
            Msg.usage(sender, command);
            return true;
        }
        PotionEffectType type = resolve(args[0]);
        if (type == null) {
            Msg.error(sender, "There is no potion effect called " + args[0] + ".");
            return true;
        }

        Player target;
        int index = 1;
        if (args.length >= 2 && !isNumber(args[1]) && !args[1].equalsIgnoreCase("infinite")) {
            target = Targets.online(sender, args[1]);
            if (target == null) {
                return true;
            }
            index = 2;
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            Msg.playerOnly(sender);
            return true;
        }

        int duration = 30;
        if (args.length > index) {
            if (args[index].equalsIgnoreCase("infinite")) {
                duration = PotionEffect.INFINITE_DURATION;
            } else {
                Integer parsed = Targets.parseInt(args[index]);
                if (parsed == null) {
                    Msg.error(sender, "The duration has to be a number of seconds or 'infinite'.");
                    return true;
                }
                duration = Math.max(1, parsed);
            }
            index++;
        }
        int amplifier = 1;
        if (args.length > index) {
            Integer parsed = Targets.parseInt(args[index]);
            if (parsed == null) {
                Msg.error(sender, "The amplifier has to be a number.");
                return true;
            }
            amplifier = Math.max(1, parsed);
        }

        target.addPotionEffect(new PotionEffect(type, duration * 20, amplifier - 1));
        Msg.success(sender, "Applied " + args[0].toLowerCase(Locale.ROOT) + " to " + target.getName() + ".");
        if (!target.equals(sender)) {
            Msg.send(target, "&7You received a potion effect from &f" + sender.getName() + "&7.");
        }
        return true;
    }

    static PotionEffectType resolve(String name) {
        String key = name.toLowerCase(Locale.ROOT).replace(' ', '_');
        if (key.startsWith("minecraft:")) {
            key = key.substring("minecraft:".length());
        }
        try {
            return RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.MOB_EFFECT)
                    .get(Key.key(Key.MINECRAFT_NAMESPACE, key));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isNumber(String raw) {
        return Targets.parseInt(raw) != null;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String @NotNull[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            try {
                for (PotionEffectType type : RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.MOB_EFFECT)) {
                    names.add(type.key().value());
                }
            } catch (Exception ignored) {
                // registry not available - no completions
            }
            return Targets.completeFrom(args[0].toLowerCase(Locale.ROOT), names);
        }
        if (args.length == 2) {
            return Targets.complete(args[1]);
        }
        if (args.length == 3) {
            return Targets.completeFrom(args[2], "10", "30", "60", "infinite");
        }
        if (args.length == 4) {
            return Targets.completeFrom(args[3], "1", "2", "3");
        }
        return Collections.emptyList();
    }
}
