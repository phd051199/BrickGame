package brickgame;

import java.util.Random;

/** Direct CLDC port of game.tanks.* including the persistent battlefield behavior. */
final class TanksGame extends Game {

    private static final Random RANDOM = new Random();

    private static final int UP = 0;
    private static final int RIGHT = 1;
    private static final int DOWN = 2;
    private static final int LEFT = 3;

    private static final int[][] TANK_SHAPES = {
        {0x2, 0x7, 0x5},
        {0x6, 0x3, 0x6},
        {0x5, 0x7, 0x2},
        {0x3, 0x6, 0x3}
    };

    private static final int[][] SPAWN_POINTS = {
        {0, 0}, {0, 7}, {17, 0}, {17, 7}
    };

    private Tank[] tanks = new Tank[16];
    private int tankCount;
    private Shot[] myShots = new Shot[16];
    private int myShotCount;
    private Shot[] shots = new Shot[16];
    private int shotCount;
    private Tank myTank;
    private int nextTankId = 1;
    private long nextTankMove;
    private long nextBullet;

    TanksGame(GameEngine engine) {
        super(engine);
    }

    void init(long now) {
        engine.setScore(0);
        myTank = new Tank(nextTankId++, 9, 4, randomDirection());
        int i;
        for (i = 0; i < SPAWN_POINTS.length; i++) {
            addTank(new Tank(nextTankId++, SPAWN_POINTS[i][0], SPAWN_POINTS[i][1], randomDirection()));
        }
        addTank(myTank);
        nextTankMove = now;
        nextBullet = now;
    }

    void tick(long now) {
        if (now >= nextTankMove) {
            nextTankMove = now + tankPeriod();
            tanksMove();
        }
        if (now >= nextBullet) {
            nextBullet = now + 150L;
            bulletFlight();
        }
    }

    void keyPressed(int action, long now) {
        boolean changed = false;
        if (action == GameEngine.ACTION_DOWN) {
            changed = doDirection(myTank, DOWN);
        } else if (action == GameEngine.ACTION_LEFT) {
            changed = doDirection(myTank, LEFT);
        } else if (action == GameEngine.ACTION_RIGHT) {
            changed = doDirection(myTank, RIGHT);
        } else if (action == GameEngine.ACTION_UP) {
            changed = doDirection(myTank, UP);
        } else if (action == GameEngine.ACTION_FIRE) {
            addMyShot(myTank.doShot());
            repaint();
            return;
        }
        if (changed) {
            repaint();
        }
    }

    private void tanksMove() {
        int i;
        for (i = 0; i < tankCount; i++) {
            Tank tank = tanks[i];
            if (tank != myTank) {
                doNextAiStep(tank);
            }
        }
        if (tankCount < 5) {
            spawn(4);
        }
        repaint();
    }

    private boolean doNextAiStep(Tank tank) {
        int attempt;
        for (attempt = 0; attempt < 5; attempt++) {
            int decision = RANDOM.nextInt(4);
            if (decision == 0) {
                if (doDirection(tank, randomDirection())) {
                    return true;
                }
            } else if (decision == 1 || decision == 2) {
                if (doDirection(tank, tank.direction)) {
                    return true;
                }
            } else {
                addShot(tank.doShot());
                return true;
            }
        }
        return false;
    }

    private boolean doDirection(Tank tank, int direction) {
        int steps = canMove(tank, direction);
        int i;
        for (i = 0; i < steps; i++) {
            tank.step(direction);
        }
        return steps > 0;
    }

    private int canMove(Tank tank, int direction) {
        Tank clone = new Tank(tank);
        clone.step(direction);
        if (clone.outside()) {
            return 0;
        }
        if (!hasOverlap(clone)) {
            return 1;
        }
        if (tank.direction != opposite(direction)) {
            return 0;
        }
        clone.step(direction);
        if (clone.outside() || hasOverlap(clone)) {
            return 0;
        }
        return 2;
    }

    private boolean hasOverlap(Tank candidate) {
        int i;
        int point;
        for (i = 0; i < tankCount; i++) {
            Tank tank = tanks[i];
            if (tank.id == candidate.id) {
                continue;
            }
            for (point = 0; point < tank.pointCount; point++) {
                if (candidate.contains(tank.y[point], tank.x[point])) {
                    return true;
                }
            }
        }
        return false;
    }

    private void spawn(int attempts) {
        if (attempts == 0) {
            return;
        }
        int[] point = SPAWN_POINTS[RANDOM.nextInt(SPAWN_POINTS.length)];
        Tank tank = new Tank(nextTankId++, point[0], point[1], randomDirection());
        if (overlapsShadow(tank)) {
            spawn(attempts - 1);
            return;
        }
        addTank(tank);
    }

    private boolean overlapsShadow(Tank candidate) {
        int i;
        int dy;
        int dx;
        for (i = 0; i < tankCount; i++) {
            Tank existing = tanks[i];
            if (existing.id == candidate.id) {
                continue;
            }
            for (dy = 0; dy < 3; dy++) {
                for (dx = 0; dx < 3; dx++) {
                    if (candidate.contains(existing.pointY + dy, existing.pointX + dx)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void bulletFlight() {
        int i;
        for (i = 0; i < myShotCount; i++) {
            myShots[i].move();
        }
        for (i = 0; i < shotCount; i++) {
            shots[i].move();
        }

        boolean[] killedTank = new boolean[tankCount];
        boolean[] usedMyShot = new boolean[myShotCount];
        int killed = 0;
        int tankIndex;
        int shotIndex;
        for (tankIndex = 0; tankIndex < tankCount; tankIndex++) {
            Tank tank = tanks[tankIndex];
            if (tank == myTank) {
                continue;
            }
            for (shotIndex = 0; shotIndex < myShotCount; shotIndex++) {
                if (tank.hasKilled(myShots[shotIndex])) {
                    killedTank[tankIndex] = true;
                    usedMyShot[shotIndex] = true;
                    killed++;
                    break;
                }
            }
        }
        for (tankIndex = tankCount - 1; tankIndex >= 0; tankIndex--) {
            if (killedTank[tankIndex]) {
                removeTank(tankIndex);
            }
        }
        for (shotIndex = myShotCount - 1; shotIndex >= 0; shotIndex--) {
            if (usedMyShot[shotIndex]) {
                removeMyShot(shotIndex);
            }
        }
        repaint();
        engine.addScore(killed * 100);

        for (i = 0; i < shotCount; i++) {
            if (myTank.hasKilled(shots[i])) {
                crash(shots[i].y, shots[i].x);
                break;
            }
        }

        for (i = myShotCount - 1; i >= 0; i--) {
            if (myShots[i].outside()) {
                removeMyShot(i);
            }
        }
        for (i = shotCount - 1; i >= 0; i--) {
            if (shots[i].outside()) {
                removeShot(i);
            }
        }
    }

    private void repaint() {
        clearBoard();
        int i;
        int p;
        for (i = 0; i < shotCount; i++) {
            cell(shots[i].y, shots[i].x, true);
        }
        for (i = 0; i < myShotCount; i++) {
            cell(myShots[i].y, myShots[i].x, true);
        }
        for (i = 0; i < tankCount; i++) {
            for (p = 0; p < tanks[i].pointCount; p++) {
                cell(tanks[i].y[p], tanks[i].x[p], true);
            }
        }
    }

    private int tankPeriod() {
        return 1000 - engine.speed * 45;
    }

    private int randomDirection() {
        return RANDOM.nextInt(4);
    }

    private void addTank(Tank tank) {
        ensureTankCapacity(tankCount + 1);
        tanks[tankCount++] = tank;
    }

    private void removeTank(int index) {
        int i;
        for (i = index + 1; i < tankCount; i++) {
            tanks[i - 1] = tanks[i];
        }
        tanks[--tankCount] = null;
    }

    private void addMyShot(Shot shot) {
        ensureMyShotCapacity(myShotCount + 1);
        myShots[myShotCount++] = shot;
    }

    private void removeMyShot(int index) {
        int i;
        for (i = index + 1; i < myShotCount; i++) {
            myShots[i - 1] = myShots[i];
        }
        myShots[--myShotCount] = null;
    }

    private void addShot(Shot shot) {
        ensureShotCapacity(shotCount + 1);
        shots[shotCount++] = shot;
    }

    private void removeShot(int index) {
        int i;
        for (i = index + 1; i < shotCount; i++) {
            shots[i - 1] = shots[i];
        }
        shots[--shotCount] = null;
    }

    private void ensureTankCapacity(int capacity) {
        if (capacity <= tanks.length) {
            return;
        }
        Tank[] next = new Tank[tanks.length * 2];
        System.arraycopy(tanks, 0, next, 0, tankCount);
        tanks = next;
    }

    private void ensureMyShotCapacity(int capacity) {
        if (capacity <= myShots.length) {
            return;
        }
        Shot[] next = new Shot[myShots.length * 2];
        System.arraycopy(myShots, 0, next, 0, myShotCount);
        myShots = next;
    }

    private void ensureShotCapacity(int capacity) {
        if (capacity <= shots.length) {
            return;
        }
        Shot[] next = new Shot[shots.length * 2];
        System.arraycopy(shots, 0, next, 0, shotCount);
        shots = next;
    }

    private static int opposite(int direction) {
        if (direction == UP) {
            return DOWN;
        }
        if (direction == RIGHT) {
            return LEFT;
        }
        if (direction == DOWN) {
            return UP;
        }
        return RIGHT;
    }

    private static final class Shot {
        int y;
        int x;
        final int direction;

        Shot(int y, int x, int direction) {
            this.y = y;
            this.x = x;
            this.direction = direction;
        }

        void move() {
            if (direction == UP) {
                y--;
            } else if (direction == RIGHT) {
                x++;
            } else if (direction == DOWN) {
                y++;
            } else {
                x--;
            }
        }

        boolean outside() {
            return x < 0 || x > 9 || y < 0 || y > 19;
        }
    }

    private static final class Tank {
        final int id;
        final int[] y = new int[9];
        final int[] x = new int[9];
        int pointCount;
        int pointY;
        int pointX;
        int direction;

        Tank(int id, int pointY, int pointX, int direction) {
            this.id = id;
            this.pointY = pointY;
            this.pointX = pointX;
            this.direction = direction;
            rebuild();
        }

        Tank(Tank source) {
            id = source.id;
            pointY = source.pointY;
            pointX = source.pointX;
            direction = source.direction;
            pointCount = source.pointCount;
            int i;
            for (i = 0; i < pointCount; i++) {
                y[i] = source.y[i];
                x[i] = source.x[i];
            }
        }

        void step(int nextDirection) {
            if (nextDirection == direction) {
                int i;
                for (i = 0; i < pointCount; i++) {
                    movePoint(i, nextDirection);
                }
                if (nextDirection == UP) {
                    pointY--;
                } else if (nextDirection == RIGHT) {
                    pointX++;
                } else if (nextDirection == DOWN) {
                    pointY++;
                } else {
                    pointX--;
                }
            } else {
                direction = nextDirection;
                rebuild();
            }
        }

        Shot doShot() {
            int shotY = pointY + 1;
            int shotX = pointX + 1;
            if (direction == UP) {
                shotY--;
            } else if (direction == DOWN) {
                shotY++;
            } else if (direction == LEFT) {
                shotX--;
            } else if (direction == RIGHT) {
                shotX++;
            }
            return new Shot(shotY, shotX, direction);
        }

        boolean outside() {
            return pointX < 0 || pointX > 7 || pointY < 0 || pointY > 17;
        }

        boolean contains(int targetY, int targetX) {
            int i;
            for (i = 0; i < pointCount; i++) {
                if (y[i] == targetY && x[i] == targetX) {
                    return true;
                }
            }
            return false;
        }

        boolean hasKilled(Shot shot) {
            return contains(shot.y, shot.x);
        }

        private void rebuild() {
            pointCount = 0;
            int row;
            int col;
            for (row = 0; row < 3; row++) {
                for (col = 0; col < 3; col++) {
                    if ((TANK_SHAPES[direction][row] & (1 << (2 - col))) != 0) {
                        y[pointCount] = pointY + row;
                        x[pointCount] = pointX + col;
                        pointCount++;
                    }
                }
            }
        }

        private void movePoint(int index, int moveDirection) {
            if (moveDirection == UP) {
                y[index]--;
            } else if (moveDirection == RIGHT) {
                x[index]++;
            } else if (moveDirection == DOWN) {
                y[index]++;
            } else {
                x[index]--;
            }
        }
    }
}
