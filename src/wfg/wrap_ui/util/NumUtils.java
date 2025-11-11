package wfg.wrap_ui.util;

public class NumUtils {
    /**
     * Smoothly interpolates a value toward a target using a combination of constant speed and proportional acceleration.
     *
     * @param current   the current value
     * @param target    the target value
     * @param baseSpeed constant interpolation speed factor
     * @param accel     proportional acceleration factor (scaled by deltaTime)
     * @param deltaTime frame delta time in seconds
     * @return the updated, smoothed value
     */
    public static final float smoothApproach(float current, float target, float baseSpeed, float accel, float deltaTime) {
        float delta = target - current;
        float change = (Math.signum(delta) * baseSpeed + delta * accel) * deltaTime;
        if (Math.abs(change) > Math.abs(delta)) {
            change = delta;
        }
        return current + change;
    }
}
