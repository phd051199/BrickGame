package brickgame;

/** Game-selection state with program letter above a compact gameplay preview. */
final class GameMenu {

    static final int NO_START = -1;

    private static final int[][] LETTERS = {
        {0x0E, 0x11, 0x1F, 0x11, 0x11},
        {0x1E, 0x11, 0x1E, 0x11, 0x1E},
        {0x0F, 0x10, 0x10, 0x10, 0x0F},
        {0x1E, 0x11, 0x11, 0x11, 0x1E},
        {0x1F, 0x10, 0x1E, 0x10, 0x1F},
        {0x1F, 0x10, 0x1E, 0x10, 0x10},
        {0x0F, 0x10, 0x17, 0x11, 0x0F},
        {0x11, 0x11, 0x1F, 0x11, 0x11},
        {0x1F, 0x04, 0x04, 0x04, 0x1F},
        {0x07, 0x01, 0x01, 0x11, 0x0E},
        {0x11, 0x12, 0x1C, 0x12, 0x11},
        {0x10, 0x10, 0x10, 0x10, 0x1F},
        {0x11, 0x1B, 0x15, 0x11, 0x11},
        {0x11, 0x19, 0x15, 0x13, 0x11},
        {0x0E, 0x11, 0x11, 0x11, 0x0E},
        {0x1E, 0x11, 0x1E, 0x10, 0x10},
        {0x0E, 0x11, 0x15, 0x12, 0x0D},
        {0x1E, 0x11, 0x1E, 0x12, 0x11},
        {0x0F, 0x10, 0x0E, 0x01, 0x1E},
        {0x1F, 0x04, 0x04, 0x04, 0x04}
    };

    private final short[] gameplayRows = new short[GameEngine.BOARD_ROWS];
    private int selected;
    private int frame;
    private long nextFrame;

    int selected() {
        return selected;
    }

    int handle(int action, GameEngine engine) {
        if (action == GameEngine.ACTION_LEFT) {
            selected--;
            if (selected < 0) {
                selected = GameCatalog.COUNT - 1;
            }
            frame = 0;
            render(engine);
        } else if (action == GameEngine.ACTION_RIGHT) {
            selected++;
            if (selected >= GameCatalog.COUNT) {
                selected = 0;
            }
            frame = 0;
            render(engine);
        } else if (action == GameEngine.ACTION_UP) {
            engine.setSpeed(engine.speed + 1);
            render(engine);
        } else if (action == GameEngine.ACTION_DOWN) {
            engine.setLevel(engine.level + 1);
            render(engine);
        } else if (action == GameEngine.ACTION_FIRE) {
            return selected;
        }
        return NO_START;
    }

    void tick(long now, GameEngine engine) {
        if (now >= nextFrame) {
            nextFrame = now + 420L;
            frame++;
            render(engine);
        }
    }

    void reset(long now, GameEngine engine) {
        frame = 0;
        nextFrame = now + 420L;
        render(engine);
    }

    void render(GameEngine engine) {
        engine.setScore(selected + 1);

        GamePreview.render(selected, frame, engine);
        System.arraycopy(engine.boardRows, 0, gameplayRows, 0,
            GameEngine.BOARD_ROWS);

        engine.clearBoard();
        engine.clearPreview();
        drawLetter(engine);
        drawSeparator(engine);
        drawGameplayPreview(engine);
        drawSidePreview(engine);
    }

    private void drawLetter(GameEngine engine) {
        int y;
        int x;
        for (y = 0; y < 5; y++) {
            int bits = LETTERS[selected][y];
            for (x = 0; x < 5; x++) {
                if ((bits & (1 << (4 - x))) != 0) {
                    engine.setBoardCell(1 + y, 2 + x, true);
                }
            }
        }
    }

    private static void drawSeparator(GameEngine engine) {
        int x;
        for (x = 1; x < 9; x++) {
            engine.setBoardCell(7, x, true);
        }
    }

    /** Compresses the real 10x20 preview into the lower ten board rows. */
    private void drawGameplayPreview(GameEngine engine) {
        int y;
        for (y = 0; y < 10; y++) {
            int source = y * 2;
            short row = (short) (gameplayRows[source]
                | gameplayRows[source + 1]);
            engine.setBoardRow(10 + y, row);
        }
    }

    /** Keeps the side 4x4 preview derived from the actual gameplay snapshot. */
    private void drawSidePreview(GameEngine engine) {
        int py;
        int px;
        for (py = 0; py < 4; py++) {
            int yStart = py * 20 / 4;
            int yEnd = (py + 1) * 20 / 4;
            for (px = 0; px < 4; px++) {
                int xStart = px * 10 / 4;
                int xEnd = (px + 1) * 10 / 4;
                boolean active = false;
                int y;
                int x;
                for (y = yStart; y < yEnd && !active; y++) {
                    int bits = gameplayRows[y] & 0x03FF;
                    for (x = xStart; x < xEnd; x++) {
                        if ((bits & (1 << x)) != 0) {
                            active = true;
                            break;
                        }
                    }
                }
                engine.setPreviewCell(py, px, active);
            }
        }
    }
}
