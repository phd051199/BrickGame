package brickgame;

/** Representative 10x20 gameplay snapshots used by the game selector. */
final class GamePreview {

    private static final int[] TANK_UP = {0x2, 0x7, 0x5};
    private static final int[] TANK_RIGHT = {0x6, 0x3, 0x6};
    private static final int[] TANK_DOWN = {0x5, 0x7, 0x2};
    private static final int[] TANK_LEFT = {0x3, 0x6, 0x3};
    private static final int[] CAR = {0x2, 0x7, 0x2, 0x5};
    private static final int[] TETRIS_PILE = {
        0x000, 0x000, 0x000, 0x000, 0x000,
        0x000, 0x000, 0x000, 0x000, 0x000,
        0x000, 0x000, 0x000, 0x000, 0x120,
        0x1A0, 0x3A4, 0x3B7, 0x2FF, 0x3DF
    };
    private static final int[] BOMBER_SKYLINE = {3, 6, 4, 8, 5, 10, 6, 3, 7, 5};
    private static final byte[] SNAKE_BODY_Y = {14, 14, 14, 13, 12, 11, 11, 11, 10, 9};
    private static final byte[] SNAKE_BODY_X = {2, 3, 4, 4, 4, 4, 5, 6, 6, 6};

    private GamePreview() {
    }

    static void render(int game, int frame, GameEngine engine) {
        engine.clearBoard();
        engine.clearPreview();
        int phase = frame & 1;
        switch (game) {
            case 0: tanks(engine, phase); break;
            case 1: breakout(engine, phase, false); break;
            case 2: breakout(engine, phase, true); break;
            case 3: wallBall(engine, phase); break;
            case 4: race(engine, phase); break;
            case 5: highway(engine, phase); break;
            case 6: tunnel(engine, phase); break;
            case 7: shoot(engine, phase); break;
            case 8: stackShoot(engine, phase); break;
            case 9: invaders(engine, phase); break;
            case 10: snake(engine, phase); break;
            case 11: frogger(engine, phase); break;
            case 12: match(engine, phase); break;
            case 13: tetris(engine, phase, false); break;
            case 14: pong(engine, phase); break;
            case 15: dodge(engine, phase); break;
            case 16: pinball(engine, phase); break;
            case 17: maze(engine, phase); break;
            case 18: bomber(engine, phase); break;
            default: tetris(engine, phase, true); break;
        }
    }

    private static void tanks(GameEngine e, int phase) {
        tank(e, 16, 3, 0);
        tank(e, 1, phase == 0 ? 0 : 1, 2);
        tank(e, 5, 7, 3);
        cell(e, 13 - phase, 4);
        cell(e, 8 + phase, 7);
    }

    private static void breakout(GameEngine e, int phase, boolean doublePad) {
        int y;
        int x;
        for (y = doublePad ? 5 : 2; y < (doublePad ? 9 : 7); y++) {
            for (x = 1; x < 9; x++) {
                if (((x + y) & 2) != 0 || y == 3) {
                    cell(e, y, x);
                }
            }
        }
        paddle(e, 19, phase == 0 ? 2 : 4, 4);
        if (doublePad) {
            paddle(e, 0, phase == 0 ? 2 : 4, 4);
        }
        cell(e, phase == 0 ? 15 : 14, phase == 0 ? 4 : 5);
    }

    private static void wallBall(GameEngine e, int phase) {
        int y;
        for (y = 0; y < 7; y++) {
            cell(e, y, 0);
            cell(e, y, 1);
            cell(e, y, 8);
            cell(e, y, 9);
        }
        paddle(e, 8, phase == 0 ? 2 : 4, 3);
        paddle(e, 19, phase == 0 ? 3 : 2, 4);
        cell(e, phase == 0 ? 14 : 13, phase == 0 ? 5 : 4);
    }

    private static void race(GameEngine e, int phase) {
        road(e, phase);
        car(e, 15, phase == 0 ? 2 : 5);
        car(e, 2 + phase, 5);
        car(e, 9 + phase, 2);
    }

    private static void highway(GameEngine e, int phase) {
        int y;
        for (y = phase; y < 20; y += 4) {
            cell(e, y, 9);
        }
        car(e, 15, 3);
        car(e, 1 + phase, 0);
        car(e, 1 + phase, 6);
        car(e, 9 + phase, 3);
    }

    private static void tunnel(GameEngine e, int phase) {
        int y;
        for (y = 0; y < 20; y++) {
            int opening = 1 + ((y + phase * 2) / 5) % 3;
            int x;
            for (x = 0; x < opening; x++) {
                cell(e, y, x);
            }
            for (x = opening + 5; x < 10; x++) {
                cell(e, y, x);
            }
        }
        car(e, 15, phase == 0 ? 3 : 4);
    }

    private static void shoot(GameEngine e, int phase) {
        int y;
        int x;
        for (y = 1; y < 7; y += 2) {
            for (x = (y + phase) & 1; x < 10; x += 3) {
                cell(e, y, x);
            }
        }
        paddle(e, 19, 3, 3);
        cell(e, 18, 4);
        cell(e, 13 - phase * 2, 4);
        cell(e, 9 + phase, 7);
    }

    private static void stackShoot(GameEngine e, int phase) {
        int y;
        int x;
        for (y = 2; y < 8; y++) {
            for (x = 0; x < 10; x++) {
                if (((x * 3 + y) % 4) != 0) {
                    cell(e, y, x);
                }
            }
        }
        paddle(e, 19, 3, 3);
        cell(e, 18, 4);
        cell(e, 13 - phase, 4);
    }

    private static void invaders(GameEngine e, int phase) {
        int y;
        int x;
        for (y = 2; y < 7; y += 2) {
            for (x = 1 + phase; x < 9; x += 3) {
                cell(e, y, x);
                cell(e, y, x + 1);
            }
        }
        paddle(e, 19, phase == 0 ? 3 : 4, 3);
        cell(e, 18, phase == 0 ? 4 : 5);
        cell(e, 11 + phase, 5);
        cell(e, 14 - phase, 2);
    }

    private static void snake(GameEngine e, int phase) {
        int i;
        for (i = 0; i < SNAKE_BODY_Y.length; i++) {
            cell(e, SNAKE_BODY_Y[i], SNAKE_BODY_X[i]);
        }
        cell(e, phase == 0 ? 6 : 7, 7);
        for (i = 1; i < 8; i++) {
            if (i != 4) {
                cell(e, 4, i);
            }
        }
    }

    private static void frogger(GameEngine e, int phase) {
        int y;
        int x;
        for (y = 2; y < 19; y += 2) {
            for (x = 0; x < 10; x++) {
                cell(e, y, x);
            }
        }
        for (y = 3; y < 18; y += 4) {
            int start = (y + phase * 2) % 5;
            cell(e, y, start);
            cell(e, y, (start + 1) % 10);
            cell(e, y, (start + 6) % 10);
        }
        cell(e, phase == 0 ? 17 : 15, 5);
        cell(e, 0, 2);
        cell(e, 0, 7);
    }

    private static void match(GameEngine e, int phase) {
        matchShape(e, 15, 1, phase == 0 ? 1 : 2);
        matchShape(e, 15, 4, 3);
        matchShape(e, 15, 7, phase == 0 ? 2 : 1);
        matchShape(e, 4 + phase * 2, 1, 1);
        matchShape(e, 4 + phase * 2, 4, 3);
        matchShape(e, 4 + phase * 2, 7, 2);
    }

    private static void tetris(GameEngine e, int phase, boolean pentris) {
        int y;
        int x;
        for (y = 0; y < 20; y++) {
            for (x = 0; x < 10; x++) {
                if ((TETRIS_PILE[y] & (1 << x)) != 0) {
                    cell(e, y, x);
                }
            }
        }
        if (pentris) {
            int top = 4 + phase;
            cell(e, top, 2);
            cell(e, top + 1, 2);
            cell(e, top + 2, 2);
            cell(e, top + 3, 2);
            cell(e, top + 3, 3);
        } else if (phase == 0) {
            cell(e, 5, 3); cell(e, 5, 4); cell(e, 5, 5); cell(e, 6, 4);
        } else {
            cell(e, 5, 4); cell(e, 6, 3); cell(e, 6, 4); cell(e, 7, 4);
        }
    }

    private static void pong(GameEngine e, int phase) {
        paddleVertical(e, phase == 0 ? 7 : 8, 0);
        paddleVertical(e, phase == 0 ? 9 : 8, 9);
        cell(e, phase == 0 ? 8 : 9, phase == 0 ? 4 : 5);
    }

    private static void dodge(GameEngine e, int phase) {
        int y;
        int x;
        for (y = phase; y < 16; y += 4) {
            for (x = (y / 2) % 3; x < 10; x += 4) {
                cell(e, y, x);
                cell(e, y, x + 1);
            }
        }
        paddle(e, 19, phase == 0 ? 3 : 5, 2);
        cell(e, 18, phase == 0 ? 3 : 5);
    }

    private static void pinball(GameEngine e, int phase) {
        int y;
        for (y = 0; y < 20; y++) {
            cell(e, y, 0);
            cell(e, y, 9);
        }
        cell(e, 5, 3); cell(e, 5, 6);
        cell(e, 9, 2); cell(e, 9, 7);
        cell(e, 12, 4); cell(e, 12, 5);
        paddle(e, 18, 2, 3);
        paddle(e, 18, 5, 3);
        cell(e, phase == 0 ? 8 : 10, phase == 0 ? 5 : 4);
    }

    private static void maze(GameEngine e, int phase) {
        int y;
        for (y = 0; y < 20; y++) {
            cell(e, y, 0);
            cell(e, y, 9);
        }
        int x;
        for (x = 0; x < 8; x++) cell(e, 2, x);
        for (x = 2; x < 10; x++) cell(e, 6, x);
        for (x = 0; x < 8; x++) cell(e, 10, x);
        for (x = 2; x < 10; x++) cell(e, 14, x);
        cell(e, phase == 0 ? 18 : 17, 1);
        cell(e, 1, 8);
    }

    private static void bomber(GameEngine e, int phase) {
        int x;
        int y;
        for (x = 0; x < 10; x++) {
            for (y = 20 - BOMBER_SKYLINE[x]; y < 20; y++) {
                cell(e, y, x);
            }
        }
        int planeX = phase == 0 ? 1 : 3;
        cell(e, 3, planeX); cell(e, 3, planeX + 1); cell(e, 3, planeX + 2);
        cell(e, 2, planeX + 1);
        cell(e, phase == 0 ? 7 : 9, planeX + 2);
    }

    private static void road(GameEngine e, int phase) {
        int y;
        for (y = 0; y < 20; y++) {
            if (((y + phase) & 3) != 3) {
                cell(e, y, 0);
                cell(e, y, 9);
            }
        }
    }

    private static void tank(GameEngine e, int top, int left, int direction) {
        int[] shape;
        if (direction == 0) shape = TANK_UP;
        else if (direction == 1) shape = TANK_RIGHT;
        else if (direction == 2) shape = TANK_DOWN;
        else shape = TANK_LEFT;
        int y;
        int x;
        for (y = 0; y < 3; y++) {
            for (x = 0; x < 3; x++) {
                if ((shape[y] & (1 << (2 - x))) != 0) {
                    cell(e, top + y, left + x);
                }
            }
        }
    }

    private static void car(GameEngine e, int top, int left) {
        int y;
        int x;
        for (y = 0; y < 4; y++) {
            for (x = 0; x < 3; x++) {
                if ((CAR[y] & (1 << (2 - x))) != 0) {
                    cell(e, top + y, left + x);
                }
            }
        }
    }

    private static void paddle(GameEngine e, int y, int x, int width) {
        int i;
        for (i = 0; i < width; i++) cell(e, y, x + i);
    }

    private static void paddleVertical(GameEngine e, int y, int x) {
        cell(e, y, x); cell(e, y + 1, x); cell(e, y + 2, x); cell(e, y + 3, x);
    }

    private static void matchShape(GameEngine e, int top, int left, int size) {
        cell(e, top, left);
        if (size > 1) cell(e, top, left + 1);
        if (size > 2) cell(e, top - 1, left);
        if (size > 3) cell(e, top - 1, left + 1);
    }

    private static void cell(GameEngine e, int y, int x) {
        e.setBoardCell(y, x, true);
    }
}
