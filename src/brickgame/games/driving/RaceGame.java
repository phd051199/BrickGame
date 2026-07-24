package brickgame;

/** Direct CLDC port of game.race.RaceGame, Car and Road. */
final class RaceGame extends Game {

    private static final OriginalRandom RANDOM = new OriginalRandom();

    private static final int[] CAR = {0x2, 0x7, 0x2, 0x5};

    private final int[] enemyY = new int[4];
    private final int[] enemyX = new int[4];
    private int enemyCount;
    private int playerX;
    private int traffic;
    private long nextMove;

    RaceGame(GameEngine engine) {
        super(engine);
        engine.setScore(0);
        engine.setLife(3);
    }

    void init(long now) {
        playerX = 2;
        enemyCount = 1;
        enemyY[0] = -4;
        enemyX[0] = RANDOM.nextBooleanValue() ? 2 : 5;
        traffic = 0;
        nextMove = now;
    }

    void tick(long now) {
        if (now >= nextMove) {
            nextMove += period();
            onMove(now);
        }
    }

    void keyPressed(int action, long now) {
        if (action == GameEngine.ACTION_LEFT) {
            if (playerX == 5) {
                playerX = 2;
            }
            repaint();
            verifyCrash();
        } else if (action == GameEngine.ACTION_RIGHT) {
            if (playerX == 2) {
                playerX = 5;
            }
            repaint();
            verifyCrash();
        } else if (action == GameEngine.ACTION_UP || action == GameEngine.ACTION_FIRE) {
            onMove(now);
        }
    }

    private void onMove(long now) {
        int i;
        for (i = 0; i < enemyCount; i++) {
            enemyY[i]++;
        }
        traffic++;

        if (traffic % 9 == 0) {
            enemyY[enemyCount] = -4;
            enemyX[enemyCount] = RANDOM.nextBooleanValue() ? 2 : 5;
            enemyCount++;
            if (enemyCount > 3) {
                for (i = 1; i < enemyCount; i++) {
                    enemyY[i - 1] = enemyY[i];
                    enemyX[i - 1] = enemyX[i];
                }
                enemyCount--;
            }
        }

        engine.addScore(10);
        repaint();
        verifyCrash();

        if (traffic >= 1000) {
            engine.increaseSpeed();
            init(now);
        }
    }

    private void verifyCrash() {
        int i;
        int y;
        int x;
        for (i = 0; i < enemyCount; i++) {
            for (y = 0; y < 4; y++) {
                for (x = 0; x < 3; x++) {
                    if (bit(y, x)
                        && contains(enemyY[i], enemyX[i], 16 + y, playerX + x)) {
                        crash(16 + y, playerX + x);
                        return;
                    }
                }
            }
        }
    }

    private static boolean contains(int top, int left, int y, int x) {
        int localY = y - top;
        int localX = x - left;
        return localY >= 0 && localY < 4 && localX >= 0 && localX < 3
            && bit(localY, localX);
    }

    private void repaint() {
        clearBoard();
        int y;
        for (y = 0; y < 20; y++) {
            int original = y - traffic;
            while (original < 0) {
                original += 20;
            }
            if (original % 4 != 0) {
                cell(y, 0, true);
                cell(y, 9, true);
            }
        }
        drawCar(16, playerX);
        int i;
        for (i = 0; i < enemyCount; i++) {
            drawCar(enemyY[i], enemyX[i]);
        }
    }

    private void drawCar(int top, int left) {
        int y;
        int x;
        for (y = 0; y < 4; y++) {
            for (x = 0; x < 3; x++) {
                if (bit(y, x)) {
                    cell(top + y, left + x, true);
                }
            }
        }
    }

    private int period() {
        return 300 - engine.speed * 18;
    }

    private static boolean bit(int y, int x) {
        return (CAR[y] & (1 << (2 - x))) != 0;
    }
}
