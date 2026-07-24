package brickgame;

/**
 * CLDC game controller. Board state is stored as row bitmasks to minimize RAM,
 * copying and per-frame work on Nokia devices.
 */
final class GameEngine {

    static final int ACTION_NONE = 0;
    static final int ACTION_UP = 1;
    static final int ACTION_DOWN = 2;
    static final int ACTION_LEFT = 3;
    static final int ACTION_RIGHT = 4;
    static final int ACTION_FIRE = 5;
    static final int ACTION_PAUSE = 6;
    static final int ACTION_MENU = 7;

    static final int BOARD_ROWS = 20;
    static final int BOARD_COLS = 10;
    static final int GAME_COUNT = GameCatalog.COUNT;

    final short[] boardRows = new short[BOARD_ROWS];
    final byte[] previewRows = new byte[4];

    int score;
    int speed;
    int level;
    int life;

    private final GameMenu menu = new GameMenu();
    private Game game;
    private CrashAnimation crash;
    private boolean menuActive = true;
    private boolean pause;
    private boolean systemPause;
    private boolean locked;
    private long logicalNow;
    private long lastRealNow;
    private boolean clockStarted;
    private int revision = 1;

    GameEngine() {
        menu.reset(0L, this);
    }

    synchronized void tick(long realNow) {
        updateClock(realNow);
        if (pause || systemPause) {
            return;
        }
        if (crash != null) {
            if (crash.update(logicalNow)) {
                finishCrash();
            }
            return;
        }
        if (menuActive) {
            menu.tick(logicalNow, this);
        } else if (game != null) {
            game.tick(logicalNow);
        }
    }

    synchronized void press(int action) {
        if (action == ACTION_PAUSE) {
            if (!menuActive) {
                pause = !pause;
                touch();
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
        if (menuActive) {
            int requested = menu.handle(action, this);
            if (requested != GameMenu.NO_START) {
                startGame(requested);
            }
        } else if (game != null) {
            game.keyPressed(action, logicalNow);
        }
    }

    synchronized boolean copySnapshot(GameSnapshot target) {
        if (target.revision == revision) {
            return false;
        }
        System.arraycopy(boardRows, 0, target.board, 0, BOARD_ROWS);
        System.arraycopy(previewRows, 0, target.preview, 0, 4);
        int selected = menu.selected();
        target.score = score;
        target.speed = speed;
        target.level = level;
        target.life = life;
        target.pause = pause;
        target.menu = menuActive;
        target.gameIndex = selected;
        target.gameCode = GameCatalog.code(selected);
        target.gameLetter = GameCatalog.letter(selected);
        target.gameLabel = GameCatalog.label(selected);
        target.gameName = GameCatalog.name(selected);
        target.usesLife = GameCatalog.usesLife(selected);
        target.usesNextPreview = GameCatalog.usesNextPreview(selected);
        target.revision = revision;
        return true;
    }

    synchronized void setSystemPaused(boolean value) {
        systemPause = value;
        lastRealNow = System.currentTimeMillis();
        clockStarted = true;
        touch();
    }

    boolean isLocked() {
        return locked;
    }

    void crash(int y, int x) {
        if (crash != null || menuActive) {
            return;
        }
        setLife(life - 1);
        locked = true;
        crash = new CrashAnimation(this, y, x, logicalNow);
        touch();
    }

    void setScore(int value) {
        int next = value;
        if (next < 0) {
            next = 0;
        } else if (next > 999999) {
            next = 999999;
        }
        if (score != next) {
            score = next;
            touch();
        }
    }

    int addScore(int value) {
        setScore(score + value);
        return score;
    }

    void setSpeed(int value) {
        int next = value;
        if (next < 0) {
            next = 15;
        } else if (next > 15) {
            next = 0;
        }
        if (speed != next) {
            speed = next;
            touch();
        }
    }

    int increaseSpeed() {
        setSpeed(speed + 1);
        return speed;
    }

    void setLevel(int value) {
        int next = value;
        if (next < 0) {
            next = 15;
        } else if (next > 15) {
            next = 0;
        }
        if (level != next) {
            level = next;
            touch();
        }
    }

    int increaseLevel() {
        setLevel(level + 1);
        return level;
    }

    void setLife(int value) {
        int next = value;
        if (next < 0) {
            next = 0;
        } else if (next > 16) {
            next = 16;
        }
        if (life == next && next != 0) {
            return;
        }
        life = next;
        if (!menuActive) {
            repaintLife();
        }
        touch();
    }

    void clearBoard() {
        boolean changed = false;
        int y;
        for (y = 0; y < BOARD_ROWS; y++) {
            if (boardRows[y] != 0) {
                boardRows[y] = 0;
                changed = true;
            }
        }
        if (changed) {
            touch();
        }
    }

    void clearPreview() {
        boolean changed = false;
        int y;
        for (y = 0; y < 4; y++) {
            if (previewRows[y] != 0) {
                previewRows[y] = 0;
                changed = true;
            }
        }
        if (changed) {
            touch();
        }
    }

    void setBoardCell(int y, int x, boolean value) {
        if (y < 0 || y >= BOARD_ROWS || x < 0 || x >= BOARD_COLS) {
            return;
        }
        int bit = 1 << x;
        short old = boardRows[y];
        short next = value ? (short) (old | bit) : (short) (old & ~bit);
        if (old != next) {
            boardRows[y] = next;
            touch();
        }
    }

    void setPreviewCell(int y, int x, boolean value) {
        if (y < 0 || y >= 4 || x < 0 || x >= 4) {
            return;
        }
        int bit = 1 << x;
        byte old = previewRows[y];
        byte next = value ? (byte) (old | bit) : (byte) (old & ~bit);
        if (old != next) {
            previewRows[y] = next;
            touch();
        }
    }

    void setBoardRow(int y, short value) {
        if (y >= 0 && y < BOARD_ROWS && boardRows[y] != value) {
            boardRows[y] = value;
            touch();
        }
    }

    void replaceBoard(short[] source) {
        boolean changed = false;
        int y;
        for (y = 0; y < BOARD_ROWS; y++) {
            if (boardRows[y] != source[y]) {
                boardRows[y] = source[y];
                changed = true;
            }
        }
        if (changed) {
            touch();
        }
    }

    void touch() {
        revision++;
        if (revision < 0) {
            revision = 1;
        }
    }

    private void updateClock(long realNow) {
        if (!clockStarted) {
            clockStarted = true;
            lastRealNow = realNow;
        }
        long elapsed = realNow - lastRealNow;
        lastRealNow = realNow;
        if (elapsed < 0L) {
            elapsed = 0L;
        } else if (elapsed > 250L) {
            elapsed = 250L;
        }
        if (!pause && !systemPause) {
            logicalNow += elapsed;
        }
    }

    private void startGame(int index) {
        menuActive = false;
        pause = false;
        locked = false;
        crash = null;
        score = 0;
        life = 0;
        clearBoard();
        clearPreview();
        game = GameCatalog.create(index, this);
        game.init(logicalNow);
        touch();
    }

    private void reset() {
        crash = null;
        locked = false;
        game = null;
        menuActive = true;
        pause = false;
        life = 0;
        menu.reset(logicalNow, this);
        touch();
    }

    private void finishCrash() {
        crash = null;
        locked = false;
        if (life <= 0) {
            game = null;
            menuActive = true;
            pause = false;
            life = 0;
            menu.reset(logicalNow, this);
        } else if (game != null) {
            game.init(logicalNow);
        }
        touch();
    }

    private void repaintLife() {
        clearPreview();
        int y;
        int x;
        for (y = 0; y < 4; y++) {
            for (x = 0; x < 4; x++) {
                setPreviewCell(y, x, (4 - y) + 4 * x <= life);
            }
        }
    }
}
