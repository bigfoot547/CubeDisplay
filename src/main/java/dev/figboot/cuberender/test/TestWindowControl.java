package dev.figboot.cuberender.test;

import dev.figboot.cuberender.api.PlayerModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.EnumMap;
import java.util.Map;

class TestWindowControl extends JFrame {
    private final GraphicsPanel graphicsPanel;

    private final JRadioButton radOverlayOpaque, radOverlayTranslucent;
    private final JRadioButton radModelNormal, radModelSlim;

    private final ButtonGroup bgRenderOverlay;
    private final ButtonGroup bgModelType;

    private final JSlider sldWalkAngle;
    private final JSlider sldCapeAngle;
    private final JSlider sldYRot;
    private final JSlider sldXRot;
    private final JSlider sldHeadPitch;

    private final EnumMap<OverlayPart, JCheckBox> cbxOverlayParts = new EnumMap<>(OverlayPart.class);

    TestWindowControl(GraphicsPanel gpanel) {
        this.graphicsPanel = gpanel;

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Control");

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);

        setContentPane(panel);

        JPanel overlayRenderPanel = new JPanel();
        overlayRenderPanel.setLayout(new BoxLayout(overlayRenderPanel, BoxLayout.Y_AXIS));

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        panel.add(overlayRenderPanel, c);

        bgRenderOverlay = new ButtonGroup();
        radOverlayOpaque = new JRadioButton("Opaque overlay");
        radOverlayTranslucent = new JRadioButton("Translucent overlay");
        radOverlayTranslucent.setSelected(true);

        radOverlayOpaque.addActionListener(this::handleAction);
        radOverlayTranslucent.addActionListener(this::handleAction);

        overlayRenderPanel.setBorder(new LineBorder(Color.BLACK, 1));
        bgRenderOverlay.add(radOverlayOpaque);
        bgRenderOverlay.add(radOverlayTranslucent);

        overlayRenderPanel.add(radOverlayOpaque);
        overlayRenderPanel.add(radOverlayTranslucent);

        JPanel modelPanel = new JPanel();
        modelPanel.setLayout(new BoxLayout(modelPanel, BoxLayout.Y_AXIS));
        modelPanel.setBorder(new LineBorder(Color.BLACK, 1));
        panel.add(modelPanel, c);

        bgModelType = new ButtonGroup();
        radModelNormal = new JRadioButton("Normal");
        radModelSlim = new JRadioButton("Slim");
        radModelNormal.setSelected(true);

        radModelNormal.addActionListener(this::handleAction);
        radModelSlim.addActionListener(this::handleAction);

        modelPanel.add(radModelNormal);
        modelPanel.add(radModelSlim);

        bgModelType.add(radModelNormal);
        bgModelType.add(radModelSlim);

        JPanel limbControlPanel = new JPanel();
        limbControlPanel.setLayout(new BoxLayout(limbControlPanel, BoxLayout.Y_AXIS));
        limbControlPanel.setBorder(new LineBorder(Color.BLACK, 1));

        c.gridx = 0;
        c.gridy = 1;

        panel.add(limbControlPanel, c);

        sldWalkAngle = new JSlider(-180, 180, 0);
        sldCapeAngle = new JSlider(-180, 180, 0);
        sldYRot = new JSlider(-180, 180, 0);
        sldXRot = new JSlider(-180, 180, 0);
        sldHeadPitch = new JSlider(-180, 180, 0);

        limbControlPanel.add(new JLabel("Walk animation"));
        limbControlPanel.add(sldWalkAngle);

        limbControlPanel.add(new JLabel("Cape angle"));
        limbControlPanel.add(sldCapeAngle);

        limbControlPanel.add(new JLabel("Y rotation"));
        limbControlPanel.add(sldYRot);

        limbControlPanel.add(new JLabel("X rotation"));
        limbControlPanel.add(sldXRot);

        limbControlPanel.add(new JLabel("Head pitch"));
        limbControlPanel.add(sldHeadPitch);

        sldWalkAngle.addChangeListener(this::handleSlider);
        sldCapeAngle.addChangeListener(this::handleSlider);
        sldYRot.addChangeListener(this::handleSlider);
        sldXRot.addChangeListener(this::handleSlider);
        sldHeadPitch.addChangeListener(this::handleSlider);

        JPanel skinPartsPanel = new JPanel();
        skinPartsPanel.setLayout(new BoxLayout(skinPartsPanel, BoxLayout.Y_AXIS));
        skinPartsPanel.setBorder(new LineBorder(Color.BLACK, 1));

        c.gridx = GridBagConstraints.RELATIVE;
        panel.add(skinPartsPanel, c);

        for (OverlayPart part : OverlayPart.values()) {
            JCheckBox cbxPart = new JCheckBox(part.name());
            cbxPart.setSelected(true);
            cbxPart.addActionListener(this::handleOverlayPartsAction);

            skinPartsPanel.add(cbxPart);

            cbxOverlayParts.put(part, cbxPart);
        }

        pack();
        setResizable(false);
        setLocationRelativeTo(null);

        updateGraphics(true, true);
    }

    private void updateGraphics(boolean slider, boolean overlay) {
        PlayerModel model = graphicsPanel.getModel();

        model.setTranslucentModel(radOverlayTranslucent.isSelected());
        model.setNormalModel(radModelNormal.isSelected());

        if (slider) {
            model.setWalkAngle((float)Math.toRadians(sldWalkAngle.getValue()));
            model.setCapeAngle((float)Math.toRadians(sldCapeAngle.getValue()));
            model.setWorldRotY((float)Math.toRadians(sldYRot.getValue()));
            model.setWorldRotX((float)Math.toRadians(sldXRot.getValue()));
            model.setHeadPitch((float)Math.toRadians(sldHeadPitch.getValue()));
            model.updateTransforms();
        }

        if (overlay) {
            int flags = 0;
            for (Map.Entry<OverlayPart, JCheckBox> entry : cbxOverlayParts.entrySet()) {
                if (entry.getValue().isSelected()) {
                    flags |= entry.getKey().getFlag();
                }
            }

            model.setRenderOverlayFlags(flags);
        }
    }

    @SuppressWarnings("unused")
    private void handleAction(ActionEvent e) {
        updateGraphics(false, false);
        graphicsPanel.repaint();
    }

    @SuppressWarnings("unused")
    private void handleOverlayPartsAction(ActionEvent e) {
        updateGraphics(false, true);
        graphicsPanel.repaint();
    }

    @SuppressWarnings("unused")
    private void handleSlider(ChangeEvent e) {
        updateGraphics(true, false);
        graphicsPanel.repaint();
    }

    @RequiredArgsConstructor
    @Getter
    private enum OverlayPart {
        HAT("Hat", PlayerModel.OVERLAY_HAT),
        TORSO("Torso overlay", PlayerModel.OVERLAY_TORSO),
        LEFT_ARM("Left arm overlay", PlayerModel.OVERLAY_LEFT_ARM),
        RIGHT_ARM("Right arm overlay", PlayerModel.OVERLAY_RIGHT_ARM),
        LEFT_LEG("Left leg overlay", PlayerModel.OVERLAY_LEFT_LEG),
        RIGHT_LEG("Right leg overlay", PlayerModel.OVERLAY_RIGHT_LEG),
        CAPE("Cape", PlayerModel.OVERLAY_CAPE);

        private final String name;
        private final int flag;
    }
}
