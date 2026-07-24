package brickgame;

/** Program L: cross eight moving traffic lanes and fill all ten goal cells. */
final class FroggerGame extends Game {

    private static final int[][] LEVEL_ROWS = {
        {0x3078, 0x183C, 0x860F, 0x10C3, 0x8787, 0xE1E1, 0x8787, 0xC1C1},
        {0x3070, 0x933C, 0x820F, 0x10C8, 0x8607, 0xE0E1, 0x8487, 0xE0C4},
        {0x3070, 0x933C, 0xB0E1, 0x10C0, 0x8671, 0xE0E1, 0x8487, 0xE0C4},
        {0x10C0, 0x3070, 0x9384, 0x8487, 0x4446, 0x8E31, 0x8407, 0xE0C4},
        {0x8487, 0x3770, 0x9084, 0x8487, 0x4446, 0x10CE, 0x3270, 0x9384},
        {0x3070, 0x3770, 0x8787, 0xB4B7, 0x8787, 0x9384, 0x3270, 0x9384},
        {0xE0E1, 0x3470, 0x9204, 0x83FF, 0xC48F, 0x8487, 0x9334, 0xE0C4},
        {0x3070, 0x933C, 0xFC3F, 0x8487, 0xE0E1, 0x83FF, 0x8787, 0xE0C4},
        {0xF8FF, 0x923C, 0xFC3F, 0x8487, 0xE0E1, 0xF8FF, 0x4446, 0xE0C4}
    };

    private static final int[][] LEVEL_DIRECTIONS = {
        {1, 1, 1, 1, 1, 1, 1, 1},
        {1, 1, 1, 0, 1, 1, 1, 1},
        {1, 0, 1, 1, 1, 0, 1, 1},
        {1, 1, 0, 1, 0, 1, 0, 1},
        {0, 1, 0, 1, 0, 1, 0, 1},
        {0, 0, 1, 0, 0, 1, 1, 0},
        {0, 0, 1, 0, 0, 0, 1, 0},
        {0, 0, 0, 0, 1, 1, 0, 0},
        {0, 0, 0, 0, 1, 0, 0, 0}
    };

    private final int[] rows = new int[8];
    private final int[] directions = new int[8];
    private final boolean[] goals = new boolean[10];

    private int playerX;
    private int playerY;
    private int configuredLevel = -1;
    private int goalCount;
    private long nextMove;

    FroggerGame(GameEngine engine) {
        super(engine);
        engine.setScore(0);
        engine.setLife(4);
    }

    void init(long now) {
        if (configuredLevel != engine.level) {
            configureLevel();
        }
        playerX = 5;
        playerY = 19;
        nextMove = now + period();
        repaint();
    }

    void tick(long now) {
        if (now >= nextMove) {
            nextMove = now + period();
            advanceTraffic(now);
        }
    }

    void keyPressed(int action, long now) {
        if (action == GameEngine.ACTION_UP) {
            moveUp(now);
        } else if (action == GameEngine.ACTION_DOWN) {
            if (playerY < 19) {
                playerY += 2;
                if (playerY > 19) {
                    playerY = 19;
                }
            }
            repaint();
            verifyCrash();
        } else if (action == GameEngine.ACTION_LEFT && playerX > 0) {
            playerX--;
            repaint();
            verifyCrash();
        } else if (action == GameEngine.ACTION_RIGHT && playerX < 9) {
            playerX++;
            repaint();
            verifyCrash();
        } else if (action == GameEngine.ACTION_FIRE) {
            advanceTraffic(now);
            nextMove = now + period();
        }
    }

    private void moveUp(long now) {
        if (playerY > 1) {
            playerY -= 2;
            repaint();
            verifyCrash();
            return;
        }
        if (playerY == 1) {
            playerY = 0;
            if (goals[playerX]) {
                repaint();
                crash(0, playerX);
                return;
            }
            goals[playerX] = true;
            goalCount++;
            engine.addScore(1);
            if (goalCount >= 10) {
                engine.increaseLevel();
                configureLevel();
            }
            playerX = 5;
            playerY = 19;
            nextMove = now + period();
            repaint();
        }
    }

    private void advanceTraffic(long now) {
        int lane;
        for (lane = 0; lane < 8; lane++) {
            int bits = rows[lane] & 0xFFFF;
            if (directions[lane] != 0) {
                rows[lane] = (bits >>> 1) | ((bits & 1) << 15);
            } else {
                rows[lane] = ((bits << 1) & 0xFFFF) | ((bits >>> 15) & 1);
            }
        }

        if (playerY != 0 && playerY != 19) {
            int direction = playerY == 1 ? 1 : directions[(playerY - 3) / 2];
            playerX += direction != 0 ? 1 : -1;
            if (playerX < 0 || playerX > 9) {
                repaint();
                crash(playerY, playerX < 0 ? 0 : 9);
                return;
            }
        }
        repaint();
        verifyCrash();
    }

    private void verifyCrash() {
        if (playerY >= 3 && playerY <= 17 && (playerY & 1) != 0) {
            int lane = (playerY - 3) / 2;
            if (rowCell(rows[lane], playerX)) {
                crash(playerY, playerX);
            }
        }
    }

    private void configureLevel() {
        configuredLevel = engine.level;
        int index = configuredLevel % LEVEL_ROWS.length;
        int i;
        for (i = 0; i < 8; i++) {
            rows[i] = LEVEL_ROWS[index][i];
            directions[i] = LEVEL_DIRECTIONS[index][i];
        }
        for (i = 0; i < goals.length; i++) {
            goals[i] = false;
        }
        goalCount = 0;
    }

    private static boolean rowCell(int bits, int x) {
        return (bits & (1 << (15 - x))) != 0;
    }

    private int period() {
        int value = 450 - engine.speed * 22;
        return value < 120 ? 120 : value;
    }

    private void repaint() {
        clearBoard();
        int x;
        int y;
        for (x = 0; x < 10; x++) {
            if (goals[x]) {
                cell(0, x, true);
            }
        }
        for (y = 2; y < 20; y += 2) {
            for (x = 0; x < 10; x++) {
                cell(y, x, true);
            }
        }
        int lane;
        for (lane = 0; lane < 8; lane++) {
            int boardY = 3 + lane * 2;
            for (x = 0; x < 10; x++) {
                if (rowCell(rows[lane], x)) {
                    cell(boardY, x, true);
                }
            }
        }
        cell(playerY, playerX, true);
    }
}
