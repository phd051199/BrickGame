package brickgame;

/** Program J: destroy a descending formation while its lowest brick fires back. */
final class InvadersGame extends Game {

    private static final OriginalRandom RANDOM = new OriginalRandom();

    private static final int[][] LEVELS = {
        {0x18C, 0x252, 0x222, 0x104, 0x088, 0x070},
        {0x186, 0x387, 0x3FF, 0x3FF, 0x387, 0x186},
        {0x3FC, 0x207, 0x205, 0x31D, 0x31F, 0x1E0},
        {0x3C0, 0x060, 0x3FF, 0x060, 0x3C0},
        {0x0C6, 0x129, 0x254, 0x210, 0x129, 0x0C6},
        {0x078, 0x3A7, 0x397, 0x397, 0x078, 0x030},
        {0x1FF, 0x345, 0x37D, 0x301, 0x301, 0x1FF},
        {0x1FE, 0x0FC, 0x078, 0x0FC, 0x186, 0x303},
        {0x0FC, 0x1B6, 0x3FF, 0x3CF, 0x0FC, 0x14A}
    };

    private final boolean[][] formation = new boolean[20][10];
    private int playerX;
    private boolean playerShot;
    private int playerShotX;
    private int playerShotY;
    private boolean enemyShot;
    private int enemyShotX;
    private int enemyShotY;
    private int enemyDx;
    private long nextDescend;
    private long nextProjectile;

    InvadersGame(GameEngine engine) {
        super(engine);
        engine.setScore(0);
        engine.setLife(3);
    }

    void init(long now) {
        playerX = 4;
        playerShot = false;
        enemyShot = false;
        loadLevel();
        nextDescend = now + descendPeriod();
        nextProjectile = now + FAST_PROJECTILE_PERIOD;
        repaint();
    }

    void tick(long now) {
        if (now >= nextDescend) {
            nextDescend += descendPeriod();
            descend(now);
            if (engine.isLocked()) {
                return;
            }
        }
        if (now >= nextProjectile) {
            nextProjectile += FAST_PROJECTILE_PERIOD;
            moveProjectiles(now);
        }
    }

    void keyPressed(int action, long now) {
        if (action == GameEngine.ACTION_LEFT && playerX > 0) {
            playerX--;
            repaint();
        } else if (action == GameEngine.ACTION_RIGHT && playerX < 9) {
            playerX++;
            repaint();
        } else if ((action == GameEngine.ACTION_UP || action == GameEngine.ACTION_FIRE)
                && !playerShot) {
            playerShot = true;
            playerShotX = playerX;
            playerShotY = 17;
            nextProjectile = now;
            repaint();
        } else if (action == GameEngine.ACTION_DOWN) {
            descend(now);
            nextDescend = now + descendPeriod();
        }
    }

    private void descend(long now) {
        if (!enemyShot) {
            launchEnemyShot();
        }
        int y;
        int x;
        for (y = 19; y > 0; y--) {
            for (x = 0; x < 10; x++) {
                formation[y][x] = formation[y - 1][x];
            }
        }
        for (x = 0; x < 10; x++) {
            formation[0][x] = false;
            if (formation[18][x]) {
                repaint();
                crash(18, x);
                return;
            }
        }
        repaint();
        checkWin(now);
    }

    private void launchEnemyShot() {
        int y;
        int x;
        for (y = 19; y >= 0; y--) {
            for (x = 0; x < 10; x++) {
                if (formation[y][x]) {
                    formation[y][x] = false;
                    enemyShot = true;
                    enemyShotX = x;
                    enemyShotY = y + 1;
                    enemyDx = x != 0 && (x == 9 || RANDOM.nextBooleanValue()) ? -1 : 1;
                    return;
                }
            }
        }
    }

    private void moveProjectiles(long now) {
        if (playerShot) {
            if (playerShotY >= 0 && formation[playerShotY][playerShotX]) {
                formation[playerShotY][playerShotX] = false;
                playerShot = false;
                engine.addScore(1);
            } else if (enemyShot
                    && playerShotX == enemyShotX
                    && playerShotY == enemyShotY) {
                playerShot = false;
                enemyShot = false;
            } else {
                playerShotY--;
                if (playerShotY < 0) {
                    playerShot = false;
                }
            }
        }

        if (enemyShot) {
            enemyShotY++;
            enemyShotX += enemyDx;
            if (enemyShotX <= 0) {
                enemyShotX = 0;
                enemyDx = 1;
            } else if (enemyShotX >= 9) {
                enemyShotX = 9;
                enemyDx = -1;
            }

            if ((enemyShotY == 18 && enemyShotX == playerX)
                    || (enemyShotY == 19
                    && enemyShotX >= playerX - 1
                    && enemyShotX <= playerX + 1)) {
                repaint();
                crash(19, playerX);
                return;
            }
            if (enemyShotY >= 20) {
                enemyShot = false;
            }
        }
        repaint();
        checkWin(now);
    }

    private void checkWin(long now) {
        if (enemyShot) {
            return;
        }
        int y;
        int x;
        for (y = 0; y < 20; y++) {
            for (x = 0; x < 10; x++) {
                if (formation[y][x]) {
                    return;
                }
            }
        }
        engine.increaseLevel();
        init(now);
    }

    private void loadLevel() {
        int y;
        int x;
        for (y = 0; y < 20; y++) {
            for (x = 0; x < 10; x++) {
                formation[y][x] = false;
            }
        }
        int[] pattern = LEVELS[engine.level % LEVELS.length];
        for (y = 0; y < pattern.length; y++) {
            for (x = 0; x < 10; x++) {
                formation[y][x] = (pattern[y] & (1 << (9 - x))) != 0;
            }
        }
    }

    private int descendPeriod() {
        int value = 1500 - engine.speed * 65;
        return value < 400 ? 400 : value;
    }

    private void repaint() {
        clearBoard();
        int y;
        int x;
        for (y = 0; y < 20; y++) {
            for (x = 0; x < 10; x++) {
                if (formation[y][x]) {
                    cell(y, x, true);
                }
            }
        }
        for (x = playerX - 1; x <= playerX + 1; x++) {
            cell(19, x, true);
        }
        cell(18, playerX, true);
        if (playerShot) {
            cell(playerShotY, playerShotX, true);
        }
        if (enemyShot) {
            cell(enemyShotY, enemyShotX, true);
        }
    }
}
