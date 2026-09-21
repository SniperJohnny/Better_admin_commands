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

/**
 * Reads player skins from the Mojang API and puts them on a player.
 *
 * <p>Mojang's own API is the source because it hands out skins with a signature
 * the Minecraft client accepts. NameMC, which most people look names up on, is
 * only a viewer for exactly this data and offers no API of its own.</p>
 *
 * <p>The lookup happens asynchronously and is cached, so a name requested twice
 * only hits Mojang once. The chosen skin is stored in the player settings and
 * applied again whenever the player joins.</p>
 */
public class SkinService {

    /** A skin exactly as Mojang signed it. */
    public record Skin(String source, String value, String signature) {

        /** Packs the skin into the single string kept in the player settings. */
        public String pack() {
            return value + "|" + signature + "|" + source;
        }

        /** Unpacks what {@link #pack()} produced, or returns {@code null}. */
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

    /**
     * Name to UUID endpoints, tried in order. The services endpoint is the one
     * Mojang points developers at now; the older api.mojang.com host is kept as
     * a fallback because it has had outages and blocks.
     */
    private static final String[] NAME_URLS = {
            "https://api.minecraftservices.com/minecraft/profile/lookup/name/",
            "https://api.mojang.com/users/profiles/minecraft/"
    };
    private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
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

    /* ----------------------------------------------------------- lookups --- */

    /**
     * Looks a Minecraft name up at Mojang.
     *
     * @return a future that completes with the skin, with {@code null} when no
     *         premium account uses that name, or exceptionally when Mojang could
     *         not be reached - the exception message names the HTTP status, so a
     *         rate limit or a blocked request is not mistaken for a wrong name
     */
    public CompletableFuture<Skin> byUsername(String username) {
        String name = username == null ? "" : username.trim();
        return lookup("name:" + name.toLowerCase(java.util.Locale.ROOT), () ->
                uuidOf(name).thenCompose(uuid -> uuid == null
                        ? CompletableFuture.completedFuture(null)
                        : profileOf(uuid)));
    }

    /** Looks the skin of an account up by UUID, used to restore a player's own. */
    public CompletableFuture<Skin> byUuid(UUID uuid) {
        return lookup("uuid:" + uuid, () -> profileOf(uuid.toString().replace("-", "")));
    }

    /**
     * Caches results and shares one request between callers asking for the same
     * name at the same time.
     */
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

    /**
     * Resolves a name, moving on to the next endpoint when one of them fails.
     * Only a 204/404 counts as "no such account"; everything else is an error
     * that must not be reported as a wrong name.
     */
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

    /** Unwraps the CompletionException a failed future is wrapped in. */
    private static Throwable cause(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null
                ? error.getCause() : error;
    }

    private CompletableFuture<Skin> profileOf(String uuid) {
        HttpRequest request = request(PROFILE_URL + uuid);
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
                throw new CompletionException(new IOException(
                        "That account exists but has no skin set."));
            }
            return skin;
        });
    }

    /** A message that names what Mojang actually answered. */
    private static String statusMessage(String what, int status) {
        String extra = switch (status) {
            case 429 -> " (Mojang is rate limiting this server, try again in a minute)";
            case 403 -> " (the request was refused - does this server allow outbound HTTPS?)";
            case 400 -> " (the name or address was rejected)";
            default -> "";
        };
        return "Mojang answered HTTP " + status + " while " + what + extra + ".";
    }

    /** Picks the signed textures property out of a profile response. */
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

    /* ---------------------------------------------------------- applying --- */

    /**
     * Puts a skin on a player. Has to run on the server thread.
     *
     * <p>The profile is cloned first: handing the server the very instance it
     * already knows about can be treated as no change at all.</p>
     */
    public void apply(Player player, Skin skin) {
        PlayerProfile profile = player.getPlayerProfile().clone();
        profile.setProperty(new ProfileProperty(TEXTURES, skin.value(), skin.signature()));
        player.setPlayerProfile(profile);
    }

    /** Drops a borrowed skin, leaving the player with whatever the server sends. */
    public void clear(Player player) {
        PlayerProfile profile = player.getPlayerProfile().clone();
        profile.removeProperty(TEXTURES);
        player.setPlayerProfile(profile);
    }

    /* ------------------------------------------------------- persistence --- */

    /** Remembers a skin so it can be applied again on every join. */
    public void remember(Player player, Skin skin) {
        plugin.preferences().set(player.getUniqueId(), PlayerPreferences.SKIN, skin.pack());
    }

    /** Forgets the borrowed skin, so the player joins with their own again. */
    public void forget(UUID uuid) {
        plugin.preferences().set(uuid, PlayerPreferences.SKIN, null);
    }

    /** Re-applies the stored skin, called when a player joins. */
    public void applyStored(Player player) {
        Skin skin = Skin.unpack(plugin.preferences().get(player.getUniqueId(), PlayerPreferences.SKIN, null));
        if (skin != null) {
            apply(player, skin);
        }
    }
}
