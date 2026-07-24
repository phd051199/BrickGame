package brickgame;

/**
 * CLDC-compatible Brick Game controller. The menu follows the common A-N
 * handheld layout and exposes fourteen distinct game programs.
 */
final class GameEngine {

    static final int ACTION_NONE = 0;
    static final int ACTION_UP = 1;
    static final int ACTION_DOWN = 2;
    static final int ACTION_LEFT = 3;
    static final int ACTION_RIGHT = 4;
    static final int ACTION_FIRE = 5;
    static final int ACTION_PAUSE = 6;
    static final int ACTION_SOUND = 7;
    static final int ACTION_MENU = 8;

    static final int BOARD_ROWS = 20;
    static final int BOARD_COLS = 10;
    static final int GAME_COUNT = 14;

    private static final String[] GAME_NAMES = {
        "TANKS", "BREAKOUT", "DOUBLE", "WALL BALL",
        "RACE", "HIGHWAY", "TUNNEL", "SHOOT",
        "STACK", "INVADERS", "SNAKE", "FROGGER",
        "MATCH", "TETRIS"
    };

    private static final int[][] MENU_LETTERS = {
        {0x0E, 0x11, 0x1F, 0x11, 0x11}, // A
        {0x1E, 0x11, 0x1E, 0x11, 0x1E}, // B
        {0x0F, 0x10, 0x10, 0x10, 0x0F}, // C
        {0x1E, 0x11, 0x11, 0x11, 0x1E}, // D
        {0x1F, 0x10, 0x1E, 0x10, 0x1F}, // E
        {0x1F, 0x10, 0x1E, 0x10, 0x10}, // F
        {0x0F, 0x10, 0x17, 0x11, 0x0F}, // G
        {0x11, 0x11, 0x1F, 0x11, 0x11}, // H
        {0x1F, 0x04, 0x04, 0x04, 0x1F}, // I
        {0x07, 0x01, 0x01, 0x11, 0x0E}, // J
        {0x11, 0x12, 0x1C, 0x12, 0x11}, // K
        {0x10, 0x10, 0x10, 0x10, 0x1F}, // L
        {0x11, 0x1B, 0x15, 0x11, 0x11}, // M
        {0x11, 0x19, 0x15, 0x13, 0x11}  // N
    };

    private static final int[][] MENU_ICONS = {
        {0x6, 0xF, 0x6, 0x9}, // tanks
        {0xF, 0xF, 0x2, 0x7}, // breakout
        {0xF, 0x2, 0x2, 0xF}, // double breakout
        {0x6, 0x2, 0x2, 0xF}, // wall ball
        {0x2, 0x7, 0x2, 0x5}, // race
        {0x5, 0x2, 0x5, 0x2}, // highway
        {0x9, 0xA, 0xA, 0x9}, // tunnel
        {0x2, 0x2, 0x2, 0x7}, // shoot
        {0x2, 0x6, 0xE, 0xF}, // stack
        {0x9, 0xF, 0x6, 0x9}, // invaders
        {0x8, 0xE, 0x2, 0x3}, // snake
        {0x6, 0xF, 0x6, 0x9}, // frogger
        {0x5, 0xA, 0xF, 0x5}, // match
        {0x0, 0x6, 0x6, 0x0}  // tetris
    };

    final boolean[][] board = new boolean[BOARD_ROWS][BOARD_COLS];
    final boolean[][] preview = new boolean[4][4];

    int score;
    int speed;
    int level;
    int life;

    private Game game;
    private CrashState crash;
    private int selectedGame;
    private int menuFrame;
    private long nextMenuFrame;
    private boolean menu = true;
    private boolean sound = true;
    private boolean pause;
    private boolean systemPause;
    private boolean locked;
    private long logicalNow;
    private long lastRealNow;
    private boolean clockStarted;

    GameEngine() {
        drawMenu();
    }

    synchronized void tick(long realNow) {
        if (!clockStarted) {
            clockStarted = true;
            lastRealNow = realNow;
        }
        long elapsed = realNow - lastRealNow;
        lastRealNow = realNow;
        if (elapsed < 0L) {
            elapsed = 0L;
        }
        if (!pause && !systemPause) {
            logicalNow += elapsed;
        }
        if (pause || systemPause) {
            return;
        }

        if (crash != null) {
            if (crash.update(logicalNow)) {
                finishCrash();
            }
            return;
        }

        if (menu) {
            if (logicalNow >= nextMenuFrame) {
                nextMenuFrame = logicalNow + 320L;
                menuFrame++;
                drawMenu();
            }
        } else if (game != null) {
            game.tick(logicalNow);
        }
    }

    synchronized void press(int action) {
        if (action == ACTION_SOUND) {
            sound = !sound;
            return;
        }
        if (action == ACTION_PAUSE) {
            if (!menu) {
                pause = !pause;
            }
            return;
        }
        if (action == ACTION_MENU) {
            reset();
            return;
        }
        if (pause || systemPause || locked) {
            return;
        }

        if (menu) {
            menuKey(action);
        } else if (game != null) {
            game.keyPressed(action, logicalNow);
        }
    }

    private void menuKey(int action) {
        if (action == ACTION_DOWN) {
            setLevel(level + 1);
            drawMenu();
        } else if (action == ACTION_LEFT) {
            selectedGame--;
            if (selectedGame < 0) {
                selectedGame = GAME_COUNT - 1;
            }
            menuFrame = 0;
            drawMenu();
        } else if (action == ACTION_RIGHT) {
            selectedGame++;
            if (selectedGame >= GAME_COUNT) {
                selectedGame = 0;
            }
            menuFrame = 0;
            drawMenu();
        } else if (action == ACTION_UP) {
            setSpeed(speed + 1);
            drawMenu();
        } else if (action == ACTION_FIRE) {
            startGame(selectedGame);
        }
    }

    private void startGame(int index) {
        menu = false;
        pause = false;
        score = 0;
        clearBoard();
        clearPreview();

        if (index == 0) {
            game = new TanksGame(this);
        } else if (index == 1) {
            game = new BreakoutGame(this, false);
        } else if (index == 2) {
            game = new BreakoutGame(this, true);
        } else if (index == 3) {
            game = new WallBallGame(this);
        } else if (index == 4) {
            game = new RaceGame(this);
        } else if (index == 5) {
            game = new HighwayGame(this);
        } else if (index == 6) {
            game = new TunnelGame(this);
        } else if (index == 7) {
            game = new ShootGame(this);
        } else if (index == 8) {
            game = new StackShootGame(this);
        } else if (index == 9) {
            game = new InvadersGame(this);
        } else if (index == 10) {
            game = new SnakeGame(this);
        } else if (index == 11) {
            game = new FroggerGame(this);
        } else if (index == 12) {
            game = new MatchGame(this);
        } else {
            game = new TetrisGame(this);
        }
        game.init(logicalNow);
    }

    private void reset() {
        crash = null;
        locked = false;
        game = null;
        menu = true;
        pause = false;
        life = 0;
        score = selectedGame + 1;
        menuFrame = 0;
        nextMenuFrame = logicalNow + 320L;
        drawMenu();
    }

    synchronized void setSystemPaused(boolean value) {
        systemPause = value;
        lastRealNow = System.currentTimeMillis();
        clockStarted = true;
    }

    boolean isLocked() {
        return locked;
    }

    void crash(int y, int x) {
        if (crash != null || menu) {
            return;
        }
        setLife(life - 1);
        locked = true;
        crash = new CrashState(y, x, logicalNow);
    }

    private void finishCrash() {
        crash = null;
        locked = false;
        if (life <= 0) {
            game = null;
            menu = true;
            pause = false;
            score = selectedGame + 1;
            menuFrame = 0;
            drawMenu();
        } else if (game != null) {
            game.init(logicalNow);
        }
    }

    void setScore(int value) {
        if (value < 0) {
            value = 0;
        }
        if (value > 999999) {
            value = 999999;
        }
        score = value;
    }

    int addScore(int value) {
        setScore(score + value);
        return score;
    }

    void setSpeed(int value) {
        if (value < 0) {
            value = 15;
        }
        speed = value > 15 ? 0 : value;
    }

    int increaseSpeed() {
        setSpeed(speed + 1);
        return speed;
    }

    void setLevel(int value) {
        if (value < 0) {
            value = 15;
        }
        level = value > 15 ? 0 : value;
    }

    int increaseLevel() {
        setLevel(level + 1);
        return level;
    }

    void setLife(int value) {
        if (value < 0) {
            value = 0;
        }
        if (value > 16) {
            value = 16;
        }
        if (life == value && value != 0) {
            return;
        }
        life = value;
        if (!menu) {
            repaintLife();
        }
    }

    private void repaintLife() {
        clearPreview();
        int y;
        int x;
        for (y = 0; y < 4; y++) {
            for (x = 0; x < 4; x++) {
                preview[y][x] = (4 - y) + (4 * x) <= life;
            }
        }
    }

    void clearBoard() {
        int y;
        int x;
        for (y = 0; y < BOARD_ROWS; y++) {
            for (x = 0; x < BOARD_COLS; x++) {
                board[y][x] = false;
            }
        }
    }

    void clearPreview() {
        int y;
        int x;
        for (y = 0; y < 4; y++) {
            for (x = 0; x < 4; x++) {
                preview[y][x] = false;
            }
        }
    }

    void setBoardCell(int y, int x, boolean value) {
        if (y >= 0 && y < BOARD_ROWS && x >= 0 && x < BOARD_COLS) {
            board[y][x] = value;
        }
    }

    void setPreviewCell(int y, int x, boolean value) {
        if (y >= 0 && y < 4 && x >= 0 && x < 4) {
            preview[y][x] = value;
        }
    }

    private void drawMenu() {
        clearBoard();
        clearPreview();
        score = selectedGame + 1;

        drawLetter(selectedGame, 2, 1);

        int x;
        for (x = 1; x < 9; x++) {
            setBoardCell(7, x, true);
        }

        drawMenuIcon(selectedGame, menuFrame);
    }

    private void drawLetter(int index, int left, int top) {
        int y;
        int x;
        for (y = 0; y < 5; y++) {
            int bits = MENU_LETTERS[index][y];
            for (x = 0; x < 5; x++) {
                if ((bits & (1 << (4 - x))) != 0) {
                    setBoardCell(top + y, left + x, true);
                }
            }
        }
    }

    private void drawMenuIcon(int index, int frame) {
        int y;
        int x;
        int offsetY = 10 + (frame & 1);
        int offsetX = 1;
        for (y = 0; y < 4; y++) {
            int bits = MENU_ICONS[index][y];
            if ((frame & 1) != 0 && (index == 4 || index == 5 || index == 6)) {
                bits = ((bits << 1) | (bits >> 3)) & 0xF;
            }
            for (x = 0; x < 4; x++) {
                boolean on = (bits & (1 << (3 - x))) != 0;
                preview[y][x] = on;
                if (on) {
                    int px = offsetX + x * 2;
                    int py = offsetY + y * 2;
                    setBoardCell(py, px, true);
                    setBoardCell(py, px + 1, true);
                    setBoardCell(py + 1, px, true);
                    setBoardCell(py + 1, px + 1, true);
                }
            }
        }
    }

    synchronized void copySnapshot(Snapshot target) {
        int y;
        int x;
        for (y = 0; y < BOARD_ROWS; y++) {
            for (x = 0; x < BOARD_COLS; x++) {
                target.board[y][x] = board[y][x];
            }
        }
        for (y = 0; y < 4; y++) {
            for (x = 0; x < 4; x++) {
                target.preview[y][x] = preview[y][x];
            }
        }
        target.score = score;
        target.speed = speed;
        target.level = level;
        target.life = life;
        target.sound = sound;
        target.pause = pause;
        target.menu = menu;
        target.gameIndex = selectedGame;
        target.gameCode = selectedGame + 1;
        target.gameLetter = (char) ('A' + selectedGame);
        target.gameName = GAME_NAMES[selectedGame];
    }

    private final class CrashState {
        private final boolean[][] screenshot = new boolean[BOARD_ROWS][BOARD_COLS];
        private final boolean[][] mask = new boolean[BOARD_ROWS][BOARD_COLS];
        private final int pointY;
        private final int pointX;
        private final long started;
        private int renderedPhase = -1;

        CrashState(int pointY, int pointX, long started) {
            this.pointY = pointY;
            this.pointX = pointX;
            this.started = started;
            int y;
            int x;
            for (y = 0; y < BOARD_ROWS; y++) {
                for (x = 0; x < BOARD_COLS; x++) {
                    screenshot[y][x] = board[y][x];
                    mask[y][x] = board[y][x]
                        && !(y >= pointY - 2 && y <= pointY + 2
                        && x >= pointX - 2 && x <= pointX + 2);
                }
            }
            paintState(2);
        }

        boolean update(long now) {
            long elapsed = now - started;
            if (elapsed < 1500L) {
                int step = (int) (elapsed / 50L);
                int state = (29 - step) % 3;
                if (state < 0) {
                    state += 3;
                }
                if (renderedPhase != step) {
                    renderedPhase = step;
                    paintState(state);
                }
                return false;
            }
            if (elapsed < 2000L) {
                int step = (int) ((elapsed - 1500L) / 25L);
                if (step > 19) {
                    step = 19;
                }
                if (renderedPhase != 100 + step) {
                    renderedPhase = 100 + step;
                    paintCleanup(step);
                }
                return false;
            }
            return true;
        }

        private void paintState(int state) {
            clearBoard();
            int y;
            int x;
            for (y = 0; y < BOARD_ROWS; y++) {
                for (x = 0; x < BOARD_COLS; x++) {
                    board[y][x] = mask[y][x];
                }
            }
            if (state == 0) {
                setBoardCell(pointY, pointX, true);
            } else if (state == 1) {
                for (y = -1; y <= 1; y++) {
                    for (x = -1; x <= 1; x++) {
                        if (y == -1 || y == 1 || x == -1 || x == 1) {
                            setBoardCell(pointY + y, pointX + x, true);
                        }
                    }
                }
            } else {
                int[] offsets = {-2, 0, 2};
                int iy;
                int ix;
                for (iy = 0; iy < offsets.length; iy++) {
                    for (ix = 0; ix < offsets.length; ix++) {
                        if (!(offsets[iy] == 0 && offsets[ix] == 0)) {
                            setBoardCell(pointY + offsets[iy], pointX + offsets[ix], true);
                        }
                    }
                }
            }
        }

        private void paintCleanup(int step) {
            int y;
            int x;
            for (y = 0; y < BOARD_ROWS; y++) {
                for (x = 0; x < BOARD_COLS; x++) {
                    board[y][x] = screenshot[y][x];
                }
            }
            int first = 19 - step;
            for (y = first; y < BOARD_ROWS; y++) {
                for (x = 0; x < BOARD_COLS; x++) {
                    board[y][x] = true;
                }
            }
        }
    }

    static final class Snapshot {
        final boolean[][] board = new boolean[BOARD_ROWS][BOARD_COLS];
        final boolean[][] preview = new boolean[4][4];
        int score;
        int speed;
        int level;
        int life;
        int gameIndex;
        int gameCode;
        char gameLetter;
        String gameName;
        boolean sound;
        boolean pause;
        boolean menu;
    }
}
