package com.hexmod.gui;

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
        item(w, "Netherytowy miecz", Items.NETHERITE_SWORD, "netherite_sword");
        item(w, "Diamentowy miecz", Items.DIAMOND_SWORD, "diamond_sword");
        item(w, "Zelazny miecz", Items.IRON_SWORD, "iron_sword");
        item(w, "Kamienny miecz", Items.STONE_SWORD, "stone_sword");
        item(w, "Zloty miecz", Items.GOLDEN_SWORD, "golden_sword");
        item(w, "Netherytowy topor", Items.NETHERITE_AXE, "netherite_axe");
        item(w, "Diamentowy topor", Items.DIAMOND_AXE, "diamond_axe");
        item(w, "Zelazny topor", Items.IRON_AXE, "iron_axe");
        item(w, "Kamienny topor", Items.STONE_AXE, "stone_axe");
        item(w, "Buzdygan (mace)", Items.MACE, "mace");
        item(w, "Trojzab", Items.TRIDENT, "trident");
        item(w, "Luk", Items.BOW, "bow");
        item(w, "Kusza", Items.CROSSBOW, "crossbow");
        item(w, "Strzala", Items.ARROW, "arrow");
        item(w, "Strzala spektralna", Items.SPECTRAL_ARROW, "spectral_arrow");
        item(w, "Wind charge", Items.WIND_CHARGE, "wind_charge");
        item(w, "Kij burzy (breeze rod)", Items.BREEZE_ROD, "breeze_rod");
        item(w, "Wedka", Items.FISHING_ROD, "fishing_rod");
        item(w, "Sniezka", Items.SNOWBALL, "snowball");
        item(w, "Jajko", Items.EGG, "egg");
        item(w, "Krzesiwo", Items.FLINT_AND_STEEL, "flint_and_steel");
        item(w, "Kula ognia", Items.FIRE_CHARGE, "fire_charge");
        item(w, "Netherytowy kilof", Items.NETHERITE_PICKAXE, "netherite_pickaxe");
        item(w, "Diamentowy kilof", Items.DIAMOND_PICKAXE, "diamond_pickaxe");
        item(w, "Netherytowa lopata", Items.NETHERITE_SHOVEL, "netherite_shovel");
        item(w, "Diamentowa lopata", Items.DIAMOND_SHOVEL, "diamond_shovel");
        PAGES.add(new Page("Bronie i narzedzia", w));

        // --- Strona 2: zbroja (ikony itemow) ---
        List<Entry> a = new ArrayList<>();
        item(a, "Netherytowy helm", Items.NETHERITE_HELMET, "netherite_helmet");
        item(a, "Netherytowy napiersnik", Items.NETHERITE_CHESTPLATE, "netherite_chestplate");
        item(a, "Netherytowe spodnie", Items.NETHERITE_LEGGINGS, "netherite_leggings");
        item(a, "Netherytowe buty", Items.NETHERITE_BOOTS, "netherite_boots");
        item(a, "Diamentowy helm", Items.DIAMOND_HELMET, "diamond_helmet");
        item(a, "Diamentowy napiersnik", Items.DIAMOND_CHESTPLATE, "diamond_chestplate");
        item(a, "Diamentowe spodnie", Items.DIAMOND_LEGGINGS, "diamond_leggings");
        item(a, "Diamentowe buty", Items.DIAMOND_BOOTS, "diamond_boots");
        item(a, "Zelazny helm", Items.IRON_HELMET, "iron_helmet");
        item(a, "Zelazny napiersnik", Items.IRON_CHESTPLATE, "iron_chestplate");
        item(a, "Zelazne spodnie", Items.IRON_LEGGINGS, "iron_leggings");
        item(a, "Zelazne buty", Items.IRON_BOOTS, "iron_boots");
        item(a, "Kolczuzny helm", Items.CHAINMAIL_HELMET, "chainmail_helmet");
        item(a, "Kolczuzny napiersnik", Items.CHAINMAIL_CHESTPLATE, "chainmail_chestplate");
        item(a, "Kolczuzne spodnie", Items.CHAINMAIL_LEGGINGS, "chainmail_leggings");
        item(a, "Kolczuzne buty", Items.CHAINMAIL_BOOTS, "chainmail_boots");
        item(a, "Zloty helm", Items.GOLDEN_HELMET, "golden_helmet");
        item(a, "Zloty napiersnik", Items.GOLDEN_CHESTPLATE, "golden_chestplate");
        item(a, "Zlote spodnie", Items.GOLDEN_LEGGINGS, "golden_leggings");
        item(a, "Zlote buty", Items.GOLDEN_BOOTS, "golden_boots");
        item(a, "Skorzany helm", Items.LEATHER_HELMET, "leather_helmet");
        item(a, "Skorzany napiersnik", Items.LEATHER_CHESTPLATE, "leather_chestplate");
        item(a, "Skorzane spodnie", Items.LEATHER_LEGGINGS, "leather_leggings");
        item(a, "Skorzane buty", Items.LEATHER_BOOTS, "leather_boots");
        item(a, "Zolwi helm", Items.TURTLE_HELMET, "turtle_helmet");
        PAGES.add(new Page("Zbroja (ikony)", a));

        // --- Strona 3: jedzenie, mikstury, krysztaly i inne ---
        List<Entry> o = new ArrayList<>();
        item(o, "Zlote jablko", Items.GOLDEN_APPLE, "golden_apple");
        item(o, "Zaczarowane zlote jablko", Items.ENCHANTED_GOLDEN_APPLE, "enchanted_golden_apple");
        item(o, "Pieczona wolowina", Items.COOKED_BEEF, "cooked_beef");
        item(o, "Pieczony schab", Items.COOKED_PORKCHOP, "cooked_porkchop");
        item(o, "Zlota marchewka", Items.GOLDEN_CARROT, "golden_carrot");
        item(o, "Owoc refrenu (chorus)", Items.CHORUS_FRUIT, "chorus_fruit");
        item(o, "Perla enderu", Items.ENDER_PEARL, "ender_pearl");
        item(o, "Oko enderu", Items.ENDER_EYE, "ender_eye");
        item(o, "Totem niesmiertelnosci", Items.TOTEM_OF_UNDYING, "totem_of_undying");
        item(o, "Mikstura", Items.POTION, "potion");
        item(o, "Miotana mikstura", Items.SPLASH_POTION, "splash_potion");
        item(o, "Mikstura zalegajaca", Items.LINGERING_POTION, "lingering_potion");
        item(o, "Szklana butelka", Items.GLASS_BOTTLE, "glass_bottle");
        item(o, "Butelka doswiadczenia", Items.EXPERIENCE_BOTTLE, "experience_bottle");
        item(o, "Rakieta", Items.FIREWORK_ROCKET, "firework_rocket");
        item(o, "Elytra", Items.ELYTRA, "elytra");
        item(o, "Krysztal kresu", Items.END_CRYSTAL, "end_crystal");
        item(o, "Wiadro lawy", Items.LAVA_BUCKET, "lava_bucket");
        item(o, "Wiadro wody", Items.WATER_BUCKET, "water_bucket");
        item(o, "Wiadro sniegu", Items.POWDER_SNOW_BUCKET, "powder_snow_bucket");
        item(o, "Wiadro", Items.BUCKET, "bucket");
        tex(o, "Obsydian", Items.OBSIDIAN, "textures/block/obsidian.png");
        tex(o, "Placzacy obsydian", Items.CRYING_OBSIDIAN, "textures/block/crying_obsidian.png");
        tex(o, "Pajeczyna", Items.COBWEB, "textures/block/cobweb.png");
        tex(o, "Kotwica odrodzenia (gora)", Items.RESPAWN_ANCHOR, "textures/block/respawn_anchor_top.png");
        tex(o, "Jasnopyl (glowstone)", Items.GLOWSTONE, "textures/block/glowstone.png");
        tex(o, "TNT (bok)", Items.TNT, "textures/block/tnt_side.png");
        PAGES.add(new Page("Jedzenie, mikstury, krysztaly i inne", o));

        // --- Strona 4: zbroja na postaci i inne tekstury modeli ---
        List<Entry> p = new ArrayList<>();
        String[][] mats = {{"netheryt", "netherite"}, {"diament", "diamond"}, {"zelazo", "iron"},
                {"kolczuga", "chainmail"}, {"zloto", "gold"}, {"skora", "leather"}};
        Item[] chest = {Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.IRON_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE};
        Item[] legs = {Items.NETHERITE_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.IRON_LEGGINGS,
                Items.CHAINMAIL_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.LEATHER_LEGGINGS};
        for (int i = 0; i < mats.length; i++) {
            tex(p, "Na postaci: " + mats[i][0] + " (gora)", chest[i],
                    "textures/entity/equipment/humanoid/" + mats[i][1] + ".png");
            tex(p, "Na postaci: " + mats[i][0] + " (spodnie)", legs[i],
                    "textures/entity/equipment/humanoid_leggings/" + mats[i][1] + ".png");
        }
        tex(p, "Na postaci: skora, nakladka (gora)", Items.LEATHER_CHESTPLATE,
                "textures/entity/equipment/humanoid/leather_overlay.png");
        tex(p, "Na postaci: skora, nakladka (spodnie)", Items.LEATHER_LEGGINGS,
                "textures/entity/equipment/humanoid_leggings/leather_overlay.png");
        tex(p, "Elytra na postaci", Items.ELYTRA, "textures/entity/equipment/wings/elytra.png");
        tex(p, "Tarcza (tekstura modelu)", Items.SHIELD, "textures/entity/shield/shield_base_nopattern.png");
        item(p, "Luk: naciaganie 1", Items.BOW, "bow_pulling_0");
        item(p, "Luk: naciaganie 2", Items.BOW, "bow_pulling_1");
        item(p, "Luk: naciaganie 3", Items.BOW, "bow_pulling_2");
        item(p, "Kusza: naciagnieta", Items.CROSSBOW, "crossbow_pulling_2");
        item(p, "Wedka: zarzucona", Items.FISHING_ROD, "fishing_rod_cast");
        PAGES.add(new Page("Zbroja na postaci i modele", p));
    }

    private final Screen parent;
    private final Path folder;
    private int page = Math.min(lastPage, PAGES.size() - 1);
    private String status = "";

    public PvpItemsScreen(Screen parent, Path folder) {
        super(Text.literal("Itemy PvP"));
        this.parent = parent;
        this.folder = folder;
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
        addDrawableChild(ButtonWidget.builder(Text.literal("< Poprzednia"), b -> go(-1))
                .dimensions(cx - 140, by, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Nastepna >"), b -> go(1))
                .dimensions(cx - 45, by, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Przeladuj"), b -> {
            client.reloadResources();
            status = "Przeladowuje tekstury...";
        }).dimensions(cx + 50, by, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Wstecz"), b -> client.setScreen(parent))
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
        ctx.drawTextWithShadow(textRenderer, "ITEMY PVP  " + (page + 1) + "/" + PAGES.size(), x, 14, HexTheme.RED);
        ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(PAGES.get(page).title(), 280), x, 26, HexTheme.TEXT_DIM);

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
            ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(hovered.label(), 280), x, ly, HexTheme.TEXT);
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
