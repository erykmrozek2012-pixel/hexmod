package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Map;

/** Widget Spotify: okladka, aktualny utwor i tekst piosenki zsynchronizowany z muzyka (bez logowania). */
public class SpotifyModule extends Module {
    private int ticks = 0;

    public SpotifyModule() {
        super("spotify", "Spotify", "Current song and lyrics");
        at(10, 150);
        option("cover", "Cover image", true);
        option("lyrics", "Lyrics", true);
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

    private static void drawCover(DrawContext ctx, int x, int y, int[] px) {
        int c = SpotifyClient.COVER;
        ctx.fill(x - 1, y - 1, x + c + 1, y + c + 1, 0xFF000000);
        for (int j = 0; j < c; j++) {
            for (int i = 0; i < c; i++) ctx.fill(x + i, y + j, x + i + 1, y + j + 1, px[j * c + i]);
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
