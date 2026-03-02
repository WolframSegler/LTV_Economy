package wfg.ltv_econ.util;

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
}