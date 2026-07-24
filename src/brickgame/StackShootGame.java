package brickgame;

/** Program I: shoot blocks upward, complete rows and keep the stack away. */
final class StackShootGame extends Game {

    private static final OriginalRandom RANDOM = new OriginalRandom();

    private final boolean[][] field = new boolean[20][10];
    private int playerX;
    private int shotX;
    private int shotY;
    private boolean shotActive;
    private long nextFall;
    private long nextShot;

    StackShootGame(GameEngine engine) {
        super(engine);
        engine.setScore(0);
        engine.setLife(3);
    }

    void init(long now) {
        playerX = 4;
        shotActive = false;
        int y;
        int x;
        for (y = 0; y < 20; y++) {
            for (x = 0; x < 10; x++) {
                field[y][x] = y < engine.level && RANDOM.nextBooleanValue();
            }
        }
        nextFall = now + fallPeriod();
        nextShot = now;
        repaint();
    }

    void tick(long now) {
        if (now >= nextFall) {
            nextFall = now + fallPeriod();
            descend(now);
            if (engine.isLocked()) {
                return;
            }
        }
        if (shotActive && now >= nextShot) {
            nextShot = now + 45L;
            moveShot(now);
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
                && !shotActive) {
            shotActive = true;
            shotX = playerX;
            shotY = 17;
            nextShot = now;
            repaint();
        } else if (action == GameEngine.ACTION_DOWN) {
            descend(now);
            nextFall = now + fallPeriod();
        }
    }

    private void descend(long now) {
        int y;
        int x;
        for (y = 19; y > 0; y--) {
            for (x = 0; x < 10; x++) {
                field[y][x] = field[y - 1][x];
            }
        }
        for (x = 0; x < 10; x++) {
            field[0][x] = RANDOM.nextInt(3) != 0;
        }
        clearFullRows(now);
        for (x = 0; x < 10; x++) {
            if (field[18][x]) {
                repaint();
                crash(18, x);
                return;
            }
        }
        repaint();
    }

    private void moveShot(long now) {
        if (shotY <= 0 || field[shotY - 1][shotX]) {
            field[shotY][shotX] = true;
            shotActive = false;
            clearFullRows(now);
            repaint();
            return;
        }
        shotY--;
        repaint();
    }

    private void clearFullRows(long now) {
        int y;
        int x;
        for (y = 0; y < 20; y++) {
            boolean full = true;
            for (x = 0; x < 10; x++) {
                if (!field[y][x]) {
                    full = false;
                    break;
                }
            }
            if (!full) {
                continue;
            }
            int move;
            for (move = y; move < 19; move++) {
                for (x = 0; x < 10; x++) {
                    field[move][x] = field[move + 1][x];
                }
            }
            for (x = 0; x < 10; x++) {
                field[19][x] = false;
            }
            engine.addScore(1);
            if (engine.score > 0 && engine.score % 50 == 0) {
                engine.increaseLevel();
                init(now);
                return;
            }
            y--;
        }
    }

    private int fallPeriod() {
        int value = 1900 - engine.speed * 85;
        return value < 500 ? 500 : value;
    }

    private void repaint() {
        clearBoard();
        int y;
        int x;
        for (y = 0; y < 20; y++) {
            for (x = 0; x < 10; x++) {
                if (field[y][x]) {
                    cell(y, x, true);
                }
            }
        }
        for (x = playerX - 1; x <= playerX + 1; x++) {
            cell(19, x, true);
        }
        cell(18, playerX, true);
        if (shotActive) {
            cell(shotY, shotX, true);
        }
    }
}
