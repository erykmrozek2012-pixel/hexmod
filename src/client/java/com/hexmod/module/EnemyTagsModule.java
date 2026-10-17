package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Wskazniki nad glowa wroga: HP, zbroja (male ikony przedmiotow z Twojego texture packa + zuzycie),
 * licznik zbitych totemow i tarcza. Wyglad (skala, kolor, pozycja, elementy) ustawiasz w edytorze HUD.
 */
public class EnemyTagsModule extends Module {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int ROW = 10;

    private record ArmorEntry(ItemStack stack, String text, int color) {}
    private record TagData(float hp, float maxHp, List<ArmorEntry> armor, int pops, boolean blocking) {}
    private record Row(String text, int color, List<ArmorEntry> armor) {}

    private static final Map<UUID, Integer> POPS = new HashMap<>();
    private final Map<UUID, Integer> lastAbsorption = new HashMap<>();
    private Object lastWorld = null;

    public EnemyTagsModule() {
        super("enemy_tags", "Wskazniki wroga", "HP, zbroja z ikonami, zbite totemy i tarcza nad glowa wroga");
        at(0, -2); // przesuniecie wzgledem domyslnej pozycji nad glowa
        option("hp", "HP", true);
        option("armor", "Zbroja", true);
        option("pops", "Totemy", true);
        option("shield", "Tarcza", true);
        option("pct", "Procenty", false);
    }

    @Override
    protected boolean overlay() {
        return true;
    }

    @Override
    public boolean hasOverlayPreview() {
        return true;
    }

    // ---------- wykrywanie zbitych totemow ----------
    // Totem daje Absorption II na 5 sekund; nowe pojawienie sie tego efektu = zbity totem.

    private static int totemAbsorption(PlayerEntity pl) {
        for (StatusEffectInstance e : pl.getStatusEffects()) {
            if (e.getEffectType().equals(StatusEffects.ABSORPTION) && e.getAmplifier() == 1 && e.getDuration() <= 100) {
                return e.getDuration();
            }
        }
        return -1;
    }

    @Override
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        if (mc.world != lastWorld) {
            lastWorld = mc.world;
            POPS.clear();
            lastAbsorption.clear();
        }
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl == mc.player) continue;
            UUID id = pl.getUuid();
            int dur = totemAbsorption(pl);
            boolean seen = lastAbsorption.containsKey(id);
            int prev = lastAbsorption.getOrDefault(id, -1);
            if (seen && dur >= 0 && (prev < 0 || dur > prev)) POPS.merge(id, 1, Integer::sum);
            lastAbsorption.put(id, dur);
        }
    }

    // ---------- dane i uklad ----------

    private int pctColor(float pct) {
        if (pct > 0.66f) return color;
        if (pct > 0.33f) return 0xFFD60A;
        return 0xD10A0A;
    }

    private ArmorEntry entry(ItemStack s) {
        if (!s.isDamageable()) return new ArmorEntry(s, "-", color);
        float pct = (float) (s.getMaxDamage() - s.getDamage()) / s.getMaxDamage();
        String text = option("pct") ? Math.round(pct * 100) + "%" : String.valueOf(s.getMaxDamage() - s.getDamage());
        return new ArmorEntry(s, text, pctColor(pct));
    }

    private TagData dataFor(PlayerEntity pl) {
        List<ArmorEntry> armor = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            ItemStack s = pl.getEquippedStack(slot);
            if (!s.isEmpty()) armor.add(entry(s));
        }
        return new TagData(pl.getHealth() + pl.getAbsorptionAmount(), pl.getMaxHealth(), armor,
                POPS.getOrDefault(pl.getUuid(), 0), pl.isBlocking());
    }

    private TagData sample() {
        List<ArmorEntry> armor = new ArrayList<>();
        ItemStack[] stacks = {
                Items.NETHERITE_HELMET.getDefaultStack(), Items.NETHERITE_CHESTPLATE.getDefaultStack(),
                Items.NETHERITE_LEGGINGS.getDefaultStack(), Items.NETHERITE_BOOTS.getDefaultStack()};
        String[] uses = {"407", "592", "555", "481"};
        String[] pcts = {"87%", "74%", "52%", "30%"};
        float[] fr = {0.87f, 0.74f, 0.52f, 0.30f};
        for (int i = 0; i < 4; i++) {
            armor.add(new ArmorEntry(stacks[i], option("pct") ? pcts[i] : uses[i], pctColor(fr[i])));
        }
        return new TagData(14.5f, 20f, armor, 2, true);
    }

    private List<Row> rows(TagData d) {
        List<Row> rows = new ArrayList<>();
        if (option("shield") && d.blocking()) rows.add(new Row("Tarcza!", 0xD10A0A, null));
        if (option("pops") && d.pops() > 0) rows.add(new Row("Totemy zbite: " + d.pops(), color, null));
        if (option("armor") && !d.armor().isEmpty()) rows.add(new Row(null, color, d.armor()));
        if (option("hp")) {
            float ratio = d.hp() / Math.max(1f, d.maxHp());
            int c = ratio > 0.66f ? 0x30D158 : ratio > 0.33f ? 0xFFD60A : 0xD10A0A;
            rows.add(new Row(String.format(Locale.ROOT, "HP %.1f", d.hp()), c, null));
        }
        return rows;
    }

    private static int rowWidth(Row r, TextRenderer tr) {
        if (r.armor() == null) return tr.getWidth(r.text());
        int w = 0;
        for (ArmorEntry a : r.armor()) w += 8 + 1 + tr.getWidth(a.text()) + 4;
        return Math.max(0, w - 4);
    }

    private static int[] measure(List<Row> rows, TextRenderer tr) {
        int w = 0;
        for (Row r : rows) w = Math.max(w, rowWidth(r, tr));
        return new int[]{Math.max(w, 30), Math.max(rows.size() * ROW, ROW)};
    }

    // ---------- rysowanie ----------

    private void drawRows(DrawContext ctx, List<Row> rows, int ax, int ay) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int[] size = measure(rows, tr);
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) (ax + x), (float) (ay + y));
        m.scale(scale, scale);
        int cy = -size[1];
        for (Row r : rows) {
            int cx = -rowWidth(r, tr) / 2;
            if (r.armor() == null) {
                ctx.drawTextWithShadow(tr, r.text(), cx, cy, 0xFF000000 | r.color());
            } else {
                for (ArmorEntry a : r.armor()) {
                    m.pushMatrix();
                    m.translate((float) cx, (float) cy);
                    m.scale(0.5f, 0.5f);
                    ctx.drawItem(a.stack(), 0, 0);
                    m.popMatrix();
                    int tw = tr.getWidth(a.text());
                    ctx.drawTextWithShadow(tr, a.text(), cx + 9, cy, 0xFF000000 | a.color());
                    cx += 8 + 1 + tw + 4;
                }
            }
            cy += ROW;
        }
        m.popMatrix();
    }

    @Override
    protected void draw(DrawContext ctx, boolean preview) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (preview || mc.player == null || mc.world == null) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;
        float tp = mc.getRenderTickCounter().getTickProgress(false);

        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl == mc.player || pl.isSpectator() || pl.isInvisible()) continue;
            if (mc.player.squaredDistanceTo(pl) > 48.0 * 48.0 || !mc.player.canSee(pl)) continue;
            float[] s = Projection.project(ctx, pl.getLerpedPos(tp).add(0, pl.getHeight() + 0.55, 0));
            if (s == null) continue;
            List<Row> rows = rows(dataFor(pl));
            if (rows.isEmpty()) continue;
            drawRows(ctx, rows, Math.round(s[0]), Math.round(s[1]));
        }
    }

    // ---------- podglad w edytorze HUD ----------

    @Override
    public void drawOverlayPreview(DrawContext ctx, int ax, int ay) {
        List<Row> rows = rows(sample());
        if (!rows.isEmpty()) drawRows(ctx, rows, ax, ay);
    }

    @Override
    public int[] overlayPreviewBox(int ax, int ay) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int[] size = measure(rows(sample()), tr);
        int w = Math.round(size[0] * scale);
        int h = Math.round(size[1] * scale);
        int left = ax + x - w / 2;
        int bottom = ay + y;
        return new int[]{left, bottom - h, left + w, bottom};
    }
}
