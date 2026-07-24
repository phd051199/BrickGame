package brickgame;

import java.util.Random;

/** Allocation-free tank battle with one active projectile per tank. */
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

    private static final int[][] PLAYER_TANK_SHAPES = {
        {0x2, 0x7, 0x7},
        {0x6, 0x7, 0x6},
        {0x7, 0x7, 0x2},
        {0x3, 0x7, 0x3}
    };

    private static final int[][] SPAWN_POINTS = {
        {0, 0}, {0, 7}, {17, 0}, {17, 7}
    };

    private final Tank[] tanks = new Tank[16];
    private final ProjectilePool playerShots = new ProjectilePool(16);
    private final ProjectilePool enemyShots = new ProjectilePool(16);
    private final Tank movementProbe = new Tank(-1, 0, 0, UP);

    private int tankCount;
    private Tank myTank;
    private int nextTankId = 1;
    private long nextTankMove;
    private long nextBullet;

    TanksGame(GameEngine engine) {
        super(engine);
        engine.setScore(0);
        engine.setLife(3);
    }

    void init(long now) {
        clearBattlefield();
        myTank = new Tank(nextTankId++, 9, 4, UP, true);
        int i;
        for (i = 0; i < SPAWN_POINTS.length; i++) {
            addTank(new Tank(nextTankId++, SPAWN_POINTS[i][0],
                SPAWN_POINTS[i][1], randomDirection()));
        }
        addTank(myTank);
        nextTankMove = now;
        nextBullet = now;
        repaint();
    }

    void tick(long now) {
        if (now >= nextTankMove) {
            nextTankMove += tankPeriod();
            tanksMove(now);
        }
        if (now >= nextBullet) {
            nextBullet += FAST_PROJECTILE_PERIOD;
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
            changed = tryFire(myTank, true, now);
        }
        if (changed) {
            repaint();
        }
    }

    private void clearBattlefield() {
        int i;
        for (i = 0; i < tankCount; i++) {
            tanks[i] = null;
        }
        tankCount = 0;
        playerShots.clear();
        enemyShots.clear();
    }

    private void tanksMove(long now) {
        int i;
        for (i = 0; i < tankCount; i++) {
            Tank tank = tanks[i];
            if (tank != myTank) {
                doNextAiStep(tank, now);
            }
        }
        if (tankCount < 5) {
            spawn(4);
        }
        repaint();
    }

    private boolean doNextAiStep(Tank tank, long now) {
        int attackDirection = alignedAttackDirection(tank);
        if (attackDirection >= 0) {
            if (tank.direction != attackDirection
                    && doDirection(tank, attackDirection)) {
                return true;
            }
            if (tank.direction == attackDirection
                    && tryFire(tank, false, now)) {
                return true;
            }
        }

        int primary = primaryChaseDirection(tank);
        int secondary = secondaryChaseDirection(tank, primary);
        if (tank.direction == primary && doDirection(tank, primary)) {
            return true;
        }
        if (tank.direction == secondary && doDirection(tank, secondary)) {
            return true;
        }
        if (doDirection(tank, primary)) {
            return true;
        }
        if (doDirection(tank, secondary)) {
            return true;
        }

        int sidestep = (tank.id & 1) == 0
            ? clockwise(primary) : counterClockwise(primary);
        if (doDirection(tank, sidestep)) {
            return true;
        }
        if (doDirection(tank, opposite(sidestep))) {
            return true;
        }
        return doDirection(tank, opposite(primary));
    }

    private int alignedAttackDirection(Tank tank) {
        int dy = myTank.pointY - tank.pointY;
        int dx = myTank.pointX - tank.pointX;
        if (absolute(dx) <= 1) {
            return dy < 0 ? UP : DOWN;
        }
        if (absolute(dy) <= 1) {
            return dx < 0 ? LEFT : RIGHT;
        }
        return -1;
    }

    private int primaryChaseDirection(Tank tank) {
        int dy = myTank.pointY - tank.pointY;
        int dx = myTank.pointX - tank.pointX;
        int vertical = dy < 0 ? UP : DOWN;
        int horizontal = dx < 0 ? LEFT : RIGHT;
        int verticalDistance = absolute(dy);
        int horizontalDistance = absolute(dx);
        if (verticalDistance == horizontalDistance) {
            return (tank.id & 1) == 0 ? vertical : horizontal;
        }
        return verticalDistance > horizontalDistance ? vertical : horizontal;
    }

    private int secondaryChaseDirection(Tank tank, int primary) {
        int dy = myTank.pointY - tank.pointY;
        int dx = myTank.pointX - tank.pointX;
        if (primary == UP || primary == DOWN) {
            return dx < 0 ? LEFT : RIGHT;
        }
        return dy < 0 ? UP : DOWN;
    }

    private static int clockwise(int direction) {
        return (direction + 1) & 3;
    }

    private static int counterClockwise(int direction) {
        return (direction + 3) & 3;
    }

    private static int absolute(int value) {
        return value < 0 ? -value : value;
    }

    private boolean tryFire(Tank tank, boolean player, long now) {
        long cooldown = player ? 0L : 620L - engine.speed * 12L;
        if (cooldown < 320L && !player) {
            cooldown = 320L;
        }
        if (now - tank.lastShotAt < cooldown) {
            return false;
        }
        ProjectilePool pool = player ? playerShots : enemyShots;
        if (pool.hasOwner(tank.id)) {
            return false;
        }
        int shotY = tank.pointY + 1;
        int shotX = tank.pointX + 1;
        if (tank.direction == UP) {
            shotY = tank.pointY - 1;
        } else if (tank.direction == RIGHT) {
            shotX = tank.pointX + 3;
        } else if (tank.direction == DOWN) {
            shotY = tank.pointY + 3;
        } else {
            shotX = tank.pointX - 1;
        }
        if (pool.add(shotY, shotX, tank.direction, tank.id)) {
            tank.lastShotAt = now;
            return true;
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
        movementProbe.copyGeometry(tank);
        movementProbe.step(direction);
        if (movementProbe.outside()) {
            return 0;
        }
        if (!hasOverlap(movementProbe, tank.id)) {
            return 1;
        }
        if (tank.direction != opposite(direction)) {
            return 0;
        }
        movementProbe.step(direction);
        if (movementProbe.outside() || hasOverlap(movementProbe, tank.id)) {
            return 0;
        }
        return 2;
    }

    private boolean hasOverlap(Tank candidate, int ignoredId) {
        int i;
        int point;
        for (i = 0; i < tankCount; i++) {
            Tank tank = tanks[i];
            if (tank.id == ignoredId) {
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
        int attempt;
        for (attempt = 0; attempt < attempts; attempt++) {
            int[] point = SPAWN_POINTS[RANDOM.nextInt(SPAWN_POINTS.length)];
            Tank tank = new Tank(nextTankId++, point[0], point[1],
                randomDirection());
            if (!overlapsShadow(tank)) {
                addTank(tank);
                return;
            }
        }
    }

    private boolean overlapsShadow(Tank candidate) {
        int i;
        int dy;
        int dx;
        for (i = 0; i < tankCount; i++) {
            Tank existing = tanks[i];
            for (dy = 0; dy < 3; dy++) {
                for (dx = 0; dx < 3; dx++) {
                    if (candidate.contains(existing.pointY + dy,
                            existing.pointX + dx)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void bulletFlight() {
        playerShots.moveAll();
        enemyShots.moveAll();
        cancelOpposingShots();
        hitEnemyTanks();
        if (hitPlayer()) {
            repaint();
            return;
        }
        removeOutside(playerShots);
        removeOutside(enemyShots);
        repaint();
    }

    private void cancelOpposingShots() {
        int i;
        int j;
        for (i = playerShots.size() - 1; i >= 0; i--) {
            for (j = enemyShots.size() - 1; j >= 0; j--) {
                if (playerShots.meets(i, enemyShots, j)) {
                    playerShots.remove(i);
                    enemyShots.remove(j);
                    break;
                }
            }
        }
    }

    private void hitEnemyTanks() {
        int shotIndex;
        int tankIndex;
        for (shotIndex = playerShots.size() - 1; shotIndex >= 0; shotIndex--) {
            for (tankIndex = tankCount - 1; tankIndex >= 0; tankIndex--) {
                Tank tank = tanks[tankIndex];
                if (tank != myTank && tank.contains(playerShots.y(shotIndex),
                        playerShots.x(shotIndex))) {
                    playerShots.remove(shotIndex);
                    removeTank(tankIndex);
                    engine.addScore(100);
                    break;
                }
            }
        }
    }

    private boolean hitPlayer() {
        int i;
        for (i = enemyShots.size() - 1; i >= 0; i--) {
            if (myTank.contains(enemyShots.y(i), enemyShots.x(i))) {
                int y = enemyShots.y(i);
                int x = enemyShots.x(i);
                enemyShots.remove(i);
                crash(y, x);
                return true;
            }
        }
        return false;
    }

    private static void removeOutside(ProjectilePool pool) {
        int i;
        for (i = pool.size() - 1; i >= 0; i--) {
            if (pool.outside(i)) {
                pool.remove(i);
            }
        }
    }

    private void repaint() {
        clearBoard();
        int i;
        int p;
        for (i = 0; i < enemyShots.size(); i++) {
            cell(enemyShots.y(i), enemyShots.x(i), true);
        }
        for (i = 0; i < playerShots.size(); i++) {
            cell(playerShots.y(i), playerShots.x(i), true);
        }
        for (i = 0; i < tankCount; i++) {
            for (p = 0; p < tanks[i].pointCount; p++) {
                cell(tanks[i].y[p], tanks[i].x[p], true);
            }
        }
    }

    private int tankPeriod() {
        int value = 1000 - engine.speed * 45;
        return value < 280 ? 280 : value;
    }

    private int randomDirection() {
        return RANDOM.nextInt(4);
    }

    private void addTank(Tank tank) {
        if (tankCount < tanks.length) {
            tanks[tankCount++] = tank;
        }
    }

    private void removeTank(int index) {
        int i;
        for (i = index + 1; i < tankCount; i++) {
            tanks[i - 1] = tanks[i];
        }
        tanks[--tankCount] = null;
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

    private static final class Tank {
        final int id;
        final int[] y = new int[9];
        final int[] x = new int[9];
        int pointCount;
        int pointY;
        int pointX;
        int direction;
        boolean playerBody;
        long lastShotAt = -10000L;

        Tank(int id, int pointY, int pointX, int direction) {
            this(id, pointY, pointX, direction, false);
        }

        Tank(int id, int pointY, int pointX, int direction,
             boolean playerBody) {
            this.id = id;
            this.pointY = pointY;
            this.pointX = pointX;
            this.direction = direction;
            this.playerBody = playerBody;
            rebuild();
        }

        void copyGeometry(Tank source) {
            pointY = source.pointY;
            pointX = source.pointX;
            direction = source.direction;
            playerBody = source.playerBody;
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

        private void rebuild() {
            pointCount = 0;
            int row;
            int col;
            int[][] shapes = playerBody ? PLAYER_TANK_SHAPES : TANK_SHAPES;
            for (row = 0; row < 3; row++) {
                for (col = 0; col < 3; col++) {
                    if ((shapes[direction][row]
                            & (1 << (2 - col))) != 0) {
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
