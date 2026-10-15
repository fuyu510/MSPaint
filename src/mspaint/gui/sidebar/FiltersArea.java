package mspaint.gui.sidebar;

import java.awt.Insets;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import mspaint.action.home.ShapeAction;
import mspaint.gui.GUIHandler;
import mspaint.helper.Filters;
import mspaint.helper.TButton;
import mspaint.initial_info.InitialValues;
import com.gmail.mohitsainiknl2.util.gui.GridBagPanel;

public class FiltersArea extends JScrollPane {
    private GridBagPanel panel;
    private final GUIHandler gui;

    public FiltersArea(GUIHandler gui) {
        super();
        this.gui = gui;
        setOpaque(false);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        initialWork();
        setViewportView(panel);
    }
    
    private void initialWork() {
        panel = new GridBagPanel(new Insets(5, 5, 5, 5));
        panel.setOpaque(false);

        for(int i = 0; i < Filters.NAMES.length; i++) {
            final String name = Filters.NAMES[i];
            FilterButton button = new FilterButton(name);
            button.addActionListener(e -> applyFilter(name));
            panel.add(button, 0, i, 1, 1, 1.0, 0);
        }
        panel.add(new JLabel(), 0, Filters.NAMES.length, 1, 1, 1.0, 1.0);   // keeps the buttons at the top

    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        panel.updateUI();
    }

    /**
     * Filters the selection if there is one, otherwise the active layer.
     */
    private void applyFilter(String name) {
        gui.textAction.commit();
        ShapeAction.finishPending();
        gui.history.begin();
        if(gui.isSelectionActive && gui.viewAction.sImage != null) {
            gui.viewAction.setFloatingImage(Filters.apply(name, gui.viewAction.sImage));
        }
        else {
            BufferedImage layer = gui.activeImage();
            BufferedImage result = Filters.apply(name, layer);
            final int w = layer.getWidth();
            final int h = layer.getHeight();
            layer.setRGB(0, 0, w, h, result.getRGB(0, 0, w, h, null, 0, w), 0, w);
        }
        gui.history.end();
        gui.view.repaint();
    }


    public class FilterButton extends TButton {

        public FilterButton() {
            this("", null);
        }
    
        public FilterButton(String text) {
            this(text, null);
        }
    
        public FilterButton(ImageIcon icon) {
            this("", icon);
        }
    
        public FilterButton(String text, ImageIcon icon) {
            super(text, icon, TButton.Type.NOT_SELECTABLE);
            setOpaque(true);
            setBorderPainted(true);
            setHorizontalAlignment(JButton.CENTER);
            resetSize();
        }
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            resetSize();
        }
        
        public void resetSize() {
            final int width = InitialValues.sidePanelWith - 30;
            final int height = 34;
            setPreferredSize(new Dimension(width, height));
        }
    }
    
}
