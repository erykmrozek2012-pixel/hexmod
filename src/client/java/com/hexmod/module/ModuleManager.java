package com.hexmod.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hexmod.json");

    private ModuleManager() {}

    /** Rejestracja modulow. Na razie same wpisy, logika dojdzie w kolejnych krokach. */
    public static void init() {
        register(new Module("cooldowns", "Cooldowny", "Czasy odnowienia wybranych przedmiotow"));
        register(new Module("saturation", "Saturacja", "Poziom i szacowany czas saturacji w walce"));
        register(new Module("eat_timer", "Czas jedzenia", "Odliczanie jedzenia z ulamkami sekundy"));
        register(new Module("enemy_shield", "Tarcza wroga", "Czy wrog ma uniesiona tarcze"));
        register(new Module("shield_break", "Tarcza break", "Komunikat po zlamaniu tarczy toporem"));
        register(new Module("allies", "Sojusznicy", "Lista przyjaciol z ikona przy nicku"));
        register(new Module("spotify", "Spotify", "Aktualny utwor i tekst piosenki"));
    }

    public static void register(Module module) {
        MODULES.add(module);
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(MODULES);
    }

    /** Glowny wylacznik: wylacza wszystkie moduly, wlaczasz je pozniej po kolei. */
    public static void disableAll() {
        for (Module m : MODULES) m.setEnabled(false);
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            String json = Files.readString(FILE);
            Map<String, Boolean> states = GSON.fromJson(json, new TypeToken<Map<String, Boolean>>() {}.getType());
            if (states == null) return;
            for (Module m : MODULES) {
                Boolean s = states.get(m.id);
                if (s != null) m.setEnabledSilently(s);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[Hexmod] Nie udalo sie wczytac configu: " + e.getMessage());
        }
    }

    public static void save() {
        Map<String, Boolean> states = new LinkedHashMap<>();
        for (Module m : MODULES) states.put(m.id, m.isEnabled());
        try {
            Files.writeString(FILE, GSON.toJson(states));
        } catch (IOException e) {
            System.err.println("[Hexmod] Nie udalo sie zapisac configu: " + e.getMessage());
        }
    }
}
