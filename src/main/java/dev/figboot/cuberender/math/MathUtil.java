package dev.figboot.cuberender.math;

public final class MathUtil {
    private MathUtil() { }

    public static float clamp(float f1, float min, float max) {
        return Math.min(Math.max(f1, min), max);
    }
}
