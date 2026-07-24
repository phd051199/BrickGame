package brickgame;

import java.util.Random;

/** Direct CLDC port of game.tetris.* including the original rotation semantics. */
final class TetrisGame extends Game {

    private static final OriginalRandom BOARD_RANDOM = new OriginalRandom();
    private static final Random FACTORY_RANDOM = new Random();

    private static final int[][][] CLASSIC_SHAPES = {
        {{0x0, 0xF, 0x0, 0x0}, {0x2, 0x2, 0x2, 0x2}, {0x0, 0xF, 0x0, 0x0}, {0x2, 0x2, 0x2, 0x2}},
        {{0x0, 0x7, 0x1, 0x0}, {0x3, 0x2, 0x2, 0x0}, {0x4, 0x7, 0x0, 0x0}, {0x2, 0x2, 0x6, 0x0}},
        {{0x0, 0x7, 0x4, 0x0}, {0x2, 0x2, 0x3, 0x0}, {0x1, 0x7, 0x0, 0x0}, {0x6, 0x2, 0x2, 0x0}},
        {{0x0, 0x6, 0x6, 0x0}, {0x0, 0x6, 0x6, 0x0}, {0x0, 0x6, 0x6, 0x0}, {0x0, 0x6, 0x6, 0x0}},
        {{0x0, 0x3, 0x6, 0x0}, {0x2, 0x3, 0x1, 0x0}, {0x0, 0x3, 0x6, 0x0}, {0x2, 0x3, 0x1, 0x0}},
        {{0x0, 0x7, 0x2, 0x0}, {0x2, 0x3, 0x2, 0x0}, {0x2, 0x7, 0x0, 0x0}, {0x2, 0x6, 0x2, 0x0}},
        {{0x0, 0x6, 0x3, 0x0}, {0x1, 0x3, 0x2, 0x0}, {0x0, 0x6, 0x3, 0x0}, {0x1, 0x3, 0x2, 0x0}}
    };

    /** Five-cell figures inspired by the unusual figure table in the Holtek ROM. */
    private static final int[][][] PENTRIS_SHAPES = {
        {{0x7, 0x2, 0x2, 0x0}, {0x1, 0x7, 0x1, 0x0}, {0x2, 0x2, 0x7, 0x0}, {0x4, 0x7, 0x4, 0x0}},
        {{0x6, 0x6, 0x4, 0x0}, {0x7, 0x3, 0x0, 0x0}, {0x2, 0x6, 0x6, 0x0}, {0x6, 0x7, 0x0, 0x0}},
        {{0x5, 0x7, 0x0, 0x0}, {0x6, 0x2, 0x6, 0x0}, {0x7, 0x5, 0x0, 0x0}, {0x3, 0x2, 0x3, 0x0}},
        {{0x4, 0x4, 0x7, 0x0}, {0x7, 0x4, 0x4, 0x0}, {0x7, 0x1, 0x1, 0x0}, {0x1, 0x1, 0x7, 0x0}},
        {{0x4, 0x6, 0x3, 0x0}, {0x3, 0x6, 0x4, 0x0}, {0x4, 0x6, 0x3, 0x0}, {0x3, 0x6, 0x4, 0x0}},
        {{0x2, 0x2, 0x2, 0x3}, {0xF, 0x1, 0x0, 0x0}, {0x6, 0x4, 0x4, 0x4}, {0x8, 0xF, 0x0, 0x0}},
        {{0x6, 0x2, 0x3, 0x0}, {0x1, 0x7, 0x2, 0x0}, {0x6, 0x4, 0xC, 0x0}, {0x4, 0xE, 0x8, 0x0}}
    };

    private final int[] boardY = new int[500];
    private final int[] boardX = new int[500];
    private final int[][][] shapes;
    private final int blockCount;
    private int boardSize;
    private Piece tetromino;
    private Piece next;
    private Piece scratch;
    private long nextDrop;

    TetrisGame(GameEngine engine) {
        this(engine, false);
    }

    TetrisGame(GameEngine engine, boolean pentris) {
        super(engine);
        shapes = pentris ? PENTRIS_SHAPES : CLASSIC_SHAPES;
        blockCount = pentris ? 5 : 4;
        tetromino = new Piece();
        next = new Piece();
        scratch = new Piece();
        engine.setScore(0);
    }

    void init(long now) {
        boardSize = 0;
        int y;
        int x;
        for (y = 20 - engine.level; y <= 19; y++) {
            for (x = 0; x < 10; x++) {
                if (BOARD_RANDOM.nextBooleanValue()) {
                    addBoardPoint(y, x);
                }
            }
        }
        generate(tetromino);
        generate(next);
        repaintPreview(next);
        nextDrop = now;
    }

    void tick(long now) {
        if (now >= nextDrop) {
            nextDrop += period();
            doDown(now);
        }
    }

    void keyPressed(int action, long now) {
        if (action == GameEngine.ACTION_DOWN) {
            doDown(now);
        } else if (action == GameEngine.ACTION_LEFT) {
            doStep(-1, 0, false);
        } else if (action == GameEngine.ACTION_RIGHT) {
            doStep(1, 0, false);
        } else if (action == GameEngine.ACTION_UP || action == GameEngine.ACTION_FIRE) {
            doStep(0, 0, true);
        }
    }

    private void doDown(long now) {
        if (isBoardFull()) {
            crash(-5, -5);
            return;
        }

        scratch.copyFrom(tetromino);
        scratch.down();
        if (tetromino.inBottom() || !boardVerify(scratch)) {
            add(tetromino, now);
            return;
        }
        swapCurrentAndScratch();
        repaintBoard();
    }

    private void add(Piece piece, long now) {
        int i;
        for (i = 0; i < piece.size; i++) {
            addBoardPoint(piece.y[i], piece.x[i]);
        }
        int lines = cleanUp();
        if (lines != 0) {
            engine.addScore(lines * 100);
        }
        Piece landed = tetromino;
        tetromino = next;
        next = landed;
        generate(next);
        repaintPreview(next);
        repaintBoard();
    }

    private void doStep(int dx, int dy, boolean rotate) {
        scratch.copyFrom(tetromino);
        if (rotate) {
            scratch.rotate();
        } else if (dx < 0) {
            scratch.left();
        } else if (dx > 0) {
            scratch.right();
        } else if (dy > 0) {
            scratch.down();
        }
        if (!boardVerify(scratch)) {
            return;
        }
        swapCurrentAndScratch();
        repaintBoard();
    }

    private boolean boardVerify(Piece piece) {
        int i;
        int j;
        for (i = 0; i < piece.size; i++) {
            for (j = 0; j < boardSize; j++) {
                if (piece.y[i] == boardY[j] && piece.x[i] == boardX[j]) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isBoardFull() {
        int i;
        for (i = 0; i < boardSize; i++) {
            if (boardY[i] == -1) {
                return true;
            }
        }
        return false;
    }

    private int cleanUp() {
        int lines = 0;
        int row;
        for (row = 0; row < 20; row++) {
            int count = 0;
            int i;
            for (i = 0; i < boardSize; i++) {
                if (boardY[i] == row) {
                    count++;
                }
            }
            if (count != 10) {
                continue;
            }
            lines++;
            for (i = boardSize - 1; i >= 0; i--) {
                if (boardY[i] == row) {
                    removeBoardPoint(i);
                }
            }
            for (i = 0; i < boardSize; i++) {
                if (boardY[i] < row) {
                    boardY[i]++;
                }
            }
        }
        return lines;
    }

    private void addBoardPoint(int y, int x) {
        if (boardSize >= boardY.length) {
            return;
        }
        boardY[boardSize] = y;
        boardX[boardSize] = x;
        boardSize++;
    }

    private void removeBoardPoint(int index) {
        int i;
        for (i = index + 1; i < boardSize; i++) {
            boardY[i - 1] = boardY[i];
            boardX[i - 1] = boardX[i];
        }
        boardSize--;
    }

    private void repaintBoard() {
        clearBoard();
        int i;
        for (i = 0; i < boardSize; i++) {
            cell(boardY[i], boardX[i], true);
        }
        for (i = 0; i < tetromino.size; i++) {
            cell(tetromino.y[i], tetromino.x[i], true);
        }
    }

    private void repaintPreview(Piece piece) {
        clearPreview();
        int i;
        for (i = 0; i < piece.size; i++) {
            previewCell(piece.y[i] + 2, piece.x[i] - 3, true);
        }
    }

    private void generate(Piece piece) {
        piece.reset(FACTORY_RANDOM.nextInt(shapes.length),
            FACTORY_RANDOM.nextInt(4));
    }

    private void swapCurrentAndScratch() {
        Piece value = tetromino;
        tetromino = scratch;
        scratch = value;
    }

    private int period() {
        return 700 - engine.speed * 40;
    }

    private boolean shapeCell(int type, int state, int row, int col) {
        return (shapes[type][state & 3][row] & (1 << (3 - col))) != 0;
    }

    private final class Piece {
        int type;
        final int size;
        final int[] y = new int[5];
        final int[] x = new int[5];
        final int[] workY = new int[5];
        final int[] workX = new int[5];
        int state;
        int dotY;
        int dotX;

        Piece() {
            size = blockCount;
        }

        void reset(int nextType, int nextState) {
            type = nextType;
            state = nextState;
            dotY = -2;
            dotX = 3;
            makePoints(dotY, dotX, state, y, x);
        }

        void copyFrom(Piece source) {
            type = source.type;
            state = source.state;
            dotY = source.dotY;
            dotX = source.dotX;
            int i;
            for (i = 0; i < size; i++) {
                y[i] = source.y[i];
                x[i] = source.x[i];
            }
        }

        void down() {
            if (!verifyMove(0, 1)) {
                return;
            }
            dotY++;
            move(0, 1);
        }

        void left() {
            if (!verifyMove(-1, 0)) {
                return;
            }
            dotX--;
            move(-1, 0);
        }

        void right() {
            if (!verifyMove(1, 0)) {
                return;
            }
            dotX++;
            move(1, 0);
        }

        void rotate() {
            int nextState = state + 1;
            int centerY = dotY;
            int centerX = dotX;
            for (;;) {
                makePoints(centerY, centerX, nextState, workY, workX);
                boolean outside = false;
                int i;
                for (i = 0; i < size; i++) {
                    if (workX[i] < 0 || workX[i] >= 10) {
                        outside = true;
                        break;
                    }
                }
                if (!outside) {
                    break;
                }
                if (centerX < 5) {
                    centerX++;
                } else {
                    centerX--;
                }
            }
            int i;
            for (i = 0; i < size; i++) {
                if (workX[i] < 0 || workX[i] >= 10 || workY[i] >= 20) {
                    return;
                }
            }
            state++;
            for (i = 0; i < size; i++) {
                y[i] = workY[i];
                x[i] = workX[i];
            }
        }

        boolean inBottom() {
            int i;
            for (i = 0; i < size; i++) {
                if (y[i] == 19) {
                    return true;
                }
            }
            return false;
        }

        private boolean verifyMove(int dx, int dy) {
            int i;
            for (i = 0; i < size; i++) {
                int nextX = x[i] + dx;
                int nextY = y[i] + dy;
                if (nextX < 0 || nextX >= 10 || nextY >= 20) {
                    return false;
                }
            }
            return true;
        }

        private void move(int dx, int dy) {
            int i;
            for (i = 0; i < size; i++) {
                x[i] += dx;
                y[i] += dy;
            }
        }

        private void makePoints(int top, int left, int shapeState,
                                int[] targetY, int[] targetX) {
            int index = 0;
            int row;
            int col;
            for (row = 0; row < 4; row++) {
                for (col = 0; col < 4; col++) {
                    if (shapeCell(type, shapeState, row, col)) {
                        targetY[index] = top + row;
                        targetX[index] = left + col;
                        index++;
                    }
                }
            }
        }
    }
}
