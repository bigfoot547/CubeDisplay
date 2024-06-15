package dev.figboot.cuberender.state;

import dev.figboot.cuberender.math.Vector3f;
import dev.figboot.cuberender.math.Vector4f;

public interface Sampleable<T> {
    T extra(int idx);

    void sample(float b0, float b1, float b2, Vector4f normal, T e1, T e2, T e3, Vector4f outColor);
}
