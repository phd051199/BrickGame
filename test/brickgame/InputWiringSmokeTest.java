package brickgame;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

public final class InputWiringSmokeTest {
    private static final int[] BUTTON_SEQUENCE = {
        MachineProfile.BUTTON_START,
        MachineProfile.BUTTON_LEFT,
        MachineProfile.BUTTON_ROTATE
    };

    private InputWiringSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        String assets = args.length == 0
                ? "/Users/duypham/Developer/BrickEmuPy/assets"
                : args[0];
        verifyHt943(assets);
        verifySpl0X(assets);
        verifyEm73000(assets);
        verifyE0C6200(assets);
        verifyKs56(assets);
        System.out.println("All representative input wiring tests passed");
    }

    private static void exercise(BrickCpu cpu) {
        cpu.runCycles(500000);
        for (int i = 0; i < BUTTON_SEQUENCE.length; i++) {
            int button = BUTTON_SEQUENCE[i];
            cpu.setButton(button, true);
            cpu.runCycles(50000);
            cpu.setButton(button, false);
            cpu.runCycles(50000);
        }
    }

    private static void verifyHt943(String assets) throws Exception {
        Ht943Cpu cpu = new Ht943Cpu(MachineProfile.ALL[0],
                read(assets + "/E23PlusMarkII96in1.bin"));
        exercise(cpu);
        byte[] ram = cpu.vram();
        assertValues("HT943 input", new int[] {
            cpu.programCounter(), integer(cpu, "acc"), integer(cpu, "timerCounter"),
            integer(cpu, "carry"), integer(cpu, "externalFlag"), integer(cpu, "timerFlag"),
            sum(ram, 15), checksum(ram, 15)
        }, new int[] {562, 6, 80, 0, 0, 0, 528, 59451});
    }

    private static void verifySpl0X(String assets) throws Exception {
        Spl0XCpu cpu = new Spl0XCpu(MachineProfile.ALL[4],
                read(assets + "/Apollo126in1B0202.bin"));
        exercise(cpu);
        byte[] vram = cpu.vram();
        assertValues("SPL0X input", new int[] {
            cpu.programCounter(), integer(cpu, "a"), integer(cpu, "x"),
            integer(cpu, "sp"), integer(cpu, "cf"),
            sum(vram, 255), checksum(vram, 255)
        }, new int[] {899, 30, 0, 181, 0, 1844, 50145});
    }

    private static void verifyEm73000(String assets) throws Exception {
        Em73000Cpu cpu = new Em73000Cpu(MachineProfile.ALL[6],
                read(assets + "/E33_2in1.bin"));
        exercise(cpu);
        byte[] ram = (byte[]) field(cpu, "ram").get(cpu);
        assertValues("EM73000 input", new int[] {
            cpu.programCounter(), integer(cpu, "acc"), integer(cpu, "hl"),
            integer(cpu, "sp"), integer(cpu, "timerA"), integer(cpu, "timerB"),
            sum(ram, 15), checksum(ram, 15)
        }, new int[] {3551, 9, 160, 12, 4090, 4082, 632, 83365});
    }

    private static void verifyE0C6200(String assets) throws Exception {
        E0C6200Cpu cpu = new E0C6200Cpu(MachineProfile.ALL[7],
                read(assets + "/RadioShackStackChallenge.bin"));
        exercise(cpu);
        byte[] ram = (byte[]) field(cpu, "ram").get(cpu);
        byte[] vram = cpu.vram();
        assertValues("E0C6200 input", new int[] {
            cpu.programCounter(), integer(cpu, "a"), integer(cpu, "b"),
            integer(cpu, "ix"), integer(cpu, "iy"), integer(cpu, "sp"),
            sum(ram, 15), checksum(ram, 15),
            sum(vram, 15), checksum(vram, 15)
        }, new int[] {507, 15, 0, 3952, 161, 0, 95, 20658, 0, 0});
    }

    private static void verifyKs56(String assets) throws Exception {
        Ks56Cpu cpu = new Ks56Cpu(MachineProfile.ALL[8],
                read(assets + "/GA878.bin"));
        exercise(cpu);
        byte[] ram = (byte[]) field(cpu, "ram").get(cpu);
        byte[] vram = cpu.vram();
        assertValues("KS56 input", new int[] {
            cpu.programCounter(), rp(cpu, 0), rp(cpu, 2), rp(cpu, 4), rp(cpu, 6),
            integer(cpu, "sp"), integer(cpu, "cy"),
            sum(ram, 15), checksum(ram, 15),
            sum(vram, 15), checksum(vram, 15)
        }, new int[] {12, 0, 78, 49, 0, 250, 0, 279, 65572, 72, 17335});
    }

    private static int rp(Ks56Cpu cpu, int rp) throws Exception {
        byte[] ram = (byte[]) field(cpu, "ram").get(cpu);
        int rbe = integer(cpu, "rbe");
        int rbs = integer(cpu, "rbs");
        int offset = (rbe * rbs * 8 + (rp & 6)) ^ ((rp & 1) << 3);
        return ((ram[offset + 1] & 15) << 4) | (ram[offset] & 15);
    }

    private static int sum(byte[] data, int mask) {
        int result = 0;
        for (int i = 0; i < data.length; i++) {
            result += data[i] & mask;
        }
        return result;
    }

    private static int checksum(byte[] data, int mask) {
        int result = 0;
        for (int i = 0; i < data.length; i++) {
            result += (i + 1) * (data[i] & mask);
        }
        return result;
    }

    private static void assertValues(String name, int[] actual, int[] expected) {
        if (actual.length != expected.length) {
            throw new AssertionError(name + " length mismatch");
        }
        for (int i = 0; i < actual.length; i++) {
            if (actual[i] != expected[i]) {
                throw new AssertionError(name + " field " + i
                        + ": expected " + expected[i] + ", got " + actual[i]);
            }
        }
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
