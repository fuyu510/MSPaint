package mspaint.gui.toolbar.home.main;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * FillTool (paint bucket): flood fills the area of the same color around a point.
 */
public class FillTool {

    public static void fill(BufferedImage image, int x, int y, Color color) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        if(x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        final int target = pixels[y * width + x];
        final int replacement = color.getRGB();
        if(target == replacement) {
            return;
        }

        // scanline flood fill with an explicit stack of seed points
        int[] stack = new int[1024];
        int top = 0;
        stack[top++] = y * width + x;
        while(top > 0) {
            final int seed = stack[--top];
            final int sy = seed / width;
            int left = seed % width;
            while(left > 0 && pixels[sy * width + left - 1] == target) {
                left--;
            }
            boolean spanUp = false, spanDown = false;
            for(int i = left; i < width && pixels[sy * width + i] == target; i++) {
                pixels[sy * width + i] = replacement;
                if(sy > 0) {
                    final boolean match = pixels[(sy - 1) * width + i] == target;
                    if(match && !spanUp) {
                        if(top == stack.length) {
                            stack = Arrays.copyOf(stack, top * 2);
                        }
                        stack[top++] = (sy - 1) * width + i;
                    }
                    spanUp = match;
                }
                if(sy < height - 1) {
                    final boolean match = pixels[(sy + 1) * width + i] == target;
                    if(match && !spanDown) {
                        if(top == stack.length) {
                            stack = Arrays.copyOf(stack, top * 2);
                        }
                        stack[top++] = (sy + 1) * width + i;
                    }
                    spanDown = match;
                }
            }
        }
        image.setRGB(0, 0, width, height, pixels, 0, width);
    }
}
