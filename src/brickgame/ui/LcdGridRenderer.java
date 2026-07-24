package brickgame;

import javax.microedition.lcdui.Graphics;

/** Shared crisp LCD drawing primitives with integer-only scaling. */
final class LcdGridRenderer {

    static final int COLOR_OUTER = 0x252B23;
    static final int COLOR_LCD = 0x737E62;
    static final int COLOR_LCD_DARK = 0x66755F;
    static final int COLOR_INK = 0x000000;
    static final int COLOR_SHADOW = 0x272C23;

    private LcdGridRenderer() {
    }

    static void drawCase(Graphics graphics, LayoutMetrics layout) {
        if (layout.landscape320) {
            graphics.setColor(COLOR_LCD);
            graphics.fillRect(0, 0, layout.screenWidth, layout.screenHeight);
            return;
        }
        graphics.setColor(COLOR_SHADOW);
        graphics.fillRect(layout.caseX, layout.caseY,
            layout.caseWidth, layout.caseHeight);
        graphics.setColor(COLOR_LCD);
        graphics.fillRect(layout.caseX + 2, layout.caseY + 2,
            layout.caseWidth - 4, layout.caseHeight - 4);
        graphics.setColor(COLOR_LCD_DARK);
        graphics.drawRect(layout.caseX + 3, layout.caseY + 3,
            layout.caseWidth - 7, layout.caseHeight - 7);
    }

    static void drawBoardGrid(Graphics graphics, LayoutMetrics layout) {
        drawGrid(graphics, 10, 20, layout.boardX, layout.boardY,
            layout.boardCell);
        /* Draw the frame last. The last column/row cells reach the frame pixel,
         * so drawing the grid after the frame hid its right and bottom edges. */
        graphics.setColor(COLOR_INK);
        graphics.drawRect(layout.boardX - 1, layout.boardY - 1,
            layout.boardWidth, layout.boardHeight);
    }

    static void drawActiveBoard(Graphics graphics, short[] rows,
                                LayoutMetrics layout) {
        drawActiveRows(graphics, rows, 10, 20,
            layout.boardX, layout.boardY, layout.boardCell);
    }

    static void drawPreviewGrid(Graphics graphics, int left, int top,
                                int width, int height, int cell) {
        drawGrid(graphics, width, height, left, top, cell);
        graphics.setColor(COLOR_SHADOW);
        graphics.drawRect(left - 2, top - 2,
            width * cell + 2, height * cell + 2);
    }

    static void drawActivePreview(Graphics graphics, byte[] rows,
                                  int left, int top, int cell) {
        graphics.setColor(COLOR_INK);
        int y;
        int x;
        for (y = 0; y < 4; y++) {
            int bits = rows[y] & 0xFF;
            for (x = 0; x < 4; x++) {
                if ((bits & (1 << x)) != 0) {
                    drawActiveCell(graphics,
                        left + x * cell, top + y * cell, cell);
                }
            }
        }
    }

    static void drawMiniBoard(Graphics graphics, short[] rows,
                              int left, int top, int cell) {
        drawPreviewGrid(graphics, left, top, 10, 20, cell);
        drawActiveRows(graphics, rows, 10, 20, left, top, cell);
    }

    private static void drawActiveRows(Graphics graphics, short[] rows,
                                       int width, int height, int left,
                                       int top, int cell) {
        graphics.setColor(COLOR_INK);
        int y;
        int x;
        for (y = 0; y < height; y++) {
            int bits = rows[y] & 0xFFFF;
            for (x = 0; x < width; x++) {
                if ((bits & (1 << x)) != 0) {
                    drawActiveCell(graphics,
                        left + x * cell, top + y * cell, cell);
                }
            }
        }
    }

    private static void drawGrid(Graphics graphics, int width, int height,
                                 int left, int top, int cell) {
        graphics.setColor(COLOR_LCD_DARK);
        int y;
        int x;
        for (y = 0; y < height; y++) {
            for (x = 0; x < width; x++) {
                drawCell(graphics, left + x * cell,
                    top + y * cell, cell, false);
            }
        }
    }

    private static void drawActiveCell(Graphics graphics, int x, int y,
                                       int cell) {
        drawCell(graphics, x, y, cell, true);
    }

    private static void drawCell(Graphics graphics, int x, int y,
                                 int cell, boolean active) {
        int gap = cell >= 8 ? 1 : 0;
        int size = cell - gap - 1;
        if (size < 2) {
            size = 2;
        }
        graphics.drawRect(x + gap, y + gap, size, size);
        int interior = size - 1;
        if (interior < 1) {
            interior = 1;
        }
        int core = cell / 3 + 1;
        if (core < 2) {
            core = 2;
        }
        if (!active && cell < 6) {
            core = 1;
        }
        if (core > interior) {
            core = interior;
        }
        int coreOffset = gap + 1 + (interior - core) / 2;
        graphics.fillRect(x + coreOffset, y + coreOffset, core, core);
    }
}
