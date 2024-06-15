package dev.figboot.cuberender.math;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Matrix4f {
    public float m00, m01, m02, m03,
                 m10, m11, m12, m13,
                 m20, m21, m22, m23,
                 m30, m31, m32, m33;

    public Matrix4f() {
        identity();
    }

    public Matrix4f(Matrix4f mat) {
        this(mat.m00, mat.m01, mat.m02, mat.m03, mat.m10, mat.m11, mat.m12, mat.m13, mat.m20, mat.m21, mat.m22, mat.m23, mat.m30, mat.m31, mat.m32, mat.m33);
    }

    public Matrix4f identity() {
        return identity(this);
    }

    public Matrix4f identity(Matrix4f target) {
        target.m00 = target.m11 = target.m22 = target.m33 = 1f;
        target.m01 = target.m02 = target.m03
                = target.m10 = target.m12 = target.m13
                = target.m20 = target.m21 = target.m23
                = target.m30 = target.m31 = target.m32 = 0;

        return target;
    }

    public Matrix4f times(Matrix4f right) {
        return times(right, this);
    }

    public Matrix4f times(Matrix4f right, Matrix4f target) {
        float m00 = this.m00 * right.m00 + this.m01 * right.m10 + this.m02 * right.m20 + this.m03 * this.m30;
        float m01 = this.m00 * right.m01 + this.m01 * right.m11 + this.m02 * right.m21 + this.m03 * this.m31;
        float m02 = this.m00 * right.m02 + this.m01 * right.m12 + this.m02 * right.m22 + this.m03 * this.m32;
        float m03 = this.m00 * right.m03 + this.m01 * right.m13 + this.m02 * right.m23 + this.m03 * this.m33;

        float m10 = this.m10 * right.m00 + this.m11 * right.m10 + this.m12 * right.m20 + this.m13 * this.m30;
        float m11 = this.m10 * right.m01 + this.m11 * right.m11 + this.m12 * right.m21 + this.m13 * this.m31;
        float m12 = this.m10 * right.m02 + this.m11 * right.m12 + this.m12 * right.m22 + this.m13 * this.m32;
        float m13 = this.m10 * right.m03 + this.m11 * right.m13 + this.m12 * right.m23 + this.m13 * this.m33;

        float m20 = this.m20 * right.m00 + this.m21 * right.m10 + this.m22 * right.m20 + this.m23 * this.m30;
        float m21 = this.m20 * right.m01 + this.m21 * right.m11 + this.m22 * right.m21 + this.m23 * this.m31;
        float m22 = this.m20 * right.m02 + this.m21 * right.m12 + this.m22 * right.m22 + this.m23 * this.m32;
        float m23 = this.m20 * right.m03 + this.m21 * right.m13 + this.m22 * right.m23 + this.m23 * this.m33;

        float m30 = this.m30 * right.m00 + this.m31 * right.m10 + this.m32 * right.m20 + this.m33 * this.m30;
        float m31 = this.m30 * right.m01 + this.m31 * right.m11 + this.m32 * right.m21 + this.m33 * this.m31;
        float m32 = this.m30 * right.m02 + this.m31 * right.m12 + this.m32 * right.m22 + this.m33 * this.m32;
        float m33 = this.m30 * right.m03 + this.m31 * right.m13 + this.m32 * right.m23 + this.m33 * this.m33;

        target.m00 = m00;
        target.m01 = m01;
        target.m02 = m02;
        target.m03 = m03;

        target.m10 = m10;
        target.m11 = m11;
        target.m12 = m12;
        target.m13 = m13;

        target.m20 = m20;
        target.m21 = m21;
        target.m22 = m22;
        target.m23 = m23;

        target.m30 = m30;
        target.m31 = m31;
        target.m32 = m32;
        target.m33 = m33;

        return target;
    }

    public Vector4f transform(Vector4f in) {
        return transform(in, new Vector4f());
    }

    public Vector4f transform(Vector4f in, Vector4f target) {
        float x = in.x * m00 + in.y * m01 + in.z * m02 + in.w * m03;
        float y = in.x * m10 + in.y * m11 + in.z * m12 + in.w * m13;
        float z = in.x * m20 + in.y * m21 + in.z * m22 + in.w * m23;
        float w = in.x * m30 + in.y * m31 + in.z * m32 + in.w * m33;

        target.x = x;
        target.y = y;
        target.z = z;
        target.w = w;

        return target;
    }

    public static Matrix4f scale(float factor) {
        return scale(factor, factor, factor);
    }

    public static Matrix4f scale(float x, float y, float z) {
        Matrix4f mat = new Matrix4f();
        mat.m00 = x;
        mat.m11 = y;
        mat.m22 = z;
        return mat;
    }

    public static Matrix4f translate(float x, float y, float z) {
        Matrix4f mat = new Matrix4f();
        mat.m03 = x;
        mat.m13 = y;
        mat.m23 = z;
        return mat;
    }

    public static Matrix4f rotateX(float rad) {
        float cos = (float)Math.cos(rad);
        float sin = (float)Math.sin(rad);
        return new Matrix4f(
                1,    0,    0, 0,
                0,  cos,  sin, 0,
                0, -sin,  cos, 0,
                0,    0,    0, 1);
    }

    public static Matrix4f rotateY(float rad) {
        float cos = (float)Math.cos(rad);
        float sin = (float)Math.sin(rad);
        return new Matrix4f(
                cos,    0, -sin, 0,
                  0,    1,    0, 0,
                sin,    0,  cos, 0,
                  0,    0,    0, 1);
    }

    public static Matrix4f rotateZ(float rad) {
        float cos = (float)Math.cos(rad);
        float sin = (float)Math.sin(rad);
        return new Matrix4f(
                cos, -sin, 0, 0,
                sin,  cos, 0, 0,
                  0,    0, 1, 0,
                  0,    0, 0, 1);
    }
}
