package brickgame;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Hashtable;

public final class ExactLcdAssetSmokeTest {
    public static void main(String[] args) throws Exception {
        for (int machine = 0; machine < MachineProfile.ALL.length; machine++) {
            MachineProfile profile = MachineProfile.ALL[machine];
            DataInputStream input = new DataInputStream(
                    new ByteArrayInputStream(Resources.read(profile.lcdPath())));
            require(input.readUnsignedByte() == 'B', profile.id);
            require(input.readUnsignedByte() == 'G', profile.id);
            require(input.readUnsignedByte() == 'L', profile.id);
            require(input.readUnsignedByte() == '1', profile.id);
            int width = input.readUnsignedShort();
            int height = input.readUnsignedShort();
            int count = input.readUnsignedShort();
            require(width == 320, profile.id + " width");
            require(height == 240, profile.id + " height");
            require(count > 0, profile.id + " segments");

            Hashtable<String, String> bits = new Hashtable<String, String>();
            for (int i = 0; i < count; i++) {
                int ram = input.readUnsignedByte();
                int bit = input.readUnsignedByte();
                int x = input.readUnsignedShort();
                int y = input.readUnsignedShort();
                int segmentWidth = input.readUnsignedShort();
                int segmentHeight = input.readUnsignedShort();
                int length = input.readUnsignedShort();
                require(bit < 8, profile.id + " bit");
                require(segmentWidth > 0 && segmentHeight > 0,
                        profile.id + " empty segment");
                require(x + segmentWidth <= width && y + segmentHeight <= height,
                        profile.id + " bounds");
                String key = ram + ":" + bit;
                require(bits.put(key, key) == null, profile.id + " duplicate " + key);
                int remaining = length;
                while (remaining > 0) {
                    int skipped = input.skipBytes(remaining);
                    require(skipped > 0, profile.id + " mask");
                    remaining -= skipped;
                }
            }
            require(input.read() == -1, profile.id + " trailing bytes");
        }
        System.out.println("All exact 320x240 LCD asset tests passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
