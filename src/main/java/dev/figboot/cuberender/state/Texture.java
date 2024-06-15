package dev.figboot.cuberender.state;

import java.awt.image.BufferedImage;

public class Texture {
    public final BufferedImage image;
    public final transient int width;
    public final transient int height;

    public Texture(BufferedImage image) {
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
    }

    public float calcAspect() {
        return (float)width / height;
    }
}
