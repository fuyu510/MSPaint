package mspaint.action;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import mspaint.action.home.ShapeAction;
import mspaint.gui.GUIHandler;
import mspaint.helper.Layer;
import mspaint.helper.TButton;
import mspaint.helper.ImageFilter;

/**
 * ClipboardAction: Cut, Copy, Paste and Import Paste.
 * While a text box is open they work on its text, otherwise on the selection.
 * Images also go through the system clipboard, so they can be pasted to and from other programs.
 */
public class ClipboardAction implements ActionListener {
    private GUIHandler gui;

    public ClipboardAction(GUIHandler gui) {
        this.gui = gui;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        TButton button = (TButton)e.getSource();
        final String name = button.getText();

        if(gui.textAction.isEditing() && !name.equals("Import Paste")) {
            gui.textAction.clipboard(name);
            return;
        }
        gui.history.begin();

        if(name.equals("Cut")) {
            if(gui.isSelectionActive && gui.viewAction.sImage != null) {
                copyToClipboard(gui.viewAction.sImage);
                gui.viewAction.cancelSelection();
                gui.view.repaint();
            }
        }
        else if(name.equals("Copy")) {
            if(gui.isSelectionActive && gui.viewAction.sImage != null) {
                copyToClipboard(gui.viewAction.sImage);
            }
        }
        else if(name.equals("Paste")) {
            BufferedImage image = readClipboard();
            if(image != null) {
                paste(image);
            }
        }
        else if(name.equals("Import Paste")) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.addChoosableFileFilter(new ImageFilter());
            fileChooser.setAcceptAllFileFilterUsed(false);

            int value = fileChooser.showOpenDialog(gui.frame);
            if(value == JFileChooser.APPROVE_OPTION){
                File file = fileChooser.getSelectedFile();
                try {
                    BufferedImage image = ImageIO.read(file);
                    if(image == null) {
                        throw new IOException("unsupported image format");
                    }
                    paste(image);
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(gui.frame, "Could not read " + file.getName() + ":\n" + e1.getMessage(), "Import Paste", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        gui.history.end();
    }

    /**
     * Pastes at the top left corner as a selection, with the selection tool active so it can be moved.
     */
    private void paste(BufferedImage image) {
        ShapeAction.finishPending();
        gui.viewAction.drawImageSelected();
        growCanvas(image.getWidth(), image.getHeight());
        gui.homeToolBar.imageTools.rectangularSecItem.doClick();
        gui.viewAction.pasteImage(image, 0, 0);
    }

    /**
     * Enlarges the canvas (never shrinks it) so an image of the given size fits; new area is transparent.
     */
    private void growCanvas(int width, int height) {
        final int newWidth = Math.max(width, gui.mainImage.getWidth());
        final int newHeight = Math.max(height, gui.mainImage.getHeight());
        if(newWidth == gui.mainImage.getWidth() && newHeight == gui.mainImage.getHeight()) {
            return;
        }
        List<BufferedImage> images = new ArrayList<>();
        for(Layer layer : gui.layers) {
            BufferedImage grown = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = grown.createGraphics();
            g.drawImage(layer.image, 0, 0, null);
            g.dispose();
            images.add(grown);
        }
        gui.setLayerImages(images);
    }

    private void copyToClipboard(BufferedImage image) {
        gui.cpyImage = clone(image);
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageTransferable(gui.cpyImage), null);
        } catch (IllegalStateException e) {
            // system clipboard busy: the image is still kept inside the program
        }
    }

    /**
     * The image on the system clipboard, or the last image copied here if the clipboard has none.
     */
    private BufferedImage readClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if(clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                Image image = (Image) clipboard.getData(DataFlavor.imageFlavor);
                if(image != null) {
                    return toBufferedImage(image);
                }
            }
        } catch (IllegalStateException | UnsupportedFlavorException | IOException e) {
            // fall back to the image copied inside the program
        }
        return gui.cpyImage;
    }

    private static BufferedImage toBufferedImage(Image image) {
        if(image instanceof BufferedImage) {
            return clone((BufferedImage) image);
        }
        BufferedImage result = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    public static BufferedImage clone(BufferedImage image) {
        if(image != null) {
            final int w = image.getWidth();
            final int h = image.getHeight();
            BufferedImage newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = newImage.createGraphics();
            g.drawImage(image, 0, 0, w, h, null, null);
            g.dispose();
            return newImage;
        }
        return null;
    }

    private static class ImageTransferable implements Transferable {
        private final BufferedImage image;

        ImageTransferable(BufferedImage image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] {DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if(!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}
