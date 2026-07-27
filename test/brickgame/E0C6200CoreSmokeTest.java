package brickgame;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

public final class E0C6200CoreSmokeTest {
    private static final int[][] EXPECTED = {
        {1000, 1038, 459, 256, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {10000, 10193, 468, 256, 0, 0, 12, 10, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
        {100000, 100037, 469, 256, 0, 0, 148, 148, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
        {500000, 500000, 507, 256, 1, 0, 179, 0, 0, 1, 0, 0, 1, 1, 58, 11284, 1200, 48600}
    };

    private E0C6200CoreSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        String assets = args.length == 0
                ? "/Users/duypham/Developer/BrickEmuPy/assets"
                : args[0];
        MachineProfile profile = MachineProfile.ALL[7];
        E0C6200Cpu cpu = new E0C6200Cpu(profile,
                read(assets + "/RadioShackStackChallenge.bin"));
        for (int step = 0; step < EXPECTED.length; step++) {
            int[] expected = EXPECTED[step];
            int used = cpu.runCycles(expected[0]);
            int[] actual = snapshot(cpu, used);
            for (int field = 1; field < expected.length; field++) {
                if (actual[field] != expected[field]) {
                    throw new AssertionError("Stack Challenge step " + step
                            + " field " + field + ": expected " + expected[field]
                            + ", got " + actual[field]);
                }
            }
        }
        System.out.println("E0C6200 ROM reference smoke test passed");
    }

    private static int[] snapshot(E0C6200Cpu cpu, int used) throws Exception {
        byte[] ram = (byte[]) field(cpu, "ram").get(cpu);
        byte[] vram = (byte[]) field(cpu, "lcdRam").get(cpu);
        int ramSum = 0;
        int ramChecksum = 0;
        for (int i = 0; i < ram.length; i++) {
            int value = ram[i] & 15;
            ramSum += value;
            ramChecksum += (i + 1) * value;
        }
        int vramSum = 0;
        int vramChecksum = 0;
        for (int i = 0; i < vram.length; i++) {
            int value = vram[i] & 15;
            vramSum += value;
            vramChecksum += (i + 1) * value;
        }
        return new int[] {
            0,
            used,
            cpu.programCounter(),
            integer(cpu, "nextPc") & 0x1F00,
            integer(cpu, "a"),
            integer(cpu, "b"),
            integer(cpu, "ix"),
            integer(cpu, "iy"),
            integer(cpu, "sp"),
            integer(cpu, "carry"),
            integer(cpu, "zero"),
            integer(cpu, "decimal"),
            integer(cpu, "interruptFlag"),
            bool(cpu, "halted") ? 1 : 0,
            ramSum,
            ramChecksum,
            vramSum,
            vramChecksum
        };
    }

    private static int integer(Object target, String name) throws Exception {
        return field(target, name).getInt(target);
    }

    private static boolean bool(Object target, String name) throws Exception {
        return field(target, name).getBoolean(target);
    }

    private static Field field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static byte[] read(String path) throws Exception {
        InputStream input = new FileInputStream(path);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        try {
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }
}
