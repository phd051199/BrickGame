package brickgame;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import javax.microedition.lcdui.Graphics;

final class BitmapFont {
    private final int firstCharacter;
    private final int glyphCount;
    private final int lineHeight;
    private final int letterSpacing;
    private final byte[] widths;
    private final short[] rows;

    private BitmapFont(int firstCharacter, int glyphCount, int lineHeight,
            int letterSpacing, byte[] widths, short[] rows) {
        this.firstCharacter = firstCharacter;
        this.glyphCount = glyphCount;
        this.lineHeight = lineHeight;
        this.letterSpacing = letterSpacing;
        this.widths = widths;
        this.rows = rows;
    }

    static BitmapFont load(String path) throws IOException {
        byte[] data = Resources.read(path);
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
        if (input.readUnsignedByte() != 'B'
                || input.readUnsignedByte() != 'M'
                || input.readUnsignedByte() != 'F'
                || input.readUnsignedByte() != '1') {
            throw new IOException("Invalid bitmap font");
        }
        int first = input.readUnsignedByte();
        int count = input.readUnsignedByte();
        input.readUnsignedByte(); // Maximum source width, kept for format validation.
        int height = input.readUnsignedByte();
        int spacing = input.readUnsignedByte();
        byte[] widths = new byte[count];
        short[] rows = new short[count * height];
        for (int glyph = 0; glyph < count; glyph++) {
            widths[glyph] = (byte) input.readUnsignedByte();
            int base = glyph * height;
            for (int row = 0; row < height; row++) {
                rows[base + row] = (short) input.readUnsignedShort();
            }
        }
        return new BitmapFont(first, count, height, spacing, widths, rows);
    }

    int lineHeight() {
        return lineHeight;
    }

    int stringWidth(String text) {
        if (text == null || text.length() == 0) {
            return 0;
        }
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '\n') {
                break;
            }
            width += glyphWidth(character);
            if (i + 1 < text.length() && text.charAt(i + 1) != '\n') {
                width += letterSpacing;
            }
        }
        return width;
    }

    void drawString(Graphics graphics, String text, int x, int baselineY, int anchor, int color) {
        if (text == null || text.length() == 0) {
            return;
        }
        int width = stringWidth(text);
        if ((anchor & Graphics.HCENTER) != 0) {
            x -= width / 2;
        } else if ((anchor & Graphics.RIGHT) != 0) {
            x -= width;
        }
        graphics.setColor(color);
        int cursor = x;
        int top = baselineY - lineHeight;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '\n') {
                break;
            }
            int glyph = glyphIndex(character);
            int glyphWidth = widths[glyph] & 255;
            int rowBase = glyph * lineHeight;
            for (int row = 0; row < lineHeight; row++) {
                int bits = rows[rowBase + row] & 0xFFFF;
                int column = 0;
                while (column < glyphWidth) {
                    while (column < glyphWidth && (bits & (1 << column)) == 0) {
                        column++;
                    }
                    if (column >= glyphWidth) {
                        break;
                    }
                    int runStart = column;
                    while (column < glyphWidth && (bits & (1 << column)) != 0) {
                        column++;
                    }
                    graphics.fillRect(cursor + runStart, top + row, column - runStart, 1);
                }
            }
            cursor += glyphWidth + letterSpacing;
        }
    }

    private int glyphWidth(char character) {
        return widths[glyphIndex(character)] & 255;
    }

    private int glyphIndex(char character) {
        int index = character - firstCharacter;
        if (index < 0 || index >= glyphCount) {
            index = '?' - firstCharacter;
        }
        return index;
    }
}
