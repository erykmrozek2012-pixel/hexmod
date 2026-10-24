package com.hexmod.gui;

import com.hexmod.module.L;
import com.hexmod.module.Module;
import com.hexmod.module.ModuleManager;
import com.hexmod.module.Remote;
import com.hexmod.module.SpotifyClient;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.lwjgl.glfw.GLFW;

/** Mini odtwarzacz nad ekwipunkiem: poprzedni utwor, odtworz/pauza, nastepny (dziala bez logowania). */
public final class SpotifyControls {
    private static final int BTN = 18;
    private static boolean wasDown = false;

    private SpotifyControls() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof InventoryScreen) {
                ScreenEvents.afterRender(screen).register((s, ctx, mouseX, mouseY, delta) ->
                        render(client, ctx, mouseX, mouseY, scaledWidth, scaledHeight));
            }
        });
    }

    private static boolean active() {
        if (Remote.isLocked()) return false;
        for (Module m : ModuleManager.all()) {
            if (m.id.equals("spotify")) return m.isEnabled();
        }
        return false;
    }

    private static void tri(DrawContext ctx, int x, int cy, boolean right, int c) {
        for (int i = 0; i < 5; i++) {
            int half = 4 - i;
            int px = right ? x + i : x + 4 - i;
            ctx.fill(px, cy - half, px + 1, cy + half + 1, c);
        }
    }

    private static void icon(DrawContext ctx, int which, int bx, int by, int c, boolean playing) {
        int cy = by + BTN / 2;
        int x0 = bx + 5;
        if (which == 0) {
            ctx.fill(x0, cy - 4, x0 + 2, cy + 5, c);
            tri(ctx, x0 + 3, cy, false, c);
        } else if (which == 1) {
            if (playing) {
                ctx.fill(x0 + 1, cy - 4, x0 + 4, cy + 5, c);
                ctx.fill(x0 + 6, cy - 4, x0 + 9, cy + 5, c);
            } else {
                tri(ctx, x0 + 1, cy, true, c);
                tri(ctx, x0 + 1, cy, true, c);
            }
        } else {
            tri(ctx, x0, cy, true, c);
            ctx.fill(x0 + 6, cy - 4, x0 + 8, cy + 5, c);
        }
    }

    private static void render(MinecraftClient mc, DrawContext ctx, int mouseX, int mouseY, int sw, int sh) {
        if (!active()) return;
        int top = (sh - 166) / 2;
        if (top - 24 < 2) return; // brak miejsca nad ekwipunkiem
        int y = top - 24;
        int totalW = BTN * 3 + 8 + 6 + 150;
        int x = sw / 2 - totalW / 2;
        Module.roundRect(ctx, x - 4, y - 3, totalW + 8, BTN + 6, 5, 0xCC101014);

        SpotifyClient.Track t = SpotifyClient.current();
        boolean playing = t != null && t.playing;
        boolean cursorEmpty = mc.player == null || mc.player.currentScreenHandler.getCursorStack().isEmpty();
        int hover = -1;
        for (int i = 0; i < 3; i++) {
            int bx = x + i * (BTN + 4);
            boolean over = mouseX >= bx && mouseX < bx + BTN && mouseY >= y && mouseY < y + BTN;
            if (over) hover = i;
            Module.roundRect(ctx, bx, y, BTN, BTN, 4, over ? 0xFF2A2A30 : 0xFF1B1B1F);
            icon(ctx, i, bx, y, over ? 0xFF1DB954 : 0xFFFFFFFF, playing);
        }
        String text = t == null ? L.t("Spotify: start music in the app") : t.artist + " - " + t.title;
        ctx.drawTextWithShadow(mc.textRenderer, mc.textRenderer.trimToWidth(text, 146), x + BTN * 3 + 14, y + 5, 0xFFC8C8C8);

        long win = mc.getWindow().getHandle();
        boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !wasDown && hover >= 0 && cursorEmpty) SpotifyClient.media(hover);
        wasDown = down;
    }
}
