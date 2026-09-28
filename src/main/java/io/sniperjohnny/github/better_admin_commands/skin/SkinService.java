package io.sniperjohnny.github.better_admin_commands.skin;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.sniperjohnny.github.better_admin_commands.Better_Admin_Commands;
import io.sniperjohnny.github.better_admin_commands.player.PlayerPreferences;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SkinService {

    public record Skin(String source, String value, String signature) {

        public String pack() {
            return value + "|" + signature + "|" + source;
        }

        public static Skin unpack(String raw) {
            if (raw == null) {
                return null;
            }
            String[] parts = raw.split("\\|", 3);
            if (parts.length < 3 || parts[0].isBlank() || parts[1].isBlank()) {
                return null;
            }
            return new Skin(parts[2], parts[0], parts[1]);
        }
    }

    private record Cached(Skin skin, long expiresAt) {
        boolean valid() {
            return System.currentTimeMillis() < expiresAt;
        }
    }

    private static final String[] NAME_URLS = {
            "https://api.minecraftservices.com/minecraft/profile/lookup/name/",
            "https://api.mojang.com/users/profiles/minecraft/"
    };
    private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private static final String SIGNED = "?unsigned=false";
    private static final String TEXTURES = "textures";

    private final Better_Admin_Commands plugin;
    private final HttpClient http;
    private final Duration timeout;
    private final long cacheMillis;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Skin>> pending = new ConcurrentHashMap<>();

    public SkinService(Better_Admin_Commands plugin) {
        this.plugin = plugin;
        this.timeout = Duration.ofSeconds(Math.max(3, plugin.getConfig().getInt("skin.timeout-seconds", 10)));
        this.cacheMillis = Math.max(1, plugin.getConfig().getLong("skin.cache-minutes", 60L)) * 60_000L;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public CompletableFuture<Skin> byUsername(String username) {
        String name = username == null ? "" : username.trim();
        return lookup("name:" + name.toLowerCase(java.util.Locale.ROOT), () ->
                uuidOf(name).thenCompose(uuid -> uuid == null
                        ? CompletableFuture.completedFuture(null)
                        : profileOf(uuid)));
    }

    public CompletableFuture<Skin> byUuid(UUID uuid) {
        return lookup("uuid:" + uuid, () -> profileOf(uuid.toString().replace("-", "")));
    }

    public Skin byTexture(String value, String signature, String source) {
        if (value == null || signature == null || value.isBlank() || signature.isBlank()) {
            return null;
        }
        String label = source == null || source.isBlank() ? "the pasted texture" : source.trim();
        Skin skin = new Skin(label, value.trim(), signature.trim());
        cache.put("texture:" + skin.value().hashCode(), new Cached(skin, System.currentTimeMillis() + cacheMillis));
        return skin;
    }

    private CompletableFuture<Skin> lookup(String key, Supplier<CompletableFuture<Skin>> source) {
        Cached cached = cache.get(key);
        if (cached != null) {
            if (cached.valid()) {
                return CompletableFuture.completedFuture(cached.skin());
            }
            cache.remove(key);
        }
        CompletableFuture<Skin> result = new CompletableFuture<>();
        CompletableFuture<Skin> running = pending.putIfAbsent(key, result);
        if (running != null) {
            return running;
        }
        source.get().whenComplete((skin, error) -> {
            pending.remove(key, result);
            if (error != null) {
                result.completeExceptionally(error);
                return;
            }
            if (skin != null) {
                cache.put(key, new Cached(skin, System.currentTimeMillis() + cacheMillis));
            }
            result.complete(skin);
        });
        return result;
    }

    private CompletableFuture<String> uuidOf(String username) {
        return uuidOf(username, 0);
    }

    private CompletableFuture<String> uuidOf(String username, int index) {
        return resolveName(NAME_URLS[index], username).handle((uuid, error) -> {
            if (error == null) {
                return CompletableFuture.completedFuture(uuid);
            }
            if (index + 1 < NAME_URLS.length) {
                return uuidOf(username, index + 1);
            }
            return CompletableFuture.<String>failedFuture(cause(error));
        }).thenCompose(future -> future);
    }

    private CompletableFuture<String> resolveName(String baseUrl, String username) {
        HttpRequest request = request(baseUrl + URLEncoder.encode(username, StandardCharsets.UTF_8));
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            int status = response.statusCode();
            if (status == 204 || status == 404) {
                return null; // nobody uses that name
            }
            if (status != 200) {
                throw new CompletionException(new IOException(
                        statusMessage("looking up '" + username + "'", status)));
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonElement id = json.get("id");
            return id == null || id.isJsonNull() ? null : id.getAsString();
        });
    }

    private static Throwable cause(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null
                ? error.getCause() : error;
    }

    private CompletableFuture<Skin> profileOf(String uuid) {
        HttpRequest request = request(PROFILE_URL + uuid + SIGNED);
        return http.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            int status = response.statusCode();
            if (status == 204 || status == 404) {
                throw new CompletionException(new IOException(
                        "That account exists, but Mojang has no profile for it."));
            }
            if (status != 200) {
                throw new CompletionException(new IOException(
                        statusMessage("reading the profile", status)));
            }
            Skin skin = readTextures(response.body());
            if (skin == null) {
                // The account exists but Mojang sent no signed textures: it never
                // set a skin, or only an unsigned one came back. Say so plainly
                // instead of pretending there is a skin we cannot use.
                throw new CompletionException(new IOException(
                        "That account exists, but Mojang has no signed skin for it - either the "
                                + "account never set a custom skin, or the texture came back unsigned. "
                                + "Try another name, or paste a texture with /skinchange value <value> "
                                + "<signature>."));
            }
            return skin;
        });
    }

    private static String statusMessage(String what, int status) {
        String extra = switch (status) {
            case 429 -> " (Mojang is rate limiting this server, try again in a minute)";
            case 403 -> " (the request was refused - does this server allow outbound HTTPS?)";
            case 400 -> " (the name or address was rejected)";
            default -> "";
        };
        return "Mojang answered HTTP " + status + " while " + what + extra + ".";
    }

    private static Skin readTextures(String body) {
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        JsonElement properties = json.get("properties");
        if (properties == null || !properties.isJsonArray()) {
            return null;
        }
        String name = json.has("name") ? json.get("name").getAsString() : "?";
        for (JsonElement element : properties.getAsJsonArray()) {
            JsonObject property = element.getAsJsonObject();
            JsonElement key = property.get("name");
            JsonElement value = property.get("value");
            JsonElement signature = property.get("signature");
            if (key == null || value == null || key.isJsonNull() || value.isJsonNull()) {
                continue;
            }
            if (!TEXTURES.equals(key.getAsString())) {
                continue;
            }
            // Without the signature the client refuses to show the skin.
            if (signature == null || signature.isJsonNull()) {
                return null;
            }
            return new Skin(name, value.getAsString(), signature.getAsString());
        }
        return null;
    }

    private HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", "Better_Admin_Commands/" + plugin.getDescription().getVersion())
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    public void apply(Player player, Skin skin) {
        PlayerProfile profile = player.getPlayerProfile().clone();
        profile.setProperty(new ProfileProperty(TEXTURES, skin.value(), skin.signature()));
        player.setPlayerProfile(profile);
    }

    public void clear(Player player) {
        PlayerProfile profile = player.getPlayerProfile().clone();
        profile.removeProperty(TEXTURES);
        player.setPlayerProfile(profile);
    }

    public void remember(Player player, Skin skin) {
        plugin.preferences().set(player.getUniqueId(), PlayerPreferences.SKIN, skin.pack());
    }

    public void forget(UUID uuid) {
        plugin.preferences().set(uuid, PlayerPreferences.SKIN, null);
    }

    public void applyStored(Player player) {
        Skin skin = Skin.unpack(plugin.preferences().get(player.getUniqueId(), PlayerPreferences.SKIN, null));
        if (skin != null) {
            apply(player, skin);
        }
    }
}
