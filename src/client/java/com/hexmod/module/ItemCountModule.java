package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Zasoby PvP: ile masz w ekwipunku krysztalow, obsydianu, perel i innych przedmiotow (z ikonami). */
public class ItemCountModule extends Module {
    private record Entry(String key, String label, Item item, boolean def) {}

    private static final List<Entry> ENTRIES = List.of(
            new Entry("crystal", "Krysztaly", Items.END_CRYSTAL, true),
            new Entry("obsidian", "Obsydian", Items.OBSIDIAN, true),
            new Entry("pearl", "Perly", Items.ENDER_PEARL, true),
            new Entry("anchor", "Kotwice", Items.RESPAWN_ANCHOR, false),
            new Entry("glowstone", "Glowstone", Items.GLOWSTONE, false),
            new Entry("pots", "Miotane mikstury", Items.SPLASH_POTION, false),
            new Entry("xp", "Butelki XP", Items.EXPERIENCE_BOTTLE, false),
            new Entry("wind", "Wind charge", Items.WIND_CHARGE, false),
            new Entry("arrow", "Strzaly", Items.ARROW, false),
            new Entry("rocket", "Rakiety", Items.FIREWORK_ROCKET, false));

    public ItemCountModule() {
        super("item_count", "PvP Items", "Liczba krysztalow, obsydianu, perel i innych przedmiotow w ekwipunku");
        at(10, 308);
        for (Entry e : ENTRIES) option(e.key(), e.label(), e.def());
        option("hide0", "Ukryj zera", false);
    }

    private List<Entry> enabled() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : ENTRIES) if (option(e.key())) out.add(e);
        return out;
    }

    @Override public int w() { return 64; }

    @Override
    public int h() {
        return Math.max(16, enabled().size() * 16);
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int y = 0;
        for (Entry e : enabled()) {
            int count = 8;
            if (!preview && mc.player != null) {
                count = 0;
                var inv = mc.player.getInventory();
                for (int i = 0; i < inv.size(); i++) {
                    ItemStack s = inv.getStack(i);
                    if (s.isOf(e.item())) count += s.getCount();
                }
            }
            if (!preview && count == 0 && option("hide0")) continue;
            ctx.drawItem(e.item().getDefaultStack(), 0, y);
            ctx.drawTextWithShadow(mc.textRenderer, "x" + count, 18, y + 4, count == 0 && !preview ? 0xFFD10A0A : rgb());
            y += 16;
        }
    }
}
