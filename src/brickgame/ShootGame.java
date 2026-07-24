package brickgame;

/** Direct CLDC port of game.shoot.ShootGame, Army and Gun. */
final class ShootGame extends Game {

    private static final OriginalRandom RANDOM = new OriginalRandom();

    private int[] enemyY = new int[32];
    private int[] enemyX = new int[32];
    private int enemyCount;
    private int[] shotY = new int[32];
    private int[] shotX = new int[32];
    private int shotCount;
    private int gunX;
    private long nextAttack;
    private long nextShoot;

    ShootGame(GameEngine engine) {
        super(engine);
        engine.setLife(3);
        engine.setScore(0);
    }

    void init(long now) {
        enemyCount = 0;
        shotCount = 0;
        gunX = 4;
        nextAttack = now;
        nextShoot = now;
    }

    void tick(long now) {
        if (now >= nextAttack) {
            nextAttack = now + attackPeriod();
            doAttack();
            if (engine.isLocked()) {
                return;
            }
        }
        if (now >= nextShoot) {
            nextShoot = now + 25L;
            doShoot();
        }
    }

    void keyPressed(int action, long now) {
        if (action == GameEngine.ACTION_LEFT) {
            if (gunX >= 1) {
                gunX--;
            }
            repaint();
        } else if (action == GameEngine.ACTION_RIGHT) {
            if (gunX <= 8) {
                gunX++;
            }
            repaint();
        } else if (action == GameEngine.ACTION_UP || action == GameEngine.ACTION_FIRE) {
            addShot(19, gunX);
            repaint();
        }
    }

    private void doShoot() {
        int i;
        for (i = 0; i < shotCount; i++) {
            shotY[i]--;
        }

        boolean[] hitShot = new boolean[shotCount];
        boolean[] hitEnemy = new boolean[enemyCount];
        int enemy;
        for (i = 0; i < shotCount; i++) {
            for (enemy = 0; enemy < enemyCount; enemy++) {
                if (shotY[i] == enemyY[enemy] && shotX[i] == enemyX[enemy]) {
                    hitShot[i] = true;
                    hitEnemy[enemy] = true;
                }
            }
        }

        boolean killed = false;
        for (i = shotCount - 1; i >= 0; i--) {
            if (hitShot[i]) {
                removeShot(i);
            }
        }
        for (i = enemyCount - 1; i >= 0; i--) {
            if (hitEnemy[i]) {
                removeEnemy(i);
                killed = true;
            }
        }
        if (killed) {
            engine.addScore(10);
        }
        repaint();
    }

    private void doAttack() {
        int i;
        for (i = 0; i < enemyCount; i++) {
            enemyY[i]++;
        }
        for (i = 0; i < 10; i++) {
            if (RANDOM.nextBooleanValue()) {
                addEnemy(0, i);
            }
        }
        repaint();
        for (i = 0; i < enemyCount; i++) {
            if (enemyY[i] > 19) {
                crash(19, gunX);
                return;
            }
        }
    }

    private void repaint() {
        clearBoard();
        cell(19, gunX, true);
        int i;
        for (i = 0; i < shotCount; i++) {
            cell(shotY[i], shotX[i], true);
        }
        for (i = 0; i < enemyCount; i++) {
            cell(enemyY[i], enemyX[i], true);
        }
    }

    private int attackPeriod() {
        return 1000 - engine.speed * 40;
    }

    private void addEnemy(int y, int x) {
        ensureEnemyCapacity(enemyCount + 1);
        enemyY[enemyCount] = y;
        enemyX[enemyCount] = x;
        enemyCount++;
    }

    private void addShot(int y, int x) {
        ensureShotCapacity(shotCount + 1);
        shotY[shotCount] = y;
        shotX[shotCount] = x;
        shotCount++;
    }

    private void removeEnemy(int index) {
        int i;
        for (i = index + 1; i < enemyCount; i++) {
            enemyY[i - 1] = enemyY[i];
            enemyX[i - 1] = enemyX[i];
        }
        enemyCount--;
    }

    private void removeShot(int index) {
        int i;
        for (i = index + 1; i < shotCount; i++) {
            shotY[i - 1] = shotY[i];
            shotX[i - 1] = shotX[i];
        }
        shotCount--;
    }

    private void ensureEnemyCapacity(int capacity) {
        if (capacity <= enemyY.length) {
            return;
        }
        int size = enemyY.length * 2;
        int[] nextY = new int[size];
        int[] nextX = new int[size];
        System.arraycopy(enemyY, 0, nextY, 0, enemyCount);
        System.arraycopy(enemyX, 0, nextX, 0, enemyCount);
        enemyY = nextY;
        enemyX = nextX;
    }

    private void ensureShotCapacity(int capacity) {
        if (capacity <= shotY.length) {
            return;
        }
        int size = shotY.length * 2;
        int[] nextY = new int[size];
        int[] nextX = new int[size];
        System.arraycopy(shotY, 0, nextY, 0, shotCount);
        System.arraycopy(shotX, 0, nextX, 0, shotCount);
        shotY = nextY;
        shotX = nextX;
    }
}
