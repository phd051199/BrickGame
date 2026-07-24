package brickgame;

/** Deterministic long-run exercise for bounds, timers and repeated input. */
public final class LongRunSmokeTest {

    public static void main(String[] args) {
        GameEngine engine = new GameEngine();
        GameSnapshot snapshot = new GameSnapshot();
        long now = 1000L;
        engine.tick(now);

        int game;
        for (game = 0; game < GameEngine.GAME_COUNT; game++) {
            engine.press(GameEngine.ACTION_FIRE);
            int step;
            for (step = 0; step < 2500; step++) {
                int action;
                if (game == 0) {
                    action = step % 3 == 0
                        ? GameEngine.ACTION_FIRE
                        : 1 + step % 4;
                } else if (game == 19) {
                    action = step % 2 == 0
                        ? GameEngine.ACTION_FIRE
                        : 1 + step % 4;
                } else {
                    action = 1 + step % 5;
                }
                engine.press(action);
                now += 45L;
                engine.tick(now);
                engine.copySnapshot(snapshot);
                if (snapshot.menu) {
                    engine.press(GameEngine.ACTION_FIRE);
                }
            }
            engine.press(GameEngine.ACTION_MENU);
            engine.copySnapshot(snapshot);
            if (game + 1 < GameEngine.GAME_COUNT) {
                engine.press(GameEngine.ACTION_RIGHT);
            }
        }
        System.out.println("Long-run smoke test passed for 20 programs");
    }
}
