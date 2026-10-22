package com.hexmod.gui;

import com.hexmod.module.L;
import com.hexmod.module.PackTools;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Przegladanie tekstur pakietu z wyszukiwarka; klikniecie PNG otwiera edytor pikseli. */
public class TextureBrowserScreen extends Screen {
    private static final int ROWS = 7;

    private final Screen parent;
    private final Path root;
    private Path dir;
    private int offset = 0;
    private String query = "";
    private boolean dirty;
    private TextFieldWidget search;
    private final List<ButtonWidget> rowButtons = new ArrayList<>();
    private List<Path> items = List.of();

    public TextureBrowserScreen(Screen parent, Path root) {
        super(Text.literal(L.t("Textures")));
        this.parent = parent;
        this.root = root;
        this.dir = root;
    }

    private List<Path> searchAll() {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> s = Files.walk(root)) {
            s.filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .filter(p -> root.relativize(p).toString().replace('\\', '/').toLowerCase(Locale.ROOT).contains(query))
                    .sorted().limit(400).forEach(out::add);
        } catch (IOException ignored) {}
        return out;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        if (search == null) {
            search = new TextFieldWidget(textRenderer, cx - 140, 34, 250, 18, Text.literal(L.t("Search")));
            search.setPlaceholder(Text.literal(L.t("Search textures (e.g. shield, sword)")));
            search.setMaxLength(40);
            search.setChangedListener(s -> {
                query = s.trim().toLowerCase(Locale.ROOT);
                offset = 0;
                dirty = true;
            });
        }
        search.setPosition(cx - 140, 34);
        addDrawableChild(search);

        addDrawableChild(ButtonWidget.builder(Text.literal("^"), b -> {
            offset = Math.max(0, offset - ROWS);
            refreshRows();
        }).dimensions(cx + 116, 34, 24, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("v"), b -> {
            if (offset + ROWS < items.size()) offset += ROWS;
            refreshRows();
        }).dimensions(cx + 116, 56, 24, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(L.t("Back")), b -> client.setScreen(parent))
                .dimensions(cx - 50, height - 28, 100, 20).build());

        rowButtons.clear();
        buildRows();
    }

    private void refreshRows() {
        for (ButtonWidget b : rowButtons) remove(b);
        rowButtons.clear();
        buildRows();
    }

    private void addRow(String label, int y, Runnable action) {
        ButtonWidget b = ButtonWidget.builder(Text.literal(textRenderer.trimToWidth(label, 240)), x -> action.run())
                .dimensions(width / 2 - 140, y, 250, 18).build();
        rowButtons.add(b);
        addDrawableChild(b);
    }

    private void buildRows() {
        boolean searching = !query.isEmpty();
        items = searching ? searchAll() : PackTools.listEntries(dir);
        int y = 58;
        if (!searching && !dir.equals(root)) {
            addRow(L.t("[..] back"), y, () -> {
                dir = dir.getParent();
                offset = 0;
                refreshRows();
            });
            y += 20;
        }
        int shown = 0;
        for (int i = offset; i < items.size() && shown < ROWS; i++, shown++) {
            final Path p = items.get(i);
            boolean isDir = Files.isDirectory(p);
            String label = searching
                    ? root.relativize(p).toString().replace('\\', '/')
                    : (isDir ? "[" + p.getFileName() + "]" : p.getFileName().toString());
            addRow(label, y, () -> {
                if (isDir) {
                    dir = p;
                    offset = 0;
                    refreshRows();
                } else {
                    client.setScreen(new PixelEditorScreen(this, p));
                }
            });
            y += 20;
        }
    }

    @Override
    public void tick() {
        if (dirty) {
            dirty = false;
            refreshRows();
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
        int x = width / 2 - 140;
        ctx.drawTextWithShadow(textRenderer, L.t("TEXTURES"), x, 14, HexTheme.RED);
        String where = query.isEmpty()
                ? "/textures/" + root.relativize(dir).toString().replace('\\', '/')
                : L.t("Search results: ") + items.size();
        ctx.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(where, 150), x + 80, 14, HexTheme.TEXT_DIM);
        if (items.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, query.isEmpty() ? L.t("Empty folder.") : L.t("No results."), x, 80, HexTheme.TEXT_DIM);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
