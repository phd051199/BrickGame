package e23;

import java.util.Calendar;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

final class E23Renderer {
    static final int LCD_COLOR = 0x737E62;
    static final int INK_COLOR = 0x000000;

    private static final int SHADOW_COLOR = 0x272C23;
    private static final int INACTIVE_COLOR = 0x66755F;
    private static final int BOARD_COLUMNS = 10;
    private static final int BOARD_ROWS = 20;
    private static final long BATTERY_REFRESH_MS = 30000L;

    private static final int[] DIGIT_MASKS = {
        0x3F, 0x06, 0x5B, 0x4F, 0x66,
        0x6D, 0x7D, 0x07, 0x7F, 0x6F
    };
    private static final short[][] SCORE_SEGMENTS = {
        {1626, 1627, 1625, 1528, 1529, 1530, 1531},
        {1498, 1499, 1497, 1512, 1513, 1514, 1515},
        {1610, 1611, 1609, 1480, 1481, 1482, 1483},
        {1594, 1595, 1593, 1432, 1433, 1434, 1435}
    };
    private static final short[] SPEED_SEGMENTS = {
        1571, 1587, 1603, 1619, 1602, 1570, 1586
    };
    private static final short[] LEVEL_SEGMENTS = {
        1635, 1651, 1667, 1683, 1666, 1634, 1650
    };

    private final BitmapFont regularFont;
    private final BitmapFont boldFont;
    private final DigitFont digits = new DigitFont();
    private final short[] boardRows = new short[BOARD_ROWS];
    private final byte[] nextRows = new byte[4];
    private long countdownStartedAt = -1L;

    private Image background;
    private int backgroundWidth = -1;
    private int backgroundHeight = -1;
    private boolean portrait;
    private int screenWidth;
    private int screenHeight;
    private int boardX;
    private int boardY;
    private int boardCell;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelCenter;
    private int nextCell;
    private int nextCenter;
    private int nextLeft;
    private int nextTop;
    private int statsCenter;
    private int statusCenter;
    private int portraitTop;
    private int portraitStatsTop;
    private int portraitFooterTop;
    private long batteryReadAt = -BATTERY_REFRESH_MS;
    private int batteryPercent = -1;

    E23Renderer(BitmapFont regularFont, BitmapFont boldFont) {
        this.regularFont = regularFont;
        this.boldFont = boldFont;
    }

    void restartCountdown() {
        countdownStartedAt = System.currentTimeMillis();
    }

    boolean countdownActive(long now) {
        return countdownStartedAt >= 0L
                && now - countdownStartedAt < 4000L;
    }

    void draw(Graphics graphics, E23Cpu cpu, E23LcdMap map, boolean paused,
            int width, int height) {
        ensureBackground(width, height);
        if (background != null) {
            graphics.drawImage(background, 0, 0, Graphics.TOP | Graphics.LEFT);
        } else {
            drawBackground(graphics);
        }

        byte[] lcdRam = cpu.lcdRam();
        map.decode(lcdRam, cpu.lcdEnabled(), boardRows, nextRows);
        drawActiveBoard(graphics);
        drawActiveNext(graphics);
        drawRuntimePanel(graphics, lcdRam, paused);
        if (!portrait) {
            drawDeviceStatus(graphics, System.currentTimeMillis());
        }
    }

    private void ensureBackground(int width, int height) {
        if (backgroundWidth == width && backgroundHeight == height) {
            return;
        }
        configure(width, height);
        backgroundWidth = width;
        backgroundHeight = height;
        background = null;
        try {
            background = Image.createImage(width, height);
            drawBackground(background.getGraphics());
        } catch (Throwable ignored) {
            background = null;
        }
    }

    private void configure(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        portrait = height > width;
        if (portrait) {
            int margin = 4;
            int gap = 5;
            boardCell = (height - margin * 2) / BOARD_ROWS;
            if (boardCell < 8) {
                boardCell = 8;
            }
            boardX = margin;
            boardY = (height - BOARD_ROWS * boardCell) / 2;
            panelX = boardX + BOARD_COLUMNS * boardCell + gap;
            panelY = margin;
            panelWidth = width - margin - panelX;
            panelCenter = panelX + panelWidth / 2;
            portraitTop = (height - 308) / 2;
            if (portraitTop < margin) {
                portraitTop = margin;
            }
            portraitStatsTop = portraitTop + 126;
            portraitFooterTop = height - 80;
            nextCell = panelWidth >= 76 ? 10 : 8;
            nextCenter = panelCenter;
            nextLeft = nextCenter - nextCell * 2;
            nextTop = portraitTop + 61;
            statsCenter = panelCenter;
            statusCenter = 0;
        } else {
            boardX = 66;
            boardY = 10;
            boardCell = 11;
            panelX = 182;
            panelY = 4;
            panelWidth = 134;
            panelCenter = 249;
            nextCell = 9;
            nextCenter = 215;
            nextLeft = 197;
            nextTop = 72;
            statsCenter = 282;
            statusCenter = 33;
        }
    }

    private void drawBackground(Graphics graphics) {
        graphics.setColor(LCD_COLOR);
        graphics.fillRect(0, 0, screenWidth, screenHeight);
        drawBoardGrid(graphics);
        if (portrait) {
            drawPortraitPanel(graphics);
        } else {
            drawLandscapePanel(graphics);
        }
    }

    private void drawBoardGrid(Graphics graphics) {
        int boardWidth = BOARD_COLUMNS * boardCell;
        int boardHeight = BOARD_ROWS * boardCell;
        graphics.setColor(INK_COLOR);
        graphics.fillRect(boardX - 1, boardY - 1, boardWidth + 2, boardHeight + 2);
        graphics.setColor(LCD_COLOR);
        graphics.fillRect(boardX, boardY, boardWidth, boardHeight);
        graphics.setColor(INACTIVE_COLOR);
        for (int row = 0; row < BOARD_ROWS; row++) {
            for (int column = 0; column < BOARD_COLUMNS; column++) {
                drawCell(graphics, boardX + column * boardCell,
                        boardY + row * boardCell, boardCell);
            }
        }
    }

    private void drawLandscapePanel(Graphics graphics) {
        boldFont.drawString(graphics, "E23 96 IN 1", panelCenter,
                panelY + 16, Graphics.HCENTER, INK_COLOR);
        regularFont.drawString(graphics, "HT943", panelCenter,
                panelY + 31, Graphics.HCENTER, INK_COLOR);

        graphics.setColor(SHADOW_COLOR);
        graphics.drawLine(panelX + 4, panelY + 42,
                panelX + panelWidth - 5, panelY + 42);

        regularFont.drawString(graphics, "NEXT", nextCenter,
                panelY + 65, Graphics.HCENTER, INK_COLOR);
        drawNextGrid(graphics);
        regularFont.drawString(graphics, "SCORE", statsCenter,
                panelY + 65, Graphics.HCENTER, INK_COLOR);
        regularFont.drawString(graphics, "SPEED", statsCenter,
                panelY + 108, Graphics.HCENTER, INK_COLOR);
        regularFont.drawString(graphics, "LEVEL", statsCenter,
                panelY + 151, Graphics.HCENTER, INK_COLOR);

        graphics.setColor(SHADOW_COLOR);
        graphics.drawLine(panelX + 4, panelY + 180,
                panelX + panelWidth - 5, panelY + 180);
        drawControlHints(graphics);

        graphics.drawLine(8, panelY + 76, 57, panelY + 76);
        graphics.drawLine(8, panelY + 156, 57, panelY + 156);
        regularFont.drawString(graphics, "BAT", statusCenter,
                panelY + 33, Graphics.HCENTER, INK_COLOR);
        regularFont.drawString(graphics, "TIME", statusCenter,
                panelY + 111, Graphics.HCENTER, INK_COLOR);
        regularFont.drawString(graphics, "START", statusCenter,
                panelY + 191, Graphics.HCENTER, INK_COLOR);
    }

    private void drawPortraitPanel(Graphics graphics) {
        boldFont.drawString(graphics, "E23", panelCenter,
                portraitTop + 14, Graphics.HCENTER, INK_COLOR);
        regularFont.drawString(graphics, "96 IN 1", panelCenter,
                portraitTop + 30, Graphics.HCENTER, INK_COLOR);

        graphics.setColor(SHADOW_COLOR);
        graphics.drawLine(panelX + 2, portraitTop + 38,
                panelX + panelWidth - 3, portraitTop + 38);

        regularFont.drawString(graphics, "NEXT", nextCenter,
                portraitTop + 53, Graphics.HCENTER, INK_COLOR);
        drawNextGrid(graphics);

        graphics.setColor(SHADOW_COLOR);
        graphics.drawLine(panelX + 2, portraitStatsTop,
                panelX + panelWidth - 3, portraitStatsTop);
        regularFont.drawString(graphics, "SCORE", statsCenter,
                portraitStatsTop + 17, Graphics.HCENTER, INK_COLOR);
        regularFont.drawString(graphics, "SPEED", statsCenter,
                portraitStatsTop + 49, Graphics.HCENTER, INK_COLOR);
        regularFont.drawString(graphics, "LEVEL", statsCenter,
                portraitStatsTop + 81, Graphics.HCENTER, INK_COLOR);

        graphics.setColor(SHADOW_COLOR);
        graphics.drawLine(panelX + 2, portraitFooterTop,
                panelX + panelWidth - 3, portraitFooterTop);
        drawControlHints(graphics);
    }

    private void drawControlHints(Graphics graphics) {
        if (portrait) {
            int left = panelX + 8;
            drawHint(graphics, "1", "START", left, portraitFooterTop + 20);
            drawHint(graphics, "2", "AUX", left, portraitFooterTop + 38);
            drawHint(graphics, "#", "PAUSE", left, portraitFooterTop + 56);
            drawHint(graphics, "*", "RESET", left, portraitFooterTop + 74);
            return;
        }
        int leftColumn = panelX + 8;
        int rightColumn = panelX + panelWidth / 2 + 8;
        drawHint(graphics, "P", "START", leftColumn, panelY + 206);
        drawHint(graphics, "O", "AUX", rightColumn, panelY + 206);
        drawHint(graphics, "#", "PAUSE", leftColumn, panelY + 223);
        drawHint(graphics, "*", "RESET", rightColumn, panelY + 223);
    }

    private void drawHint(Graphics graphics, String key, String label,
            int left, int baseline) {
        int labelLeft = left + 13;
        boldFont.drawString(graphics, key, left, baseline + 2,
                Graphics.LEFT, INK_COLOR);
        regularFont.drawString(graphics, label, labelLeft, baseline,
                Graphics.LEFT, INK_COLOR);
    }

    private void drawNextGrid(Graphics graphics) {
        graphics.setColor(INACTIVE_COLOR);
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                drawPreviewCell(graphics, nextLeft + column * nextCell,
                        nextTop + row * nextCell, nextCell);
            }
        }
        graphics.setColor(SHADOW_COLOR);
        graphics.drawRect(nextLeft - 1, nextTop - 1,
                nextCell * 4 + 1, nextCell * 4 + 1);
    }

    private void drawActiveBoard(Graphics graphics) {
        graphics.setColor(INK_COLOR);
        for (int row = 0; row < BOARD_ROWS; row++) {
            int bits = boardRows[row] & 0xFFFF;
            for (int column = 0; column < BOARD_COLUMNS; column++) {
                if ((bits & (1 << column)) != 0) {
                    drawCell(graphics, boardX + column * boardCell,
                            boardY + row * boardCell, boardCell);
                }
            }
        }
    }

    private void drawActiveNext(Graphics graphics) {
        int firstRow = 4;
        int lastRow = -1;
        for (int row = 0; row < 4; row++) {
            if ((nextRows[row] & 15) != 0) {
                if (row < firstRow) {
                    firstRow = row;
                }
                lastRow = row;
            }
        }
        int rowShift = 0;
        if (lastRow >= firstRow) {
            rowShift = (4 - (lastRow - firstRow + 1)) / 2 - firstRow;
        }

        graphics.setColor(INK_COLOR);
        for (int row = 0; row < 4; row++) {
            int bits = nextRows[row] & 255;
            int targetRow = row + rowShift;
            if (targetRow < 0 || targetRow >= 4) {
                continue;
            }
            for (int column = 0; column < 4; column++) {
                if ((bits & (1 << column)) != 0) {
                    drawPreviewCell(graphics, nextLeft + column * nextCell,
                            nextTop + targetRow * nextCell, nextCell);
                }
            }
        }
    }

    private static void drawCell(Graphics graphics, int x, int y, int cell) {
        int inset = 1;
        int borderExtent = cell - inset * 2 - 1;
        graphics.drawRect(x + inset, y + inset,
                borderExtent, borderExtent);
        int inside = borderExtent - 1;
        int core = inside / 2;
        if (((inside - core) & 1) != 0) {
            core++;
        }
        if (core < 2) {
            core = 2;
        }
        int offset = inset + 1 + (inside - core) / 2;
        graphics.fillRect(x + offset, y + offset, core, core);
    }

    private static void drawPreviewCell(Graphics graphics, int x, int y,
            int cell) {
        int inset = 1;
        int borderExtent = cell - inset * 2 - 1;
        graphics.drawRect(x + inset, y + inset,
                borderExtent, borderExtent);
        int inside = borderExtent - 1;
        int core = inside / 2;
        if (((inside - core) & 1) != 0) {
            core++;
        }
        if (core < 2) {
            core = 2;
        }
        int offset = inset + 1 + (inside - core) / 2;
        graphics.fillRect(x + offset, y + offset, core, core);
    }

    private void drawRuntimePanel(Graphics graphics, byte[] lcdRam, boolean paused) {
        if (portrait) {
            regularFont.drawString(graphics, paused ? "PAUSED" : "RUNNING",
                    nextCenter, portraitTop + 118,
                    Graphics.HCENTER, INK_COLOR);
            drawNumber(graphics, decodeNumber(lcdRam, SCORE_SEGMENTS), 4,
                    statsCenter, portraitStatsTop + 22);
            drawNumber(graphics,
                    decodeTwoDigits(lcdRam, SPEED_SEGMENTS, (short) 1618), 2,
                    statsCenter, portraitStatsTop + 54);
            drawNumber(graphics,
                    decodeTwoDigits(lcdRam, LEVEL_SEGMENTS, (short) 1682), 2,
                    statsCenter, portraitStatsTop + 86);
            return;
        }

        drawNumber(graphics, decodeNumber(lcdRam, SCORE_SEGMENTS), 4,
                statsCenter, panelY + 72);
        drawNumber(graphics,
                decodeTwoDigits(lcdRam, SPEED_SEGMENTS, (short) 1618), 2,
                statsCenter, panelY + 115);
        drawNumber(graphics,
                decodeTwoDigits(lcdRam, LEVEL_SEGMENTS, (short) 1682), 2,
                statsCenter, panelY + 158);
        regularFont.drawString(graphics, paused ? "PAUSED" : "RUNNING",
                nextCenter, panelY + 135, Graphics.HCENTER, INK_COLOR);
    }

    private void drawNumber(Graphics graphics, int value, int count,
            int center, int y) {
        if (value < 0) {
            regularFont.drawString(graphics, "--", center, y + 10,
                    Graphics.HCENTER, INK_COLOR);
        } else {
            digits.drawCentered(graphics, value, count, center, y, INK_COLOR);
        }
    }

    private void drawDeviceStatus(Graphics graphics, long now) {
        updateBattery(now);
        if (batteryPercent < 0) {
            regularFont.drawString(graphics, "--", statusCenter,
                    panelY + 52, Graphics.HCENTER, INK_COLOR);
        } else {
            digits.drawCentered(graphics, batteryPercent,
                    batteryPercent >= 100 ? 3 : 2,
                    statusCenter, panelY + 42, INK_COLOR);
        }

        Calendar calendar = Calendar.getInstance();
        digits.drawClock(graphics,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), statusCenter,
                panelY + 120, INK_COLOR);

        if (countdownStartedAt < 0L) {
            regularFont.drawString(graphics, "--", statusCenter,
                    panelY + 210, Graphics.HCENTER, INK_COLOR);
            return;
        }
        long elapsed = now - countdownStartedAt;
        if (elapsed < 3000L) {
            int value = 3 - (int) (elapsed / 1000L);
            digits.drawCentered(graphics, value, 1,
                    statusCenter, panelY + 200, INK_COLOR);
        } else {
            boldFont.drawString(graphics, "GO", statusCenter,
                    panelY + 214, Graphics.HCENTER, INK_COLOR);
        }
    }

    private void updateBattery(long now) {
        if (now - batteryReadAt < BATTERY_REFRESH_MS) {
            return;
        }
        batteryReadAt = now;
        String value;
        try {
            value = System.getProperty("com.nokia.mid.batterylevel");
        } catch (Throwable ignored) {
            value = null;
        }
        batteryPercent = parsePercent(value);
    }

    private static int parsePercent(String value) {
        if (value == null) {
            return -1;
        }
        int result = 0;
        boolean found = false;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character >= '0' && character <= '9') {
                found = true;
                result = result * 10 + character - '0';
                if (result > 100) {
                    return -1;
                }
            } else if (found) {
                break;
            }
        }
        return found ? result : -1;
    }

    private static int decodeTwoDigits(byte[] lcdRam, short[] units, short tens) {
        int unit = decodeDigit(lcdRam, units);
        boolean hasTens = bit(lcdRam, tens);
        if (unit < 0) {
            return hasTens ? 10 : -1;
        }
        return unit + (hasTens ? 10 : 0);
    }

    private static int decodeNumber(byte[] lcdRam, short[][] segments) {
        int value = 0;
        boolean seen = false;
        for (int i = 0; i < segments.length; i++) {
            int digit = decodeDigit(lcdRam, segments[i]);
            if (digit < 0) {
                if (!seen && segmentMask(lcdRam, segments[i]) == 0) {
                    continue;
                }
                return -1;
            }
            seen = true;
            value = value * 10 + digit;
        }
        return seen ? value : -1;
    }

    private static int decodeDigit(byte[] lcdRam, short[] segments) {
        int mask = segmentMask(lcdRam, segments);
        if (mask == 0) {
            return -1;
        }
        for (int digit = 0; digit < DIGIT_MASKS.length; digit++) {
            if (DIGIT_MASKS[digit] == mask) {
                return digit;
            }
        }
        return -1;
    }

    private static int segmentMask(byte[] lcdRam, short[] segments) {
        int mask = 0;
        for (int i = 0; i < segments.length; i++) {
            if (bit(lcdRam, segments[i])) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    private static boolean bit(byte[] lcdRam, short reference) {
        int encoded = reference & 65535;
        return ((lcdRam[encoded >> 3] & 255) & (1 << (encoded & 7))) != 0;
    }
}
