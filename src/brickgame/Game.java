package brickgame;

/** Small CLDC-friendly game contract shared by all five game modes. */
abstract class Game {

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
