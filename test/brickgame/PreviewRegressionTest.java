package brickgame;

/** Ensures every selector keeps its letter header and distinct lower preview. */
public final class PreviewRegressionTest {

    public static void main(String[] args) {
        GameEngine engine = new GameEngine();
        GameSnapshot snapshot = new GameSnapshot();
        int[] hashes = new int[GameCatalog.COUNT];
        int animated = 0;
        long now = 1000L;
        engine.tick(now);

        int index;
        for (index = 0; index < GameCatalog.COUNT; index++) {
            engine.copySnapshot(snapshot);
            require(snapshot.menu, "selector state " + index);
            require(active(snapshot.board, 0, 8) > 8,
                "letter header " + index);
            require(active(snapshot.board, 10, 20) > 0,
                "lower gameplay preview " + index);
            hashes[index] = hash(snapshot.board, 10, 20);
            int previous = hashes[index];
            now += 500L;
            engine.tick(now);
            engine.copySnapshot(snapshot);
            if (hash(snapshot.board, 10, 20) != previous) {
                animated++;
            }
            int other;
            for (other = 0; other < index; other++) {
                require(hashes[index] != hashes[other],
                    "duplicate preview " + index + " and " + other);
            }
            if (index + 1 < GameCatalog.COUNT) {
                engine.press(GameEngine.ACTION_RIGHT);
            }
        }
        require(animated >= 9, "animated selector previews: " + animated);
        System.out.println("Preview regression test passed for 20 programs");
    }

    private static int active(short[] rows, int first, int limit) {
        int count = 0;
        int y;
        int x;
        for (y = first; y < limit; y++) {
            int bits = rows[y] & 0xFFFF;
            for (x = 0; x < 10; x++) {
                if ((bits & (1 << x)) != 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int hash(short[] rows, int first, int limit) {
        int value = 17;
        int i;
        for (i = first; i < limit; i++) {
            value = value * 31 + (rows[i] & 0xFFFF);
        }
        return value;
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new IllegalStateException(message);
        }
    }
}
