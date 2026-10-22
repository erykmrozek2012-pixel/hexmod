package com.hexmod.module;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lista spolecznosci pobierana z repozytorium hexmod-data (community.json): configi i paczki tekstur.
 * Admin dodaje wpisy w repozytorium, a kazdy gracz widzi je od razu, bez aktualizacji moda.
 */
public final class Community {
    public static class Entry {
        public String name = "";
        public String author = "";
        public String file = "";
    }

    public static class Data {
        public List<Entry> configs = new ArrayList<>();
        public List<Entry> packs = new ArrayList<>();
    }

    private static final Gson GSON = new Gson();

    private Community() {}

    public static Data load() throws IOException {
        Data d = GSON.fromJson(Web.get(Remote.BASE + "community.json"), Data.class);
        if (d == null) d = new Data();
        if (d.configs == null) d.configs = new ArrayList<>();
        if (d.packs == null) d.packs = new ArrayList<>();
        return d;
    }

    private static String url(Entry e) throws IOException {
        String f = e.file == null ? "" : e.file.trim();
        if (f.isEmpty() || f.contains("..") || f.contains("://") || f.startsWith("/")) {
            throw new IOException(L.t("Invalid file path."));
        }
        return Remote.BASE + f.replace(" ", "%20");
    }

    private static String safe(String s) {
        return s == null ? "" : s.replaceAll("[^A-Za-z0-9._ -]", "_");
    }

    /** Pobiera config z listy i od razu go wczytuje. Zwraca true, gdy plik byl poprawny. */
    public static boolean installConfig(Entry e) throws IOException {
        return ModuleManager.importJson(Web.get(url(e)));
    }

    /** Pobiera paczke tekstur do folderu resourcepacks i zwraca nazwe zapisanego pliku. */
    public static String downloadPack(Entry e) throws IOException {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
        Files.createDirectories(dir);
        String file = "hexpack-" + safe(e.author) + "-" + safe(e.name) + ".zip";
        Web.download(url(e), dir.resolve(file));
        return file;
    }
}
