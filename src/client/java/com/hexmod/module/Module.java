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
    private static final Set<String> BASIC = Set.of("fps", "cps", "cooldowns", "effects", "spotify");
    public static final int[] PALETTE = {0xD10A0A, 0xFFFFFF, 0xFFD60A, 0x30D158, 0x64D2FF, 0x0A84FF, 0xBF5AF2};

    public final String id;
    public final String name;
    public final String description;

    public int x = 10;
    public int y = 10;
    public float scale = 1f;
    public int color = 0xFFFFFF;
    public boolean hidden = false;

    /** Klawisz (kod GLFW) do pokazywania/ukrywania widgetu, -1 = brak. */
    public int bindKey = -1;
    /** true = widget widoczny tylko, gdy trzymasz klawisz; false = klawisz przelacza. */
    public boolean bindHold = false;
    private boolean keyHidden = false;
    private boolean keyWasDown = false;

    public final Map<String, Boolean> options = new LinkedHashMap<>();
    public final Map<String, String> optionLabels = new LinkedHashMap<>();

    private int centerDy = 0;
    private boolean enabled;

    public Module(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        option("bg", "Tlo", false);
    }

    public Module at(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
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
            return;
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
        if (option("bg")) {
            ctx.fill(-3, -2, w() + 3, h() + 2, 0x99000000);
            ctx.fill(-3, -2, -1, h() + 2, 0xFFD10A0A);
        }
        draw(ctx, preview);
        m.popMatrix();
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
