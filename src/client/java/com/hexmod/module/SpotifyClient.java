package com.hexmod.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Logowanie do Spotify (PKCE, bez serwera) i odczyt aktualnie granego utworu + tekst z LRCLIB. */
public final class SpotifyClient {
    public static final String REDIRECT = "http://127.0.0.1:8888/callback";
    private static final String SCOPE = "user-read-currently-playing user-read-playback-state";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8)).build();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hexmod").resolve("spotify.json");
    private static final Pattern LRC = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)]\\s*(.*)");

    private static class Cfg {
        String clientId = "";
        String refreshToken = "";
        String accessToken = "";
        long expiresAt = 0;
        String verifier = "";
    }

    public static class Track {
        public String key = "";
        public String title = "";
        public String artist = "";
        public long durationMs;
        public volatile long progressMs;
        public volatile boolean playing;
        public volatile long sampledAt;
        public volatile TreeMap<Long, String> lyrics = new TreeMap<>();
    }

    private static Cfg cfg = new Cfg();
    private static boolean loaded = false;
    private static volatile Track track = null;
    private static final AtomicBoolean polling = new AtomicBoolean(false);
    public static volatile String status = "";

    private SpotifyClient() {}

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            if (Files.exists(FILE)) {
                Cfg c = GSON.fromJson(Files.readString(FILE), Cfg.class);
                if (c != null) cfg = c;
            }
        } catch (IOException | RuntimeException ignored) {}
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(cfg));
        } catch (IOException ignored) {}
    }

    public static String clientId() {
        ensureLoaded();
        return cfg.clientId;
    }

    public static void setClientId(String id) {
        ensureLoaded();
        cfg.clientId = id == null ? "" : id.trim();
        save();
    }

    public static boolean isLoggedIn() {
        ensureLoaded();
        return !cfg.refreshToken.isEmpty();
    }

    public static void logout() {
        ensureLoaded();
        cfg.refreshToken = "";
        cfg.accessToken = "";
        cfg.expiresAt = 0;
        track = null;
        save();
        status = "Wylogowano.";
    }

    public static Track current() {
        return track;
    }

    public static long progressNow(Track t) {
        long p = t.progressMs;
        if (t.playing) p += System.currentTimeMillis() - t.sampledAt;
        return Math.max(0, Math.min(p, t.durationMs));
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String b64url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /** Tworzy link logowania (zapisuje klucz PKCE). Zwraca null, gdy brak Client ID. */
    public static String loginUrl() {
        ensureLoaded();
        if (cfg.clientId.isEmpty()) return null;
        try {
            byte[] rnd = new byte[48];
            new SecureRandom().nextBytes(rnd);
            cfg.verifier = b64url(rnd);
            save();
            String challenge = b64url(MessageDigest.getInstance("SHA-256")
                    .digest(cfg.verifier.getBytes(StandardCharsets.US_ASCII)));
            return "https://accounts.spotify.com/authorize?client_id=" + enc(cfg.clientId)
                    + "&response_type=code&redirect_uri=" + enc(REDIRECT)
                    + "&code_challenge_method=S256&code_challenge=" + challenge
                    + "&scope=" + enc(SCOPE);
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonObject postForm(Map<String, String> form) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : form.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(enc(e.getKey())).append('=').append(enc(e.getValue()));
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(sb.toString()))
                .build();
        try {
            HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() / 100 != 2) throw new IOException("HTTP " + r.statusCode());
            return JsonParser.parseString(r.body()).getAsJsonObject();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    private static void storeTokens(JsonObject o) {
        cfg.accessToken = o.get("access_token").getAsString();
        if (o.has("refresh_token") && !o.get("refresh_token").isJsonNull()) {
            cfg.refreshToken = o.get("refresh_token").getAsString();
        }
        long sec = o.has("expires_in") ? o.get("expires_in").getAsLong() : 3600;
        cfg.expiresAt = System.currentTimeMillis() + sec * 1000 - 30_000;
        save();
    }

    /** Konczy logowanie po wklejeniu adresu, na ktory przekierowala przegladarka. */
    public static void completeLogin(String pasted) {
        ensureLoaded();
        String code = null;
        String text = pasted == null ? "" : pasted.trim();
        int q = text.indexOf('?');
        if (q >= 0) {
            for (String part : text.substring(q + 1).split("&")) {
                if (part.startsWith("code=")) code = part.substring(5);
                if (part.startsWith("error=")) { status = "Spotify odrzucil logowanie."; return; }
            }
        }
        if (code == null || code.isEmpty()) {
            status = "W schowku nie ma adresu z kodem. Skopiuj caly adres z przegladarki.";
            return;
        }
        final String c = code;
        status = "Loguje...";
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, String> f = new LinkedHashMap<>();
                f.put("grant_type", "authorization_code");
                f.put("code", c);
                f.put("redirect_uri", REDIRECT);
                f.put("client_id", cfg.clientId);
                f.put("code_verifier", cfg.verifier);
                storeTokens(postForm(f));
                status = "Zalogowano do Spotify.";
            } catch (IOException | RuntimeException e) {
                status = "Blad logowania: " + e.getMessage() + ". Sprawdz Client ID i Redirect URI.";
            }
        });
    }

    private static String token() throws IOException {
        if (cfg.refreshToken.isEmpty()) return null;
        if (cfg.accessToken.isEmpty() || System.currentTimeMillis() >= cfg.expiresAt) {
            Map<String, String> f = new LinkedHashMap<>();
            f.put("grant_type", "refresh_token");
            f.put("refresh_token", cfg.refreshToken);
            f.put("client_id", cfg.clientId);
            storeTokens(postForm(f));
        }
        return cfg.accessToken;
    }

    /** Odpytuje Spotify w tle (wywoluj co kilka sekund). */
    public static void pollAsync() {
        ensureLoaded();
        if (!isLoggedIn() || !polling.compareAndSet(false, true)) return;
        CompletableFuture.runAsync(() -> {
            try {
                String tok = token();
                if (tok == null) return;
                HttpRequest req = HttpRequest.newBuilder(
                                URI.create("https://api.spotify.com/v1/me/player/currently-playing"))
                        .header("Authorization", "Bearer " + tok)
                        .timeout(Duration.ofSeconds(10)).GET().build();
                HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 204) { track = null; return; }
                if (r.statusCode() == 401) { cfg.accessToken = ""; return; }
                if (r.statusCode() / 100 != 2) { status = "Spotify: HTTP " + r.statusCode(); return; }
                JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
                JsonElement item = o.get("item");
                if (item == null || item.isJsonNull()) { track = null; return; }
                JsonObject it = item.getAsJsonObject();
                String title = it.get("name").getAsString();
                String artist = it.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
                long dur = it.get("duration_ms").getAsLong();
                long prog = o.has("progress_ms") && !o.get("progress_ms").isJsonNull() ? o.get("progress_ms").getAsLong() : 0;
                boolean playing = o.has("is_playing") && o.get("is_playing").getAsBoolean();
                String key = artist + "|" + title + "|" + dur;

                Track cur = track;
                if (cur == null || !cur.key.equals(key)) {
                    Track t = new Track();
                    t.key = key;
                    t.title = title;
                    t.artist = artist;
                    t.durationMs = dur;
                    t.progressMs = prog;
                    t.playing = playing;
                    t.sampledAt = System.currentTimeMillis();
                    track = t;
                    fetchLyrics(t);
                } else {
                    cur.progressMs = prog;
                    cur.playing = playing;
                    cur.sampledAt = System.currentTimeMillis();
                }
            } catch (IOException | RuntimeException e) {
                status = "Spotify: " + e.getMessage();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                polling.set(false);
            }
        });
    }

    private static void fetchLyrics(Track t) {
        CompletableFuture.runAsync(() -> {
            try {
                String url = "https://lrclib.net/api/get?track_name=" + enc(t.title)
                        + "&artist_name=" + enc(t.artist) + "&duration=" + (t.durationMs / 1000);
                JsonObject o = JsonParser.parseString(Web.get(url)).getAsJsonObject();
                if (!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) return;
                TreeMap<Long, String> map = new TreeMap<>();
                for (String line : o.get("syncedLyrics").getAsString().split("\n")) {
                    Matcher m = LRC.matcher(line.trim());
                    if (!m.matches()) continue;
                    long ms = (long) ((Integer.parseInt(m.group(1)) * 60 + Double.parseDouble(m.group(2))) * 1000);
                    map.put(ms, m.group(3));
                }
                t.lyrics = map;
            } catch (IOException | RuntimeException ignored) {}
        });
    }
}
