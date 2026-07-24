package brickgame;

/** Desktop-side regression checks for all fourteen Brick Game programs. */
public final class EngineSmokeTest {

    private static final String[] NAMES = {
        "TANKS", "BREAKOUT", "DOUBLE", "WALL BALL",
        "RACE", "HIGHWAY", "TUNNEL", "SHOOT",
        "STACK", "INVADERS", "SNAKE", "FROGGER",
        "MATCH", "TETRIS"
    };

    public static void main(String[] args) {
        GameEngine engine = new GameEngine();
        GameEngine.Snapshot snapshot = new GameEngine.Snapshot();
        long now = 1000L;
        engine.tick(now);

        int index;
        for (index = 0; index < GameEngine.GAME_COUNT; index++) {
            engine.copySnapshot(snapshot);
            require(snapshot.menu, "program " + index + " should be in menu");
            require(snapshot.gameIndex == index,
                "menu index " + index + " but was " + snapshot.gameIndex);
            require(snapshot.gameCode == index + 1, "program code");
            require(snapshot.gameLetter == (char) ('A' + index), "program letter");
            require(NAMES[index].equals(snapshot.gameName),
                "program name " + index + " was " + snapshot.gameName);
            require(count(snapshot.board) > 0, "menu board preview " + index);
            require(count(snapshot.preview) > 0, "side preview " + index);

            engine.press(GameEngine.ACTION_FIRE);
            engine.copySnapshot(snapshot);
            require(!snapshot.menu, "program " + index + " starts");
            require(snapshot.gameIndex == index, "running program index");

            int step;
            for (step = 0; step < 80; step++) {
                int action = 1 + (step % 5);
                engine.press(action);
                now += 100L;
                engine.tick(now);
                engine.copySnapshot(snapshot);
                if (snapshot.menu) {
                    break;
                }
            }

            engine.press(GameEngine.ACTION_MENU);
            engine.copySnapshot(snapshot);
            require(snapshot.menu, "program " + index + " returns to menu");
            if (index + 1 < GameEngine.GAME_COUNT) {
                engine.press(GameEngine.ACTION_RIGHT);
            }
        }

        engine.copySnapshot(snapshot);
        require(snapshot.gameIndex == GameEngine.GAME_COUNT - 1, "menu ends on N");
        engine.press(GameEngine.ACTION_RIGHT);
        engine.copySnapshot(snapshot);
        require(snapshot.gameIndex == 0, "menu wraps N to A");
        engine.press(GameEngine.ACTION_LEFT);
        engine.copySnapshot(snapshot);
        require(snapshot.gameIndex == GameEngine.GAME_COUNT - 1,
            "menu wraps A to N");

        for (index = 0; index < 16; index++) {
            engine.press(GameEngine.ACTION_UP);
            engine.press(GameEngine.ACTION_DOWN);
        }
        engine.copySnapshot(snapshot);
        require(snapshot.speed == 0, "speed wraps at 15");
        require(snapshot.level == 0, "level wraps at 15");

        System.out.println("Engine smoke test passed for 14 programs");
    }

    private static int count(boolean[][] values) {
        int count = 0;
        int y;
        int x;
        for (y = 0; y < values.length; y++) {
            for (x = 0; x < values[y].length; x++) {
                if (values[y][x]) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new IllegalStateException(message);
        }
    }
}
