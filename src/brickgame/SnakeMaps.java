package brickgame;

import java.io.IOException;
import java.io.InputStream;

/** Exact sixteen map resources used by the original Snake game. */
final class SnakeMaps {

    private static final boolean[][][] MAPS = load();

    private SnakeMaps() {
    }

    static void copy(int level, boolean[][] target) {
        boolean[][] source = MAPS[level & 15];
        int y;
        int x;
        for (y = 0; y < 20; y++) {
            for (x = 0; x < 10; x++) {
                target[y][x] = source[y][x];
            }
        }
    }

    private static boolean[][][] load() {
        boolean[][][] maps = new boolean[16][20][10];
        InputStream input = new SnakeMaps().getClass()
            .getResourceAsStream("/game/snake/maps.txt");
        if (input == null) {
            throw new IllegalStateException("Missing snake maps");
        }
        try {
            if (!"BRICKGAME-SNAKE-MAPS-1".equals(readLine(input))) {
                throw new IllegalStateException("Invalid snake map header");
            }
            String line;
            int map = -1;
            int row = 0;
            while ((line = readLine(input)) != null) {
                if (line.length() == 3 && line.charAt(0) == '@') {
                    map = (line.charAt(1) - '0') * 10 + (line.charAt(2) - '0');
                    row = 0;
                } else if (map >= 0 && map < 16 && row < 20 && line.length() >= 10) {
                    int x;
                    for (x = 0; x < 10; x++) {
                        maps[map][row][x] = line.charAt(x) != '0';
                    }
                    row++;
                }
            }
        } catch (IOException error) {
            throw new IllegalStateException(error.toString());
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {
            }
        }
        return maps;
    }

    private static String readLine(InputStream input) throws IOException {
        StringBuffer buffer = new StringBuffer();
        int value;
        boolean found = false;
        while ((value = input.read()) != -1) {
            found = true;
            if (value == '\n') {
                break;
            }
            if (value != '\r') {
                buffer.append((char) value);
            }
        }
        if (!found && buffer.length() == 0) {
            return null;
        }
        return buffer.toString();
    }
}
