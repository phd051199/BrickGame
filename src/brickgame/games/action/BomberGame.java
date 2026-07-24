package brickgame;

import java.util.Random;

/** Blitz-style bomber: descend on each pass and flatten every tower. */
final class BomberGame extends Game {

    private static final Random RANDOM = new Random();

    private final int[] height = new int[10];
    private int planeY;
    private int planeX;
    private boolean bomb;
    private int bombY;
    private int bombX;
    private long nextPlane;
    private long nextBomb;

    BomberGame(GameEngine engine) {
        super(engine);
        engine.setScore(0);
        engine.setLife(3);
    }

    void init(long now) {
        int x;
        int max = 5 + engine.level / 2;
        if (max > 13) {
            max = 13;
        }
        for (x = 0; x < 10; x++) {
            height[x] = 1 + RANDOM.nextInt(max);
        }
        planeY = 1;
        planeX = -2;
        bomb = false;
        nextPlane = now;
        nextBomb = now;
        render();
    }

    void tick(long now) {
        boolean changed = false;
        if (now >= nextPlane) {
            nextPlane += planePeriod();
            movePlane();
            changed = true;
        }
        if (bomb && now >= nextBomb) {
            nextBomb += bombPeriod();
            moveBomb();
            changed = true;
        }
        if (changed && !engine.isLocked()) {
            render();
        }
    }

    void keyPressed(int action, long now) {
        if ((action == GameEngine.ACTION_FIRE || action == GameEngine.ACTION_DOWN)
                && !bomb) {
            bomb = true;
            bombY = planeY + 1;
            bombX = planeX + 1;
            nextBomb = now;
            render();
        } else if (action == GameEngine.ACTION_RIGHT
                || action == GameEngine.ACTION_UP) {
            movePlane();
            if (!engine.isLocked()) {
                render();
            }
        }
    }

    private void movePlane() {
        planeX++;
        if (planeX > 9) {
            planeX = -2;
            planeY++;
        }
        if (planeY >= 19) {
            crash(planeY, planeX + 1);
            return;
        }
        int x;
        for (x = planeX; x < planeX + 3; x++) {
            if (x >= 0 && x < 10 && 20 - height[x] <= planeY) {
                crash(planeY, x);
                return;
            }
        }
    }

    private void moveBomb() {
        bombY++;
        if (bombX < 0 || bombX >= 10 || bombY >= 20) {
            bomb = false;
            return;
        }
        int towerTop = 20 - height[bombX];
        if (height[bombX] > 0 && bombY >= towerTop) {
            height[bombX]--;
            bomb = false;
            engine.addScore(20);
            if (cleared()) {
                engine.addScore(500);
                engine.increaseLevel();
                init(nextPlane);
            }
        }
    }

    private boolean cleared() {
        int x;
        for (x = 0; x < 10; x++) {
            if (height[x] > 0) {
                return false;
            }
        }
        return true;
    }

    private void render() {
        clearBoard();
        int x;
        int y;
        for (x = 0; x < 10; x++) {
            for (y = 19; y >= 20 - height[x]; y--) {
                cell(y, x, true);
            }
        }
        cell(planeY, planeX, true);
        cell(planeY, planeX + 1, true);
        cell(planeY, planeX + 2, true);
        cell(planeY - 1, planeX + 1, true);
        if (bomb) {
            cell(bombY, bombX, true);
        }
    }

    private int planePeriod() {
        int value = 230 - engine.speed * 10;
        return value < 75 ? 75 : value;
    }

    private int bombPeriod() {
        int value = 100 - engine.speed * 3;
        return value < 45 ? 45 : value;
    }
}
