package brickgame;

/** Program F: three-lane racing with paired cars and a moving safe lane. */
final class HighwayGame extends Game {

    private static final OriginalRandom RANDOM = new OriginalRandom();
    private static final int[] CAR = {0x2, 0x7, 0x2, 0x5};

    private final int[] enemyX = new int[6];
    private final int[] enemyY = new int[6];
    private final int[] hole = new int[3];

    private int lane;
    private int playerY;
    private int borderPhase;
    private long nextMove;

    HighwayGame(GameEngine engine) {
        super(engine);
        engine.setScore(0);
        engine.setLife(3);
    }

    void init(long now) {
        lane = 1;
        playerY = 16 - engine.level;
        if (playerY < 4) {
            playerY = 4;
        }
        borderPhase = 0;
        hole[0] = 3;
        int i;
        for (i = 0; i < 3; i++) {
            if (i > 0) {
                hole[i] = hole[i - 1];
            }
            chooseNextHole(i);
            setPair(i, -5 - 9 * i);
        }
        nextMove = now + period();
        repaint();
    }

    void tick(long now) {
        if (now >= nextMove) {
            nextMove = now + period();
            advance(now);
        }
    }

    void keyPressed(int action, long now) {
        if (action == GameEngine.ACTION_LEFT && lane > 0) {
            lane--;
            repaint();
            verifyCrash();
        } else if (action == GameEngine.ACTION_RIGHT && lane < 2) {
            lane++;
            repaint();
            verifyCrash();
        } else if (action == GameEngine.ACTION_UP || action == GameEngine.ACTION_FIRE) {
            advance(now);
            nextMove = now + period();
        }
    }

    private void advance(long now) {
        borderPhase = (borderPhase + 1) & 3;
        int i;
        for (i = 0; i < 3; i++) {
            enemyY[i]++;
            enemyY[i + 3]++;
            if (enemyY[i] == playerY) {
                engine.addScore(1);
                if (engine.score > 0 && engine.score % 100 == 0) {
                    engine.increaseLevel();
                    init(now);
                    return;
                }
            }
            if (enemyY[i] >= 20) {
                hole[i] = i > 0 ? hole[i - 1] : hole[2];
                chooseNextHole(i);
                setPair(i, -7);
            }
        }
        repaint();
        verifyCrash();
    }

    private void chooseNextHole(int index) {
        if (RANDOM.nextBooleanValue() && hole[index] != 0) {
            hole[index] -= 3;
        } else if (hole[index] != 6) {
            hole[index] += 3;
        } else {
            hole[index] -= 3;
        }
    }

    private void setPair(int pair, int y) {
        if (hole[pair] == 0) {
            enemyX[pair] = 3;
            enemyX[pair + 3] = 6;
        } else if (hole[pair] == 3) {
            enemyX[pair] = 0;
            enemyX[pair + 3] = 6;
        } else {
            enemyX[pair] = 0;
            enemyX[pair + 3] = 3;
        }
        enemyY[pair] = y;
        enemyY[pair + 3] = y;
    }

    private void verifyCrash() {
        int playerX = lane * 3;
        int i;
        for (i = 0; i < 6; i++) {
            if (enemyX[i] == playerX
                    && playerY <= enemyY[i] + 3
                    && playerY + 3 >= enemyY[i]) {
                crash(playerY + 2, playerX + 1);
                return;
            }
        }
    }

    private int period() {
        int value = 210 - engine.speed * 10;
        return value < 55 ? 55 : value;
    }

    private void repaint() {
        clearBoard();
        int y;
        for (y = 0; y < 20; y++) {
            if (((y + borderPhase) & 3) != 3) {
                cell(y, 9, true);
            }
        }
        drawCar(playerY, lane * 3);
        int i;
        for (i = 0; i < 6; i++) {
            drawCar(enemyY[i], enemyX[i]);
        }
    }

    private void drawCar(int top, int left) {
        int y;
        int x;
        for (y = 0; y < 4; y++) {
            int bits = CAR[y];
            for (x = 0; x < 3; x++) {
                if ((bits & (1 << (2 - x))) != 0) {
                    cell(top + y, left + x, true);
                }
            }
        }
    }
}
