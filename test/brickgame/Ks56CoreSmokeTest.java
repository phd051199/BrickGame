package brickgame;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

public final class Ks56CoreSmokeTest {
    private static final String[] ROM_NAMES = {"GA878", "MiconKC32"};
    private static final int[][][] EXPECTED = {
        {
            {1000, 1000, 115, 195, 0, 0, 0, 0, 0, 0, 1, 0, 0, 15, 0, 40, 6400, 0, 0},
            {10000, 10020, 133, 0, 210, 0, 255, 0, 0, 0, 1, 0, 0, 0, 0, 63, 4873, 0, 0},
            {100000, 100000, 12, 0, 78, 49, 0, 250, 0, 0, 1, 1, 0, 0, 0, 242, 67071, 88, 21159},
            {500000, 500000, 12, 0, 78, 49, 0, 250, 1, 0, 1, 1, 0, 0, 0, 258, 65495, 72, 17335}
        },
        {
            {1000, 1024, 132, 0, 9, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 9, 27, 0, 0},
            {10000, 10176, 131, 0, 25, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 10, 31, 0, 0},
            {100000, 100480, 131, 0, 182, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 17, 62, 0, 0},
            {500000, 500000, 237, 0, 78, 65, 246, 0, 0, 0, 1, 1, 0, 0, 0, 327, 60006, 30, 7133}
        }
    };

    private Ks56CoreSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        String assets = args.length == 0
                ? "/Users/duypham/Developer/BrickEmuPy/assets"
                : args[0];
        for (int machine = 0; machine < ROM_NAMES.length; machine++) {
            MachineProfile profile = MachineProfile.ALL[machine + 8];
            Ks56Cpu cpu = new Ks56Cpu(profile,
                    read(assets + "/" + ROM_NAMES[machine] + ".bin"));
            verifyMachine(machine, cpu);
        }
        System.out.println("All KS56 ROM reference smoke tests passed");
    }

    private static void verifyMachine(int machine, Ks56Cpu cpu) throws Exception {
        for (int step = 0; step < EXPECTED[machine].length; step++) {
            int[] expected = EXPECTED[machine][step];
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

    private static int[] snapshot(Ks56Cpu cpu, int used) throws Exception {
        byte[] ram = (byte[]) field(cpu, "ram").get(cpu);
        int sum = 0;
        int checksum = 0;
        for (int i = 0; i < ram.length; i++) {
            int value = ram[i] & 15;
            sum += value;
            checksum += (i + 1) * value;
        }
        int vramSum = 0;
        int vramChecksum = 0;
        for (int i = 0; i < 256; i++) {
            int value = ram[0x100 + i] & 15;
            vramSum += value;
            vramChecksum += (i + 1) * value;
        }
        return new int[] {
            0,
            used,
            cpu.programCounter(),
            rp(cpu, 0),
            rp(cpu, 2),
            rp(cpu, 4),
            rp(cpu, 6),
            integer(cpu, "sp"),
            integer(cpu, "cy"),
            integer(cpu, "rbe"),
            integer(cpu, "mbe"),
            integer(cpu, "ime"),
            integer(cpu, "rbs"),
            integer(cpu, "mbs"),
            integer(cpu, "sbs"),
            sum,
            checksum,
            vramSum,
            vramChecksum
        };
    }

    private static int rp(Ks56Cpu cpu, int rp) throws Exception {
        Field methodHack = field(cpu, "ram");
        byte[] ram = (byte[]) methodHack.get(cpu);
        int rbe = integer(cpu, "rbe");
        int rbs = integer(cpu, "rbs");
        int offset = (rbe * rbs * 8 + (rp & 6)) ^ ((rp & 1) << 3);
        return ((ram[offset + 1] & 15) << 4) | (ram[offset] & 15);
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
            while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
            return output.toByteArray();
        } finally {
            input.close();
        }
    }
}
