package mspaint.action.home;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import mspaint.gui.GUIHandler;
import mspaint.gui.toolbar.text.FontTool;
import mspaint.helper.Point;

/**
 * TextAction: the Text tool. A click on the canvas opens an editable text box there;
 * the text is drawn onto the active layer when the box is committed.
 */
public class TextAction {
    private static final int MIN_WIDTH = 40;

    private final GUIHandler gui;
    private JTextArea textArea;
    private Point location;   // top left of the text, in image pixels

    public TextAction(GUIHandler gui) {
        this.gui = gui;
    }

    public boolean isEditing() {
        return textArea != null;
    }

    public void start(int x, int y) {
        commit();
        location = new Point(x, y);

        textArea = new JTextArea() {
            @Override
            protected void paintComponent(Graphics g) {
                setForeground(gui.activeColor1);   // follow the palette while typing
                setCaretColor(gui.activeColor1);
                super.paintComponent(g);
                g.setColor(Color.GRAY);
                ((Graphics2D) g).setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[] {3, 3}, 0));
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        textArea.setBorder(new EmptyBorder(0, 0, 0, 0));
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { resize(); }
            public void removeUpdate(DocumentEvent e) { resize(); }
            public void changedUpdate(DocumentEvent e) { resize(); }
        });
        textArea.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancelText");
        textArea.getActionMap().put("cancelText", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        updateStyle();
        gui.textToolBar.clipboardTools.cutTool.setTEnabled(true);
        gui.textToolBar.clipboardTools.copyTool.setTEnabled(true);
        gui.view.add(textArea);
        gui.showTextToolBar();
        textArea.requestFocusInWindow();
    }

    /**
     * Re-reads the font and background settings of the Text tool bar.
     */
    public void updateStyle() {
        if(textArea == null) {
            return;
        }
        textArea.setFont(getFont(gui.view.scale));
        final boolean opaque = gui.textToolBar.backgroundTools.isOpaqueBackground();
        textArea.setOpaque(opaque);
        textArea.setBackground(opaque ? gui.activeColor2 : new Color(0, 0, 0, 0));
        resize();
        gui.view.repaint();
    }

    /**
     * Cut, Copy or Paste of the clipboard buttons, applied to the text being typed.
     */
    public void clipboard(String name) {
        if(textArea == null) {
            return;
        }
        if(name.equals("Cut")) {
            textArea.cut();
        }
        else if(name.equals("Copy")) {
            textArea.copy();
        }
        else if(name.equals("Paste")) {
            textArea.paste();
        }
        textArea.requestFocusInWindow();
    }

    public void commit() {
        if(textArea == null) {
            return;
        }
        final String text = textArea.getText();
        if(!text.isEmpty()) {
            gui.history.begin();
            draw(text);
            gui.history.end();
        }
        close();
    }

    public void cancel() {
        if(textArea != null) {
            close();
        }
    }

    private void close() {
        gui.view.remove(textArea);
        textArea = null;
        gui.textToolBar.clipboardTools.cutTool.setTEnabled(false);
        gui.textToolBar.clipboardTools.copyTool.setTEnabled(false);
        gui.showHomeToolBar();
        gui.view.repaint();
        gui.view.requestFocusInWindow();
    }

    private void draw(String text) {
        final Font font = getFont(1.0);
        Graphics2D g = gui.graphics;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        final FontMetrics metrics = g.getFontMetrics();
        final String[] lines = text.split("\n", -1);

        if(gui.textToolBar.backgroundTools.isOpaqueBackground()) {
            int width = 0;
            for(String line : lines) {
                width = Math.max(width, metrics.stringWidth(line));
            }
            g.setColor(gui.activeColor2);
            g.fillRect(location.x, location.y, width, metrics.getHeight() * lines.length);
        }
        g.setColor(gui.activeColor1);
        for(int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], location.x, location.y + metrics.getAscent() + i * metrics.getHeight());
        }
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
    }

    private Font getFont(double scale) {
        FontTool fontTool = gui.textToolBar.fontTool;
        int style = Font.PLAIN;
        if(fontTool.isBold()) {
            style |= Font.BOLD;
        }
        if(fontTool.isItalic()) {
            style |= Font.ITALIC;
        }
        Font font = new Font(fontTool.getFontName(), style, 1).deriveFont((float) (fontTool.getFontSize() * scale));

        Map<TextAttribute, Object> attributes = new HashMap<>();
        if(fontTool.isUnderline()) {
            attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        }
        if(fontTool.isStrike()) {
            attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        }
        return attributes.isEmpty() ? font : font.deriveFont(attributes);
    }

    private void resize() {
        if(textArea == null) {
            return;
        }
        final double scale = gui.view.scale;
        final java.awt.Dimension size = textArea.getPreferredSize();
        final FontMetrics metrics = textArea.getFontMetrics(textArea.getFont());
        // leave room for the caret and the next character
        final int width = Math.max(size.width + metrics.charWidth('W'), (int) (MIN_WIDTH * scale));
        textArea.setBounds((int) (location.x * scale), (int) (location.y * scale), width, size.height);
        gui.view.repaint();
    }
}
