package io.sniperjohnny.github.better_admin_commands.kit;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kits are defined in config.yml under {@code kits.<name>} so server owners can
 * change them without recompiling. Cooldowns are kept in memory.
 */
public class KitManager {

    private final Better_Admin_Commands plugin;
    private final Map<UUID, Map<String, Long>> lastUsed = new ConcurrentHashMap<>();

    public KitManager(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    private ConfigurationSection section(String name) {
        ConfigurationSection kits = plugin.getConfig().getConfigurationSection("kits");
        if (kits == null) {
            return null;
        }
        for (String key : kits.getKeys(false)) {
            if (key.equalsIgnoreCase(name)) {
                return kits.getConfigurationSection(key);
            }
        }
        return null;
    }

    public Set<String> names() {
        ConfigurationSection kits = plugin.getConfig().getConfigurationSection("kits");
        return kits == null ? Collections.emptySet() : kits.getKeys(false);
    }

    public boolean exists(String name) {
        return section(name) != null;
    }

    public String permission(String name) {
        ConfigurationSection kit = section(name);
        return kit == null ? null : kit.getString("permission");
    }

    public long cooldownMillis(String name) {
        ConfigurationSection kit = section(name);
        if (kit == null) {
            return 0L;
        }
        long seconds = kit.getLong("cooldown-seconds", 0L);
        return seconds <= 0 ? 0L : seconds * 1000L;
    }

    /** Milliseconds until the player may use the kit again, 0 when ready. */
    public long remainingMillis(Player player, String name) {
        Map<String, Long> used = lastUsed.get(player.getUniqueId());
        if (used == null) {
            return 0L;
        }
        Long last = used.get(name.toLowerCase(Locale.ROOT));
        if (last == null) {
            return 0L;
        }
        long cooldown = cooldownMillis(name);
        if (cooldown <= 0) {
            return 0L;
        }
        long remaining = (last + cooldown) - System.currentTimeMillis();
        return Math.max(0L, remaining);
    }

    public void markUsed(Player player, String name) {
        lastUsed.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                .put(name.toLowerCase(Locale.ROOT), System.currentTimeMillis());
    }

    public void clearCooldown(Player player, String name) {
        Map<String, Long> used = lastUsed.get(player.getUniqueId());
        if (used != null) {
            used.remove(name.toLowerCase(Locale.ROOT));
        }
    }

    /** How a claim attempt ended, so the command and the GUI can react the same way. */
    public enum ClaimResult { SUCCESS, UNKNOWN, NO_ITEMS, NO_PERMISSION, COOLDOWN }

    /** The outcome of a claim, with the remaining cooldown when there is one. */
    public record Claim(ClaimResult result, long remainingMillis) {
    }

    /**
     * Gives a kit to a player, checking the permission and the cooldown first.
     * Everything that does not fit is dropped at the player's feet, and the
     * cooldown only starts when the kit really was handed out.
     */
    public Claim claim(Player player, String name) {
        if (!exists(name)) {
            return new Claim(ClaimResult.UNKNOWN, 0L);
        }
        String permission = permission(name);
        if (permission != null && !permission.isBlank() && !player.hasPermission(permission)) {
            return new Claim(ClaimResult.NO_PERMISSION, 0L);
        }
        long remaining = remainingMillis(player, name);
        if (remaining > 0) {
            return new Claim(ClaimResult.COOLDOWN, remaining);
        }
        List<ItemStack> items = items(name);
        if (items.isEmpty()) {
            return new Claim(ClaimResult.NO_ITEMS, 0L);
        }
        for (ItemStack stack : items) {
            player.getInventory().addItem(stack).forEach((index, leftover) ->
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
        markUsed(player, name);
        return new Claim(ClaimResult.SUCCESS, 0L);
    }

    /** The icon configured for a kit, or {@code null} when none is set. */
    public Material icon(String name) {
        ConfigurationSection kit = section(name);
        if (kit == null) {
            return null;
        }
        String configured = kit.getString("icon");
        if (configured == null || configured.isBlank()) {
            return null;
        }
        Material material = Material.matchMaterial(configured.trim().toUpperCase(Locale.ROOT));
        return material == null || material.isAir() ? null : material;
    }

    /**
     * Builds the item list of a kit. Entry format:
     * {@code MATERIAL}, {@code MATERIAL:AMOUNT} or
     * {@code MATERIAL:AMOUNT:ENCHANTMENT:LEVEL}.
     */
    public List<ItemStack> items(String name) {
        List<ItemStack> result = new ArrayList<>();
        ConfigurationSection kit = section(name);
        if (kit == null) {
            return result;
        }
        for (String raw : kit.getStringList("items")) {
            ItemStack stack = parseItem(raw);
            if (stack != null) {
                result.add(stack);
            } else {
                plugin.getLogger().warning("Kit '" + name + "' has an invalid item entry: " + raw);
            }
        }
        return result;
    }

    private ItemStack parseItem(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.split(":");
        Material material = Material.matchMaterial(parts[0].trim().toUpperCase(Locale.ROOT));
        if (material == null || material.isAir()) {
            return null;
        }
        ItemStack stack = new ItemStack(material, 1);
        if (parts.length > 1) {
            try {
                stack.setAmount(Math.max(1, Math.min(material.getMaxStackSize(), Integer.parseInt(parts[1].trim()))));
            } catch (NumberFormatException ignored) {
                // keep the default amount
            }
        }
        if (parts.length > 3) {
            Enchantment enchantment = resolveEnchantment(parts[2].trim());
            Integer level = parseLevel(parts[3].trim());
            if (enchantment != null && level != null) {
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    meta.addEnchant(enchantment, level, true);
                    stack.setItemMeta(meta);
                }
            }
        }
        return stack;
    }

    private Integer parseLevel(String raw) {
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Enchantment resolveEnchantment(String key) {
        try {
            String value = key.contains(":") ? key.substring(key.indexOf(':') + 1) : key;
            return RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .getOrThrow(Key.key(Key.MINECRAFT_NAMESPACE, value.toLowerCase(Locale.ROOT)));
        } catch (Exception e) {
            return null;
        }
    }
}
