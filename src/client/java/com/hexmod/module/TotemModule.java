package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;

/** Licznik totemow nieśmiertelności w ekwipunku i drugiej rece. */
public class TotemModule extends Module {
    public TotemModule() {
        super("totems", "Totem Count", "How many totems you carry");
        at(10, 124);
    }

    @Override public int w() { return 80; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity p = mc.player;
        if (p == null) return;
        int n;
        if (preview) {
            n = 3;
        } else {
            n = 0;
            PlayerInventory inv = p.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                if (inv.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) n += inv.getStack(i).getCount();
            }
            if (inv.size() <= 36 && p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) n++;
        }
        ctx.drawTextWithShadow(mc.textRenderer, L.t("Totems: ") + n, 0, 0, n == 0 ? 0xFFD10A0A : rgb());
    }
}
