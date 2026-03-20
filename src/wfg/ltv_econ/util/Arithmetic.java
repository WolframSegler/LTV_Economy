package wfg.ltv_econ.util;

import org.lwjgl.util.vector.Vector2f;

public final class Arithmetic {
    private Arithmetic() {};

    public static final float clamp(float value, float floor, float ceiling) {
        return value < floor ? floor : (value > ceiling ? ceiling : value);
    }
    public static final int clamp(int value, int floor, int ceiling) {
        return value < floor ? floor : (value > ceiling ? ceiling : value);
    }
    public static final double clamp(double value, double floor, double ceiling) {
        return value < floor ? floor : (value > ceiling ? ceiling : value);
    }
    public static final long clamp(long value, long floor, long ceiling) {
        return value < floor ? floor : (value > ceiling ? ceiling : value);
    }
    
    public static final Vector2f lerp(final Vector2f src, final Vector2f dest, float t) {
        return new Vector2f(
            src.x + (dest.x - src.x) * t,
            src.y + (dest.y - src.y) * t
        );
    }
    public static final void lerp(final Vector2f src, final Vector2f dest, float t, Vector2f out) {
        out.x = src.x + (dest.x - src.x) * t;
        out.y = src.y + (dest.y - src.y) * t;
    }
    public static final float dist(final Vector2f src, final Vector2f dest) {
        final float dx = dest.x - src.x;
        final float dy = dest.y - src.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    public static final float dist(final float x1, final float y1, final float x2, final float y2) {
        final float dx = x2 - x1;
        final float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public static final float lerp(final float x, final float y, float t) {
        return x + (y - x) * t;
    }
    public static final int lerp(final int x, final int y, float t) {
        return Math.round(x + (y - x) * t);
    }
    public static final double lerp(final double x, final double y, float t) {
        return x + (y - x) * t;
    }
    public static final long lerp(final long x, final long y, double t) {
        return Math.round(x + (y - x) * t);
    }

}