package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

/** Naladowanie ataku broni (pasek i procent), zeby bic dopiero przy pelnym ciosie. */
public class AttackCooldownModule extends Module {
    public AttackCooldownModule() {
        super("attack_cooldown", "Attack Cooldown", "Weapon charge before the next full hit");
        at(130, 58);
    }

    @Override public int w() { return 80; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        float c = preview ? 0.7f : (p == null ? 1f : p.getAttackCooldownProgress(0f));
        c = Math.max(0f, Math.min(1f, c));
        int col = c >= 0.999f ? 0xFF30D158 : 0xFFFFD60A;
        ctx.drawTextWithShadow(mc.textRenderer, L.t("Charge ") + Math.round(c * 100f) + "%", 0, 0, col);
        ctx.fill(0, 9, 70, 12, 0x66000000);
        ctx.fill(0, 9, Math.round(70 * c), 12, col);
    }
}
