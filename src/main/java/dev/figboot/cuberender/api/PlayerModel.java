package dev.figboot.cuberender.api;

import dev.figboot.cuberender.math.Matrix4f;
import dev.figboot.cuberender.math.Vector2f;
import dev.figboot.cuberender.math.Vector4f;
import dev.figboot.cuberender.state.BlendMode;
import dev.figboot.cuberender.state.Framebuffer;
import dev.figboot.cuberender.state.Mesh;
import dev.figboot.cuberender.state.Texture;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.awt.image.BufferedImage;
import java.util.EnumMap;

/**
 * Represents a player model that can be rendered onto a Framebuffer.
 */
public class PlayerModel {
    /**
     * Head overlay (hat)
     */
    public static final int OVERLAY_HAT = 0x0001;

    /**
     * Torso overlay
     */
    public static final int OVERLAY_TORSO = 0x0002;

    /**
     * Left arm overlay
     */
    public static final int OVERLAY_LEFT_ARM = 0x0004;

    /**
     * Right arm overlay
     */
    public static final int OVERLAY_RIGHT_ARM = 0x0008;

    /**
     * Left leg overlay
     */
    public static final int OVERLAY_LEFT_LEG = 0x0010;

    /**
     * Right leg overlay
     */
    public static final int OVERLAY_RIGHT_LEG = 0x0020;

    /**
     * Cape overlay
     */
    public static final int OVERLAY_CAPE = 0x0100;

    /**
     * All overlay components
     */
    public static final int OVERLAY_ALL = OVERLAY_HAT | OVERLAY_TORSO | OVERLAY_LEFT_ARM | OVERLAY_RIGHT_ARM
            | OVERLAY_LEFT_LEG | OVERLAY_RIGHT_LEG | OVERLAY_CAPE;

    private static final BodyPart[] MAIN_PARTS = new BodyPart[]{BodyPart.HEAD, BodyPart.TORSO, BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG};
    private static final BodyPart[] MAIN_PARTS_SLIM = new BodyPart[]{BodyPart.HEAD, BodyPart.TORSO, BodyPart.LEFT_ARM_SLIM, BodyPart.RIGHT_ARM_SLIM, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG};

    private final BodyPart[] OVERLAY_PARTS = new BodyPart[]{BodyPart.HAT, BodyPart.TORSO_OVERLAY, BodyPart.LEFT_ARM_OVERLAY, BodyPart.RIGHT_ARM_OVERLAY, BodyPart.LEFT_LEG_OVERLAY, BodyPart.RIGHT_LEG_OVERLAY};
    private final BodyPart[] OVERLAY_PARTS_SLIM = new BodyPart[]{BodyPart.HAT, BodyPart.TORSO_OVERLAY, BodyPart.LEFT_ARM_OVERLAY_SLIM, BodyPart.RIGHT_ARM_OVERLAY_SLIM, BodyPart.LEFT_LEG_OVERLAY, BodyPart.RIGHT_LEG_OVERLAY};

    private int renderOverlayFlags;

    /**
     * If true, the model is rendered with translucent support (like in 1.9+).
     * Otherwise, model is rendered like 1.8 and earlier.
     */
    @Setter private boolean translucentModel;

    private boolean normalModel;

    private float walkAngle;
    private float capeAngle;
    private float worldRotY;
    private float worldRotX;
    private float headPitch;
    private float worldScale;

    private boolean transformAngleDirty;

    private final EnumMap<BodyPart, Matrix4f> transforms = new EnumMap<>(BodyPart.class);

    private BodyPart[] overlayParts;
    private BodyPart[] overlayPartsSlim;

    private BodyPart[] renderPartsMain, renderPartsOverlay;

    private boolean capeEnabled;

    private final EnumMap<BodyPart, Mesh<?>> meshes = new EnumMap<>(BodyPart.class);

    /**
     * Creates a PlayerModel with default settings.
     */
    public PlayerModel(BufferedImage skinTexture, BufferedImage capeTexture) {
        translucentModel = true;
        normalModel = true;

        walkAngle = 0.0f;
        capeAngle = 0.0f;
        worldRotY = 0.0f;
        worldRotX = 0.0f;
        headPitch = 0.0f;
        worldScale = 0.75f;

        Texture tex = new Texture(skinTexture);

        for (BodyPart part : BodyPart.values()) {
            if (part == BodyPart.CAPE) continue;

            meshes.put(part, part.toBuilder(tex, 0)
                    .attach(Mesh.AttachmentType.LIGHT_FACTOR, 1f)
                    .attach(Mesh.AttachmentType.LIGHT_VECTOR, new Vector4f(0, 0, 1, 0)).build());
        }

        if (capeTexture == null) {
            capeEnabled = false;
        } else {
            capeEnabled = true;
            meshes.put(BodyPart.CAPE, BodyPart.CAPE.toBuilder(new Texture(capeTexture), 0)
                    .attach(Mesh.AttachmentType.LIGHT_FACTOR, 1f)
                    .attach(Mesh.AttachmentType.LIGHT_VECTOR, new Vector4f(0, 0, 1, 0)).build());
        }

        setRenderOverlayFlags(OVERLAY_ALL);
    }

    private void updateRenderParts() {
        if (normalModel) {
            renderPartsMain = MAIN_PARTS;
            renderPartsOverlay = overlayParts;
        } else {
            renderPartsMain = MAIN_PARTS_SLIM;
            renderPartsOverlay = overlayPartsSlim;
        }
    }

    /**
     * Sets whether the normal or slim (Alex) model is used.
     * @param normal true if the normal model should be used, false otherwise
     */
    public void setNormalModel(boolean normal) {
        this.normalModel = normal;
        updateRenderParts();
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

        updateRenderParts();
    }
    
    /**
     * Set which parts of the player's overlay are rendered.
     * @param flags Bitmask of flags for which overlay parts to render.
     * <p>Should be a bitwise OR of 0 or more of the following flags (or {@link PlayerModel#OVERLAY_ALL}):
     * <ul>
     *     <li>{@link PlayerModel#OVERLAY_HAT}</li>
     *     <li>{@link PlayerModel#OVERLAY_TORSO}</li>
     *     <li>{@link PlayerModel#OVERLAY_LEFT_ARM}</li>
     *     <li>{@link PlayerModel#OVERLAY_RIGHT_ARM}</li>
     *     <li>{@link PlayerModel#OVERLAY_LEFT_LEG}</li>
     *     <li>{@link PlayerModel#OVERLAY_RIGHT_LEG}</li>
     * </ul>
     * </p>
     */
    public void setRenderOverlayFlags(int flags) {
        if (!capeEnabled) {
            flags &= ~OVERLAY_CAPE;
        }

        this.renderOverlayFlags = flags;

        updateOverlayParts();
    }

    /**
     * Sets the walking animation angle. This controls the player's arms and legs.
     * @param angle the angle in radians
     * @see PlayerModel#updateTransforms()
     */
    public void setWalkAngle(float angle) {
        this.walkAngle = angle;
        transformAngleDirty = true;
    }

    /**
     * Sets the cape angle.
     * @param angle the angle in radians
     * @see PlayerModel#updateTransforms()
     */
    public void setCapeAngle(float angle) {
        this.capeAngle = angle;
        transformAngleDirty = true;
    }

    /**
     * Sets the world rotation about the Y axis.
     * @param angle the angle in radians
     * @see PlayerModel#updateTransforms()
     */
    public void setWorldRotY(float angle) {
        this.worldRotY = angle;
        transformAngleDirty = true;
    }

    /**
     * Sets the world rotation about the X axis.
     * @param angle the angle in radians
     * @see PlayerModel#updateTransforms()
     */
    public void setWorldRotX(float angle) {
        this.worldRotX = angle;
        transformAngleDirty = true;
    }

    /**
     * Sets the player's head pitch.
     * @param angle the angle in radians
     * @see PlayerModel#updateTransforms()
     */
    public void setHeadPitch(float angle) {
        this.headPitch = angle;
        transformAngleDirty = true;
    }

    /**
     * Sets the world scale.
     * @param scale the world scale (default is 0.75)
     * @see PlayerModel#updateTransforms()
     */
    public void setWorldScale(float scale) {
        this.worldScale = scale;
        transformAngleDirty = true;
    }

    /**
     * Updates the transformation matrices that this model will be rendered with. This function should be called after
     * modifying the angles, world rotation, or world scale. It is also called automatically when the next frame is rendered.
     */
    public void updateTransforms() {
        if (!transformAngleDirty) return;

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

        Matrix4f worldTransform = Matrix4f.scale(worldScale).times(Matrix4f.rotateX(worldRotX)).times(Matrix4f.rotateY(worldRotY));
        for (BodyPart part : BodyPart.values()) {
            transforms.put(part, worldTransform.times(transforms.get(part), new Matrix4f()));
        }

        transformAngleDirty = false;
    }

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

    public void render(Framebuffer fb) {
        updateTransforms(); // no-op if the angles are not dirty

        fb.setBlendMode(BlendMode.DISABLE);
        fb.setCullBackFace(true);

        if ((renderOverlayFlags & OVERLAY_CAPE) != 0) {
            fb.setTransform(transforms.get(BodyPart.CAPE));
            fb.drawMesh(meshes.get(BodyPart.CAPE));
        }

        fb.setDepthMode(Framebuffer.FB_DEPTH_COMMIT | Framebuffer.FB_DEPTH_USE);

        for (BodyPart part : renderPartsMain) {
            fb.setTransform(transforms.get(part));
            fb.drawMesh(meshes.get(part));
        }

        if (translucentModel) {
            fb.setDepthMode(Framebuffer.FB_DEPTH_USE | Framebuffer.FB_DEPTH_COMMIT_TRANSPARENT);
            fb.setBlendMode(BlendMode.BLEND_OVER);
            fb.setCullBackFace(false);
        } else {
            fb.setBlendMode(BlendMode.BINARY);
        }

        if (renderPartsOverlay != null) {
            for (BodyPart part : renderPartsOverlay) {
                fb.setTransform(transforms.get(part));
                fb.drawMesh(meshes.get(part));
            }
        }
    }

    @RequiredArgsConstructor
    private enum BodyPart {
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
        @SuppressWarnings("unused")
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

        Mesh.Builder toBuilder(Texture tex, int base) {
            Mesh.Builder builder = new Mesh.Builder().texture(tex);
            addCuboid(builder, xMin, yMin, zMin, xMax, yMax, zMax, texBaseX, texBaseY, texSpanX, texSpanY, texSpanZ, tex.calcAspect(), base);
            return builder;
        }
    }
}
