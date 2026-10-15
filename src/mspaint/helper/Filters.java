package mspaint.helper;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * Filters: the image filters of the Filter side bar. Each returns a new image and keeps transparency.
 */
public class Filters {
    public static final String[] NAMES = {"Grayscale", "Invert", "Sepia", "Blur", "Sharpen", "Brighten", "Darken"};

    public static BufferedImage apply(String name, BufferedImage image) {
        switch(name) {
            case "Blur":
                return convolve(image, new float[] {1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f, 1/9f});
            case "Sharpen":
                return convolve(image, new float[] {0, -1, 0, -1, 5, -1, 0, -1, 0});
        }
        final int w = image.getWidth();
        final int h = image.getHeight();
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
        for(int i = 0; i < pixels.length; i++) {
            final int a = pixels[i] >>> 24;
            final int r = (pixels[i] >> 16) & 0xff;
            final int g = (pixels[i] >> 8) & 0xff;
            final int b = pixels[i] & 0xff;
            int nr = r, ng = g, nb = b;
            switch(name) {
                case "Grayscale": {
                    final int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                    nr = ng = nb = gray;
                    break;
                }
                case "Invert":
                    nr = 255 - r;
                    ng = 255 - g;
                    nb = 255 - b;
                    break;
                case "Sepia":
                    nr = (int) (0.393 * r + 0.769 * g + 0.189 * b);
                    ng = (int) (0.349 * r + 0.686 * g + 0.168 * b);
                    nb = (int) (0.272 * r + 0.534 * g + 0.131 * b);
                    break;
                case "Brighten":
                    nr = r + (255 - r) / 5;
                    ng = g + (255 - g) / 5;
                    nb = b + (255 - b) / 5;
                    break;
                case "Darken":
                    nr = r * 4 / 5;
                    ng = g * 4 / 5;
                    nb = b * 4 / 5;
                    break;
            }
            pixels[i] = (a << 24) | (clamp(nr) << 16) | (clamp(ng) << 8) | clamp(nb);
        }
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, w, h, pixels, 0, w);
        return result;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static BufferedImage convolve(BufferedImage image, float[] kernel) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null).filter(image, result);
        return result;
    }
}
