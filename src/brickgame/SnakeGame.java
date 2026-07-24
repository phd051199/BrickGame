package brickgame;

import java.util.Random;

/** Direct CLDC port of game.snake.SnakeGame and its model classes. */
final class SnakeGame extends Game {

    private static final Random RANDOM = new Random();

    private static final int NOT_DEFINED = 0;
    private static final int LEFT = 1;
    private static final int RIGHT = 2;
    private static final int UP = 3;
    private static final int DOWN = 4;

    private final int[] snakeY = new int[200];
    private final int[] snakeX = new int[200];
    private final boolean[][] map = new boolean[20][10];

    private int snakeSize;
    private int mouseY;
    private int mouseX;
    private int direction;
    private boolean keyPressed;
    private long nextMove;

    SnakeGame(GameEngine engine) {
        super(engine);
        engine.setLife(3);
        engine.setScore(0);
    }

    void init(long now) {
        SnakeMaps.copy(engine.level, map);
        snakeSize = 3;
        snakeY[0] = 0;
        snakeX[0] = 0;
        snakeY[1] = 0;
        snakeX[1] = 1;
        snakeY[2] = 0;
        snakeX[2] = 2;
        generateMouse();
        direction = NOT_DEFINED;
        nextMove = now;
        repaint();
    }

    void tick(long now) {
        if (now < nextMove) {
            return;
        }
        nextMove = now + period();
        onMove(now);
    }

    void keyPressed(int action, long now) {
        if (action == GameEngine.ACTION_DOWN) {
            if (doStep(DOWN, now)) {
                keyPressed = true;
            }
        } else if (action == GameEngine.ACTION_LEFT) {
            if (direction != NOT_DEFINED && doStep(LEFT, now)) {
                keyPressed = true;
            }
        } else if (action == GameEngine.ACTION_RIGHT) {
            if (doStep(RIGHT, now)) {
                keyPressed = true;
            }
        } else if (action == GameEngine.ACTION_UP) {
            if (direction != NOT_DEFINED && doStep(UP, now)) {
                keyPressed = true;
            }
        } else if (action == GameEngine.ACTION_FIRE) {
            onMove(now);
        }
    }

    private void onMove(long now) {
        if (direction == NOT_DEFINED) {
            return;
        }
        if (keyPressed) {
            keyPressed = false;
            return;
        }
        doStep(direction, now);
    }

    private boolean doStep(int course, long now) {
        if (direction == reverse(course)) {
            return false;
        }
        direction = course;

        int headIndex = snakeSize;
        snakeY[headIndex] = snakeY[snakeSize - 1];
        snakeX[headIndex] = snakeX[snakeSize - 1];
        snakeSize++;
        movePoint(headIndex, course);

        int headY = snakeY[headIndex];
        int headX = snakeX[headIndex];
        if (headX < 0 || headX >= 10 || headY < 0 || headY >= 20) {
            crash(headY, headX);
            return true;
        }

        boolean ate = headY == mouseY && headX == mouseX;
        if (ate) {
            generateMouse();
            engine.addScore(10);
        } else {
            removeTail();
            headIndex--;
            headY = snakeY[headIndex];
            headX = snakeX[headIndex];
        }

        int i;
        for (i = 0; i < snakeSize - 1; i++) {
            if (snakeY[i] == headY && snakeX[i] == headX) {
                crash(headY, headX);
                return true;
            }
        }
        if (map[headY][headX]) {
            crash(headY, headX);
            return true;
        }

        repaint();
        if (snakeSize > 33) {
            if (engine.increaseSpeed() == 0) {
                engine.increaseLevel();
            }
            init(now);
        }
        return true;
    }

    private void removeTail() {
        int i;
        for (i = 1; i < snakeSize; i++) {
            snakeY[i - 1] = snakeY[i];
            snakeX[i - 1] = snakeX[i];
        }
        snakeSize--;
    }

    private void generateMouse() {
        for (;;) {
            int y = RANDOM.nextInt(20);
            int x = RANDOM.nextInt(10);
            if (!map[y][x] && !snakeContains(y, x)) {
                mouseY = y;
                mouseX = x;
                return;
            }
        }
    }

    private boolean snakeContains(int y, int x) {
        int i;
        for (i = 0; i < snakeSize; i++) {
            if (snakeY[i] == y && snakeX[i] == x) {
                return true;
            }
        }
        return false;
    }

    private void repaint() {
        clearBoard();
        int y;
        int x;
        int i;
        for (y = 0; y < 20; y++) {
            for (x = 0; x < 10; x++) {
                if (map[y][x]) {
                    cell(y, x, true);
                }
            }
        }
        for (i = 0; i < snakeSize; i++) {
            cell(snakeY[i], snakeX[i], true);
        }
        cell(mouseY, mouseX, true);
    }

    private int period() {
        return 300 - engine.speed * 15;
    }

    private void movePoint(int index, int course) {
        if (course == LEFT) {
            snakeX[index]--;
        } else if (course == RIGHT) {
            snakeX[index]++;
        } else if (course == UP) {
            snakeY[index]--;
        } else if (course == DOWN) {
            snakeY[index]++;
        }
    }

    private static int reverse(int course) {
        if (course == LEFT) {
            return RIGHT;
        }
        if (course == RIGHT) {
            return LEFT;
        }
        if (course == UP) {
            return DOWN;
        }
        if (course == DOWN) {
            return UP;
        }
        return NOT_DEFINED;
    }
}
