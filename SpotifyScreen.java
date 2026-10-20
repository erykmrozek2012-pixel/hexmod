package com.hexmod.gui;

import com.hexmod.module.SpotifyClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/** Spotify: dziala bez logowania (czyta aplikacje Spotify na komputerze). Logowanie jest opcjonalne. */
public class SpotifyScreen extends Screen {
    private static final int MAIN = 0, GUIDE = 1, ADVANCED = 2;

    private final Screen parent;
    private int mode = MAIN;
    private TextFieldWidget idField;

    public SpotifyScreen(Screen parent) {
        super(Text.literal("Spotify"));
        this.parent = parent;
    }

    private void tab(String label, int x, int w, int target) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> { mode = target; clearAndInit(); })
                .dimensions(x, 26, w, 18).build());
    }

    @Override
    protected void init() {
        int cx = width / 2;
        tab("Status", cx - 140, 90, MAIN);
        tab("Poradnik", cx - 46, 90, GUIDE);
        tab("Zaawansowane", cx + 48, 92, ADVANCED);

        if (mode == ADVANCED) {
            idField = new TextFieldWidget(textRenderer, cx - 140, 76, 200, 20, Text.literal("Client ID"));
            idField.setPlaceholder(Text.literal("Wlasny Client ID (opcjonalnie)"));
            idField.setMaxLength(64);
            idField.setText(SpotifyClient.clientId());
            addDrawableChild(idField);
            addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz"), b -> {
                SpotifyClient.setClientId(idField.getText());
                SpotifyClient.status = "Zapisano Client ID.";
            }).dimensions(cx + 66, 76, 74, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Zaloguj (dokladna synchronizacja)"),
                    b -> SpotifyClient.startAutoLogin()).dimensions(cx - 140, 102, 280, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Wyloguj"), b -> SpotifyClient.logout())
                    .dimensions(cx - 140, 126, 280, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Wstecz"), b -> client.setScreen(parent))
                .dimensions(cx - 50, height - 28, 100, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, HexTheme.OVERLAY);
        ctx.fill(width / 2 - 150, 4, width / 2 + 150, height - 2, HexTheme.PANEL);
        ctx.fill(width / 2 - 150, 4, width / 2 + 150, 7, HexTheme.RED);
    }

    private int wrapped(DrawContext ctx, String text, int x, int y, int maxW, int color) {
        for (OrderedText line : textRenderer.wrapLines(Text.literal(text), maxW)) {
            ctx.drawTextWithShadow(textRenderer, line, x, y, color);
            y += 10;
        }
        return y;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        int x = cx - 140;
        ctx.drawTextWithShadow(textRenderer, "SPOTIFY", x, 12, HexTheme.RED);

        if (mode == MAIN) {
            int y = 54;
            y = wrapped(ctx, "Nic nie musisz robic. Uruchom aplikacje Spotify na komputerze i wlacz muzyke. "
                    + "Widget pokaze utwor i tekst sam.", x, y, 280, HexTheme.TEXT_DIM) + 6;
            String info = SpotifyClient.localInfo.isEmpty() ? "Czekam na dane z aplikacji Spotify..." : SpotifyClient.localInfo;
            ctx.drawTextWithShadow(textRenderer, "Wykryto:", x, y, HexTheme.TEXT);
            wrapped(ctx, info, x, y + 12, 280, HexTheme.RED);
            wrapped(ctx, "Pamietaj, aby wlaczyc modul 'Spotify' w zakladce Moduly.", x, y + 40, 280, HexTheme.TEXT_DIM);
        } else if (mode == GUIDE) {
            int y = 54;
            String[] steps = {
                    "1. Uruchom aplikacje Spotify na komputerze i wlacz muzyke.",
                    "2. W menu Hexmoda (Prawy Shift), zakladka Moduly, wlacz 'Spotify'.",
                    "3. Utwor i tekst piosenki pojawia sie na ekranie. Nie trzeba sie logowac.",
                    "4. Pozycje, wielkosc i kolor widgetu ustawisz w 'Edytuj HUD'.",
                    "Tekst synchronizuje sie od poczatku utworu, wiec po przewinieciu piosenki moze sie rozjechac. "
                            + "Dla dokladnej synchronizacji zaloguj sie w zakladce Zaawansowane."
            };
            for (String step : steps) {
                y = wrapped(ctx, step, x, y, 280, HexTheme.TEXT) + 3;
            }
        } else {
            int y = 152;
            y = wrapped(ctx, "Opcjonalnie: logowanie do Spotify daje dokladny czas utworu i tekst zsynchronizowany "
                    + "nawet po przewinieciu. Client ID ustawia admin w pliku config.json (repozytorium hexmod-data).",
                    x, y, 280, HexTheme.TEXT_DIM);
        }

        if (!SpotifyClient.status.isEmpty() && mode == ADVANCED) {
            ctx.drawCenteredTextWithShadow(textRenderer, SpotifyClient.status, cx, height - 42, HexTheme.TEXT);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
