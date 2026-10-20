package com.hexmod.module;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** Wytrzymalosc zbroi wroga nad glowa (srednia ze wszystkich zalozonych czesci). */
public class EnemyArmorModule extends OverheadModule {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public EnemyArmorModule() {
        super("enemy_armor", "Zbroja wroga", "Ile wytrzymalosci zbroi ma wrog (nad glowa)", 10);
    }

    private float[] stats(PlayerEntity p) {
        int count = 0;
        float sum = 0f;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack s = p.getEquippedStack(slot);
            if (s.isEmpty()) continue;
            count++;
            sum += s.isDamageable() ? (float) (s.getMaxDamage() - s.getDamage()) / s.getMaxDamage() : 1f;
        }
        return new float[]{count, count == 0 ? 0f : sum / count};
    }

    @Override
    protected String text(PlayerEntity p) {
        float[] st = stats(p);
        if (st[0] == 0) return "Zbroja: brak";
        return "Zbroja " + Math.round(st[1] * 100) + "% (" + (int) st[0] + "/4)";
    }

    @Override
    protected int colorOf(PlayerEntity p) {
        float[] st = stats(p);
        if (st[0] == 0) return 0x9A9A9A;
        if (st[1] > 0.66f) return 0x64D2FF;
        if (st[1] > 0.33f) return 0xFFD60A;
        return 0xD10A0A;
    }
}
