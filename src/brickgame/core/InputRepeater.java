package brickgame;

/** Device-independent delayed auto-shift with direction + fire chord support. */
final class InputRepeater {

    private static final int ACTION_COUNT = GameEngine.ACTION_MENU + 1;

    private final boolean[] held = new boolean[ACTION_COUNT];
    private final long[] nextRepeat = new long[ACTION_COUNT];
    private final int[] pressOrder = new int[ACTION_COUNT];
    private int sequence;

    synchronized void press(int action, long now, GameEngine engine) {
        if (!isRepeatable(action)) {
            engine.press(action);
            clear();
            return;
        }
        if (held[action]) {
            return;
        }
        held[action] = true;
        pressOrder[action] = ++sequence;
        nextRepeat[action] = now + initialDelay(action);
        engine.press(action);
    }

    synchronized void release(int action) {
        if (action >= 0 && action < ACTION_COUNT) {
            held[action] = false;
            pressOrder[action] = 0;
            nextRepeat[action] = 0L;
        }
    }

    synchronized void tick(long now, GameEngine engine) {
        int direction = newestHeldDirection();
        if (direction != GameEngine.ACTION_NONE) {
            repeatIfDue(direction, now, engine);
        }
        if (held[GameEngine.ACTION_FIRE]) {
            repeatIfDue(GameEngine.ACTION_FIRE, now, engine);
        }
    }

    synchronized void clear() {
        int action;
        for (action = 0; action < ACTION_COUNT; action++) {
            held[action] = false;
            nextRepeat[action] = 0L;
            pressOrder[action] = 0;
        }
    }

    private void repeatIfDue(int action, long now, GameEngine engine) {
        if (now < nextRepeat[action]) {
            return;
        }
        engine.press(action);
        long period = repeatPeriod(action);
        do {
            nextRepeat[action] += period;
        } while (nextRepeat[action] <= now);
    }

    private int newestHeldDirection() {
        int selected = GameEngine.ACTION_NONE;
        int newest = -1;
        int action;
        for (action = GameEngine.ACTION_UP;
                action <= GameEngine.ACTION_RIGHT; action++) {
            if (held[action] && pressOrder[action] > newest) {
                selected = action;
                newest = pressOrder[action];
            }
        }
        return selected;
    }

    private static long initialDelay(int action) {
        return action == GameEngine.ACTION_FIRE ? 198L : 165L;
    }

    private static long repeatPeriod(int action) {
        return action == GameEngine.ACTION_FIRE ? 99L : 66L;
    }

    private static boolean isRepeatable(int action) {
        return action >= GameEngine.ACTION_UP && action <= GameEngine.ACTION_FIRE;
    }
}
