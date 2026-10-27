package mspaint.helper;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Deque;

import mspaint.action.home.ShapeAction;
import mspaint.gui.GUIHandler;
import mspaint.gui.sidebar.LayersArea;

/**
 * Undo/Redo history of the layer pixels.
 * Call begin() before an edit and end() after it; a step is recorded only if the pixels changed.
 * A snapshot holds one pixel array per layer followed by {width, height} of the canvas.
 */
public class History {
    private static final int MAX_STEPS = 50;

    private final GUIHandler gui;
    private final Deque<int[][]> undoStack = new ArrayDeque<>();
    private final Deque<int[][]> redoStack = new ArrayDeque<>();
    private int[][] pending;
    private boolean pendingPushed;
    private int[][] saved;

    public History(GUIHandler gui) {
        this.gui = gui;
    }

    public void begin() {
        pending = snapshot();
        pendingPushed = false;
    }

    public void end() {
        if(pending == null || pendingPushed) {
            return;
        }
        if(!Arrays.deepEquals(pending, snapshot())) {
            undoStack.push(pending);
            if(undoStack.size() > MAX_STEPS) {
                undoStack.removeLast();
            }
            redoStack.clear();
            pendingPushed = true;
        }
    }

    public void undo() {
        if(gui.textAction.isEditing()) {
            gui.textAction.cancel();
            return;
        }
        move(undoStack, redoStack);
    }

    public void redo() {
        move(redoStack, undoStack);
    }

    /**
     * Remembers the current pixels as the saved picture, e.g. after opening or saving a file.
     */
    public void markSaved() {
        saved = snapshot();
    }

    /**
     * @return whether the pixels differ from the ones last marked saved
     */
    public boolean isModified() {
        return saved == null || !Arrays.deepEquals(saved, snapshot());
    }

    /**
     * Forgets every step, e.g. when another picture is opened.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        pending = null;
    }

    /**
     * Drops a deleted layer from the stored steps so the other layers keep their place.
     */
    public void removeLayer(int index) {
        removeLayer(undoStack, index);
        removeLayer(redoStack, index);
        pending = null;
    }

    private static void removeLayer(Deque<int[][]> stack, int index) {
        Deque<int[][]> updated = new ArrayDeque<>();
        for(int[][] data : stack) {
            if(index < data.length - 1) {
                int[][] copy = new int[data.length - 1][];
                for(int i = 0, j = 0; i < data.length; i++) {
                    if(i != index) {
                        copy[j++] = data[i];
                    }
                }
                data = copy;
            }
            updated.addLast(data);
        }
        stack.clear();
        stack.addAll(updated);
    }

    private void move(Deque<int[][]> from, Deque<int[][]> to) {
        if(from.isEmpty() || gui.viewAction.isDragging || gui.viewAction.isDraggingImage) {
            return;
        }
        int[][] current = snapshot();
        gui.viewAction.cancelSelection();
        ShapeAction.cancelPending();
        restore(from.pop());
        to.push(current);
        pending = null;
        gui.view.repaint();
    }

    /**
     * Pixels of every layer, with the floating selection (if any) stamped on the active layer.
     */
    private int[][] snapshot() {
        int[][] data = new int[gui.layers.size() + 1][];
        data[data.length - 1] = new int[] {gui.mainImage.getWidth(), gui.mainImage.getHeight()};
        for(int i = 0; i < data.length - 1; i++) {
            BufferedImage image = gui.layers.get(i).image;
            if(i == LayersArea.activeLayer && gui.isSelectionActive && gui.viewAction.sImage != null) {
                BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = copy.createGraphics();
                g.drawImage(image, 0, 0, null);
                Point loc = gui.viewAction.getSelectionLocation();
                g.drawImage(gui.viewAction.sImage, loc.x, loc.y, null);
                g.dispose();
                image = copy;
            }
            data[i] = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        }
        return data;
    }

    private void restore(int[][] data) {
        final int[] size = data[data.length - 1];
        if(size[0] != gui.mainImage.getWidth() || size[1] != gui.mainImage.getHeight()) {
            List<BufferedImage> images = new ArrayList<>();
            for(int i = 0; i < gui.layers.size(); i++) {
                images.add(new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB));
            }
            gui.setLayerImages(images);
        }
        for(int i = 0; i < data.length - 1 && i < gui.layers.size(); i++) {
            BufferedImage image = gui.layers.get(i).image;
            if(data[i].length == image.getWidth() * image.getHeight()) {
                image.setRGB(0, 0, image.getWidth(), image.getHeight(), data[i], 0, image.getWidth());
            }
        }
    }
}
