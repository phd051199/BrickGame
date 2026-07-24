package brickgame;

import javax.microedition.lcdui.Graphics;

/**
 * Sharp monochrome renderer for the original 8x13 Brick Game digits.
 * The row masks were extracted from the original digit PNGs and are rendered
 * without palette conversion or interpolation.
 */
final class BrickNumberFont {

    static final int DIGIT_WIDTH = 8;
    static final int DIGIT_HEIGHT = 13;
    static final int SPACING = 1;

    private static final short[][] DIGITS = {
        {0x7E, 0xBD, 0xC3, 0xC3, 0xC3, 0x81, 0x00, 0x81, 0xC3, 0xC3, 0xC3, 0xBD, 0x7E},
        {0x00, 0x01, 0x03, 0x03, 0x03, 0x01, 0x00, 0x01, 0x03, 0x03, 0x03, 0x01, 0x00},
        {0x7E, 0x3D, 0x03, 0x03, 0x03, 0x3D, 0x7E, 0xBC, 0xC0, 0xC0, 0xC0, 0xBC, 0x7E},
        {0x7E, 0x3D, 0x03, 0x03, 0x03, 0x3D, 0x7E, 0x3D, 0x03, 0x03, 0x03, 0x3D, 0x7E},
        {0x00, 0x81, 0xC3, 0xC3, 0xC3, 0xBD, 0x7E, 0x3D, 0x03, 0x03, 0x03, 0x01, 0x00},
        {0x7E, 0xBC, 0xC0, 0xC0, 0xC0, 0xBC, 0x7E, 0x3D, 0x03, 0x03, 0x03, 0x3D, 0x7E},
        {0x7E, 0xBC, 0xC0, 0xC0, 0xC0, 0xBC, 0x7E, 0xBD, 0xC3, 0xC3, 0xC3, 0xBD, 0x7E},
        {0x7E, 0x3D, 0x03, 0x03, 0x03, 0x01, 0x00, 0x01, 0x03, 0x03, 0x03, 0x01, 0x00},
        {0x7E, 0xBD, 0xC3, 0xC3, 0xC3, 0xBD, 0x7E, 0xBD, 0xC3, 0xC3, 0xC3, 0xBD, 0x7E},
        {0x7E, 0xBD, 0xC3, 0xC3, 0xC3, 0xBD, 0x7E, 0x3D, 0x03, 0x03, 0x03, 0x3D, 0x7E}
    };

    int width(int capacity, int scale) {
        return capacity * DIGIT_WIDTH * scale
            + (capacity - 1) * SPACING * scale;
    }

    void draw(Graphics graphics, int value, int capacity, int x, int y,
              int color, int scale) {
        int divisor = divisor(capacity);
        graphics.setColor(color);
        int i;
        for (i = 0; i < capacity; i++) {
            int digit = (value / divisor) % 10;
            if (digit < 0) {
                digit = -digit;
            }
            drawDigit(graphics, digit, x, y, scale);
            x += (DIGIT_WIDTH + SPACING) * scale;
            if (divisor > 1) {
                divisor /= 10;
            }
        }
    }

    void drawCentered(Graphics graphics, int value, int capacity, int centerX,
                      int y, int color, int scale) {
        draw(graphics, value, capacity,
            centerX - width(capacity, scale) / 2, y, color, scale);
    }

    private static void drawDigit(Graphics graphics, int digit, int left,
                                  int top, int scale) {
        int y;
        int x;
        for (y = 0; y < DIGIT_HEIGHT; y++) {
            int row = DIGITS[digit][y] & 0xFF;
            for (x = 0; x < DIGIT_WIDTH; x++) {
                if ((row & (1 << (DIGIT_WIDTH - 1 - x))) != 0) {
                    graphics.fillRect(left + x * scale, top + y * scale,
                        scale, scale);
                }
            }
        }
    }

    private static int divisor(int capacity) {
        int divisor = 1;
        int i;
        for (i = 1; i < capacity; i++) {
            divisor *= 10;
        }
        return divisor;
    }
}
