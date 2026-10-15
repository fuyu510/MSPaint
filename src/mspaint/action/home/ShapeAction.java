package mspaint.action.home;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.pump.awt.BrushStroke;
import java.awt.event.MouseEvent;

import mspaint.action.ClipboardAction;
import mspaint.action.MainViewAction;
import mspaint.gui.GUIHandler;
import mspaint.gui.toolbar.home.ShapeTools;
import mspaint.gui.toolbar.home.shapes.ShapeFactory;
import mspaint.helper.Point;

public class ShapeAction {
    private static GUIHandler gui;
    private static ShapeTools shapeTools;
    private static boolean isDragging;

    // Curve: first drag sets the end points, the next two drags (or clicks) bend it
    private static int curveStage;
    private static Point curveStart, curveEnd, control1, control2;

    // Polygon: first drag draws a side, then each click adds a corner;
    // clicking near the first corner or double-clicking closes it
    private static final List<Point> polygon = new ArrayList<>();
    private static Point polygonStart;
    private static final int CLOSE_DISTANCE = 8;

    public ShapeAction(GUIHandler gui, ShapeTools shapeTools) {
        ShapeAction.gui = gui;
        ShapeAction.shapeTools = shapeTools;
    }

    public static boolean mouseDragged(Point startPoint, Point endPoint) {
        final String tool = gui.activeToolName;

        if(tool.equals("Line")) {
            drawPreview(new Line2D.Double(startPoint.x, startPoint.y, endPoint.x, endPoint.y), false);
        }
        else if(tool.equals("Curve")) {
            if(curveStage == 0) {
                curveStart = copy(startPoint);
                curveEnd = copy(endPoint);
                control1 = copy(startPoint);
                control2 = copy(endPoint);
            }
            else {
                setControl(endPoint);
            }
            drawPreview(curve(), false);
        }
        else if(tool.equals("Polygon")) {
            Path2D path = polyline();
            if(polygon.isEmpty()) {
                polygonStart = copy(startPoint);
                path.moveTo(startPoint.x, startPoint.y);
            }
            path.lineTo(endPoint.x, endPoint.y);
            drawPreview(path, false);
        }
        else {
            Shape shape = ShapeFactory.create(tool, box(startPoint, endPoint));
            if(shape == null) {
                return false;
            }
            drawPreview(shape, true);
        }
        isDragging = true;
        return true;
    }

    public static boolean mouseReleased(MouseEvent e) {
        final String tool = gui.activeToolName;
        final Point point = new Point((int) (e.getX() * (1/gui.view.scale)), (int) (e.getY() * (1/gui.view.scale)));

        if(tool.equals("Curve")) {
            if(curveStage == 0 && !isDragging) {
                return true;
            }
            if(curveStage > 0) {
                setControl(point);
                drawPreview(curve(), false);
            }
            isDragging = false;
            if(++curveStage == 3) {
                curveStage = 0;
                commit();
            }
            return true;
        }
        else if(tool.equals("Polygon")) {
            if(polygon.isEmpty()) {
                if(isDragging) {
                    polygon.add(polygonStart);
                    polygon.add(point);
                    drawPreview(polyline(), false);
                }
            }
            else {
                final Point first = polygon.get(0);
                final Point last = polygon.get(polygon.size() - 1);
                final boolean nearFirst = Math.abs(point.x - first.x) <= CLOSE_DISTANCE && Math.abs(point.y - first.y) <= CLOSE_DISTANCE;
                final boolean sameAsLast = point.x == last.x && point.y == last.y;
                if(!nearFirst && !sameAsLast) {
                    polygon.add(point);
                }
                if(nearFirst || e.getClickCount() >= 2) {
                    Path2D path = polyline();
                    path.closePath();
                    drawPreview(path, true);
                    polygon.clear();
                    commit();
                }
                else {
                    drawPreview(polyline(), false);
                }
            }
            isDragging = false;
            return true;
        }

        if(isDragging) {
            isDragging = false;
            commit();
            return true;
        }
        return false;
    }

    /**
     * Stamps an unfinished curve or polygon onto the canvas, e.g. when another tool is used.
     */
    public static void finishPending() {
        if(curveStage == 0 && polygon.isEmpty()) {
            return;
        }
        if(!polygon.isEmpty() && polygon.size() > 2) {
            Path2D path = polyline();
            path.closePath();
            drawPreview(path, true);
        }
        gui.graphics.drawImage(gui.shapeImage, 0, 0, null);
        cancelPending();
    }

    public static void cancelPending() {
        curveStage = 0;
        polygon.clear();
        isDragging = false;
        MainViewAction.clearImage(gui.shapeImage);
    }

    public static boolean hasPending(String toolName) {
        return (curveStage > 0 && toolName.equals("Curve")) || (!polygon.isEmpty() && toolName.equals("Polygon"));
    }

    /**
     * Moves the finished shape from the shape layer into a floating selection, so it can still be moved.
     */
    private static void commit() {
        Rectangle bounds = opaqueBounds(gui.shapeImage);
        if(bounds == null) {
            return;
        }
        BufferedImage image = ClipboardAction.clone(gui.shapeImage.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height));
        MainViewAction.clearImage(gui.shapeImage);
        gui.homeToolBar.imageTools.rectangularSecItem.doClick();
        gui.viewAction.pasteImage(image, bounds.x, bounds.y);
    }

    private static Rectangle opaqueBounds(BufferedImage image) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                if((pixels[y * width + x] >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if(maxX < 0) {
            return null;
        }
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static void drawPreview(Shape shape, boolean isClosed) {
        MainViewAction.clearImage(gui.shapeImage);
        Graphics2D g = gui.shapeGraphics;
        final int size = gui.activeSize;

        if(isClosed) {
            Stroke[] fillStroke = getFillStroke(shapeTools.activeFill, size);
            Color color = gui.activeColor2;
            if(fillStroke[0] != null) {
                g.setColor(color);
                g.fill(shape);
                color = lighter(gui.activeColor2, getLightness(shapeTools.activeFill));
            }
            if(fillStroke[1] != null) {
                g.setColor(color);
                g.setStroke(fillStroke[1]);
                Rectangle2D b = shape.getBounds2D();
                for(int gap = size / 2; gap < Math.min(b.getWidth(), b.getHeight()) / 2; gap += Math.max(size, 1)) {
                    g.draw(inset(shape, gap));
                }
            }
        }

        // a line or curve without an outline would be invisible, so they always get one
        String outline = shapeTools.activeOutline;
        if(!isClosed && outline.equals("No Outline")) {
            outline = "Solid";
        }
        Stroke[] lineStroke = getLineStroke(outline, size);
        if(lineStroke[0] != null) {
            g.setColor(gui.activeColor1);
            g.setStroke(lineStroke[0]);
            g.draw(shape);
        }
        if(lineStroke[1] != null) {
            g.setColor(lighter(gui.activeColor1, getLightness(outline)));
            g.setStroke(lineStroke[1]);
            g.draw(shape);
        }
    }

    private static Shape inset(Shape shape, double gap) {
        Rectangle2D b = shape.getBounds2D();
        AffineTransform t = new AffineTransform();
        t.translate(b.getCenterX(), b.getCenterY());
        t.scale((b.getWidth() - 2 * gap) / b.getWidth(), (b.getHeight() - 2 * gap) / b.getHeight());
        t.translate(-b.getCenterX(), -b.getCenterY());
        return t.createTransformedShape(shape);
    }

    private static Rectangle2D box(Point a, Point b) {
        return new Rectangle2D.Double(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.abs(b.x - a.x), Math.abs(b.y - a.y));
    }

    private static Shape curve() {
        return new CubicCurve2D.Double(curveStart.x, curveStart.y, control1.x, control1.y,
                                       control2.x, control2.y, curveEnd.x, curveEnd.y);
    }

    private static void setControl(Point point) {
        if(curveStage == 1) {
            control1 = copy(point);
            control2 = copy(point);
        }
        else {
            control2 = copy(point);
        }
    }

    private static Path2D polyline() {
        Path2D path = new Path2D.Double();
        for(int i = 0; i < polygon.size(); i++) {
            final Point p = polygon.get(i);
            if(i == 0) {
                path.moveTo(p.x, p.y);
            }
            else {
                path.lineTo(p.x, p.y);
            }
        }
        return path;
    }

    private static Point copy(Point p) {
        return new Point(p.x, p.y);
    }

    private static Color lighter(Color color, int light) {
        final int red = color.getRed();
        final int green = color.getGreen();
        final int blue = color.getBlue();
        return new Color(
            (red+light > 255)? red-light : red+light,
            (green+light > 255)? green-light : green+light,
            (blue+light > 255)? blue-light : blue+light, 80);
    }

    private static int getLightness(String activeOutlineOrFill) {
        if(activeOutlineOrFill.equals("Oil")) {
            return 60;
        }
        else if(activeOutlineOrFill.equals("Watercolor")) {
            return 30;
        }
        return 0;
    }

    private static Stroke[] getLineStroke(String activeOutline, int size) {
        Stroke[] stroke = new Stroke[2];

        if(activeOutline.equals("Solid")) {
            stroke[0] = new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            stroke[1] = null;
        }
        else if(activeOutline.equals("Brush")) {
            stroke[0] = new BrushStroke(size, 0.87f);
            stroke[1] = null;
        }
        else if(activeOutline.equals("Oil")) {
            stroke[0] = new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            stroke[1] = new BrushStroke(size+2, 0.78f);
        }
        else if(activeOutline.equals("Watercolor")) {
            stroke[0] = new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            stroke[1] = new BrushStroke(size+2, 0.78f);
        }

        return stroke;
    }

    private static Stroke[] getFillStroke(String activeFill, int size) {
        Stroke[] stroke = new Stroke[2];

        if(activeFill.equals("Solid")) {
            stroke[0] = new BasicStroke(size);
            stroke[1] = null;
        }
        else if(activeFill.equals("Brush")) {
            stroke[0] = null;
            stroke[1] = new BrushStroke(size, 0.87f);
        }
        else if(activeFill.equals("Oil")) {
            stroke[0] = new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            stroke[1] = new BrushStroke(size+2, 0.78f);
        }
        else if(activeFill.equals("Watercolor")) {
            stroke[0] = new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            stroke[1] = new BrushStroke(size+2, 0.78f);
        }

        return stroke;
    }
}
