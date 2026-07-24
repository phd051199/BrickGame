package brickgame;

/** Program D: guide a ricocheting ball past a moving blocker to the top. */
final class WallBallGame extends Game {

    private static final int[][] LEVELS = {
        {0x303, 0x201},
        {0x303, 0x303, 0x201},
        {0x303, 0x303, 0x201, 0x201},
        {0x387, 0x303, 0x201},
        {0x387, 0x387, 0x303, 0x201},
        {0x387, 0x387, 0x303, 0x303, 0x201},
        {0x387, 0x387, 0x303, 0x303, 0x201, 0x201},
        {0x3CF, 0x387, 0x303, 0x201},
        {0x3CF, 0x3CF, 0x387, 0x303, 0x201, 0x201}
    };

    private final boolean[][] walls = new boolean[20][10];
    private int paddleX;
    private int enemyX;
    private int enemyDirection;
    private int ballX;
    private int ballY;
    private int dx;
    private int dy;
    private boolean launched;
    private long nextMove;

    WallBallGame(GameEngine engine) {
        super(engine);
        engine.setScore(0);
        engine.setLife(4);
    }

    void init(long now) {
        paddleX = 3;
        enemyX = 3;
        enemyDirection = -1;
        ballX = 4;
        ballY = 18;
        dx = -1;
        dy = -1;
        launched = false;
        loadWalls();
        nextMove = now + period();
        repaint();
    }

    void tick(long now) {
        if (now >= nextMove) {
            nextMove += period();
            moveEnemy();
            if (launched) {
                moveBall(now);
            } else {
                repaint();
            }
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
            }
            moveEnemy();
            moveBall(now);
            nextMove = now + period();
        }
    }

    private void movePaddle(int amount) {
        int next = paddleX + amount;
        if (next < 0 || next > 6) {
            return;
        }
        paddleX = next;
        if (!launched) {
            ballX += amount;
            dx = amount;
        }
        repaint();
    }

    private void moveEnemy() {
        enemyX += enemyDirection;
        if (enemyX <= 2) {
            enemyX = 2;
            enemyDirection = 1;
        } else if (enemyX >= 5) {
            enemyX = 5;
            enemyDirection = -1;
        }
    }

    private void moveBall(long now) {
        if (!launched) {
            repaint();
            return;
        }

        int nx = ballX + dx;
        int ny = ballY + dy;
        if (nx < 0 || nx >= 10) {
            dx = -dx;
            nx = ballX + dx;
        }

        if (dy > 0 && ny >= 18) {
            if (nx >= paddleX && nx <= paddleX + 3) {
                dy = -1;
                ny = 18;
                if (nx == paddleX) {
                    dx = -1;
                } else if (nx == paddleX + 3) {
                    dx = 1;
                }
            } else if (ny > 19) {
                crash(19, nx);
                return;
            }
        }

        boolean horizontal = solid(ballY, nx);
        boolean vertical = solid(ny, ballX);
        boolean diagonal = solid(ny, nx);
        if (horizontal) {
            dx = -dx;
        }
        if (vertical) {
            dy = -dy;
        }
        if (!horizontal && !vertical && diagonal) {
            dx = -dx;
            dy = -dy;
        }
        if (horizontal || vertical || diagonal) {
            nx = ballX + dx;
            ny = ballY + dy;
        }

        ballX = nx;
        ballY = ny;
        if (ballY <= 0) {
            engine.addScore(1);
            if (engine.score % 10 == 0) {
                engine.increaseLevel();
            }
            init(now);
            return;
        }
        repaint();
    }

    private boolean solid(int y, int x) {
        if (x < 0 || x >= 10 || y < 0 || y >= 20) {
            return false;
        }
        if (walls[y][x]) {
            return true;
        }
        return y == 8 && x >= enemyX && x < enemyX + 3;
    }

    private void loadWalls() {
        int y;
        int x;
        for (y = 0; y < 20; y++) {
            for (x = 0; x < 10; x++) {
                walls[y][x] = false;
            }
        }
        int[] pattern = LEVELS[engine.level % LEVELS.length];
        for (y = 0; y < pattern.length; y++) {
            for (x = 0; x < 10; x++) {
                walls[y][x] = (pattern[y] & (1 << (9 - x))) != 0;
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
                if (walls[y][x]) {
                    cell(y, x, true);
                }
            }
        }
        for (x = paddleX; x < paddleX + 4; x++) {
            cell(19, x, true);
        }
        for (x = enemyX; x < enemyX + 3; x++) {
            cell(8, x, true);
        }
        cell(ballY, ballX, true);
    }
}
