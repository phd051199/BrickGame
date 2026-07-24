package brickgame;

import java.util.Random;

/** Three-lane obstacle-dodging program inspired by handheld racing variants. */
final class DodgeGame extends Game {

    private static final Random RANDOM = new Random();
    private static final int[] LANES = {0, 3, 6};
    private static final int[] CAR = {0x2, 0x7, 0x2, 0x5};

    private final int[] obstacleY = new int[12];
    private final int[] obstacleLane = new int[12];
    private int obstacleCount;
    private int playerLane;
    private int ticks;
    private long nextMove;

    DodgeGame(GameEngine engine) {
        super(engine);
        engine.setScore(0);
        engine.setLife(3);
    }

    void init(long now) {
        obstacleCount = 0;
        playerLane = 1;
        ticks = 0;
        nextMove = now;
        spawn(-4);
        render();
    }

    void tick(long now) {
        if (now >= nextMove) {
            nextMove += period();
            step();
        }
    }

    void keyPressed(int action, long now) {
        if (action == GameEngine.ACTION_LEFT && playerLane > 0) {
            playerLane--;
            render();
            verifyCollision();
        } else if (action == GameEngine.ACTION_RIGHT && playerLane < 2) {
            playerLane++;
            render();
            verifyCollision();
        } else if (action == GameEngine.ACTION_FIRE
                || action == GameEngine.ACTION_UP) {
            step();
        }
    }

    private void step() {
        int i;
        for (i = 0; i < obstacleCount; i++) {
            obstacleY[i]++;
        }
        ticks++;
        int gap = 8 - engine.level / 3;
        if (gap < 4) {
            gap = 4;
        }
        if (ticks % gap == 0 && obstacleCount < obstacleY.length) {
            spawn(-4);
        }
        for (i = obstacleCount - 1; i >= 0; i--) {
            if (obstacleY[i] > 20) {
                remove(i);
                engine.addScore(20);
            }
        }
        render();
        verifyCollision();
    }

    private void spawn(int y) {
        obstacleY[obstacleCount] = y;
        obstacleLane[obstacleCount] = RANDOM.nextInt(3);
        obstacleCount++;
    }

    private void verifyCollision() {
        int i;
        for (i = 0; i < obstacleCount; i++) {
            if (carsOverlap(16, playerLane, obstacleY[i], obstacleLane[i])) {
                crash(18, LANES[playerLane] + 1);
                return;
            }
        }
    }

    private boolean carsOverlap(int firstY, int firstLane,
                                int secondY, int secondLane) {
        if (firstLane != secondLane) {
            return false;
        }
        return firstY <= secondY + 3 && firstY + 3 >= secondY;
    }

    private void render() {
        clearBoard();
        int y;
        for (y = 0; y < 20; y++) {
            if (((y + ticks) & 3) != 3) {
                cell(y, 9, true);
            }
        }
        drawCar(16, LANES[playerLane]);
        int i;
        for (i = 0; i < obstacleCount; i++) {
            drawCar(obstacleY[i], LANES[obstacleLane[i]]);
        }
    }

    private void drawCar(int top, int left) {
        int y;
        int x;
        for (y = 0; y < 4; y++) {
            for (x = 0; x < 3; x++) {
                if ((CAR[y] & (1 << (2 - x))) != 0) {
                    cell(top + y, left + x, true);
                }
            }
        }
    }

    private void remove(int index) {
        int i;
        for (i = index + 1; i < obstacleCount; i++) {
            obstacleY[i - 1] = obstacleY[i];
            obstacleLane[i - 1] = obstacleLane[i];
        }
        obstacleCount--;
    }

    private int period() {
        int value = 240 - engine.speed * 11;
        return value < 65 ? 65 : value;
    }
}
