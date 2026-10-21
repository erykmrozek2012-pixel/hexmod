package com.hexmod.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class ModuleManager {
    /** Zapisany stan jednego modulu (to samo trafia do configow). */
    public static class State {
        public boolean enabled;
        public int x = 10;
        public int y = 10;
        public float scale = 1f;
        public int color = 0xFFFFFF;
        public boolean hidden;
        public int bindKey = -1;
        public boolean bindHold;
        public Map<String, Boolean> options = new LinkedHashMap<>();
    }

    private static final List<Module> MODULES = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STATE_MAP = new TypeToken<Map<String, State>>() {}.getType();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hexmod.json");
    private static final Path CONFIGS = FabricLoader.getInstance().getConfigDir().resolve("hexmod").resolve("configs");

    private ModuleManager() {}

    public static void init() {
        register(new FpsModule());
        register(new PingModule());
        register(new CoordsModule());
        register(new SaturationModule());
        register(new EatTimerModule());
        register(new CooldownModule());
        register(new TotemModule());
        register(new EnemyTagsModule());
        register(new ReachCircleModule());
        register(new DamageDealtModule());
        register(new PearlTimerModule());
        register(new CpsModule());
        register(new ArmorModule());
        register(new EffectsModule());
        register(new EnemyShieldModule());
        register(new ShieldBreakModule());
        register(new AlliesModule());
        register(new SpotifyModule());
        register(new ItemLockModule());
        register(new WaypointsModule());
        register(new ComboModule());
        register(new ReachModule());
        register(new SpeedModule());
        register(new DirectionModule());
        register(new ClockModule());
        register(new MemoryModule());
    }

    public static void register(Module module) {
        MODULES.add(module);
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(MODULES);
    }

    public static void disableAll() {
        for (Module m : MODULES) m.setEnabled(false);
    }

    // ---- zapis i odczyt stanu ----

    private static Map<String, State> snapshot() {
        Map<String, State> map = new LinkedHashMap<>();
        for (Module m : MODULES) {
            State s = new State();
            s.enabled = m.isEnabled();
            s.x = m.x;
            s.y = m.y;
            s.scale = m.scale;
            s.color = m.color;
            s.hidden = m.hidden;
            s.bindKey = m.bindKey;
            s.bindHold = m.bindHold;
            s.options = new LinkedHashMap<>(m.options);
            map.put(m.id, s);
        }
        return map;
    }

    private static void apply(Map<String, State> map) {
        for (Module m : MODULES) {
            State s = map.get(m.id);
            if (s == null) continue;
            m.setEnabledSilently(s.enabled);
            m.x = s.x;
            m.y = s.y;
            m.scale = Math.max(0.5f, Math.min(3f, s.scale));
            m.color = s.color & 0xFFFFFF;
            m.hidden = s.hidden;
            m.bindKey = s.bindKey;
            m.bindHold = s.bindHold;
            if (s.options != null) {
                for (Map.Entry<String, Boolean> e : s.options.entrySet()) {
                    if (m.options.containsKey(e.getKey()) && e.getValue() != null) {
                        m.options.put(e.getKey(), e.getValue());
                    }
                }
            }
        }
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            Map<String, State> map = GSON.fromJson(Files.readString(FILE), STATE_MAP);
            if (map != null) apply(map);
        } catch (IOException | RuntimeException e) {
            System.err.println("[Hexmod] Nie udalo sie wczytac configu: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(snapshot()));
        } catch (IOException e) {
            System.err.println("[Hexmod] Nie udalo sie zapisac configu: " + e.getMessage());
        }
    }

    // ---- configi uzytkownika ----

    public static String exportJson() {
        return GSON.toJson(snapshot());
    }

    public static boolean importJson(String json) {
        try {
            Map<String, State> map = GSON.fromJson(json, STATE_MAP);
            if (map == null || map.isEmpty()) return false;
            apply(map);
            save();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String clean(String name) {
        return name == null ? "" : name.replaceAll("[^A-Za-z0-9_-]", "");
    }

    public static List<String> listConfigs() {
        List<String> names = new ArrayList<>();
        try {
            Files.createDirectories(CONFIGS);
            try (Stream<Path> s = Files.list(CONFIGS)) {
                s.map(p -> p.getFileName().toString())
                        .filter(n -> n.endsWith(".json"))
                        .forEach(n -> names.add(n.substring(0, n.length() - 5)));
            }
        } catch (IOException ignored) {}
        Collections.sort(names);
        return names;
    }

    public static boolean saveConfig(String name) {
        String n = clean(name);
        if (n.isEmpty()) return false;
        try {
            Files.createDirectories(CONFIGS);
            Files.writeString(CONFIGS.resolve(n + ".json"), exportJson());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean loadConfig(String name) {
        try {
            return importJson(Files.readString(CONFIGS.resolve(clean(name) + ".json")));
        } catch (IOException e) {
            return false;
        }
    }

    public static void deleteConfig(String name) {
        try {
            Files.deleteIfExists(CONFIGS.resolve(clean(name) + ".json"));
        } catch (IOException ignored) {}
    }
}
