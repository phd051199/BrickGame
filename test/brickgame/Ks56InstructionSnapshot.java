package brickgame;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class Ks56InstructionSnapshot {
    public static void main(String[] args) throws Exception {
        int count = Integer.parseInt(args[1]);
        Ks56Cpu cpu = new Ks56Cpu(MachineProfile.ALL[8], read(args[0] + "/GA878.bin"));
        Method clock = Ks56Cpu.class.getDeclaredMethod("clockUnits", new Class[0]);
        clock.setAccessible(true);
        long units = 0;
        while (integer(cpu, "instructionCounter") < count) {
            units += ((Long) clock.invoke(cpu, new Object[0])).longValue();
        }
        byte[] ram = (byte[]) field(cpu, "ram").get(cpu);
        int sum=0,check=0; for(int i=0;i<ram.length;i++){int v=ram[i]&15;sum+=v;check+=(i+1)*v;}
        System.out.println(count+","+((units+32767)/32768)+","+cpu.programCounter()+","+rp(cpu,0)+","+rp(cpu,2)+","+rp(cpu,4)+","+rp(cpu,6)+","+integer(cpu,"sp")+","+integer(cpu,"cy")+","+integer(cpu,"mbe")+","+integer(cpu,"rbe")+","+integer(cpu,"mbs")+","+integer(cpu,"rbs")+","+sum+","+check);
    }
    private static int rp(Ks56Cpu cpu,int rp)throws Exception{byte[]ram=(byte[])field(cpu,"ram").get(cpu);int rbe=integer(cpu,"rbe"),rbs=integer(cpu,"rbs");int o=(rbe*rbs*8+(rp&6))^((rp&1)<<3);return((ram[o+1]&15)<<4)|(ram[o]&15);}
    private static int integer(Object o,String n)throws Exception{return field(o,n).getInt(o);} private static Field field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f;}
    private static byte[] read(String p)throws Exception{InputStream i=new FileInputStream(p);ByteArrayOutputStream o=new ByteArrayOutputStream();byte[]b=new byte[1024];int n;while((n=i.read(b))>=0)o.write(b,0,n);i.close();return o.toByteArray();}
}
