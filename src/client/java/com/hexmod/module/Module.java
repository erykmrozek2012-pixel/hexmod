package com.hexmod.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Modul Hexmoda: widget HUD z pozycja, skala, kolorem i opcjami, ktory mozna wlaczyc/wylaczyc. */
public class Module {
    public static final int CENTER = -1;

    /** Globalny przelacznik: gdy true, widoczne sa tylko podstawowe widgety. */
    public static boolean extrasHidden = false;
    private static final Set<String> CAT_PVP = Set.of("enemy_tags", "enemy_shield", "reach", "combo",
            "damage_dealt", "shield_break", "allies", "cps", "combat_timer", "kd",
            "attack_cooldown", "crit", "low_hp", "hit_marker");
    private static final Set<String> CAT_OTHER = Set.of("fps", "ping", "coords", "memory", "waypoints", "spotify", "hex_badge");
    private static final Set<String> BASIC = Set.of("fps", "cps", "cooldowns", "effects", "spotify");
    public static final int[] PALETTE = {0xD10A0A, 0xFFFFFF, 0xFFD60A, 0x30D158, 0x64D2FF, 0x0A84FF, 0xBF5AF2};

    public final String id;

    /** Kategoria w menu: PvP (walka), Status (Ty i Twoje zasoby) albo Other (reszta). */
    public String category() {
        if (CAT_PVP.contains(id)) return "PvP";
        return CAT_OTHER.contains(id) ? "Other" : "Status";
    }

    /** Wypelniony prostokat z zaokraglonymi rogami (promien r). */
    public static void roundRect(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        for (int i = 0; i < h; i++) {
            int d = Math.min(i, h - 1 - i);
            int inset = 0;
            if (d < r) {
                double dy = r - d - 0.5;
                inset = r - (int) Math.round(Math.sqrt(Math.max(0.0, r * r - dy * dy)));
            }
            ctx.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
        }
    }

    /** Rysuje pasek postepu (0..1). */
    protected static void drawBar(DrawContext ctx, int x, int y, int w, int h, float frac, int argb) {
        float f = frac < 0f ? 0f : Math.min(frac, 1f);
        ctx.fill(x, y, x + w, y + h, 0xAA000000);
        int fw = Math.round((w - 2) * f);
        if (fw > 0) ctx.fill(x + 1, y + 1, x + 1 + fw, y + h - 1, argb);
    }

    /** Czy to podstawowy widget, ktory zostaje widoczny po .hexoff. */
    public boolean isBasic() { return BASIC.contains(id); }
    public final String name;
    public final String description;

    public int x = 10;
    public int y = 10;
    public float scale = 1f;
    public int color = 0xFFFFFF;
    public boolean hidden = false;

    /** Wyglad ramki widgetu (indeks w STYLE_NAMES). */
    public int style = 0;
    public static final String[] STYLE_NAMES = {"Classic", "Panel", "Outline", "Underline", "Glass", "Brackets", "Pill", "Rounded", "Neon", "Gradient", "Shadow", "Split"};
    public static final int STYLE_COUNT = STYLE_NAMES.length;

    /** Klawisz (kod GLFW) do pokazywania/ukrywania widgetu, -1 = brak. */
    public int bindKey = -1;
    /** true = widget widoczny tylko, gdy trzymasz klawisz; false = klawisz przelacza. */
    public boolean bindHold = false;
    private boolean keyHidden = false;
    private boolean keyWasDown = false;
    private boolean stealthInit = false;
    private boolean lastStealth = false;

    public final Map<String, Boolean> options = new LinkedHashMap<>();
    public final Map<String, String> optionLabels = new LinkedHashMap<>();

    private int centerDy = 0;
    private boolean enabled;

    public Module(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        option("bg", "Background", false);
        option("stealth", "Hide until key", false);
    }

    private int defX = 10, defY = 10;
    private int defStyle = 0;

    /** Zasieg w blokach (0 = modul nie ma ustawienia zasiegu). Ustawiany w edytorze HUD. */
    public int range = 0;
    private int defRange = 0;

    protected Module rangeOption(int def) {
        this.range = def;
        this.defRange = def;
        return this;
    }

    public boolean hasRange() {
        return defRange > 0;
    }

    /** Ustawia domyslny styl ramki widgetu (uzywany tez przez Reset). */
    protected Module defaultStyle(int s) {
        this.style = s;
        this.defStyle = s;
        return this;
    }

    public Module at(int x, int y) {
        this.x = x;
        this.y = y;
        this.defX = x;
        this.defY = y;
        return this;
    }

    /** Przywraca domyslna pozycje, skale, kolor i styl widgetu. */
    public void resetLook() {
        x = defX;
        y = defY;
        scale = 1f;
        color = 0xFFFFFF;
        style = defStyle;
        range = defRange;
        hidden = false;
    }

    /** Domyslnie na srodku ekranu, przesuniety w dol o dy. */
    public Module centered(int dy) {
        this.x = CENTER;
        this.y = CENTER;
        this.centerDy = dy;
        return this;
    }

    protected void option(String key, String label, boolean def) {
        options.put(key, def);
        optionLabels.put(key, label);
    }

    public boolean option(String key) {
        return options.getOrDefault(key, true);
    }

    public int rgb() {
        return 0xFF000000 | color;
    }

    /** Szerokosc i wysokosc tresci widgetu (bez skali), uzywane do ramki w edytorze HUD. */
    public int w() { return 110; }
    public int h() { return 12; }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        if (enabled && Remote.isLocked()) return;
        this.enabled = enabled;
        if (enabled) onEnable(); else onDisable();
        ModuleManager.save();
    }

    void setEnabledSilently(boolean enabled) {
        this.enabled = enabled;
    }

    /** Zamienia pozycje CENTER na konkretne wspolrzedne. */
    public void place(DrawContext ctx) {
        if (x == CENTER) x = ctx.getScaledWindowWidth() / 2 - Math.round(w() * scale / 2f);
        if (y == CENTER) y = ctx.getScaledWindowHeight() / 2 + centerDy;
        int maxX = Math.max(0, ctx.getScaledWindowWidth() - 12);
        int maxY = Math.max(0, ctx.getScaledWindowHeight() - 12);
        if (x > maxX) x = maxX;
        if (y > maxY) y = maxY;
        if (x < 0) x = 0;
        if (y < 0) y = 0;
    }

    public boolean contains(int mx, int my) {
        return mx >= x - 2 && mx <= x + w() * scale + 2 && my >= y - 2 && my <= y + h() * scale + 2;
    }

    /** Rysowanie w grze. */
    public final void render(DrawContext ctx) {
        if (Remote.isLocked()) return;
        pollKey();
        if (hidden || keyHidden) return;
        if (extrasHidden && !BASIC.contains(id)) return;
        paint(ctx, false);
    }

    /** Sprawdza klawisz widgetu co klatke (tylko gdy nie ma otwartego ekranu, np. czatu). */
    private void pollKey() {
        if (bindKey <= 0) {
            keyHidden = false;
            stealthInit = false;
            return;
        }
        boolean stealth = option("stealth");
        if (!stealthInit || stealth != lastStealth) {
            stealthInit = true;
            lastStealth = stealth;
            keyHidden = stealth;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) return;
        boolean down = GLFW.glfwGetKey(mc.getWindow().getHandle(), bindKey) == GLFW.GLFW_PRESS;
        if (bindHold) {
            keyHidden = !down;
        } else if (down && !keyWasDown) {
            keyHidden = !keyHidden;
        }
        keyWasDown = down;
    }

    /** Rysowanie z uwzglednieniem pozycji i skali. preview=true w edytorze HUD (pokazuje przyklad). */
    public final void paint(DrawContext ctx, boolean preview) {
        if (overlay()) {
            draw(ctx, preview);
            return;
        }
        place(ctx);
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) x, (float) y);
        m.scale(scale, scale);
        drawFrame(ctx);
        draw(ctx, preview);
        m.popMatrix();
    }

    /** Ramka widgetu wedlug wybranego stylu (kolor akcentu = kolor widgetu). */
    private void drawFrame(DrawContext ctx) {
        drawStyleFrame(ctx, w(), h());
    }

    /** Ramka wedlug stylu dla prostokata w x h od punktu (0,0); uzywana tez przez moduly nakladkowe (np. ping nad glowa). */
    protected final void drawStyleFrame(DrawContext ctx, int w, int h) {
        int c = rgb();
        switch (style) {
            case 1 -> { // Panel
                ctx.fill(-3, -2, w + 3, h + 2, 0xCC101014);
                ctx.fill(-3, -2, -1, h + 2, c);
            }
            case 2 -> { // Outline
                ctx.fill(-3, -2, w + 3, h + 2, 0x55000000);
                ctx.fill(-3, -2, w + 3, -1, c);
                ctx.fill(-3, h + 1, w + 3, h + 2, c);
                ctx.fill(-3, -2, -2, h + 2, c);
                ctx.fill(w + 2, -2, w + 3, h + 2, c);
            }
            case 3 -> { // Underline
                ctx.fill(-3, -2, w + 3, h + 2, 0x33000000);
                ctx.fill(-3, h + 1, w + 3, h + 2, c);
            }
            case 4 -> { // Glass
                ctx.fill(-3, -2, w + 3, h + 2, 0x66202028);
                ctx.fill(-3, -2, w + 3, -1, 0x55FFFFFF);
                ctx.fill(-3, h + 1, w + 3, h + 2, c);
            }
            case 5 -> { // Brackets
                ctx.fill(-3, -2, w + 3, h + 2, 0x44000000);
                int l = 4;
                ctx.fill(-3, -2, -3 + l, -1, c);
                ctx.fill(-3, -2, -2, -2 + l, c);
                ctx.fill(w + 3 - l, -2, w + 3, -1, c);
                ctx.fill(w + 2, -2, w + 3, -2 + l, c);
                ctx.fill(-3, h + 1, -3 + l, h + 2, c);
                ctx.fill(-3, h + 2 - l, -2, h + 2, c);
                ctx.fill(w + 3 - l, h + 1, w + 3, h + 2, c);
                ctx.fill(w + 2, h + 2 - l, w + 3, h + 2, c);
            }
            case 6 -> { // Pill
                ctx.fill(-1, -2, w + 1, h + 2, 0xCC101014);
                ctx.fill(-3, 0, w + 3, h, 0xCC101014);
                ctx.fill(-2, h / 2 - 1, 0, h / 2 + 2, c);
            }
            case 7 -> { // Rounded
                roundRect(ctx, -5, -4, w + 10, h + 8, 5, 0x66000000 | (c & 0xFFFFFF));
                roundRect(ctx, -4, -3, w + 8, h + 6, 4, 0xDD101014);
            }
            case 8 -> { // Neon
                ctx.fill(-4, -3, w + 4, h + 3, 0x55000000 | (c & 0xFFFFFF));
                ctx.fill(-3, -2, w + 3, h + 2, 0xDD08080C);
                ctx.fill(-3, -2, w + 3, -1, c);
                ctx.fill(-3, h + 1, w + 3, h + 2, c);
                ctx.fill(-3, -2, -2, h + 2, c);
                ctx.fill(w + 2, -2, w + 3, h + 2, c);
            }
            case 9 -> { // Gradient
                int rows = h + 4;
                for (int i = 0; i < rows; i++) {
                    int a = 0xDD - ((0xDD - 0x33) * i) / Math.max(1, rows - 1);
                    ctx.fill(-3, -2 + i, w + 3, -1 + i, (a << 24) | 0x101014);
                }
                ctx.fill(-3, -2, w + 3, -1, c);
            }
            case 10 -> { // Shadow
                ctx.fill(-1, 0, w + 5, h + 4, 0x77000000);
                ctx.fill(-3, -2, w + 3, h + 2, 0xCC14141A);
            }
            case 11 -> { // Split
                ctx.fill(-3, -2, w + 3, h + 2, 0xAA000000);
                ctx.fill(-3, -2, 0, h + 2, c);
                ctx.fill(0, h + 1, w + 3, h + 2, 0x88000000 | (c & 0xFFFFFF));
            }
            default -> {
                if (option("bg")) {
                    ctx.fill(-3, -2, w + 3, h + 2, 0x99000000);
                    ctx.fill(-3, -2, -1, h + 2, 0xFFD10A0A);
                }
            }
        }
    }

    /** Wlasciwa tresc widgetu, rysowana od punktu (0,0). */
    protected void draw(DrawContext ctx, boolean preview) {}

    /** Modul rysuje sam w swiecie ekranu (np. nad glowami graczy), bez wlasnej pozycji. */
    protected boolean overlay() { return false; }

    public final boolean isOverlay() { return overlay(); }

    /** Modul z podgladem w edytorze HUD (np. wskazniki nad glowa wroga). */
    public boolean hasOverlayPreview() { return false; }

    public void drawOverlayPreview(DrawContext ctx, int ax, int ay) {}

    /** Ramka podgladu {lewo, gora, prawo, dol} lub null. */
    public int[] overlayPreviewBox(int ax, int ay) { return null; }

    public void tick() {}

    protected void onEnable() {}

    protected void onDisable() {}
}
