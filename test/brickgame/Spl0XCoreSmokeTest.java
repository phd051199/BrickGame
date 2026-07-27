package brickgame;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

public final class Spl0XCoreSmokeTest {
    private static final String[] ROM_NAMES = {
        "Apollo126in1B0202", "Apollo18in1B0302"
    };

    private static final int[][][] EXPECTED = {
        {
            {1000, 1004, 815, 815, 0, 191, 187, 0, 0, 0, 0, 1, 0, 0, 140, 4440},
            {10000, 10000, 817, 817, 0, 191, 187, 0, 0, 0, 0, 1, 0, 0, 102, 3132},
            {100000, 100000, 813, 813, 0, 24, 185, 1, 0, 0, 0, 1, 0, 1, 261, 8226},
            {500000, 500004, 815, 815, 0, 0, 187, 0, 0, 0, 0, 1, 1, 1, 272, 11964}
        },
        {
            {1000, 1001, 802, 802, 0, 127, 123, 0, 0, 0, 0, 1, 0, 0, 155, 4620},
            {10000, 10003, 808, 808, 0, 127, 123, 0, 0, 0, 0, 1, 0, 0, 187, 1883},
            {100000, 100001, 806, 806, 0, 127, 123, 0, 0, 0, 0, 1, 0, 0, 252, 4775},
            {500000, 500001, 802, 802, 0, 0, 123, 0, 0, 0, 0, 1, 0, 0, 562, 18605}
        }
    };

    private Spl0XCoreSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        String assets = args.length == 0
                ? "/Users/duypham/Developer/BrickEmuPy/assets"
                : args[0];
        for (int machine = 0; machine < ROM_NAMES.length; machine++) {
            MachineProfile profile = MachineProfile.ALL[machine + 4];
            Spl0XCpu cpu = new Spl0XCpu(profile,
                    read(assets + "/" + ROM_NAMES[machine] + ".bin"));
            verifyMachine(machine, cpu);
        }
        System.out.println("All SPL0X ROM reference smoke tests passed");
    }

    private static void verifyMachine(int machine, Spl0XCpu cpu) throws Exception {
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

    private static int[] snapshot(Spl0XCpu cpu, int used) throws Exception {
        byte[] vram = cpu.vram();
        int sum = 0;
        int checksum = 0;
        for (int i = 0; i < vram.length; i++) {
            int value = vram[i] & 255;
            sum += value;
            checksum += (i + 1) * value;
        }
        return new int[] {
            0,
            used,
            cpu.programCounter(),
            integer(cpu, "pc"),
            integer(cpu, "a"),
            integer(cpu, "x"),
            integer(cpu, "sp"),
            integer(cpu, "nf"),
            integer(cpu, "vf"),
            integer(cpu, "df"),
            integer(cpu, "bf"),
            integer(cpu, "interruptFlag"),
            integer(cpu, "zf"),
            integer(cpu, "cf"),
            sum,
            checksum
        };
    }

    private static int integer(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(target);
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
