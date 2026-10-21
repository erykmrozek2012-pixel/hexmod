package com.hexmod.gui;

import com.hexmod.module.L;
import com.hexmod.module.PackTools;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Szybki wybor tekstur do PvP: wszystkie itemy uzywane w PvP na kilku stronach, klikasz ikone i edytujesz teksture. */
public class PvpItemsScreen extends Screen {
    private record Entry(String label, Item icon, String path) {}

    private record Page(String title, List<Entry> entries) {}

    private static final int COLS = 7;
    private static final int CELL = 40;
    private static final List<Page> PAGES = new ArrayList<>();
    private static int lastPage = 0;

    private static void item(List<Entry> l, String label, Item icon, String name) {
        l.add(new Entry(label, icon, "textures/item/" + name + ".png"));
    }

    private static void tex(List<Entry> l, String label, Item icon, String path) {
        l.add(new Entry(label, icon, path));
    }

    static {
        // --- Strona 1: bronie i narzedzia ---
        List<Entry> w = new ArrayList<>();
        item(w, "", Items.NETHERITE_SWORD, "netherite_sword");
        item(w, "", Items.DIAMOND_SWORD, "diamond_sword");
        item(w, "", Items.IRON_SWORD, "iron_sword");
        item(w, "", Items.STONE_SWORD, "stone_sword");
        item(w, "", Items.GOLDEN_SWORD, "golden_sword");
        item(w, "", Items.NETHERITE_AXE, "netherite_axe");
        item(w, "", Items.DIAMOND_AXE, "diamond_axe");
        item(w, "", Items.IRON_AXE, "iron_axe");
        item(w, "", Items.STONE_AXE, "stone_axe");
        item(w, "", Items.MACE, "mace");
        item(w, "", Items.TRIDENT, "trident");
        item(w, "", Items.BOW, "bow");
        item(w, "", Items.CROSSBOW, "crossbow");
        item(w, "", Items.ARROW, "arrow");
        item(w, "", Items.SPECTRAL_ARROW, "spectral_arrow");
        item(w, "", Items.WIND_CHARGE, "wind_charge");
        item(w, "", Items.BREEZE_ROD, "breeze_rod");
        item(w, "", Items.FISHING_ROD, "fishing_rod");
        item(w, "", Items.SNOWBALL, "snowball");
        item(w, "", Items.EGG, "egg");
        item(w, "", Items.FLINT_AND_STEEL, "flint_and_steel");
        item(w, "", Items.FIRE_CHARGE, "fire_charge");
        item(w, "", Items.NETHERITE_PICKAXE, "netherite_pickaxe");
        item(w, "", Items.DIAMOND_PICKAXE, "diamond_pickaxe");
        item(w, "", Items.NETHERITE_SHOVEL, "netherite_shovel");
        item(w, "", Items.DIAMOND_SHOVEL, "diamond_shovel");
        PAGES.add(new Page("Weapons and tools", w));

        // --- Strona 2: zbroja (ikony itemow) ---
        List<Entry> a = new ArrayList<>();
        item(a, "", Items.NETHERITE_HELMET, "netherite_helmet");
        item(a, "", Items.NETHERITE_CHESTPLATE, "netherite_chestplate");
        item(a, "", Items.NETHERITE_LEGGINGS, "netherite_leggings");
        item(a, "", Items.NETHERITE_BOOTS, "netherite_boots");
        item(a, "", Items.DIAMOND_HELMET, "diamond_helmet");
        item(a, "", Items.DIAMOND_CHESTPLATE, "diamond_chestplate");
        item(a, "", Items.DIAMOND_LEGGINGS, "diamond_leggings");
        item(a, "", Items.DIAMOND_BOOTS, "diamond_boots");
        item(a, "", Items.IRON_HELMET, "iron_helmet");
        item(a, "", Items.IRON_CHESTPLATE, "iron_chestplate");
        item(a, "", Items.IRON_LEGGINGS, "iron_leggings");
        item(a, "", Items.IRON_BOOTS, "iron_boots");
        item(a, "", Items.CHAINMAIL_HELMET, "chainmail_helmet");
        item(a, "", Items.CHAINMAIL_CHESTPLATE, "chainmail_chestplate");
        item(a, "", Items.CHAINMAIL_LEGGINGS, "chainmail_leggings");
        item(a, "", Items.CHAINMAIL_BOOTS, "chainmail_boots");
        item(a, "", Items.GOLDEN_HELMET, "golden_helmet");
        item(a, "", Items.GOLDEN_CHESTPLATE, "golden_chestplate");
        item(a, "", Items.GOLDEN_LEGGINGS, "golden_leggings");
        item(a, "", Items.GOLDEN_BOOTS, "golden_boots");
        item(a, "", Items.LEATHER_HELMET, "leather_helmet");
        item(a, "", Items.LEATHER_CHESTPLATE, "leather_chestplate");
        item(a, "", Items.LEATHER_LEGGINGS, "leather_leggings");
        item(a, "", Items.LEATHER_BOOTS, "leather_boots");
        item(a, "", Items.TURTLE_HELMET, "turtle_helmet");
        PAGES.add(new Page("Armor (icons)", a));

        // --- Strona 3: jedzenie, mikstury, krysztaly i inne ---
        List<Entry> o = new ArrayList<>();
        item(o, "", Items.GOLDEN_APPLE, "golden_apple");
        item(o, "", Items.ENCHANTED_GOLDEN_APPLE, "enchanted_golden_apple");
        item(o, "", Items.COOKED_BEEF, "cooked_beef");
        item(o, "", Items.COOKED_PORKCHOP, "cooked_porkchop");
        item(o, "", Items.GOLDEN_CARROT, "golden_carrot");
        item(o, "", Items.CHORUS_FRUIT, "chorus_fruit");
        item(o, "", Items.ENDER_PEARL, "ender_pearl");
        item(o, "", Items.ENDER_EYE, "ender_eye");
        item(o, "", Items.TOTEM_OF_UNDYING, "totem_of_undying");
        item(o, "", Items.POTION, "potion");
        item(o, "", Items.SPLASH_POTION, "splash_potion");
        item(o, "", Items.LINGERING_POTION, "lingering_potion");
        item(o, "", Items.GLASS_BOTTLE, "glass_bottle");
        item(o, "", Items.EXPERIENCE_BOTTLE, "experience_bottle");
        item(o, "", Items.FIREWORK_ROCKET, "firework_rocket");
        item(o, "", Items.ELYTRA, "elytra");
        item(o, "", Items.END_CRYSTAL, "end_crystal");
        item(o, "", Items.LAVA_BUCKET, "lava_bucket");
        item(o, "", Items.WATER_BUCKET, "water_bucket");
        item(o, "", Items.POWDER_SNOW_BUCKET, "powder_snow_bucket");
        item(o, "", Items.BUCKET, "bucket");
        tex(o, "", Items.OBSIDIAN, "textures/block/obsidian.png");
        tex(o, "", Items.CRYING_OBSIDIAN, "textures/block/crying_obsidian.png");
        tex(o, "", Items.COBWEB, "textures/block/cobweb.png");
        tex(o, "", Items.RESPAWN_ANCHOR, "textures/block/respawn_anchor_top.png");
        tex(o, "", Items.GLOWSTONE, "textures/block/glowstone.png");
        tex(o, "", Items.TNT, "textures/block/tnt_side.png");
        PAGES.add(new Page("Food, potions, crystals and more", o));

        // --- Strona 4: zbroja na postaci i inne tekstury modeli ---
        List<Entry> p = new ArrayList<>();
        String[] mats = {"netherite", "diamond", "iron", "chainmail", "gold", "leather"};
        Item[] chest = {Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.IRON_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE};
        Item[] legs = {Items.NETHERITE_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.IRON_LEGGINGS,
                Items.CHAINMAIL_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.LEATHER_LEGGINGS};
        for (int i = 0; i < mats.length; i++) {
            tex(p, "player:top", chest[i], "textures/entity/equipment/humanoid/" + mats[i] + ".png");
            tex(p, "player:legs", legs[i], "textures/entity/equipment/humanoid_leggings/" + mats[i] + ".png");
        }
        tex(p, "player:top overlay", Items.LEATHER_CHESTPLATE, "textures/entity/equipment/humanoid/leather_overlay.png");
        tex(p, "player:legs overlay", Items.LEATHER_LEGGINGS, "textures/entity/equipment/humanoid_leggings/leather_overlay.png");
        tex(p, "Elytra on player", Items.ELYTRA, "textures/entity/equipment/wings/elytra.png");
        tex(p, "Shield (model texture)", Items.SHIELD, "textures/entity/shield/shield_base_nopattern.png");
        item(p, "Bow: pulling 1", Items.BOW, "bow_pulling_0");
        item(p, "Bow: pulling 2", Items.BOW, "bow_pulling_1");
        item(p, "Bow: pulling 3", Items.BOW, "bow_pulling_2");
        item(p, "Crossbow: loaded", Items.CROSSBOW, "crossbow_pulling_2");
        item(p, "Fishing rod: cast", Items.FISHING_ROD, "fishing_rod_cast");
        PAGES.add(new Page("Armor on player and models", p));
    }

    private final Screen parent;
    private final Path folder;
    private int page = Math.min(lastPage, PAGES.size() - 1);
    private String status = "";

    public PvpItemsScreen(Screen parent, Path folder) {
        super(Text.literal(L.t("PvP Items")));
        this.parent = parent;
        this.folder = folder;
    }

    /** Nazwa wpisu: nazwa przedmiotu z gry albo przetlumaczona etykieta. */
    private static String labelOf(Entry e) {
        String l = e.label();
        String itemName = e.icon().getName().getString();
        if (l.isEmpty()) return itemName;
        if (l.startsWith("player:")) return L.t("On player") + ": " + itemName + " (" + L.t(l.substring(7)) + ")";
        return L.t(l);
    }

    private List<Entry> entries() {
        return PAGES.get(page).entries();
    }

    private int x0() { return width / 2 - 140; }

    private int y0() { return 40; }

    private void open(Entry e) {
        try {
            Path file = PackTools.ensureTexture(folder, e.path());
            client.setScreen(new PixelEditorScreen(this, file));
        } catch (IOException ex) {
            status = ex.getMessage();
        }
    }

    private void go(int delta) {
        page = (page + delta + PAGES.size()) % PAGES.size();
        lastPage = page;
        status = "";
        clearAndInit();
    }

    @Override
    protected void init() {
        int cx = width / 2;
        List<Entry> list = entries();
        for (int i = 0; i < list.size(); i++) {
            final Entry e = list.get(i);
            addDrawableChild(ButtonWidget.builder(Text.empty(), b -> open(e))
                    .dimensions(x0() + (i % COLS) * CELL, y0() + (i / COLS) * CELL, CELL - 2, CELL - 2).build());
        }
        int by = height - 54;
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("< Previous")), b -> go(-1))
                .dimensions(cx - 140, by, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Next >")), b -> go(1))
                .dimensions(cx - 45, by, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Reload")), b -> {
            client.reloadResources();
            status = L.t("Reloading textures...");
        }).dimensions(cx + 50, by, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Back")), b -> client.setScreen(parent))
                .dimensions(cx - 50, height - 28, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(width / 2 - 150, 4, width / 2 + 150, height - 2, HexTheme.PANEL);
        ctx.fill(width / 2 - 150, 4, width / 2 + 150, 7, HexTheme.RED);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int x = x0();
        ctx.drawTextWithShadow(textRenderer, L.t("PVP ITEMS  ") + (page + 1) + "/" + PAGES.size(), x, 14, HexTheme.RED);
        ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(L.t(PAGES.get(page).title()), 280), x, 26, HexTheme.TEXT_DIM);

        List<Entry> list = entries();
        Entry hovered = null;
        var m = ctx.getMatrices();
        for (int i = 0; i < list.size(); i++) {
            int bx = x + (i % COLS) * CELL, by = y0() + (i / COLS) * CELL;
            m.pushMatrix();
            m.translate((float) (bx + 3), (float) (by + 3));
            m.scale(2f, 2f);
            ctx.drawItem(list.get(i).icon().getDefaultStack(), 0, 0);
            m.popMatrix();
            if (mouseX >= bx && mouseX < bx + CELL - 2 && mouseY >= by && mouseY < by + CELL - 2) hovered = list.get(i);
        }
        int rows = (list.size() + COLS - 1) / COLS;
        int ly = y0() + rows * CELL + 4;
        if (hovered != null) {
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(labelOf(hovered), 280), x, ly, HexTheme.TEXT);
        }
        if (!status.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(status, 280), x, ly + 12, HexTheme.RED);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
