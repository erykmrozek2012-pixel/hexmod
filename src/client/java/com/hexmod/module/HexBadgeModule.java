package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Ikona Hex nad glowa graczy, ktorzy sa na liscie hexmod-data/users.json (dodaje ja admin recznie).
 * Mod nie ma serwera, wiec sam nie wie, kto jeszcze go uzywa. Pozycje i skale ustawiasz w edytorze HUD.
 */
public class HexBadgeModule extends Module {
    private static final int SIZE = 16;

    /** Ikona 16x16 jako sklejone prostokaty: x, y, szerokosc, wysokosc, kolor ARGB (malo prostokatow = malo kosztu). */
    private static final int[] RUNS = {
            7, 0, 2, 1, 0xFF400000,
            4, 1, 2, 1, 0xFF400000,
            6, 1, 1, 2, 0xFF000000,
            7, 1, 1, 1, 0xFF200000,
            8, 1, 1, 1, 0xFF400000,
            9, 1, 1, 1, 0xFF000000,
            10, 1, 1, 1, 0xFF400000,
            11, 1, 1, 1, 0xFF800000,
            3, 2, 1, 1, 0xFF600000,
            4, 2, 1, 1, 0xFF000000,
            5, 2, 1, 1, 0xFF200000,
            7, 2, 1, 1, 0xFF600000,
            8, 2, 1, 1, 0xFF800000,
            9, 2, 1, 1, 0xFF200000,
            10, 2, 1, 1, 0xFF600000,
            11, 2, 1, 1, 0xFF400000,
            12, 2, 1, 1, 0xFF800000,
            2, 3, 3, 1, 0xFF200000,
            5, 3, 1, 1, 0xFF600000,
            6, 3, 1, 1, 0xFF400000,
            7, 3, 1, 1, 0xFFA00000,
            8, 3, 1, 1, 0xFFC02020,
            9, 3, 1, 1, 0xFF600000,
            10, 3, 1, 1, 0xFF800000,
            11, 3, 1, 1, 0xFF000000,
            12, 3, 1, 1, 0xFF200000,
            13, 3, 1, 1, 0xFF800000,
            1, 4, 1, 1, 0xFF400000,
            2, 4, 1, 1, 0xFF200000,
            3, 4, 1, 2, 0xFF000000,
            4, 4, 1, 1, 0xFF600000,
            5, 4, 1, 1, 0xFF200000,
            6, 4, 1, 1, 0xFF800000,
            7, 4, 1, 1, 0xFF400000,
            8, 4, 1, 1, 0xFF600000,
            9, 4, 1, 1, 0xFF800000,
            10, 4, 1, 1, 0xFF600000,
            11, 4, 2, 1, 0xFF800000,
            13, 4, 2, 1, 0xFF400000,
            1, 5, 2, 1, 0xFF200000,
            4, 5, 1, 2, 0xFF200000,
            5, 5, 2, 2, 0xFF400000,
            7, 5, 2, 1, 0xFF200000,
            9, 5, 1, 1, 0xFF400000,
            10, 5, 1, 1, 0xFF200000,
            11, 5, 1, 1, 0xFF600000,
            12, 5, 1, 1, 0xFFA00000,
            13, 5, 1, 1, 0xFF200000,
            14, 5, 1, 1, 0xFF400000,
            1, 6, 1, 1, 0xFF200000,
            2, 6, 1, 1, 0xFF600000,
            3, 6, 1, 1, 0xFF400000,
            7, 6, 1, 1, 0xFF602000,
            8, 6, 2, 1, 0xFF600000,
            10, 6, 1, 1, 0xFF400000,
            11, 6, 1, 1, 0xFF200000,
            12, 6, 2, 1, 0xFF400000,
            14, 6, 1, 1, 0xFF200000,
            0, 7, 2, 2, 0xFF200000,
            2, 7, 2, 1, 0xFF600000,
            4, 7, 1, 1, 0xFF602020,
            5, 7, 2, 1, 0xFF802020,
            7, 7, 1, 1, 0xFFA04040,
            8, 7, 1, 1, 0xFF602020,
            9, 7, 1, 1, 0xFF600000,
            10, 7, 2, 1, 0xFF802020,
            12, 7, 1, 1, 0xFF600000,
            13, 7, 1, 1, 0xFF000000,
            14, 7, 2, 1, 0xFF200000,
            2, 8, 1, 1, 0xFF400000,
            3, 8, 1, 1, 0xFF600000,
            4, 8, 1, 1, 0xFF402020,
            5, 8, 1, 1, 0xFF602020,
            6, 8, 1, 1, 0xFF400000,
            7, 8, 1, 1, 0xFF802020,
            8, 8, 1, 1, 0xFF400000,
            9, 8, 1, 1, 0xFF000000,
            10, 8, 2, 1, 0xFF602020,
            12, 8, 1, 1, 0xFF000000,
            13, 8, 2, 1, 0xFF400000,
            15, 8, 1, 1, 0xFF200000,
            1, 9, 6, 1, 0xFF400000,
            7, 9, 2, 1, 0xFF602020,
            9, 9, 1, 2, 0xFF400000,
            10, 9, 1, 1, 0xFF602000,
            11, 9, 1, 1, 0xFF602020,
            12, 9, 2, 1, 0xFF600000,
            14, 9, 1, 1, 0xFF400000,
            1, 10, 1, 2, 0xFF400000,
            2, 10, 1, 1, 0xFF600000,
            3, 10, 1, 1, 0xFF800000,
            4, 10, 2, 1, 0xFF400000,
            6, 10, 1, 3, 0xFF200000,
            7, 10, 2, 1, 0xFF000000,
            10, 10, 1, 1, 0xFF200000,
            11, 10, 1, 1, 0xFF400000,
            12, 10, 1, 1, 0xFF600000,
            13, 10, 2, 1, 0xFF200000,
            2, 11, 2, 1, 0xFF200000,
            4, 11, 2, 1, 0xFF000000,
            7, 11, 2, 1, 0xFF600000,
            9, 11, 1, 1, 0xFF200000,
            10, 11, 1, 1, 0xFF000000,
            11, 11, 3, 1, 0xFF200000,
            14, 11, 1, 1, 0xFF400000,
            2, 12, 1, 1, 0xFF400000,
            3, 12, 1, 1, 0xFF200000,
            4, 12, 2, 1, 0xFF400000,
            7, 12, 2, 1, 0xFF800000,
            9, 12, 2, 2, 0xFF000000,
            11, 12, 1, 1, 0xFF600000,
            12, 12, 2, 1, 0xFF400000,
            3, 13, 1, 1, 0xFF400000,
            4, 13, 2, 1, 0xFF200000,
            6, 13, 1, 1, 0xFF000000,
            7, 13, 2, 1, 0xFF200000,
            11, 13, 1, 1, 0xFF400000,
            12, 13, 1, 1, 0xFF600000,
            4, 14, 2, 1, 0xFF400000,
            6, 14, 4, 1, 0xFF200000,
            10, 14, 1, 1, 0xFF600000,
            11, 14, 1, 1, 0xFF200000,
            7, 15, 2, 1, 0xFF200000};

    public HexBadgeModule() {
        super("hex_badge", "Hex Badge", "Hex icon above players who use the mod");
        at(0, -20);
        rangeOption(48);
    }

    @Override
    protected boolean overlay() {
        return true;
    }

    @Override
    public boolean hasOverlayPreview() {
        return true;
    }

    private void drawIcon(DrawContext ctx, int ax, int ay) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) (ax + x), (float) (ay + y));
        m.scale(scale, scale);
        for (int i = 0; i < RUNS.length; i += 5) {
            int rx = RUNS[i] - SIZE / 2, ry = RUNS[i + 1] - SIZE;
            ctx.fill(rx, ry, rx + RUNS[i + 2], ry + RUNS[i + 3], RUNS[i + 4]);
        }
        m.popMatrix();
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (preview || mc.player == null || mc.world == null) return;
        float tp = mc.getRenderTickCounter().getTickProgress(false);

        double rangeSq = (double) range * range;
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl == mc.player || pl.isSpectator() || pl.isInvisible()) continue;
            if (mc.player.squaredDistanceTo(pl) > rangeSq || !Projection.canSee(pl)) continue;
            if (!Remote.isHexUser(pl.getName().getString(), pl.getUuid().toString())) continue;
            float[] s = Projection.project(ctx, pl.getLerpedPos(tp).add(0, pl.getHeight() + 0.1, 0));
            if (s == null) continue;
            drawIcon(ctx, Math.round(s[0]), Math.round(s[1]));
        }
    }

    // ---------- podglad w edytorze HUD ----------

    @Override
    public void drawOverlayPreview(DrawContext ctx, int ax, int ay) {
        drawIcon(ctx, ax, ay);
    }

    @Override
    public int[] overlayPreviewBox(int ax, int ay) {
        int w = Math.round(SIZE * scale);
        int left = ax + x - w / 2;
        int bottom = ay + y;
        return new int[]{left, bottom - w, left + w, bottom};
    }
}
