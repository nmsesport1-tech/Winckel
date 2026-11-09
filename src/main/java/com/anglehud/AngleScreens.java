package com.anglehud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

public final class AngleScreens {
    private AngleScreens() {}

    // ------------------------------------------------------------- Hilfsklassen
    private static Button toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        return Button.builder(toggleText(label, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(toggleText(label, get.getAsBoolean()));
        }).bounds(x, y, w, 20).build();
    }

    private static Component toggleText(String label, boolean on) {
        return Component.literal(label + ": " + (on ? "ON" : "OFF"));
    }

    private static final class PercentSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final DoubleConsumer setter;

        PercentSlider(int x, int y, int w, int h, String label, double min, double max, double initial, DoubleConsumer setter) {
            super(x, y, w, h, Component.empty(), (Math.max(min, Math.min(max, initial)) - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        private double current() {
            return min + this.value * (max - min);
        }

        @Override
        protected void updateMessage() {
            if (label == null) return;
            setMessage(Component.literal(label + ": " + Math.round(current() * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            setter.accept(current());
        }
    }

    public abstract static class Base extends Screen {
        protected final Screen parent;

        protected Base(Component title, Screen parent) {
            super(title);
            this.parent = parent;
        }

        @Override
        public void onClose() {
            AngleConfig.save();
            this.minecraft.setScreen(parent);
        }
    }

    // ---------------------------------------------------------------- Hauptmenü
    public static class MainMenu extends Base {
        private static final int TW = 96;
        private static final int TH = 70;
        private static final int GAP = 10;

        private record Tile(String label, ItemStack icon, Supplier<Screen> target) {}

        private final List<Tile> tiles = new ArrayList<>();

        public MainMenu(Screen parent) {
            super(Component.literal("Angle HUD"), parent);
        }

        private int gridLeft() {
            return this.width / 2 - (2 * TW + GAP) / 2;
        }

        private int gridTop() {
            return Math.max(40, this.height / 2 - TH - GAP / 2 - 10);
        }

        @Override
        protected void init() {
            tiles.clear();
            tiles.add(new Tile("Display", new ItemStack(Items.COMPASS), () -> new DisplayScreen(this)));
            tiles.add(new Tile("Design", new ItemStack(Items.PAINTING), () -> new DesignScreen(this)));
            tiles.add(new Tile("Elements", new ItemStack(Items.CLOCK), () -> new ElementsScreen(this)));
            tiles.add(new Tile("Chat", new ItemStack(Items.NAME_TAG), () -> new ChatSettingsScreen(this)));

            this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                    .bounds(this.width / 2 - 100, gridTop() + 2 * TH + GAP + 12, 200, 20).build());
        }

        private int tileX(int i) {
            return gridLeft() + (i % 2) * (TW + GAP);
        }

        private int tileY(int i) {
            return gridTop() + (i / 2) * (TH + GAP);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
            super.render(g, mouseX, mouseY, delta);

            // Titel in doppelter Größe
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate(this.width / 2f, 10f);
            pose.scale(2f, 2f);
            g.drawCenteredString(this.font, "Angle HUD", 0, 0, 0xFFFFFFFF);
            pose.popMatrix();

            for (int i = 0; i < tiles.size(); i++) {
                Tile t = tiles.get(i);
                int x = tileX(i);
                int y = tileY(i);
                boolean hover = mouseX >= x && mouseX < x + TW && mouseY >= y && mouseY < y + TH;
                g.fill(x, y, x + TW, y + TH, hover ? 0xDD3B3B5C : 0xB0202030);
                int border = hover ? 0xFFB8A6FF : 0xFF55557A;
                g.fill(x, y, x + TW, y + 1, border);
                g.fill(x, y + TH - 1, x + TW, y + TH, border);
                g.fill(x, y, x + 1, y + TH, border);
                g.fill(x + TW - 1, y, x + TW, y + TH, border);

                var p = g.pose();
                p.pushMatrix();
                p.translate(x + TW / 2f - 16f, y + 10f);
                p.scale(2f, 2f);
                g.renderItem(t.icon(), 0, 0);
                p.popMatrix();
                g.drawCenteredString(this.font, t.label(), x + TW / 2, y + TH - 16, 0xFFFFFFFF);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (super.mouseClicked(event, doubleClick)) return true;
            if (event.button() == 0) {
                for (int i = 0; i < tiles.size(); i++) {
                    int x = tileX(i);
                    int y = tileY(i);
                    if (event.x() >= x && event.x() < x + TW && event.y() >= y && event.y() < y + TH) {
                        this.minecraft.setScreen(tiles.get(i).target().get());
                        return true;
                    }
                }
            }
            return false;
        }
    }

    // ----------------------------------------------------------------- Elements
    public static class ElementsScreen extends Base {
        public ElementsScreen(Screen parent) {
            super(Component.literal("Elements"), parent);
        }

        @Override
        protected void init() {
            AngleConfig c = AngleConfig.get();
            int cx = this.width / 2;
            int y = this.height / 4;
            this.addRenderableWidget(toggle(cx - 100, y, 200, "HUD", () -> c.enabled, v -> c.enabled = v));
            this.addRenderableWidget(toggle(cx - 100, y + 30, 200, "Angle (pitch)", () -> c.pitch.enabled, v -> c.pitch.enabled = v));
            this.addRenderableWidget(toggle(cx - 100, y + 54, 200, "Heading (yaw + N/E/S/W)", () -> c.heading.enabled, v -> c.heading.enabled = v));
            this.addRenderableWidget(toggle(cx - 100, y + 78, 200, "Coordinates", () -> c.coords.enabled, v -> c.coords.enabled = v));
            this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                    .bounds(cx - 100, y + 114, 200, 20).build());
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
            super.render(g, mouseX, mouseY, delta);
            g.drawCenteredString(this.font, "Elements", this.width / 2, 12, 0xFFFFFFFF);
        }
    }

    // ------------------------------------------------------------------- Design
    public static class DesignScreen extends Base {
        private static final int R = 54;
        private static final int STEP = 3;
        private boolean dragging = false;
        private int wcx;
        private int wcy;

        public DesignScreen(Screen parent) {
            super(Component.literal("Design"), parent);
        }

        @Override
        protected void init() {
            AngleConfig c = AngleConfig.get();
            wcx = this.width / 2 - 90;
            wcy = this.height / 2 + 10;
            int x = this.width / 2 + 5;
            int y = wcy - R;
            this.addRenderableWidget(new PercentSlider(x, y, 130, 20, "Brightness", 0.0, 1.0, c.val, v -> c.val = v));
            this.addRenderableWidget(new PercentSlider(x, y + 24, 130, 20, "Size", 0.5, 3.0, c.pitch.safeScale(), v -> {
                c.pitch.scale = Math.round(v * 10.0) / 10.0;
                c.heading.scale = c.pitch.scale;
                c.coords.scale = c.pitch.scale;
            }));
            this.addRenderableWidget(toggle(x, y + 48, 130, "Shadow", () -> c.shadow, v -> c.shadow = v));
            this.addRenderableWidget(Button.builder(decimalsLabel(), b -> {
                c.decimals = c.decimals > 0 ? 0 : 1;
                b.setMessage(decimalsLabel());
            }).bounds(x, y + 72, 130, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                    .bounds(x, y + 100, 130, 20).build());
        }

        private static Component decimalsLabel() {
            return Component.literal("Decimals: " + (AngleConfig.get().decimals > 0 ? "1" : "0"));
        }

        private void pick(double mx, double my) {
            AngleConfig c = AngleConfig.get();
            double dx = mx - wcx;
            double dy = my - wcy;
            double dist = Math.sqrt(dx * dx + dy * dy);
            double h = Math.atan2(dy, dx) / (2.0 * Math.PI);
            if (h < 0) h += 1.0;
            c.hue = h;
            c.sat = Math.min(1.0, dist / R);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
            super.render(g, mouseX, mouseY, delta);
            AngleConfig c = AngleConfig.get();
            g.drawCenteredString(this.font, "Design", this.width / 2, 10, 0xFFFFFFFF);

            // Vorschau
            AngleRenderer.drawAt(g, this.minecraft, "45.0\u00B0", this.width / 2f, wcy - R - 22f, 1.5);

            // Farbrad
            for (int py = -R; py < R; py += STEP) {
                for (int px = -R; px < R; px += STEP) {
                    double dx = px + STEP / 2.0;
                    double dy = py + STEP / 2.0;
                    double d = Math.sqrt(dx * dx + dy * dy);
                    if (d > R) continue;
                    double h = Math.atan2(dy, dx) / (2.0 * Math.PI);
                    if (h < 0) h += 1.0;
                    int rgb = Hsv.rgb(h, d / R, c.val);
                    g.fill(wcx + px, wcy + py, wcx + px + STEP, wcy + py + STEP, 0xFF000000 | rgb);
                }
            }
            // Markierung
            int mx = (int) Math.round(wcx + Math.cos(c.hue * 2.0 * Math.PI) * c.sat * R);
            int my = (int) Math.round(wcy + Math.sin(c.hue * 2.0 * Math.PI) * c.sat * R);
            g.fill(mx - 3, my - 3, mx + 3, my + 3, 0xFF000000);
            g.fill(mx - 2, my - 2, mx + 2, my + 2, 0xFFFFFFFF);

            String hex = String.format("#%06X", c.rgb());
            g.drawCenteredString(this.font, hex, wcx, wcy + R + 8, 0xFF000000 | c.rgb());
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (super.mouseClicked(event, doubleClick)) return true;
            double dx = event.x() - wcx;
            double dy = event.y() - wcy;
            if (event.button() == 0 && Math.sqrt(dx * dx + dy * dy) <= R + 6) {
                dragging = true;
                pick(event.x(), event.y());
                return true;
            }
            return false;
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            if (dragging) {
                pick(event.x(), event.y());
                return true;
            }
            return super.mouseDragged(event, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            dragging = false;
            return super.mouseReleased(event);
        }
    }

    // ------------------------------------------------------------------ Display
    public static class DisplayScreen extends Base {
        private static final double SNAP_PX = 4.0;
        private AngleRenderer.Kind active = null;
        private double grabDx = 0;
        private double grabDy = 0;

        public DisplayScreen(Screen parent) {
            super(Component.literal("Display"), parent);
        }

        @Override
        protected void init() {
            int cx = this.width / 2;
            int y = this.height - 28;
            this.addRenderableWidget(Button.builder(Component.literal("Reset positions"), b -> {
                AngleConfig.get().resetPositions();
            }).bounds(cx - 102, y, 100, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                    .bounds(cx + 2, y, 100, 20).build());
        }

        @Override
        public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
            g.fill(0, 0, this.width, this.height, 0x99202020);
        }

        private AngleRenderer.Kind hitKind(double mx, double my) {
            AngleRenderer.Kind[] kinds = AngleRenderer.Kind.values();
            for (int i = kinds.length - 1; i >= 0; i--) {
                AngleRenderer.Kind k = kinds[i];
                AngleConfig.Elem e = AngleRenderer.elem(k);
                if (!e.enabled) continue;
                String t = AngleRenderer.text(k, this.minecraft);
                double cx = e.x * this.width;
                double cy = e.y * this.height;
                if (Math.abs(mx - cx) <= AngleRenderer.halfW(this.font, t, e.safeScale())
                        && Math.abs(my - cy) <= AngleRenderer.halfH(this.font, e.safeScale())) {
                    return k;
                }
            }
            return null;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
            boolean snapX = false;
            boolean snapY = false;
            for (AngleRenderer.Kind k : AngleRenderer.Kind.values()) {
                AngleConfig.Elem e = AngleRenderer.elem(k);
                if (!e.enabled) continue;
                if (Math.abs(e.x - 0.5) < 1e-6) snapX = true;
                if (Math.abs(e.y - 0.5) < 1e-6) snapY = true;
            }

            // Hilfslinien durch die Bildschirmmitte
            int mx = this.width / 2;
            int my = this.height / 2;
            g.fill(mx, 0, mx + 1, this.height, snapX ? 0xFF55FF55 : 0x44FFFFFF);
            g.fill(0, my, this.width, my + 1, snapY ? 0xFF55FF55 : 0x44FFFFFF);

            super.render(g, mouseX, mouseY, delta);
            g.drawCenteredString(this.font, "Drag elements. Mouse wheel = size. Esc = done.", this.width / 2, 12, 0xFFFFFFFF);

            boolean any = false;
            for (AngleRenderer.Kind k : AngleRenderer.Kind.values()) {
                AngleConfig.Elem e = AngleRenderer.elem(k);
                if (!e.enabled) continue;
                any = true;
                String t = AngleRenderer.text(k, this.minecraft);
                double cx = e.x * this.width;
                double cy = e.y * this.height;
                int l = (int) Math.round(cx - AngleRenderer.halfW(this.font, t, e.safeScale()));
                int r = (int) Math.round(cx + AngleRenderer.halfW(this.font, t, e.safeScale()));
                int tp = (int) Math.round(cy - AngleRenderer.halfH(this.font, e.safeScale()));
                int bt = (int) Math.round(cy + AngleRenderer.halfH(this.font, e.safeScale()));
                g.fill(l, tp, r, bt, active == k ? 0x66FFFFFF : 0x33FFFFFF);
                AngleRenderer.drawAt(g, this.minecraft, t, (float) cx, (float) cy, e.safeScale());
            }
            if (!any) {
                g.drawCenteredString(this.font, "No elements enabled - turn them on under Elements.",
                        this.width / 2, this.height / 2 - 20, 0xFFFFAA00);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (super.mouseClicked(event, doubleClick)) return true;
            if (event.button() == 0) {
                AngleRenderer.Kind k = hitKind(event.x(), event.y());
                if (k != null) {
                    active = k;
                    AngleConfig.Elem e = AngleRenderer.elem(k);
                    grabDx = e.x * this.width - event.x();
                    grabDy = e.y * this.height - event.y();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
            if (active != null) {
                AngleConfig.Elem e = AngleRenderer.elem(active);
                double px = event.x() + grabDx;
                double py = event.y() + grabDy;
                // Einrasten an der Bildschirmmitte
                if (Math.abs(px - this.width / 2.0) <= SNAP_PX) px = this.width / 2.0;
                if (Math.abs(py - this.height / 2.0) <= SNAP_PX) py = this.height / 2.0;
                e.x = Math.max(0.0, Math.min(1.0, px / this.width));
                e.y = Math.max(0.0, Math.min(1.0, py / this.height));
                return true;
            }
            return super.mouseDragged(event, dragX, dragY);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            active = null;
            return super.mouseReleased(event);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            AngleRenderer.Kind k = hitKind(mouseX, mouseY);
            if (k != null) {
                AngleConfig.Elem e = AngleRenderer.elem(k);
                double s = e.safeScale() + (scrollY > 0 ? 0.1 : -0.1);
                e.scale = Math.round(Math.max(0.5, Math.min(3.0, s)) * 10.0) / 10.0;
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
    }

    // --------------------------------------------------------------------- Chat
    public static class ChatSettingsScreen extends Base {
        public ChatSettingsScreen(Screen parent) {
            super(Component.literal("Chat"), parent);
        }

        @Override
        protected void init() {
            AngleConfig c = AngleConfig.get();
            int cx = this.width / 2;
            int y = this.height / 4;
            this.addRenderableWidget(toggle(cx - 100, y, 200, "Buttons in chat window", () -> c.chatButtons, v -> c.chatButtons = v));
            this.addRenderableWidget(Button.builder(Component.literal("Open translator"),
                    b -> this.minecraft.setScreen(new TranslatorScreen(this))).bounds(cx - 100, y + 26, 200, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                    .bounds(cx - 100, y + 62, 200, 20).build());
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
            super.render(g, mouseX, mouseY, delta);
            g.drawCenteredString(this.font, "Chat", this.width / 2, 12, 0xFFFFFFFF);
            g.drawCenteredString(this.font, "In the chat window: [Translate] opens the translator,",
                    this.width / 2, this.height / 4 + 96, 0xFFAAAAAA);
            g.drawCenteredString(this.font, "[-> EN] translates what you typed into English.",
                    this.width / 2, this.height / 4 + 108, 0xFFAAAAAA);
        }
    }

    // --------------------------------------------------------------- Übersetzer
    public static class TranslatorScreen extends Base {
        private static final int ROW_H = 12;
        private static final int ROWS = 8;
        private List<String> msgs = new ArrayList<>();
        private int selected = -1;
        private EditBox box;
        private String result = "";
        private String status = "";

        public TranslatorScreen(Screen parent) {
            super(Component.literal("Translator"), parent);
        }

        private int listLeft() {
            return 20;
        }

        private int listWidth() {
            return this.width - 40;
        }

        private static final int LIST_TOP = 28;

        @Override
        protected void init() {
            msgs = ChatHistory.last(ROWS);
            int left = listLeft();
            int w = listWidth();
            int boxY = LIST_TOP + ROWS * ROW_H + 6;
            box = new EditBox(this.font, left, boxY, w, 18, Component.literal("text"));
            box.setMaxLength(400);
            box.setHint(Component.literal("Or type your own text here..."));
            this.addRenderableWidget(box);

            int half = (w - 4) / 2;
            int by = boxY + 24;
            this.addRenderableWidget(Button.builder(Component.literal("Message \u2192 Deutsch"),
                    b -> runMessage("en", "de")).bounds(left, by, half, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Message \u2192 English"),
                    b -> runMessage("de", "en")).bounds(left + half + 4, by, half, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("My text \u2192 English"),
                    b -> run(box.getValue(), "de", "en")).bounds(left, by + 22, half, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("My text \u2192 Deutsch"),
                    b -> run(box.getValue(), "en", "de")).bounds(left + half + 4, by + 22, half, 20).build());

            int third = (w - 8) / 3;
            int fy = this.height - 26;
            this.addRenderableWidget(Button.builder(Component.literal("Copy result"), b -> {
                if (!result.isEmpty()) this.minecraft.keyboardHandler.setClipboard(result);
            }).bounds(left, fy, third, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Show in chat"), b -> {
                if (!result.isEmpty() && this.minecraft.player != null) {
                    this.minecraft.player.displayClientMessage(Component.literal("[Translation] " + result), false);
                }
            }).bounds(left + third + 4, fy, third, 20).build());
            this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                    .bounds(left + 2 * (third + 4), fy, third, 20).build());
        }

        private void runMessage(String from, String to) {
            if (selected < 0 || selected >= msgs.size()) {
                status = "Select a message first (click it).";
                return;
            }
            run(msgs.get(selected), from, to);
        }

        private void run(String text, String from, String to) {
            if (text == null || text.isBlank()) {
                status = "Nothing to translate.";
                return;
            }
            status = "Translating...";
            Translator.translate(text, from, to, r -> {
                result = r;
                status = "";
            }, e -> status = "Error: " + e);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
            super.render(g, mouseX, mouseY, delta);
            g.drawCenteredString(this.font, "Translator  (double-click a message = to Deutsch)", this.width / 2, 12, 0xFFFFFFFF);

            int left = listLeft();
            int w = listWidth();
            g.fill(left, LIST_TOP, left + w, LIST_TOP + ROWS * ROW_H, 0x88000000);
            for (int i = 0; i < ROWS; i++) {
                int idx = i - (ROWS - msgs.size());
                if (idx < 0) continue;
                int y = LIST_TOP + i * ROW_H;
                if (idx == selected) g.fill(left, y, left + w, y + ROW_H, 0x886644CC);
                String line = this.font.plainSubstrByWidth(msgs.get(idx), w - 8);
                g.drawString(this.font, line, left + 4, y + 2, 0xFFFFFFFF, false);
            }
            if (msgs.isEmpty()) {
                g.drawString(this.font, "No chat messages yet.", left + 4, LIST_TOP + 4, 0xFFAAAAAA, false);
            }

            int ry = LIST_TOP + ROWS * ROW_H + 6 + 24 + 22 + 26;
            if (!status.isEmpty()) {
                g.drawString(this.font, status, left, ry, 0xFFFFAA00, false);
                ry += 12;
            }
            if (!result.isEmpty()) {
                for (FormattedCharSequence line : this.font.split(Component.literal(result), w)) {
                    if (ry > this.height - 38) break;
                    g.drawString(this.font, line, left, ry, 0xFF55FF55, false);
                    ry += 10;
                }
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (super.mouseClicked(event, doubleClick)) return true;
            if (event.button() == 0) {
                int left = listLeft();
                int w = listWidth();
                if (event.x() >= left && event.x() < left + w
                        && event.y() >= LIST_TOP && event.y() < LIST_TOP + ROWS * ROW_H) {
                    int row = (int) ((event.y() - LIST_TOP) / ROW_H);
                    int idx = row - (ROWS - msgs.size());
                    if (idx >= 0 && idx < msgs.size()) {
                        selected = idx;
                        if (doubleClick) runMessage("en", "de");
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
