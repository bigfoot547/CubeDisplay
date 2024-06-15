package dev.figboot.cuberender.math;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Vector2f {
    public float x, y;

    public Vector2f() {
        this(0, 0);
    }

    public Vector2f(Vector2f vector) {
        this.x = vector.x;
        this.y = vector.y;
    }
}
