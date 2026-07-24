package brickgame;

/** Procedural corridor maze with a slow LCD chaser. */
final class MazeGame extends Game {

    private final short[] walls = new short[20];
    private int playerY;
    private int playerX;
    private int enemyY;
    private int enemyX;
    private boolean blink;
    private long nextBlink;
    private long nextEnemy;

    MazeGame(GameEngine engine) {
        super(engine);
        engine.setScore(0);
        engine.setLife(3);
    }

    void init(long now) {
        buildMaze();
        playerY = 18;
        playerX = 1;
        enemyY = 1;
        enemyX = 1;
        blink = true;
        nextBlink = now + 280L;
        nextEnemy = now + enemyPeriod();
        render();
    }

    void tick(long now) {
        boolean changed = false;
        if (now >= nextBlink) {
            nextBlink += 280L;
            blink = !blink;
            changed = true;
        }
        if (now >= nextEnemy) {
            nextEnemy += enemyPeriod();
            moveEnemy();
            changed = true;
        }
        if (changed) {
            render();
            verifyCollision();
        }
    }

    void keyPressed(int action, long now) {
        int dy = 0;
        int dx = 0;
        if (action == GameEngine.ACTION_UP) {
            dy = -1;
        } else if (action == GameEngine.ACTION_DOWN) {
            dy = 1;
        } else if (action == GameEngine.ACTION_LEFT) {
            dx = -1;
        } else if (action == GameEngine.ACTION_RIGHT) {
            dx = 1;
        } else if (action == GameEngine.ACTION_FIRE) {
            moveEnemy();
            render();
            verifyCollision();
            return;
        }
        int nextY = playerY + dy;
        int nextX = playerX + dx;
        if ((dy != 0 || dx != 0) && !wall(nextY, nextX)) {
            playerY = nextY;
            playerX = nextX;
            engine.addScore(1);
            if (playerY == 1 && playerX == 8) {
                engine.addScore(500);
                engine.increaseLevel();
                init(now);
                return;
            }
            render();
            verifyCollision();
        }
    }

    private void buildMaze() {
        int y;
        for (y = 0; y < 20; y++) {
            walls[y] = 0;
            walls[y] |= 1;
            walls[y] |= 1 << 9;
        }
        walls[0] = 0x03FF;
        walls[19] = 0x03FF;

        int wallIndex = 0;
        for (y = 3; y <= 15; y += 3) {
            int gap;
            if ((wallIndex & 1) == 0) {
                gap = 8 - ((engine.level + wallIndex) % 3);
            } else {
                gap = 1 + ((engine.level + wallIndex) % 3);
            }
            int x;
            for (x = 1; x <= 8; x++) {
                if (x != gap) {
                    walls[y] |= 1 << x;
                }
            }
            wallIndex++;
        }
        walls[1] &= ~(1 << 8);
        walls[18] &= ~(1 << 1);
    }

    private void moveEnemy() {
        int dy = playerY < enemyY ? -1 : playerY > enemyY ? 1 : 0;
        int dx = playerX < enemyX ? -1 : playerX > enemyX ? 1 : 0;
        if (dy != 0 && !wall(enemyY + dy, enemyX)) {
            enemyY += dy;
        } else if (dx != 0 && !wall(enemyY, enemyX + dx)) {
            enemyX += dx;
        } else if (!wall(enemyY, enemyX + 1)) {
            enemyX++;
        } else if (!wall(enemyY, enemyX - 1)) {
            enemyX--;
        } else if (!wall(enemyY + 1, enemyX)) {
            enemyY++;
        } else if (!wall(enemyY - 1, enemyX)) {
            enemyY--;
        }
    }

    private boolean wall(int y, int x) {
        return y < 0 || y >= 20 || x < 0 || x >= 10
            || (walls[y] & (1 << x)) != 0;
    }

    private void verifyCollision() {
        if (playerY == enemyY && playerX == enemyX) {
            crash(playerY, playerX);
        }
    }

    private void render() {
        clearBoard();
        int y;
        int x;
        for (y = 0; y < 20; y++) {
            for (x = 0; x < 10; x++) {
                if ((walls[y] & (1 << x)) != 0) {
                    cell(y, x, true);
                }
            }
        }
        if (blink) {
            cell(1, 8, true);
        }
        cell(enemyY, enemyX, true);
        cell(playerY, playerX, true);
    }

    private int enemyPeriod() {
        int value = 760 - engine.speed * 25 - engine.level * 12;
        return value < 220 ? 220 : value;
    }
}
