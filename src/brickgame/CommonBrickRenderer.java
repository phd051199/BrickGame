package brickgame;

import java.util.Calendar;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

final class CommonBrickRenderer {
    private static final long BATTERY_REFRESH_MS = 30000L;
    private static final String[] BATTERY_KEYS = {
        "com.nokia.mid.batterylevel",
        "com.sonyericsson.battery.level",
        "com.samsung.battery.level",
        "battery.level"
    };

    private final BitmapFont regularFont;
    private final BitmapFont boldFont;
    private final BrickNumberFont numberFont = new BrickNumberFont();
    private final short[] board = new short[20];
    private final byte[] preview = new byte[4];
    private final long startedAt = System.currentTimeMillis();

    private Image base;
    private LayoutMetrics layout;
    private int baseWidth = -1;
    private int baseHeight = -1;
    private String baseProfileId;
    private long batteryReadAt = -BATTERY_REFRESH_MS;
    private int batteryPercent = -1;

    CommonBrickRenderer(BitmapFont regularFont, BitmapFont boldFont) {
        this.regularFont = regularFont;
        this.boldFont = boldFont;
    }

    void invalidate() {
        base = null;
        layout = null;
        baseWidth = -1;
        baseHeight = -1;
        baseProfileId = null;
    }

    void render(Graphics graphics, MachineProfile profile, BrickCpu cpu,
            CommonLcdMap map, boolean paused, int width, int height) {
        ensureBase(profile, width, height);
        if (base != null) {
            graphics.drawImage(base, 0, 0, Graphics.TOP | Graphics.LEFT);
        } else {
            drawStatic(graphics, layout);
        }

        map.decode(cpu.vram(), cpu.displayEnabled(), board, preview);
        LcdGridRenderer.drawActiveBoard(graphics, board, layout);
        LcdGridRenderer.drawActivePreview(graphics, preview,
                previewLeft(layout), layout.panelY + 70, layout.previewCell);
        drawPanel(graphics, profile, cpu, paused);
        if (width == 320 && height == 240) {
            drawDeviceStatus(graphics, System.currentTimeMillis());
        }
    }

    private void ensureBase(MachineProfile profile, int width, int height) {
        if (layout != null && width == baseWidth && height == baseHeight
                && profile.id.equals(baseProfileId)) {
            return;
        }
        layout = new LayoutMetrics(profile, width, height);
        baseWidth = width;
        baseHeight = height;
        baseProfileId = profile.id;
        base = null;
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

        regularFont.drawString(graphics, "BRICK GAME", metrics.panelX + 4,
                metrics.panelY + 11, Graphics.LEFT,
                LcdGridRenderer.COLOR_INK);

        graphics.setColor(LcdGridRenderer.COLOR_SHADOW);
        graphics.drawLine(metrics.panelX + 3, metrics.panelY + 47,
                metrics.panelX + metrics.panelWidth - 4, metrics.panelY + 47);

        int previewLeft = previewLeft(metrics);
        int previewCenter = previewLeft + metrics.previewCell * 2;
        regularFont.drawString(graphics, "NEXT", previewCenter,
                metrics.panelY + 62, Graphics.HCENTER,
                LcdGridRenderer.COLOR_INK);
        LcdGridRenderer.drawPreviewGrid(graphics, previewLeft,
                metrics.panelY + 70, metrics.previewCell);

        int statsX = statsX(metrics);
        regularFont.drawString(graphics, "SCORE", statsX,
                metrics.panelY + 62, Graphics.LEFT,
                LcdGridRenderer.COLOR_INK);
        regularFont.drawString(graphics, "SPEED", statsX,
                metrics.panelY + 104, Graphics.LEFT,
                LcdGridRenderer.COLOR_INK);
        regularFont.drawString(graphics, "LEVEL", statsX,
                metrics.panelY + 146, Graphics.LEFT,
                LcdGridRenderer.COLOR_INK);

        if (metrics.screenWidth == 320 && metrics.screenHeight == 240) {
            int railRight = metrics.statusX + metrics.statusWidth - 1;
            graphics.setColor(LcdGridRenderer.COLOR_SHADOW);
            graphics.drawLine(metrics.statusX, metrics.boardY + 63,
                    railRight, metrics.boardY + 63);
            graphics.drawLine(metrics.statusX, metrics.boardY + 121,
                    railRight, metrics.boardY + 121);
            regularFont.drawString(graphics, "BAT", metrics.statusCenter,
                    metrics.boardY + 11, Graphics.HCENTER,
                    LcdGridRenderer.COLOR_INK);
            regularFont.drawString(graphics, "TIME", metrics.statusCenter,
                    metrics.boardY + 76, Graphics.HCENTER,
                    LcdGridRenderer.COLOR_INK);
            regularFont.drawString(graphics, "UP", metrics.statusCenter,
                    metrics.boardY + 137, Graphics.HCENTER,
                    LcdGridRenderer.COLOR_INK);
        }
    }

    private void drawPanel(Graphics graphics, MachineProfile profile,
            BrickCpu cpu, boolean paused) {
        regularFont.drawString(graphics, coreName(profile.coreType),
                layout.panelX + layout.panelWidth - 4, layout.panelY + 11,
                Graphics.RIGHT, LcdGridRenderer.COLOR_INK);

        int split = splitName(profile.name);
        if (split > 0) {
            drawFitted(graphics, profile.name.substring(0, split),
                    layout.panelCenter, layout.panelY + 25,
                    layout.panelWidth - 8, true);
            drawFitted(graphics, profile.name.substring(split + 1),
                    layout.panelCenter, layout.panelY + 37,
                    layout.panelWidth - 8, false);
        } else {
            drawFitted(graphics, profile.name, layout.panelCenter,
                    layout.panelY + 31, layout.panelWidth - 8, true);
        }

        byte[] vram = cpu.vram();
        int statsX = statsX(layout);
        int score = LcdHudDecoder.score(profile, vram);
        int scoreDigits = LcdHudDecoder.scoreDigits(profile);
        drawValue(graphics, score, scoreDigits, statsX, layout.panelY + 67);
        drawValue(graphics, LcdHudDecoder.speed(profile, vram), 2,
                statsX, layout.panelY + 109);
        drawValue(graphics, LcdHudDecoder.level(profile, vram), 2,
                statsX, layout.panelY + 151);

        int previewCenter = previewLeft(layout) + layout.previewCell * 2;
        boldFont.drawString(graphics, paused ? "PAUSED" : "RUNNING",
                previewCenter, layout.panelY + 137, Graphics.HCENTER,
                LcdGridRenderer.COLOR_INK);
    }

    private static int previewLeft(LayoutMetrics metrics) {
        return metrics.panelX + 6;
    }

    private int statsX(LayoutMetrics metrics) {
        return metrics.panelX + metrics.panelWidth - numberFont.width(6) - 4;
    }

    private void drawValue(Graphics graphics, int value, int digits,
            int x, int y) {
        if (value < 0 || digits <= 0) {
            regularFont.drawString(graphics, "--", x, y + 10,
                    Graphics.LEFT, LcdGridRenderer.COLOR_INK);
            return;
        }
        numberFont.draw(graphics, value, digits, x, y,
                LcdGridRenderer.COLOR_INK);
    }

    private void drawFitted(Graphics graphics, String text, int center,
            int baseline, int maxWidth, boolean bold) {
        BitmapFont font = bold ? boldFont : regularFont;
        String value = text;
        while (value.length() > 1 && font.stringWidth(value) > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        font.drawString(graphics, value, center, baseline,
                Graphics.HCENTER, LcdGridRenderer.COLOR_INK);
    }

    private static int splitName(String value) {
        if (value == null || value.length() < 14) {
            return -1;
        }
        int middle = value.length() / 2;
        int left = value.lastIndexOf(' ', middle);
        int right = value.indexOf(' ', middle);
        if (left < 0) {
            return right;
        }
        if (right < 0) {
            return left;
        }
        return middle - left <= right - middle ? left : right;
    }

    private void drawDeviceStatus(Graphics graphics, long now) {
        updateBattery(now);
        int batteryTop = layout.boardY + 27;
        drawBattery(graphics, layout.statusCenter - 9, batteryTop, batteryPercent);
        regularFont.drawString(graphics, batteryText(batteryPercent),
                layout.statusCenter, layout.boardY + 54,
                Graphics.HCENTER, LcdGridRenderer.COLOR_INK);

        Calendar calendar = Calendar.getInstance();
        regularFont.drawString(graphics,
                timeText(calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE)),
                layout.statusCenter, layout.boardY + 102,
                Graphics.HCENTER, LcdGridRenderer.COLOR_INK);

        long elapsed = now - startedAt;
        if (elapsed < 0L) {
            elapsed = 0L;
        }
        int minutes = (int) (elapsed / 60000L);
        int hours = minutes / 60;
        if (hours > 99) {
            hours = 99;
        }
        regularFont.drawString(graphics, timeText(hours, minutes % 60),
                layout.statusCenter, layout.boardY + 162,
                Graphics.HCENTER, LcdGridRenderer.COLOR_INK);
    }

    private static void drawBattery(Graphics graphics, int x, int y, int percent) {
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
        for (int i = 0; i < BATTERY_KEYS.length; i++) {
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

    private static String batteryText(int value) {
        return value < 0 ? "--" : String.valueOf(value);
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

    private static String coreName(int coreType) {
        switch (coreType) {
            case MachineProfile.CORE_HT943:
                return "HT943";
            case MachineProfile.CORE_SPL02:
                return "SPL02";
            case MachineProfile.CORE_SPL03:
                return "SPL03";
            case MachineProfile.CORE_EM73000:
                return "EM73000";
            case MachineProfile.CORE_E0C6200:
                return "E0C6200";
            case MachineProfile.CORE_KS56:
                return "KS56";
            default:
                return "CORE";
        }
    }
}
