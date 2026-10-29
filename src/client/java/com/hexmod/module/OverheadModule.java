package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

/** Bazowy modul rysujacy napis nad glowa widocznych graczy (tylko ci, ktorych naprawde widzisz). */
public abstract class OverheadModule extends Module {
    private final int lineOffset;

    protected OverheadModule(String id, String name, String description, int lineOffset) {
        super(id, name, description);
        this.lineOffset = lineOffset;
    }

    protected abstract String text(PlayerEntity p);

    protected abstract int colorOf(PlayerEntity p);

    @Override
    protected boolean overlay() {
        return true;
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        if (preview) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;
        float tp = mc.getRenderTickCounter().getTickProgress(false);

        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl == mc.player || pl.isSpectator() || pl.isInvisible()) continue;
            if (mc.player.squaredDistanceTo(pl) > 48.0 * 48.0 || !Projection.canSee(pl)) continue;
            float[] s = Projection.project(ctx, pl.getLerpedPos(tp).add(0, pl.getHeight() + 0.55, 0));
            if (s == null) continue;
            String t = text(pl);
            int w = mc.textRenderer.getWidth(t);
            ctx.drawTextWithShadow(mc.textRenderer, t,
                    Math.round(s[0] - w / 2f), Math.round(s[1]) - lineOffset, 0xFF000000 | colorOf(pl));
        }
    }
}
