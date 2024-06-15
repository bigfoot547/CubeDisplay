package dev.figboot.cuberender.test;

import javax.swing.*;
import java.awt.*;

public class TestWindow extends JFrame {
    public TestWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Graphics test");
        setSize(300, 300);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        JSlider sliderY = new JSlider();
        JSlider sliderX = new JSlider();
        GraphicsPanel gp = new GraphicsPanel();

        sliderY.setMinimum(-180);
        sliderY.setMaximum(180);

        sliderX.setMinimum(-180);
        sliderX.setMaximum(180);
        sliderX.setOrientation(JSlider.VERTICAL);

        sliderX.setValue(0);
        sliderY.setValue(0);

        panel.setLayout(new BorderLayout());
        panel.add(gp, BorderLayout.CENTER);
        panel.add(sliderY, BorderLayout.SOUTH);
        panel.add(sliderX, BorderLayout.EAST);

        setContentPane(panel);

        sliderY.addChangeListener(e -> {
            gp.setYRot((float)Math.toRadians(sliderY.getValue()));
            gp.repaint();
        });

        sliderX.addChangeListener(e -> {
            gp.setXRot((float)Math.toRadians(sliderX.getValue()));
            gp.repaint();
        });
    }

    public static void main(String[] args) {
        new TestWindow().setVisible(true);
    }
}
