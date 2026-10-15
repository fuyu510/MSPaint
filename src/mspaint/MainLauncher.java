package mspaint;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import mspaint.gui.GUIHandler;
import mspaint.style.DefaultStyle;
import mspaint.style.Style;
import mspaint.theme.DefaultTheme;
import mspaint.theme.Theme;

/**
 * MainLauncher
 */
public class MainLauncher {
    GUIHandler gui;
    Style style;
    Theme theme;

    private MainLauncher() {
        style = new DefaultStyle();
        theme = new DefaultTheme();
        gui = new GUIHandler(style, theme);
        gui.handle();
    }

    public static void main(String[] args) {
        try {
            // The GTK look and feel takes its text colors from the desktop theme, which gives
            // white text on this app's light panels under a dark theme, so use Metal instead.
            String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
            if(lookAndFeel.contains("GTK")) {
                lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
            }
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
        }
        new MainLauncher();
    }
}