package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

/** Zasoby PvP: ile masz w ekwipunku krysztalow, obsydianu, perel i innych przedmiotow (z ikonami). */
public class ItemCountModule extends Module {
    private record Entry(String key, String label, Item item, boolean def) {}

    private static final List<Entry> ENTRIES = List.of(
            new Entry("crystal", "Crystals", Items.END_CRYSTAL, true),
            new Entry("obsidian", "Obsidian", Items.OBSIDIAN, true),
            new Entry("pearl", "Pearls", Items.ENDER_PEARL, true),
            new Entry("anchor", "Anchors", Items.RESPAWN_ANCHOR, false),
            new Entry("glowstone", "Glowstone", Items.GLOWSTONE, false),
            new Entry("pots", "Splash potions", Items.SPLASH_POTION, false),
            new Entry("xp", "XP bottles", Items.EXPERIENCE_BOTTLE, false),
            new Entry("wind", "Wind charges", Items.WIND_CHARGE, false),
            new Entry("arrow", "Arrows", Items.ARROW, false),
            new Entry("rocket", "Rockets", Items.FIREWORK_ROCKET, false));

    public ItemCountModule() {
        super("item_count", "PvP Items", "Counts of crystals, obsidian, pearls and other items");
        at(10, 308);
        for (Entry e : ENTRIES) option(e.key(), e.label(), e.def());
        option("hide0", "Hide zeros", false);
    }

    private final ItemStack[] stacks = new ItemStack[ENTRIES.size()];
    private final int[] counts = new int[ENTRIES.size()];
    private long countAt = 0;

    private int enabledCount() {
        int n = 0;
        for (Entry e : ENTRIES) if (option(e.key())) n++;
        return n;
    }

    @Override public int w() { return 64; }

    @Override
    public int h() {
        return Math.max(16, enabledCount() * 16);
    }

    /** Jedno przejscie po ekwipunku dla wszystkich przedmiotow, co 100 ms zamiast kilkanascie razy na klatke. */
    private void refreshCounts(MinecraftClient mc) {
        long now = System.currentTimeMillis();
        if (now - countAt < 100 || mc.player == null) return;
        countAt = now;
        java.util.Arrays.fill(counts, 0);
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isEmpty()) continue;
            for (int k = 0; k < ENTRIES.size(); k++) {
                if (s.isOf(ENTRIES.get(k).item())) {
                    counts[k] += s.getCount();
                    break;
                }
            }
        }
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!preview) refreshCounts(mc);
        int y = 0;
        for (int k = 0; k < ENTRIES.size(); k++) {
            Entry e = ENTRIES.get(k);
            if (!option(e.key())) continue;
            int count = preview ? 8 : counts[k];
            if (!preview && count == 0 && option("hide0")) continue;
            if (stacks[k] == null) stacks[k] = e.item().getDefaultStack();
            ctx.drawItem(stacks[k], 0, y);
            ctx.drawTextWithShadow(mc.textRenderer, "x" + count, 18, y + 4, count == 0 && !preview ? 0xFFD10A0A : rgb());
            y += 16;
        }
    }
}
