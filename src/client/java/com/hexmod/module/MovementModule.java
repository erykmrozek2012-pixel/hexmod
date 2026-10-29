package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;

/** Stan ruchu: sprint, skradanie, lot albo chodzenie. */
public class MovementModule extends Module {
    public MovementModule() {
        super("movement", "Movement State", "Sprinting, sneaking or flying");
        at(130, 130);
    }

    @Override public int w() { return 90; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        String s = L.t("Sprinting");
        if (!preview && p != null) {
            if (p.getAbilities().flying) s = L.t("Flying");
            else if (p.isSneaking()) s = L.t("Sneaking");
            else if (p.isSprinting()) s = L.t("Sprinting");
            else s = L.t("Walking");
        }
        ctx.drawTextWithShadow(mc.textRenderer, s, 0, 0, rgb());
    }
}
