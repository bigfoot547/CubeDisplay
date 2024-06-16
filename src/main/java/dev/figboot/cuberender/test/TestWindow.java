package dev.figboot.cuberender.test;

import javax.swing.*;

public class TestWindow extends JFrame {
    public TestWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Graphics test");
        setSize(300, 300);
        setLocationRelativeTo(null);

        GraphicsPanel gp = new GraphicsPanel();

        setContentPane(gp);

        TestWindowControl control = new TestWindowControl(gp);
        control.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TestWindow().setVisible(true));
    }
}
