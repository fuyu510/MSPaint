package mspaint.action;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

import mspaint.action.home.ShapeAction;
import mspaint.gui.GUIHandler;
import mspaint.helper.Point;
import mspaint.helper.ResourceLoader;
import mspaint.helper.TButton;
import mspaint.helper.Texture;
import mspaint.gui.toolbar.home.ColorPlateTool.ColorButton;
import mspaint.gui.toolbar.home.Image.RecSelectionTool;
import mspaint.gui.toolbar.home.brushes.AirBrushTool;
import mspaint.gui.toolbar.home.brushes.CalligraphyBrush1Tool;
import mspaint.gui.toolbar.home.brushes.CalligraphyBrush2Tool;
import mspaint.gui.toolbar.home.brushes.ColorPencilTool;
import mspaint.gui.toolbar.home.brushes.CrayonTool;
import mspaint.gui.toolbar.home.brushes.LineBrushTool;
import mspaint.gui.toolbar.home.brushes.MarkerTool;
import mspaint.gui.toolbar.home.brushes.OilBrushTool;
import mspaint.gui.toolbar.home.brushes.WatercolorBrushTool;
import mspaint.gui.toolbar.home.main.FillTool;
import mspaint.gui.toolbar.home.main.PencilTool;
import mspaint.gui.sidebar.LayersArea;

/**
 * Action
 */
public class MainViewAction extends MouseAdapter implements ActionListener {
    private Point startPoint, endPoint;
    private GUIHandler gui;
    public boolean isDragging;
    public boolean isDraggingImage;
    public boolean isInSelection;
    public BufferedImage sImage;
    private Point imgLoc, temps, tempe;
    private Path2D freePath = new Path2D.Double();
    private TButton cutTool, copyTool, cropTool;

    public MainViewAction(GUIHandler gui) {
        this.gui = gui;
        startPoint = new Point(0, 0);
        endPoint = new Point(0, 0);
        cutTool = gui.homeToolBar.clipboardTools.cutTool;
        copyTool = gui.homeToolBar.clipboardTools.copyTool;
        cropTool = gui.homeToolBar.imageTools.cropTool;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        final int x = (int) (e.getX() * (1/gui.view.scale));
        final int y = (int) (e.getY() * (1/gui.view.scale));
        endPoint.resetPoint(x, y);

        if(!dragIsInBrushTools(gui.graphics, startPoint, endPoint) && !ShapeAction.mouseDragged(startPoint, endPoint)) {
            if(gui.activeToolName.equals("Rectangular Selection Tool") || gui.activeToolName.equals("Free-form Selection Tool")) {
                if(!isDragging && isInSelection) {
                    if(sImage != null) {
                        final int w = sImage.getWidth();
                        final int h = sImage.getHeight();
                        temps = new Point(imgLoc.x + (x - startPoint.x),imgLoc.y + (y - startPoint.y));
                        tempe = new Point(temps.x + w, temps.y + h);

                        clearImage(gui.tempImage);
                        RecSelectionTool.setTempImage(gui.tempImage);
                        RecSelectionTool.drawWithImage(sImage, temps, tempe);
                        isDraggingImage = true;
                    }
                }
                else {
                    clearImage(gui.tempImage);
                    if(isFreeForm()) {
                        freePath.lineTo(x, y);
                        Graphics2D g = gui.tempImage.createGraphics();
                        g.setPaint(Texture.getSelectionPaint());
                        g.draw(freePath);
                        g.dispose();
                    }
                    else {
                        RecSelectionTool.setTempImage(gui.tempImage);
                        RecSelectionTool.draw(startPoint, endPoint);
                    }
                    final int w = Math.abs(x - startPoint.x);
                    final int h = Math.abs(y - startPoint.y);
                    gui.selectionResoLabel.setText(w + " ," + h + " px");
                    isDragging = true;
                }
            }
        }

        gui.view.repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        final int x = (int) (e.getX() * (1/gui.view.scale));
        final int y = (int) (e.getY() * (1/gui.view.scale));
        gui.pointerLocLabel.setText(x + "," + y + " px");

        if(!gui.activeToolName.equals("Rectangular Selection Tool") &&
           !gui.activeToolName.equals("Free-form Selection Tool") &&
           !moveIsInBrushTools(new Point(x, y))) {

            final int z_width = gui.mainImage.getWidth() / 4;
            final int z_height = gui.mainImage.getHeight() / 4;
            final int w_half = z_width / 2;
            final int h_half = z_height / 2;
            
            if(gui.activeToolName.equals("Zoom Tool")) {
                clearImage(gui.tempImage);
                Graphics2D g = gui.tempImage.createGraphics();
                g.setColor(Color.BLACK);
                g.drawRect(x - w_half, y - h_half, z_width, z_height);
                gui.view.repaint();
            }
        }
        if(gui.isSelectionActive) {
            if(pointInSelection(x, y)) {
                gui.view.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                isInSelection = true;
            }
            else {
                gui.view.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                isInSelection = false;
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        final int x = (int) (e.getX() * (1/gui.view.scale));
        final int y = (int) (e.getY() * (1/gui.view.scale));
        clickIsInBrushTools(new Point(x, y));
        gui.history.end();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        final int x = (int) (e.getX() * (1/gui.view.scale));
        final int y = (int) (e.getY() * (1/gui.view.scale));
        startPoint.resetPoint(x, y);
        if(gui.textAction.isEditing()) {
            // a click outside the text box finishes the text
            gui.textAction.commit();
            if(gui.activeToolName.equals("Text Tool")) {
                return;
            }
        }
        gui.history.begin();
        if(!ShapeAction.hasPending(gui.activeToolName)) {
            ShapeAction.finishPending();
        }
        if(!isInSelection) {
            drawImageSelected();
            freePath = new Path2D.Double();
            freePath.moveTo(x, y);
        }
        if(gui.activeToolName.equals("Color Picker Tool")) {
            if(x < 0 || y < 0 || x >= gui.mainImage.getWidth() || y >= gui.mainImage.getHeight()) {
                return;
            }
            int rgb = gui.activeImage().getRGB(x, y);
            Color c = new Color(rgb);
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 255);
            ColorButton color1 = gui.homeToolBar.colorPlate.color1Button;
            ColorButton color2 = gui.homeToolBar.colorPlate.color2Button;
            if(color1.isSelected()) {
                gui.activeColor1 = c;
                color1.setColor(c);
                color1.repaint();
            }
            else {
                gui.activeColor2 = c;
                color2.setColor(c);
                color2.repaint();
            }
        }
        else if(gui.activeToolName.equals("Text Tool")) {
            gui.textAction.start(x, y);
        }
        else if(gui.activeToolName.equals("Fill Tool")) {
            Color color = SwingUtilities.isRightMouseButton(e) ? gui.activeColor2 : gui.activeColor1;
            FillTool.fill(gui.layers.get(LayersArea.activeLayer).image, x, y, color);
            gui.view.repaint();
        }
        else if(gui.activeToolName.equals("Zoom Tool")) {
            if(SwingUtilities.isRightMouseButton(e)) {
                gui.viewToolBar.zoomTools.zoomOut.doClick();
            }
            else if(SwingUtilities.isLeftMouseButton(e)) {
                gui.viewToolBar.zoomTools.zoomIn.doClick();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(!ShapeAction.mouseReleased(e)) {
            if(gui.activeToolName.equals("Rectangular Selection Tool") || gui.activeToolName.equals("Free-form Selection Tool")) {
                if(isDragging) {
                    if(isFreeForm()) {
                        setFreeFormSelection(freePath);
                    }
                    else {
                        setSelection(startPoint.x, startPoint.y, endPoint.x - startPoint.x, endPoint.y - startPoint.y, false);
                    }
                    isDragging = false;
                }
                else if(isDraggingImage) {
                    imgLoc = temps;
                    isDraggingImage = false;
                }
            }
        }
        gui.history.end();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseExited(MouseEvent e) {
        gui.pointerLocLabel.setText("");
        if(!isDragging && !isDraggingImage && !gui.isSelectionActive) {
            clearImage(gui.tempImage);
            gui.view.repaint();
        }
    }


    private boolean pointInSelection(int x, int y) {
        final int x1 = temps.x;
        final int y1 = temps.y;
        final int x2 = tempe.x;
        final int y2 = tempe.y;

        if((x1 < x && x < x2) && (y1 < y && y < y2)) {
            return true;
        }
        return false;
    }

    public static void clearImage(final BufferedImage image) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final Color color = new Color(0, 0, 0, 0);

        for(int i= 0 ; i < width ; i++){
            for(int j = 0 ; j < height; j++){  
                image.setRGB(i, j, color.getRGB());
            }
        }
    }

    /**
     * Makes the pixels of the rectangle transparent (clipped to the image).
     */
    public static void clearArea(final BufferedImage image, int x, int y, int w, int h) {
        Rectangle area = new Rectangle(x, y, w, h).intersection(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
        if(!area.isEmpty()) {
            image.setRGB(area.x, area.y, area.width, area.height, new int[area.width * area.height], 0, area.width);
        }
    }

    /**
     * Cuts the pixels inside the area (and inside the mask, if given) out of the active layer.
     */
    private BufferedImage liftPixels(Rectangle area, Shape mask) {
        final BufferedImage source = gui.activeImage();
        final int w = area.width;
        final int h = area.height;
        int[] pixels = source.getRGB(area.x, area.y, w, h, null, 0, w);
        int[] lifted = new int[w * h];
        for(int j = 0; j < h; j++) {
            for(int i = 0; i < w; i++) {
                if(mask == null || mask.contains(area.x + i + 0.5, area.y + j + 0.5)) {
                    lifted[j * w + i] = pixels[j * w + i];
                    pixels[j * w + i] = 0;
                }
            }
        }
        source.setRGB(area.x, area.y, w, h, pixels, 0, w);
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, w, h, lifted, 0, w);
        return image;
    }

    private boolean isFreeForm() {
        return gui.activeToolName.equals("Free-form Selection Tool");
    }

    public void pasteImage(BufferedImage image) {
        pasteImage(image, 0, 0);
    }

    public void pasteImage(BufferedImage image, final int x, final int y) {
        drawImageSelected();
        showFloating(ClipboardAction.clone(image), x, y);
    }

    public void setSelection(int x, int y, int w, int h, boolean isFull) {
        Rectangle canvas = new Rectangle(0, 0, gui.mainImage.getWidth(), gui.mainImage.getHeight());
        Rectangle area = isFull ? canvas : new Rectangle(Math.min(x, x + w), Math.min(y, y + h), Math.abs(w), Math.abs(h)).intersection(canvas);
        if(area.isEmpty()) {
            clearImage(gui.tempImage);
            gui.selectionResoLabel.setText("");
            return;
        }
        showFloating(liftPixels(area, null), area.x, area.y);
    }

    private void setFreeFormSelection(Path2D path) {
        path.closePath();
        Rectangle area = path.getBounds().intersection(new Rectangle(0, 0, gui.mainImage.getWidth(), gui.mainImage.getHeight()));
        if(area.isEmpty()) {
            clearImage(gui.tempImage);
            gui.selectionResoLabel.setText("");
            return;
        }
        showFloating(liftPixels(area, path), area.x, area.y);
    }

    /**
     * Shows the image as the floating selection at (x, y); it is stamped on the layer by drawImageSelected().
     */
    private void showFloating(BufferedImage image, int x, int y) {
        sImage = image;
        imgLoc = new Point(x, y);
        redrawFloating();
        gui.isSelectionActive = true;
        cutTool.setTEnabled(true);
        copyTool.setTEnabled(true);
        cropTool.setTEnabled(true);
        gui.view.repaint();
    }

    /**
     * Replaces the floating image (e.g. rotated or filtered), keeping its top left corner.
     */
    public void setFloatingImage(BufferedImage image) {
        if(gui.isSelectionActive) {
            showFloating(image, imgLoc.x, imgLoc.y);
        }
    }

    private void redrawFloating() {
        temps = new Point(imgLoc.x, imgLoc.y);
        tempe = new Point(imgLoc.x + sImage.getWidth(), imgLoc.y + sImage.getHeight());
        clearImage(gui.tempImage);
        RecSelectionTool.setTempImage(gui.tempImage);
        RecSelectionTool.drawWithImage(sImage, temps, tempe);
        gui.selectionResoLabel.setText(sImage.getWidth() + " ," + sImage.getHeight() + " px");
    }

    /**
     * @return the floating selection's area on the canvas, or null when nothing is selected
     */
    public Rectangle getSelectionBounds() {
        if(!gui.isSelectionActive || sImage == null) {
            return null;
        }
        return new Rectangle(imgLoc.x, imgLoc.y, sImage.getWidth(), sImage.getHeight());
    }

    public void drawImageSelected() {
        if(gui.isSelectionActive) {
            if(sImage != null) {
                gui.graphics.drawImage(sImage, null, imgLoc.x, imgLoc.y);
                clearImage(gui.tempImage);
                gui.view.repaint();
            }
            gui.isSelectionActive = false;
            isInSelection = false;
            gui.selectionResoLabel.setText("");
            cutTool.setTEnabled(false);
            copyTool.setTEnabled(false);
            cropTool.setTEnabled(false);
        }
    }

    public void cancelSelection() {
        if(gui.isSelectionActive) {
            sImage = null;
            clearImage(gui.tempImage);
            gui.isSelectionActive = false;
            isInSelection = false;
            gui.selectionResoLabel.setText("");
            cutTool.setTEnabled(false);
            copyTool.setTEnabled(false);
            cropTool.setTEnabled(false);
        }
    }

    public Point getSelectionLocation() {
        return imgLoc;
    }

    private boolean clickIsInBrushTools(Point point) {
        boolean isIn;
        isIn = dragIsInBrushTools(gui.graphics, point, point);
        gui.view.repaint();
        return isIn;
    }

    private boolean moveIsInBrushTools(Point point) {
        clearImage(gui.tempImage);
        boolean isIn;
        if(!gui.activeToolName.equals("Eraser Tool")) {
            isIn = dragIsInBrushTools(gui.tempGraphics, point, point);
        }
        else {
            final int x = point.x;
            final int y = point.y;
            final int size = gui.activeSize;
            final int half = gui.activeSize / 2;
            gui.tempGraphics.setStroke(new BasicStroke(1));
            gui.tempGraphics.setColor(Color.WHITE);
            gui.tempGraphics.fillRect(x - half, y - half, size, size);
            gui.tempGraphics.setColor(Color.BLACK);
            gui.tempGraphics.drawRect(x - half, y - half, size, size);

            isIn = true;
        }
        gui.view.repaint();
        return isIn;
    }

    private boolean dragIsInBrushTools(Graphics2D graphics, Point startPoint, Point endPoint) {
        if(gui.activeToolName.equals("Pencil Tool")) {
            PencilTool.setGraphics(graphics);
            PencilTool.setColor(gui.activeColor1);
            PencilTool.setStrokeSize(gui.activeSize);
            PencilTool.draw(startPoint, endPoint);
            startPoint.resetPoint(endPoint.x, endPoint.y);
            return true;
        }
        else if(gui.activeToolName.equals("Eraser Tool")) {
            final int x = startPoint.x;
            final int y = startPoint.y;
            final int size = gui.activeSize;
            final int half = gui.activeSize / 2;

            clearImage(gui.tempImage);
            gui.tempGraphics.setStroke(new BasicStroke(1));
            gui.tempGraphics.setColor(Color.WHITE);
            gui.tempGraphics.fillRect(x - half, y - half, size, size);
            gui.tempGraphics.setColor(Color.BLACK);
            gui.tempGraphics.drawRect(x - half, y - half, size, size);

            clearArea(gui.activeImage(), x - half, y - half, size, size);
            startPoint.resetPoint(endPoint.x, endPoint.y);
            return true;
        }
        else if(gui.activeToolName.equals("Line Brush")) {
            LineBrushTool.setGraphics(graphics);
            LineBrushTool.setColor(gui.activeColor1);
            LineBrushTool.setStrokeSize(gui.activeSize);
            LineBrushTool.draw(startPoint, endPoint);
            startPoint.resetPoint(endPoint.x, endPoint.y);
            return true;
        }
        else if(gui.activeToolName.equals("Air Brush")) {
            AirBrushTool.setGraphics(graphics);
            AirBrushTool.setColor(gui.activeColor1);
            AirBrushTool.setStrokeSize(gui.activeSize);
            AirBrushTool.draw(startPoint, endPoint);
            startPoint.resetPoint(endPoint.x, endPoint.y);
            return true;
        }
        else if(gui.activeToolName.equals("Calligraphy Brush 1")) {
            CalligraphyBrush1Tool.setGraphics(graphics);
            CalligraphyBrush1Tool.setColor(gui.activeColor1);
            CalligraphyBrush1Tool.setStrokeSize(gui.activeSize);
            CalligraphyBrush1Tool.draw(startPoint, endPoint);
            startPoint.resetPoint(endPoint.x, endPoint.y);
            return true;
        }
        else if(gui.activeToolName.equals("Calligraphy Brush 2")) {
            CalligraphyBrush2Tool.setGraphics(graphics);
            CalligraphyBrush2Tool.setColor(gui.activeColor1);
            CalligraphyBrush2Tool.setStrokeSize(gui.activeSize);
            CalligraphyBrush2Tool.draw(startPoint, endPoint);
            startPoint.resetPoint(endPoint.x, endPoint.y);
            return true;
        }
        else if(gui.activeToolName.equals("Color Pencil")) {
            ColorPencilTool.setGraphics(graphics);
            ColorPencilTool.setColor(gui.activeColor1);
            ColorPencilTool.setStrokeSize(gui.activeSize);
            ColorPencilTool.draw(startPoint, endPoint);
            startPoint.resetPoint(endPoint.x, endPoint.y);
            return true;
        }
        else if(gui.activeToolName.equals("Oil Brush")) {
            OilBrushTool.setGraphics(graphics);
            OilBrushTool.setColor(gui.activeColor1);
            OilBrushTool.setStrokeSize(gui.activeSize);
            OilBrushTool.draw(startPoint, endPoint);
            startPoint.resetPoint(endPoint.x, endPoint.y);
            return true;
        }
        else if(gui.activeToolName.equals("Crayon")) {
            CrayonTool.setGraphics(graphics);
            CrayonTool.setColor(gui.activeColor1);
            CrayonTool.setStrokeSize(gui.activeSize);
            CrayonTool.draw(startPoint, endPoint);
            startPoint.resetPoint(endPoint.x, endPoint.y);
            return true;
        }
        else if(gui.activeToolName.equals("Marker")) {
            MarkerTool.setGraphics(graphics);
            MarkerTool.setColor(gui.activeColor1);
            MarkerTool.setStrokeSize(gui.activeSize);
            MarkerTool.draw(startPoint, endPoint);
            startPoint.resetPoint(endPoint.x, endPoint.y);
            return true;
        }
        else if(gui.activeToolName.equals("Watercolor Brush")) {
            WatercolorBrushTool.setGraphics(graphics);
            WatercolorBrushTool.setColor(gui.activeColor1);
            WatercolorBrushTool.setStrokeSize(gui.activeSize);
            WatercolorBrushTool.draw(startPoint, endPoint);
            startPoint.resetPoint(endPoint.x, endPoint.y);
            return true;
        }
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        TButton button = (TButton)e.getSource();
        gui.history.begin();

        if(button.getText().equals("Select all")) {
            ShapeAction.finishPending();
            drawImageSelected();
            gui.homeToolBar.imageTools.rectangularSecItem.doClick();
            setSelection(0, 0, 0, 0, true);
        }
        else if(button.getText().equals("Delete")) {
            cancelSelection();
            gui.view.repaint();
        }
        gui.history.end();
    }
}
