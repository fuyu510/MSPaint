package mspaint.gui.sidebar;

import java.awt.Dimension;
import javax.swing.JLabel;

import mspaint.helper.TSeparator;
import mspaint.style.Style;
import mspaint.theme.Theme;
import com.gmail.mohitsainiknl2.util.gui.GridBagPanel;

public class FilterSettings extends GridBagPanel {
    private Theme theme;
    
    public FilterSettings(Style style, Theme theme) {
        super();
        this.theme = theme;
        initialWork();
    }

    private void initialWork() {
        setOpaque(false);


        JLabel info = new JLabel("<html><center>Click a filter to apply it to the selection, or to the whole layer.</center></html>", JLabel.CENTER);
        info.setPreferredSize(new Dimension(100, 60));
        add(info, 0, 0, 1, 1, 1.0, 1.0);
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
