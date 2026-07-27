package brickgame;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

final class LcdLayout {
    static final int BACKGROUND = 0xC7D2A0;
    static final int FOREGROUND = 0x26352A;

    private static final class Segment {
        int ram;
        int bit;
        int x;
        int y;
        short[] runs;
        boolean visible;
    }

    final int width;
    final int height;

    private final Segment[] segments;
    private final Image surface;
    private final Graphics surfaceGraphics;

    private LcdLayout(int width, int height, Segment[] segments) {
        this.width = width;
        this.height = height;
        this.segments = segments;
        surface = Image.createImage(width, height);
        surfaceGraphics = surface.getGraphics();
        reset();
    }

    static LcdLayout load(String path) throws IOException {
        byte[] data = Resources.read(path);
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
        if (input.readUnsignedByte() != 'B'
                || input.readUnsignedByte() != 'G'
                || input.readUnsignedByte() != 'L'
                || input.readUnsignedByte() != '1') {
            throw new IOException("Invalid LCD layout");
        }
        int width = input.readUnsignedShort();
        int height = input.readUnsignedShort();
        int count = input.readUnsignedShort();
        Segment[] segments = new Segment[count];
        for (int i = 0; i < count; i++) {
            Segment segment = new Segment();
            segment.ram = input.readUnsignedByte();
            segment.bit = input.readUnsignedByte();
            segment.x = input.readUnsignedShort();
            segment.y = input.readUnsignedShort();
            int segmentWidth = input.readUnsignedShort();
            int segmentHeight = input.readUnsignedShort();
            int length = input.readUnsignedShort();
            byte[] mask = new byte[length];
            input.readFully(mask);
            segment.runs = decodeRuns(mask, segmentWidth, segmentHeight);
            segments[i] = segment;
        }
        return new LcdLayout(width, height, segments);
    }

    private static short[] decodeRuns(byte[] mask, int width, int height) {
        int runCount = 0;
        for (int row = 0; row < height; row++) {
            int column = 0;
            while (column < width) {
                while (column < width && !maskBit(mask, row * width + column)) {
                    column++;
                }
                if (column >= width) {
                    break;
                }
                runCount++;
                while (column < width && maskBit(mask, row * width + column)) {
                    column++;
                }
            }
        }
        short[] runs = new short[runCount * 3];
        int index = 0;
        for (int row = 0; row < height; row++) {
            int column = 0;
            while (column < width) {
                while (column < width && !maskBit(mask, row * width + column)) {
                    column++;
                }
                if (column >= width) {
                    break;
                }
                int start = column;
                while (column < width && maskBit(mask, row * width + column)) {
                    column++;
                }
                runs[index++] = (short) row;
                runs[index++] = (short) start;
                runs[index++] = (short) (column - start);
            }
        }
        return runs;
    }

    private static boolean maskBit(byte[] mask, int bitIndex) {
        int byteIndex = bitIndex >> 3;
        return byteIndex < mask.length
                && ((mask[byteIndex] & 255) & (1 << (7 - (bitIndex & 7)))) != 0;
    }

    void reset() {
        surfaceGraphics.setClip(0, 0, width, height);
        surfaceGraphics.setColor(BACKGROUND);
        surfaceGraphics.fillRect(0, 0, width, height);
        for (int i = 0; i < segments.length; i++) {
            segments[i].visible = false;
        }
    }

    boolean render(byte[] ram, boolean displayEnabled) {
        boolean changed = false;
        for (int i = 0; i < segments.length; i++) {
            Segment segment = segments[i];
            boolean active = displayEnabled
                    && segment.ram < ram.length
                    && (((ram[segment.ram] & 255) >> segment.bit) & 1) != 0;
            if (active != segment.visible) {
                drawSegment(segment, active ? FOREGROUND : BACKGROUND);
                segment.visible = active;
                changed = true;
            }
        }
        return changed;
    }

    private void drawSegment(Segment segment, int color) {
        surfaceGraphics.setColor(color);
        short[] runs = segment.runs;
        for (int i = 0; i < runs.length; i += 3) {
            surfaceGraphics.fillRect(segment.x + (runs[i + 1] & 65535),
                    segment.y + (runs[i] & 65535), runs[i + 2] & 65535, 1);
        }
    }

    void draw(Graphics graphics, int canvasWidth, int canvasHeight) {
        int x = (canvasWidth - width) / 2;
        int y = (canvasHeight - height) / 2;
        graphics.drawImage(surface, x, y, Graphics.TOP | Graphics.LEFT);
    }

    int segmentCount() {
        return segments.length;
    }
}
