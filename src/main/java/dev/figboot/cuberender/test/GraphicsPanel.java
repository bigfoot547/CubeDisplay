package dev.figboot.cuberender.test;

import dev.figboot.cuberender.math.*;
import dev.figboot.cuberender.state.BlendMode;
import dev.figboot.cuberender.state.Framebuffer;
import dev.figboot.cuberender.state.Mesh;
import dev.figboot.cuberender.state.Texture;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;

public class GraphicsPanel extends JPanel {
    private Framebuffer framebuffer;

    private final EnumMap<BodyPart, Mesh<?>> meshes = new EnumMap<>(BodyPart.class);
    private float xRot = 0, yRot = 0, capeRot = 0;

    private final long[] clrTime = new long[32];
    private final long[] meshTime = new long[32];
    private int tidx = 0;
    boolean rollOver = false;

    private Matrix4f defaultTransform, capeTransform;

    private static void addCuboid(Mesh.Builder mb, float x1, float y1, float z1, float x2, float y2, float z2, float tx, float ty, float tspanX, float tspanY, float tspanZ, float aspect, int ibase) {
        mb.vertex(new Vector4f(x1, y1, z2), /* front */
                        new Vector4f(x1, y2, z2),
                        new Vector4f(x2, y2, z2),
                        new Vector4f(x2, y1, z2),

                        new Vector4f(x2, y1, z2), /* +X side */
                        new Vector4f(x2, y2, z2),
                        new Vector4f(x2, y2, z1),
                        new Vector4f(x2, y1, z1),

                        new Vector4f(x2, y1, z1), /* back */
                        new Vector4f(x2, y2, z1),
                        new Vector4f(x1, y2, z1),
                        new Vector4f(x1, y1, z1),

                        new Vector4f(x1, y1, z1), /* -X side */
                        new Vector4f(x1, y2, z1),
                        new Vector4f(x1, y2, z2),
                        new Vector4f(x1, y1, z2),

                        new Vector4f(x1, y1, z1), /* top */
                        new Vector4f(x1, y1, z2),
                        new Vector4f(x2, y1, z2),
                        new Vector4f(x2, y1, z1),

                        new Vector4f(x1, y2, z1), /* bottom */
                        new Vector4f(x1, y2, z2),
                        new Vector4f(x2, y2, z2),
                        new Vector4f(x2, y2, z1))
                .normals(new Vector4f(0, 0, 1, 0),
                        new Vector4f(0, 0, 1, 0),

                        new Vector4f(1, 0, 0, 0),
                        new Vector4f(1, 0, 0, 0),

                        new Vector4f(0, 0, -1, 0),
                        new Vector4f(0, 0, -1, 0),

                        new Vector4f(-1, 0, 0, 0),
                        new Vector4f(-1, 0, 0, 0),

                        new Vector4f(0, -1, 0, 0),
                        new Vector4f(0, -1, 0, 0),

                        new Vector4f(0, 1, 0, 0),
                        new Vector4f(0, 1, 0, 0))
                .texCoords(new Vector2f(tx, ty),
                        new Vector2f(tx, ty - tspanY),
                        new Vector2f(tx + tspanX, ty - tspanY),
                        new Vector2f(tx + tspanX, ty),

                        new Vector2f(tx + tspanX, ty),
                        new Vector2f(tx + tspanX, ty - tspanY),
                        new Vector2f(tx + tspanX + tspanZ, ty - tspanY),
                        new Vector2f(tx + tspanX + tspanZ, ty),

                        new Vector2f(tx + tspanX + tspanZ, ty),
                        new Vector2f(tx + tspanX + tspanZ, ty - tspanY),
                        new Vector2f(tx + 2 * tspanX + tspanZ, ty - tspanY),
                        new Vector2f(tx + 2 * tspanX + tspanZ, ty),

                        new Vector2f(tx - tspanZ, ty),
                        new Vector2f(tx - tspanZ, ty - tspanY),
                        new Vector2f(tx, ty - tspanY),
                        new Vector2f(tx, ty),

                        new Vector2f(tx, ty + tspanZ),
                        new Vector2f(tx, ty),
                        new Vector2f(tx + tspanX, ty),
                        new Vector2f(tx + tspanX, ty + (tspanZ / aspect)),

                        new Vector2f(tx + tspanX, ty + (tspanZ / aspect)),
                        new Vector2f(tx + tspanX, ty),
                        new Vector2f(tx + 2 * tspanX, ty),
                        new Vector2f(tx + 2 * tspanX, ty + tspanZ))
                .indices(ibase, ibase + 1, ibase + 2, ibase, ibase + 2, ibase + 3,
                        ibase + 4, ibase + 5, ibase + 6, ibase + 4, ibase + 6, ibase + 7,
                        ibase + 8, ibase + 9, ibase + 10, ibase + 8, ibase + 10, ibase + 11,
                        ibase + 12, ibase + 13, ibase + 14, ibase + 12, ibase + 14, ibase + 15,
                        ibase + 16, ibase + 17, ibase + 18, ibase + 16, ibase + 18, ibase + 19,
                        ibase + 20, ibase + 21, ibase + 22, ibase + 20, ibase + 22, ibase + 23);
    }

    private void copyLimbFlipped(BufferedImage src, BufferedImage target) {
        for (int y = 4, maxY = src.getHeight(); y < maxY; ++y) {
            for (int x = 0; x < 4; ++x) {
                target.setRGB(x, y, src.getRGB(11 - x, y));
                target.setRGB(x + 4, y, src.getRGB(7 - x, y));
                target.setRGB(x + 8, y, src.getRGB(3 - x, y));
                target.setRGB(x + 12, y, src.getRGB(15 - x, y));
            }
        }

        for (int y = 0; y < 4; ++y) {
            for (int x = 0; x < 4; ++x) {
                target.setRGB(x + 4, y, src.getRGB(7 - x, y));
                target.setRGB(x + 8, y, src.getRGB(11 - x, y));
            }
        }
    }

    private BufferedImage convertToModernSkin(BufferedImage bi) {
        BufferedImage realBI = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = realBI.createGraphics();

        g2d.drawImage(bi, 0, 0, 64, 32, null);

        // TODO: this is incomplete. the actual game mirrors the arm (so there is always an "inside" and an "outside")
        copyLimbFlipped(bi.getSubimage(0, 16, 16, 16), realBI.getSubimage(16, 48, 16, 16));
        copyLimbFlipped(bi.getSubimage(40, 16, 16, 16), realBI.getSubimage(32, 48, 16, 16));
        g2d.dispose();

        try {
            ImageIO.write(realBI, "PNG", new File("test.png"));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return realBI;
    }

    private static Mesh.Builder defaultBuilder() {
        return new Mesh.Builder().attach(Mesh.AttachmentType.LIGHT_FACTOR, 1f)
                .attach(Mesh.AttachmentType.LIGHT_VECTOR, new Vector4f(0, 0, 1, 0));
    }

    public GraphicsPanel() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                handleResize(getWidth(), getHeight());
            }
        });

        BufferedImage bi;
        try (InputStream is = getClass().getResourceAsStream("/translucent.png")) {
            bi = ImageIO.read(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BufferedImage capeBI;
        try (InputStream is = getClass().getResourceAsStream("/cape.png")) {
            capeBI = ImageIO.read(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (bi.getHeight() == 32) {
            bi = convertToModernSkin(bi);
        }

        Texture tex = new Texture(bi);

        Mesh.Builder bodyBuilder = defaultBuilder().texture(tex);

        // TODO: slim model
        boolean slim = false;
        float overlayOffset = 1/64f;
        float aspect = tex.calcAspect();

        int idxbase = -24;
        // head
        addCuboid(bodyBuilder,
                -4 / 16f, -16 / 16f, -4 / 16f,
                 4 / 16f,  -8 / 16f,  4 / 16f,
                 1 /  8f,   7 /  8f,
                 1 /  8f,   1 /  8f,  1 / 8f, aspect, idxbase += 24);

        // torso
        addCuboid(bodyBuilder,
                -4 / 16f, -8 / 16f, -2 / 16f,
                 4 / 16f,  4 / 16f,  2 / 16f,
                 5 / 16f, 11 / 16f,
                 1 / 8f,   3 / 16f,  1 / 16f, aspect, idxbase += 24);

        // left+right arm
        addCuboid(bodyBuilder,
                (slim ? -7 : -8) / 16f, -8 / 16f, -2 / 16f,
                -4 / 16f,  4 / 16f,  2 / 16f,
                11 / 16f, 11 / 16f,
                (slim ? 3 / 64f : 1 / 16f),  3 / 16f,  1 / 16f, aspect, idxbase += 24);
        addCuboid(bodyBuilder,
                 4 / 16f, -8 / 16f, -2 / 16f,
                (slim ? 7 : 8) / 16f,  4 / 16f,  2 / 16f,
                 9 / 16f,  3 / 16f,
                (slim ? 3 / 64f : 1 / 16f),  3 / 16f,  1 / 16f, aspect, idxbase += 24);

        // left+right leg
        addCuboid(bodyBuilder,
                -4 / 16f,  4 / 16f, -2 / 16f,
                 0 / 16f, 16 / 16f,  2 / 16f,
                 1 / 16f, 11 / 16f,
                 1 / 16f,  3 / 16f,  1 / 16f, aspect, idxbase += 24);
        addCuboid(bodyBuilder,
                 0 / 16f,  4 / 16f, -2 / 16f,
                 4 / 16f, 16 / 16f,  2 / 16f,
                 5 / 16f,  3 / 16f,
                 1 / 16f,  3 / 16f,  1 / 16f, aspect, idxbase += 24);
        meshes.put(BodyPart.MAIN, bodyBuilder.build());

        Mesh.Builder hatBuilder = defaultBuilder().texture(tex);
        addCuboid(hatBuilder,
                -4 / 16f - overlayOffset, -16 / 16f - overlayOffset, -4 / 16f - overlayOffset,
                 4 / 16f + overlayOffset,  -8 / 16f + overlayOffset,  4 / 16f + overlayOffset,
                 5 /  8f,   7 /  8f,
                 1 /  8f,   1 /  8f,  1 / 8f, aspect, 0);

        meshes.put(BodyPart.HAT, hatBuilder.build());

        overlayOffset *= 1.01f;
        Mesh.Builder torsoBuilder = defaultBuilder().texture(tex);
        addCuboid(torsoBuilder,
                -4 / 16f - overlayOffset, -8 / 16f - overlayOffset, -2 / 16f - overlayOffset,
                 4 / 16f + overlayOffset,  4 / 16f + overlayOffset,  2 / 16f + overlayOffset,
                 5 / 16f,  7 / 16f,
                 1 / 8f,   3 / 16f,  1 / 16f, aspect, 0);

        meshes.put(BodyPart.TORSO_OVERLAY, torsoBuilder.build());

        overlayOffset /= 1.01f;
        Mesh.Builder leftArmBuilder = defaultBuilder().texture(tex);
        addCuboid(leftArmBuilder,
                (slim ? -7 : -8) / 16f - overlayOffset, -8 / 16f - overlayOffset, -2 / 16f - overlayOffset,
                -4 / 16f + overlayOffset,  4 / 16f + overlayOffset,  2 / 16f + overlayOffset,
                 13 / 16f, 3 / 16f,
                (slim ? 3 / 64f : 1 / 16f),  3 / 16f,  1 / 16f, aspect, 0);

        meshes.put(BodyPart.LEFT_ARM_OVERLAY, leftArmBuilder.build());

        Mesh.Builder rightArmBuilder = defaultBuilder().texture(tex);
        addCuboid(rightArmBuilder,
                 4 / 16f - overlayOffset, -8 / 16f - overlayOffset, -2 / 16f - overlayOffset,
                (slim ? 7 : 8) / 16f + overlayOffset,  4 / 16f + overlayOffset,  2 / 16f + overlayOffset,
                11 / 16f,  7 / 16f,
                (slim ? 3 / 64f : 1 / 16f),  3 / 16f,  1 / 16f, aspect, 0);

        meshes.put(BodyPart.RIGHT_ARM_OVERLAY, rightArmBuilder.build());

        overlayOffset /= 1.01f;
        Mesh.Builder leftLegBuilder = defaultBuilder().texture(tex);
        addCuboid(leftLegBuilder,
                -4 / 16f - overlayOffset,  4 / 16f - overlayOffset, -2 / 16f - overlayOffset,
                 0 / 16f + overlayOffset, 16 / 16f + overlayOffset,  2 / 16f + overlayOffset,
                 1 / 16f,  7 / 16f,
                 1 / 16f,  3 / 16f,  1 / 16f, aspect, 0);
        meshes.put(BodyPart.LEFT_LEG_OVERLAY, leftLegBuilder.build());

        overlayOffset /= 1.01f;
        Mesh.Builder rightLegBuilder = defaultBuilder().texture(tex);
        addCuboid(rightLegBuilder,
                0 / 16f - overlayOffset,  4 / 16f - overlayOffset, -2 / 16f - overlayOffset,
                4 / 16f + overlayOffset, 16 / 16f + overlayOffset,  2 / 16f + overlayOffset,
                1 / 16f,  3 / 16f,
                1 / 16f,  3 / 16f,  1 / 16f, aspect, 0);
        meshes.put(BodyPart.RIGHT_LEG_OVERLAY, rightLegBuilder.build());

        Texture capeTex = new Texture(capeBI);

        Mesh.Builder capeBuilder = defaultBuilder().texture(capeTex);
        addCuboid(capeBuilder,
                -4/16f, 0, 0,
                 4/16f, 16/16f, 1/16f,
                1/64f, 31/32f,
                10/64f, 16/32f, 1/64f, capeTex.calcAspect(), 0);
        meshes.put(BodyPart.CAPE, capeBuilder.build());
    }

    private void handleResize(int width, int height) {
        framebuffer = new Framebuffer(width, height);
        updateTransform();
    }

    private void updateTransform() {
        defaultTransform = Matrix4f.rotateY(yRot).times(Matrix4f.rotateX(xRot)).times(Matrix4f.scale(0.75f));
        capeTransform = new Matrix4f(defaultTransform).times(Matrix4f.scale(-1, 1, -1)).times(Matrix4f.translate(0, -8/16f, 2/16f)).times(Matrix4f.rotateX(capeRot));
    }

    @Override
    public void paintComponent(Graphics g) {
        if (framebuffer == null) handleResize(getWidth(), getHeight());

        long start = System.nanoTime();
        framebuffer.setBlendMode(BlendMode.DISABLE);
        framebuffer.setCullBackFace(true);
        framebuffer.clear(Framebuffer.FB_CLEAR_COLOR | Framebuffer.FB_CLEAR_DEPTH, 0xFF000000);
        long t1 = System.nanoTime();

        framebuffer.setTransform(capeTransform);
        framebuffer.drawMesh(meshes.get(BodyPart.CAPE));

        framebuffer.setTransform(defaultTransform);
        framebuffer.setDepthMode(Framebuffer.FB_DEPTH_COMMIT | Framebuffer.FB_DEPTH_USE);
        framebuffer.drawMesh(meshes.get(BodyPart.MAIN));

        framebuffer.setDepthMode(Framebuffer.FB_DEPTH_USE | Framebuffer.FB_DEPTH_COMMIT_TRANSPARENT);
        framebuffer.setBlendMode(BlendMode.BLEND_OVER);
        framebuffer.setCullBackFace(false);
        framebuffer.drawMesh(meshes.get(BodyPart.HAT));
        framebuffer.drawMesh(meshes.get(BodyPart.TORSO_OVERLAY));
        framebuffer.drawMesh(meshes.get(BodyPart.LEFT_ARM_OVERLAY));
        framebuffer.drawMesh(meshes.get(BodyPart.RIGHT_ARM_OVERLAY));
        framebuffer.drawMesh(meshes.get(BodyPart.LEFT_LEG_OVERLAY));
        framebuffer.drawMesh(meshes.get(BodyPart.RIGHT_LEG_OVERLAY));
        long t2 = System.nanoTime();

        g.clearRect(0, 0, getWidth(), getHeight());
        g.drawImage(framebuffer.getColor(), 0, 0, null);

        g.setColor(Color.RED);

        addTiming(t1 - start, t2 - t1);

        int y = -2;
        g.drawString(String.format("tot %.02fms", (t2 - start) / 1000000.), 10, y += 12);
        g.drawString(String.format("clr %.02fms", (t1 - start) / 1000000.), 10, y += 12);
        g.drawString(String.format("msh %.02fms", (t2 - t1) / 1000000.), 10, y += 12);
        g.drawString(getAvgClr(), 10, y += 12);
        g.drawString(String.format("%dx%d", framebuffer.getWidth(), framebuffer.getHeight()), 10, y += 12);
    }

    private void addTiming(long clr, long msh) {
        clrTime[tidx] = clr;
        meshTime[tidx] = msh;

        if (++tidx >= 32) {
            tidx = 0;
            rollOver = true;
        }
    }

    private String getAvgClr() {
        int n = rollOver ? 32 : tidx;
        if (n == 0) return "avg ???";

        long sumClr = 0, sumMsh = 0;

        for (int i = 0; i < n; ++i) {
            sumClr += clrTime[i];
            sumMsh += meshTime[i];
        }

        return String.format("avg %.02fms clr %.02fms msh", sumClr / (double)n / 1000000, sumMsh / (double)n / 1000000);
    }

    public void setYRot(float rad) {
        this.yRot = rad;
        updateTransform();
    }

    public void setXRot(float rad) {
        this.xRot = rad;
        updateTransform();
    }

    public void setCapeRot(float rad) {
        this.capeRot = rad;
        updateTransform();
    }

    public enum BodyPart {
        MAIN,
        HAT,
        TORSO_OVERLAY,
        LEFT_ARM_OVERLAY,
        RIGHT_ARM_OVERLAY,
        LEFT_LEG_OVERLAY,
        RIGHT_LEG_OVERLAY,
        CAPE
    }
}
