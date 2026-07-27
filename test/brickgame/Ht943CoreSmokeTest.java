package brickgame;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

public final class Ht943CoreSmokeTest {
    private static final String[] ROM_NAMES = {
        "E23PlusMarkII96in1", "E88_8in1", "GA888", "Keychain55in1"
    };
    private static final int[][][] EXPECTED = {
        {
            {1000, 1000, 3689, 0, 62, 0, 0, 0, 0, 0, 15, 15, 15, 15, 15, 1},
            {10000, 10000, 3553, 0, 175, 0, 0, 0, 0, 0, 15, 15, 15, 138, 11845, 23},
            {100000, 100000, 3015, 3, 25, 1, 0, 1, 0, 0, 15, 15, 15, 367, 59026, 65},
            {500000, 500004, 3577, 3, 43, 0, 0, 0, 0, 0, 15, 15, 15, 379, 60838, 65}
        },
        {
            {1000, 1000, 3547, 0, 62, 0, 0, 0, 0, 0, 15, 15, 15, 0, 0, 0},
            {10000, 10000, 2289, 6, 175, 0, 0, 0, 0, 0, 15, 15, 15, 135, 11691, 22},
            {100000, 100004, 2737, 0, 25, 0, 0, 0, 0, 0, 15, 15, 15, 315, 44487, 41},
            {500000, 500004, 3802, 15, 44, 0, 0, 0, 0, 0, 15, 15, 15, 315, 44498, 43}
        },
        {
            {1000, 1004, 2775, 10, 62, 0, 0, 0, 0, 0, 15, 15, 15, 0, 0, 0},
            {10000, 10004, 2398, 7, 176, 0, 0, 0, 0, 0, 15, 15, 15, 113, 7545, 19},
            {100000, 100004, 841, 0, 26, 0, 0, 0, 0, 0, 15, 15, 15, 145, 22837, 30},
            {500000, 500004, 851, 0, 44, 0, 0, 0, 0, 0, 15, 15, 15, 151, 23463, 33}
        },
        {
            {1000, 1000, 2651, 9, 123, 0, 0, 0, 0, 0, 15, 15, 15, 0, 0, 0},
            {10000, 10000, 2327, 0, 93, 0, 0, 0, 0, 0, 15, 15, 15, 59, 2425, 13},
            {100000, 100000, 3752, 0, 49, 0, 0, 0, 0, 0, 15, 15, 15, 486, 80326, 50},
            {500000, 500000, 3240, 2, 85, 0, 0, 0, 0, 0, 15, 15, 15, 500, 79236, 51}
        }
    };

    private Ht943CoreSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        String assets = args.length == 0
                ? "/Users/duypham/Developer/BrickEmuPy/assets"
                : args[0];
        for (int machine = 0; machine < ROM_NAMES.length; machine++) {
            MachineProfile profile = MachineProfile.ALL[machine];
            Ht943Cpu cpu = new Ht943Cpu(
                    profile,
                    read(assets + "/" + ROM_NAMES[machine] + ".bin"));
            verifyMachine(machine, cpu);
        }
        System.out.println("All HT943 ROM reference smoke tests passed");
    }

    private static void verifyMachine(int machine, Ht943Cpu cpu) throws Exception {
        int[][] checkpoints = EXPECTED[machine];
        for (int step = 0; step < checkpoints.length; step++) {
            int[] expected = checkpoints[step];
            int used = cpu.runCycles(expected[0]);
            int[] actual = snapshot(cpu, used);
            for (int field = 1; field < expected.length; field++) {
                if (actual[field] != expected[field]) {
                    throw new AssertionError(ROM_NAMES[machine] + " step " + step
                            + " field " + field + ": expected " + expected[field]
                            + ", got " + actual[field]);
                }
            }
        }
    }

    private static int[] snapshot(Ht943Cpu cpu, int used) throws Exception {
        byte[] ram = cpu.vram();
        int ramSum = 0;
        int checksum = 0;
        int nonZero = 0;
        for (int i = 0; i < ram.length; i++) {
            int value = ram[i] & 15;
            ramSum += value;
            checksum += (i + 1) * value;
            if (value != 0) {
                nonZero++;
            }
        }
        return new int[] {
            0,
            used,
            cpu.programCounter(),
            integer(cpu, "acc"),
            integer(cpu, "timerCounter"),
            integer(cpu, "carry"),
            integer(cpu, "externalFlag"),
            integer(cpu, "timerFlag"),
            integer(cpu, "interruptEnable"),
            bool(cpu, "halted") ? 1 : 0,
            integer(cpu, "pp"),
            integer(cpu, "pm"),
            integer(cpu, "ps"),
            ramSum,
            checksum,
            nonZero
        };
    }

    private static int integer(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
    }

    private static boolean bool(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getBoolean(target);
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
