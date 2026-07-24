package brickgame;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;

/** Orientation-aware MIDP renderer for 320x240 and 240x320 Nokia screens. */
final class BrickCanvas extends GameCanvas implements Runnable {

    private static final int FRAME_TIME = 20;

    private static final int COLOR_OUTER = 0x252B23;
    private static final int COLOR_LCD = 0x6D785C;
    private static final int COLOR_LCD_DARK = 0x61705B;
    private static final int COLOR_INK = 0x000000;
    private static final int COLOR_SHADOW = 0x272C23;

    private static final int BOARD_WIDTH = 110;
    private static final int BOARD_HEIGHT = 220;

    private final GameEngine engine = new GameEngine();
    private final GameEngine.Snapshot snapshot = new GameEngine.Snapshot();
    private final BitmapAssets assets = new BitmapAssets();
    private final BitmapFont font = new BitmapFont();

    private volatile boolean running;
    private Thread loop;

    BrickCanvas() {
        super(false);
        setFullScreenMode(true);
    }

    synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        loop = new Thread(this);
        loop.start();
    }

    synchronized void stop() {
        running = false;
        loop = null;
    }

    void setSystemPaused(boolean paused) {
        engine.setSystemPaused(paused);
    }

    public void run() {
        while (running) {
            long started = System.currentTimeMillis();
            engine.tick(started);
            drawFrame();
            long delay = FRAME_TIME - (System.currentTimeMillis() - started);
            if (delay < 1L) {
                delay = 1L;
            }
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void paint(Graphics graphics) {
        render(graphics);
    }

    private void drawFrame() {
        try {
            Graphics graphics = getGraphics();
            render(graphics);
            flushGraphics();
        } catch (Throwable ignored) {
            // Some devices reject active rendering while the MIDlet is hidden.
        }
    }

    private void render(Graphics graphics) {
        engine.copySnapshot(snapshot);
        int width = getWidth();
        int height = getHeight();

        graphics.setColor(COLOR_OUTER);
        graphics.fillRect(0, 0, width, height);

        if (width >= height && width >= 300 && height >= 230) {
            drawLandscape(graphics, width, height);
        } else if (width >= 230 && height >= 300) {
            drawPortrait(graphics, width, height);
        } else {
            drawCompact(graphics, width, height);
        }
    }

    private void drawLandscape(Graphics graphics, int width, int height) {
        int contentWidth = 312;
        int contentHeight = 232;
        int originX = (width - contentWidth) / 2;
        int originY = (height - contentHeight) / 2;
        int boardX = originX + 8;
        int boardY = originY + 6;
        int infoX = originX + 126;
        int infoY = originY + 6;
        int infoWidth = 180;

        drawCase(graphics, originX, originY, contentWidth, contentHeight);
        drawBoard(graphics, boardX, boardY);

        graphics.setColor(COLOR_SHADOW);
        graphics.drawLine(originX + 121, originY + 5,
            originX + 121, originY + contentHeight - 6);

        int center = infoX + infoWidth / 2;
        font.drawCentered(graphics, "BRICK GAME", center, infoY + 1, COLOR_INK);
        font.drawCentered(graphics, programLabel(), center, infoY + 14, COLOR_INK);
        font.drawCentered(graphics, snapshot.gameName, center, infoY + 27, COLOR_INK);
        graphics.setColor(COLOR_SHADOW);
        graphics.drawLine(infoX + 3, infoY + 41,
            infoX + infoWidth - 4, infoY + 41);

        int previewX = infoX + 9;
        int previewY = infoY + 57;
        font.drawCentered(graphics, previewLabel(), previewX + 22,
            infoY + 44, COLOR_INK);
        drawPreview(graphics, previewX, previewY);

        drawImage(graphics, snapshot.sound ? assets.soundOn : assets.soundOff,
            infoX + 23, infoY + 111);
        drawImage(graphics, snapshot.pause ? assets.pauseOn : assets.pauseOff,
            infoX + 11, infoY + 139);
        font.drawCentered(graphics, snapshot.sound ? "SOUND ON" : "SOUND OFF",
            previewX + 22, infoY + 158, COLOR_INK);

        int statsX = infoX + 70;
        font.draw(graphics, "SCORE", statsX, infoY + 44, COLOR_INK);
        drawNumber(graphics, snapshot.score, 6, statsX, infoY + 57);
        font.draw(graphics, "SPEED", statsX, infoY + 78, COLOR_INK);
        drawNumber(graphics, snapshot.speed, 2, statsX, infoY + 91);
        font.draw(graphics, "LEVEL", statsX, infoY + 111, COLOR_INK);
        drawNumber(graphics, snapshot.level, 2, statsX, infoY + 124);
        font.draw(graphics, "LIFE", statsX, infoY + 144, COLOR_INK);
        drawNumber(graphics, snapshot.life, 2, statsX, infoY + 157);

        graphics.setColor(COLOR_SHADOW);
        graphics.drawLine(infoX + 3, infoY + 177,
            infoX + infoWidth - 4, infoY + 177);
        drawLandscapeHelp(graphics, center, infoY + 181);
    }

    private void drawLandscapeHelp(Graphics graphics, int center, int top) {
        if (snapshot.menu) {
            font.drawCentered(graphics, "4/6 SELECT   5 START", center,
                top, COLOR_INK);
            font.drawCentered(graphics, "2 SPEED      8 LEVEL", center,
                top + 13, COLOR_INK);
            font.drawCentered(graphics, "* SOUND", center,
                top + 26, COLOR_INK);
        } else {
            font.drawCentered(graphics, "2/4/6/8 MOVE", center,
                top, COLOR_INK);
            font.drawCentered(graphics, "5 ACTION   0 PAUSE", center,
                top + 13, COLOR_INK);
            font.drawCentered(graphics, "# MENU     * SOUND", center,
                top + 26, COLOR_INK);
        }
    }

    private void drawPortrait(Graphics graphics, int width, int height) {
        int contentWidth = 232;
        int contentHeight = 312;
        int originX = (width - contentWidth) / 2;
        int originY = (height - contentHeight) / 2;
        int boardX = originX + 8;
        int boardY = originY + 6;
        int infoX = originX + 126;
        int infoY = originY + 6;
        int infoWidth = 100;

        drawCase(graphics, originX, originY, contentWidth, contentHeight);
        drawBoard(graphics, boardX, boardY);

        graphics.setColor(COLOR_SHADOW);
        graphics.drawLine(originX + 121, originY + 5,
            originX + 121, originY + 226);

        int center = infoX + infoWidth / 2;
        font.drawCentered(graphics, programLabel(), center, infoY + 1, COLOR_INK);
        font.drawCentered(graphics, snapshot.gameName, center, infoY + 14, COLOR_INK);

        font.drawCentered(graphics, previewLabel(), center, infoY + 29, COLOR_INK);
        drawPreview(graphics, infoX + 28, infoY + 42);

        font.drawCentered(graphics, "SCORE", center, infoY + 91, COLOR_INK);
        drawNumber(graphics, snapshot.score, 6, infoX + 23, infoY + 104);

        font.draw(graphics, "SPD", infoX + 5, infoY + 128, COLOR_INK);
        drawNumber(graphics, snapshot.speed, 2, infoX + 7, infoY + 141);
        font.draw(graphics, "LVL", infoX + 55, infoY + 128, COLOR_INK);
        drawNumber(graphics, snapshot.level, 2, infoX + 58, infoY + 141);

        font.drawCentered(graphics, "LIFE", center, infoY + 164, COLOR_INK);
        drawNumber(graphics, snapshot.life, 2, infoX + 41, infoY + 177);

        drawImage(graphics, snapshot.sound ? assets.soundOn : assets.soundOff,
            infoX + 10, infoY + 199);
        drawImage(graphics, snapshot.pause ? assets.pauseOn : assets.pauseOff,
            infoX + 43, infoY + 200);

        graphics.setColor(COLOR_SHADOW);
        graphics.drawLine(originX + 5, originY + 233,
            originX + contentWidth - 6, originY + 233);
        drawPortraitHelp(graphics, originX + contentWidth / 2, originY + 239);
    }

    private void drawPortraitHelp(Graphics graphics, int center, int top) {
        if (snapshot.menu) {
            font.drawCentered(graphics, "4/6 SELECT   5 START", center,
                top, COLOR_INK);
            font.drawCentered(graphics, "2 SPEED     8 LEVEL", center,
                top + 15, COLOR_INK);
            font.drawCentered(graphics, "* SOUND", center,
                top + 30, COLOR_INK);
        } else {
            font.drawCentered(graphics, "2/4/6/8 MOVE   5 ACTION", center,
                top, COLOR_INK);
            font.drawCentered(graphics, "0 PAUSE     # MENU", center,
                top + 15, COLOR_INK);
            font.drawCentered(graphics, "* SOUND", center,
                top + 30, COLOR_INK);
        }
    }

    private void drawCompact(Graphics graphics, int width, int height) {
        int sideWidth = 62;
        int cellByHeight = (height - 4) / 20;
        int cellByWidth = (width - sideWidth - 6) / 10;
        int cell = cellByHeight < cellByWidth ? cellByHeight : cellByWidth;
        if (cell < 3) {
            cell = 3;
        }
        int boardWidth = cell * 10;
        int boardHeight = cell * 20;
        int left = (width - boardWidth - sideWidth - 4) / 2;
        int top = (height - boardHeight) / 2;
        if (left < 1) {
            left = 1;
        }
        if (top < 1) {
            top = 1;
        }

        graphics.setColor(COLOR_LCD);
        graphics.fillRect(left - 1, top - 1, boardWidth + sideWidth + 6,
            boardHeight + 2);
        drawScaledCells(graphics, snapshot.board, 10, 20, left, top, cell);
        int panelX = left + boardWidth + 4;
        font.drawCentered(graphics, programLabel(), panelX + sideWidth / 2,
            top + 2, COLOR_INK);
        font.drawCentered(graphics, snapshot.gameName, panelX + sideWidth / 2,
            top + 15, COLOR_INK);
        font.drawCentered(graphics, "SCORE", panelX + sideWidth / 2,
            top + 32, COLOR_INK);
        drawNumber(graphics, snapshot.score, 6, panelX + 4, top + 45);
        font.drawCentered(graphics, "5 ACTION", panelX + sideWidth / 2,
            top + boardHeight - 14, COLOR_INK);
    }

    private static void drawCase(Graphics graphics, int x, int y,
                                 int width, int height) {
        graphics.setColor(COLOR_SHADOW);
        graphics.fillRect(x, y, width, height);
        graphics.setColor(COLOR_LCD);
        graphics.fillRect(x + 2, y + 2, width - 4, height - 4);
        graphics.setColor(COLOR_LCD_DARK);
        graphics.drawRect(x + 3, y + 3, width - 7, height - 7);
    }

    private void drawBoard(Graphics graphics, int left, int top) {
        graphics.setColor(COLOR_INK);
        graphics.drawRect(left - 1, top - 1, BOARD_WIDTH, BOARD_HEIGHT);
        drawCells(graphics, snapshot.board, 10, 20, left, top);
    }

    private void drawPreview(Graphics graphics, int left, int top) {
        graphics.setColor(COLOR_SHADOW);
        graphics.drawRect(left - 2, top - 2, 45, 45);
        drawCells(graphics, snapshot.preview, 4, 4, left, top);
    }

    private static void drawCells(Graphics graphics, boolean[][] values,
                                  int width, int height, int left, int top) {
        int y;
        int x;
        for (y = 0; y < height; y++) {
            for (x = 0; x < width; x++) {
                graphics.setColor(values[y][x] ? COLOR_INK : COLOR_LCD_DARK);
                int px = left + x * 11;
                int py = top + y * 11;
                graphics.drawRect(px, py, 9, 9);
                graphics.fillRect(px + 3, py + 3, 4, 4);
            }
        }
    }

    private static void drawScaledCells(Graphics graphics, boolean[][] values,
                                        int width, int height, int left,
                                        int top, int cell) {
        int inset = cell > 5 ? 1 : 0;
        int y;
        int x;
        for (y = 0; y < height; y++) {
            for (x = 0; x < width; x++) {
                int px = left + x * cell;
                int py = top + y * cell;
                graphics.setColor(values[y][x] ? COLOR_INK : COLOR_LCD_DARK);
                graphics.drawRect(px + inset, py + inset,
                    cell - inset - 1, cell - inset - 1);
                if (cell >= 5) {
                    int core = cell / 3;
                    if (core < 2) {
                        core = 2;
                    }
                    graphics.fillRect(px + (cell - core) / 2,
                        py + (cell - core) / 2, core, core);
                }
            }
        }
    }

    private void drawNumber(Graphics graphics, int value, int capacity,
                            int left, int top) {
        int divisor = 1;
        int i;
        for (i = 1; i < capacity; i++) {
            divisor *= 10;
        }
        for (i = 0; i < capacity; i++) {
            int digit = (value / divisor) % 10;
            if (digit < 0) {
                digit = -digit;
            }
            drawImage(graphics, assets.digits[digit], left + i * 9, top);
            if (divisor > 1) {
                divisor /= 10;
            }
        }
    }

    private String programLabel() {
        StringBuffer value = new StringBuffer();
        value.append(snapshot.gameLetter);
        value.append('-');
        if (snapshot.gameCode < 10) {
            value.append('0');
        }
        value.append(snapshot.gameCode);
        return value.toString();
    }

    private String previewLabel() {
        if (snapshot.menu) {
            return "PREVIEW";
        }
        if (snapshot.gameIndex == 13) {
            return "NEXT";
        }
        return "LIFE";
    }

    private static void drawImage(Graphics graphics, Image image, int x, int y) {
        if (image != null) {
            graphics.drawImage(image, x, y, Graphics.TOP | Graphics.LEFT);
        }
    }

    protected void keyPressed(int keyCode) {
        int action = mapKey(keyCode);
        if (action != GameEngine.ACTION_NONE) {
            engine.press(action);
        }
    }

    protected void keyRepeated(int keyCode) {
        int action = mapKey(keyCode);
        if (action >= GameEngine.ACTION_UP && action <= GameEngine.ACTION_FIRE) {
            engine.press(action);
        }
    }

    private int mapKey(int keyCode) {
        if (keyCode == KEY_NUM2) {
            return GameEngine.ACTION_UP;
        }
        if (keyCode == KEY_NUM8) {
            return GameEngine.ACTION_DOWN;
        }
        if (keyCode == KEY_NUM4) {
            return GameEngine.ACTION_LEFT;
        }
        if (keyCode == KEY_NUM6) {
            return GameEngine.ACTION_RIGHT;
        }
        if (keyCode == KEY_NUM5) {
            return GameEngine.ACTION_FIRE;
        }
        if (keyCode == KEY_NUM0 || keyCode == -6) {
            return GameEngine.ACTION_PAUSE;
        }
        if (keyCode == KEY_STAR) {
            return GameEngine.ACTION_SOUND;
        }
        if (keyCode == KEY_POUND || keyCode == -7) {
            return GameEngine.ACTION_MENU;
        }
        try {
            int gameAction = getGameAction(keyCode);
            if (gameAction == UP) {
                return GameEngine.ACTION_UP;
            }
            if (gameAction == DOWN) {
                return GameEngine.ACTION_DOWN;
            }
            if (gameAction == LEFT) {
                return GameEngine.ACTION_LEFT;
            }
            if (gameAction == RIGHT) {
                return GameEngine.ACTION_RIGHT;
            }
            if (gameAction == FIRE) {
                return GameEngine.ACTION_FIRE;
            }
        } catch (IllegalArgumentException ignored) {
        }
        return GameEngine.ACTION_NONE;
    }
}
