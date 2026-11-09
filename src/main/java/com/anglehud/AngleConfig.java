package com.anglehud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AngleConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("angle_hud.json");

    /** Ein verschiebbares HUD-Element. x/y = Mitte als Bruchteil der GUI-Größe. */
    public static final class Elem {
        public boolean enabled = true;
        public double x = 0.5;
        public double y = 0.5;
        public double scale = 1.0;

        public Elem() {}

        public Elem(boolean enabled, double x, double y) {
            this.enabled = enabled;
            this.x = x;
            this.y = y;
        }

        public double safeScale() {
            return (scale < 0.5 || scale > 3.0) ? 1.0 : scale;
        }
    }

    public boolean enabled = true;
    public double hue = 0.0;
    public double sat = 0.0;
    public double val = 1.0;
    public boolean shadow = true;
    public int decimals = 1;
    public boolean chatButtons = true;

    public Elem pitch = new Elem(true, 0.5, 0.545);
    public Elem heading = new Elem(false, 0.5, 0.05);
    public Elem coords = new Elem(false, 0.14, 0.05);

    private static AngleConfig instance = new AngleConfig();

    public static AngleConfig get() {
        return instance;
    }

    public int rgb() {
        return Hsv.rgb(hue, sat, val);
    }

    public void resetPositions() {
        pitch.x = 0.5;
        pitch.y = 0.545;
        heading.x = 0.5;
        heading.y = 0.05;
        coords.x = 0.14;
        coords.y = 0.05;
    }

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE)) {
            AngleConfig c = GSON.fromJson(reader, AngleConfig.class);
            if (c != null) {
                if (c.pitch == null) c.pitch = new Elem(true, 0.5, 0.545);
                if (c.heading == null) c.heading = new Elem(false, 0.5, 0.05);
                if (c.coords == null) c.coords = new Elem(false, 0.14, 0.05);
                instance = c;
            }
        } catch (Exception e) {
            AngleHudClient.LOGGER.error("Konnte Config nicht laden", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) {
                GSON.toJson(instance, w);
            }
        } catch (Exception e) {
            AngleHudClient.LOGGER.error("Konnte Config nicht speichern", e);
        }
    }
}
