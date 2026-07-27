package brickgame;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/** RAM-bit to shared 10x20 board mapping. Contains no image geometry. */
final class CommonLcdMap {
    private final byte[] boardRam;
    private final byte[] boardBit;
    private final byte[] boardX;
    private final byte[] boardY;
    private final byte[] previewRam;
    private final byte[] previewBit;
    private final byte[] previewX;
    private final byte[] previewY;

    private CommonLcdMap(int boardCount, int previewCount) {
        boardRam = new byte[boardCount];
        boardBit = new byte[boardCount];
        boardX = new byte[boardCount];
        boardY = new byte[boardCount];
        previewRam = new byte[previewCount];
        previewBit = new byte[previewCount];
        previewX = new byte[previewCount];
        previewY = new byte[previewCount];
    }

    static CommonLcdMap load(String path) throws IOException {
        byte[] data = Resources.read(path);
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
        if (input.readUnsignedByte() != 'B'
                || input.readUnsignedByte() != 'G'
                || input.readUnsignedByte() != 'M'
                || input.readUnsignedByte() != '1') {
            throw new IOException("Invalid common LCD map");
        }
        int boardCount = input.readUnsignedShort();
        int previewCount = input.readUnsignedShort();
        CommonLcdMap map = new CommonLcdMap(boardCount, previewCount);
        readEntries(input, map.boardRam, map.boardBit, map.boardX, map.boardY);
        readEntries(input, map.previewRam, map.previewBit, map.previewX, map.previewY);
        return map;
    }

    private static void readEntries(DataInputStream input, byte[] ram,
            byte[] bit, byte[] x, byte[] y) throws IOException {
        for (int i = 0; i < ram.length; i++) {
            ram[i] = (byte) input.readUnsignedByte();
            bit[i] = (byte) input.readUnsignedByte();
            x[i] = (byte) input.readUnsignedByte();
            y[i] = (byte) input.readUnsignedByte();
        }
    }

    int decode(byte[] vram, boolean enabled, short[] board, byte[] preview) {
        for (int i = 0; i < board.length; i++) {
            board[i] = 0;
        }
        for (int i = 0; i < preview.length; i++) {
            preview[i] = 0;
        }
        if (!enabled) {
            return 0;
        }

        int active = 0;
        for (int i = 0; i < boardRam.length; i++) {
            int address = boardRam[i] & 255;
            int mask = 1 << (boardBit[i] & 7);
            if (address < vram.length && ((vram[address] & 255) & mask) != 0) {
                int row = boardY[i] & 255;
                int column = boardX[i] & 255;
                if (row < 20 && column < 10) {
                    board[row] = (short) (board[row] | (1 << column));
                    active++;
                }
            }
        }
        for (int i = 0; i < previewRam.length; i++) {
            int address = previewRam[i] & 255;
            int mask = 1 << (previewBit[i] & 7);
            if (address < vram.length && ((vram[address] & 255) & mask) != 0) {
                int row = previewY[i] & 255;
                int column = previewX[i] & 255;
                if (row < 4 && column < 4) {
                    preview[row] = (byte) (preview[row] | (1 << column));
                }
            }
        }
        return active;
    }
}
