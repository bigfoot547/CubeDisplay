package dev.figboot.cuberender.math;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Vector3f {
    public float x, y, z;

    public Vector3f() {
        this(0, 0, 0);
    }

    public Vector3f(Vector3f vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = vector.z;
    }

    public float dot(Vector3f vector) {
        return this.x * vector.x + this.y * vector.y + this.z * vector.z;
    }

    public float lengthSquared() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public float length() {
        return (float)Math.sqrt(lengthSquared());
    }

    public Vector3f normalize() {
        return normalize(this);
    }

    public Vector3f normalize(Vector3f target) {
        float len = length();
        target.x /= len;
        target.y /= len;
        target.z /= len;
        return target;
    }
}
