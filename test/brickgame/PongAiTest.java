package brickgame;

import java.lang.reflect.Field;

/** Focused regression checks for predictive held-repeat Pong AI movement. */
public final class PongAiTest {

    public static void main(String[] args) throws Exception {
        testWallPrediction();
        testAiRepeatsBetweenBallSteps();
        testFastBallCoverage();
        testAiRecentersWhileBallMovesAway();
        System.out.println("Pong AI test passed");
    }

    private static void testWallPrediction() {
        require(PongGame.predictImpactY(0, -1, 1) == 1,
            "top-wall reflection");
        require(PongGame.predictImpactY(19, 1, 1) == 18,
            "bottom-wall reflection");
        require(PongGame.predictImpactY(1, -1, 3) == 2,
            "multi-step top reflection");
        require(PongGame.predictImpactY(18, 1, 3) == 17,
            "multi-step bottom reflection");
    }

    private static void testAiRepeatsBetweenBallSteps() throws Exception {
        GameEngine engine = new GameEngine();
        PongGame game = new PongGame(engine);
        game.init(0L);
        prepareIncomingBall(game, 8, 1);
        field("nextMove").setLong(game, 1000L);

        game.tick(0L);
        game.tick(65L);
        game.tick(66L);
        game.tick(132L);

        require(field("ballX").getInt(game) == 1,
            "ball should not move before its own deadline");
        require(field("aiY").getInt(game) == 11,
            "AI should repeat movement like a held direction");
    }

    private static void testFastBallCoverage() throws Exception {
        int startY;
        int direction;
        for (startY = 0; startY < 20; startY++) {
            for (direction = -1; direction <= 1; direction += 2) {
                GameEngine engine = new GameEngine();
                engine.speed = 15;
                PongGame game = new PongGame(engine);
                game.init(0L);
                prepareIncomingBall(game, startY, direction);

                boolean returned = false;
                long now;
                for (now = 0L; now <= 700L; now += 33L) {
                    game.tick(now);
                    if (field("dx").getInt(game) < 0) {
                        returned = true;
                        break;
                    }
                    if (engine.score != 0) {
                        break;
                    }
                }
                require(returned,
                    "AI missed fast trajectory y=" + startY
                    + " dy=" + direction);
                require(engine.score == 0,
                    "AI conceded fast trajectory y=" + startY
                    + " dy=" + direction);
            }
        }
    }

    private static void testAiRecentersWhileBallMovesAway() throws Exception {
        GameEngine engine = new GameEngine();
        PongGame game = new PongGame(engine);
        game.init(0L);
        field("launched").setBoolean(game, true);
        field("aiY").setInt(game, 0);
        field("dx").setInt(game, -1);
        field("nextMove").setLong(game, 1000L);
        field("nextAiMove").setLong(game, 0L);
        field("aiMoveDirection").setInt(game, 0);

        long now;
        for (now = 0L; now <= 462L; now += 66L) {
            game.tick(now);
        }
        require(field("aiY").getInt(game) == 8,
            "AI should return to center while waiting");
    }

    private static void prepareIncomingBall(PongGame game, int y,
                                             int direction) throws Exception {
        field("ballY").setInt(game, y);
        field("ballX").setInt(game, 1);
        field("dy").setInt(game, direction);
        field("dx").setInt(game, 1);
        field("aiY").setInt(game, 8);
        field("aiTargetY").setInt(game, 8);
        field("aiMoveDirection").setInt(game, 0);
        field("launched").setBoolean(game, true);
        field("nextMove").setLong(game, 0L);
        field("nextAiMove").setLong(game, 0L);
    }

    private static Field field(String name) throws Exception {
        Field value = PongGame.class.getDeclaredField(name);
        value.setAccessible(true);
        return value;
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new IllegalStateException(message);
        }
    }
}
