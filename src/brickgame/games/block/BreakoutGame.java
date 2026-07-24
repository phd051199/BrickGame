package brickgame;

/**
 * Programs B and C from the common 14-game handheld set. Program B is classic
 * Breakout; program C adds a second controlled paddle at the top.
 */
final class BreakoutGame extends Game {

    private static final int[][] SINGLE_LEVELS = {
        {0x000, 0x000, 0x000, 0x1FE, 0x0FC, 0x0FC, 0x1FE},
        {0x000, 0x000, 0x000, 0x1FE, 0x030, 0x0FC, 0x030, 0x1FE},
        {0x000, 0x000, 0x048, 0x1FE, 0x0FC, 0x030, 0x0FC, 0x1CE},
        {0x000, 0x084, 0x078, 0x0FC, 0x1B6, 0x1FE, 0x1CE, 0x0FC, 0x078},
        {0x000, 0x180, 0x1CA, 0x18E, 0x004, 0x030, 0x134, 0x380, 0x28C, 0x006, 0x00C},
        {0x000, 0x1FE, 0x084, 0x084, 0x048, 0x030, 0x048, 0x084, 0x084, 0x1FE},
        {0x000, 0x000, 0x030, 0x048, 0x084, 0x1FE, 0x102, 0x162, 0x162, 0x162, 0x3FF},
        {0x3FF, 0x3FF, 0x303, 0x303, 0x303, 0x303, 0x303, 0x303, 0x303, 0x303, 0x3FF, 0x3FF},
        {0x3FF, 0x3FF, 0x000, 0x176, 0x126, 0x180, 0x03A, 0x182, 0x336, 0x030, 0x000, 0x3FF, 0x3FF}
    };

    private static final int[][] DOUBLE_LEVELS = {
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

    private final boolean dualPaddles;
    private final boolean[][] bricks = new boolean[20][10];

    private int paddleX;
    private int ballX;
    private int ballY;
    private int dx;
    private int dy;
    private int brickCount;
    private boolean launched;
    private long nextMove;

    BreakoutGame(GameEngine engine, boolean dualPaddles) {
        super(engine);
        this.dualPaddles = dualPaddles;
        engine.setScore(0);
        engine.setLife(4);
    }

    void init(long now) {
        paddleX = 3;
        ballX = 4;
        ballY = 18;
        dx = -1;
        dy = -1;
        launched = false;
        loadLevel();
        nextMove = now + period();
        repaint();
    }

    void tick(long now) {
        if (launched && now >= nextMove) {
            nextMove += period();
            moveBall(now);
        }
    }

    void keyPressed(int action, long now) {
        if (action == GameEngine.ACTION_LEFT) {
            movePaddle(-1);
        } else if (action == GameEngine.ACTION_RIGHT) {
            movePaddle(1);
        } else if (action == GameEngine.ACTION_UP || action == GameEngine.ACTION_FIRE) {
            if (!launched) {
                launched = true;
                nextMove = now;
            } else {
                moveBall(now);
                nextMove = now + period();
            }
        }
    }

    private void movePaddle(int direction) {
        int next = paddleX + direction;
        if (next < 0 || next > 6) {
            return;
        }
        paddleX = next;
        if (!launched) {
            ballX += direction;
        } else if (ballY == 18 && ballX >= paddleX && ballX <= paddleX + 3) {
            dx = direction;
        }
        repaint();
    }

    private void moveBall(long now) {
        int nx = ballX + dx;
        int ny = ballY + dy;

        if (nx < 0 || nx >= 10) {
            dx = -dx;
            nx = ballX + dx;
        }

        if (!dualPaddles && ny < 0) {
            dy = 1;
            ny = ballY + dy;
        }

        if (dualPaddles && dy < 0 && ny <= 0) {
            if (nx >= paddleX && nx <= paddleX + 3) {
                dy = 1;
                ny = 1;
                steerFromPaddle(nx);
            } else if (ny < 0) {
                crash(0, nx);
                return;
            }
        }

        if (dy > 0 && ny >= 18) {
            if (nx >= paddleX && nx <= paddleX + 3) {
                dy = -1;
                ny = 18;
                steerFromPaddle(nx);
            } else if (ny > 19) {
                crash(19, nx);
                return;
            }
        }

        boolean hitHorizontal = brickAt(ballY, nx);
        boolean hitVertical = brickAt(ny, ballX);
        boolean hitDiagonal = brickAt(ny, nx);
        if (hitHorizontal || hitVertical || hitDiagonal) {
            if (hitHorizontal) {
                removeBrick(ballY, nx);
                dx = -dx;
            }
            if (hitVertical) {
                removeBrick(ny, ballX);
                dy = -dy;
            }
            if (!hitHorizontal && !hitVertical && hitDiagonal) {
                removeBrick(ny, nx);
                dx = -dx;
                dy = -dy;
            } else if (hitDiagonal) {
                removeBrick(ny, nx);
            }
            nx = ballX + dx;
            ny = ballY + dy;
        }

        if (nx < 0) {
            nx = 0;
        } else if (nx > 9) {
            nx = 9;
        }
        if (ny < 0) {
            ny = 0;
        }

        ballX = nx;
        ballY = ny;
        repaint();

        if (brickCount <= 0) {
            engine.increaseLevel();
            init(now);
        }
    }

    private void steerFromPaddle(int contactX) {
        if (contactX <= paddleX) {
            dx = -1;
        } else if (contactX >= paddleX + 3) {
            dx = 1;
        }
    }

    private boolean brickAt(int y, int x) {
        return y >= 0 && y < 20 && x >= 0 && x < 10 && bricks[y][x];
    }

    private void removeBrick(int y, int x) {
        if (brickAt(y, x)) {
            bricks[y][x] = false;
            brickCount--;
            engine.addScore(10);
        }
    }

    private void loadLevel() {
        int y;
        int x;
        for (y = 0; y < 20; y++) {
            for (x = 0; x < 10; x++) {
                bricks[y][x] = false;
            }
        }
        brickCount = 0;
        int[][] levels = dualPaddles ? DOUBLE_LEVELS : SINGLE_LEVELS;
        int[] pattern = levels[engine.level % levels.length];
        int top = dualPaddles ? 6 : 1;
        for (y = 0; y < pattern.length && top + y < 18; y++) {
            int bits = pattern[y];
            for (x = 0; x < 10; x++) {
                if ((bits & (1 << (9 - x))) != 0) {
                    bricks[top + y][x] = true;
                    brickCount++;
                }
            }
        }
    }

    private int period() {
        int value = 230 - engine.speed * 11;
        return value < 55 ? 55 : value;
    }

    private void repaint() {
        clearBoard();
        int y;
        int x;
        for (y = 0; y < 20; y++) {
            for (x = 0; x < 10; x++) {
                if (bricks[y][x]) {
                    cell(y, x, true);
                }
            }
        }
        for (x = paddleX; x < paddleX + 4; x++) {
            cell(19, x, true);
            if (dualPaddles) {
                cell(0, x, true);
            }
        }
        cell(ballY, ballX, true);
    }
}
