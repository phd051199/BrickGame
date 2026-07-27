package brickgame;

final class Spl0XCpu implements BrickCpu {
    private static final int SUB_CLOCK = 32768;
    private static final int ADDRESS_SPACE_SIZE = 0x2000;
    private static final int VADDR_NMI = 0x1FFA;
    private static final int VADDR_RESET = 0x1FFC;
    private static final int VADDR_IRQ = 0x1FFE;

    private static final int IO_CTRL_ALDIR = 0x01;
    private static final int IO_CTRL_AHDIR = 0x02;
    private static final int IO_CTRL_BDIR = 0x04;
    private static final int IO_CTRL_ROSC = 0x10;
    private static final int IO_CTRL_CPU_CLOCK = 0x20;

    private static final int IO_INT_CFG_T2HZ_INT = 0x01;
    private static final int IO_INT_CFG_T256HZ_INT = 0x02;
    private static final int IO_INT_CFG_POWERKEY_INT = 0x04;
    private static final int IO_INT_CFG_NMI_ENBL = 0x80;

    private static final int IO_SYS_CTRL_32K_ENBL = 0x20;
    private static final int IO_SYS_CTRL_CPU_STOP = 0x40;
    private static final int IO_SYS_CTRL_ROSC_STOP = 0x80;

    private static final int SFR_OFFSET = 0xC0;
    private static final int SFR_SIZE = 0x40;
    private static final int CPU_RAM_OFFSET = 0x30;
    private static final int LCD_RAM_OFFSET = 0x00;
    private static final int LCD_RAM_SIZE = 0x30;

    private static final int[] BUTTON_MASKS = {8, 16, 4, 2, 64, 1, 32, 0};

    private final MachineProfile profile;
    private final byte[] rom;
    private final boolean spl03;
    private final byte[] ram;
    private final byte[] lcdRam = new byte[LCD_RAM_SIZE];
    private final boolean[] buttons = new boolean[MachineProfile.BUTTON_COUNT];

    private long cycleCounter;
    private int instructionCounter;
    private int t2HzCounter;
    private int t256HzCounter;
    private int pc;
    private int sp;
    private int a;
    private int x;
    private int nf;
    private int vf;
    private int df;
    private int bf;
    private int interruptFlag;
    private int zf;
    private int cf;
    private boolean cpuEnabled;
    private boolean roscEnabled;
    private boolean clock32Enabled;
    private int romBank;
    private int pdirPA;
    private int pdirPB;
    private int platchPA;
    private int platchPB;
    private int inputLowPA;
    private int inputHighPA;
    private int inputLowPB;
    private int inputHighPB;
    private int ioCtrl;
    private int interruptConfig;
    private int interruptRequest;
    private int systemCtrl;
    private int prescalar;

    Spl0XCpu(MachineProfile profile, byte[] rom) {
        if (rom == null || rom.length == 0) {
            throw new IllegalArgumentException("Empty SPL0X ROM");
        }
        this.profile = profile;
        this.rom = rom;
        spl03 = profile.coreType == MachineProfile.CORE_SPL03;
        ram = new byte[spl03 ? 0x50 : 0x90];
        reset();
    }

    private void reset() {
        t2HzCounter = 0;
        t256HzCounter = 0;
        pc = 0;
        sp = 0;
        a = 0;
        x = 0;
        setProcessorStatus(0x04);
        cpuEnabled = true;
        roscEnabled = true;
        clock32Enabled = true;
        clear(ram);
        clear(lcdRam);
        romBank = 0;
        pdirPA = 0;
        pdirPB = 0;
        platchPA = 0;
        platchPB = 0;
        inputLowPA = 0;
        inputHighPA = 0;
        inputLowPB = 0;
        inputHighPB = 0;
        ioCtrl = 0;
        interruptConfig = 0;
        interruptRequest = 0;
        systemCtrl = 0;
        prescalar = 1;
        cycleCounter = 0;
        instructionCounter = 0;
        goVector(VADDR_RESET);
    }

    private void clear(byte[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = 0;
        }
    }

    public synchronized void setButton(int button, boolean down) {
        if (button < 0 || button >= buttons.length || buttons[button] == down) {
            return;
        }
        buttons[button] = down;
        if (button == MachineProfile.BUTTON_RESET) {
            if (down) {
                reset();
                buttons[button] = true;
            }
            return;
        }
        int mask = BUTTON_MASKS[button];
        int previous = portReadPA();
        inputLowPA &= ~mask;
        inputHighPA &= ~mask;
        if (down) {
            inputHighPA |= mask;
        }
        if ((previous & profile.powerKeyPA) < (portReadPA() & profile.powerKeyPA)) {
            if ((interruptConfig & IO_INT_CFG_POWERKEY_INT) != 0) {
                interruptRequest |= IO_INT_CFG_POWERKEY_INT;
                nmi();
            }
        }
    }

    public synchronized int runCycles(int budget) {
        int consumed = 0;
        while (consumed < budget) {
            consumed += clock();
        }
        return consumed;
    }

    private int clock() {
        int executeCycles = 1;
        if (roscEnabled) {
            if (cpuEnabled) {
                int address = getPc();
                int opcodeByte = readRom(address);
                int bytes = instructionBytes(opcodeByte);
                int opcode = readRomBytes(address, bytes);
                pc += bytes;
                executeCycles = execute(opcodeByte, opcode);
                instructionCounter++;
            }
            timersClock(executeCycles);
        } else if ((systemCtrl & IO_SYS_CTRL_32K_ENBL) != 0) {
            executeCycles = profile.nonCrystalDiv;
            timersClock(executeCycles);
        }
        cycleCounter += executeCycles;
        return executeCycles;
    }

    private void timersClock(int cycles) {
        t2HzCounter -= cycles;
        while (t2HzCounter <= 0) {
            t2HzCounter += profile.nonCrystalDiv * (SUB_CLOCK / 2);
            if ((interruptConfig & IO_INT_CFG_T2HZ_INT) != 0) {
                interruptRequest |= IO_INT_CFG_T2HZ_INT;
                nmi();
            }
        }
        t256HzCounter -= cycles;
        while (t256HzCounter <= 0) {
            t256HzCounter += profile.nonCrystalDiv * (SUB_CLOCK / 256);
            if ((interruptConfig & IO_INT_CFG_T256HZ_INT) != 0) {
                interruptRequest |= IO_INT_CFG_T256HZ_INT;
                nmi();
            }
        }
    }

    private int getPc() {
        if (pc > 0xFFF) {
            return (pc % ADDRESS_SPACE_SIZE) + (romBank << 12);
        }
        return pc;
    }

    private int readRom(int address) {
        int index = address % rom.length;
        if (index < 0) {
            index += rom.length;
        }
        return rom[index] & 255;
    }

    private int readRomBytes(int address, int count) {
        int value = 0;
        for (int i = 0; i < count; i++) {
            value = (value << 8) | readRom(address + i);
        }
        return value;
    }

    private int readRomWordLsb(int address) {
        return readRom(address) | (readRom(address + 1) << 8);
    }

    private void goVector(int address) {
        pc = readRomWordLsb(address + (romBank << 12));
    }

    private void irq() {
        writeMemory(sp, pc >> 8);
        sp = (sp - 1) & 255;
        writeMemory(sp, pc & 255);
        sp = (sp - 1) & 255;
        writeMemory(sp, processorStatus());
        sp = (sp - 1) & 255;
        interruptFlag = 1;
        goVector(VADDR_IRQ);
    }

    private void nmi() {
        if ((interruptConfig & IO_INT_CFG_NMI_ENBL) == 0) {
            return;
        }
        if (roscEnabled && cpuEnabled) {
            writeMemory(sp, pc >> 8);
            sp = (sp - 1) & 255;
            writeMemory(sp, pc & 255);
            sp = (sp - 1) & 255;
            writeMemory(sp, processorStatus());
            sp = (sp - 1) & 255;
            goVector(VADDR_NMI);
        } else {
            cpuEnabled = true;
            roscEnabled = true;
            goVector(VADDR_RESET);
        }
    }

    private int portReadPA() {
        return (((~pdirPA) & platchPA)
                | (pdirPA & ((~inputLowPA) & (inputHighPA | profile.pullupPA)))) & 255;
    }

    private int portReadPB() {
        return (((~pdirPB) & platchPB)
                | (pdirPB & ((~inputLowPB) & (inputHighPB | profile.pullupPB)))) & 255;
    }

    private void writeMemory(int address, int value) {
        value &= 255;
        if (address >= LCD_RAM_OFFSET && address < LCD_RAM_OFFSET + LCD_RAM_SIZE) {
            lcdRam[address - LCD_RAM_OFFSET] = (byte) value;
        } else if (address >= CPU_RAM_OFFSET && address < CPU_RAM_OFFSET + ram.length) {
            ram[address - CPU_RAM_OFFSET] = (byte) value;
        } else {
            writeIo(address, value);
        }
    }

    private int readMemory(int address) {
        if (address >= LCD_RAM_OFFSET && address < LCD_RAM_OFFSET + LCD_RAM_SIZE) {
            return lcdRam[address - LCD_RAM_OFFSET] & 255;
        }
        if (address >= CPU_RAM_OFFSET && address < CPU_RAM_OFFSET + ram.length) {
            return ram[address - CPU_RAM_OFFSET] & 255;
        }
        if (address >= SFR_OFFSET && address < SFR_OFFSET + SFR_SIZE) {
            return readIo(address);
        }
        if (address >= 0x1000) {
            address = (address % ADDRESS_SPACE_SIZE) + (romBank << 12);
        }
        return readRom(address);
    }

    private int readIo(int address) {
        switch (address) {
            case 0xC0:
                return ioCtrl;
            case 0xC1:
                return portReadPA();
            case 0xC3:
                return portReadPB();
            case 0xC4:
            case 0xC6:
                return 0;
            case 0xC8:
            case 0xCA:
                return spl03 ? 0 : 0;
            case 0xCC:
            case 0xCE:
                return 0;
            case 0xD0:
                return systemCtrl;
            case 0xD2:
                int value = interruptRequest | (interruptConfig & 0x80);
                interruptRequest = 0;
                return value;
            case 0xD4:
                return 0;
            case 0xD5:
                return (int) ((cycleCounter >> 3) & 1);
            case 0xD7:
                return romBank;
            default:
                return 0;
        }
    }

    private void writeIo(int address, int value) {
        switch (address) {
            case 0xC0:
                if ((value & IO_CTRL_ALDIR) != 0) {
                    pdirPA |= 0x0F;
                }
                if ((value & IO_CTRL_AHDIR) != 0) {
                    pdirPA |= 0xF0;
                }
                if ((value & IO_CTRL_BDIR) != 0) {
                    pdirPB |= 0x03;
                }
                roscEnabled = ((ioCtrl | ~value) & IO_CTRL_ROSC) != 0;
                prescalar = (value & IO_CTRL_CPU_CLOCK) != 0 ? 8 : 1;
                ioCtrl = value;
                break;
            case 0xC1:
                platchPA = value;
                break;
            case 0xC3:
                platchPB = value;
                break;
            case 0xC4:
            case 0xC6:
            case 0xC8:
            case 0xCA:
            case 0xCC:
            case 0xCE:
                break;
            case 0xD0:
                roscEnabled = ((systemCtrl | ~value) & IO_SYS_CTRL_ROSC_STOP) != 0;
                cpuEnabled = ((systemCtrl | ~value) & IO_SYS_CTRL_CPU_STOP) != 0;
                systemCtrl = value;
                clock32Enabled = (value & IO_SYS_CTRL_32K_ENBL) != 0;
                break;
            case 0xD2:
                interruptConfig = value;
                break;
            case 0xD4:
            case 0xD5:
                break;
            case 0xD7:
                romBank = spl03 ? value & 1 : value;
                break;
            default:
                break;
        }
    }

    private int processorStatus() {
        return (nf << 7) | (vf << 6) | (bf << 4) | (df << 3)
                | (interruptFlag << 2) | (zf << 1) | cf;
    }

    private void setProcessorStatus(int status) {
        nf = (status >> 7) & 1;
        vf = (status & 0x40) != 0 ? 1 : 0;
        bf = (status & 0x10) != 0 ? 1 : 0;
        df = (status & 0x08) != 0 ? 1 : 0;
        interruptFlag = (status & 0x04) != 0 ? 1 : 0;
        zf = (status & 0x02) != 0 ? 1 : 0;
        cf = status & 1;
    }

    private void adc(int operand) {
        int oldA = a;
        int value = oldA + operand + cf;
        if (df != 0 && (oldA & 15) + (operand & 15) + cf > 9) {
            value += 6;
        }
        vf = ((~(oldA ^ operand) & (oldA ^ value)) >> 7) & 1;
        nf = (value >> 7) & 1;
        if (df != 0 && value > 0x99) {
            value += 0x60;
        }
        zf = (value & 255) == 0 ? 1 : 0;
        cf = value > 255 ? 1 : 0;
        a = value & 255;
    }

    private void sbc(int operand) {
        int oldA = a;
        int borrow = cf == 0 ? 1 : 0;
        int value = oldA - operand - borrow;
        if (df != 0) {
            if ((oldA & 15) - (operand & 15) - borrow < 0) {
                value -= 6;
            }
            if (value < 0) {
                value -= 0x60;
            }
        }
        vf = (((oldA ^ operand) & (oldA ^ value)) >> 7) & 1;
        nf = (value >> 7) & 1;
        zf = (value & 255) == 0 ? 1 : 0;
        cf = value >= 0 ? 1 : 0;
        a = value & 255;
    }

    private int instructionBytes(int opcode) {
        switch (opcode) {
            case 0x20:
            case 0x2C:
            case 0x4C:
            case 0x6C:
            case 0x8E:
            case 0xAD:
            case 0xAE:
            case 0xBD:
                return 3;
            case 0x05:
            case 0x09:
            case 0x10:
            case 0x24:
            case 0x25:
            case 0x26:
            case 0x29:
            case 0x30:
            case 0x45:
            case 0x49:
            case 0x50:
            case 0x55:
            case 0x65:
            case 0x66:
            case 0x69:
            case 0x70:
            case 0x81:
            case 0x85:
            case 0x86:
            case 0x90:
            case 0x95:
            case 0xA1:
            case 0xA2:
            case 0xA5:
            case 0xA6:
            case 0xA9:
            case 0xB0:
            case 0xB5:
            case 0xC5:
            case 0xC6:
            case 0xC9:
            case 0xD0:
            case 0xD5:
            case 0xD6:
            case 0xE0:
            case 0xE4:
            case 0xE5:
            case 0xE6:
            case 0xE9:
            case 0xF0:
                return 2;
            default:
                return 1;
        }
    }

    private int branch(int opcode, boolean condition) {
        if (!condition) {
            return 2;
        }
        int previous = pc;
        int relative = opcode & 255;
        pc = (pc + relative - ((relative & 0x80) << 1)) & 0xFFFF;
        return 3 + (((pc ^ previous) > 255) ? 1 : 0);
    }

    private int execute(int instruction, int opcode) {
        int value;
        int address;
        int previous;
        switch (instruction) {
            case 0x00:
                bf = 1;
                pc = (pc + 1) & 0xFFFF;
                irq();
                return 7;
            case 0x05:
                a |= readMemory(opcode & 255);
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 3;
            case 0x08:
                writeMemory(sp, processorStatus());
                sp = (sp - 1) & 255;
                return 3;
            case 0x09:
                a |= opcode & 255;
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 2;
            case 0x10:
                return branch(opcode, nf == 0);
            case 0x18:
                cf = 0;
                return 2;
            case 0x20:
                value = pc - 1;
                writeMemory(sp, (value >> 8) & 255);
                sp = (sp - 1) & 255;
                writeMemory(sp, value & 255);
                sp = (sp - 1) & 255;
                pc = ((opcode >> 8) & 255) | ((opcode & 255) << 8);
                return 6;
            case 0x24:
                value = readMemory(opcode & 255);
                nf = value >> 7;
                vf = (value & 0x40) != 0 ? 1 : 0;
                zf = (a & value) == 0 ? 1 : 0;
                return 3;
            case 0x25:
                a &= readMemory(opcode & 255);
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 3;
            case 0x26:
                value = (readMemory(opcode & 255) << 1) | cf;
                writeMemory(opcode & 255, value & 255);
                nf = (value & 0x80) != 0 ? 1 : 0;
                zf = (value & 255) == 0 ? 1 : 0;
                cf = value > 255 ? 1 : 0;
                return 5;
            case 0x28:
                sp = (sp + 1) & 255;
                setProcessorStatus(readMemory(sp));
                return 4;
            case 0x29:
                a &= opcode;
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 2;
            case 0x2A:
                value = (a << 1) | cf;
                a = value & 255;
                nf = (value & 0x80) != 0 ? 1 : 0;
                zf = (value & 255) == 0 ? 1 : 0;
                cf = value > 255 ? 1 : 0;
                return 2;
            case 0x2C:
                address = ((opcode & 255) << 8) | ((opcode >> 8) & 255);
                value = readMemory(address);
                nf = value >> 7;
                vf = (value & 0x40) != 0 ? 1 : 0;
                zf = (a & value) == 0 ? 1 : 0;
                return 4;
            case 0x30:
                return branch(opcode, nf != 0);
            case 0x38:
                cf = 1;
                return 2;
            case 0x40:
                sp = (sp + 1) & 255;
                setProcessorStatus(readMemory(sp));
                sp = (sp + 1) & 255;
                pc = readMemory(sp);
                sp = (sp + 1) & 255;
                pc |= readMemory(sp) << 8;
                return 6;
            case 0x45:
                a ^= readMemory(opcode & 255);
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 3;
            case 0x48:
                writeMemory(sp, a);
                sp = (sp - 1) & 255;
                return 3;
            case 0x49:
                a ^= opcode & 255;
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 2;
            case 0x4C:
                pc = ((opcode & 255) << 8) | ((opcode >> 8) & 255);
                return 3;
            case 0x50:
                return branch(opcode, vf == 0);
            case 0x55:
                a ^= readMemory((opcode + x) & 255);
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 4;
            case 0x58:
                interruptFlag = 0;
                return 2;
            case 0x60:
                sp = (sp + 1) & 255;
                pc = readMemory(sp);
                sp = (sp + 1) & 255;
                pc |= readMemory(sp) << 8;
                pc++;
                return 6;
            case 0x65:
                adc(readMemory(opcode & 255));
                return 3;
            case 0x66:
                previous = readMemory(opcode & 255);
                writeMemory(opcode & 255, (previous >> 1) | (cf << 7));
                nf = cf;
                zf = ((previous & 0xFE) | cf) == 0 ? 1 : 0;
                cf = previous & 1;
                return 5;
            case 0x68:
                sp = (sp + 1) & 255;
                a = readMemory(sp);
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 4;
            case 0x69:
                adc(opcode & 255);
                return 2;
            case 0x6A:
                previous = a;
                a = (previous >> 1) | (cf << 7);
                nf = cf;
                zf = ((previous & 0xFE) | cf) == 0 ? 1 : 0;
                cf = previous & 1;
                return 2;
            case 0x6C:
                address = ((opcode >> 8) & 255) | ((opcode & 255) << 8);
                pc = readMemory(address);
                pc |= readMemory((address + 1) & 0xFFFF) << 8;
                return 6;
            case 0x70:
                return branch(opcode, vf != 0);
            case 0x78:
                interruptFlag = 1;
                return 2;
            case 0x81:
                address = readMemory((opcode + x) & 255)
                        | (readMemory((opcode + x + 1) & 255) << 8);
                writeMemory(address, a);
                return 6;
            case 0x85:
                writeMemory(opcode & 255, a);
                return 3;
            case 0x86:
                writeMemory(opcode & 255, x);
                return 3;
            case 0x8A:
                a = x;
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 2;
            case 0x8E:
                address = ((opcode & 255) << 8) | ((opcode >> 8) & 255);
                writeMemory(address, x);
                return 4;
            case 0x90:
                return branch(opcode, cf == 0);
            case 0x95:
                writeMemory((opcode + x) & 255, a);
                return 4;
            case 0x9A:
                sp = x;
                return 2;
            case 0xA1:
                address = readMemory((opcode + x) & 255)
                        | (readMemory((opcode + x + 1) & 255) << 8);
                a = readMemory(address);
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 6;
            case 0xA2:
                x = opcode & 255;
                nf = x >> 7;
                zf = x == 0 ? 1 : 0;
                return 2;
            case 0xA5:
                a = readMemory(opcode & 255);
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 3;
            case 0xA6:
                x = readMemory(opcode & 255);
                nf = x >> 7;
                zf = x == 0 ? 1 : 0;
                return 3;
            case 0xA9:
                a = opcode & 255;
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 2;
            case 0xAA:
                x = a;
                nf = x >> 7;
                zf = x == 0 ? 1 : 0;
                return 2;
            case 0xAD:
                a = readMemory(((opcode & 255) << 8) | ((opcode >> 8) & 255));
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 4;
            case 0xAE:
                address = ((opcode & 255) << 8) | ((opcode >> 8) & 255);
                x = readMemory(address);
                nf = x >> 7;
                zf = x == 0 ? 1 : 0;
                return 4;
            case 0xB0:
                return branch(opcode, cf != 0);
            case 0xB5:
                a = readMemory((opcode + x) & 255);
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 4;
            case 0xB8:
                vf = 0;
                return 2;
            case 0xBA:
                x = sp;
                nf = x >> 7;
                zf = x == 0 ? 1 : 0;
                return 2;
            case 0xBD:
                address = (((opcode << 8) | ((opcode >> 8) & 255)) + x) & 0xFFFF;
                a = readMemory(address);
                nf = a >> 7;
                zf = a == 0 ? 1 : 0;
                return 4 + (((pc ^ address) > 255) ? 1 : 0);
            case 0xC5:
                value = a - readMemory(opcode & 255);
                nf = (value >> 7) & 1;
                zf = value == 0 ? 1 : 0;
                cf = value >= 0 ? 1 : 0;
                return 3;
            case 0xC6:
                value = (readMemory(opcode & 255) - 1) & 255;
                writeMemory(opcode & 255, value);
                nf = value >> 7;
                zf = value == 0 ? 1 : 0;
                return 5;
            case 0xC9:
                value = a - (opcode & 255);
                nf = (value >> 7) & 1;
                zf = value == 0 ? 1 : 0;
                cf = value >= 0 ? 1 : 0;
                return 2;
            case 0xCA:
                x = (x - 1) & 255;
                nf = x >> 7;
                zf = x == 0 ? 1 : 0;
                return 2;
            case 0xD0:
                return branch(opcode, zf == 0);
            case 0xD5:
                value = a - readMemory((opcode + x) & 255);
                nf = (value >> 7) & 1;
                zf = value == 0 ? 1 : 0;
                cf = value >= 0 ? 1 : 0;
                return 4;
            case 0xD6:
                address = (opcode + x) & 255;
                value = (readMemory(address) - 1) & 255;
                writeMemory(address, value);
                nf = value >> 7;
                zf = value == 0 ? 1 : 0;
                return 6;
            case 0xE0:
                value = x - (opcode & 255);
                nf = (value >> 7) & 1;
                zf = value == 0 ? 1 : 0;
                cf = value >= 0 ? 1 : 0;
                return 2;
            case 0xE4:
                value = x - readMemory(opcode & 255);
                nf = (value >> 7) & 1;
                zf = value == 0 ? 1 : 0;
                cf = value >= 0 ? 1 : 0;
                return 3;
            case 0xE5:
                sbc(readMemory(opcode & 255));
                return 3;
            case 0xE6:
                value = (readMemory(opcode & 255) + 1) & 255;
                writeMemory(opcode & 255, value);
                nf = value >> 7;
                zf = value == 0 ? 1 : 0;
                return 5;
            case 0xE8:
                x = (x + 1) & 255;
                nf = x >> 7;
                zf = x == 0 ? 1 : 0;
                return 2;
            case 0xE9:
                sbc(opcode & 255);
                return 2;
            case 0xEA:
                return 2;
            case 0xF0:
                return branch(opcode, zf != 0);
            case 0xF8:
                df = 1;
                return 2;
            default:
                return 2;
        }
    }

    public byte[] vram() {
        return lcdRam;
    }

    public boolean displayEnabled() {
        return true;
    }

    public int programCounter() {
        return getPc();
    }
}
