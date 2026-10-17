package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/** Czy gracz, na ktorego patrzysz, ma uniesiona tarcze. */
public class EnemyShieldModule extends Module {
    public EnemyShieldModule() {
        super("enemy_shield", "Tarcza wroga", "Czy wrog ma uniesiona tarcze");
        centered(34);
    }

    @Override public int w() { return 130; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (preview) {
            ctx.drawTextWithShadow(mc.textRenderer, "Tarcza wroga: UNIESIONA", 0, 0, rgb());
            return;
        }
        Entity t = mc.targetedEntity;
        if (mc.player == null || !(t instanceof PlayerEntity target) || target == mc.player) return;
        boolean up = target.isBlocking();
        ctx.drawTextWithShadow(mc.textRenderer,
                up ? "Tarcza wroga: UNIESIONA" : "Tarcza wroga: opuszczona", 0, 0, up ? 0xFFD10A0A : rgb());
    }
}
