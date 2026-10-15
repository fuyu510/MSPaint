package mspaint.action.home;

import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import mspaint.gui.GUIHandler;
import mspaint.helper.Layer;
import mspaint.helper.TButton;

/**
 * ImageAction: Crop, Resize, Rotate and Flip of the Image tool group.
 * Rotate and Flip work on the selection when there is one, otherwise on the whole picture.
 */
public class ImageAction implements ActionListener {
    private static final int MAX_SIZE = 10000;
    private final GUIHandler gui;

    public ImageAction(GUIHandler gui) {
        this.gui = gui;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final String name = ((TButton) e.getSource()).getText();
        gui.textAction.commit();
        ShapeAction.finishPending();
        gui.history.begin();

        switch(name) {
            case "Crop":
                crop();
                break;
            case "Resize":
                resize();
                break;
            case "Rotate Right 90 dgree":
                transform(image -> rotate(image, 90));
                break;
            case "Rotate Left 90 dgree":
                transform(image -> rotate(image, -90));
                break;
            case "Rotate 180 dgree":
                transform(image -> rotate(image, 180));
                break;
            case "Flip Vertical":
                transform(image -> flip(image, false));
                break;
            case "Flip Horizonal":
                transform(image -> flip(image, true));
                break;
        }
        gui.history.end();
        gui.view.repaint();
    }

    private void crop() {
        Rectangle area = gui.viewAction.getSelectionBounds();
        if(area == null) {
            return;
        }
        gui.viewAction.drawImageSelected();
        area = area.intersection(new Rectangle(0, 0, gui.mainImage.getWidth(), gui.mainImage.getHeight()));
        if(area.isEmpty()) {
            return;
        }
        final Rectangle r = area;
        applyToLayers(image -> copy(image.getSubimage(r.x, r.y, r.width, r.height)));
    }

    private void resize() {
        gui.viewAction.drawImageSelected();
        final int width = gui.mainImage.getWidth();
        final int height = gui.mainImage.getHeight();

        JRadioButton byPercent = new JRadioButton("Percentage", true);
        JRadioButton byPixels = new JRadioButton("Pixels");
        ButtonGroup group = new ButtonGroup();
        group.add(byPercent);
        group.add(byPixels);
        JSpinner horizontal = new JSpinner(new SpinnerNumberModel(100, 1, MAX_SIZE, 1));
        JSpinner vertical = new JSpinner(new SpinnerNumberModel(100, 1, MAX_SIZE, 1));
        JCheckBox keepRatio = new JCheckBox("Maintain aspect ratio", true);

        final boolean[] updating = {false};
        horizontal.addChangeListener(ev -> {
            if(keepRatio.isSelected() && !updating[0]) {
                updating[0] = true;
                final int h = (Integer) horizontal.getValue();
                vertical.setValue(byPercent.isSelected() ? h : Math.max(1, (int) Math.round(h * (double) height / width)));
                updating[0] = false;
            }
        });
        vertical.addChangeListener(ev -> {
            if(keepRatio.isSelected() && !updating[0]) {
                updating[0] = true;
                final int v = (Integer) vertical.getValue();
                horizontal.setValue(byPercent.isSelected() ? v : Math.max(1, (int) Math.round(v * (double) width / height)));
                updating[0] = false;
            }
        });
        byPercent.addActionListener(ev -> { updating[0] = true; horizontal.setValue(100); vertical.setValue(100); updating[0] = false; });
        byPixels.addActionListener(ev -> { updating[0] = true; horizontal.setValue(width); vertical.setValue(height); updating[0] = false; });

        JPanel panel = new JPanel(new GridLayout(0, 2, 8, 6));
        panel.add(new JLabel("By:"));
        JPanel by = new JPanel(new GridLayout(1, 2));
        by.add(byPercent);
        by.add(byPixels);
        panel.add(by);
        panel.add(new JLabel("Horizontal:"));
        panel.add(horizontal);
        panel.add(new JLabel("Vertical:"));
        panel.add(vertical);
        panel.add(keepRatio);

        final int answer = JOptionPane.showConfirmDialog(gui.frame, panel, "Resize", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(answer != JOptionPane.OK_OPTION) {
            return;
        }
        final int h = (Integer) horizontal.getValue();
        final int v = (Integer) vertical.getValue();
        final int newWidth = byPercent.isSelected() ? Math.max(1, width * h / 100) : h;
        final int newHeight = byPercent.isSelected() ? Math.max(1, height * v / 100) : v;
        if(newWidth > MAX_SIZE || newHeight > MAX_SIZE) {
            JOptionPane.showMessageDialog(gui.frame, "The picture can be at most " + MAX_SIZE + " pixels wide and high.", "Resize", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(newWidth != width || newHeight != height) {
            applyToLayers(image -> scale(image, newWidth, newHeight));
        }
    }

    /**
     * Rotates/flips the selection if there is one, otherwise every layer.
     */
    private void transform(UnaryOperator<BufferedImage> operation) {
        if(gui.isSelectionActive && gui.viewAction.sImage != null) {
            gui.viewAction.setFloatingImage(operation.apply(gui.viewAction.sImage));
        }
        else {
            applyToLayers(operation);
        }
    }

    private void applyToLayers(UnaryOperator<BufferedImage> operation) {
        List<BufferedImage> images = new ArrayList<>();
        for(Layer layer : gui.layers) {
            images.add(operation.apply(layer.image));
        }
        gui.setLayerImages(images);
    }

    private static BufferedImage copy(BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    private static BufferedImage scale(BufferedImage image, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return result;
    }

    private static BufferedImage rotate(BufferedImage image, int degree) {
        final int w = image.getWidth();
        final int h = image.getHeight();
        final boolean swap = (degree % 180) != 0;
        BufferedImage result = new BufferedImage(swap ? h : w, swap ? w : h, BufferedImage.TYPE_INT_ARGB);
        AffineTransform t = new AffineTransform();
        if(degree == 90) {
            t.translate(h, 0);
        }
        else if(degree == -90) {
            t.translate(0, w);
        }
        else {
            t.translate(w, h);
        }
        t.rotate(Math.toRadians(degree));
        Graphics2D g = result.createGraphics();
        g.drawImage(image, t, null);
        g.dispose();
        return result;
    }

    private static BufferedImage flip(BufferedImage image, boolean horizontal) {
        final int w = image.getWidth();
        final int h = image.getHeight();
        AffineTransform t = horizontal ? new AffineTransform(-1, 0, 0, 1, w, 0) : new AffineTransform(1, 0, 0, -1, 0, h);
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, t, null);
        g.dispose();
        return result;
    }
}
