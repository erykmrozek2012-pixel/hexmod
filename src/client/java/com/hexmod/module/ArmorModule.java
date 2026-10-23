package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/** Wytrzymalosc zbroi w procentach. */
public class ArmorModule extends Module {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final String[] LABELS = {"Helmet", "Chestplate", "Leggings", "Boots"};

    public ArmorModule() {
        super("armor", "Armor Status", "Armor durability in percent");
        at(120, 22);
    }

    @Override public int w() { return 100; }
    @Override public int h() { return 48; }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        int line = 0;
        for (int i = 0; i < SLOTS.length; i++) {
            String t;
            int col = rgb();
            if (preview) {
                t = L.t(LABELS[i]) + ": 87%";
            } else {
                ItemStack s = mc.player.getEquippedStack(SLOTS[i]);
                if (s.isEmpty() || !s.isDamageable()) continue;
                int pct = Math.round(100f * (s.getMaxDamage() - s.getDamage()) / s.getMaxDamage());
                t = L.t(LABELS[i]) + ": " + pct + "%";
                if (pct <= 15) col = 0xFFD10A0A;
                else if (pct <= 35) col = 0xFFFF9500;
            }
            ctx.drawTextWithShadow(mc.textRenderer, t, 0, line, col);
            line += 12;
        }
    }
}
