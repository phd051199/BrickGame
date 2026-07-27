package brickgame;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

public final class Em73000CoreSmokeTest {
    private static final int[][] EXPECTED = {
        {1000, 1000, 162, 0, 21, 11, 0, 0, 0, 0, 0, 0, 0, 1, 0, 37, 8992},
        {10000, 10008, 4, 6, 240, 11, 0, 0, 2, 0, 4080, 0, 0, 1, 0, 106, 18726},
        {100000, 100000, 3552, 14, 160, 12, 3719, 0, 2, 0, 4081, 0, 0, 1, 1, 553, 71076},
        {500000, 500000, 3551, 0, 160, 12, 3759, 0, 2, 0, 4089, 0, 0, 1, 1, 587, 79155}
    };

    private Em73000CoreSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        String assets = args.length == 0
                ? "/Users/duypham/Developer/BrickEmuPy/assets"
                : args[0];
        MachineProfile profile = MachineProfile.ALL[6];
        Em73000Cpu cpu = new Em73000Cpu(profile,
                read(assets + "/E33_2in1.bin"));
        for (int step = 0; step < EXPECTED.length; step++) {
            int[] expected = EXPECTED[step];
            int used = cpu.runCycles(expected[0]);
            int[] actual = snapshot(cpu, used);
            for (int field = 1; field < expected.length; field++) {
                if (actual[field] != expected[field]) {
                    throw new AssertionError("E33 step " + step + " field " + field
                            + ": expected " + expected[field] + ", got " + actual[field]);
                }
            }
        }
        System.out.println("EM73000 ROM reference smoke test passed");
    }

    private static int[] snapshot(Em73000Cpu cpu, int used) throws Exception {
        byte[] ram = (byte[]) field(cpu, "ram").get(cpu);
        int sum = 0;
        int checksum = 0;
        for (int i = 0; i < ram.length; i++) {
            int value = ram[i] & 15;
            sum += value;
            checksum += (i + 1) * value;
        }
        return new int[] {
            0,
            used,
            cpu.programCounter(),
            integer(cpu, "acc"),
            integer(cpu, "hl"),
            integer(cpu, "sp"),
            integer(cpu, "dp"),
            integer(cpu, "interruptLatch"),
            integer(cpu, "interruptMask"),
            integer(cpu, "timerA"),
            integer(cpu, "timerB"),
            integer(cpu, "carry"),
            integer(cpu, "zero"),
            integer(cpu, "status"),
            integer(cpu, "interruptEnable"),
            sum,
            checksum
        };
    }

    private static int integer(Object target, String name) throws Exception {
        return field(target, name).getInt(target);
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
