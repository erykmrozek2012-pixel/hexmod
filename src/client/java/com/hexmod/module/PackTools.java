package com.hexmod.module;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Narzedzia do pakietow tekstur: lista, rozpakowanie kopii, odczyt/zapis PNG i obraz na tarcze. */
public final class PackTools {
    public record Img(int w, int h, int[] px) {}

    private PackTools() {}

    public static Path packsDir() {
        return FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
    }

    public static Path shieldDir() {
        return FabricLoader.getInstance().getGameDir().resolve("hexmod").resolve("shield");
    }

    public static List<Path> listPacks() {
        List<Path> out = new ArrayList<>();
        try {
            Files.createDirectories(packsDir());
            try (Stream<Path> s = Files.list(packsDir())) {
                s.filter(p -> Files.isDirectory(p) || p.getFileName().toString().toLowerCase().endsWith(".zip"))
                        .sorted().forEach(out::add);
            }
        } catch (IOException ignored) {}
        return out;
    }

    /** Dla .zip rozpakowuje kopie do folderu "<nazwa>_hexedit" (jesli jej nie ma) i zwraca ten folder. */
    public static Path extractIfZip(Path pack) throws IOException {
        String name = pack.getFileName().toString();
        if (Files.isDirectory(pack)) return pack;
        String base = name.substring(0, name.length() - 4);
        Path target = packsDir().resolve(base + "_hexedit").toAbsolutePath().normalize();
        if (Files.exists(target)) return target;
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(pack))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                Path out = target.resolve(e.getName()).normalize();
                if (!out.startsWith(target)) continue;
                if (e.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zin, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        return target;
    }

    /** Korzen pakietu (folder z "assets"), takze gdy pakiet jest zapakowany w dodatkowy folder. */
    public static Path packRoot(Path folder) {
        if (Files.isDirectory(folder.resolve("assets"))) return folder;
        try (Stream<Path> s = Files.list(folder)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (Files.isDirectory(p) && Files.isDirectory(p.resolve("assets"))) return p;
            }
        } catch (IOException ignored) {}
        return folder;
    }

    public static Path texturesDir(Path folder) {
        return packRoot(folder).resolve("assets").resolve("minecraft").resolve("textures");
    }

    /** Foldery i pliki PNG w katalogu (foldery pierwsze). */
    public static List<Path> listEntries(Path dir) {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> Files.isDirectory(p) || p.getFileName().toString().toLowerCase().endsWith(".png"))
                    .sorted((a, b) -> {
                        boolean da = Files.isDirectory(a), db = Files.isDirectory(b);
                        if (da != db) return da ? -1 : 1;
                        return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
                    }).forEach(out::add);
        } catch (IOException ignored) {}
        return out;
    }

    // ---------- obrazy ----------

    public static Img readImage(InputStream in) throws IOException {
        try (NativeImage img = NativeImage.read(in)) {
            int w = img.getWidth(), h = img.getHeight();
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) px[y * w + x] = img.getColorArgb(x, y);
            }
            return new Img(w, h, px);
        }
    }

    public static Img readImage(Path p) throws IOException {
        try (InputStream in = Files.newInputStream(p)) {
            return readImage(in);
        }
    }

    public static void writeImage(Path p, Img im) throws IOException {
        try (NativeImage img = new NativeImage(NativeImage.Format.RGBA, im.w(), im.h(), false)) {
            for (int y = 0; y < im.h(); y++) {
                for (int x = 0; x < im.w(); x++) img.setColorArgb(x, y, im.px()[y * im.w() + x]);
            }
            Files.createDirectories(p.getParent());
            img.writeTo(p);
        }
    }

    private static int over(int top, int bottom) {
        int ta = top >>> 24;
        if (ta == 255) return top;
        if (ta == 0) return bottom;
        float a = ta / 255f;
        float b = (bottom >>> 24) / 255f * (1f - a);
        float oa = a + b;
        if (oa <= 0f) return 0;
        int r = Math.round((((top >> 16) & 255) * a + ((bottom >> 16) & 255) * b) / oa);
        int g = Math.round((((top >> 8) & 255) * a + ((bottom >> 8) & 255) * b) / oa);
        int bl = Math.round(((top & 255) * a + (bottom & 255) * b) / oa);
        return (Math.round(oa * 255f) << 24) | (r << 16) | (g << 8) | bl;
    }

    private static final String[] SHIELD_TEXTURES = {
            "textures/entity/shield/shield_base_nopattern.png",
            "textures/entity/shield/base_nopattern.png",
            "textures/entity/shield_base_nopattern.png",
            "textures/entity/shield/shield_base.png",
            "textures/entity/shield/base.png",
            "textures/entity/shield_base.png"
    };

    /**
     * Wkleja obraz uzytkownika na przednia czesc tarczy (obszar 12x22 w teksturze 64x64) i zapisuje
     * wynik do pakietu. Baza to aktualna tekstura tarczy z gry. Zwraca komunikat dla uzytkownika.
     */
    public static String applyShield(Path packFolder, Path userImage) {
        try {
            Img src = readImage(userImage);
            var rm = MinecraftClient.getInstance().getResourceManager();
            Path root = packRoot(packFolder);
            int applied = 0;
            for (String cand : SHIELD_TEXTURES) {
                Optional<Resource> res = rm.getResource(Identifier.ofVanilla(cand));
                if (res.isEmpty()) continue;
                Img base;
                try (InputStream in = res.get().getInputStream()) {
                    base = readImage(in);
                }
                int f = Math.max(1, base.w() / 64);
                int x0 = f, y0 = f, rw = 12 * f, rh = 22 * f;
                int[] px = base.px().clone();
                for (int ty = 0; ty < rh; ty++) {
                    for (int tx = 0; tx < rw; tx++) {
                        int sx = Math.min(src.w() - 1, tx * src.w() / rw);
                        int sy = Math.min(src.h() - 1, ty * src.h() / rh);
                        int i = (y0 + ty) * base.w() + (x0 + tx);
                        if (i < px.length) px[i] = over(src.px()[sy * src.w() + sx], px[i]);
                    }
                }
                writeImage(root.resolve("assets").resolve("minecraft").resolve(cand), new Img(base.w(), base.h(), px));
                applied++;
            }
            if (applied == 0) return "Nie znaleziono tekstury tarczy w tej wersji gry.";
            return "Zapisano obraz na tarczy w pakiecie. Wlacz pakiet w ustawieniach i przeladuj tekstury.";
        } catch (IOException | RuntimeException e) {
            return "Blad: " + e.getMessage();
        }
    }
}
