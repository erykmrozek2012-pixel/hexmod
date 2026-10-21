package com.hexmod.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Krotka sciaga: komendy czatu i skroty Hexmoda. */
public class HelpScreen extends Screen {
    private static final String[][] LINES = {
            {"MENU", "Prawy Shift otwiera menu. Zmiana w Sterowaniu."},
            {"HUD", "Edytuj HUD: przeciagaj widgety, kliknij widget = skala, kolor, opcje, klawisz."},
            {"STREAM", "Opcja 'Ukryj do klawisza': widget jest niewidoczny, dopoki nie wcisniesz jego klawisza."},
            {".hexoff", "Wylacza wszystkie widgety (awaryjnie, np. na streamie). .hexon przywraca."},
            {".hexoff nazwa", "Wylacza jeden modul. .hexon nazwa wlacza go z powrotem."},
            {".wp add [nazwa]", "Zapisuje waypoint w miejscu, w ktorym stoisz."},
            {".wp del nazwa", "Usuwa waypoint. .wp list pokazuje wszystkie."},
            {".lock", "Blokuje przedmiot z reki. .lock nazwa (np. ender_pearl) blokuje po nazwie."},
            {".lock list / clear", "Pokazuje albo czysci liste zablokowanych przedmiotow."},
    };

    private final Screen parent;

    public HelpScreen(Screen parent) {
        super(Text.literal("Komendy i pomoc"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("Wstecz"), b -> client.setScreen(parent))
                .dimensions(width / 2 - 50, height - 28, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(width / 2 - 170, 4, width / 2 + 170, height - 2, HexTheme.PANEL);
        ctx.fill(width / 2 - 170, 4, width / 2 + 170, 7, HexTheme.RED);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int x = width / 2 - 160;
        ctx.drawTextWithShadow(textRenderer, "KOMENDY I POMOC", x, 14, HexTheme.RED);
        int y = 30;
        for (String[] l : LINES) {
            ctx.drawTextWithShadow(textRenderer, l[0], x, y, HexTheme.RED);
            for (var line : textRenderer.wrapLines(Text.literal(l[1]), 320)) {
                ctx.drawTextWithShadow(textRenderer, line, x, y + 10, HexTheme.TEXT);
                y += 10;
            }
            y += 16;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
