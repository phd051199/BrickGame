package brickgame;

import java.util.Calendar;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/** Adaptive cached renderer using the original unboxed Brick Game label style. */
final class BrickRenderer {

    private static final long BATTERY_REFRESH_MS = 30000L;
    private static final String[] BATTERY_KEYS = {
        "com.nokia.mid.batterylevel",
        "com.sonyericsson.battery.level",
        "com.samsung.battery.level",
        "battery.level"
    };

    private final BitmapFont textFont = new BitmapFont();
    private final BrickNumberFont numberFont = new BrickNumberFont();
    private final long startedAt = System.currentTimeMillis();
    private Image base;
    private int baseWidth = -1;
    private int baseHeight = -1;
    private LayoutMetrics layout;
    private BitmapAssets statusAssets;
    private boolean statusAssetsAttempted;
    private long batteryReadAt = -BATTERY_REFRESH_MS;
    private int batteryPercent = -1;

    void render(Graphics graphics, GameSnapshot snapshot, int width, int height) {
        ensureBase(width, height);
        if (base != null) {
            graphics.drawImage(base, 0, 0, Graphics.TOP | Graphics.LEFT);
        } else {
            drawStatic(graphics, layout);
        }
        drawDynamic(graphics, snapshot, layout, System.currentTimeMillis());
    }

    void invalidate() {
        base = null;
        baseWidth = -1;
        baseHeight = -1;
        layout = null;
    }

    private void ensureBase(int width, int height) {
        if (layout != null && width == baseWidth && height == baseHeight) {
            return;
        }
        base = null;
        baseWidth = width;
        baseHeight = height;
        layout = new LayoutMetrics(width, height);
        try {
            Image image = Image.createImage(width, height);
            drawStatic(image.getGraphics(), layout);
            base = image;
        } catch (Throwable ignored) {
        }
    }

    private void drawStatic(Graphics graphics, LayoutMetrics metrics) {
        graphics.setColor(LcdGridRenderer.COLOR_OUTER);
        graphics.fillRect(0, 0, metrics.screenWidth, metrics.screenHeight);
        LcdGridRenderer.drawCase(graphics, metrics);
        LcdGridRenderer.drawBoardGrid(graphics, metrics);

        textFont.drawCentered(graphics,
            metrics.widePanel ? "BRICK GAME" : "BRICK",
            metrics.panelCenter, metrics.panelY + 1,
            LcdGridRenderer.COLOR_INK);

        if (metrics.landscape320) {
            drawLandscape320Static(graphics, metrics);
        } else if (metrics.widePanel) {
            drawWideStatic(graphics, metrics);
        } else {
            drawNarrowStatic(graphics, metrics);
        }
    }

    private void drawLandscape320Static(Graphics graphics,
                                        LayoutMetrics metrics) {
        int x = metrics.panelX;
        int y = metrics.panelY;
        int statsX = x + 72;

        drawSeparator(graphics, x + 3, y + 45, metrics.panelWidth - 7);
        LcdGridRenderer.drawPreviewGrid(graphics, x + 11, y + 67,
            4, 4, metrics.previewCell);

        textFont.draw(graphics, "SCORE", statsX, y + 51,
            LcdGridRenderer.COLOR_INK);
        textFont.draw(graphics, "SPEED", statsX, y + 91,
            LcdGridRenderer.COLOR_INK);
        textFont.draw(graphics, "LEVEL", statsX, y + 131,
            LcdGridRenderer.COLOR_INK);
        textFont.draw(graphics, "LIFE", statsX, y + 171,
            LcdGridRenderer.COLOR_INK);

        int railRight = metrics.statusX + metrics.statusWidth - 1;
        graphics.setColor(LcdGridRenderer.COLOR_SHADOW);
        graphics.drawLine(metrics.statusX, metrics.boardY + 67,
            railRight, metrics.boardY + 67);
        graphics.drawLine(metrics.statusX, metrics.boardY + 124,
            railRight, metrics.boardY + 124);

        textFont.drawCentered(graphics, "BAT", metrics.statusCenter,
            metrics.boardY + 8, LcdGridRenderer.COLOR_INK);
        textFont.drawCentered(graphics, "TIME", metrics.statusCenter,
            metrics.boardY + 77, LcdGridRenderer.COLOR_INK);
        textFont.drawCentered(graphics, "UP", metrics.statusCenter,
            metrics.boardY + 137, LcdGridRenderer.COLOR_INK);
    }

    private void drawWideStatic(Graphics graphics, LayoutMetrics metrics) {
        int x = metrics.panelX;
        int y = metrics.panelY;
        int statsX = x + 72;

        drawSeparator(graphics, x + 3, y + 41, metrics.panelWidth - 7);
        LcdGridRenderer.drawPreviewGrid(graphics, x + 11, y + 61,
            4, 4, metrics.previewCell);

        textFont.draw(graphics, "SCORE", statsX, y + 45,
            LcdGridRenderer.COLOR_INK);
        textFont.draw(graphics, "SPEED", statsX, y + 79,
            LcdGridRenderer.COLOR_INK);
        textFont.draw(graphics, "LEVEL", statsX, y + 112,
            LcdGridRenderer.COLOR_INK);
        textFont.draw(graphics, "LIFE", statsX, y + 145,
            LcdGridRenderer.COLOR_INK);
    }

    private void drawNarrowStatic(Graphics graphics, LayoutMetrics metrics) {
        int x = metrics.panelX;
        int y = metrics.panelY;
        int half = metrics.panelWidth / 2;

        drawSeparator(graphics, x + 2, y + 41, metrics.panelWidth - 5);
        LcdGridRenderer.drawPreviewGrid(graphics,
            metrics.panelCenter - metrics.previewCell * 2, y + 61,
            4, 4, metrics.previewCell);

        textFont.drawCentered(graphics, "SCORE", metrics.panelCenter,
            y + 102, LcdGridRenderer.COLOR_INK);
        textFont.drawCentered(graphics, "SPD", x + half / 2,
            y + 140, LcdGridRenderer.COLOR_INK);
        textFont.drawCentered(graphics, "LVL", x + half + half / 2,
            y + 140, LcdGridRenderer.COLOR_INK);
        textFont.drawCentered(graphics, "LIFE", metrics.panelCenter,
            y + 177, LcdGridRenderer.COLOR_INK);
        drawSeparator(graphics, x + 2, y + 211, metrics.panelWidth - 5);
    }

    private void drawDynamic(Graphics graphics, GameSnapshot snapshot,
                             LayoutMetrics metrics, long now) {
        LcdGridRenderer.drawActiveBoard(graphics, snapshot.board, metrics);
        textFont.drawCentered(graphics, snapshot.gameLabel,
            metrics.panelCenter, metrics.panelY +
                (metrics.landscape320 ? 15 : 14),
            LcdGridRenderer.COLOR_INK);
        textFont.drawCentered(graphics, snapshot.gameName,
            metrics.panelCenter, metrics.panelY +
                (metrics.landscape320 ? 29 : 27),
            LcdGridRenderer.COLOR_INK);

        if (metrics.landscape320) {
            drawLandscape320Dynamic(graphics, snapshot, metrics, now);
        } else if (metrics.widePanel) {
            drawWideDynamic(graphics, snapshot, metrics);
        } else {
            drawNarrowDynamic(graphics, snapshot, metrics);
        }
    }

    private void drawLandscape320Dynamic(Graphics graphics,
                                         GameSnapshot snapshot,
                                         LayoutMetrics metrics, long now) {
        int x = metrics.panelX;
        int y = metrics.panelY;
        int previewCenter = x + 11 + metrics.previewCell * 2;
        int statsX = x + 72;

        textFont.drawCentered(graphics, previewLabel(snapshot),
            previewCenter, y + 51, LcdGridRenderer.COLOR_INK);
        LcdGridRenderer.drawActivePreview(graphics, snapshot.preview,
            x + 11, y + 67, metrics.previewCell);

        numberFont.draw(graphics, snapshot.score, 6,
            statsX, y + 64, LcdGridRenderer.COLOR_INK, 1);
        numberFont.draw(graphics, snapshot.speed, 2,
            statsX, y + 104, LcdGridRenderer.COLOR_INK, 1);
        numberFont.draw(graphics, snapshot.level, 2,
            statsX, y + 144, LcdGridRenderer.COLOR_INK, 1);
        if (snapshot.usesLife) {
            numberFont.draw(graphics, snapshot.life, 2,
                statsX, y + 184, LcdGridRenderer.COLOR_INK, 1);
        } else {
            textFont.draw(graphics, "--", statsX, y + 185,
                LcdGridRenderer.COLOR_INK);
        }

        drawPauseStatus(graphics, snapshot, previewCenter, y);
        textFont.drawCentered(graphics, statusText(snapshot),
            previewCenter, y + 170, LcdGridRenderer.COLOR_INK);
        drawDeviceStatus(graphics, metrics, now);
    }

    private void drawWideDynamic(Graphics graphics, GameSnapshot snapshot,
                                 LayoutMetrics metrics) {
        int x = metrics.panelX;
        int y = metrics.panelY;
        int previewCenter = x + 11 + metrics.previewCell * 2;
        int statsX = x + 72;

        textFont.drawCentered(graphics, previewLabel(snapshot),
            previewCenter, y + 45, LcdGridRenderer.COLOR_INK);
        LcdGridRenderer.drawActivePreview(graphics, snapshot.preview,
            x + 11, y + 61, metrics.previewCell);

        numberFont.draw(graphics, snapshot.score, 6,
            statsX, y + 58, LcdGridRenderer.COLOR_INK, 1);
        numberFont.draw(graphics, snapshot.speed, 2,
            statsX, y + 92, LcdGridRenderer.COLOR_INK, 1);
        numberFont.draw(graphics, snapshot.level, 2,
            statsX, y + 125, LcdGridRenderer.COLOR_INK, 1);
        if (snapshot.usesLife) {
            numberFont.draw(graphics, snapshot.life, 2,
                statsX, y + 158, LcdGridRenderer.COLOR_INK, 1);
        } else {
            textFont.draw(graphics, "--", statsX, y + 159,
                LcdGridRenderer.COLOR_INK);
        }

        textFont.drawCentered(graphics, statusText(snapshot),
            previewCenter, y + 118, LcdGridRenderer.COLOR_INK);
    }

    private void drawNarrowDynamic(Graphics graphics, GameSnapshot snapshot,
                                   LayoutMetrics metrics) {
        int x = metrics.panelX;
        int y = metrics.panelY;
        int half = metrics.panelWidth / 2;
        int previewLeft = metrics.panelCenter - metrics.previewCell * 2;

        textFont.drawCentered(graphics, previewLabel(snapshot),
            metrics.panelCenter, y + 47, LcdGridRenderer.COLOR_INK);
        LcdGridRenderer.drawActivePreview(graphics, snapshot.preview,
            previewLeft, y + 61, metrics.previewCell);

        numberFont.drawCentered(graphics, snapshot.score, 6,
            metrics.panelCenter, y + 115,
            LcdGridRenderer.COLOR_INK, 1);
        numberFont.drawCentered(graphics, snapshot.speed, 2,
            x + half / 2, y + 153,
            LcdGridRenderer.COLOR_INK, 1);
        numberFont.drawCentered(graphics, snapshot.level, 2,
            x + half + half / 2, y + 153,
            LcdGridRenderer.COLOR_INK, 1);
        if (snapshot.usesLife) {
            numberFont.drawCentered(graphics, snapshot.life, 2,
                metrics.panelCenter, y + 190,
                LcdGridRenderer.COLOR_INK, 1);
        } else {
            textFont.drawCentered(graphics, "--", metrics.panelCenter,
                y + 191, LcdGridRenderer.COLOR_INK);
        }

        textFont.drawCentered(graphics, statusText(snapshot),
            metrics.panelCenter, y + 220,
            LcdGridRenderer.COLOR_INK);
    }

    private void drawPauseStatus(Graphics graphics,
                                 GameSnapshot snapshot,
                                 int centerX, int panelY) {
        BitmapAssets assets = statusAssets();
        if (assets == null) {
            textFont.drawCentered(graphics,
                snapshot.pause ? "PAUSED" : "PAUSE",
                centerX, panelY + 137, LcdGridRenderer.COLOR_INK);
            return;
        }

        Image pause = snapshot.pause ? assets.pauseOn : assets.pauseOff;
        if (pause != null) {
            graphics.drawImage(pause, centerX, panelY + 134,
                Graphics.TOP | Graphics.HCENTER);
        }
    }

    private void drawDeviceStatus(Graphics graphics, LayoutMetrics metrics,
                                  long now) {
        updateBattery(now);
        int batteryTop = metrics.boardY + 27;
        drawBattery(graphics, metrics.statusCenter - 9, batteryTop,
            batteryPercent);
        textFont.drawCentered(graphics, percentText(batteryPercent),
            metrics.statusCenter, metrics.boardY + 42,
            LcdGridRenderer.COLOR_INK);

        Calendar calendar = Calendar.getInstance();
        textFont.drawCentered(graphics,
            timeText(calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE)),
            metrics.statusCenter, metrics.boardY + 95,
            LcdGridRenderer.COLOR_INK);

        long elapsed = now - startedAt;
        if (elapsed < 0L) {
            elapsed = 0L;
        }
        int totalMinutes = (int) (elapsed / 60000L);
        int hours = totalMinutes / 60;
        if (hours > 99) {
            hours = 99;
        }
        textFont.drawCentered(graphics,
            timeText(hours, totalMinutes % 60),
            metrics.statusCenter, metrics.boardY + 155,
            LcdGridRenderer.COLOR_INK);
    }

    private static void drawBattery(Graphics graphics, int x, int y,
                                    int percent) {
        graphics.setColor(LcdGridRenderer.COLOR_SHADOW);
        graphics.drawRect(x, y, 16, 8);
        graphics.fillRect(x + 17, y + 2, 2, 5);
        if (percent < 0) {
            graphics.drawLine(x + 4, y + 4, x + 12, y + 4);
            return;
        }
        int fill = percent * 13 / 100;
        if (fill < 1 && percent > 0) {
            fill = 1;
        }
        graphics.setColor(LcdGridRenderer.COLOR_INK);
        graphics.fillRect(x + 2, y + 2, fill, 5);
    }

    private void updateBattery(long now) {
        if (now - batteryReadAt < BATTERY_REFRESH_MS) {
            return;
        }
        batteryReadAt = now;
        batteryPercent = -1;
        int i;
        for (i = 0; i < BATTERY_KEYS.length; i++) {
            String value;
            try {
                value = System.getProperty(BATTERY_KEYS[i]);
            } catch (Throwable ignored) {
                value = null;
            }
            int parsed = parsePercent(value);
            if (parsed >= 0) {
                batteryPercent = parsed;
                return;
            }
        }
    }

    private BitmapAssets statusAssets() {
        if (!statusAssetsAttempted) {
            statusAssetsAttempted = true;
            try {
                statusAssets = new BitmapAssets();
            } catch (Throwable ignored) {
                statusAssets = null;
            }
        }
        return statusAssets;
    }

    private static int parsePercent(String value) {
        if (value == null) {
            return -1;
        }
        int result = 0;
        boolean found = false;
        int i;
        for (i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= '0' && c <= '9') {
                found = true;
                result = result * 10 + c - '0';
                if (result > 100) {
                    return -1;
                }
            } else if (found) {
                break;
            }
        }
        return found ? result : -1;
    }

    private static String timeText(int first, int second) {
        StringBuffer text = new StringBuffer(5);
        if (first < 10) {
            text.append('0');
        }
        text.append(first);
        text.append(':');
        if (second < 10) {
            text.append('0');
        }
        text.append(second);
        return text.toString();
    }

    private static String percentText(int value) {
        if (value < 0) {
            return "--%";
        }
        StringBuffer text = new StringBuffer(4);
        text.append(value);
        text.append('%');
        return text.toString();
    }

    private static void drawSeparator(Graphics graphics, int x, int y,
                                      int width) {
        graphics.setColor(LcdGridRenderer.COLOR_SHADOW);
        graphics.drawLine(x, y, x + width - 1, y);
    }

    private static String statusText(GameSnapshot snapshot) {
        if (snapshot.pause) {
            return "PAUSED";
        }
        return snapshot.menu ? "READY" : "RUNNING";
    }

    private static String previewLabel(GameSnapshot snapshot) {
        if (snapshot.menu) {
            return "PREVIEW";
        }
        if (snapshot.usesNextPreview) {
            return "NEXT";
        }
        return snapshot.usesLife ? "LIFE" : "MODE";
    }
}
