package io.sniperjohnny.github.better_admin_commands.shop;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads EconomyShopGUI's shop files and turns them into this plugin's own shops.
 *
 * <p>The importer is deliberately defensive: instead of relying on the layout of
 * one edition, it walks every YAML file it finds and treats <em>anything that
 * names a material</em> as a shop entry. That works for both the free and the
 * premium edition, and for shop files written by hand, because the layouts only
 * differ in where they put the entries, not in how an entry is described.</p>
 *
 * <p>Every file becomes one shop, named after the file unless the file names
 * itself. Prices, amounts, slots, display names, lore and enchantments are read
 * from the usual key spellings, and unknown keys are simply ignored.</p>
 */
public class EconomyShopGuiImporter {

    /** The result of one import run. */
    public record Imported(List<ShopService.Shop> shops,
                           Map<String, List<ShopService.ShopItem>> items,
                           List<String> sources) {

        public int itemCount() {
            return items.values().stream().mapToInt(List::size).sum();
        }
    }

    /** Files that are never shop files, even though they may contain a material somewhere. */
    private static final Set<String> IGNORED_FILES = Set.of(
            "config.yml", "config.yaml", "messages.yml", "messages.yaml", "sounds.yml", "sounds.yaml",
            "plugin.yml", "language.yml", "language.yaml", "lang.yml", "lang.yaml", "data.yml", "data.yaml");

    /** Key spellings that mark a section as a shop entry. */
    private static final List<String> MATERIAL_KEYS = List.of("material", "item", "type", "material-data");
    private static final List<String> BUY_KEYS = List.of(
            "buy", "buy-price", "buyprice", "buy_price", "price", "cost", "money", "buy-amount", "purchase");
    private static final List<String> SELL_KEYS = List.of(
            "sell", "sell-price", "sellprice", "sell_price", "sell-amount", "sellamount", "sell-worth");
    private static final List<String> AMOUNT_KEYS = List.of("amount", "quantity", "give", "give-amount", "count");
    private static final List<String> NAME_KEYS = List.of("display-name", "displayname", "display_name", "name", "title");
    private static final List<String> LORE_KEYS = List.of("lore", "description", "desc", "text", "lines");
    private static final List<String> SLOT_KEYS = List.of("slot", "position", "index", "place");
    private static final List<String> PAGE_KEYS = List.of("page", "page-number", "pagenumber");
    private static final List<String> SHOP_NAME_KEYS = List.of("shop-name", "shopname", "shop_name", "title", "display-name");
    private static final List<String> ICON_KEYS = List.of("icon", "shop-icon", "display-item", "menu-item", "head");
    private static final List<String> ROWS_KEYS = List.of("rows", "gui-rows", "menu-rows");
    private static final List<String> SIZE_KEYS = List.of("size", "menu-size", "inventory-size", "slots");

    private final Better_Admin_Commands plugin;

    public EconomyShopGuiImporter(Better_Admin_Commands plugin) {
        this.plugin = plugin;
    }

    /* ------------------------------------------------------------- scan --- */

    /** Scans every configured EconomyShopGUI folder. Never throws. */
    public Imported scan() {
        List<File> folders = folders();
        List<ShopService.Shop> shops = new ArrayList<>();
        Map<String, List<ShopService.ShopItem>> items = new LinkedHashMap<>();
        List<String> sources = new ArrayList<>();
        int cap = Math.max(1, plugin.getConfig().getInt("shop.import.max-items", 5000));
        int total = 0;

        for (File folder : folders) {
            List<File> files = new ArrayList<>();
            collectYaml(folder, files);
            files.sort(Comparator.comparing(File::getAbsolutePath));
            for (File file : files) {
                if (total >= cap) {
                    plugin.getLogger().warning("Stopped importing shops after " + cap
                            + " items (shop.import.max-items).");
                    break;
                }
                try {
                    List<ShopService.ShopItem> found = readShopFile(folder, file);
                    if (found.isEmpty()) {
                        continue;
                    }
                    String id = shopId(folder, file);
                    ShopService.ShopItem first = found.get(0);
                    shops.add(new ShopService.Shop(id, displayOf(found, file, id), iconOf(file, first),
                            rowsOf(file)));
                    items.put(id, found);
                    sources.add(relative(folder, file) + " (" + found.size() + " item(s))");
                    total += found.size();
                } catch (RuntimeException e) {
                    plugin.getLogger().warning("Could not read the shop file "
                            + relative(folder, file) + ": " + e.getMessage());
                }
            }
        }
        return new Imported(shops, items, sources);
    }

    /** The plugins/ folders to scan: the configured ones plus anything named EconomyShopGUI*. */
    private List<File> folders() {
        File pluginsDir = plugin.getDataFolder().getParentFile();
        List<File> found = new ArrayList<>();
        if (pluginsDir == null || !pluginsDir.isDirectory()) {
            return found;
        }
        List<String> configured = plugin.getConfig().getStringList("shop.import.folders");
        for (String name : configured) {
            if (name == null || name.isBlank()) {
                continue;
            }
            File folder = new File(pluginsDir, name.trim());
            if (folder.isDirectory() && !found.contains(folder)) {
                found.add(folder);
            }
        }
        File[] children = pluginsDir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory() && child.getName().toLowerCase(Locale.ROOT).startsWith("economyshopgui")
                        && !found.contains(child)) {
                    found.add(child);
                }
            }
        }
        return found;
    }

    private static void collectYaml(File folder, List<File> out) {
        File[] children = folder.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectYaml(child, out);
            } else if (child.getName().toLowerCase(Locale.ROOT).endsWith(".yml")
                    || child.getName().toLowerCase(Locale.ROOT).endsWith(".yaml")) {
                if (!IGNORED_FILES.contains(child.getName().toLowerCase(Locale.ROOT))) {
                    out.add(child);
                }
            }
        }
    }

    /* -------------------------------------------------------- one file ---- */

    /** @return every entry in one shop file; empty when the file holds no shop */
    private List<ShopService.ShopItem> readShopFile(File folder, File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        String id = shopId(folder, file);
        List<ShopService.ShopItem> found = new ArrayList<>();

        // A file may itself describe one item (rare) - check that first.
        if (looksLikeItem(yaml)) {
            ShopService.ShopItem item = buildItem(id, "0", yaml);
            if (item != null) {
                found.add(item);
            }
            return found;
        }

        // A list of entries directly at the root, e.g. "items: [...]".
        for (String key : yaml.getKeys(false)) {
            if (yaml.isList(key)) {
                collectList(id, key, yaml.getList(key), found);
            }
        }
        walk(yaml, id, "", found);

        // Keep the natural file order: page first, then the slot it was given.
        found.sort(Comparator.comparingInt(ShopService.ShopItem::page)
                .thenComparingInt(ShopService.ShopItem::slot));
        return found;
    }

    /** Walks a section, turning every item-looking child into a shop entry. */
    private void walk(ConfigurationSection section, String shopId, String path, List<ShopService.ShopItem> out) {
        for (String key : section.getKeys(false)) {
            String childPath = path.isEmpty() ? key : path + "." + key;
            if (section.isConfigurationSection(key)) {
                ConfigurationSection child = section.getConfigurationSection(key);
                if (child == null) {
                    continue;
                }
                if (looksLikeItem(child)) {
                    ShopService.ShopItem item = buildItem(shopId, childPath, child);
                    if (item != null) {
                        out.add(item);
                    }
                    continue; // an item's children are its own details
                }
                walk(child, shopId, childPath, out);
            } else if (section.isList(key)) {
                collectList(shopId, childPath, section.getList(key), out);
            } else if (section.isItemStack(key)) {
                ItemStack stack = section.getItemStack(key);
                if (stack != null && !stack.getType().isAir()) {
                    out.add(new ShopService.ShopItem(shopId, childPath, stack.getType(), stack,
                            -1.0, -1.0, out.size(), 1, searchText(stack)));
                }
            }
        }
    }

    /** Handles {@code items:} written as a list, either of maps or of material names. */
    private void collectList(String shopId, String path, List<?> list, List<ShopService.ShopItem> out) {
        if (list == null) {
            return;
        }
        int index = 0;
        for (Object element : list) {
            String elementPath = path + "." + index;
            index++;
            if (element instanceof Map<?, ?> map) {
                MemoryConfiguration memory = new MemoryConfiguration();
                ConfigurationSection section = memory.createSection(elementPath, map);
                if (looksLikeItem(section)) {
                    ShopService.ShopItem item = buildItem(shopId, elementPath, section);
                    if (item != null) {
                        out.add(item);
                    }
                } else {
                    walk(section, shopId, elementPath, out);
                }
            } else if (element instanceof String text) {
                Material material = parseMaterial(text);
                if (material != null) {
                    ItemStack stack = new ItemStack(material);
                    out.add(new ShopService.ShopItem(shopId, elementPath, material, stack,
                            -1.0, -1.0, out.size(), 1, searchText(stack)));
                }
            }
        }
    }

    /* ------------------------------------------------------- item building - */

    /** Whether a section describes a shop entry, i.e. whether it names a material. */
    private boolean looksLikeItem(ConfigurationSection section) {
        for (String key : MATERIAL_KEYS) {
            String raw = rawString(section, key);
            if (raw != null && parseMaterial(raw) != null) {
                return true;
            }
        }
        return false;
    }

    private ShopService.ShopItem buildItem(String shopId, String path, ConfigurationSection section) {
        Material material = null;
        for (String key : MATERIAL_KEYS) {
            material = parseMaterial(rawString(section, key));
            if (material != null) {
                break;
            }
        }
        if (material == null) {
            return null;
        }

        int amount = intOf(section, AMOUNT_KEYS, 1);
        ItemStack stack = new ItemStack(material, Math.max(1, Math.min(material.getMaxStackSize(), amount)));

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String name = stringOf(section, NAME_KEYS);
            if (name != null && !name.isBlank()) {
                meta.displayName(colorize(name));
            }
            List<String> lore = loreOf(section);
            if (!lore.isEmpty()) {
                List<net.kyori.adventure.text.Component> lines = new ArrayList<>(lore.size());
                for (String line : lore) {
                    lines.add(colorize(line));
                }
                meta.lore(lines);
            }
            enchantmentsOf(section).forEach((enchantment, level) ->
                    meta.addEnchant(enchantment, level, true));
            int model = intOf(section, List.of("custom-model-data", "custommodeldata", "model", "custom-model"), 0);
            if (model > 0) {
                meta.setCustomModelData(model);
            }
            if (meta instanceof SkullMeta skull) {
                String owner = stringOf(section, List.of("skull-owner", "skull", "head-owner", "owner"));
                if (owner != null && !owner.isBlank() && !owner.equalsIgnoreCase("self")) {
                    var offline = Bukkit.getOfflinePlayerIfCached(owner);
                    if (offline != null) {
                        skull.setOwningPlayer(offline);
                    }
                }
            }
            stack.setItemMeta(meta);
        }

        double buy = priceOf(section, true);
        double sell = priceOf(section, false);
        int slot = intOf(section, SLOT_KEYS, -1);
        int page = Math.max(1, intOf(section, PAGE_KEYS, 1));

        // An entry with no slot keeps -1, which tells the layout to drop it into the
        // next free slot of its page. Filling in the page number here instead used to
        // push every slotless entry to that column, scrambling the order.
        return new ShopService.ShopItem(shopId, path, material, stack, buy, sell, slot, page,
                searchText(stack));
    }

    /**
     * Reads the buy or sell price. Prices may sit directly on the entry or in a
     * nested {@code price:}/{@code economy:} section, and may be written as a
     * number or as a string such as {@code "*1.5"}.
     *
     * @return the price, or -1 when this entry does not offer that side of the trade
     */
    private double priceOf(ConfigurationSection section, boolean buying) {
        List<String> keys = buying ? BUY_KEYS : SELL_KEYS;
        Double direct = doubleOf(section, keys);
        if (direct != null) {
            return direct;
        }
        for (String nested : List.of("price", "prices", "economy", "cost", "values", "trade")) {
            for (String key : keys(section, nested)) {
                ConfigurationSection child = section.getConfigurationSection(key);
                if (child == null) {
                    continue;
                }
                Double value = doubleOf(child, keys);
                if (value == null && !buying) {
                    // "price: { amount: 10, sell: 5 }" also shows up the other way round
                    continue;
                }
                if (value != null) {
                    return value;
                }
                if (buying) {
                    Double amount = doubleOf(child, List.of("amount", "value", "default"));
                    if (amount != null) {
                        return amount;
                    }
                }
            }
        }
        if (buying) {
            // Some layouts only have a "sell-multiplier" and a single base price.
            Double base = doubleOf(section, List.of("price", "cost", "money"));
            if (base != null) {
                return base;
            }
        }
        return -1.0;
    }

    private Map<Enchantment, Integer> enchantmentsOf(ConfigurationSection section) {
        Map<Enchantment, Integer> found = new LinkedHashMap<>();
        for (String container : List.of("enchantments", "enchants", "enchantment")) {
            for (String key : keys(section, container)) {
                ConfigurationSection child = section.getConfigurationSection(key);
                if (child != null) {
                    for (String raw : child.getKeys(false)) {
                        Enchantment enchantment = parseEnchantment(raw);
                        if (enchantment != null) {
                            found.put(enchantment, Math.max(1, intOf(child, List.of(raw), 1)));
                        }
                    }
                    continue;
                }
                Object value = section.get(key);
                if (value instanceof String text) {
                    Enchantment enchantment = parseEnchantment(text);
                    if (enchantment != null) {
                        found.put(enchantment, 1);
                    }
                } else if (value instanceof List<?> list) {
                    for (Object element : list) {
                        if (element instanceof String text) {
                            String[] parts = text.trim().split("[:\\s]+");
                            Enchantment enchantment = parseEnchantment(parts[0]);
                            if (enchantment != null) {
                                int level = parts.length > 1 ? parseInt(parts[1], 1) : 1;
                                found.put(enchantment, Math.max(1, level));
                            }
                        }
                    }
                }
            }
        }
        return found;
    }

    private List<String> loreOf(ConfigurationSection section) {
        for (String key : LORE_KEYS) {
            for (String actual : keys(section, key)) {
                Object value = section.get(actual);
                if (value instanceof List<?> list) {
                    List<String> lines = new ArrayList<>(list.size());
                    for (Object element : list) {
                        if (element != null) {
                            lines.add(String.valueOf(element));
                        }
                    }
                    if (!lines.isEmpty()) {
                        return lines;
                    }
                } else if (value instanceof String text && !text.isBlank()) {
                    return List.of(text.split("\\R"));
                }
            }
        }
        return List.of();
    }

    /* --------------------------------------------------- shop metadata ----- */

    /** A stable, lowercase id built from the path inside the plugin folder. */
    private static String shopId(File folder, File file) {
        String relative = relative(folder, file);
        int dot = relative.lastIndexOf('.');
        if (dot > 0) {
            relative = relative.substring(0, dot);
        }
        return relative.replace('\\', '/').toLowerCase(Locale.ROOT);
    }

    private String displayOf(List<ShopService.ShopItem> items, File file, String id) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : SHOP_NAME_KEYS) {
            for (String actual : keys(yaml, key)) {
                String value = rawString(yaml, actual);
                if (value != null && !value.isBlank() && parseMaterial(value) == null) {
                    return value;
                }
            }
        }
        String base = id.contains("/") ? id.substring(id.lastIndexOf('/') + 1) : id;
        return prettify(base);
    }

    private ItemStack iconOf(File file, ShopService.ShopItem fallback) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : ICON_KEYS) {
            for (String actual : keys(yaml, key)) {
                Material material = parseMaterial(rawString(yaml, actual));
                if (material != null) {
                    return new ItemStack(material);
                }
            }
        }
        return new ItemStack(fallback.material());
    }

    private int rowsOf(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : ROWS_KEYS) {
            for (String actual : keys(yaml, key)) {
                int rows = (int) toDouble(yaml.get(actual), -1);
                if (rows >= 2) {
                    return Math.min(6, rows);
                }
            }
        }
        for (String key : SIZE_KEYS) {
            for (String actual : keys(yaml, key)) {
                int size = (int) toDouble(yaml.get(actual), -1);
                if (size >= 18) {
                    return Math.max(3, Math.min(6, size / 9));
                }
            }
        }
        return 0; // 0 means "use the configured default"
    }

    /* -------------------------------------------------------- plugins ----- */

    /**
     * Turns EconomyShopGUI off after a successful import, so the two shops
     * cannot both answer to /shop.
     *
     * @return the name of the plugin that was disabled, or {@code null}
     */
    public String disablePluginIfPresent() {
        PluginManager manager = plugin.getServer().getPluginManager();
        for (String name : List.of("EconomyShopGUI-Premium", "EconomyShopGUI")) {
            Plugin target = manager.getPlugin(name);
            if (target != null && target.isEnabled()) {
                manager.disablePlugin(target);
                return target.getName();
            }
        }
        return null;
    }

    /* --------------------------------------------------------- helpers ---- */

    /** Every key spelling for a section that actually exists in the config. */
    private static List<String> keys(ConfigurationSection section, String wanted) {
        List<String> found = new ArrayList<>(1);
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase(wanted)) {
                found.add(key);
            }
        }
        return found;
    }

    private static String stringOf(ConfigurationSection section, List<String> aliases) {
        for (String alias : aliases) {
            for (String key : keys(section, alias)) {
                String value = rawString(section, key);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private static String rawString(ConfigurationSection section, String key) {
        for (String actual : keys(section, key)) {
            Object value = section.get(actual);
            if (value != null && !(value instanceof ConfigurationSection) && !(value instanceof List)) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    private static int intOf(ConfigurationSection section, List<String> aliases, int fallback) {
        for (String alias : aliases) {
            for (String key : keys(section, alias)) {
                Object value = section.get(key);
                int parsed = (int) toDouble(value, fallback);
                if (parsed != fallback) {
                    return parsed;
                }
            }
        }
        return fallback;
    }

    private static Double doubleOf(ConfigurationSection section, List<String> aliases) {
        for (String alias : aliases) {
            for (String key : keys(section, alias)) {
                Object value = section.get(key);
                if (value instanceof Number || value instanceof String) {
                    double parsed = toDouble(value, Double.NaN);
                    if (!Double.isNaN(parsed)) {
                        return parsed;
                    }
                }
            }
        }
        return null;
    }

    private static double toDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            String cleaned = text.trim().replace(",", "");
            while (cleaned.startsWith("*")) {
                cleaned = cleaned.substring(1);
            }
            if (cleaned.startsWith("$")) {
                cleaned = cleaned.substring(1);
            }
            if (cleaned.equalsIgnoreCase("free")) {
                return 0.0;
            }
            int space = cleaned.indexOf(' ');
            if (space > 0) {
                cleaned = cleaned.substring(0, space);
            }
            try {
                return Double.parseDouble(cleaned);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static Material parseMaterial(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        int colon = value.indexOf(':');
        if (colon > 0 && value.substring(0, colon).equalsIgnoreCase("minecraft")) {
            value = value.substring(colon + 1);
        }
        // "DIAMOND:4" style entries carry the amount after a colon.
        int extra = value.indexOf(':');
        if (extra > 0) {
            value = value.substring(0, extra);
        }
        value = value.replace(' ', '_').toUpperCase(Locale.ROOT);
        Material material = Material.matchMaterial(value);
        return material == null || material.isAir() ? null : material;
    }

    static Enchantment parseEnchantment(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.contains(":")) {
            value = value.substring(value.indexOf(':') + 1);
        }
        try {
            return RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .getOrThrow(Key.key(Key.MINECRAFT_NAMESPACE, value));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String searchText(ItemStack stack) {
        StringBuilder text = new StringBuilder(stack.getType().name().toLowerCase(Locale.ROOT).replace('_', ' '));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.displayName() != null) {
            text.append(' ').append(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(meta.displayName()).toLowerCase(Locale.ROOT));
        }
        return text.toString();
    }

    private static net.kyori.adventure.text.Component colorize(String raw) {
        return io.sniperjohnny.github.better_admin_commands.util.Msg.component(raw);
    }

    private static String prettify(String raw) {
        String[] parts = raw.replace('-', ' ').replace('_', ' ').split("\\s+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    private static String relative(File folder, File file) {
        return folder.toURI().relativize(file.toURI()).getPath();
    }
}
