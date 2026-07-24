package brickgame;

/** Allocation-free port of the descending army and gun program. */
final class ShootGame extends Game {

    private static final OriginalRandom RANDOM = new OriginalRandom();
    private static final int MAX_ENEMIES = 256;
    private static final int MAX_SHOTS = 128;

    private final int[] enemyY = new int[MAX_ENEMIES];
    private final int[] enemyX = new int[MAX_ENEMIES];
    private final int[] shotY = new int[MAX_SHOTS];
    private final int[] shotX = new int[MAX_SHOTS];

    private int enemyCount;
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
        repaint();
    }

    void tick(long now) {
        if (now >= nextAttack) {
            nextAttack += attackPeriod();
            doAttack();
            if (engine.isLocked()) {
                return;
            }
        }
        if (now >= nextShoot) {
            nextShoot += FAST_PROJECTILE_PERIOD;
            doShoot();
        }
    }

    void keyPressed(int action, long now) {
        if (action == GameEngine.ACTION_LEFT) {
            if (gunX > 0) {
                gunX--;
            }
            repaint();
        } else if (action == GameEngine.ACTION_RIGHT) {
            if (gunX < 9) {
                gunX++;
            }
            repaint();
        } else if (action == GameEngine.ACTION_UP || action == GameEngine.ACTION_FIRE) {
            addShot(18, gunX);
            repaint();
        }
    }

    private void doShoot() {
        int i;
        for (i = shotCount - 1; i >= 0; i--) {
            shotY[i]--;
            if (shotY[i] < 0) {
                removeShot(i);
            }
        }

        int killed = 0;
        int shot;
        int enemy;
        for (shot = shotCount - 1; shot >= 0; shot--) {
            for (enemy = enemyCount - 1; enemy >= 0; enemy--) {
                if (shotY[shot] == enemyY[enemy]
                        && shotX[shot] == enemyX[enemy]) {
                    removeShot(shot);
                    removeEnemy(enemy);
                    killed++;
                    break;
                }
            }
        }
        if (killed != 0) {
            engine.addScore(killed * 10);
        }
        repaint();
    }

    private void doAttack() {
        int i;
        for (i = 0; i < enemyCount; i++) {
            enemyY[i]++;
        }
        for (i = 0; i < 10 && enemyCount < MAX_ENEMIES; i++) {
            if (RANDOM.nextBooleanValue()) {
                addEnemy(0, i);
            }
        }
        for (i = 0; i < enemyCount; i++) {
            if (enemyY[i] > 19) {
                repaint();
                crash(19, gunX);
                return;
            }
        }
        repaint();
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
        if (enemyCount < MAX_ENEMIES) {
            enemyY[enemyCount] = y;
            enemyX[enemyCount] = x;
            enemyCount++;
        }
    }

    private void addShot(int y, int x) {
        if (shotCount < MAX_SHOTS) {
            shotY[shotCount] = y;
            shotX[shotCount] = x;
            shotCount++;
        }
    }

    private void removeEnemy(int index) {
        int last = --enemyCount;
        if (index != last) {
            enemyY[index] = enemyY[last];
            enemyX[index] = enemyX[last];
        }
    }

    private void removeShot(int index) {
        int last = --shotCount;
        if (index != last) {
            shotY[index] = shotY[last];
            shotX[index] = shotX[last];
        }
    }
}
