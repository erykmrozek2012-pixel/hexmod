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

/** Szybki wybor tekstur do PvP: klikasz ikonke itemu i od razu edytujesz jego teksture, bez szukania folderow. */
public class PvpItemsScreen extends Screen {
    private record Entry(String label, Item icon, String path) {}

    private static final int COLS = 7;
    private static final int CELL = 40;

    private static final List<Entry> PAGE1 = new ArrayList<>();
    private static final List<Entry> PAGE2 = new ArrayList<>();

    private static void a(String label, Item icon, String name) {
        PAGE1.add(new Entry(label, icon, "textures/item/" + name + ".png"));
    }

    static {
        a("Netherytowy miecz", Items.NETHERITE_SWORD, "netherite_sword");
        a("Diamentowy miecz", Items.DIAMOND_SWORD, "diamond_sword");
        a("Netherytowy topor", Items.NETHERITE_AXE, "netherite_axe");
        a("Diamentowy topor", Items.DIAMOND_AXE, "diamond_axe");
        a("Buzdygan (mace)", Items.MACE, "mace");
        a("Trojzab", Items.TRIDENT, "trident");
        a("Luk", Items.BOW, "bow");
        a("Kusza", Items.CROSSBOW, "crossbow");
        a("Strzala", Items.ARROW, "arrow");
        a("Wind charge", Items.WIND_CHARGE, "wind_charge");
        a("Totem niesmiertelnosci", Items.TOTEM_OF_UNDYING, "totem_of_undying");
        a("Perla enderu", Items.ENDER_PEARL, "ender_pearl");
        a("Zlote jablko", Items.GOLDEN_APPLE, "golden_apple");
        a("Zaczarowane zlote jablko", Items.ENCHANTED_GOLDEN_APPLE, "enchanted_golden_apple");
        a("Krysztal kresu", Items.END_CRYSTAL, "end_crystal");
        a("Netherytowy helm", Items.NETHERITE_HELMET, "netherite_helmet");
        a("Netherytowy napiersnik", Items.NETHERITE_CHESTPLATE, "netherite_chestplate");
        a("Netherytowe spodnie", Items.NETHERITE_LEGGINGS, "netherite_leggings");
        a("Netherytowe buty", Items.NETHERITE_BOOTS, "netherite_boots");
        a("Diamentowy helm", Items.DIAMOND_HELMET, "diamond_helmet");
        a("Diamentowy napiersnik", Items.DIAMOND_CHESTPLATE, "diamond_chestplate");
        a("Diamentowe spodnie", Items.DIAMOND_LEGGINGS, "diamond_leggings");
        a("Diamentowe buty", Items.DIAMOND_BOOTS, "diamond_boots");
        a("Miotana mikstura", Items.SPLASH_POTION, "splash_potion");
        a("Butelka doswiadczenia", Items.EXPERIENCE_BOTTLE, "experience_bottle");
        a("Rakieta", Items.FIREWORK_ROCKET, "firework_rocket");
        a("Elytra", Items.ELYTRA, "elytra");
        PAGE1.add(new Entry("Obsydian", Items.OBSIDIAN, "textures/block/obsidian.png"));

        PAGE2.add(new Entry("Zbroja na postaci: netheryt (gora)", Items.NETHERITE_CHESTPLATE,
                "textures/entity/equipment/humanoid/netherite.png"));
        PAGE2.add(new Entry("Zbroja na postaci: netheryt (spodnie)", Items.NETHERITE_LEGGINGS,
                "textures/entity/equipment/humanoid_leggings/netherite.png"));
        PAGE2.add(new Entry("Zbroja na postaci: diament (gora)", Items.DIAMOND_CHESTPLATE,
                "textures/entity/equipment/humanoid/diamond.png"));
        PAGE2.add(new Entry("Zbroja na postaci: diament (spodnie)", Items.DIAMOND_LEGGINGS,
                "textures/entity/equipment/humanoid_leggings/diamond.png"));
        PAGE2.add(new Entry("Luk: naciaganie 1", Items.BOW, "textures/item/bow_pulling_0.png"));
        PAGE2.add(new Entry("Luk: naciaganie 2", Items.BOW, "textures/item/bow_pulling_1.png"));
        PAGE2.add(new Entry("Luk: naciaganie 3", Items.BOW, "textures/item/bow_pulling_2.png"));
        PAGE2.add(new Entry("Pajeczyna", Items.COBWEB, "textures/block/cobweb.png"));
        PAGE2.add(new Entry("Kotwica odrodzenia", Items.RESPAWN_ANCHOR, "textures/block/respawn_anchor_top.png"));
    }

    private final Screen parent;
    private final Path folder;
    private int page = 0;
    private String status = "";

    public PvpItemsScreen(Screen parent, Path folder) {
        super(Text.literal("Itemy PvP"));
        this.parent = parent;
        this.folder = folder;
    }

    private List<Entry> entries() {
        return page == 0 ? PAGE1 : PAGE2;
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
        addDrawableChild(ButtonWidget.builder(
                Text.literal(page == 0 ? "Dalej: zbroja na postaci >" : "< Wroc: bronie i itemy"), b -> {
            page = 1 - page;
            clearAndInit();
        }).dimensions(cx - 140, by, 138, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Przeladuj tekstury"), b -> {
            client.reloadResources();
            status = "Przeladowuje tekstury...";
        }).dimensions(cx + 2, by, 138, 20).build());
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
        ctx.drawTextWithShadow(textRenderer, "ITEMY PVP", x, 14, HexTheme.RED);
        ctx.drawTextWithShadow(textRenderer, "Kliknij item, aby edytowac jego teksture.", x, 26, HexTheme.TEXT_DIM);

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
