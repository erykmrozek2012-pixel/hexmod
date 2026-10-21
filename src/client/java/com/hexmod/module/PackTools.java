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

    /** Powieksza obraz k razy (najblizszy sasiad), zeby tarcza mogla miec wyzsza rozdzielczosc. */
    private static Img upscale(Img im, int k) {
        if (k <= 1) return im;
        int nw = im.w() * k, nh = im.h() * k;
        int[] out = new int[nw * nh];
        for (int y = 0; y < nh; y++) {
            for (int x = 0; x < nw; x++) out[y * nw + x] = im.px()[(y / k) * im.w() + (x / k)];
        }
        return new Img(nw, nh, out);
    }

    private static int clampPx(Img s, int x, int y) {
        int cx = Math.max(0, Math.min(x, s.w() - 1)), cy = Math.max(0, Math.min(y, s.h() - 1));
        return s.px()[cy * s.w() + cx];
    }

    /** Wygladzanie dwuliniowe, gdy obraz zrodlowy jest mniejszy niz obszar docelowy (bez pikseli). */
    private static int bilinear(Img s, int tx, int ty, int rw, int rh) {
        float u = (tx + 0.5f) * s.w() / rw - 0.5f, v = (ty + 0.5f) * s.h() / rh - 0.5f;
        int x0 = (int) Math.floor(u), y0 = (int) Math.floor(v);
        float fx = u - x0, fy = v - y0;
        int[] c = {clampPx(s, x0, y0), clampPx(s, x0 + 1, y0), clampPx(s, x0, y0 + 1), clampPx(s, x0 + 1, y0 + 1)};
        float[] wt = {(1 - fx) * (1 - fy), fx * (1 - fy), (1 - fx) * fy, fx * fy};
        float a = 0, r = 0, g = 0, b = 0;
        for (int i = 0; i < 4; i++) {
            float ca = (c[i] >>> 24) * wt[i];
            a += ca;
            r += ((c[i] >> 16) & 255) * ca;
            g += ((c[i] >> 8) & 255) * ca;
            b += (c[i] & 255) * ca;
        }
        if (a <= 0.5f) return 0;
        return (Math.round(a) << 24) | (Math.round(r / a) << 16) | (Math.round(g / a) << 8) | Math.round(b / a);
    }

    /** Usrednia fragment obrazu zrodlowego odpowiadajacy jednemu pikselowi docelowemu (ostry obraz po zmniejszeniu). */
    private static int sample(Img s, int tx, int ty, int rw, int rh) {
        if (s.w() < rw || s.h() < rh) return bilinear(s, tx, ty, rw, rh);
        int x0 = tx * s.w() / rw, x1 = Math.max(x0 + 1, (tx + 1) * s.w() / rw);
        int y0 = ty * s.h() / rh, y1 = Math.max(y0 + 1, (ty + 1) * s.h() / rh);
        x1 = Math.min(x1, s.w());
        y1 = Math.min(y1, s.h());
        x0 = Math.min(x0, s.w() - 1);
        y0 = Math.min(y0, s.h() - 1);
        long a = 0, r = 0, g = 0, b = 0;
        int n = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int c = s.px()[y * s.w() + x];
                int ca = c >>> 24;
                a += ca;
                r += (long) ((c >> 16) & 255) * ca;
                g += (long) ((c >> 8) & 255) * ca;
                b += (long) (c & 255) * ca;
                n++;
            }
        }
        if (n == 0 || a == 0) return 0;
        int oa = (int) (a / n);
        return (oa << 24) | ((int) (r / a) << 16) | ((int) (g / a) << 8) | (int) (b / a);
    }

    /** Zgodnosc ze starym wywolaniem: jakosc 4x, bez odbicia. */
    public static String applyShield(Path packFolder, Path userImage) {
        return applyShield(packFolder, userImage, 4, false);
    }

    /**
     * Wkleja obraz uzytkownika na przednia czesc tarczy (obszar 12x22 w teksturze 64x64, w wyzszej jakosci
     * proporcjonalnie wiekszy) i zapisuje wynik do pakietu. quality = 1, 2, 4, 8 (rozdzielczosc 64*quality).
     * Baza to aktualna tekstura tarczy z gry. Zwraca komunikat dla uzytkownika.
     */
    public static String applyShield(Path packFolder, Path userImage, int quality, boolean flip) {
        try {
            Img src = readImage(userImage);
            var rm = MinecraftClient.getInstance().getResourceManager();
            Path root = packRoot(packFolder);
            int applied = 0;
            int lastSize = 64;
            for (String cand : SHIELD_TEXTURES) {
                Optional<Resource> res = rm.getResource(Identifier.ofVanilla(cand));
                if (res.isEmpty()) continue;
                Img base;
                try (InputStream in = res.get().getInputStream()) {
                    base = readImage(in);
                }
                int bf = Math.max(1, base.w() / 64);
                if (quality > bf && quality % bf == 0 && base.w() % 64 == 0 && base.w() == base.h()) {
                    base = upscale(base, quality / bf);
                }
                int f = Math.max(1, base.w() / 64);
                int x0 = f, y0 = f, rw = 12 * f, rh = 22 * f;
                int[] px = base.px().clone();
                for (int ty = 0; ty < rh; ty++) {
                    for (int tx = 0; tx < rw; tx++) {
                        int sxp = flip ? rw - 1 - tx : tx;
                        int i = (y0 + ty) * base.w() + (x0 + tx);
                        if (i < px.length) px[i] = over(sample(src, sxp, ty, rw, rh), px[i]);
                    }
                }
                writeImage(root.resolve("assets").resolve("minecraft").resolve(cand), new Img(base.w(), base.h(), px));
                applied++;
                lastSize = base.w();
            }
            if (applied == 0) return "Nie znaleziono tekstury tarczy w tej wersji gry.";
            return "Zapisano obraz na tarczy (" + lastSize + "x" + lastSize + "). Wlacz pakiet w ustawieniach i przeladuj tekstury.";
        } catch (IOException | RuntimeException e) {
            return "Blad: " + e.getMessage();
        }
    }

    /**
     * Zwraca plik tekstury w pakiecie. Jesli pakiet jeszcze jej nie ma, kopiuje aktualna teksture z gry
     * (np. oryginalna), zeby mozna ja bylo edytowac.
     */
    public static Path ensureTexture(Path packFolder, String rel) throws IOException {
        Path target = packRoot(packFolder).resolve("assets").resolve("minecraft").resolve(rel);
        if (Files.exists(target)) return target;
        var rm = MinecraftClient.getInstance().getResourceManager();
        Optional<Resource> res = rm.getResource(Identifier.ofVanilla(rel));
        if (res.isEmpty()) throw new IOException("Brak tej tekstury w grze: " + rel);
        Files.createDirectories(target.getParent());
        try (InputStream in = res.get().getInputStream()) {
            Files.copy(in, target);
        }
        return target;
    }
}
