package brickgame;

/** Small CLDC-friendly game contract shared by all game modes. */
abstract class Game {

    protected static final long FAST_PROJECTILE_PERIOD = 33L;

    protected final GameEngine engine;

    Game(GameEngine engine) {
        this.engine = engine;
    }

    abstract void init(long now);

    abstract void tick(long now);

    abstract void keyPressed(int action, long now);

    protected final void clearBoard() {
        engine.clearBoard();
    }

    protected final void clearPreview() {
        engine.clearPreview();
    }

    protected final void cell(int y, int x, boolean value) {
        engine.setBoardCell(y, x, value);
    }

    protected final void previewCell(int y, int x, boolean value) {
        engine.setPreviewCell(y, x, value);
    }

    protected final void crash(int y, int x) {
        engine.crash(y, x);
    }
}
