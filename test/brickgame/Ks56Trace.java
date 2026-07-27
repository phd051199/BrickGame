package brickgame;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class Ks56Trace {
    public static void main(String[] args) throws Exception {
        Ks56Cpu cpu = new Ks56Cpu(MachineProfile.ALL[8], read(args[0] + "/GA878.bin"));
        Method clock = Ks56Cpu.class.getDeclaredMethod("clockUnits", new Class[0]);
        clock.setAccessible(true);
        byte[] rom = read(args[0] + "/GA878.bin");
        for (int i = 0; i < 50; i++) {
            int pc = integer(cpu, "pc");
            int first = rom[pc % rom.length] & 255;
            long units = ((Long) clock.invoke(cpu, new Object[0])).longValue();
            System.out.println(i + " " + Integer.toHexString(pc) + " "
                    + Integer.toHexString(first) + " " + (units / 32768L) + " "
                    + Integer.toHexString(integer(cpu, "pc")) + " "
                    + rp(cpu, 0) + " " + rp(cpu, 2) + " " + rp(cpu, 4) + " "
                    + rp(cpu, 6) + " " + integer(cpu, "sp") + " "
                    + integer(cpu, "mbs") + " " + integer(cpu, "rbs"));
        }
    }

    private static int rp(Ks56Cpu cpu, int rp) throws Exception {
        byte[] ram = (byte[]) field(cpu, "ram").get(cpu);
        int rbe = integer(cpu, "rbe"), rbs = integer(cpu, "rbs");
        int offset = (rbe * rbs * 8 + (rp & 6)) ^ ((rp & 1) << 3);
        return ((ram[offset + 1] & 15) << 4) | (ram[offset] & 15);
    }
    private static int integer(Object target, String name) throws Exception { return field(target, name).getInt(target); }
    private static Field field(Object target, String name) throws Exception { Field f=target.getClass().getDeclaredField(name); f.setAccessible(true); return f; }
    private static byte[] read(String path) throws Exception { InputStream in=new FileInputStream(path); ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] b=new byte[1024]; int n; while((n=in.read(b))>=0) out.write(b,0,n); in.close(); return out.toByteArray(); }
}
