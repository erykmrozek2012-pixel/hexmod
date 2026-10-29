package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

/** Pokazuje, czy w tej chwili trafisz cios krytyczny (spadasz, nie sprintujesz, nie jestes w wodzie ani na drabinie). */
public class CritModule extends Module {
    public CritModule() {
        super("crit", "Crit Ready", "Shows when your next hit would be a critical hit");
        at(130, 106);
    }

    @Override public int w() { return 90; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        boolean ready = preview;
        if (!preview && p != null) {
            ready = p.fallDistance > 0.0 && !p.isOnGround() && !p.isClimbing() && !p.isTouchingWater()
                    && !p.isSprinting() && p.getAttackCooldownProgress(0f) > 0.9f;
        }
        ctx.drawTextWithShadow(mc.textRenderer, ready ? L.t("CRIT READY") : L.t("Crit: no"), 0, 0,
                ready ? 0xFF30D158 : 0xFF9A9A9A);
    }
}
