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
}