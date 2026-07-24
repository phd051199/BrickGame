package brickgame;

/** Compact monochrome pinball table with persistent bumpers and a bottom bat. */
final class PinballGame extends Game {

    private static final int[] BUMPER_Y = {4, 4, 7, 7, 10, 10, 13};
    private static final int[] BUMPER_X = {2, 7, 4, 8, 1, 6, 4};

    private int paddleX;
    private int ballY;
    private int ballX;
    private int dy;
    private int dx;
    private boolean launched;
    private long nextMove;

    PinballGame(GameEngine engine) {
        super(engine);
        engine.setScore(0);
        engine.setLife(4);
    }

    void init(long now) {
        paddleX = 3;
        ballY = 17;
        ballX = paddleX + 1;
        dy = -1;
        dx = 1;
        launched = false;
        nextMove = now;
        render();
    }

    void tick(long now) {
        if (launched && now >= nextMove) {
            nextMove += period();
            step();
        }
    }

    void keyPressed(int action, long now) {
        if (action == GameEngine.ACTION_LEFT && paddleX > 0) {
            paddleX--;
            if (!launched) {
                ballX = paddleX + 1;
            }
            render();
        } else if (action == GameEngine.ACTION_RIGHT && paddleX < 6) {
            paddleX++;
            if (!launched) {
                ballX = paddleX + 1;
            }
            render();
        } else if (action == GameEngine.ACTION_FIRE
                || action == GameEngine.ACTION_UP) {
            if (!launched) {
                launched = true;
                nextMove = now;
            } else {
                step();
            }
        }
    }

    private void step() {
        int nextY = ballY + dy;
        int nextX = ballX + dx;
        if (nextX < 0 || nextX >= 10) {
            dx = -dx;
            nextX = ballX + dx;
        }
        if (nextY < 0) {
            dy = 1;
            nextY = 1;
        }

        int bumper = bumperAt(nextY, nextX);
        if (bumper >= 0) {
            dy = -dy;
            if (((bumper + engine.score) & 1) != 0) {
                dx = -dx;
            }
            engine.addScore(25);
            nextY = ballY + dy;
            nextX = ballX + dx;
        }

        if (nextY == 18 && nextX >= paddleX && nextX < paddleX + 4) {
            dy = -1;
            if (nextX == paddleX) {
                dx = -1;
            } else if (nextX == paddleX + 3) {
                dx = 1;
            }
            nextY = 17;
        }

        ballY = nextY;
        ballX = nextX;
        if (ballY >= 20) {
            crash(19, ballX);
            return;
        }
        render();
    }

    private int bumperAt(int y, int x) {
        int i;
        for (i = 0; i < BUMPER_Y.length; i++) {
            if (BUMPER_Y[i] == y && BUMPER_X[i] == x) {
                return i;
            }
        }
        return -1;
    }

    private void render() {
        clearBoard();
        int y;
        for (y = 0; y < 20; y++) {
            if ((y & 1) == 0) {
                cell(y, 0, true);
                cell(y, 9, true);
            }
        }
        int i;
        for (i = 0; i < BUMPER_Y.length; i++) {
            cell(BUMPER_Y[i], BUMPER_X[i], true);
        }
        for (i = 0; i < 4; i++) {
            cell(19, paddleX + i, true);
        }
        cell(ballY, ballX, true);
    }

    private int period() {
        int value = 125 - engine.speed * 4;
        return value < 45 ? 45 : value;
    }
}
