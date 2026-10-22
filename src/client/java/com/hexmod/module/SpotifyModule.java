package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Map;

/** Widget Spotify: aktualny utwor i tekst piosenki zsynchronizowany z muzyka. */
public class SpotifyModule extends Module {
    private int ticks = 0;

    public SpotifyModule() {
        super("spotify", "Spotify", "Current song and lyrics");
        at(10, 150);
    }

    private static final int GREEN = 0xFF1DB954;

    /** Rysowana ikona nuty muzycznej (8x10 px). */
    private static void note(DrawContext ctx, int x, int y, int c) {
        ctx.fill(x + 3, y, x + 4, y + 8, c);
        ctx.fill(x, y + 6, x + 4, y + 9, c);
        ctx.fill(x + 4, y, x + 7, y + 2, c);
        ctx.fill(x + 6, y + 2, x + 7, y + 4, c);
    }

    @Override public int w() { return 200; }
    @Override public int h() { return 24; }

    @Override
    public void tick() {
        if (++ticks % 60 == 0) SpotifyClient.pollAsync();
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (preview) {
            note(ctx, 0, 0, GREEN);
            ctx.drawTextWithShadow(mc.textRenderer, L.t("Artist - Title"), 11, 0, rgb());
            ctx.drawTextWithShadow(mc.textRenderer, L.t("The current lyric line appears here"), 11, 12, rgb());
            return;
        }
        SpotifyClient.Track t = SpotifyClient.current();
        if (t == null) {
            String msg;
            if (!SpotifyClient.isLoggedIn()) msg = SpotifyClient.localInfo.isEmpty() ? L.t("Spotify: start music in the app") : SpotifyClient.localInfo;
            else if (SpotifyClient.status.startsWith("Spotify")) msg = SpotifyClient.status;
            else msg = L.t("Spotify: nothing playing (start music in the app)");
            note(ctx, 0, 0, 0xFF6A6A6A);
            ctx.drawTextWithShadow(mc.textRenderer, mc.textRenderer.trimToWidth(msg, 189), 11, 0, 0xFF9A9A9A);
            return;
        }
        String head = mc.textRenderer.trimToWidth(t.artist + " - " + t.title, 189);
        note(ctx, 0, 0, GREEN);
        ctx.drawTextWithShadow(mc.textRenderer, head, 11, 0, rgb());
        Map.Entry<Long, String> e = t.lyrics.floorEntry(SpotifyClient.progressNow(t));
        if (e != null && !e.getValue().isEmpty()) {
            ctx.drawTextWithShadow(mc.textRenderer, mc.textRenderer.trimToWidth(e.getValue(), 189), 11, 12, rgb());
        }
    }
}
