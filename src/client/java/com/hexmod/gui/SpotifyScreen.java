package com.hexmod.gui;

import com.hexmod.module.SpotifyClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/** Spotify: logowanie jednym klikniem, poradnik dla graczy i ustawienia zaawansowane dla admina. */
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

        tab("Logowanie", cx - 140, 90, MAIN);
        tab("Poradnik", cx - 46, 90, GUIDE);
        tab("Zaawansowane", cx + 48, 92, ADVANCED);

        if (mode == MAIN) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Zaloguj przez Spotify"),
                    b -> SpotifyClient.startAutoLogin()).dimensions(cx - 140, 76, 280, 24).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Wyloguj"),
                    b -> SpotifyClient.logout()).dimensions(cx - 140, 106, 280, 20).build());
        } else if (mode == ADVANCED) {
            idField = new TextFieldWidget(textRenderer, cx - 140, 78, 200, 20, Text.literal("Client ID"));
            idField.setPlaceholder(Text.literal("Wlasny Client ID (opcjonalnie)"));
            idField.setMaxLength(64);
            idField.setText(SpotifyClient.clientId());
            addDrawableChild(idField);
            addDrawableChild(ButtonWidget.builder(Text.literal("Zapisz"), b -> {
                SpotifyClient.setClientId(idField.getText());
                SpotifyClient.status = "Zapisano Client ID.";
            }).dimensions(cx + 66, 78, 74, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Kopiuj link logowania"), b -> {
                String url = SpotifyClient.currentLoginUrl();
                if (url == null) {
                    SpotifyClient.status = "Brak Client ID.";
                } else {
                    client.keyboard.setClipboard(url);
                    SpotifyClient.status = "Skopiowano link. Wklej go w przegladarce.";
                }
            }).dimensions(cx - 140, 104, 138, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Wklej adres po logowaniu"),
                    b -> SpotifyClient.completeLogin(client.keyboard.getClipboard()))
                    .dimensions(cx + 2, 104, 138, 20).build());
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
        boolean in = SpotifyClient.isLoggedIn();
        String st = in ? "Zalogowano" : "Niezalogowano";
        ctx.drawTextWithShadow(textRenderer, st, cx + 140 - textRenderer.getWidth(st), 12,
                in ? HexTheme.TEXT : HexTheme.TEXT_DIM);

        if (mode == MAIN) {
            int y = 136;
            y = wrapped(ctx, "Kliknij przycisk, zaloguj sie w przegladarce i kliknij 'Zgadzam sie'. "
                    + "Potem wroc do gry. Wszystko dzieje sie samo.", x, y, 280, HexTheme.TEXT_DIM);
            wrapped(ctx, "Nie wiesz, co dalej? Otworz zakladke Poradnik.", x, y + 4, 280, HexTheme.TEXT_DIM);
        } else if (mode == GUIDE) {
            int y = 54;
            String[] steps = {
                    "1. W zakladce Logowanie kliknij 'Zaloguj przez Spotify'.",
                    "2. W przegladarce zaloguj sie na swoje konto Spotify i kliknij 'Zgadzam sie'.",
                    "3. Zobaczysz napis 'Zalogowano!'. Zamknij karte i wroc do Minecrafta.",
                    "4. W menu Hexmoda (Prawy Shift) wlacz modul 'Spotify'.",
                    "5. Puść muzyke w aplikacji Spotify. Utwor i tekst pojawia sie na ekranie.",
                    "6. Pozycje, wielkosc i kolor widgetu ustawisz w 'Edytuj HUD'.",
                    "Nie dziala? Sprawdz, czy muzyka naprawde gra w Spotify, czy admin dodal Cie "
                            + "do uzytkownikow aplikacji i czy tekst istnieje dla tej piosenki (nie kazda go ma)."
            };
            for (String step : steps) {
                y = wrapped(ctx, step, x, y, 280, HexTheme.TEXT) + 3;
            }
        } else {
            int y = 130;
            y = wrapped(ctx, "Dla admina: wejdz na developer.spotify.com/dashboard, kliknij Create app, "
                    + "w Redirect URI wpisz http://127.0.0.1:8888/callback i zaznacz Web API.", x, y, 280, HexTheme.TEXT_DIM) + 3;
            wrapped(ctx, "Skopiuj Client ID i wpisz go w pliku config.json w repozytorium hexmod-data: "
                    + "{\"spotifyClientId\": \"TWOJ_ID\"}. Wtedy gracze loguja sie sami, bez wlasnego ID. "
                    + "Pole wyzej to wlasny ID, ktory ma pierwszenstwo.", x, y, 280, HexTheme.TEXT_DIM);
        }

        if (!SpotifyClient.status.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, SpotifyClient.status, cx, height - 42, HexTheme.TEXT);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
