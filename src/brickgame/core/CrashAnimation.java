package brickgame;

/** Low-allocation crash animation shared by all games. */
final class CrashAnimation {

    private static final short FULL_ROW = 0x03FF;

    private final GameEngine engine;
    private final short[] screenshot = new short[GameEngine.BOARD_ROWS];
    private final short[] mask = new short[GameEngine.BOARD_ROWS];
    private final int pointY;
    private final int pointX;
    private final long started;
    private int renderedPhase = -1;

    CrashAnimation(GameEngine engine, int pointY, int pointX, long started) {
        this.engine = engine;
        this.pointY = pointY;
        this.pointX = pointX;
        this.started = started;
        System.arraycopy(engine.boardRows, 0, screenshot, 0, screenshot.length);
        System.arraycopy(screenshot, 0, mask, 0, mask.length);
        clearImpactArea();
        paintState(2);
    }

    boolean update(long now) {
        long elapsed = now - started;
        if (elapsed < 1500L) {
            int step = (int) (elapsed / 50L);
            int state = (29 - step) % 3;
            if (state < 0) {
                state += 3;
            }
            if (renderedPhase != step) {
                renderedPhase = step;
                paintState(state);
            }
            return false;
        }
        if (elapsed < 2000L) {
            int step = (int) ((elapsed - 1500L) / 25L);
            if (step > 19) {
                step = 19;
            }
            if (renderedPhase != 100 + step) {
                renderedPhase = 100 + step;
                paintCleanup(step);
            }
            return false;
        }
        return true;
    }

    private void clearImpactArea() {
        int y;
        int x;
        for (y = pointY - 2; y <= pointY + 2; y++) {
            if (y < 0 || y >= GameEngine.BOARD_ROWS) {
                continue;
            }
            for (x = pointX - 2; x <= pointX + 2; x++) {
                if (x >= 0 && x < GameEngine.BOARD_COLS) {
                    mask[y] = (short) (mask[y] & ~(1 << x));
                }
            }
        }
    }

    private void paintState(int state) {
        engine.replaceBoard(mask);
        int y;
        int x;
        if (state == 0) {
            engine.setBoardCell(pointY, pointX, true);
        } else if (state == 1) {
            for (y = -1; y <= 1; y++) {
                for (x = -1; x <= 1; x++) {
                    if (y == -1 || y == 1 || x == -1 || x == 1) {
                        engine.setBoardCell(pointY + y, pointX + x, true);
                    }
                }
            }
        } else {
            int[] offsets = {-2, 0, 2};
            int iy;
            int ix;
            for (iy = 0; iy < offsets.length; iy++) {
                for (ix = 0; ix < offsets.length; ix++) {
                    if (offsets[iy] != 0 || offsets[ix] != 0) {
                        engine.setBoardCell(pointY + offsets[iy],
                            pointX + offsets[ix], true);
                    }
                }
            }
        }
    }

    private void paintCleanup(int step) {
        engine.replaceBoard(screenshot);
        int first = 19 - step;
        int y;
        for (y = first; y < GameEngine.BOARD_ROWS; y++) {
            engine.setBoardRow(y, FULL_ROW);
        }
    }
}
