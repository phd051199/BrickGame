package brickgame;

import javax.microedition.lcdui.Graphics;

final class LcdGridRenderer {
    static final int COLOR_OUTER = 0x252B23;
    static final int COLOR_LCD = 0x737E62;
    static final int COLOR_LCD_DARK = 0x66755F;
    static final int COLOR_INK = 0x000000;
    static final int COLOR_SHADOW = 0x272C23;

    private LcdGridRenderer() {
    }

    static void drawCase(Graphics graphics, LayoutMetrics layout) {
        graphics.setColor(COLOR_LCD);
        graphics.fillRect(0, 0, layout.screenWidth, layout.screenHeight);
    }

    static void drawBoardGrid(Graphics graphics, LayoutMetrics layout) {
        graphics.setColor(COLOR_INK);
        graphics.fillRect(layout.boardX - 1, layout.boardY - 1,
                layout.boardWidth + 2, layout.boardHeight + 2);
        graphics.setColor(COLOR_LCD);
        graphics.fillRect(layout.boardX, layout.boardY,
                layout.boardWidth, layout.boardHeight);
        drawGrid(graphics, layout.boardColumns, layout.boardRows,
                layout.boardX, layout.boardY, layout.boardCell);
    }

    static void drawActiveBoard(Graphics graphics, short[] rows,
            LayoutMetrics layout) {
        graphics.setColor(COLOR_INK);
        for (int y = 0; y < layout.boardRows; y++) {
            int sourceY = y + layout.boardRowOffset;
            int bits = sourceY < rows.length ? rows[sourceY] & 0xFFFF : 0;
            for (int x = 0; x < layout.boardColumns; x++) {
                int sourceX = x + layout.boardColumnOffset;
                if ((bits & (1 << sourceX)) != 0) {
                    drawCell(graphics, layout.boardX + x * layout.boardCell,
                            layout.boardY + y * layout.boardCell,
                            layout.boardCell, true);
                }
            }
        }
    }

    static void drawPreviewGrid(Graphics graphics, int left, int top, int cell) {
        drawGrid(graphics, 4, 4, left, top, cell);
        graphics.setColor(COLOR_SHADOW);
        graphics.drawRect(left - 2, top - 2, cell * 4 + 2, cell * 4 + 2);
    }

    static void drawActivePreview(Graphics graphics, byte[] rows,
            int left, int top, int cell) {
        graphics.setColor(COLOR_INK);
        for (int y = 0; y < 4; y++) {
            int bits = rows[y] & 255;
            for (int x = 0; x < 4; x++) {
                if ((bits & (1 << x)) != 0) {
                    drawCell(graphics, left + x * cell, top + y * cell,
                            cell, true);
                }
            }
        }
    }

    private static void drawGrid(Graphics graphics, int width, int height,
            int left, int top, int cell) {
        graphics.setColor(COLOR_LCD_DARK);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                drawCell(graphics, left + x * cell, top + y * cell,
                        cell, false);
            }
        }
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
        int core = active ? cell / 2 : cell / 3;
        if (core < 2) {
            core = 2;
        }
        if (!active && cell < 6) {
            core = 1;
        }
        if (core > interior) {
            core = interior;
        }
        int coreOffset = gap + 1 + (interior - core + 1) / 2;
        graphics.fillRect(x + coreOffset, y + coreOffset, core, core);
    }
}
