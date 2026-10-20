package com.hexmod.module;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Pobieranie modow z Modrinth i wlaczanie/wylaczanie (zmiana nazwy pliku). Zmiany dzialaja po restarcie. */
public final class ModCatalog {
    public enum State { NOT_INSTALLED, ENABLED, DISABLED }

    private ModCatalog() {}

    private static String clean(String slug) {
        return slug == null ? "" : slug.replaceAll("[^A-Za-z0-9_-]", "");
    }

    private static Path modsDir() {
        return FabricLoader.getInstance().getGameDir().resolve("mods");
    }

    private static Path jar(String slug) {
        return modsDir().resolve("hexmod-" + clean(slug) + ".jar");
    }

    private static Path off(String slug) {
        return modsDir().resolve("hexmod-" + clean(slug) + ".jar.disabled");
    }

    public static State state(String slug) {
        if (Files.exists(jar(slug))) return State.ENABLED;
        if (Files.exists(off(slug))) return State.DISABLED;
        return State.NOT_INSTALLED;
    }

    public static void setEnabled(String slug, boolean on) throws IOException {
        Path from = on ? off(slug) : jar(slug);
        Path to = on ? jar(slug) : off(slug);
        if (Files.exists(from)) Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void install(String slug) throws IOException {
        String s = clean(slug);
        if (s.isEmpty()) throw new IOException("Zla nazwa moda");
        String url = "https://api.modrinth.com/v2/project/" + s
                + "/version?loaders=%5B%22fabric%22%5D&game_versions=%5B%221.21.11%22%5D";
        JsonArray versions = JsonParser.parseString(Web.get(url)).getAsJsonArray();
        if (versions.isEmpty()) throw new IOException("Brak wersji na 1.21.11");
        JsonArray files = versions.get(0).getAsJsonObject().getAsJsonArray("files");
        if (files == null || files.isEmpty()) throw new IOException("Brak plikow");
        JsonObject pick = files.get(0).getAsJsonObject();
        for (JsonElement f : files) {
            JsonObject o = f.getAsJsonObject();
            if (o.has("primary") && o.get("primary").getAsBoolean()) { pick = o; break; }
        }
        Files.createDirectories(modsDir());
        Files.deleteIfExists(off(s));
        Web.download(pick.get("url").getAsString(), jar(s));
    }
}
