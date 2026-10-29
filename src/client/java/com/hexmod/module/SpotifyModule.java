package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Widget Spotify: okladka, aktualny utwor i tekst piosenki zsynchronizowany z muzyka (bez logowania). */
public class SpotifyModule extends Module {
    private int ticks = 0;

    public SpotifyModule() {
        super("spotify", "Spotify", "Current song and lyrics");
        at(10, 150);
        option("cover", "Cover image", true);
        option("lyrics", "Lyrics", true);
        option("hq_cover", "Sharp cover (slower)", false);
        defaultStyle(7);
    }

    private static final int GREEN = 0xFF1DB954;

    /** Rysowana ikona nuty muzycznej (8x10 px). */
    private static void note(DrawContext ctx, int x, int y, int c) {
        ctx.fill(x + 3, y, x + 4, y + 8, c);
        ctx.fill(x, y + 6, x + 4, y + 9, c);
        ctx.fill(x + 4, y, x + 7, y + 2, c);
        ctx.fill(x + 6, y + 2, x + 7, y + 4, c);
    }

    private int[] coverSrc;
    private int coverStep = -1;
    private int[] coverRects = new int[0];

    /** Skleja piksele okladki w jak najmniej prostokatow (mniej prostokatow = duzo wiecej FPS). */
    private static int[] buildRects(int[] px, int c, int step) {
        int g = c / step;
        int[] cell = new int[g * g];
        for (int j = 0; j < g; j++) {
            for (int i = 0; i < g; i++) {
                int r = 0, gr = 0, b = 0, n = 0;
                for (int dy = 0; dy < step; dy++) {
                    for (int dx = 0; dx < step; dx++) {
                        int p = px[(j * step + dy) * c + i * step + dx];
                        r += (p >> 16) & 255;
                        gr += (p >> 8) & 255;
                        b += p & 255;
                        n++;
                    }
                }
                r /= n;
                gr /= n;
                b /= n;
                if (step > 1) {
                    r &= 0xF8;
                    gr &= 0xF8;
                    b &= 0xF8;
                }
                cell[j * g + i] = 0xFF000000 | (r << 16) | (gr << 8) | b;
            }
        }
        List<int[]> out = new ArrayList<>();
        Map<Long, int[]> open = new HashMap<>();
        for (int j = 0; j < g; j++) {
            Map<Long, int[]> next = new HashMap<>();
            int i = 0;
            while (i < g) {
                int col = cell[j * g + i];
                int e = i;
                while (e + 1 < g && cell[j * g + e + 1] == col) e++;
                int w = e - i + 1;
                long key = ((long) i << 40) | ((long) w << 32) | (col & 0xFFFFFFFFL);
                int[] r = open.get(key);
                if (r == null) {
                    r = new int[]{i, j, w, 1, col};
                    out.add(r);
                } else {
                    r[3]++;
                }
                next.put(key, r);
                i = e + 1;
            }
            open = next;
        }
        int[] flat = new int[out.size() * 5];
        for (int k = 0; k < out.size(); k++) System.arraycopy(out.get(k), 0, flat, k * 5, 5);
        return flat;
    }

    private void drawCover(DrawContext ctx, int x, int y, int[] px) {
        int c = SpotifyClient.COVER;
        int step = option("hq_cover") ? 1 : 2;
        if (px.length < c * c) return;
        if (px != coverSrc || step != coverStep) {
            coverRects = buildRects(px, c, step);
            coverSrc = px;
            coverStep = step;
        }
        ctx.fill(x - 1, y - 1, x + c + 1, y + c + 1, 0xFF000000);
        int[] r = coverRects;
        for (int k = 0; k < r.length; k += 5) {
            ctx.fill(x + r[k] * step, y + r[k + 1] * step,
                    x + (r[k] + r[k + 2]) * step, y + (r[k + 1] + r[k + 3]) * step, r[k + 4]);
        }
    }

    @Override public int w() { return 230; }
    @Override public int h() { return 30; }

    @Override
    public void tick() {
        if (++ticks % SpotifyClient.pollEvery() == 0) SpotifyClient.pollAsync();
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int cs = SpotifyClient.COVER;
        if (preview) {
            if (option("cover")) {
                ctx.fill(0, 1, cs, 1 + cs, 0xFF303036);
                note(ctx, 10, 11, GREEN);
            }
            int tx = option("cover") ? cs + 6 : 11;
            if (!option("cover")) note(ctx, 0, 4, GREEN);
            ctx.drawTextWithShadow(mc.textRenderer, L.t("Artist - Title"), tx, 4, rgb());
            if (option("lyrics")) ctx.drawTextWithShadow(mc.textRenderer, L.t("The current lyric line appears here"), tx, 17, rgb());
            return;
        }
        SpotifyClient.Track t = SpotifyClient.current();
        if (t == null) {
            String msg;
            if (!SpotifyClient.isLoggedIn()) msg = SpotifyClient.localInfo.isEmpty() ? L.t("Spotify: start music in the app") : SpotifyClient.localInfo;
            else if (SpotifyClient.status.startsWith("Spotify")) msg = SpotifyClient.status;
            else msg = L.t("Spotify: nothing playing (start music in the app)");
            note(ctx, 0, 10, 0xFF6A6A6A);
            ctx.drawTextWithShadow(mc.textRenderer, mc.textRenderer.trimToWidth(msg, w() - 14), 11, 10, 0xFF9A9A9A);
            return;
        }
        int[] cover = t.cover;
        int tx;
        if (option("cover") && cover != null) {
            drawCover(ctx, 0, 1, cover);
            tx = cs + 6;
        } else {
            note(ctx, 0, 4, GREEN);
            tx = 11;
        }
        String line = null;
        if (option("lyrics")) {
            Map.Entry<Long, String> e = t.lyrics.floorEntry(SpotifyClient.progressNow(t) + SpotifyClient.lyricOffsetMs());
            if (e != null && !e.getValue().isEmpty()) line = e.getValue();
        }
        int maxW = w() - tx - 2;
        ctx.drawTextWithShadow(mc.textRenderer, mc.textRenderer.trimToWidth(t.artist + " - " + t.title, maxW), tx, line == null ? 11 : 4, rgb());
        if (line != null) ctx.drawTextWithShadow(mc.textRenderer, mc.textRenderer.trimToWidth(line, maxW), tx, 17, 0xFFC8C8C8);
    }
}
