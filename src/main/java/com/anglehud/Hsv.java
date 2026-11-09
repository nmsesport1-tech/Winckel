package com.anglehud;

public final class Hsv {
    private Hsv() {}

    /** h, s, v jeweils 0..1. Ergebnis: 0xRRGGBB. */
    public static int rgb(double h, double s, double v) {
        h = h - Math.floor(h);
        s = Math.max(0.0, Math.min(1.0, s));
        v = Math.max(0.0, Math.min(1.0, v));
        double c = v * s;
        double hp = h * 6.0;
        double x = c * (1.0 - Math.abs(hp % 2.0 - 1.0));
        double r = 0, g = 0, b = 0;
        switch ((int) hp) {
            case 0 -> { r = c; g = x; }
            case 1 -> { r = x; g = c; }
            case 2 -> { g = c; b = x; }
            case 3 -> { g = x; b = c; }
            case 4 -> { r = x; b = c; }
            default -> { r = c; b = x; }
        }
        double m = v - c;
        int ri = (int) Math.round((r + m) * 255.0);
        int gi = (int) Math.round((g + m) * 255.0);
        int bi = (int) Math.round((b + m) * 255.0);
        return (ri << 16) | (gi << 8) | bi;
    }
}
