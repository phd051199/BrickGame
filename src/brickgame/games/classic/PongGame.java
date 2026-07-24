package brickgame;

/** Single-player LCD Pong with a small predictive AI paddle. */
final class PongGame extends Game {

    private static final int BOARD_HEIGHT = 20;
    private static final int PADDLE_HEIGHT = 4;
    private static final int AI_X = 9;
    private static final int MAX_PADDLE_Y = BOARD_HEIGHT - PADDLE_HEIGHT;
    private static final int CENTER_PADDLE_Y = MAX_PADDLE_Y / 2;
    /** Matches the held-direction repeat rate used by InputRepeater. */
    private static final long AI_REPEAT_PERIOD = 66L;

    private int playerY;
    private int aiY;
    private int aiTargetY;
    private int aiMoveDirection;
    private int ballY;
    private int ballX;
    private int dy;
    private int dx;
    private boolean launched;
    private long nextMove;
    private long nextAiMove;

    PongGame(GameEngine engine) {
        super(engine);
        engine.setScore(0);
        engine.setLife(4);
    }

    void init(long now) {
        playerY = CENTER_PADDLE_Y;
        aiY = CENTER_PADDLE_Y;
        aiTargetY = CENTER_PADDLE_Y;
        aiMoveDirection = 0;
        ballY = playerY + 1;
        ballX = 1;
        dx = 1;
        dy = ((engine.level + engine.score) & 1) == 0 ? -1 : 1;
        launched = false;
        nextMove = now;
        nextAiMove = now;
        render();
    }

    void tick(long now) {
        if (!launched) {
            return;
        }
        boolean aiMoved = moveAi(now);
        if (now >= nextMove) {
            nextMove += period();
            step();
        } else if (aiMoved) {
            render();
        }
    }

    void keyPressed(int action, long now) {
        if (action == GameEngine.ACTION_UP && playerY > 0) {
            playerY--;
            if (!launched) {
                ballY = playerY + 1;
            }
            render();
        } else if (action == GameEngine.ACTION_DOWN
                && playerY < MAX_PADDLE_Y) {
            playerY++;
            if (!launched) {
                ballY = playerY + 1;
            }
            render();
        } else if (action == GameEngine.ACTION_FIRE
                || action == GameEngine.ACTION_RIGHT) {
            if (!launched) {
                launched = true;
                nextMove = now;
                nextAiMove = now;
                aiMoveDirection = 0;
            } else {
                step();
            }
        }
    }

    private void step() {
        int nextY = ballY + dy;
        int nextX = ballX + dx;
        if (nextY < 0 || nextY >= BOARD_HEIGHT) {
            dy = -dy;
            nextY = ballY + dy;
        }

        if (nextX == 0 && within(nextY, playerY, PADDLE_HEIGHT)) {
            dx = 1;
            nextX = 1;
            adjustVertical(nextY, playerY);
        } else if (nextX == AI_X
                && within(nextY, aiY, PADDLE_HEIGHT)) {
            dx = -1;
            nextX = AI_X - 1;
            adjustVertical(nextY, aiY);
        }

        ballY = nextY;
        ballX = nextX;
        if (ballX < 0) {
            crash(ballY, 0);
            return;
        }
        if (ballX > AI_X) {
            engine.addScore(100);
            resetRound();
        }
        render();
    }

    private boolean moveAi(long now) {
        aiTargetY = chooseAiTargetY();
        int direction = 0;
        if (aiY < aiTargetY) {
            direction = 1;
        } else if (aiY > aiTargetY) {
            direction = -1;
        }

        if (direction == 0) {
            aiMoveDirection = 0;
            nextAiMove = now;
            return false;
        }

        if (direction != aiMoveDirection) {
            aiMoveDirection = direction;
            aiY += direction;
            nextAiMove = now + AI_REPEAT_PERIOD;
            return true;
        }
        if (now < nextAiMove) {
            return false;
        }

        aiY += direction;
        do {
            nextAiMove += AI_REPEAT_PERIOD;
        } while (nextAiMove <= now);
        return true;
    }

    private int chooseAiTargetY() {
        if (dx < 0) {
            return CENTER_PADDLE_Y;
        }
        int impactY = predictImpactY(ballY, dy, AI_X - ballX);
        return clampPaddleY(impactY - PADDLE_HEIGHT / 2);
    }

    static int predictImpactY(int startY, int verticalDirection,
                              int horizontalSteps) {
        int y = startY;
        int direction = verticalDirection;
        int step;
        for (step = 0; step < horizontalSteps; step++) {
            int nextY = y + direction;
            if (nextY < 0 || nextY >= BOARD_HEIGHT) {
                direction = -direction;
                nextY = y + direction;
            }
            y = nextY;
        }
        return y;
    }

    private static int clampPaddleY(int value) {
        if (value < 0) {
            return 0;
        }
        return value > MAX_PADDLE_Y ? MAX_PADDLE_Y : value;
    }

    private void adjustVertical(int hitY, int paddleY) {
        if (hitY <= paddleY) {
            dy = -1;
        } else if (hitY >= paddleY + PADDLE_HEIGHT - 1) {
            dy = 1;
        }
    }

    private void resetRound() {
        launched = false;
        ballX = 1;
        ballY = playerY + 1;
        dx = 1;
        aiTargetY = aiY;
        aiMoveDirection = 0;
        nextAiMove = nextMove;
    }

    private void render() {
        clearBoard();
        int y;
        for (y = 0; y < PADDLE_HEIGHT; y++) {
            cell(playerY + y, 0, true);
            cell(aiY + y, AI_X, true);
        }
        cell(ballY, ballX, true);
    }

    private int period() {
        int value = 190 - engine.speed * 8;
        return value < 55 ? 55 : value;
    }

    private static boolean within(int value, int start, int length) {
        return value >= start && value < start + length;
    }
}
