package com.hexmod.module;

import com.google.gson.Gson;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Dane zdalne trzymane w publicznym repozytorium GitHub (tylko wlasciciel repo moze je zmieniac):
 * blocklist.json (blokady dostepu) i catalog.json (katalog modow).
 */
public final class Remote {
    public static final String BASE = "https://raw.githubusercontent.com/erykmrozek2012-pixel/hexmod-data/main/";
    public static final String ADMIN_NICK = "er__is";

    public static class BlockedEntry {
        public String uuid = "";
        public String nick = "";
    }

    public static class Blocklist {
        public List<BlockedEntry> blocked = new ArrayList<>();
    }

    public static class CatalogEntry {
        public String id = "";
        public String name = "";
        public String modrinth = "";
    }

    public static class AppConfig {
        public String spotifyClientId = "";
    }

    public static class Catalog {
        public List<CatalogEntry> mods = new ArrayList<>();
    }

    private static final Gson GSON = new Gson();
    private static volatile Blocklist blocklist = new Blocklist();
    private static volatile Catalog catalog = new Catalog();
    private static volatile boolean ready = false;
    private static volatile AppConfig appConfig = new AppConfig();

    private Remote() {}

    public static void refresh() {
        CompletableFuture.runAsync(() -> {
            try {
                Blocklist b = GSON.fromJson(Web.get(BASE + "blocklist.json"), Blocklist.class);
                if (b != null && b.blocked != null) blocklist = b;
            } catch (IOException | RuntimeException ignored) {}
            try {
                Catalog c = GSON.fromJson(Web.get(BASE + "catalog.json"), Catalog.class);
                if (c != null && c.mods != null) catalog = c;
            } catch (IOException | RuntimeException ignored) {}
            try {
                AppConfig a = GSON.fromJson(Web.get(BASE + "config.json"), AppConfig.class);
                if (a != null) appConfig = a;
            } catch (IOException | RuntimeException ignored) {}
            ready = true;
        });
    }

    public static Blocklist blocklist() { return blocklist; }

    public static Catalog catalog() { return catalog; }

    /** Client ID aplikacji Spotify ustawiony przez admina w hexmod-data/config.json. */
    public static String spotifyClientId() {
        String id = appConfig.spotifyClientId;
        return id == null ? "" : id.trim();
    }

    public static String username() {
        var s = MinecraftClient.getInstance().getSession();
        return s == null ? "" : s.getUsername();
    }

    private static String uuid() {
        var s = MinecraftClient.getInstance().getSession();
        if (s == null || s.getUuidOrNull() == null) return "";
        return s.getUuidOrNull().toString().replace("-", "").toLowerCase();
    }

    /** Blokada po UUID lub nicku. Gdy nie da sie pobrac listy, dostep zostaje (brak blokady). */
    public static boolean isBlocked() {
        String name = username();
        String id = uuid();
        for (BlockedEntry e : blocklist.blocked) {
            if (e == null) continue;
            if (!id.isEmpty() && e.uuid != null && e.uuid.replace("-", "").equalsIgnoreCase(id)) return true;
            if (!name.isEmpty() && e.nick != null && e.nick.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    /** Mod jest zablokowany: do pierwszego sprawdzenia listy albo gdy gracz jest na liscie blokad. */
    public static boolean isLocked() {
        return !ready || isBlocked();
    }

    public static boolean isReady() {
        return ready;
    }

    public static boolean isAdmin() {
        return ADMIN_NICK.equals(username());
    }
}
