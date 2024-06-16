package dev.figboot.cuberender.test;

import dev.figboot.cuberender.api.PlayerModel;
import dev.figboot.cuberender.api.SkinUtil;
import dev.figboot.cuberender.state.Framebuffer;
import lombok.Getter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

class GraphicsPanel extends JPanel {
    private Framebuffer framebuffer;

    private final long[] clrTime = new long[32];
    private final long[] meshTime = new long[32];
    private int tidx = 0;
    boolean rollOver = false;

    @Getter private final PlayerModel model;

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
            bi = SkinUtil.convertToModernSkin(bi, null);
        }

        model = new PlayerModel(bi, capeBI);
    }

    private void handleResize(int width, int height) {
        framebuffer = new Framebuffer(width, height);
    }

    @Override
    public void paintComponent(Graphics g) {
        if (framebuffer == null) handleResize(getWidth(), getHeight());

        long start = System.nanoTime();
        framebuffer.clear(Framebuffer.FB_CLEAR_COLOR | Framebuffer.FB_CLEAR_DEPTH, 0xFF000000);
        long t1 = System.nanoTime();
        model.render(framebuffer);
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
}
