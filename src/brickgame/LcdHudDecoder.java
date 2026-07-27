package brickgame;

final class LcdHudDecoder {
    private static final int[] DIGIT_MASKS = {
        0x3F, 0x06, 0x5B, 0x4F, 0x66,
        0x6D, 0x7D, 0x07, 0x7F, 0x6F
    };

    private static final short[][] E_SCORE = {
        {1626, 1627, 1625, 1528, 1529, 1530, 1531},
        {1498, 1499, 1497, 1512, 1513, 1514, 1515},
        {1610, 1611, 1609, 1480, 1481, 1482, 1483},
        {1594, 1595, 1593, 1432, 1433, 1434, 1435}
    };
    private static final short[][] E_SPEED = {
        {1571, 1587, 1603, 1619, 1602, 1570, 1586}
    };
    private static final short[][] E_LEVEL = {
        {1635, 1651, 1667, 1683, 1666, 1634, 1650}
    };

    private static final short[][] E33_SCORE = {
        {433, 305, 561, 704, 576, 448, 320},
        {450, 322, 578, 705, 577, 449, 321},
        {432, 304, 560, 707, 579, 451, 323},
        {427, 299, 555, 714, 586, 458, 330}
    };
    private static final short[][] E33_SPEED = {
        {810, 811, 816, 817, 944, 938, 939}
    };
    private static final short[][] E33_LEVEL = {
        {818, 819, 824, 825, 952, 946, 947}
    };

    private static final short[][] GA_SCORE = {
        {1939, 1936, 1937, 1946, 1945, 1947, 1944},
        {1923, 1920, 1921, 1930, 1929, 1931, 1928},
        {1803, 1800, 1802, 1794, 1793, 1795, 1792}
    };

    private static final short[][] STACK_SCORE = {
        {48, 49, 50, 51, 34, 32, 33},
        {80, 81, 82, 83, 66, 64, 65},
        {112, 113, 114, 115, 98, 96, 97},
        {144, 145, 146, 147, 130, 128, 129},
        {176, 177, 178, 179, 162, 160, 161}
    };
    private static final short[][] STACK_LEVEL = {
        {250, 233, 248, 264, 265, 266, 249}
    };

    private LcdHudDecoder() {
    }

    static int score(MachineProfile profile, byte[] vram) {
        String id = profile.id;
        if ("e23".equals(id) || "e88".equals(id)) {
            return decodeNumber(vram, E_SCORE);
        }
        if ("e33".equals(id)) {
            return decodeNumber(vram, E33_SCORE);
        }
        if ("ga888".equals(id) || "key55".equals(id)) {
            return decodeNumber(vram, GA_SCORE);
        }
        if ("stack".equals(id)) {
            return decodeNumber(vram, STACK_SCORE);
        }
        return -1;
    }

    static int scoreDigits(MachineProfile profile) {
        String id = profile.id;
        if ("ga888".equals(id) || "key55".equals(id)) {
            return 3;
        }
        if ("stack".equals(id)) {
            return 5;
        }
        if ("e23".equals(id) || "e88".equals(id) || "e33".equals(id)) {
            return 4;
        }
        return 0;
    }

    static int speed(MachineProfile profile, byte[] vram) {
        String id = profile.id;
        if ("e23".equals(id) || "e88".equals(id)) {
            return decodeTwoDigit(vram, E_SPEED[0], (short) 1618);
        }
        if ("e33".equals(id)) {
            return decodeTwoDigit(vram, E33_SPEED[0], (short) 945);
        }
        return -1;
    }

    static int level(MachineProfile profile, byte[] vram) {
        String id = profile.id;
        if ("e23".equals(id) || "e88".equals(id)) {
            return decodeTwoDigit(vram, E_LEVEL[0], (short) 1682);
        }
        if ("e33".equals(id)) {
            return decodeTwoDigit(vram, E33_LEVEL[0], (short) 953);
        }
        if ("stack".equals(id)) {
            return decodeDigit(vram, STACK_LEVEL[0]);
        }
        return -1;
    }

    private static int decodeTwoDigit(byte[] vram, short[] unitRefs, short tensRef) {
        int unit = decodeDigit(vram, unitRefs);
        boolean tens = bit(vram, tensRef);
        if (unit < 0) {
            return tens ? 10 : -1;
        }
        return unit + (tens ? 10 : 0);
    }

    private static int decodeNumber(byte[] vram, short[][] digits) {
        int value = 0;
        boolean seen = false;
        for (int i = 0; i < digits.length; i++) {
            int digit = decodeDigit(vram, digits[i]);
            if (digit < 0) {
                if (!seen && mask(vram, digits[i]) == 0) {
                    continue;
                }
                return -1;
            }
            seen = true;
            value = value * 10 + digit;
        }
        return seen ? value : -1;
    }

    private static int decodeDigit(byte[] vram, short[] refs) {
        int value = mask(vram, refs);
        if (value == 0) {
            return -1;
        }
        for (int digit = 0; digit < DIGIT_MASKS.length; digit++) {
            if (DIGIT_MASKS[digit] == value) {
                return digit;
            }
        }
        return -1;
    }

    private static int mask(byte[] vram, short[] refs) {
        int value = 0;
        for (int i = 0; i < refs.length; i++) {
            if (bit(vram, refs[i])) {
                value |= 1 << i;
            }
        }
        return value;
    }

    private static boolean bit(byte[] vram, short reference) {
        int encoded = reference & 65535;
        int address = encoded >> 3;
        int bit = encoded & 7;
        return address < vram.length && ((vram[address] & 255) & (1 << bit)) != 0;
    }
}
