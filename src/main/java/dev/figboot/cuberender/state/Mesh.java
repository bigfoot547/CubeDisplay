package dev.figboot.cuberender.state;

import dev.figboot.cuberender.math.MathUtil;
import dev.figboot.cuberender.math.Vector2f;
import dev.figboot.cuberender.math.Vector4f;
import dev.figboot.cuberender.math.Vector4f;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class Mesh<T> implements Sampleable<T> {
    final Vector4f[] vertices;
    final Vector4f[] normals;
    final int[] indices;

    final Map<AttachmentType, Object> attachments;

    protected void applyLighting(Vector4f color, Vector4f normal) {
        Float lightFact = (Float)attachments.get(AttachmentType.LIGHT_FACTOR);

        if (lightFact == null) {
            return;
        }

        float fact = 1 - (normal.dot((Vector4f)attachments.get(AttachmentType.LIGHT_VECTOR)) + 1) / 2;
        fact *= lightFact; // lightFact should kinda set the "black level"
        fact = 1 - fact;

        fact = MathUtil.clamp(fact, 0, 1);

        color.x *= fact;
        color.y *= fact;
        color.z *= fact;
    }

    public static class Builder {
        private final List<Vector4f> vertices = new ArrayList<>();
        private final List<Vector4f> normals = new ArrayList<>();
        private final List<Integer> indices = new ArrayList<>();
        private final List<Vector2f> texCoords = new ArrayList<>();
        private int color;
        private Texture texture;

        private final Map<AttachmentType, Object> attachments = new EnumMap<>(AttachmentType.class);

        public Builder() {
            color = 0xFF000000;
            texture = null;
        }

        public Builder color(int color) {
            this.color = color;
            return this;
        }

        public Builder texture(Texture t) {
            this.texture = t;
            return this;
        }

        public Builder texCoords(Vector2f... tex) {
            texCoords.addAll(Arrays.asList(tex));
            return this;
        }

        public Builder vertex(Vector4f... vert) {
            vertices.addAll(Arrays.asList(vert));
            return this;
        }

        public Builder normals(Vector4f... norm) {
            normals.addAll(Arrays.asList(norm));
            return this;
        }

        public Builder indices(int... indices) {
            for (int idx : indices) {
                this.indices.add(idx);
            }
            return this;
        }

        public Builder attach(AttachmentType type, Object o) {
            attachments.put(type, o);
            return this;
        }

        public Mesh<?> build() {
            int[] idxArr;
            if (indices.isEmpty()) {
                idxArr = null;
            } else {
                idxArr = new int[indices.size()];
                for (int i = 0, max = indices.size(); i < max; ++i) {
                    idxArr[i] = indices.get(i);
                }
            }

            if (texture == null) {
                return new ColorMesh(vertices.toArray(new Vector4f[0]), normals.toArray(new Vector4f[0]), idxArr, attachments, color);
            } else {
                return new TextureMesh(vertices.toArray(new Vector4f[0]), normals.toArray(new Vector4f[0]), idxArr, attachments, texture, texCoords.toArray(new Vector2f[0]));
            }
        }
    }

    private static class ColorMesh extends Mesh<Void> {
        int color;

        ColorMesh(Vector4f[] vertices, Vector4f[] normals, int[] indices, Map<AttachmentType, Object> attachments, int color) {
            super(vertices, normals, indices, attachments);
            this.color = color;
        }

        @Override
        public Void extra(int idx) {
            return null;
        }

        @Override
        public void sample(float b0, float b1, float b2, Vector4f normal, Void u1, Void u2, Void u3, Vector4f outColor) {
            applyLighting(outColor.fromARGB(color), normal);
        }
    }

    private static class TextureMesh extends Mesh<Vector2f> {
        Texture texture;
        Vector2f[] texCoords;

        TextureMesh(Vector4f[] vertices, Vector4f[] normals, int[] indices, Map<AttachmentType, Object> attachments, Texture tex, Vector2f[] texCoords) {
            super(vertices, normals, indices, attachments);
            this.texture = tex;
            this.texCoords = texCoords;
        }

        @Override
        public Vector2f extra(int idx) {
            return texCoords[idx];
        }

        @Override
        public void sample(float b0, float b1, float b2, Vector4f normal, Vector2f tc1, Vector2f tc2, Vector2f tc3, Vector4f color) {
            float texX = b0 * tc1.x + b1 * tc2.x + b2 * tc3.x;
            float texY = b0 * tc1.y + b1 * tc2.y + b2 * tc3.y;

            int texiX = (int)Math.min(texture.width-1, Math.max(0, Math.floor(texX * texture.width)));
            int texiY = (int)Math.min(texture.height-1, Math.max(0, Math.floor((1f - texY) * texture.height)));

            applyLighting(color.fromARGB(texture.image.getRGB(texiX, texiY)), normal);
        }
    }

    public enum AttachmentType {
        LIGHT_FACTOR, // float
        LIGHT_VECTOR  // Vector4f
    }
}
