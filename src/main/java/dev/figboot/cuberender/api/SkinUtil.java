package dev.figboot.cuberender.api;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class SkinUtil {
    private SkinUtil() { }

    private static void copyLimbFlipped(BufferedImage src, BufferedImage target) {
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

    /**
     * This function converts a 64x32 skin (1.8 and earlier) into a modern (64x64) skin texture.
     * @param image the old skin
     * @param target the new skin, or {@code null} if a new image should be created
     * @return the new skin
     */
    public static BufferedImage convertToModernSkin(BufferedImage image, BufferedImage target) {
        if (image.getWidth() != 64 || image.getHeight() != 32) {
            throw new IllegalArgumentException("source image must be a 64x32 skin texture");
        }

        if (target == null) {
            target = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        } else if (target.getWidth() != 64 || target.getHeight() != 64) {
            throw new IllegalArgumentException("target image must be 64x64");
        }

        Graphics2D g2d = target.createGraphics();

        g2d.drawImage(image, 0, 0, 64, 32, null);

        copyLimbFlipped(image.getSubimage(0, 16, 16, 16), target.getSubimage(16, 48, 16, 16));
        copyLimbFlipped(image.getSubimage(40, 16, 16, 16), target.getSubimage(32, 48, 16, 16));
        g2d.dispose();

        return target;
    }

    /**
     * This function converts an OptiFine cape texture into a regular cape texture.
     * @param image the OptiFine cape texture
     * @param target the image to place the cape texture into, or {@code null} if a new image should be created
     * @return the regular cape texture
     */
    public static BufferedImage convertOFToRegular(BufferedImage image, BufferedImage target) {
        if (image.getWidth() != 46 || image.getHeight() != 22) {
            throw new IllegalArgumentException("image must be a 46x22 OptiFine cape texture");
        }

        if (target == null) {
            target = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        } else if (target.getWidth() != 64 || target.getHeight() != 32) {
            throw new IllegalArgumentException("target image must be 64x32");
        }

        Graphics2D g2d = target.createGraphics();
        g2d.drawImage(image, 0, 0, 46, 22, null);
        g2d.dispose();

        return target;
    }
}
