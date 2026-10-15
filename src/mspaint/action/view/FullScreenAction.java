package mspaint.action.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;

import java.awt.event.KeyEvent;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import mspaint.gui.GUIHandler;

public class FullScreenAction implements ActionListener {
    private GUIHandler gui;
    private JDialog dialog;

    public FullScreenAction(GUIHandler gui) {
        this.gui = gui;
        // a click on the gray area around the picture, or Esc, leaves full screen
        gui.viewport.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                close();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(dialog != null) {
            return;
        }
        dialog = new JDialog(gui.frame);
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setSize(size);
        dialog.setUndecorated(true);
        dialog.setAlwaysOnTop(true);
        dialog.setLayout(new BorderLayout());
        dialog.add(gui.viewport, BorderLayout.CENTER);
        dialog.getRootPane().registerKeyboardAction(ev -> close(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setVisible(true);
    }

    private void close() {
        if(dialog == null) {
            return;
        }
        dialog.remove(gui.viewport);
        dialog.dispose();
        dialog = null;
        gui.frame.getContentPane().add(gui.viewport, BorderLayout.CENTER);
        gui.frame.getContentPane().revalidate();
        gui.frame.repaint();
    }
}
