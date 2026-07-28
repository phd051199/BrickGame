package e23;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

final class E23LcdMap {
    private final byte[] boardRam;
    private final byte[] boardBit;
    private final byte[] boardX;
    private final byte[] boardY;
    private final byte[] nextRam;
    private final byte[] nextBit;
    private final byte[] nextX;
    private final byte[] nextY;

    private E23LcdMap(int boardCount, int nextCount) {
        boardRam = new byte[boardCount];
        boardBit = new byte[boardCount];
        boardX = new byte[boardCount];
        boardY = new byte[boardCount];
        nextRam = new byte[nextCount];
        nextBit = new byte[nextCount];
        nextX = new byte[nextCount];
        nextY = new byte[nextCount];
    }

    static E23LcdMap load(String path) throws IOException {
        DataInputStream input = new DataInputStream(
                new ByteArrayInputStream(E23Assets.read(path)));
        if (input.readUnsignedByte() != 'B'
                || input.readUnsignedByte() != 'G'
                || input.readUnsignedByte() != 'M'
                || input.readUnsignedByte() != '1') {
            throw new IOException("Invalid E23 LCD map");
        }
        E23LcdMap map = new E23LcdMap(
                input.readUnsignedShort(), input.readUnsignedShort());
        readEntries(input, map.boardRam, map.boardBit, map.boardX, map.boardY);
        readEntries(input, map.nextRam, map.nextBit, map.nextX, map.nextY);
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

    void decode(byte[] lcdRam, boolean enabled, short[] board, byte[] next) {
        for (int i = 0; i < board.length; i++) {
            board[i] = 0;
        }
        for (int i = 0; i < next.length; i++) {
            next[i] = 0;
        }
        if (!enabled) {
            return;
        }
        for (int i = 0; i < boardRam.length; i++) {
            int address = boardRam[i] & 255;
            if ((lcdRam[address] & (1 << (boardBit[i] & 7))) != 0) {
                int row = boardY[i] & 255;
                board[row] = (short) (board[row] | (1 << (boardX[i] & 255)));
            }
        }
        for (int i = 0; i < nextRam.length; i++) {
            int address = nextRam[i] & 255;
            if ((lcdRam[address] & (1 << (nextBit[i] & 7))) != 0) {
                int row = nextY[i] & 255;
                next[row] = (byte) (next[row] | (1 << (nextX[i] & 255)));
            }
        }
    }
}
