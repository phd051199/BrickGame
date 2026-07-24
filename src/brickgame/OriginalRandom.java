package brickgame;

import java.util.Random;

/** Restores java.util.Random.nextBoolean(), omitted by some CLDC libraries. */
final class OriginalRandom extends Random {

    boolean nextBooleanValue() {
        return next(1) != 0;
    }
}
