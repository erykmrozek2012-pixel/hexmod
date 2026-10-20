package com.hexmod.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Waypointy zapisywane w config/hexmod/waypoints.json (nazwa, pozycja, wymiar, kolor). */
public final class WaypointManager {
    public static class Waypoint {
        public String name = "Punkt";
        public int x, y, z;
        public String dim = "";
        public int color = 0xFFD60A;
    }

    public static final int MAX = 30;
    public static final int[] COLORS = {0xD10A0A, 0xFFD60A, 0x30D158, 0x64D2FF, 0x0A84FF, 0xBF5AF2, 0xFFFFFF};

    private static final List<Waypoint> LIST = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hexmod").resolve("waypoints.json");

    private WaypointManager() {}

    public static List<Waypoint> list() {
        return LIST;
    }

    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            List<Waypoint> loaded = GSON.fromJson(Files.readString(FILE), new TypeToken<List<Waypoint>>() {}.getType());
            LIST.clear();
            if (loaded != null) LIST.addAll(loaded);
        } catch (IOException | RuntimeException ignored) {}
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(LIST));
        } catch (IOException ignored) {}
    }

    public static String currentDim() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return "";
        return mc.world.getRegistryKey().getValue().toString();
    }

    /** Dodaje waypoint w miejscu, w ktorym stoisz. Zwraca false, gdy nie da sie (brak gracza, limit). */
    public static boolean addHere(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || LIST.size() >= MAX) return false;
        Waypoint w = new Waypoint();
        String n = name == null ? "" : name.trim();
        w.name = n.isEmpty() ? "Punkt " + (LIST.size() + 1) : n;
        w.x = mc.player.getBlockX();
        w.y = mc.player.getBlockY();
        w.z = mc.player.getBlockZ();
        w.dim = currentDim();
        w.color = COLORS[LIST.size() % COLORS.length];
        LIST.add(w);
        save();
        return true;
    }

    public static void remove(Waypoint w) {
        LIST.remove(w);
        save();
    }

    public static void cycleColor(Waypoint w) {
        int idx = 0;
        for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == w.color) idx = i;
        w.color = COLORS[(idx + 1) % COLORS.length];
        save();
    }

    public static boolean removeByName(String name) {
        for (Waypoint w : LIST) {
            if (w.name.equalsIgnoreCase(name)) {
                remove(w);
                return true;
            }
        }
        return false;
    }
}
