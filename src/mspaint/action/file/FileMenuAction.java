package mspaint.action.file;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import mspaint.gui.GUIHandler;
import mspaint.gui.menubar.FilePopupMenu;
import mspaint.helper.ImageFilter;
import mspaint.helper.RecentFiles;
import mspaint.helper.TButton;
import mspaint.initial_info.InitialValues;

/**
 * FileMenuAction
 */
public class FileMenuAction implements ActionListener {
    private GUIHandler gui;
    private FilePopupMenu menu;

    public FileMenuAction(GUIHandler gui, FilePopupMenu menu) {
        this.gui = gui;
        this.menu = menu;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        TButton button = (TButton)e.getSource();

        if(menu.isShowing()) {
            menu.setVisible(false);
        }
        gui.textAction.commit();

        if(button.getText().equals("New")) {
            newFile();
        }
        else if(button.getText().equals("Open")) {
            open();
        }
        else if(button.getText().equals("Save")) {
            save();
        }
        else if(button.getText().equals("Save as")) {
            saveAs(null);
        }
        else if(button.getText().equals("Print")) {
            print();
        }
        else if(button.getText().equals("Set as Desktop Background")) {
            setAsDesktopBackground();
        }
        else if(button.getText().equals("About")) {
            JOptionPane.showMessageDialog(gui.frame,
                "MSPaint\nA Microsoft Paint clone written in Java Swing.\n\n" +
                "Original author: Mohit Saini (github.com/mohitsainiknl/MSPaint)\n" +
                "Released under the MIT License.",
                "About MSPaint", JOptionPane.INFORMATION_MESSAGE);
        }
        else if(button.getText().equals("Exit")) {
            System.exit(0);
        }

        else if(button.getName() != null) {
            switch(button.getName()) {
                case "png":
                case "jpeg":
                case "bmp":
                case "gif":
                    saveAs(button.getName());
                    break;
                case "other":
                    saveAs(null);
                    break;
            }
        }
    }

    public void newFile() {
        gui.textAction.commit();
        BufferedImage blank = new BufferedImage(InitialValues.newImageWidth, InitialValues.newImageHeight, BufferedImage.TYPE_INT_ARGB);
        gui.newDocument(blank, null);
    }

    public void open() {
        gui.textAction.commit();
        JFileChooser chooser = new JFileChooser(gui.currentFile);
        chooser.addChoosableFileFilter(new ImageFilter());
        chooser.setAcceptAllFileFilterUsed(false);
        if(chooser.showOpenDialog(gui.frame) == JFileChooser.APPROVE_OPTION) {
            open(chooser.getSelectedFile());
        }
    }

    public void save() {
        gui.textAction.commit();
        if(gui.currentFile != null) {
            write(gui.currentFile);
        }
        else {
            saveAs(null);
        }
    }

    public void open(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            if(image == null) {
                throw new IOException("unsupported image format");
            }
            gui.newDocument(image, file);
            RecentFiles.add(file);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(gui.frame, "Could not open " + file.getName() + ":\n" + e.getMessage(), "Open", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Asks for a file name and saves; the format comes from the extension, defaulting to the given one.
     */
    public void saveAs(String extension) {
        gui.textAction.commit();
        JFileChooser chooser = new JFileChooser(gui.currentFile);
        chooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter png = new FileNameExtensionFilter("PNG picture (*.png)", "png");
        FileNameExtensionFilter jpeg = new FileNameExtensionFilter("JPEG picture (*.jpg; *.jpeg)", "jpg", "jpeg");
        FileNameExtensionFilter bmp = new FileNameExtensionFilter("BMP picture (*.bmp)", "bmp");
        FileNameExtensionFilter gif = new FileNameExtensionFilter("GIF picture (*.gif)", "gif");
        chooser.addChoosableFileFilter(png);
        chooser.addChoosableFileFilter(jpeg);
        chooser.addChoosableFileFilter(bmp);
        chooser.addChoosableFileFilter(gif);

        if(extension == null) {
            extension = (gui.currentFile != null) ? getExtension(gui.currentFile) : "png";
        }
        switch(extension == null ? "png" : extension) {
            case "jpg":
            case "jpeg": chooser.setFileFilter(jpeg); extension = "jpeg"; break;
            case "bmp": chooser.setFileFilter(bmp); break;
            case "gif": chooser.setFileFilter(gif); break;
            default: chooser.setFileFilter(png); extension = "png";
        }
        String baseName = (gui.currentFile != null) ? gui.currentFile.getName().replaceFirst("\\.[^.]*$", "") : "Untitled";
        chooser.setSelectedFile(new File(baseName + "." + extension));

        if(chooser.showSaveDialog(gui.frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if(getExtension(file) == null) {
            final String chosen = ((FileNameExtensionFilter) chooser.getFileFilter()).getExtensions()[0];
            file = new File(file.getPath() + "." + chosen);
        }
        if(file.exists()) {
            final int answer = JOptionPane.showConfirmDialog(gui.frame, file.getName() + " already exists.\nDo you want to replace it?", "Confirm Save As", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if(answer != JOptionPane.YES_OPTION) {
                return;
            }
        }
        write(file);
    }

    private void write(File file) {
        String format = getExtension(file);
        if(format == null) {
            format = "png";
        }
        if(format.equals("jpg")) {
            format = "jpeg";
        }
        BufferedImage image = gui.mergedImage();
        if(format.equals("jpeg") || format.equals("bmp")) {
            image = flatten(image);   // these formats have no transparency
        }
        try {
            if(!ImageIO.write(image, format, file)) {
                throw new IOException("saving as " + format.toUpperCase(Locale.ROOT) + " is not supported");
            }
            gui.currentFile = file;
            RecentFiles.add(file);
            gui.updateImageInfo();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(gui.frame, "Could not save " + file.getName() + ":\n" + e.getMessage(), "Save", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static BufferedImage flatten(BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    private static String getExtension(File file) {
        return new ImageFilter().getExtension(file);
    }

    public void print() {
        final BufferedImage image = gui.mergedImage();
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName((gui.currentFile != null) ? gui.currentFile.getName() : "MSPaint");
        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics graphics, PageFormat format, int pageIndex) {
                if(pageIndex > 0) {
                    return NO_SUCH_PAGE;
                }
                // fit the picture on the page, never enlarging it
                final double scale = Math.min(1.0, Math.min(format.getImageableWidth() / image.getWidth(), format.getImageableHeight() / image.getHeight()));
                Graphics2D g = (Graphics2D) graphics;
                g.translate(format.getImageableX(), format.getImageableY());
                g.drawImage(image, 0, 0, (int) (image.getWidth() * scale), (int) (image.getHeight() * scale), null);
                return PAGE_EXISTS;
            }
        });
        if(job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(gui.frame, "Could not print:\n" + e.getMessage(), "Print", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Saves the picture under ~/.local/share/mspaint and makes it the GNOME desktop background.
     */
    private void setAsDesktopBackground() {
        try {
            File dir = new File(System.getProperty("user.home"), ".local/share/mspaint");
            if(!dir.isDirectory() && !dir.mkdirs()) {
                throw new IOException("cannot create " + dir);
            }
            File[] old = dir.listFiles((d, name) -> name.startsWith("wallpaper-") && name.endsWith(".png"));
            if(old != null) {
                for(File f : old) {
                    f.delete();
                }
            }
            // a new name each time, otherwise GNOME keeps showing its cached copy
            File file = new File(dir, "wallpaper-" + System.currentTimeMillis() + ".png");
            ImageIO.write(gui.mergedImage(), "png", file);
            final String uri = file.toURI().toString();
            runGsettings("picture-uri", uri);
            runGsettings("picture-uri-dark", uri);
            JOptionPane.showMessageDialog(gui.frame, "The picture is now the desktop background.", "Set as Desktop Background", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | InterruptedException e) {
            JOptionPane.showMessageDialog(gui.frame, "Could not set the desktop background:\n" + e.getMessage() +
                "\n(This works on GNOME desktops, using the gsettings command.)", "Set as Desktop Background", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void runGsettings(String key, String value) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("gsettings", "set", "org.gnome.desktop.background", key, value)
            .redirectErrorStream(true).start();
        final String output = new String(process.getInputStream().readAllBytes());
        if(process.waitFor() != 0) {
            throw new IOException("gsettings failed: " + output.trim());
        }
    }
}
