package com.hexmod.gui;

import com.hexmod.module.MacroManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Makra: przypisz klawisz do komendy (np. /spawn) albo tekstu wysylanego na czacie. */
public class MacroScreen extends Screen {
    private final Screen parent;
    private MacroManager.Macro listening;
    private final boolean[] held = new boolean[349];
    private boolean needsInit;
    private String status = "";

    public MacroScreen(Screen parent) {
        super(Text.literal("Makra"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        List<MacroManager.Macro> list = MacroManager.list();
        int y = 44;
        for (MacroManager.Macro m : list) {
            String keyLabel = m == listening ? "Nacisnij..." : HudEditorScreen.keyName(m.key);
            addDrawableChild(ButtonWidget.builder(Text.literal(textRenderer.trimToWidth(keyLabel, 80)), b -> {
                listening = listening == m ? null : m;
                long win = client.getWindow().getHandle();
                for (int k = 32; k < held.length; k++) held[k] = GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS;
                needsInit = true;
            }).dimensions(cx - 140, y, 86, 18).build());

            TextFieldWidget tf = new TextFieldWidget(textRenderer, cx - 50, y, 146, 18, Text.literal("Tekst"));
            tf.setMaxLength(100);
            tf.setPlaceholder(Text.literal("Komenda (/...) lub tekst"));
            tf.setText(m.text);
            tf.setChangedListener(s -> m.text = s);
            addDrawableChild(tf);

            addDrawableChild(ButtonWidget.builder(Text.literal("Usun"), b -> {
                MacroManager.list().remove(m);
                MacroManager.save();
                needsInit = true;
            }).dimensions(cx + 100, y, 40, 18).build());
            y += 22;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Dodaj makro"), b -> {
            if (MacroManager.list().size() >= MacroManager.MAX) {
                status = "Maksymalnie " + MacroManager.MAX + " makr.";
            } else {
                MacroManager.list().add(new MacroManager.Macro());
                MacroManager.save();
                needsInit = true;
            }
        }).dimensions(cx - 140, height - 54, 138, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Wstecz"), b -> client.setScreen(parent))
                .dimensions(cx + 2, height - 54, 138, 20).build());
    }

    @Override
    public void tick() {
        if (needsInit) {
            needsInit = false;
            clearAndInit();
        }
    }

    private void pollListening() {
        if (listening == null) return;
        long win = client.getWindow().getHandle();
        for (int k = 32; k < held.length; k++) {
            boolean down = GLFW.glfwGetKey(win, k) == GLFW.GLFW_PRESS;
            if (!down) { held[k] = false; continue; }
            if (held[k] || k == GLFW.GLFW_KEY_ESCAPE) continue;
            listening.key = k == GLFW.GLFW_KEY_BACKSPACE ? -1 : k;
            listening = null;
            MacroManager.save();
            needsInit = true;
            return;
        }
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
        pollListening();
        int x = width / 2 - 140;
        ctx.drawTextWithShadow(textRenderer, "MAKRA", x, 14, HexTheme.RED);
        ctx.drawTextWithShadow(textRenderer, "Klawisz wysyla komende albo tekst na czacie.", x, 27, HexTheme.TEXT_DIM);
        if (MacroManager.list().isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "Brak makr. Kliknij 'Dodaj makro'.", x, 50, HexTheme.TEXT_DIM);
        }
        if (!status.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 68, HexTheme.TEXT);
        }
    }

    @Override
    public void removed() {
        MacroManager.save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
