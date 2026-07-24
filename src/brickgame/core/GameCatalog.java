package brickgame;

/** Metadata and factory for every Brick Game program. */
final class GameCatalog {

    static final int COUNT = 20;

    private static final String[] NAMES = {
        "TANKS", "BREAKOUT", "DOUBLE", "WALL BALL",
        "RACE", "HIGHWAY", "TUNNEL", "SHOOT",
        "STACK", "INVADERS", "SNAKE", "FROGGER",
        "MATCH", "TETRIS", "PONG", "DODGE",
        "PINBALL", "MAZE", "BOMBER", "PENTRIS"
    };

    private static final String[] LABELS = {
        "A-01", "B-02", "C-03", "D-04", "E-05",
        "F-06", "G-07", "H-08", "I-09", "J-10",
        "K-11", "L-12", "M-13", "N-14", "O-15",
        "P-16", "Q-17", "R-18", "S-19", "T-20"
    };

    private static final boolean[] LIFE = {
        true, true, true, true, true,
        true, true, true, true, true,
        true, true, true, false, true,
        true, true, true, true, false
    };

    private GameCatalog() {
    }

    static String name(int index) {
        return NAMES[normalize(index)];
    }

    static String label(int index) {
        return LABELS[normalize(index)];
    }

    static char letter(int index) {
        return (char) ('A' + normalize(index));
    }

    static int code(int index) {
        return normalize(index) + 1;
    }

    static boolean usesLife(int index) {
        return LIFE[normalize(index)];
    }

    static boolean usesNextPreview(int index) {
        int value = normalize(index);
        return value == 13 || value == 19;
    }

    static Game create(int index, GameEngine engine) {
        int value = normalize(index);
        if (value == 0) {
            return new TanksGame(engine);
        }
        if (value == 1) {
            return new BreakoutGame(engine, false);
        }
        if (value == 2) {
            return new BreakoutGame(engine, true);
        }
        if (value == 3) {
            return new WallBallGame(engine);
        }
        if (value == 4) {
            return new RaceGame(engine);
        }
        if (value == 5) {
            return new HighwayGame(engine);
        }
        if (value == 6) {
            return new TunnelGame(engine);
        }
        if (value == 7) {
            return new ShootGame(engine);
        }
        if (value == 8) {
            return new StackShootGame(engine);
        }
        if (value == 9) {
            return new InvadersGame(engine);
        }
        if (value == 10) {
            return new SnakeGame(engine);
        }
        if (value == 11) {
            return new FroggerGame(engine);
        }
        if (value == 12) {
            return new MatchGame(engine);
        }
        if (value == 13) {
            return new TetrisGame(engine, false);
        }
        if (value == 14) {
            return new PongGame(engine);
        }
        if (value == 15) {
            return new DodgeGame(engine);
        }
        if (value == 16) {
            return new PinballGame(engine);
        }
        if (value == 17) {
            return new MazeGame(engine);
        }
        if (value == 18) {
            return new BomberGame(engine);
        }
        return new TetrisGame(engine, true);
    }

    private static int normalize(int index) {
        int value = index % COUNT;
        return value < 0 ? value + COUNT : value;
    }
}
