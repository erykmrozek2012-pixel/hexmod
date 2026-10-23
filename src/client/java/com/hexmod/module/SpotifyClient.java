package com.hexmod.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
import java.util.concurrent.TimeUnit;
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
    /** Co wykryto w lokalnej aplikacji Spotify (bez logowania). */
    public static volatile String localInfo = "";
    private static volatile Track localTrack = null;
    private static final AtomicBoolean localBusy = new AtomicBoolean(false);
    private static volatile String lastUrl = "";
    private static final AtomicBoolean autoRunning = new AtomicBoolean(false);

    private SpotifyClient() {}

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            if (Files.exists(FILE)) {
                Cfg c = GSON.fromJson(Files.readString(FILE), Cfg.class);
                if (c != null) cfg = c;
                if (!cfg.refreshToken.isEmpty()) {
                    // Stare logowanie przez API powodowalo bledy HTTP: kasujemy je, mod czyta aplikacje lokalnie.
                    cfg.refreshToken = "";
                    cfg.accessToken = "";
                    cfg.expiresAt = 0;
                    save();
                }
            }
        } catch (IOException | RuntimeException ignored) {}
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(cfg));
        } catch (IOException ignored) {}
    }

    /** Client ID wpisany recznie (zaawansowane). */
    public static String clientId() {
        ensureLoaded();
        return cfg.clientId;
    }

    /** Client ID, ktorego mod faktycznie uzywa: wlasny albo ustawiony przez admina. */
    public static String effectiveClientId() {
        ensureLoaded();
        return cfg.clientId.isEmpty() ? Remote.spotifyClientId() : cfg.clientId;
    }

    public static void setClientId(String id) {
        ensureLoaded();
        cfg.clientId = id == null ? "" : id.trim();
        save();
    }

    /** Logowanie do API Spotify jest wylaczone: mod zawsze czyta lokalnie aplikacje Spotify (bez logowania). */
    public static boolean isLoggedIn() {
        ensureLoaded();
        return false;
    }

    public static void logout() {
        ensureLoaded();
        cfg.refreshToken = "";
        cfg.accessToken = "";
        cfg.expiresAt = 0;
        track = null;
        save();
        status = L.t("Logged out.");
    }

    public static Track current() {
        return isLoggedIn() ? track : localTrack;
    }

    public static long progressNow(Track t) {
        long p = t.progressMs;
        if (t.playing) p += System.currentTimeMillis() - t.sampledAt;
        return t.durationMs > 0 ? Math.max(0, Math.min(p, t.durationMs)) : Math.max(0, p);
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
        if (effectiveClientId().isEmpty()) return null;
        try {
            byte[] rnd = new byte[48];
            new SecureRandom().nextBytes(rnd);
            cfg.verifier = b64url(rnd);
            save();
            String challenge = b64url(MessageDigest.getInstance("SHA-256")
                    .digest(cfg.verifier.getBytes(StandardCharsets.US_ASCII)));
            lastUrl = "https://accounts.spotify.com/authorize?client_id=" + enc(effectiveClientId())
                    + "&response_type=code&redirect_uri=" + enc(REDIRECT)
                    + "&code_challenge_method=S256&code_challenge=" + challenge
                    + "&scope=" + enc(SCOPE);
            return lastUrl;
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

    private static void exchangeCode(String code) throws IOException {
        Map<String, String> f = new LinkedHashMap<>();
        f.put("grant_type", "authorization_code");
        f.put("code", code);
        f.put("redirect_uri", REDIRECT);
        f.put("client_id", effectiveClientId());
        f.put("code_verifier", cfg.verifier);
        storeTokens(postForm(f));
        lastUrl = "";
    }

    /** Link logowania z biezacej proby (nie zmienia klucza PKCE trwajacego logowania). */
    public static String currentLoginUrl() {
        return lastUrl.isEmpty() ? loginUrl() : lastUrl;
    }

    /** Logowanie reczne: wklejony adres, na ktory przekierowala przegladarka. */
    public static void completeLogin(String pasted) {
        ensureLoaded();
        String code = null;
        String text = pasted == null ? "" : pasted.trim();
        int q = text.indexOf('?');
        if (q >= 0) {
            for (String part : text.substring(q + 1).split("&")) {
                if (part.startsWith("code=")) code = part.substring(5);
                if (part.startsWith("error=")) { status = L.t("Spotify rejected the login."); return; }
            }
        }
        if (code == null || code.isEmpty()) {
            status = L.t("The clipboard has no address with a code. Copy the whole address from the browser.");
            return;
        }
        final String c = code;
        status = L.t("Logging in...");
        CompletableFuture.runAsync(() -> {
            try {
                exchangeCode(c);
                status = L.t("Logged in to Spotify.");
            } catch (IOException | RuntimeException e) {
                status = L.t("Login error: ") + e.getMessage() + L.t(". Check the Client ID and Redirect URI.");
            }
        });
    }

    private static void reply(Socket s, int code, String html) throws IOException {
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        OutputStream out = s.getOutputStream();
        out.write(("HTTP/1.1 " + code + " OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "
                + body.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }

    private static String page(String title, String text) {
        return "<!doctype html><meta charset=utf-8><title>Hexmod</title>"
                + "<body style=\"background:#0d0d0d;color:#fff;font-family:sans-serif;text-align:center;padding-top:15vh\">"
                + "<h1 style=\"color:#d10a0a\">" + title + "</h1><p>" + text + "</p></body>";
    }

    /** Logowanie jednym klikniem: otwiera przegladarke i sam odbiera odpowiedz Spotify. */
    public static void startAutoLogin() {
        ensureLoaded();
        String url = loginUrl();
        if (url == null) {
            status = L.t("No Client ID. Ask the admin to set it (Advanced tab).");
            return;
        }
        if (!autoRunning.compareAndSet(false, true)) {
            status = L.t("Login is already in progress. Finish it in the browser.");
            return;
        }
        status = L.t("Opening the browser... log in and come back to the game.");
        CompletableFuture.runAsync(() -> {
            try (ServerSocket server = new ServerSocket(8888, 5, InetAddress.getByName("127.0.0.1"))) {
                server.setSoTimeout(15_000);
                if (!Browser.open(url)) {
                    status = L.t("Cannot open the browser. Use manual login (Advanced).");
                }
                long deadline = System.currentTimeMillis() + 180_000;
                while (System.currentTimeMillis() < deadline) {
                    try (Socket s = server.accept()) {
                        s.setSoTimeout(5000);
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(s.getInputStream(), StandardCharsets.US_ASCII));
                        String line = in.readLine();
                        String[] parts = line == null ? new String[0] : line.split(" ");
                        String path = parts.length > 1 ? parts[1] : "";
                        if (!path.startsWith("/callback")) {
                            reply(s, 404, "");
                            continue;
                        }
                        String code = null;
                        boolean denied = false;
                        int q = path.indexOf('?');
                        if (q >= 0) {
                            for (String part : path.substring(q + 1).split("&")) {
                                if (part.startsWith("code=")) code = part.substring(5);
                                if (part.startsWith("error=")) denied = true;
                            }
                        }
                        if (code == null || denied) {
                            reply(s, 200, page("Login failed", "Spotify rejected the login. Return to Minecraft and try again."));
                            status = L.t("Spotify rejected the login.");
                            return;
                        }
                        reply(s, 200, page("Logged in!", "You can close this tab and return to Minecraft."));
                        status = L.t("Logging in...");
                        try {
                            exchangeCode(code);
                            status = L.t("Logged in to Spotify.");
                        } catch (IOException | RuntimeException e) {
                            status = L.t("Login error: ") + e.getMessage();
                        }
                        return;
                    } catch (SocketTimeoutException ignored) {
                        // brak polaczenia w tej chwili - czekaj dalej do konca limitu czasu
                    }
                }
                status = L.t("The login timed out. Try again.");
            } catch (IOException e) {
                status = L.t("Cannot start the login (port 8888 busy?). Use manual login.");
            } finally {
                autoRunning.set(false);
            }
        });
    }

    private static String token() throws IOException {
        if (cfg.refreshToken.isEmpty()) return null;
        if (cfg.accessToken.isEmpty() || System.currentTimeMillis() >= cfg.expiresAt) {
            Map<String, String> f = new LinkedHashMap<>();
            f.put("grant_type", "refresh_token");
            f.put("refresh_token", cfg.refreshToken);
            f.put("client_id", effectiveClientId());
            storeTokens(postForm(f));
        }
        return cfg.accessToken;
    }

    /** Odpytuje Spotify w tle (wywoluj co kilka sekund). */
    public static void pollAsync() {
        ensureLoaded();
        if (!isLoggedIn()) {
            pollLocalAsync();
            return;
        }
        if (!polling.compareAndSet(false, true)) return;
        CompletableFuture.runAsync(() -> {
            try {
                String tok = token();
                if (tok == null) return;
                HttpRequest req = HttpRequest.newBuilder(
                                URI.create("https://api.spotify.com/v1/me/player/currently-playing"))
                        .header("Authorization", "Bearer " + tok)
                        .timeout(Duration.ofSeconds(10)).GET().build();
                HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 204) { track = null; if (status.startsWith("Spotify")) status = ""; return; }
                if (r.statusCode() == 401) { cfg.accessToken = ""; return; }
                if (r.statusCode() / 100 != 2) { status = "Spotify: " + L.t("Connection error."); return; }
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

                if (status.startsWith("Spotify")) status = "";
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
                JsonObject o = null;
                if (t.durationMs > 0) {
                    String url = "https://lrclib.net/api/get?track_name=" + enc(t.title)
                            + "&artist_name=" + enc(t.artist) + "&duration=" + (t.durationMs / 1000);
                    o = JsonParser.parseString(Web.get(url)).getAsJsonObject();
                } else {
                    String url = "https://lrclib.net/api/search?track_name=" + enc(t.title)
                            + "&artist_name=" + enc(t.artist);
                    JsonArray arr = JsonParser.parseString(Web.get(url)).getAsJsonArray();
                    for (JsonElement e : arr) {
                        JsonObject c = e.getAsJsonObject();
                        if (c.has("syncedLyrics") && !c.get("syncedLyrics").isJsonNull()) { o = c; break; }
                    }
                }
                if (o == null || !o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) return;
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

    // ---------- lokalne wykrywanie utworu (okno aplikacji Spotify, bez logowania) ----------

    private static String run(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            p.waitFor(5, TimeUnit.SECONDS);
            return out.trim();
        } catch (IOException e) {
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }

    /** Zwraca {stan, tytul}: stan 0 = Spotify nie dziala, 1 = pauza, 2 = gra. */
    private static String[] readLocal() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            String out = run("powershell", "-NoProfile", "-NonInteractive", "-Command",
                    "[Console]::OutputEncoding=[Text.Encoding]::UTF8; "
                            + "$p = Get-Process -Name Spotify -ErrorAction SilentlyContinue; "
                            + "if (-not $p) { '' } else { "
                            + "$t = $p | Where-Object { $_.MainWindowTitle } | Select-Object -First 1 -ExpandProperty MainWindowTitle; "
                            + "if ($t) { $t } else { '#hidden' } }");
            if (out.isEmpty()) return new String[]{"0", ""};
            if (out.equals("#hidden")) return new String[]{"3", ""};
            if (out.contains(" - ")) return new String[]{"2", out};
            return new String[]{"1", ""};
        }
        if (os.contains("mac")) {
            String out = run("osascript", "-e",
                    "tell application \"System Events\" to if (name of processes) contains \"Spotify\" then "
                            + "tell application \"Spotify\" to if player state is playing then "
                            + "return \"P|\" & (artist of current track) & \" - \" & (name of current track) "
                            + "else return \"S\"");
            if (out.startsWith("P|")) return new String[]{"2", out.substring(2)};
            if (out.equals("S")) return new String[]{"1", ""};
            return new String[]{"0", ""};
        }
        String status = run("sh", "-c", "playerctl -p spotify status 2>/dev/null");
        if (status.isEmpty()) return new String[]{"0", ""};
        if (!status.equalsIgnoreCase("Playing")) return new String[]{"1", ""};
        String title = run("sh", "-c", "playerctl -p spotify metadata --format '{{artist}} - {{title}}' 2>/dev/null");
        return new String[]{"2", title};
    }

    private static void applyLocal(String[] r) {
        int state = Integer.parseInt(r[0]);
        Track t = localTrack;
        long now = System.currentTimeMillis();
        if (state == 0) {
            localTrack = null;
            localInfo = L.t("Spotify is not running");
            return;
        }
        if (state == 3) {
            localTrack = null;
            localInfo = L.t("Spotify: restore the Spotify window (it is minimized to the tray)");
            return;
        }
        if (state == 1) {
            if (t != null && t.playing) {
                t.progressMs = progressNow(t);
                t.playing = false;
            }
            localInfo = L.t("Spotify: paused");
            return;
        }
        String full = r[1];
        if (t == null || !t.key.equals(full)) {
            int i = full.indexOf(" - ");
            Track nt = new Track();
            nt.key = full;
            nt.artist = i > 0 ? full.substring(0, i) : "";
            nt.title = i > 0 ? full.substring(i + 3) : full;
            nt.durationMs = 0;
            nt.progressMs = 0;
            nt.sampledAt = now;
            nt.playing = true;
            localTrack = nt;
            fetchLyrics(nt);
        } else if (!t.playing) {
            t.sampledAt = now;
            t.playing = true;
        }
        localInfo = L.t("Playing: ") + full;
    }

    private static void pollLocalAsync() {
        if (!localBusy.compareAndSet(false, true)) return;
        CompletableFuture.runAsync(() -> {
            try {
                applyLocal(readLocal());
            } catch (RuntimeException ignored) {
            } finally {
                localBusy.set(false);
            }
        });
    }
}
