package brickgame;

/** Desktop-side state and runtime regression checks for every program. */
public final class EngineSmokeTest {

    public static void main(String[] args) {
        GameEngine engine = new GameEngine();
        GameSnapshot snapshot = new GameSnapshot();
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
            require(GameCatalog.name(index).equals(snapshot.gameName),
                "program name " + index + " was " + snapshot.gameName);
            require(GameCatalog.label(index).equals(snapshot.gameLabel),
                "program label " + index);
            require(count(snapshot.board, 0, 8) > 8,
                "program letter and separator " + index);
            require(count(snapshot.board, 10, 20) > 0,
                "lower-half gameplay preview " + index);
            require(count(snapshot.preview) > 0,
                "side gameplay preview " + index);

            engine.press(GameEngine.ACTION_FIRE);
            engine.copySnapshot(snapshot);
            require(!snapshot.menu, "program " + index + " starts");
            require(snapshot.gameIndex == index, "running program index");

            int step;
            for (step = 0; step < 120; step++) {
                engine.press(1 + step % 5);
                now += 80L;
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
        require(snapshot.gameIndex == GameEngine.GAME_COUNT - 1,
            "menu ends on T");
        engine.press(GameEngine.ACTION_RIGHT);
        engine.copySnapshot(snapshot);
        require(snapshot.gameIndex == 0, "menu wraps T to A");
        engine.press(GameEngine.ACTION_LEFT);
        engine.copySnapshot(snapshot);
        require(snapshot.gameIndex == GameEngine.GAME_COUNT - 1,
            "menu wraps A to T");

        for (index = 0; index < 16; index++) {
            engine.press(GameEngine.ACTION_UP);
            engine.press(GameEngine.ACTION_DOWN);
        }
        engine.copySnapshot(snapshot);
        require(snapshot.speed == 0, "speed wraps at 15");
        require(snapshot.level == 0, "level wraps at 15");

        testSnapshotRevision(engine, snapshot);
        testProjectilePool();
        testTankPlayerBodyAndProjectileTiming();
        testDirectionAndFireChord();
        testSnakeHeadBlink();
        System.out.println("Engine smoke test passed for 20 programs");
    }

    private static void testSnapshotRevision(GameEngine engine,
                                             GameSnapshot snapshot) {
        require(!engine.copySnapshot(snapshot),
            "unchanged snapshots must not be copied twice");
        engine.press(GameEngine.ACTION_RIGHT);
        require(engine.copySnapshot(snapshot),
            "changed state must publish a snapshot");
        require(!engine.copySnapshot(snapshot),
            "published revision must remain stable");
    }

    private static void testProjectilePool() {
        ProjectilePool mine = new ProjectilePool(4);
        ProjectilePool enemy = new ProjectilePool(4);
        require(mine.add(5, 4, 1, 7), "first owner shot");
        require(!mine.add(5, 4, 1, 7), "one active shot per owner");
        require(enemy.add(5, 6, 3, 9), "enemy shot");
        mine.moveAll();
        enemy.moveAll();
        require(mine.meets(0, enemy, 0), "opposing shots collide");
        mine.remove(0);
        require(!mine.hasOwner(7), "removed shot releases owner");
    }

    private static void testTankPlayerBodyAndProjectileTiming() {
        GameEngine engine = new GameEngine();
        TanksGame game = new TanksGame(engine);
        game.init(0L);
        require(hasCell(engine.boardRows, 11, 5),
            "player tank fills the center between its tracks");

        game.keyPressed(GameEngine.ACTION_RIGHT, 0L);
        require(hasCell(engine.boardRows, 10, 4),
            "right-facing player tank center");
        game.keyPressed(GameEngine.ACTION_DOWN, 0L);
        require(hasCell(engine.boardRows, 9, 5),
            "down-facing player tank center");
        game.keyPressed(GameEngine.ACTION_LEFT, 0L);
        require(hasCell(engine.boardRows, 10, 6),
            "left-facing player tank center");
        game.keyPressed(GameEngine.ACTION_UP, 0L);

        game.tick(0L);
        game.keyPressed(GameEngine.ACTION_FIRE, 0L);
        require(hasCell(engine.boardRows, 8, 5),
            "tank projectile starts in front of barrel");
        game.tick(Game.FAST_PROJECTILE_PERIOD - 1L);
        require(hasCell(engine.boardRows, 8, 5),
            "tank projectile waits for shared Shoot timing");
        game.tick(Game.FAST_PROJECTILE_PERIOD);
        require(hasCell(engine.boardRows, 7, 5)
                && !hasCell(engine.boardRows, 8, 5),
            "tank projectile advances at Shoot speed");
    }

    private static void testSnakeHeadBlink() {
        GameEngine engine = new GameEngine();
        SnakeGame game = new SnakeGame(engine);
        game.init(0L);
        require(hasCell(engine.boardRows, 0, 2),
            "snake head starts visible");

        game.tick(249L);
        require(hasCell(engine.boardRows, 0, 2),
            "snake head remains visible before blink deadline");
        game.tick(250L);
        require(!hasCell(engine.boardRows, 0, 2),
            "snake head blinks off");
        game.tick(500L);
        require(hasCell(engine.boardRows, 0, 2),
            "snake head blinks on again");
    }

    private static void testDirectionAndFireChord() {
        GameEngine engine = new GameEngine();
        GameSnapshot snapshot = new GameSnapshot();
        engine.tick(0L);
        engine.press(GameEngine.ACTION_FIRE);

        InputRepeater input = new InputRepeater();
        input.press(GameEngine.ACTION_RIGHT, 0L, engine);
        input.press(GameEngine.ACTION_FIRE, 0L, engine);
        input.tick(165L, engine);
        engine.copySnapshot(snapshot);
        require(hasCell(snapshot.board, 10, 6),
            "holding fire must not cancel held direction");

        input.release(GameEngine.ACTION_RIGHT);
        input.release(GameEngine.ACTION_FIRE);
    }

    private static boolean hasCell(short[] rows, int y, int x) {
        return (rows[y] & (1 << x)) != 0;
    }

    private static int count(short[] rows) {
        return count(rows, 0, rows.length);
    }

    private static int count(short[] rows, int first, int limit) {
        int count = 0;
        int y;
        int x;
        for (y = first; y < limit; y++) {
            int bits = rows[y] & 0xFFFF;
            for (x = 0; x < 10; x++) {
                if ((bits & 1 << x) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int count(byte[] rows) {
        int count = 0;
        int y;
        int x;
        for (y = 0; y < rows.length; y++) {
            int bits = rows[y] & 0xFF;
            for (x = 0; x < 4; x++) {
                if ((bits & 1 << x) != 0) {
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
