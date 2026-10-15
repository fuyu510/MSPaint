package mspaint.gui.sidebar;

import java.awt.BorderLayout;
import javax.swing.JPanel;

import mspaint.gui.GUIHandler;
import mspaint.helper.TSeparator;
import mspaint.style.Style;
import mspaint.theme.Theme;

public class FilterSideBar extends JPanel {
    private final Style style;
    private final Theme theme;
    private final GUIHandler gui;


    public FilterSideBar(GUIHandler gui) {
        super(new BorderLayout());
        this.gui = gui;
        this.style = gui.style;
        this.theme = gui.theme;
        initialWork();
    }

    private void initialWork() {
        setOpaque(false);

        add(new FilterSettings(style, theme), BorderLayout.NORTH);
        add(new FiltersArea(gui), BorderLayout.CENTER);

    }

    /**
     * Appends a separator of a specified size to the end
     * of the tool bar.
     *
     * @param size the <code>Dimension</code> of the separator
     */
    public void addSeparator(int gridx, int gridy) {
        TSeparator s = new TSeparator(9, 92, 0, theme.getBorderColor(), TSeparator.VERTICAL);
        add(s, gridx, gridy);
    }
}
