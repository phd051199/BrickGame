package brickgame;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Focused regression checks for target-aware tank bot decisions. */
public final class TanksAiTest {

    public static void main(String[] args) throws Exception {
        GameEngine engine = new GameEngine();
        TanksGame game = new TanksGame(engine);
        game.init(0L);

        Object[] tanks = (Object[]) field(TanksGame.class, "tanks").get(game);
        Object enemy = tanks[0];
        Class<?> tankType = enemy.getClass();
        Field pointY = field(tankType, "pointY");
        Field pointX = field(tankType, "pointX");
        Field direction = field(tankType, "direction");
        pointY.setInt(enemy, 0);
        pointX.setInt(enemy, 4);
        direction.setInt(enemy, 1);
        Method rebuild = tankType.getDeclaredMethod("rebuild", new Class[0]);
        rebuild.setAccessible(true);
        rebuild.invoke(enemy, new Object[0]);

        Method decide = TanksGame.class.getDeclaredMethod("doNextAiStep",
            new Class[] {tankType, Long.TYPE});
        decide.setAccessible(true);
        decide.invoke(game, new Object[] {enemy, Long.valueOf(10000L)});
        require(direction.getInt(enemy) == 2,
            "aligned bot should turn toward the player");

        decide.invoke(game, new Object[] {enemy, Long.valueOf(10000L)});
        ProjectilePool shots = (ProjectilePool) field(TanksGame.class,
            "enemyShots").get(game);
        require(shots.size() == 1,
            "aligned bot should fire after facing the player");
        require(shots.x(0) == 5 && shots.y(0) == 3,
            "bot projectile should start at its barrel");
        System.out.println("Tanks AI test passed");
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field value = type.getDeclaredField(name);
        value.setAccessible(true);
        return value;
    }

    private static void require(boolean value, String message) {
        if (!value) {
            throw new IllegalStateException(message);
        }
    }
}
