package brickgame;

/** Immutable-by-convention render snapshot copied from the synchronized engine. */
final class GameSnapshot {

    final short[] board = new short[GameEngine.BOARD_ROWS];
    final byte[] preview = new byte[4];

    int revision = -1;
    int score;
    int speed;
    int level;
    int life;
    int gameIndex;
    int gameCode;
    char gameLetter;
    String gameLabel;
    String gameName;
    boolean pause;
    boolean menu;
    boolean usesLife;
    boolean usesNextPreview;
}
