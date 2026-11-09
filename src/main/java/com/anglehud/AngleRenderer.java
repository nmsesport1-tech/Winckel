package com.anglehud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;

import java.util.Locale;

public final class AngleRenderer {
    private AngleRenderer() {}

    public enum Kind { PITCH, HEADING, COORDS }

    private static final String[] DIRS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    public static AngleConfig.Elem elem(Kind kind) {
        AngleConfig c = AngleConfig.get();
        return switch (kind) {
            case PITCH -> c.pitch;
            case HEADING -> c.heading;
            case COORDS -> c.coords;
        };
    }

    /** Hoch = positive Grad, runter = negative Grad. */
    public static String pitchText(float xRot) {
        AngleConfig c = AngleConfig.get();
        double deg = -xRot + 0.0;
        if (c.decimals <= 0) {
            return Math.round(deg) + "\u00B0";
        }
        if (Math.abs(deg) < 0.05) deg = 0.0;
        return String.format(Locale.ROOT, "%.1f\u00B0", deg);
    }

    public static String text(Kind kind, Minecraft mc) {
        LocalPlayer p = mc.player;
        switch (kind) {
            case PITCH:
                return pitchText(p != null ? p.getXRot() : 0f);
            case HEADING: {
                float yaw = p != null ? p.getYRot() : 0f;
                double h = (((yaw + 180.0) % 360.0) + 360.0) % 360.0;
                int idx = (int) Math.round(h / 45.0) % 8;
                return String.format(Locale.ROOT, "%s %d\u00B0", DIRS[idx], Math.round(h) % 360);
            }
            default: {
                if (p == null) return "0 64 0";
                return String.format(Locale.ROOT, "%d %d %d",
                        (int) Math.floor(p.getX()), (int) Math.floor(p.getY()), (int) Math.floor(p.getZ()));
            }
        }
    }

    public static double halfW(Font font, String text, double scale) {
        return font.width(text) * scale / 2.0 + 4;
    }

    public static double halfH(Font font, double scale) {
        return font.lineHeight * scale / 2.0 + 3;
    }

    /** Zeichnet den Text exakt zentriert um (cx, cy) mit der angegebenen Größe. */
    public static void drawAt(GuiGraphics g, Minecraft mc, String text, float cx, float cy, double scale) {
        AngleConfig c = AngleConfig.get();
        float s = (float) scale;
        float w = mc.font.width(text);
        float h = mc.font.lineHeight;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.scale(s, s);
        pose.translate(-w / 2f, -h / 2f);
        g.drawString(mc.font, text, 0, 0, 0xFF000000 | c.rgb(), c.shadow);
        pose.popMatrix();
    }

    public static void drawHud(GuiGraphics g, Minecraft mc) {
        AngleConfig c = AngleConfig.get();
        if (!c.enabled) return;
        for (Kind k : Kind.values()) {
            AngleConfig.Elem e = elem(k);
            if (!e.enabled) continue;
            drawAt(g, mc, text(k, mc), (float) (e.x * g.guiWidth()), (float) (e.y * g.guiHeight()), e.safeScale());
        }
    }
}
