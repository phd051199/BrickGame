package brickgame;

import javax.microedition.lcdui.Graphics;

/** Sharp original-style 8x13 Brick Game digits. */
final class BrickNumberFont {
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
    private static final int COMPACT_WIDTH = 5;
    private static final int COMPACT_HEIGHT = 9;
    private static final byte[][] COMPACT_DIGITS = {
        {0x0E,0x11,0x11,0x13,0x15,0x19,0x11,0x11,0x0E},
        {0x04,0x0C,0x14,0x04,0x04,0x04,0x04,0x04,0x1F},
        {0x0E,0x11,0x01,0x01,0x06,0x08,0x10,0x10,0x1F},
        {0x1E,0x01,0x01,0x01,0x0E,0x01,0x01,0x01,0x1E},
        {0x02,0x06,0x0A,0x12,0x1F,0x02,0x02,0x02,0x02},
        {0x1F,0x10,0x10,0x10,0x1E,0x01,0x01,0x11,0x0E},
        {0x0E,0x10,0x10,0x10,0x1E,0x11,0x11,0x11,0x0E},
        {0x1F,0x01,0x02,0x02,0x04,0x04,0x08,0x08,0x08},
        {0x0E,0x11,0x11,0x11,0x0E,0x11,0x11,0x11,0x0E},
        {0x0E,0x11,0x11,0x11,0x0F,0x01,0x01,0x01,0x0E}
    };

    int width(int digits) {
        return digits * WIDTH + (digits - 1) * SPACING;
    }

    void draw(Graphics graphics, int value, int digits, int x, int y, int color) {
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

    void drawCentered(Graphics graphics, int value, int digits,
            int centerX, int y, int color) {
        draw(graphics, value, digits, centerX - width(digits) / 2, y, color);
    }

    void drawClockCentered(Graphics graphics, int first, int second,
            int centerX, int y, int color) {
        if (first < 0) {
            first = 0;
        }
        if (second < 0) {
            second = 0;
        }
        first %= 100;
        second %= 100;
        int pairWidth = WIDTH * 2 + SPACING;
        int clockWidth = pairWidth * 2 + CLOCK_GAP * 2 + 1;
        int left = centerX - clockWidth / 2;
        int colonX = left + pairWidth + CLOCK_GAP;
        int secondPairX = colonX + 1 + CLOCK_GAP;
        graphics.setColor(color);
        drawDigit(graphics, first / 10, left, y);
        drawDigit(graphics, first % 10, left + WIDTH + SPACING, y);
        graphics.fillRect(colonX, y + 4, 1, 2);
        graphics.fillRect(colonX, y + 9, 1, 2);
        drawDigit(graphics, second / 10, secondPairX, y);
        drawDigit(graphics, second % 10,
                secondPairX + WIDTH + SPACING, y);
    }

    void drawCompactCentered(Graphics graphics, String text,
            int centerX, int y, int color) {
        int x = centerX - compactWidth(text) / 2;
        graphics.setColor(color);
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character >= '0' && character <= '9') {
                drawCompactDigit(graphics, character - '0', x, y);
                x += COMPACT_WIDTH;
            } else if (character == ':') {
                graphics.fillRect(x, y + 2, 2, 2);
                graphics.fillRect(x, y + 6, 2, 2);
                x += 2;
            } else if (character == '-') {
                graphics.fillRect(x, y + 4, COMPACT_WIDTH, 1);
                x += COMPACT_WIDTH;
            }
            if (i + 1 < text.length()) {
                x++;
            }
        }
    }

    private static int compactWidth(String text) {
        int result = 0;
        for (int i = 0; i < text.length(); i++) {
            result += text.charAt(i) == ':' ? 2 : COMPACT_WIDTH;
            if (i + 1 < text.length()) {
                result++;
            }
        }
        return result;
    }

    private static void drawCompactDigit(Graphics graphics, int digit,
            int left, int top) {
        for (int y = 0; y < COMPACT_HEIGHT; y++) {
            int bits = COMPACT_DIGITS[digit][y] & 0x1F;
            for (int x = 0; x < COMPACT_WIDTH; x++) {
                if ((bits & (1 << (COMPACT_WIDTH - 1 - x))) != 0) {
                    graphics.fillRect(left + x, top + y, 1, 1);
                }
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
