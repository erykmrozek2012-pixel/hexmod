package com.hexmod.module;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** Ulubione moduly (id modulu) i mody z katalogu ("mod:" + nazwa na Modrinth), zapis w config/hexmod/favorites.txt. */
public final class Favorites {
    private static final Set<String> SET = new LinkedHashSet<>();
    private static boolean loaded = false;

    private Favorites() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("hexmod").resolve("favorites.txt");
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        try {
            if (Files.exists(file())) {
                for (String line : Files.readAllLines(file())) {
                    String t = line.trim();
                    if (!t.isEmpty()) SET.add(t);
                }
            }
        } catch (IOException ignored) {}
    }

    public static boolean has(String key) {
        load();
        return SET.contains(key);
    }

    /** Dodaje albo usuwa z ulubionych. Zwraca true, gdy jest teraz ulubiony. */
    public static boolean toggle(String key) {
        load();
        boolean now = !SET.remove(key);
        if (now) SET.add(key);
        try {
            Files.createDirectories(file().getParent());
            Files.write(file(), SET);
        } catch (IOException ignored) {}
        return now;
    }
}
