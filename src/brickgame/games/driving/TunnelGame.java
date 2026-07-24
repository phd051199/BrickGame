package brickgame;

/** Program G: steer a car through a five-cell-wide scrolling tunnel. */
final class TunnelGame extends Game {

    private static final OriginalRandom RANDOM = new OriginalRandom();
    private static final int[] CAR = {0x2, 0x7, 0x2, 0x5};

    private final int[] opening = new int[20];
    private int playerX;
    private int playerY;
    private int straightCounter;
    private int scoreCounter;
    private long nextMove;

    TunnelGame(GameEngine engine) {
        super(engine);
        engine.setScore(0);
        engine.setLife(3);
    }

    void init(long now) {
        playerX = 3;
        playerY = 16 - engine.level;
        if (playerY < 1) {
            playerY = 1;
        }
        int i;
        for (i = 0; i < opening.length; i++) {
            opening[i] = 2;
        }
        straightCounter = 0;
        scoreCounter = 0;
        nextMove = now + period();
        repaint();
    }

    void tick(long now) {
        if (now >= nextMove) {
            nextMove += period();
            advance(now);
        }
    }

    void keyPressed(int action, long now) {
        if (action == GameEngine.ACTION_LEFT && playerX > 0) {
            playerX--;
            repaint();
            verifyCrash();
        } else if (action == GameEngine.ACTION_RIGHT && playerX < 7) {
            playerX++;
            repaint();
            verifyCrash();
        } else if (action == GameEngine.ACTION_UP || action == GameEngine.ACTION_FIRE) {
            advance(now);
            nextMove = now + period();
        }
    }

    private void advance(long now) {
        int i;
        for (i = opening.length - 1; i > 0; i--) {
            opening[i] = opening[i - 1];
        }

        int straightLimit = 11 - engine.level;
        if (straightLimit < 1) {
            straightLimit = 1;
        }
        if (straightCounter < straightLimit) {
            straightCounter++;
            opening[0] = opening[1];
        } else {
            straightCounter = 0;
            opening[0] = opening[1] + (RANDOM.nextBooleanValue() ? 1 : -1);
            if (opening[0] < 1) {
                opening[0] = 1;
            } else if (opening[0] > 4) {
                opening[0] = 4;
            }
        }

        scoreCounter++;
        if (scoreCounter >= 10) {
            scoreCounter = 0;
            engine.addScore(1);
            if (engine.score > 0 && engine.score % 100 == 0) {
                engine.increaseLevel();
                init(now);
                return;
            }
        }
        repaint();
        verifyCrash();
    }

    private void verifyCrash() {
        int y;
        for (y = 0; y < 4; y++) {
            int row = playerY + y;
            if (row < 0 || row >= 20) {
                continue;
            }
            int carBits = CAR[y];
            int x;
            for (x = 0; x < 3; x++) {
                if ((carBits & (1 << (2 - x))) == 0) {
                    continue;
                }
                int boardX = playerX + x;
                if (boardX < opening[row] || boardX > opening[row] + 4) {
                    crash(playerY + 2, playerX + 1);
                    return;
                }
            }
        }
    }

    private int period() {
        int value = 250 - engine.speed * 12;
        return value < 60 ? 60 : value;
    }

    private void repaint() {
        clearBoard();
        int y;
        int x;
        for (y = 0; y < 20; y++) {
            for (x = 0; x < 10; x++) {
                if (x < opening[y] || x > opening[y] + 4) {
                    cell(y, x, true);
                }
            }
        }
        for (y = 0; y < 4; y++) {
            int bits = CAR[y];
            for (x = 0; x < 3; x++) {
                if ((bits & (1 << (2 - x))) != 0) {
                    cell(playerY + y, playerX + x, true);
                }
            }
        }
    }
}
