package e23;

import javax.microedition.lcdui.Graphics;

final class DigitFont {
    private static final int WIDTH = 8;
    private static final int HEIGHT = 13;
    private static final int SPACING = 2;
    private static final int CLOCK_GAP = 3;
    private static final short[][] DIGITS = {
        {0x7E,0xBD,0xC3,0xC3,0xC3,0x81,0x00,0x81,0xC3,0xC3,0xC3,0xBD,0x7E},
        {0x00,0x01,0x03,0x03,0x03,0x01,0x00,0x01,0x03,0x03,0x03,0x01,0x00},
        {0x7E,0x3D,0x03,0x03,0x03,0x3D,0x7E,0xBC,0xC0,0xC0,0xC0,0xBC,0x7E},
        {0x7E,0x3D,0x03,0x03,0x03,0x3D,0x7E,0x3D,0x03,0x03,0x03,0x3D,0x7E},
        {0x00,0x81,0xC3,0xC3,0xC3,0xBD,0x7E,0x3D,0x03,0x03,0x03,0x01,0x00},
        {0x7E,0xBC,0xC0,0xC0,0xC0,0xBC,0x7E,0x3D,0x03,0x03,0x03,0x3D,0x7E},
        {0x7E,0xBC,0xC0,0xC0,0xC0,0xBC,0x7E,0xBD,0xC3,0xC3,0xC3,0xBD,0x7E},
        {0x7E,0x3D,0x03,0x03,0x03,0x01,0x00,0x01,0x03,0x03,0x03,0x01,0x00},
        {0x7E,0xBD,0xC3,0xC3,0xC3,0xBD,0x7E,0xBD,0xC3,0xC3,0xC3,0xBD,0x7E},
        {0x7E,0xBD,0xC3,0xC3,0xC3,0xBD,0x7E,0x3D,0x03,0x03,0x03,0x3D,0x7E}
    };

    void drawCentered(Graphics graphics, int value, int digits,
            int centerX, int y, int color) {
        draw(graphics, value, digits,
                centerX - width(digits) / 2, y, color);
    }

    void drawClock(Graphics graphics, int first, int second,
            int centerX, int y, int color) {
        first = first < 0 ? 0 : first % 100;
        second = second < 0 ? 0 : second % 100;
        int pairWidth = WIDTH * 2 + SPACING;
        int totalWidth = pairWidth * 2 + CLOCK_GAP * 2 + 1;
        int left = centerX - totalWidth / 2;
        int colonX = left + pairWidth + CLOCK_GAP;
        int secondX = colonX + 1 + CLOCK_GAP;
        graphics.setColor(color);
        drawDigit(graphics, first / 10, left, y);
        drawDigit(graphics, first % 10, left + WIDTH + SPACING, y);
        graphics.fillRect(colonX, y + 4, 1, 2);
        graphics.fillRect(colonX, y + 9, 1, 2);
        drawDigit(graphics, second / 10, secondX, y);
        drawDigit(graphics, second % 10, secondX + WIDTH + SPACING, y);
    }

    private static int width(int digits) {
        return digits * WIDTH + (digits - 1) * SPACING;
    }

    private static void draw(Graphics graphics, int value, int digits,
            int x, int y, int color) {
        int divisor = 1;
        for (int i = 1; i < digits; i++) {
            divisor *= 10;
        }
        graphics.setColor(color);
        for (int i = 0; i < digits; i++) {
            int digit = value / divisor % 10;
            if (digit < 0) {
                digit = -digit;
            }
            drawDigit(graphics, digit, x, y);
            x += WIDTH + SPACING;
            if (divisor > 1) {
                divisor /= 10;
            }
        }
    }

    private static void drawDigit(Graphics graphics, int digit, int left, int top) {
        for (int y = 0; y < HEIGHT; y++) {
            int bits = DIGITS[digit][y] & 255;
            for (int x = 0; x < WIDTH; x++) {
                if ((bits & (1 << (WIDTH - 1 - x))) != 0) {
                    graphics.fillRect(left + x, top + y, 1, 1);
                }
            }
        }
    }
}
