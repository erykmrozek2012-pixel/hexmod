package com.hexmod.module;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Lista sojusznikow (przyjaciol), zapisywana w config/hexmod/allies.json. */
public final class AlliesManager {
    private static final List<String> NAMES = new ArrayList<>();
    private static final Gson GSON = new Gson();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hexmod").resolve("allies.json");

    private AlliesManager() {}

    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            List<String> list = GSON.fromJson(Files.readString(FILE), new TypeToken<List<String>>() {}.getType());
            NAMES.clear();
            if (list != null) NAMES.addAll(list);
        } catch (IOException | RuntimeException ignored) {}
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(NAMES));
        } catch (IOException ignored) {}
    }

    public static List<String> list() {
        return List.copyOf(NAMES);
    }

    public static boolean isAlly(String name) {
        for (String n : NAMES) if (n.equalsIgnoreCase(name)) return true;
        return false;
    }

    public static boolean add(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty() || isAlly(n)) return false;
        NAMES.add(n);
        save();
        return true;
    }

    public static void remove(String name) {
        NAMES.removeIf(n -> n.equalsIgnoreCase(name));
        save();
    }
}
