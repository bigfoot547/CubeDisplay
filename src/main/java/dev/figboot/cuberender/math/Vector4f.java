package dev.figboot.cuberender.math;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Vector4f {
    public float x, y, z, w;

    public Vector4f() {
        this(0, 0, 0, 0);
    }

    public Vector4f(Vector4f vec) {
        this(vec.x, vec.y, vec.z, vec.w);
    }

    public Vector4f fromARGB(int argb) {
        return fromARGB(argb, this);
    }

    public Vector4f fromARGB(int argb, Vector4f target) {
        target.x = ((argb & 0x00FF0000) >>> 16) / 255f;
        target.y = ((argb & 0x0000FF00) >>> 8) / 255f;
        target.z = ((argb & 0x000000FF)) / 255f;
        target.w = ((argb & 0xFF000000) >>> 24) / 255f;
        return target;
    }

    public int toARGB() {
        return ((int)(MathUtil.clamp(w, 0, 1) * 255) << 24)
                | ((int)(MathUtil.clamp(x, 0, 1) * 255) << 16)
                | ((int)(MathUtil.clamp(y, 0, 1) * 255) << 8)
                | ((int)(MathUtil.clamp(z, 0, 1) * 255));
    }

    public Vector4f times(float fact) {
        return times(fact, this);
    }

    public Vector4f times(float fact, Vector4f target) {
        target.x *= fact;
        target.y *= fact;
        target.z *= fact;
        target.w *= fact;
        return target;
    }
}
