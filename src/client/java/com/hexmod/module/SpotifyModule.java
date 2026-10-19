package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Map;

/** Widget Spotify: aktualny utwor i tekst piosenki zsynchronizowany z muzyka. */
public class SpotifyModule extends Module {
    private int ticks = 0;

    public SpotifyModule() {
        super("spotify", "Spotify", "Aktualny utwor i tekst piosenki");
        at(10, 150);
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
            ctx.drawTextWithShadow(mc.textRenderer, "\u266A Artysta - Tytul", 0, 0, rgb());
            ctx.drawTextWithShadow(mc.textRenderer, "Tu bedzie aktualna linijka tekstu", 0, 12, rgb());
            return;
        }
        SpotifyClient.Track t = SpotifyClient.current();
        if (t == null) {
            String msg;
            if (!SpotifyClient.isLoggedIn()) msg = SpotifyClient.localInfo.isEmpty() ? "Spotify: wlacz muzyke w aplikacji" : SpotifyClient.localInfo;
            else if (SpotifyClient.status.startsWith("Spotify")) msg = SpotifyClient.status;
            else msg = "Spotify: nic nie gra (wlacz muzyke w aplikacji)";
            ctx.drawTextWithShadow(mc.textRenderer, mc.textRenderer.trimToWidth(msg, 200), 0, 0, 0xFF9A9A9A);
            return;
        }
        String head = mc.textRenderer.trimToWidth("\u266A " + t.artist + " - " + t.title, 200);
        ctx.drawTextWithShadow(mc.textRenderer, head, 0, 0, rgb());
        Map.Entry<Long, String> e = t.lyrics.floorEntry(SpotifyClient.progressNow(t));
        if (e != null && !e.getValue().isEmpty()) {
            ctx.drawTextWithShadow(mc.textRenderer, mc.textRenderer.trimToWidth(e.getValue(), 200), 0, 12, rgb());
        }
    }
}
