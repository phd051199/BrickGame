package brickgame;

/** Fixed primitive projectile pool: no per-shot allocation and no GC churn. */
final class ProjectilePool {

    private final int[] y;
    private final int[] x;
    private final int[] previousY;
    private final int[] previousX;
    private final byte[] direction;
    private final int[] owner;
    private int size;

    ProjectilePool(int capacity) {
        y = new int[capacity];
        x = new int[capacity];
        previousY = new int[capacity];
        previousX = new int[capacity];
        direction = new byte[capacity];
        owner = new int[capacity];
    }

    int size() {
        return size;
    }

    int y(int index) {
        return y[index];
    }

    int x(int index) {
        return x[index];
    }

    int previousY(int index) {
        return previousY[index];
    }

    int previousX(int index) {
        return previousX[index];
    }

    int owner(int index) {
        return owner[index];
    }

    boolean hasOwner(int ownerId) {
        int i;
        for (i = 0; i < size; i++) {
            if (owner[i] == ownerId) {
                return true;
            }
        }
        return false;
    }

    boolean add(int shotY, int shotX, int shotDirection, int ownerId) {
        if (size >= y.length || hasOwner(ownerId)) {
            return false;
        }
        y[size] = shotY;
        x[size] = shotX;
        previousY[size] = shotY;
        previousX[size] = shotX;
        direction[size] = (byte) shotDirection;
        owner[size] = ownerId;
        size++;
        return true;
    }

    void moveAll() {
        int i;
        for (i = 0; i < size; i++) {
            previousY[i] = y[i];
            previousX[i] = x[i];
            int value = direction[i];
            if (value == 0) {
                y[i]--;
            } else if (value == 1) {
                x[i]++;
            } else if (value == 2) {
                y[i]++;
            } else {
                x[i]--;
            }
        }
    }

    boolean outside(int index) {
        return x[index] < 0 || x[index] >= GameEngine.BOARD_COLS
            || y[index] < 0 || y[index] >= GameEngine.BOARD_ROWS;
    }

    boolean meets(int index, ProjectilePool other, int otherIndex) {
        return y[index] == other.y[otherIndex] && x[index] == other.x[otherIndex]
            || y[index] == other.previousY[otherIndex]
            && x[index] == other.previousX[otherIndex]
            && previousY[index] == other.y[otherIndex]
            && previousX[index] == other.x[otherIndex];
    }

    void remove(int index) {
        int last = --size;
        if (index != last) {
            y[index] = y[last];
            x[index] = x[last];
            previousY[index] = previousY[last];
            previousX[index] = previousX[last];
            direction[index] = direction[last];
            owner[index] = owner[last];
        }
    }

    void clear() {
        size = 0;
    }
}
