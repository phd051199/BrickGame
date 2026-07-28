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
                previewLeft(layout), previewTop(layout), layout.previewCell);
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

        if (isE72(metrics)) {
            drawE72Static(graphics, metrics);
            return;
        }

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
                previewTop(metrics), metrics.previewCell);

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
    }

    private void drawE72Static(Graphics graphics, LayoutMetrics metrics) {
        boldFont.drawString(graphics, "BRICK GAME", metrics.panelCenter,
                metrics.panelY + 16, Graphics.HCENTER,
                LcdGridRenderer.COLOR_INK);

        graphics.setColor(LcdGridRenderer.COLOR_SHADOW);
        graphics.drawLine(metrics.panelX + 4, metrics.panelY + 53,
                metrics.panelX + metrics.panelWidth - 5, metrics.panelY + 53);

        int previewCenter = previewCenter(metrics);
        int statsCenter = statsCenter(metrics);
        regularFont.drawString(graphics, "NEXT", previewCenter,
                metrics.panelY + 69, Graphics.HCENTER,
                LcdGridRenderer.COLOR_INK);
        LcdGridRenderer.drawPreviewGrid(graphics, previewLeft(metrics),
                previewTop(metrics), metrics.previewCell);
        regularFont.drawString(graphics, "SCORE", statsCenter,
                metrics.panelY + 69, Graphics.HCENTER,
                LcdGridRenderer.COLOR_INK);
        regularFont.drawString(graphics, "SPEED", statsCenter,
                metrics.panelY + 112, Graphics.HCENTER,
                LcdGridRenderer.COLOR_INK);
        regularFont.drawString(graphics, "LEVEL", statsCenter,
                metrics.panelY + 155, Graphics.HCENTER,
                LcdGridRenderer.COLOR_INK);

        graphics.setColor(LcdGridRenderer.COLOR_SHADOW);
        graphics.drawLine(metrics.panelX + 4, metrics.panelY + 184,
                metrics.panelX + metrics.panelWidth - 5, metrics.panelY + 184);
        regularFont.drawString(graphics, "LSK PAUSE", metrics.panelCenter,
                metrics.panelY + 203, Graphics.HCENTER,
                LcdGridRenderer.COLOR_INK);
        regularFont.drawString(graphics, "RSK MENU", metrics.panelCenter,
                metrics.panelY + 220, Graphics.HCENTER,
                LcdGridRenderer.COLOR_INK);

        int railLeft = metrics.statusX + 4;
        int railRight = metrics.statusX + metrics.statusWidth - 5;
        graphics.setColor(LcdGridRenderer.COLOR_SHADOW);
        graphics.drawLine(railLeft, metrics.panelY + 76,
                railRight, metrics.panelY + 76);
        graphics.drawLine(railLeft, metrics.panelY + 156,
                railRight, metrics.panelY + 156);
        regularFont.drawString(graphics, "BAT", metrics.statusCenter,
                metrics.panelY + 15, Graphics.HCENTER,
                LcdGridRenderer.COLOR_INK);
        regularFont.drawString(graphics, "TIME", metrics.statusCenter,
                metrics.panelY + 95, Graphics.HCENTER,
                LcdGridRenderer.COLOR_INK);
        regularFont.drawString(graphics, "UP", metrics.statusCenter,
                metrics.panelY + 175, Graphics.HCENTER,
                LcdGridRenderer.COLOR_INK);
    }

    private void drawPanel(Graphics graphics, MachineProfile profile,
            BrickCpu cpu, boolean paused) {
        if (isE72(layout)) {
            drawE72Panel(graphics, profile, cpu, paused);
            return;
        }

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

    private void drawE72Panel(Graphics graphics, MachineProfile profile,
            BrickCpu cpu, boolean paused) {
        regularFont.drawString(graphics, coreName(profile.coreType),
                layout.panelCenter, layout.panelY + 31,
                Graphics.HCENTER, LcdGridRenderer.COLOR_INK);
        drawFitted(graphics, profile.name, layout.panelCenter,
                layout.panelY + 46, layout.panelWidth - 8, false);

        byte[] vram = cpu.vram();
        int statsCenter = statsCenter(layout);
        int score = LcdHudDecoder.score(profile, vram);
        int scoreDigits = LcdHudDecoder.scoreDigits(profile);
        drawValueCentered(graphics, score, scoreDigits, statsCenter,
                layout.panelY + 76);
        drawValueCentered(graphics, LcdHudDecoder.speed(profile, vram), 2,
                statsCenter, layout.panelY + 119);
        drawValueCentered(graphics, LcdHudDecoder.level(profile, vram), 2,
                statsCenter, layout.panelY + 162);

        regularFont.drawString(graphics, paused ? "PAUSED" : "RUNNING",
                previewCenter(layout), layout.panelY + 139,
                Graphics.HCENTER, LcdGridRenderer.COLOR_INK);
    }

    private static boolean isE72(LayoutMetrics metrics) {
        return metrics.screenWidth == 320 && metrics.screenHeight == 240;
    }

    private static int previewCenter(LayoutMetrics metrics) {
        if (isE72(metrics)) {
            return metrics.panelX + metrics.panelWidth / 4;
        }
        return metrics.panelX + 6 + metrics.previewCell * 2;
    }

    private static int previewLeft(LayoutMetrics metrics) {
        return previewCenter(metrics) - metrics.previewCell * 2;
    }

    private static int previewTop(LayoutMetrics metrics) {
        return metrics.panelY + (isE72(metrics) ? 76 : 70);
    }

    private int statsCenter(LayoutMetrics metrics) {
        int center = metrics.panelX + metrics.panelWidth * 3 / 4;
        int maximum = metrics.panelX + metrics.panelWidth - 4
                - numberFont.width(6) / 2;
        return center < maximum ? center : maximum;
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

    private void drawValueCentered(Graphics graphics, int value, int digits,
            int center, int y) {
        if (value < 0 || digits <= 0) {
            regularFont.drawString(graphics, "--", center, y + 10,
                    Graphics.HCENTER, LcdGridRenderer.COLOR_INK);
            return;
        }
        numberFont.drawCentered(graphics, value, digits, center, y,
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
        if (batteryPercent < 0) {
            regularFont.drawString(graphics, "--", layout.statusCenter,
                    layout.panelY + 32, Graphics.HCENTER,
                    LcdGridRenderer.COLOR_INK);
        } else {
            int batteryDigits = batteryPercent >= 100 ? 3 : 2;
            numberFont.drawCentered(graphics, batteryPercent, batteryDigits,
                    layout.statusCenter, layout.panelY + 22,
                    LcdGridRenderer.COLOR_INK);
        }

        Calendar calendar = Calendar.getInstance();
        numberFont.drawClockCentered(graphics,
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE), layout.statusCenter,
                layout.panelY + 102, LcdGridRenderer.COLOR_INK);

        long elapsed = now - startedAt;
        if (elapsed < 0L) {
            elapsed = 0L;
        }
        int minutes = (int) (elapsed / 60000L);
        int hours = minutes / 60;
        if (hours > 99) {
            hours = 99;
        }
        numberFont.drawClockCentered(graphics, hours, minutes % 60,
                layout.statusCenter, layout.panelY + 182,
                LcdGridRenderer.COLOR_INK);
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
