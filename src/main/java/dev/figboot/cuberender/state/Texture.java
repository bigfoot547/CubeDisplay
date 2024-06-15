package dev.figboot.cuberender.state;

import java.awt.image.BufferedImage;

public class Texture {
    public final BufferedImage image;
    public transient int width, height;

    public Texture(BufferedImage image) {
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
    }
}
