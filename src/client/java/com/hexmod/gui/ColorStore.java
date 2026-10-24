package com.hexmod.gui;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Wlasne kolory widgetow zapisane w config/hexmod/hudcolors.txt (do 7, najnowszy pierwszy). */
final class ColorStore {
    static final int MAX = 7;
    private static final List<Integer> LIST = new ArrayList<>();
    private static boolean loaded = false;

    private ColorStore() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("hexmod").resolve("hudcolors.txt");
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        try {
            if (!Files.exists(file())) return;
            for (String line : Files.readAllLines(file())) {
                String t = line.trim().replace("#", "");
                if (t.length() == 6 && LIST.size() < MAX) LIST.add(Integer.parseInt(t, 16));
            }
        } catch (IOException | RuntimeException ignored) {}
    }

    static List<Integer> list() {
        load();
        return LIST;
    }

    static void add(int rgb) {
        load();
        rgb &= 0xFFFFFF;
        LIST.remove(Integer.valueOf(rgb));
        LIST.add(0, rgb);
        while (LIST.size() > MAX) LIST.remove(LIST.size() - 1);
        try {
            Files.createDirectories(file().getParent());
            List<String> lines = new ArrayList<>();
            for (int c : LIST) lines.add(String.format("%06X", c));
            Files.write(file(), lines);
        } catch (IOException ignored) {}
    }
}
