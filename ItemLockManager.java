package com.hexmod.module;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Lista zablokowanych typow przedmiotow (config/hexmod/itemlock.json), np. minecraft:ender_pearl. */
public final class ItemLockManager {
    private static final Set<String> IDS = new LinkedHashSet<>();
    private static final Gson GSON = new Gson();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hexmod").resolve("itemlock.json");

    private ItemLockManager() {}

    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            List<String> list = GSON.fromJson(Files.readString(FILE), new TypeToken<List<String>>() {}.getType());
            IDS.clear();
            if (list != null) IDS.addAll(list);
        } catch (IOException | RuntimeException ignored) {}
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(new ArrayList<>(IDS)));
        } catch (IOException ignored) {}
    }

    public static String idOf(ItemStack s) {
        return Registries.ITEM.getId(s.getItem()).toString();
    }

    public static boolean has(ItemStack s) {
        return s != null && !s.isEmpty() && IDS.contains(idOf(s));
    }

    /** Przelacza blokade typu przedmiotu. Zwraca true, gdy jest teraz zablokowany. */
    public static boolean toggleId(String id) {
        boolean now = !IDS.remove(id);
        if (now) IDS.add(id);
        save();
        return now;
    }

    /** Zamienia "ender_pearl" albo "minecraft:ender_pearl" na poprawne id albo null, gdy takiego przedmiotu nie ma. */
    public static String resolve(String name) {
        if (name == null) return null;
        String n = name.trim().toLowerCase();
        if (n.isEmpty()) return null;
        Identifier id = Identifier.tryParse(n.contains(":") ? n : "minecraft:" + n);
        if (id == null || !Registries.ITEM.containsId(id)) return null;
        return id.toString();
    }

    public static List<String> list() {
        return new ArrayList<>(IDS);
    }

    public static void clear() {
        IDS.clear();
        save();
    }
}
