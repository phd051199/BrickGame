package e23;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

final class E23Canvas extends GameCanvas implements Runnable {
    private static final int LOADING = 0;
    private static final int RUNNING = 1;
    private static final int PAUSED = 2;
    private static final int ERROR = 3;
    private static final int FRAME_MS = 33;
    private static final int MAX_ELAPSED_MS = 100;
    private static final int CPU_SLICE = 16384;
    private static final long CLOCK_HZ = 1000000L;

    private final E23Midlet app;
    private volatile boolean active;
    private volatile int mode = LOADING;
    private volatile boolean redraw = true;
    private Thread loop;
    private String errorText = "";
    private E23Cpu cpu;
    private E23LcdMap lcdMap;
    private E23Renderer renderer;
    private byte[] previousLcd;
    private boolean previousLcdEnabled;
    private long previousCpuTime;
    private long cycleFraction;
    private int cycleOvershoot;

    E23Canvas(E23Midlet app) {
        super(false);
        this.app = app;
        setFullScreenMode(true);
    }

    synchronized void start() {
        if (active) {
            return;
        }
        active = true;
        if (mode == RUNNING) {
            previousCpuTime = System.currentTimeMillis();
        }
        loop = new Thread(this);
        loop.start();
    }

    synchronized void stop() {
        active = false;
        loop = null;
        releaseButtons();
    }

    public void run() {
        while (active) {
            long frameStart = System.currentTimeMillis();
            int currentMode = mode;
            if (currentMode == LOADING) {
                drawLoading();
                loadGame();
            } else if (currentMode == RUNNING) {
                if (runFrame(frameStart) || redraw
                        || renderer != null && renderer.countdownActive(frameStart)) {
                    drawGame(false);
                    redraw = false;
                }
            } else if (currentMode == PAUSED) {
                if (redraw) {
                    drawGame(true);
                    redraw = false;
                }
            } else if (redraw) {
                drawError();
                redraw = false;
            }

            long elapsed = System.currentTimeMillis() - frameStart;
            long sleep = (currentMode == RUNNING ? FRAME_MS : 80) - elapsed;
            if (sleep < 1) {
                sleep = 1;
            }
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void paint(Graphics graphics) {
        if (mode == RUNNING || mode == PAUSED) {
            drawGame(graphics, mode == PAUSED);
        } else if (mode == LOADING) {
            drawLoading(graphics);
        } else {
            drawError(graphics);
        }
    }

    private boolean runFrame(long now) {
        if (cpu == null) {
            return false;
        }
        long elapsed = now - previousCpuTime;
        if (elapsed < 0) {
            elapsed = 0;
        } else if (elapsed > MAX_ELAPSED_MS) {
            elapsed = FRAME_MS;
        }
        previousCpuTime = now;

        cycleFraction += elapsed * CLOCK_HZ;
        int budget = (int) (cycleFraction / 1000L);
        cycleFraction %= 1000L;

        if (cycleOvershoot > 0) {
            budget -= cycleOvershoot;
            if (budget < 0) {
                cycleOvershoot = -budget;
                budget = 0;
            } else {
                cycleOvershoot = 0;
            }
        }

        while (budget > 0) {
            int slice = budget > CPU_SLICE ? CPU_SLICE : budget;
            int consumed = cpu.runCycles(slice);
            if (consumed <= 0) {
                break;
            }
            budget -= consumed;
        }
        if (budget < 0) {
            cycleOvershoot = -budget;
        }
        return lcdChanged();
    }

    private boolean lcdChanged() {
        byte[] current = cpu.lcdRam();
        boolean enabled = cpu.lcdEnabled();
        if (previousLcd == null) {
            previousLcd = new byte[current.length];
            for (int i = 0; i < current.length; i++) {
                previousLcd[i] = current[i];
            }
            previousLcdEnabled = enabled;
            return true;
        }
        boolean changed = enabled != previousLcdEnabled;
        previousLcdEnabled = enabled;
        for (int i = 0; i < current.length; i++) {
            byte value = current[i];
            if (previousLcd[i] != value) {
                previousLcd[i] = value;
                changed = true;
            }
        }
        return changed;
    }

    private void loadGame() {
        try {
            cpu = new E23Cpu(E23Assets.read("/e23/program.bin"));
            lcdMap = E23LcdMap.load("/e23/display.map");
            renderer = new E23Renderer(
                    BitmapFont.load("/e23/regular.bmf"),
                    BitmapFont.load("/e23/bold.bmf"));
            previousLcd = null;
            previousLcdEnabled = !cpu.lcdEnabled();
            lcdChanged();
            previousCpuTime = System.currentTimeMillis();
            cycleFraction = 0;
            cycleOvershoot = 0;
            redraw = true;
            mode = RUNNING;
        } catch (Throwable error) {
            errorText = error.toString();
            redraw = true;
            mode = ERROR;
        }
    }

    private void retry() {
        releaseButtons();
        cpu = null;
        lcdMap = null;
        renderer = null;
        errorText = "";
        redraw = true;
        mode = LOADING;
    }

    private void drawGame(boolean paused) {
        try {
            Graphics graphics = getGraphics();
            drawGame(graphics, paused);
            flushGraphics();
        } catch (Throwable ignored) {
        }
    }

    private void drawGame(Graphics graphics, boolean paused) {
        graphics.setClip(0, 0, getWidth(), getHeight());
        if (renderer == null || lcdMap == null || cpu == null) {
            graphics.setColor(E23Renderer.LCD_COLOR);
            graphics.fillRect(0, 0, getWidth(), getHeight());
            return;
        }
        renderer.draw(graphics, cpu, lcdMap, paused, getWidth(), getHeight());
    }

    private void drawLoading() {
        try {
            Graphics graphics = getGraphics();
            drawLoading(graphics);
            flushGraphics();
        } catch (Throwable ignored) {
        }
    }

    private void drawLoading(Graphics graphics) {
        int width = getWidth();
        int height = getHeight();
        graphics.setClip(0, 0, width, height);
        graphics.setColor(E23Renderer.LCD_COLOR);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(E23Renderer.INK_COLOR);
        graphics.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        centerText(graphics, "E23 96 IN 1", width, height / 2 - 22);
        graphics.setFont(Font.getDefaultFont());
        centerText(graphics, "LOADING", width, height / 2 + 4);
    }

    private void drawError() {
        try {
            Graphics graphics = getGraphics();
            drawError(graphics);
            flushGraphics();
        } catch (Throwable ignored) {
        }
    }

    private void drawError(Graphics graphics) {
        int width = getWidth();
        int height = getHeight();
        graphics.setClip(0, 0, width, height);
        graphics.setColor(E23Renderer.LCD_COLOR);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(E23Renderer.INK_COLOR);
        graphics.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        centerText(graphics, "E23 LOAD FAILED", width, 38);
        graphics.setFont(Font.getDefaultFont());
        drawWrapped(graphics, errorText, 12, 72, width - 24, 6);
        centerText(graphics, "ANY KEY: RETRY", width, height - 24);
    }

    private static void drawWrapped(Graphics graphics, String text, int x,
            int top, int maxWidth, int maxLines) {
        if (text == null) {
            return;
        }
        String remaining = text.trim();
        int y = top;
        Font font = graphics.getFont();
        for (int line = 0; line < maxLines && remaining.length() > 0; line++) {
            int split = remaining.length();
            while (split > 1 && font.stringWidth(remaining.substring(0, split)) > maxWidth) {
                split--;
            }
            int space = remaining.lastIndexOf(' ', split - 1);
            if (space > 0) {
                split = space;
            }
            graphics.drawString(remaining.substring(0, split).trim(), x, y,
                    Graphics.TOP | Graphics.LEFT);
            remaining = remaining.substring(split).trim();
            y += font.getHeight() + 2;
        }
    }

    private static void centerText(Graphics graphics, String text, int width, int y) {
        graphics.drawString(text, width / 2, y, Graphics.TOP | Graphics.HCENTER);
    }

    protected void keyPressed(int keyCode) {
        if (mode == ERROR) {
            retry();
            return;
        }
        if (mode == LOADING) {
            return;
        }
        if (mode == PAUSED) {
            if (isRightSoftKey(keyCode)) {
                app.exit();
            } else {
                previousCpuTime = System.currentTimeMillis();
                redraw = true;
                mode = RUNNING;
            }
            return;
        }
        if (isLeftSoftKey(keyCode) || keyCode == KEY_NUM0
                || keyCode == KEY_POUND) {
            releaseButtons();
            redraw = true;
            mode = PAUSED;
            return;
        }
        if (isRightSoftKey(keyCode)) {
            app.exit();
            return;
        }
        int button = mapKey(keyCode);
        if (button >= 0 && cpu != null) {
            if (button == E23Cpu.BUTTON_START && renderer != null) {
                renderer.restartCountdown();
                redraw = true;
            }
            cpu.setButton(button, true);
        }
    }

    protected void keyReleased(int keyCode) {
        if (mode != RUNNING || cpu == null) {
            return;
        }
        int button = mapKey(keyCode);
        if (button >= 0) {
            cpu.setButton(button, false);
        }
    }

    private int mapKey(int keyCode) {
        if (keyCode == KEY_NUM4) {
            return E23Cpu.BUTTON_LEFT;
        }
        if (keyCode == KEY_NUM6) {
            return E23Cpu.BUTTON_RIGHT;
        }
        if (keyCode == KEY_NUM8) {
            return E23Cpu.BUTTON_DOWN;
        }
        if (keyCode == KEY_NUM2 || keyCode == KEY_NUM5) {
            return E23Cpu.BUTTON_ROTATE;
        }
        if (keyCode == KEY_NUM1) {
            return E23Cpu.BUTTON_START;
        }
        if (keyCode == KEY_NUM3) {
            return E23Cpu.BUTTON_AUX;
        }
        if (keyCode == KEY_NUM7) {
            return E23Cpu.BUTTON_OPTION;
        }
        if (keyCode == KEY_STAR) {
            return E23Cpu.BUTTON_RESET;
        }

        int action = gameAction(keyCode);
        if (action == LEFT) {
            return E23Cpu.BUTTON_LEFT;
        }
        if (action == RIGHT) {
            return E23Cpu.BUTTON_RIGHT;
        }
        if (action == DOWN) {
            return E23Cpu.BUTTON_DOWN;
        }
        if (action == UP || action == FIRE) {
            return E23Cpu.BUTTON_ROTATE;
        }
        return -1;
    }

    private int gameAction(int keyCode) {
        try {
            return getGameAction(keyCode);
        } catch (IllegalArgumentException ignored) {
            return 0;
        }
    }

    private static boolean isLeftSoftKey(int keyCode) {
        return keyCode == -6 || keyCode == -21;
    }

    private static boolean isRightSoftKey(int keyCode) {
        return keyCode == -7 || keyCode == -22;
    }

    private void releaseButtons() {
        if (cpu == null) {
            return;
        }
        for (int i = 0; i < E23Cpu.BUTTON_COUNT; i++) {
            cpu.setButton(i, false);
        }
    }
}
