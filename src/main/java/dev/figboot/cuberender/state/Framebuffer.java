package dev.figboot.cuberender.state;

import dev.figboot.cuberender.math.Matrix4f;
import dev.figboot.cuberender.math.Vector4f;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class Framebuffer {
    public static final int FB_CLEAR_COLOR = 0x01;
    public static final int FB_CLEAR_DEPTH = 0x02;

    public static final int FB_DEPTH_USE = 0x01;
    public static final int FB_DEPTH_COMMIT = 0x02;
    public static final int FB_DEPTH_COMMIT_TRANSPARENT = 0x04;

    @Getter private final int width, height;

    @Getter private final BufferedImage color;
    private final float[] depth;

    @Setter private int depthMode = FB_DEPTH_USE | FB_DEPTH_COMMIT;

    @Setter private Matrix4f transform;

    @Setter private BlendMode blendMode = BlendMode.DISABLE;
    @Setter private boolean cullBackFace = true;

    public Framebuffer(int width, int height) {
        this.width = width;
        this.height = height;

        this.color = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        depth = new float[width * height];
    }

    public void clear(int bits, int color) {
        if ((bits & FB_CLEAR_COLOR) != 0) {
            Graphics gfx = this.color.getGraphics();
            gfx.setColor(new Color(color, true));
            gfx.fillRect(0, 0, width, height);
        }

        if ((bits & FB_CLEAR_DEPTH) != 0) {
            Arrays.fill(depth, Float.NEGATIVE_INFINITY);
        }
    }

    public void drawMesh(Mesh<?> mesh) {
        // this seems redundant but it saves us having to check it each loop iteration
        if (mesh.indices != null) {
            drawIndexedMesh(mesh);
        } else {
            drawFlatMesh(mesh);
        }
    }

    @SuppressWarnings("unchecked")
    private void drawIndexedMesh(Mesh<?> mesh) {
        int ntris = mesh.indices.length / 3;
        Sampleable<Object> s = (Sampleable<Object>)mesh;

        int i0, i1, i2;

        for (int tri = 0; tri < ntris; ++tri) {
            i0 = mesh.indices[tri * 3];
            i1 = mesh.indices[tri * 3 + 1];
            i2 = mesh.indices[tri * 3 + 2];

            Vector4f vert0 = mesh.vertices[i0];
            Vector4f vert1 = mesh.vertices[i1];
            Vector4f vert2 = mesh.vertices[i2];

            drawTriangle(vert0, vert1, vert2, mesh.normals[tri], s, i0, i1, i2);
        }
    }

    @SuppressWarnings("unchecked")
    private void drawFlatMesh(Mesh<?> mesh) {
        int ntris = mesh.vertices.length / 3;
        Sampleable<Object> s = (Sampleable<Object>)mesh;

        int i0, i1, i2;

        for (int tri = 0; tri < ntris; ++tri) {
            i0 = tri * 3;
            i1 = tri * 3 + 1;
            i2 = tri * 3 + 2;

            Vector4f vert0 = mesh.vertices[i0];
            Vector4f vert1 = mesh.vertices[i1];
            Vector4f vert2 = mesh.vertices[i2];

            drawTriangle(vert0, vert1, vert2, mesh.normals[tri], s, i0, i1, i2);
        }
    }

    private float logToScrX(float x) {
        return ((x + 1f) / 2) * width;
    }

    private float logToScrY(float y) {
        return ((y + 1f) / 2) * height;
    }

    // triangles have flat normals (we don't need anything more than that in this renderer and it saves us the trouble of interpolating between 3 normal vectors)
    private void drawTriangle(Vector4f vert0, Vector4f vert1, Vector4f vert2, Vector4f normal, Sampleable<Object> sampleable, int i0, int i1, int i2) {
        Vector4f outColor = new Vector4f(), prevColor = new Vector4f();

        vert0 = transform.transform(vert0);
        vert1 = transform.transform(vert1);
        vert2 = transform.transform(vert2);
        normal = transform.transform(normal).normalize();

        if (cullBackFace && normal.z < 0) {
            return;
        }

        float sx0 = logToScrX(vert0.x), sy0 = logToScrY(vert0.y);
        float sx1 = logToScrX(vert1.x), sy1 = logToScrY(vert1.y);
        float sx2 = logToScrX(vert2.x), sy2 = logToScrY(vert2.y);

        // optimization: Math.floor and Math.ceil convert float arguments to double
        int minX = (int)Math.floor(Math.max(0, Math.min(sx0, Math.min(sx1, sx2))));
        int maxX = (int)Math.ceil(Math.min(width - 1, Math.max(sx0, Math.max(sx1, sx2))));

        int minY = (int)Math.floor(Math.max(0, Math.min(sy0, Math.min(sy1, sy2))));
        int maxY = (int)Math.ceil(Math.min(height - 1, Math.max(sy0, Math.max(sy1, sy2))));

        float area = (sy0 - sy2) * (sx1 - sx2) + (sy1 - sy2) * (sx2 - sx0);

        for (int y = minY; y <= maxY; ++y) {
            for (int x = minX; x < maxX; ++x) {
                float b0 = ((y - sy2) * (sx1 - sx2) + (sy1 - sy2) * (sx2 - x)) / area;
                float b1 = ((y - sy0) * (sx2 - sx0) + (sy2 - sy0) * (sx0 - x)) / area;
                float b2 = ((y - sy1) * (sx0 - sx1) + (sy0 - sy1) * (sx1 - x)) / area;

                if (b0 < 0 || b0 >= 1 || b1 < 0 || b1 >= 1 || b2 < 0 || b2 >= 1) continue;

                float z = b0 * vert0.z + b1 * vert1.z + b2 * vert2.z;
                if ((depthMode & FB_DEPTH_USE) != 0 && z <= depth[y * width + x]) continue;

                if ((depthMode & FB_DEPTH_COMMIT) != 0 && outColor.w > 0) {
                    depth[y * width + x] = z;
                }

                prevColor.fromARGB(color.getRGB(x, y));
                sampleable.sample(b0, b1, b2, normal, sampleable.extra(i0), sampleable.extra(i1), sampleable.extra(i2), outColor);

                if ((depthMode & FB_DEPTH_COMMIT_TRANSPARENT) != 0 && outColor.w > 0) {
                    depth[y * width + x] = z;
                }

                blendMode.getFunction().blend(outColor, prevColor);
                color.setRGB(x, y, outColor.toARGB());
            }
        }
    }
}
