package dev.figboot.cuberender.test;

import dev.figboot.cuberender.math.*;
import dev.figboot.cuberender.state.BlendMode;
import dev.figboot.cuberender.state.Framebuffer;
import dev.figboot.cuberender.state.Mesh;
import dev.figboot.cuberender.state.Texture;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

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

    @Setter private boolean translucentModel = true, normalModel = true;
    @Setter private float walkAngle, capeAngle, worldRotY, worldRotX, headPitch;

    public static final int OVERLAY_HAT = 0x0001;
    public static final int OVERLAY_TORSO = 0x0002;
    public static final int OVERLAY_LEFT_ARM = 0x0004;
    public static final int OVERLAY_RIGHT_ARM = 0x0008;
    public static final int OVERLAY_LEFT_LEG = 0x0010;
    public static final int OVERLAY_RIGHT_LEG = 0x0020;

    public static final int OVERLAY_CAPE = 0x0100;

    private int renderOverlayFlags;

    private final long[] clrTime = new long[32];
    private final long[] meshTime = new long[32];
    private int tidx = 0;
    boolean rollOver = false;

    private final EnumMap<BodyPart, Matrix4f> transforms = new EnumMap<>(BodyPart.class);

    private static final BodyPart[] MAIN_PARTS = new BodyPart[]{BodyPart.HEAD, BodyPart.TORSO, BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG};
    private static final BodyPart[] MAIN_PARTS_SLIM = new BodyPart[]{BodyPart.HEAD, BodyPart.TORSO, BodyPart.LEFT_ARM_SLIM, BodyPart.RIGHT_ARM_SLIM, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG};

    private final BodyPart[] OVERLAY_PARTS = new BodyPart[]{BodyPart.HAT, BodyPart.TORSO_OVERLAY, BodyPart.LEFT_ARM_OVERLAY, BodyPart.RIGHT_ARM_OVERLAY, BodyPart.LEFT_LEG_OVERLAY, BodyPart.RIGHT_LEG_OVERLAY};
    private final BodyPart[] OVERLAY_PARTS_SLIM = new BodyPart[]{BodyPart.HAT, BodyPart.TORSO_OVERLAY, BodyPart.LEFT_ARM_OVERLAY_SLIM, BodyPart.RIGHT_ARM_OVERLAY_SLIM, BodyPart.LEFT_LEG_OVERLAY, BodyPart.RIGHT_LEG_OVERLAY};

    private BodyPart[] overlayParts;
    private BodyPart[] overlayPartsSlim;

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

    public GraphicsPanel() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                handleResize(getWidth(), getHeight());
            }
        });

        BufferedImage bi;
        try (InputStream is = getClass().getResourceAsStream("/skinSlim.png")) {
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
        Texture capeTex = new Texture(capeBI);

        for (BodyPart part : BodyPart.values()) {
            meshes.put(part, part.toBuilder(part != BodyPart.CAPE ? tex : capeTex, 0)
                    .attach(Mesh.AttachmentType.LIGHT_FACTOR, 1f)
                    .attach(Mesh.AttachmentType.LIGHT_VECTOR, new Vector4f(0, 0, 1, 0)).build());
        }
    }

    private void updateOverlayParts() {
        int realOverlayParts = this.renderOverlayFlags & ~OVERLAY_CAPE;
        int nParts = Integer.bitCount(realOverlayParts);
        int idx = 0;

        if (nParts == 0) {
            overlayParts = overlayPartsSlim = new BodyPart[0];
            return;
        }

        overlayParts = new BodyPart[nParts];
        overlayPartsSlim = new BodyPart[nParts];

        for (int i = 0; idx < nParts && i < OVERLAY_PARTS.length; ++i) {
            if ((realOverlayParts & (1 << i)) != 0) {
                overlayParts[idx] = OVERLAY_PARTS[i];
                overlayPartsSlim[idx] = OVERLAY_PARTS_SLIM[i];

                ++idx;
            }
        }
    }

    public void setRenderOverlayFlags(int flags) {
        this.renderOverlayFlags = flags;
        updateOverlayParts();
    }

    private void handleResize(int width, int height) {
        framebuffer = new Framebuffer(width, height);
        updateTransform();
    }

    public void updateTransform() {
        transforms.put(BodyPart.HEAD, calculateTransform(BodyPart.HEAD, headPitch, 0));
        transforms.put(BodyPart.TORSO, calculateTransform(BodyPart.TORSO, 0, 0));
        transforms.put(BodyPart.LEFT_ARM, calculateTransform(BodyPart.LEFT_ARM, walkAngle, 0));
        transforms.put(BodyPart.LEFT_ARM_SLIM, calculateTransform(BodyPart.LEFT_ARM_SLIM, walkAngle, 0));
        transforms.put(BodyPart.RIGHT_ARM, calculateTransform(BodyPart.RIGHT_ARM, -walkAngle, 0));
        transforms.put(BodyPart.RIGHT_ARM_SLIM, calculateTransform(BodyPart.RIGHT_ARM_SLIM, -walkAngle, 0));
        transforms.put(BodyPart.LEFT_LEG, calculateTransform(BodyPart.LEFT_LEG, walkAngle, 0));
        transforms.put(BodyPart.RIGHT_LEG, calculateTransform(BodyPart.RIGHT_LEG, -walkAngle, 0));

        transforms.put(BodyPart.HAT, calculateTransform(BodyPart.HEAD, headPitch, 0));
        transforms.put(BodyPart.TORSO_OVERLAY, calculateTransform(BodyPart.TORSO_OVERLAY, 0, 0));
        transforms.put(BodyPart.LEFT_ARM_OVERLAY, calculateTransform(BodyPart.LEFT_ARM_OVERLAY, walkAngle, 0));
        transforms.put(BodyPart.LEFT_ARM_OVERLAY_SLIM, calculateTransform(BodyPart.LEFT_ARM_OVERLAY_SLIM, walkAngle, 0));
        transforms.put(BodyPart.RIGHT_ARM_OVERLAY, calculateTransform(BodyPart.RIGHT_ARM_OVERLAY, -walkAngle, 0));
        transforms.put(BodyPart.RIGHT_ARM_OVERLAY_SLIM, calculateTransform(BodyPart.RIGHT_ARM_OVERLAY_SLIM, -walkAngle, 0));
        transforms.put(BodyPart.LEFT_LEG_OVERLAY, calculateTransform(BodyPart.LEFT_LEG_OVERLAY, walkAngle, 0));
        transforms.put(BodyPart.RIGHT_LEG_OVERLAY, calculateTransform(BodyPart.RIGHT_LEG_OVERLAY, -walkAngle, 0));

        transforms.put(BodyPart.CAPE, calculateTransform(BodyPart.CAPE, capeAngle, 0));

        for (BodyPart part : BodyPart.values()) {
            transforms.put(part, Matrix4f.scale(0.75f).times(Matrix4f.rotateX(worldRotX)).times(Matrix4f.rotateY(worldRotY)).times(transforms.get(part)));
        }
    }

    /* f1 and f2 control part-specific stuff (f1 is usually main rotation) */
    private Matrix4f calculateTransform(BodyPart part, float f1, float f2) {
        switch (part) {
            case HEAD:
            case HAT:
                return Matrix4f.translate(0, -8/16f, 0).times(Matrix4f.rotateX(f1)).times(Matrix4f.translate(0, -4/16f, 0));
            case TORSO:
            case TORSO_OVERLAY:
                return Matrix4f.translate(0, -2/16f, 0);
            case LEFT_ARM:
            case LEFT_ARM_OVERLAY:
                return Matrix4f.translate(-6/16f, -6/16f, 0).times(Matrix4f.rotateX(f1).times(Matrix4f.translate(0, 4/16f, 0)));
            case LEFT_ARM_SLIM:
            case LEFT_ARM_OVERLAY_SLIM:
                return Matrix4f.translate(-5.5f/16f, -6/16f, 0).times(Matrix4f.rotateX(f1).times(Matrix4f.translate(0, 4/16f, 0)));
            case RIGHT_ARM:
            case RIGHT_ARM_OVERLAY:
                return Matrix4f.translate(6/16f, -6/16f, 0).times(Matrix4f.rotateX(f1).times(Matrix4f.translate(0, 4/16f, 0)));
            case RIGHT_ARM_SLIM:
            case RIGHT_ARM_OVERLAY_SLIM:
                return Matrix4f.translate(5.5f/16f, -6/16f, 0).times(Matrix4f.rotateX(f1).times(Matrix4f.translate(0, 4/16f, 0)));
            case LEFT_LEG:
            case LEFT_LEG_OVERLAY:
                return Matrix4f.translate(-2f/16f, 4/16f, 0).times(Matrix4f.rotateX(f1).times(Matrix4f.translate(0, 6/16f, 0)));
            case RIGHT_LEG:
            case RIGHT_LEG_OVERLAY:
                return Matrix4f.translate(2f/16f, 4/16f, 0).times(Matrix4f.rotateX(f1).times(Matrix4f.translate(0, 6/16f, 0)));
            case CAPE:
                return Matrix4f.translate(0, -8/16f, -2/16f).times(Matrix4f.rotateX(f1).times(Matrix4f.translate(0, 8/16f, 0).times(Matrix4f.scale(-1, 1, -1))));
            default:
                throw new IllegalArgumentException("invalid body part");
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        if (framebuffer == null) handleResize(getWidth(), getHeight());

        long start = System.nanoTime();

        BodyPart[] main, overlay;
        if (normalModel) {
            main = MAIN_PARTS;
            overlay = overlayParts;
        } else {
            main = MAIN_PARTS_SLIM;
            overlay = overlayPartsSlim;
        }

        framebuffer.setBlendMode(BlendMode.DISABLE);
        framebuffer.setCullBackFace(true);
        framebuffer.clear(Framebuffer.FB_CLEAR_COLOR | Framebuffer.FB_CLEAR_DEPTH, 0xFF000000);

        long t1 = System.nanoTime();

        if ((renderOverlayFlags & OVERLAY_CAPE) != 0) {
            framebuffer.setTransform(transforms.get(BodyPart.CAPE));
            framebuffer.drawMesh(meshes.get(BodyPart.CAPE));
        }

        framebuffer.setDepthMode(Framebuffer.FB_DEPTH_COMMIT | Framebuffer.FB_DEPTH_USE);

        for (BodyPart part : main) {
            framebuffer.setTransform(transforms.get(part));
            framebuffer.drawMesh(meshes.get(part));
        }

        if (translucentModel) {
            framebuffer.setDepthMode(Framebuffer.FB_DEPTH_USE | Framebuffer.FB_DEPTH_COMMIT_TRANSPARENT);
            framebuffer.setBlendMode(BlendMode.BLEND_OVER);
            framebuffer.setCullBackFace(false);
        } else {
            framebuffer.setBlendMode(BlendMode.BINARY);
        }

        if (overlay != null) {
            for (BodyPart part : overlay) {
                framebuffer.setTransform(transforms.get(part));
                framebuffer.drawMesh(meshes.get(part));
            }
        }

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

    @RequiredArgsConstructor
    public enum BodyPart {
        HEAD(8/16f, 8/16f, 8/16f, 8/64f, 56/64f),
        TORSO(8/16f, 12/16f, 4/16f, 20/64f, 44/64f),
        LEFT_ARM(4/16f, 12/16f, 4/16f, 44/64f, 44/64f),
        RIGHT_ARM(4/16f, 12/16f, 4/16f, 36/64f, 12/64f),
        LEFT_ARM_SLIM(3/16f, 12/16f, 4/16f, 44/64f, 44/64f),
        RIGHT_ARM_SLIM(3/16f, 12/16f, 4/16f, 36/64f, 12/64f),
        LEFT_LEG(4/16f, 12/16f, 4/16f, 4/64f, 44/64f, -1),
        RIGHT_LEG(4/16f, 12/16f, 4/16f, 20/64f, 12/64f, -1),
        HAT(8/16f, 8/16f, 8/16f, 40/64f, 56/64f, 0, true),
        TORSO_OVERLAY(8/16f, 12/16f, 4/16f, 20/64f, 28/64f, 2, true),
        LEFT_ARM_OVERLAY(4/16f, 12/16f, 4/16f, 52/64f, 12/64f, 0, true),
        LEFT_ARM_OVERLAY_SLIM(3/16f, 12/16f, 4/16f, 52/64f, 12/64f, 0, true),
        RIGHT_ARM_OVERLAY(4/16f, 12/16f, 4/16f, 44/64f, 28/64f, 0, true),
        RIGHT_ARM_OVERLAY_SLIM(3/16f, 12/16f, 4/16f, 44/64f, 28/64f, 0, true),
        LEFT_LEG_OVERLAY(4/16f, 12/16f, 4/16f, 4/64f, 28/64f, 0, true),
        RIGHT_LEG_OVERLAY(4/16f, 12/16f, 4/16f, 4/64f, 12/64f, 1, true),
        CAPE(8/16f, 16/16f, 1/16f, 1/64f, 31/32f, 10/64f, 16/32f, 1/64f, true);

        private final float xMin, yMin, zMin;
        private final float xMax, yMax, zMax;
        private final float texBaseX, texBaseY;
        private final float texSpanX, texSpanY, texSpanZ;

        private static final float OVERLAY_OFFSET = 2f/64;
        private static final float PLANE_FIGHT_OFFSET = 0.001f;
        private static final float CAPE_OFFSET = -2 * PLANE_FIGHT_OFFSET;

        // for overlay parts
        BodyPart(float spanX, float spanY, float spanZ, float texBaseX, float texBaseY, int planeFightOffset, boolean overlay) {
            this(spanX + (planeFightOffset * PLANE_FIGHT_OFFSET) + OVERLAY_OFFSET,
                    spanY + (planeFightOffset * PLANE_FIGHT_OFFSET) + OVERLAY_OFFSET,
                    spanZ + (planeFightOffset * PLANE_FIGHT_OFFSET) + OVERLAY_OFFSET,
                    texBaseX, texBaseY, spanX / 4, spanY / 4, spanZ / 4);
        }

        // for legs (they visibly plane-fight with torso)
        BodyPart(float spanX, float spanY, float spanZ, float texBaseX, float texBaseY, int planeFightOffset) {
            this(spanX + (planeFightOffset * PLANE_FIGHT_OFFSET),
                    spanY + (planeFightOffset * PLANE_FIGHT_OFFSET),
                    spanZ + (planeFightOffset * PLANE_FIGHT_OFFSET),
                    texBaseX, texBaseY, spanX / 4, spanY / 4, spanZ / 4);
        }

        // for main parts
        BodyPart(float spanX, float spanY, float spanZ, float texBaseX, float texBaseY) {
            this(spanX, spanY, spanZ, texBaseX, texBaseY, spanX / 4, spanY / 4, spanZ / 4);
        }

        @SuppressWarnings("unused")
        BodyPart(float spanX, float spanY, float spanZ, float texBaseX, float texBaseY, float texSpanX, float texSpanY, float texSpanZ, boolean cape) {
            this(spanX + CAPE_OFFSET, spanY + CAPE_OFFSET, spanZ + CAPE_OFFSET, texBaseX, texBaseY, texSpanX, texSpanY, texSpanZ);
        }

        // for cape (it does not follow the convention of 1px per 1/32 logical units)
        BodyPart(float spanX, float spanY, float spanZ, float texBaseX, float texBaseY, float texSpanX, float texSpanY, float texSpanZ) {
            this(-spanX / 2, -spanY / 2, -spanZ / 2, spanX / 2, spanY / 2, spanZ / 2, texBaseX, texBaseY, texSpanX, texSpanY, texSpanZ);
        }

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

        public Mesh.Builder toBuilder(Texture tex, int base) {
            Mesh.Builder builder = new Mesh.Builder().texture(tex);
            addCuboid(builder, xMin, yMin, zMin, xMax, yMax, zMax, texBaseX, texBaseY, texSpanX, texSpanY, texSpanZ, tex.calcAspect(), base);
            return builder;
        }
    }
}
