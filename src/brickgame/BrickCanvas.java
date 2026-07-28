package brickgame;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

final class BrickCanvas extends GameCanvas implements Runnable {
    private static final int STATE_IDLE = 0;
    private static final int STATE_LOADING = 1;
    private static final int STATE_RUNNING = 2;
    private static final int STATE_PAUSED = 3;
    private static final int STATE_ERROR = 4;

    private static final int FRAME_MS = 33;
    private static final int MAX_ELAPSED_MS = 75;
    private static final int CPU_SLICE_CYCLES = 1024;

    private final Midlet midlet;

    private volatile boolean alive;
    private volatile int state = STATE_IDLE;
    private volatile boolean forcePaint;
    private Thread loopThread;
    private int loadingMachine = -1;
    private String errorMessage = "";
    private MachineProfile profile;
    private BrickCpu cpu;
    private CommonLcdMap map;
    private CommonBrickRenderer renderer;
    private byte[] lastVram;
    private boolean lastDisplayEnabled;
    private long lastCpuTime;
    private long cycleRemainder;
    private int cycleOvershoot;

    BrickCanvas(Midlet midlet) {
        super(false);
        this.midlet = midlet;
        setFullScreenMode(true);
    }

    synchronized void start() {
        if (alive) {
            return;
        }
        alive = true;
        if (state == STATE_RUNNING) {
            lastCpuTime = System.currentTimeMillis();
        }
        loopThread = new Thread(this);
        loopThread.start();
    }

    synchronized void stop() {
        alive = false;
        Thread thread = loopThread;
        loopThread = null;
        if (thread != null) {
            thread.interrupt();
        }
        releaseAllButtons();
    }

    synchronized void loadMachine(int index) {
        if (index < 0 || index >= MachineProfile.ALL.length) {
            return;
        }
        releaseAllButtons();
        loadingMachine = index;
        errorMessage = "";
        state = STATE_LOADING;
        forcePaint = true;
        wakeLoop();
    }

    synchronized void enterIdle() {
        releaseAllButtons();
        loadingMachine = -1;
        state = STATE_IDLE;
        forcePaint = false;
    }

    public void run() {
        while (alive) {
            long frameStart = System.currentTimeMillis();
            int currentState = state;
            if (currentState == STATE_LOADING) {
                paintLoading();
                loadSelectedMachine();
            } else if (currentState == STATE_RUNNING) {
                boolean changed = emulateFrame(frameStart);
                if (changed || forcePaint) {
                    paintGame(false);
                    forcePaint = false;
                }
            } else if (currentState == STATE_PAUSED) {
                if (forcePaint) {
                    paintGame(true);
                    forcePaint = false;
                }
            } else if (currentState == STATE_ERROR) {
                if (forcePaint) {
                    paintError();
                    forcePaint = false;
                }
            }

            long elapsed = System.currentTimeMillis() - frameStart;
            long sleep = (currentState == STATE_RUNNING ? FRAME_MS : 80) - elapsed;
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
        int currentState = state;
        if (currentState == STATE_RUNNING || currentState == STATE_PAUSED) {
            paintGameTo(graphics, currentState == STATE_PAUSED);
        } else if (currentState == STATE_LOADING) {
            paintLoadingTo(graphics);
        } else if (currentState == STATE_ERROR) {
            paintErrorTo(graphics);
        }
    }

    private boolean emulateFrame(long now) {
        if (cpu == null || map == null || renderer == null || profile == null) {
            return false;
        }
        long elapsed = now - lastCpuTime;
        if (elapsed < 0) {
            elapsed = 0;
        } else if (elapsed > MAX_ELAPSED_MS) {
            elapsed = FRAME_MS;
        }
        lastCpuTime = now;

        cycleRemainder += elapsed * (long) profile.clockHz;
        int budget = (int) (cycleRemainder / 1000L);
        cycleRemainder %= 1000L;

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
            int slice = budget > CPU_SLICE_CYCLES ? CPU_SLICE_CYCLES : budget;
            int consumed = cpu.runCycles(slice);
            if (consumed <= 0) {
                break;
            }
            budget -= consumed;
        }
        if (budget < 0) {
            cycleOvershoot = -budget;
        }
        return displayChanged();
    }

    private boolean displayChanged() {
        byte[] current = cpu.vram();
        boolean enabled = cpu.displayEnabled();
        if (lastVram == null || lastVram.length != current.length) {
            lastVram = new byte[current.length];
            for (int i = 0; i < current.length; i++) {
                lastVram[i] = current[i];
            }
            lastDisplayEnabled = enabled;
            return true;
        }
        boolean changed = enabled != lastDisplayEnabled;
        lastDisplayEnabled = enabled;
        for (int i = 0; i < current.length; i++) {
            if (lastVram[i] != current[i]) {
                lastVram[i] = current[i];
                changed = true;
            }
        }
        return changed;
    }

    private void loadSelectedMachine() {
        int index = loadingMachine;
        if (index < 0 || index >= MachineProfile.ALL.length) {
            state = STATE_IDLE;
            return;
        }
        try {
            MachineProfile nextProfile = MachineProfile.ALL[index];
            byte[] rom = Resources.read(nextProfile.romPath());
            CommonLcdMap nextMap = CommonLcdMap.load(nextProfile.mapPath());
            CommonBrickRenderer nextRenderer = renderer;
            if (nextRenderer == null) {
                nextRenderer = new CommonBrickRenderer(
                        BitmapFont.load("/ui/regular7.bmf"),
                        BitmapFont.load("/ui/bold8.bmf"));
            }
            BrickCpu nextCpu = CpuFactory.create(nextProfile, rom);
            profile = nextProfile;
            map = nextMap;
            renderer = nextRenderer;
            renderer.invalidate();
            cpu = nextCpu;
            lastVram = null;
            lastDisplayEnabled = !nextCpu.displayEnabled();
            displayChanged();
            lastCpuTime = System.currentTimeMillis();
            cycleRemainder = 0;
            cycleOvershoot = 0;
            forcePaint = true;
            state = STATE_RUNNING;
        } catch (Throwable error) {
            errorMessage = error.toString();
            forcePaint = true;
            state = STATE_ERROR;
        }
    }

    private void paintGame(boolean paused) {
        try {
            Graphics graphics = getGraphics();
            paintGameTo(graphics, paused);
            flushGraphics();
        } catch (Throwable ignored) {
        }
    }

    private void paintGameTo(Graphics graphics, boolean paused) {
        int width = getWidth();
        int height = getHeight();
        graphics.setClip(0, 0, width, height);
        if (renderer == null || map == null || profile == null || cpu == null) {
            graphics.setColor(LcdGridRenderer.COLOR_LCD);
            graphics.fillRect(0, 0, width, height);
            return;
        }
        renderer.render(graphics, profile, cpu, map, paused, width, height);
    }

    private void paintLoading() {
        try {
            Graphics graphics = getGraphics();
            paintLoadingTo(graphics);
            flushGraphics();
        } catch (Throwable ignored) {
        }
    }

    private void paintLoadingTo(Graphics graphics) {
        int width = getWidth();
        int height = getHeight();
        graphics.setClip(0, 0, width, height);
        graphics.setColor(LcdGridRenderer.COLOR_LCD);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(LcdGridRenderer.COLOR_INK);
        graphics.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        drawCentered(graphics, "LOADING", width, height / 2 - 8);
    }

    private void paintError() {
        try {
            Graphics graphics = getGraphics();
            paintErrorTo(graphics);
            flushGraphics();
        } catch (Throwable ignored) {
        }
    }

    private void paintErrorTo(Graphics graphics) {
        int width = getWidth();
        int height = getHeight();
        graphics.setClip(0, 0, width, height);
        graphics.setColor(LcdGridRenderer.COLOR_LCD);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(LcdGridRenderer.COLOR_INK);
        graphics.setFont(Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        drawCentered(graphics, "LOAD FAILED", width, 38);
        graphics.setFont(Font.getDefaultFont());
        drawWrapped(graphics, errorMessage, 12, 72, width - 24, 6);
        drawCentered(graphics, "ANY KEY: CORE LIST", width, height - 24);
    }

    private void drawWrapped(Graphics graphics, String text, int x,
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
            String current = remaining.substring(0, split).trim();
            graphics.drawString(current, x, y, Graphics.TOP | Graphics.LEFT);
            remaining = remaining.substring(split).trim();
            y += font.getHeight() + 2;
        }
    }

    private static void drawCentered(Graphics graphics, String text, int width, int y) {
        graphics.drawString(text, width / 2, y, Graphics.TOP | Graphics.HCENTER);
    }

    protected void keyPressed(int keyCode) {
        int currentState = state;
        if (currentState == STATE_ERROR) {
            midlet.showMachineList();
            return;
        }
        if (currentState == STATE_LOADING || currentState == STATE_IDLE) {
            return;
        }
        if (currentState == STATE_PAUSED) {
            if (isRightSoftKey(keyCode)) {
                midlet.showMachineList();
            } else {
                lastCpuTime = System.currentTimeMillis();
                forcePaint = true;
                state = STATE_RUNNING;
                wakeLoop();
            }
            return;
        }
        if (isLeftSoftKey(keyCode) || keyCode == KEY_POUND) {
            releaseAllButtons();
            forcePaint = true;
            state = STATE_PAUSED;
            wakeLoop();
            return;
        }
        if (isRightSoftKey(keyCode)) {
            midlet.showMachineList();
            return;
        }
        int button = buttonForKey(keyCode);
        if (button >= 0 && cpu != null) {
            cpu.setButton(button, true);
            wakeLoop();
        }
    }

    protected void keyReleased(int keyCode) {
        if (state != STATE_RUNNING || cpu == null) {
            return;
        }
        int button = buttonForKey(keyCode);
        if (button >= 0) {
            cpu.setButton(button, false);
            wakeLoop();
        }
    }

    protected void keyRepeated(int keyCode) {
    }

    private int buttonForKey(int keyCode) {
        int action = gameAction(keyCode);
        if (action == LEFT || keyCode == 'a' || keyCode == 'A') {
            return MachineProfile.BUTTON_LEFT;
        }
        if (action == RIGHT || keyCode == 'd' || keyCode == 'D') {
            return MachineProfile.BUTTON_RIGHT;
        }
        if (action == DOWN || keyCode == 's' || keyCode == 'S') {
            return MachineProfile.BUTTON_DOWN;
        }
        if (action == UP || action == FIRE || keyCode == ' '
                || keyCode == 'w' || keyCode == 'W') {
            return MachineProfile.BUTTON_ROTATE;
        }
        if (keyCode == KEY_NUM1 || keyCode == 'p' || keyCode == 'P') {
            return MachineProfile.BUTTON_START;
        }
        if (keyCode == KEY_NUM2 || keyCode == 'o' || keyCode == 'O') {
            return MachineProfile.BUTTON_AUX;
        }
        if (keyCode == KEY_NUM3 || keyCode == 'm' || keyCode == 'M') {
            return MachineProfile.BUTTON_OPTION;
        }
        if (keyCode == KEY_STAR || keyCode == 'r' || keyCode == 'R') {
            return MachineProfile.BUTTON_RESET;
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

    private boolean isLeftSoftKey(int keyCode) {
        return keyCode == -6 || keyCode == -21;
    }

    private boolean isRightSoftKey(int keyCode) {
        return keyCode == -7 || keyCode == -22;
    }

    private void wakeLoop() {
        Thread thread = loopThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void releaseAllButtons() {
        BrickCpu currentCpu = cpu;
        if (currentCpu == null) {
            return;
        }
        for (int i = 0; i < MachineProfile.BUTTON_COUNT; i++) {
            currentCpu.setButton(i, false);
        }
    }
}
