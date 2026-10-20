package com.hexmod.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Makra: klawisz wysyla komende (zaczyna sie od /) albo tekst na czacie. */
public final class MacroManager {
    public static class Macro {
        public int key = -1;
        public String text = "";
        transient boolean wasDown;
    }

    public static final int MAX = 6;

    private static final List<Macro> MACROS = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("hexmod").resolve("macros.json");

    private MacroManager() {}

    public static List<Macro> list() {
        return MACROS;
    }

    public static void load() {
        try {
            if (!Files.exists(FILE)) return;
            List<Macro> loaded = GSON.fromJson(Files.readString(FILE), new TypeToken<List<Macro>>() {}.getType());
            MACROS.clear();
            if (loaded != null) MACROS.addAll(loaded);
        } catch (IOException | RuntimeException ignored) {}
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(MACROS));
        } catch (IOException ignored) {}
    }

    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null || mc.currentScreen != null) {
            for (Macro m : MACROS) m.wasDown = false;
            return;
        }
        long win = mc.getWindow().getHandle();
        for (Macro m : MACROS) {
            if (m.key <= 0 || m.text == null || m.text.isBlank()) continue;
            boolean down = GLFW.glfwGetKey(win, m.key) == GLFW.GLFW_PRESS;
            if (down && !m.wasDown) send(mc, m.text.trim());
            m.wasDown = down;
        }
    }

    private static void send(MinecraftClient mc, String text) {
        if (text.startsWith("/")) mc.getNetworkHandler().sendChatCommand(text.substring(1));
        else mc.getNetworkHandler().sendChatMessage(text);
    }
}
