package dev.figboot.cuberender.math;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Vector4f {
    public float x, y, z, w;

    public Vector4f() {
        this(0, 0, 0, 0);
    }

    public Vector4f(float x, float y, float z) {
        this(x, y, z, 1f);
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
        target.x = x * fact;
        target.y = y * fact;
        target.z = z * fact;
        target.w = w * fact;
        return target;
    }

    public float dot(Vector4f that) {
        return this.x * that.x + this.y * that.y + this.z * that.z + this.w * that.w;
    }

    public float lengthSquared() {
        // dot(this, this)
        return this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w;
    }

    public float length() {
        return (float)Math.sqrt(lengthSquared());
    }

    public Vector4f normalize() {
        return normalize(this);
    }

    public Vector4f normalize(Vector4f target) {
        float len = length();
        target.x = this.x / len;
        target.y = this.y / len;
        target.z = this.z / len;
        target.w = this.w / len;
        return target;
    }

    public void copyFrom(Vector4f src) {
        x = src.x;
        y = src.y;
        z = src.z;
        w = src.w;
    }
}
