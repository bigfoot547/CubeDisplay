package dev.figboot.cuberender.math;

public class Matrix3f {
    public float m00, m01, m02,
                 m10, m11, m12,
                 m20, m21, m22;

    public Matrix3f() {
        this(1, 0, 0, 0, 1, 0, 0, 0, 1);
    }

    public Matrix3f(Matrix3f m) {
        this(m.m00, m.m01, m.m02, m.m10, m.m11, m.m12, m.m20, m.m21, m.m22);
    }

    public Matrix3f(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
    }

    public Matrix3f identity() {
        return identity(this);
    }

    public Matrix3f identity(Matrix3f target) {
        target.m00 = 1f;
        target.m11 = 1f;
        target.m22 = 1f;

        target.m01 = target.m02 = target.m10 = target.m12 = target.m20 = target.m21 = 0;
        return target;
    }

    public Matrix3f times(Matrix3f other) {
        return times(other, this);
    }

    public Matrix3f times(Matrix3f other, Matrix3f target) {
        float m00 = this.m00 * other.m00 + this.m01 * other.m10 + this.m02 * other.m20;
        float m01 = this.m00 * other.m01 + this.m01 * other.m11 + this.m02 * other.m21;
        float m02 = this.m00 * other.m02 + this.m01 * other.m21 + this.m02 * other.m22;

        float m10 = this.m10 * other.m00 + this.m11 * other.m10 + this.m12 * other.m20;
        float m11 = this.m10 * other.m01 + this.m11 * other.m11 + this.m12 * other.m21;
        float m12 = this.m10 * other.m02 + this.m11 * other.m12 + this.m12 * other.m22;

        float m20 = this.m20 * other.m00 + this.m21 * other.m10 + this.m22 * other.m20;
        float m21 = this.m20 * other.m01 + this.m21 * other.m11 + this.m22 * other.m21;
        float m22 = this.m20 * other.m02 + this.m21 * other.m12 + this.m22 * other.m22;

        target.m00 = m00;
        target.m01 = m01;
        target.m02 = m02;

        target.m10 = m10;
        target.m11 = m11;
        target.m12 = m12;

        target.m20 = m20;
        target.m21 = m21;
        target.m22 = m22;

        return target;
    }

    public Vector3f transform(Vector3f other) {
        return new Vector3f(
                this.m00 * other.x + this.m01 * other.y + this.m02 * other.z,
                this.m10 * other.x + this.m11 * other.y + this.m12 * other.z,
                this.m20 * other.x + this.m21 * other.y + this.m22 * other.z);
    }

    public static Matrix3f rotateX(float radians) {
        float f1 = (float)Math.cos(radians);
        float f2 = (float)Math.sin(radians);

        return new Matrix3f(
                1, 0, 0,
                0, f1, f2,
                0, -f2, f1);
    }

    public static Matrix3f rotateY(float radians) {
        float f1 = (float)Math.cos(radians);
        float f2 = (float)Math.sin(radians);

        return new Matrix3f(
                f1, 0, -f2,
                0, 1, 0,
                f2, 0, f1);
    }

    public static Matrix3f rotateZ(float radians) {
        float f1 = (float)Math.cos(radians);
        float f2 = (float)Math.sin(radians);

        return new Matrix3f(
                f1, -f2, 0,
                f2, f1, 0,
                0, 0, 1);
    }

    public static Matrix3f scaleX(float by) {
        return new Matrix3f(by, 0, 0, 0, 1, 0, 0, 0, 1);
    }

    public static Matrix3f scaleY(float by) {
        return new Matrix3f(1, 0, 0, 0, by, 0, 0, 0, 1);
    }

    public static Matrix3f scaleZ(float by) {
        return new Matrix3f(1, 0, 0, 0, 1, 0, 0, 0, by);
    }
}
