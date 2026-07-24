package brickgame;

/** Program M: match three two-by-two height codes before the falling row lands. */
final class MatchGame extends Game {

    private static final OriginalRandom RANDOM = new OriginalRandom();

    private final int[] player = new int[3];
    private final int[] falling = new int[3];
    private int fallingY;
    private int playerY;
    private long nextMove;

    MatchGame(GameEngine engine) {
        super(engine);
        engine.setScore(0);
        engine.setLife(4);
    }

    void init(long now) {
        int i;
        for (i = 0; i < 3; i++) {
            player[i] = 0;
            falling[i] = RANDOM.nextInt(4);
        }
        fallingY = 2;
        playerY = 19 - engine.level;
        if (playerY < 5) {
            playerY = 5;
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
        if (action == GameEngine.ACTION_LEFT) {
            player[0] = (player[0] + 1) & 3;
            repaint();
        } else if (action == GameEngine.ACTION_RIGHT) {
            player[2] = (player[2] + 1) & 3;
            repaint();
        } else if (action == GameEngine.ACTION_UP || action == GameEngine.ACTION_DOWN) {
            player[1] = (player[1] + 1) & 3;
            repaint();
        } else if (action == GameEngine.ACTION_FIRE) {
            advance(now);
            nextMove = now + period();
        }
    }

    private void advance(long now) {
        if (fallingY >= playerY) {
            verifyMatch(now);
            return;
        }
        fallingY++;
        if (fallingY >= playerY) {
            verifyMatch(now);
        } else {
            repaint();
        }
    }

    private void verifyMatch(long now) {
        if (player[0] == falling[0]
                && player[1] == falling[1]
                && player[2] == falling[2]) {
            engine.addScore(1);
            if (engine.score > 0 && engine.score % 15 == 0) {
                engine.increaseLevel();
            }
            init(now);
        } else {
            repaint();
            crash(playerY, 4);
        }
    }

    private int period() {
        int value = 520 - engine.speed * 24;
        return value < 140 ? 140 : value;
    }

    private void repaint() {
        clearBoard();
        int i;
        for (i = 0; i < 3; i++) {
            drawCode(player[i], 1 + i * 3, playerY);
            drawCode(falling[i], 1 + i * 3, fallingY);
        }
    }

    private void drawCode(int value, int left, int bottom) {
        cell(bottom, left, true);
        if (value >= 1) {
            cell(bottom, left + 1, true);
        }
        if (value >= 2) {
            cell(bottom - 1, left, true);
        }
        if (value >= 3) {
            cell(bottom - 1, left + 1, true);
        }
    }
}
