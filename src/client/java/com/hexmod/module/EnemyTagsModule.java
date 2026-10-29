package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;

/** Wskazniki nad glowa wroga: tylko ping gracza. Pozycje, skale i kolor ustawiasz w edytorze HUD. */
public class EnemyTagsModule extends Module {
    private static final int ROW = 10;

    public EnemyTagsModule() {
        super("enemy_tags", "Enemy Tags", "Ping of the player above the enemy head");
        at(0, -6); // przesuniecie w pikselach nad glowa (stale, niezalezne od odleglosci)
        rangeOption(48); // zasieg w blokach, ustawiany w edytorze HUD
    }

    @Override
    protected boolean overlay() {
        return true;
    }

    @Override
    public boolean hasOverlayPreview() {
        return true;
    }

    private static int pingColor(int ping) {
        return ping < 80 ? 0x30D158 : ping < 150 ? 0xFFD60A : 0xD10A0A;
    }

    private static int pingOf(PlayerEntity pl) {
        var handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) return -1;
        PlayerListEntry entry = handler.getPlayerListEntry(pl.getUuid());
        return entry == null ? -1 : entry.getLatency();
    }

    private void drawText(DrawContext ctx, String text, int color, int ax, int ay) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) (ax + x), (float) (ay + y));
        m.scale(scale, scale);
        int tw = tr.getWidth(text);
        if (style != 0) {
            m.pushMatrix();
            m.translate((float) (-tw / 2), (float) -ROW);
            drawStyleFrame(ctx, tw, 9);
            m.popMatrix();
        }
        ctx.drawTextWithShadow(tr, text, -tw / 2, -ROW, 0xFF000000 | color);
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
            int ping = pingOf(pl);
            if (ping < 0) continue;
            float[] s = Projection.project(ctx, pl.getLerpedPos(tp).add(0, pl.getHeight() + 0.1, 0));
            if (s == null) continue;
            drawText(ctx, "Ping: " + ping + "ms", pingColor(ping), Math.round(s[0]), Math.round(s[1]));
        }
    }

    // ---------- podglad w edytorze HUD ----------

    @Override
    public void drawOverlayPreview(DrawContext ctx, int ax, int ay) {
        drawText(ctx, "Ping: 38ms", pingColor(38), ax, ay);
    }

    @Override
    public int[] overlayPreviewBox(int ax, int ay) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int w = Math.round(Math.max(30, tr.getWidth("Ping: 38ms")) * scale);
        int h = Math.round(ROW * scale);
        int left = ax + x - w / 2;
        int bottom = ay + y;
        return new int[]{left, bottom - h, left + w, bottom};
    }
}
