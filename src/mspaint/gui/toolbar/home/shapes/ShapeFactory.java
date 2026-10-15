package mspaint.gui.toolbar.home.shapes;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * ShapeFactory: builds the closed shapes of the Shapes tool group, fitted to a bounding box.
 */
public class ShapeFactory {

    /**
     * @return the shape named by the tool, fitted to the box, or null if the name is not a closed shape
     */
    public static Shape create(String name, Rectangle2D box) {
        final double x = box.getX(), y = box.getY(), w = box.getWidth(), h = box.getHeight();
        switch(name) {
            case "Oval":
                return new Ellipse2D.Double(x, y, w, h);
            case "Rectangle":
                return new Rectangle2D.Double(x, y, w, h);
            case "Round Rec": {
                final double arc = Math.min(w, h) / 4;
                return new RoundRectangle2D.Double(x, y, w, h, arc, arc);
            }
        }
        Shape unit = createUnit(name, w, h);
        if(unit == null) {
            return null;
        }
        AffineTransform t = new AffineTransform();
        t.translate(x, y);
        t.scale(w, h);
        return t.createTransformedShape(unit);
    }

    /**
     * The shape in a 1x1 box; w and h are the real size, for parts that must keep their proportions.
     */
    private static Shape createUnit(String name, double w, double h) {
        switch(name) {
            case "Triangle":
                return polygon(0.5, 0, 1, 1, 0, 1);
            case "Right Triangle":
                return polygon(0, 0, 1, 1, 0, 1);
            case "Diamond":
                return polygon(0.5, 0, 1, 0.5, 0.5, 1, 0, 0.5);
            case "Pentagon":
                return regular(5, 1.0, -90);
            case "Hexagon":
                return regular(6, 1.0, 0);
            case "Right Arrow":
                return arrow(0);
            case "Down Arrow":
                return arrow(90);
            case "Left Arrow":
                return arrow(180);
            case "Up Arrow":
                return arrow(270);
            case "4 Point Star":
                return star(4, 0.3, -90);
            case "5 Point Star":
                return star(5, 0.38, -90);
            case "6 Point Star":
                return star(6, 0.55, -90);
            case "Round Rec Callout": {
                final double arc = 0.2;
                Area area = new Area(new RoundRectangle2D.Double(0, 0, 1, 0.75, arc * Math.min(w, h) / w, arc * Math.min(w, h) / (0.75 * h)));
                area.add(new Area(polygon(0.2, 0.7, 0.4, 0.7, 0.1, 1)));
                return area;
            }
            case "Oval Callout": {
                Area area = new Area(new Ellipse2D.Double(0, 0, 1, 0.75));
                area.add(new Area(polygon(0.25, 0.65, 0.42, 0.7, 0.12, 1)));
                return area;
            }
            case "Cloud Callout":
                return cloud();
            case "Heart":
                return heart();
            case "Lightening":
                return polygon(0.38, 0, 0.62, 0.22, 0.52, 0.27, 0.78, 0.5, 0.68, 0.55, 1, 1,
                               0.5, 0.65, 0.6, 0.6, 0.25, 0.42, 0.37, 0.36, 0, 0.18);
        }
        return null;
    }

    private static Path2D polygon(double... xy) {
        Path2D path = new Path2D.Double();
        path.moveTo(xy[0], xy[1]);
        for(int i = 2; i < xy.length; i += 2) {
            path.lineTo(xy[i], xy[i + 1]);
        }
        path.closePath();
        return path;
    }

    /**
     * Polygon through the given points, stretched so it fills the 1x1 box.
     */
    private static Path2D fitted(double[] xy) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for(int i = 0; i < xy.length; i += 2) {
            minX = Math.min(minX, xy[i]);
            maxX = Math.max(maxX, xy[i]);
            minY = Math.min(minY, xy[i + 1]);
            maxY = Math.max(maxY, xy[i + 1]);
        }
        for(int i = 0; i < xy.length; i += 2) {
            xy[i] = (xy[i] - minX) / (maxX - minX);
            xy[i + 1] = (xy[i + 1] - minY) / (maxY - minY);
        }
        return polygon(xy);
    }

    private static Path2D regular(int sides, double radius, double startDegree) {
        double[] xy = new double[sides * 2];
        for(int i = 0; i < sides; i++) {
            final double angle = Math.toRadians(startDegree + 360.0 * i / sides);
            xy[2 * i] = radius * Math.cos(angle);
            xy[2 * i + 1] = radius * Math.sin(angle);
        }
        return fitted(xy);
    }

    private static Path2D star(int points, double innerRatio, double startDegree) {
        double[] xy = new double[points * 4];
        for(int i = 0; i < points * 2; i++) {
            final double radius = (i % 2 == 0) ? 1.0 : innerRatio;
            final double angle = Math.toRadians(startDegree + 180.0 * i / points);
            xy[2 * i] = radius * Math.cos(angle);
            xy[2 * i + 1] = radius * Math.sin(angle);
        }
        return fitted(xy);
    }

    private static Shape arrow(double degree) {
        Path2D right = polygon(0, 0.25, 0.6, 0.25, 0.6, 0, 1, 0.5, 0.6, 1, 0.6, 0.75, 0, 0.75);
        AffineTransform t = AffineTransform.getRotateInstance(Math.toRadians(degree), 0.5, 0.5);
        return t.createTransformedShape(right);
    }

    private static Shape cloud() {
        Area area = new Area();
        final double[][] puffs = {
            {0.05, 0.15, 0.35, 0.35}, {0.25, 0.0, 0.35, 0.35}, {0.5, 0.02, 0.35, 0.35}, {0.65, 0.15, 0.35, 0.35},
            {0.55, 0.35, 0.35, 0.35}, {0.3, 0.4, 0.35, 0.33}, {0.05, 0.35, 0.33, 0.33}, {0.0, 0.25, 0.3, 0.3},
            {0.2, 0.15, 0.6, 0.45}
        };
        for(double[] p : puffs) {
            area.add(new Area(new Ellipse2D.Double(p[0], p[1], p[2], p[3])));
        }
        area.add(new Area(new Ellipse2D.Double(0.2, 0.8, 0.1, 0.08)));
        area.add(new Area(new Ellipse2D.Double(0.12, 0.92, 0.06, 0.06)));
        return area;
    }

    private static Shape heart() {
        Path2D path = new Path2D.Double();
        path.moveTo(0.5, 0.25);
        path.curveTo(0.5, 0.0, 0.0, -0.05, 0.0, 0.3);
        path.curveTo(0.0, 0.6, 0.35, 0.75, 0.5, 1.0);
        path.curveTo(0.65, 0.75, 1.0, 0.6, 1.0, 0.3);
        path.curveTo(1.0, -0.05, 0.5, 0.0, 0.5, 0.25);
        path.closePath();
        Rectangle2D b = path.getBounds2D();
        AffineTransform t = new AffineTransform();
        t.scale(1 / b.getWidth(), 1 / b.getHeight());
        t.translate(-b.getX(), -b.getY());
        return t.createTransformedShape(path);
    }
}
