package brickgame;

final class Ks56Cpu implements BrickCpu {
    private static final int SCALE = 32768;
    private static final int SUB_CLOCK = 32768;
    private static final int RAM_SIZE = 0x1000;
    private static final int VRAM_OFFSET = 0x100;
    private static final int VRAM_SIZE = 256;
    private static final int ROM_MASK = 0xFFF;

    private static final int MODE_NORMAL = 0;
    private static final int MODE_HALT = 1;
    private static final int MODE_STOP = 2;

    private static final int REG_A = 0;
    private static final int REG_X = 1;
    private static final int REG_L = 2;
    private static final int REG_H = 3;
    private static final int REG_E = 4;
    private static final int REG_D = 5;
    private static final int REG_C = 6;
    private static final int REG_B = 7;

    private static final int RPE_XA = 0;
    private static final int RPE_XAE = 1;
    private static final int RPE_HL = 2;
    private static final int RPE_HLE = 3;
    private static final int RPE_DE = 4;
    private static final int RPE_DEE = 5;
    private static final int RPE_BC = 6;
    private static final int RPE_BCE = 7;

    private static final int IRQ_INTBT = 1;
    private static final int IRQ_INT4 = 1;
    private static final int IRQ_INT0 = 2;
    private static final int IRQ_INT1 = 3;
    private static final int IRQ_INTT0 = 5;

    private static final int INTA_IRQBT = 1;
    private static final int INTA_IEBT = 2;
    private static final int INTA_IRQ4 = 4;
    private static final int INTA_IE4 = 8;
    private static final int INTC_IRQW = 1;
    private static final int INTC_IEW = 2;
    private static final int INTE_IRQT0 = 1;
    private static final int INTE_IET0 = 2;
    private static final int INTG_IRQ0 = 1;
    private static final int INTG_IE0 = 2;
    private static final int INTG_IRQ1 = 4;
    private static final int INTG_IE1 = 8;
    private static final int INTH_IRQ2 = 1;
    private static final int INTH_IE2 = 2;

    private static final int[] BASIC_TIMER_DIV = {4096, 4096, 512, 512, 128, 128, 32};
    private static final int[] WATCH_TIMER_DIV = {16384, 128};
    private static final int[] TIMER_T0_DIV = {0, 0, 0, 0, 1024, 256, 64, 16};
    private static final int[] MAIN_CLOCK_DIV = {64, 16, 8, 4};

    private final MachineProfile profile;
    private final byte[] rom;
    private final byte[] ram = new byte[RAM_SIZE];
    private final byte[] displayRam = new byte[VRAM_SIZE];
    private final boolean[] buttons = new boolean[MachineProfile.BUTTON_COUNT];
    private final int[] buttonPorts = new int[MachineProfile.BUTTON_COUNT];
    private final int[] buttonMasks = new int[MachineProfile.BUTTON_COUNT];
    private final int[][] portInput = new int[10][2];
    private final int[] portLatch = new int[10];

    private int pc;
    private int cy;
    private int sp;
    private int mbe;
    private int mbs;
    private int rbe;
    private int rbs;
    private int sbs;
    private int tmod2h;
    private int wdtm;
    private int bt;
    private int btm;
    private int ist;
    private int sk;
    private int scc;
    private int wml;
    private int wmh;
    private int tm0l;
    private int tm0h;
    private int t0;
    private int tmod0;
    private int ime;
    private int ips;
    private int pccMode;
    private int pccClock;
    private int im0;
    private int im1;
    private int im2;
    private int inta;
    private int intc;
    private int inte;
    private int intf;
    private int intg;
    private int inth;
    private int poga;
    private int pogb;
    private int pm3;
    private int pm6;

    private long basicTimerUnits;
    private long t0TimerUnits;
    private long watchTimerUnits;
    private long systemCycleUnits;
    private int instructionCounter;

    Ks56Cpu(MachineProfile profile, byte[] rom) {
        if (rom == null || rom.length == 0) {
            throw new IllegalArgumentException("Empty KS56 ROM");
        }
        this.profile = profile;
        this.rom = rom;
        configureButtons();
        reset();
    }

    private void configureButtons() {
        for (int i = 0; i < buttonPorts.length; i++) {
            buttonPorts[i] = -1;
            buttonMasks[i] = 0;
        }
        if ("ga878".equals(profile.id)) {
            map(MachineProfile.BUTTON_LEFT, 1, 8);
            map(MachineProfile.BUTTON_RIGHT, 1, 4);
            map(MachineProfile.BUTTON_DOWN, 6, 1);
            map(MachineProfile.BUTTON_ROTATE, 6, 2);
            map(MachineProfile.BUTTON_START, 6, 4);
            map(MachineProfile.BUTTON_AUX, 6, 2);
            map(MachineProfile.BUTTON_OPTION, 1, 8);
        } else {
            map(MachineProfile.BUTTON_LEFT, 1, 1);
            map(MachineProfile.BUTTON_RIGHT, 1, 4);
            map(MachineProfile.BUTTON_DOWN, 1, 2);
            map(MachineProfile.BUTTON_ROTATE, 6, 4);
            map(MachineProfile.BUTTON_START, 1, 8);
            map(MachineProfile.BUTTON_AUX, 6, 2);
            map(MachineProfile.BUTTON_OPTION, 6, 1);
        }
    }

    private void map(int button, int port, int mask) {
        buttonPorts[button] = port;
        buttonMasks[button] = mask;
    }

    private void reset() {
        clear(ram);
        for (int i = 0; i < portInput.length; i++) {
            portInput[i][0] = 0;
            portInput[i][1] = 0;
            portLatch[i] = 0;
        }
        pc = 0;
        cy = 0;
        sp = 0;
        mbe = 0;
        mbs = 0;
        rbe = 0;
        rbs = 0;
        sbs = 0;
        tmod2h = 0xFF;
        wdtm = 0;
        bt = 0;
        btm = 0;
        ist = 0;
        sk = 0;
        scc = 0;
        wml = 0;
        wmh = 0;
        tm0l = 0;
        tm0h = 0;
        t0 = 0;
        tmod0 = 0xFF;
        ime = 0;
        ips = 0;
        pccMode = MODE_NORMAL;
        pccClock = 0;
        im0 = 0;
        im1 = 0;
        im2 = 0;
        inta = 0;
        intc = 0;
        inte = 0;
        intf = 0;
        intg = 0;
        inth = 0;
        poga = 0;
        pogb = 0;
        pm3 = 0;
        pm6 = 0;
        basicTimerUnits = 0;
        t0TimerUnits = 0;
        watchTimerUnits = 0;
        systemCycleUnits = 0;
        instructionCounter = 0;
        for (int i = 0; i < buttons.length; i++) buttons[i] = false;
        goVector(0);
    }

    private static void clear(byte[] data) {
        for (int i = 0; i < data.length; i++) data[i] = 0;
    }

    public synchronized void setButton(int button, boolean down) {
        if (button < 0 || button >= buttons.length || buttons[button] == down) return;
        buttons[button] = down;
        if (button == MachineProfile.BUTTON_RESET) {
            if (down) {
                reset();
                buttons[button] = true;
            }
            return;
        }
        int port = buttonPorts[button];
        int mask = buttonMasks[button];
        if (port < 0 || mask == 0) return;
        setPortInput(port, mask, down ? 0 : -1);
    }

    private void setPortInput(int port, int mask, int level) {
        int previous = readPort(port);
        portInput[port][0] &= ~mask;
        portInput[port][1] &= ~mask;
        if (level >= 0) portInput[port][level] |= mask;
        int current = readPort(port);
        if (port == 1 && previous != current) {
            if ((mask & 2) != 0 && im1 != ((current >> 1) & 1)) intg |= INTG_IRQ1;
            if ((mask & 1) != 0 && im0 != 3
                    && (((im0 & 1) != level) || (im0 & 2) != 0)) intg |= INTG_IRQ0;
        } else if (port == 6 && level == 0 && previous != current
                && (pm6 & mask) == 0
                && (im2 == 3 || (im2 == 2 && (mask & 12) != 0))) {
            inth |= INTH_IRQ2;
        }
    }

    public synchronized int runCycles(int budget) {
        long target = (long) budget * SCALE;
        long used = 0;
        while (used < target) used += clockUnits();
        return (int) ((used + SCALE - 1) / SCALE);
    }

    private long clockUnits() {
        long cpuCycleUnits = currentCpuCycleUnits();
        int executeTime = 1;
        if (pccMode == MODE_NORMAL) {
            int first = readRom(pc);
            int bytes = instructionBytes(first);
            int opcode = readRomBytes(pc, bytes);
            pc = (pc + bytes) & ROM_MASK;
            executeTime = execute(first, opcode);
            instructionCounter++;
        } else if (wakeRequested()) {
            pccMode = MODE_NORMAL;
        }
        long units = (long) executeTime * cpuCycleUnits;
        if (pccMode != MODE_STOP) {
            processWatchTimer(units);
            if ((scc & 8) == 0) {
                processBasicTimer(units);
                processT0Timer(units);
            }
        }
        if (ime != 0) {
            int irq = interruptVector();
            if (irq > 0) interrupt(irq);
        }
        systemCycleUnits += units;
        return units;
    }

    private long currentCpuCycleUnits() {
        if ((scc & 1) != 0) return profile.clockHz;
        return (long) MAIN_CLOCK_DIV[pccClock & 3] * SCALE;
    }

    private boolean wakeRequested() {
        return ((intc & INTC_IRQW) != 0 && (intc & INTC_IEW) != 0)
                || ((inth & INTH_IRQ2) != 0 && (inth & INTH_IE2) != 0)
                || ((intg & INTG_IRQ0) != 0 && (intg & INTG_IE0) != 0 && (im0 & 4) != 0)
                || ((intg & INTG_IRQ1) != 0 && (intg & INTG_IE1) != 0)
                || ((inte & INTE_IET0) != 0 && (inte & INTE_IRQT0) != 0)
                || ((inta & INTA_IEBT) != 0 && (inta & INTA_IRQBT) != 0);
    }

    private void processBasicTimer(long units) {
        basicTimerUnits -= units;
        long reload = (long) BASIC_TIMER_DIV[btm] * SCALE;
        while (basicTimerUnits <= 0) {
            basicTimerUnits += reload;
            bt = (bt + 1) & 255;
            if (bt == 0 && wdtm == 0) inta |= INTA_IRQBT;
        }
    }

    private void processT0Timer(long units) {
        int div = TIMER_T0_DIV[tm0h & 7];
        if (div > 0 && (tm0l & 4) != 0) {
            t0TimerUnits -= units;
            long reload = (long) div * SCALE;
            while (t0TimerUnits <= 0) {
                t0TimerUnits += reload;
                t0 = (t0 + 1) & 255;
                if (t0 == tmod0) {
                    inte |= INTE_IRQT0;
                    t0 = 0;
                }
            }
        }
    }

    private void processWatchTimer(long units) {
        if ((wml & 4) != 0) {
            watchTimerUnits -= units;
            long reload;
            if ((wml & 1) != 0) reload = (long) WATCH_TIMER_DIV[(wml >> 1) & 1] * profile.clockHz;
            else reload = (long) WATCH_TIMER_DIV[(wml >> 1) & 1] * 128L * SCALE;
            while (watchTimerUnits <= 0) {
                watchTimerUnits += reload;
                intc |= INTC_IRQW;
            }
        } else watchTimerUnits = 0;
    }

    private int readRom(int address) {
        int index = address % rom.length;
        if (index < 0) index += rom.length;
        return rom[index] & 255;
    }

    private int readRomBytes(int address, int count) {
        int value = 0;
        for (int i = 0; i < count; i++) value = (value << 8) | readRom(address + i);
        return value;
    }

    private int readRomWord(int address) {
        return (readRom(address) << 8) | readRom(address + 1);
    }

    private void goVector(int address) {
        int vector = readRomWord(address);
        mbe = (vector >> 15) & 1;
        rbe = (vector >> 14) & 1;
        pc = vector & ROM_MASK;
    }

    private void interrupt(int irq) {
        stackPush((cy << 3) | sk);
        stackPush((ist << 2) | (mbe << 1) | rbe);
        stackPush((pc >> 4) & 15);
        stackPush(pc & 15);
        stackPush((mbe << 3) | (rbe << 2) | ((pc >> 12) & 1));
        stackPush((pc >> 8) & 15);
        goVector(irq << 1);
        ist++;
        if (irq == IRQ_INTBT) {
            if ((inta & INTA_IEBT) == 0) inta &= ~INTA_IRQ4;
            else if ((inta & INTA_IE4) == 0) inta &= ~INTA_IRQBT;
        } else if (irq == IRQ_INTT0) inte &= ~INTE_IRQT0;
        else if (irq == IRQ_INT0) intg &= ~INTG_IRQ0;
        else if (irq == IRQ_INT1) intg &= ~INTG_IRQ1;
    }

    private int interruptVector() {
        int irq = 0;
        if ((inte & INTE_IET0) != 0 && (inte & INTE_IRQT0) != 0) {
            if (IRQ_INTT0 == ips && ist <= 1) return IRQ_INTT0;
            else if (ist == 0) irq = IRQ_INTT0;
        }
        if ((intg & INTG_IE1) != 0 && (intg & INTG_IRQ1) != 0) {
            if (IRQ_INT1 == ips && ist <= 1) return IRQ_INT1;
            else if (ist == 0) irq = IRQ_INT1;
        }
        if ((intg & INTG_IE0) != 0 && (intg & INTG_IRQ0) != 0) {
            if (IRQ_INT0 == ips && ist <= 1) return IRQ_INT0;
            else if (ist == 0) irq = IRQ_INT0;
        }
        if ((inta & INTA_IE4) != 0 && (inta & INTA_IRQ4) != 0) {
            if (IRQ_INT4 == ips && ist <= 1) return IRQ_INT4;
            else if (ist == 0) irq = IRQ_INT4;
        }
        if ((inta & INTA_IEBT) != 0 && (inta & INTA_IRQBT) != 0) {
            if (IRQ_INTBT == ips && ist <= 1) return IRQ_INTBT;
            else if (ist == 0) irq = IRQ_INTBT;
        }
        return irq;
    }

    private int getMem(int address) {
        int bank = 15;
        if (mbe != 0 || address < 0x80) bank = mbe * mbs;
        if (bank != 15) return ram[(bank << 8) + address] & 15;
        return readIo((bank << 8) + address);
    }

    private void setMem(int address, int value) {
        value &= 15;
        int bank = 15;
        if (mbe != 0 || address < 0x80) bank = mbe * mbs;
        if (bank != 15) ram[(bank << 8) + address] = (byte) value;
        else writeIo((bank << 8) + address, value);
    }

    private int getAhl() {
        int bank = mbe * mbs;
        int address = getRp(RPE_HL);
        if (bank != 15) return ram[(bank << 8) + address] & 15;
        return readIo((bank << 8) + address);
    }

    private int getAhlByte() {
        int bank = mbe * mbs;
        int address = (bank << 8) + getRp(RPE_HL);
        if (bank != 15) return ((ram[address + 1] & 15) << 4) | (ram[address] & 15);
        return (readIo(address + 1) << 4) | readIo(address);
    }

    private void setAhl(int value) {
        int bank = mbe * mbs;
        int address = getRp(RPE_HL);
        if (bank != 15) ram[(bank << 8) + address] = (byte) (value & 15);
        else writeIo((bank << 8) + address, value);
    }

    private void setAhlByte(int value) {
        int bank = mbe * mbs;
        int address = (bank << 8) + getRp(RPE_HL);
        if (bank != 15) {
            ram[address] = (byte) (value & 15);
            ram[address + 1] = (byte) ((value >> 4) & 15);
        } else {
            writeIo(address, value & 15);
            writeIo(address + 1, value >> 4);
        }
    }

    private int getHmem(int opcode) {
        int address = (getReg(REG_H) << 4) | (opcode & 15);
        int bank = mbe * mbs;
        return bank != 15 ? ram[(bank << 8) + address] & 15 : readIo((bank << 8) + address);
    }

    private void setHmem(int opcode, int value) {
        int address = (getReg(REG_H) << 4) | (opcode & 15);
        int bank = mbe * mbs;
        if (bank != 15) ram[(bank << 8) + address] = (byte) (value & 15);
        else writeIo((bank << 8) + address, value);
    }

    private int getPmeml(int opcode) {
        int address = 0xFC0 | ((opcode & 15) << 2) | (getReg(REG_L) >> 2);
        return readIo(address);
    }

    private void setPmeml(int opcode, int value) {
        int address = 0xFC0 | ((opcode & 15) << 2) | (getReg(REG_L) >> 2);
        writeIo(address, value);
    }

    private int getFmem(int opcode) {
        return readIo(0xFB0 | (opcode & 0x4F));
    }

    private void setFmem(int opcode, int value) {
        writeIo(0xFB0 | (opcode & 0x4F), value);
    }

    private int getReg(int reg) {
        return ram[rbe * rbs * 8 + reg] & 15;
    }

    private void setReg(int reg, int value) {
        ram[rbe * rbs * 8 + reg] = (byte) (value & 15);
    }

    private int getRp(int rp) {
        int offset = (rbe * rbs * 8 + (rp & 6)) ^ ((rp & 1) << 3);
        return ((ram[offset + 1] & 15) << 4) | (ram[offset] & 15);
    }

    private void setRp(int rp, int value) {
        int offset = (rbe * rbs * 8 + (rp & 6)) ^ ((rp & 1) << 3);
        ram[offset] = (byte) (value & 15);
        ram[offset + 1] = (byte) ((value >> 4) & 15);
    }

    private void stackPush(int value) {
        sp = (sp - 1) & 255;
        ram[((sbs & 1) << 8) + sp] = (byte) (value & 15);
    }

    private void stackPushByte(int value) {
        sp = (sp - 1) & 255;
        ram[((sbs & 1) << 8) + sp] = (byte) ((value >> 4) & 15);
        sp = (sp - 1) & 255;
        ram[((sbs & 1) << 8) + sp] = (byte) (value & 15);
    }

    private int stackPop() {
        int address = ((sbs & 1) << 8) + sp;
        sp = (sp + 1) & 255;
        return ram[address] & 15;
    }

    private int stackPopByte() {
        int lowAddress = ((sbs & 1) << 8) + sp;
        int highAddress = ((sbs & 1) << 8) + ((sp + 1) & 255);
        sp = (sp + 2) & 255;
        return ((ram[highAddress] & 15) << 4) | (ram[lowAddress] & 15);
    }

    private int skipNext() {
        int first = readRom(pc);
        int bytes = instructionBytes(first);
        pc = (pc + bytes) & ROM_MASK;
        return bytes < 3 ? 1 : 2;
    }

    private int instructionBytes(int first) {
        if ((first >= 0x40 && first <= 0x47) || (first >= 0x50 && first <= 0x5F)
                || first == 0x82 || (first >= 0x84 && first <= 0x87)
                || first == 0x89 || first == 0x8B || first == 0x8D || first == 0x8F
                || (first >= 0x92 && first <= 0x97)
                || (first >= 0x99 && first <= 0x9D) || first == 0x9F
                || (first >= 0xA2 && first <= 0xA7) || first == 0xAA
                || first == 0xAC || first == 0xAE
                || (first >= 0xB2 && first <= 0xB7) || first == 0xB9
                || (first >= 0xBC && first <= 0xBF)) return 2;
        if (first == 0xAB) return 3;
        return 1;
    }

    private int execute(int first, int opcode) {
        if (first <= 0x0F) { pc = (pc + opcode) & ROM_MASK; return 8; }
        if (first <= 0x3F) return executeGeti(opcode);
        if (first <= 0x47) return callFar(opcode);
        if (first <= 0x4F) {
            if ((first & 1) == 0) setRp(first & 6, stackPopByte());
            else stackPushByte(getRp(first & 6));
            return 1;
        }
        if (first <= 0x5F) { pc = ((pc & 0xF000) | (opcode & 0xFFF)) & ROM_MASK; return 2; }
        if (first <= 0x6F) {
            int value = getReg(REG_A) + (first & 15);
            setReg(REG_A, value);
            return value > 15 ? 1 + skipNext() : 1;
        }
        if (first <= 0x7F) return moveAImmediate(first & 15);
        if (first >= 0xC0 && first <= 0xC7) return incReg(first & 7);
        if (first >= 0xC8 && first <= 0xCF) return decReg(first & 7);
        if (first >= 0xD8 && first <= 0xDF) return exchangeAReg(first & 7);
        if (first >= 0xF0) { pc = (pc - (16 - (first & 15))) & ROM_MASK; return 8; }
        switch (first) {
            case 0x80: return getReg(REG_A) == getAhl() ? 1 + skipNext() : 1;
            case 0x82: return incMem(opcode & 255);
            case 0x84: case 0x94: case 0xA4: case 0xB4:
                return memoryBit(opcode, 0);
            case 0x85: case 0x95: case 0xA5: case 0xB5:
                return memoryBit(opcode, 1);
            case 0x86: case 0x96: case 0xA6: case 0xB6:
                return memoryBit(opcode, 2);
            case 0x87: case 0x97: case 0xA7: case 0xB7:
                return memoryBit(opcode, 3);
            case 0x89: return moveXaImmediate(opcode & 255);
            case 0x8A: return incRp(first & 6);
            case 0x8B: return moveHlImmediate(opcode & 255);
            case 0x8C: return incRp(4);
            case 0x8D: setRp(RPE_DE, opcode); return 2;
            case 0x8E: return incRp(6);
            case 0x8F: setRp(RPE_BC, opcode); return 2;
            case 0x90: setReg(REG_A, getReg(REG_A) & getAhl()); return 1;
            case 0x92: setMem(opcode & 255, getReg(REG_A)); setMem((opcode & 255) + 1, getReg(REG_X)); return 2;
            case 0x93: setMem(opcode & 255, getReg(REG_A)); return 2;
            case 0x98: int bit = getReg(REG_A) & 1; setReg(REG_A, (getReg(REG_A) >> 1) | (cy << 3)); cy = bit; return 1;
            case 0x99: return execute99(opcode & 255);
            case 0x9A: return execute9A(opcode & 255);
            case 0x9B: return executeBitFamily(opcode, 0);
            case 0x9C: return executeBitFamily(opcode, 1);
            case 0x9D: return executeBitFamily(opcode, 2);
            case 0x9F: return executeBitFamily(opcode, 3);
            case 0xA0: setReg(REG_A, getReg(REG_A) | getAhl()); return 1;
            case 0xA2: setReg(REG_A, getMem(opcode & 254)); setReg(REG_X, getMem((opcode & 254) + 1)); return 2;
            case 0xA3: setReg(REG_A, getMem(opcode & 255)); return 2;
            case 0xA8: return subSkipAhl(false);
            case 0xA9: return addCarryAhl();
            case 0xAA: return executeAA(opcode & 255);
            case 0xAB: return (opcode & 0x4000) == 0 ? branchAbsolute(opcode) : callAbsolute(opcode);
            case 0xAC: return executeLogicBit(opcode, 0);
            case 0xAE: return executeLogicBit(opcode, 1);
            case 0xB0: setReg(REG_A, getReg(REG_A) ^ getAhl()); return 1;
            case 0xB2: return exchangeXaMem(opcode);
            case 0xB3: int address = opcode & 255; int old = getReg(REG_A); setReg(REG_A, getMem(address)); setMem(address, old); return 2;
            case 0xB8: return subCarryAhl();
            case 0xB9: return addXaImmediate(opcode);
            case 0xBC: return executeLogicBit(opcode, 2);
            case 0xBD: return executeMoveCarryBit(opcode);
            case 0xBE: return executeSkipBit(opcode, false);
            case 0xBF: return executeSkipBit(opcode, true);
            case 0xD0: return moveTable((pc & 0xFF00) | getRp(RPE_XA));
            case 0xD1: return moveTable((getRp(RPE_BC) << 8) | getRp(RPE_XA));
            case 0xD2: int sum = getReg(REG_A) + getAhl(); setReg(REG_A, sum); return sum > 15 ? 1 + skipNext() : 1;
            case 0xD4: return moveTable((pc & 0xFF00) | getRp(RPE_DE));
            case 0xD5: return moveTable((getRp(RPE_BC) << 8) | getRp(RPE_DE));
            case 0xD6: cy ^= 1; return 1;
            case 0xD7: return cy != 0 ? 1 + skipNext() : 1;
            case 0xE0: return returnsAndSkip();
            case 0xE1: setReg(REG_A, getAhl()); return 1;
            case 0xE2: return moveAhlAndChangeL(1, false);
            case 0xE3: return moveAhlAndChangeL(-1, false);
            case 0xE4: setReg(REG_A, ram[getRp(RPE_DE)]); return 1;
            case 0xE5: setReg(REG_A, ram[(getReg(REG_D) << 4) | getReg(REG_L)]); return 1;
            case 0xE6: cy = 0; return 1;
            case 0xE7: cy = 1; return 1;
            case 0xE8: setAhl(getReg(REG_A)); return 1;
            case 0xE9: return exchangeAhl(0);
            case 0xEA: return exchangeAhl(1);
            case 0xEB: return exchangeAhl(-1);
            case 0xEC: return exchangeDirect(getRp(RPE_DE));
            case 0xED: return exchangeDirect((getReg(REG_D) << 4) | getReg(REG_L));
            case 0xEE: return returnNormal(false);
            case 0xEF: return returnNormal(true);
            default: return 0;
        }
    }

    private int executeGeti(int opcode) {
        int tableAddress = opcode << 1;
        int first = readRom(tableAddress);
        int bytes = instructionBytes(first);
        int time = 1;
        if (bytes == 1) {
            if ((first & 0xC0) == 0) {
                pc = readRomWord(tableAddress) & ROM_MASK;
                return 3;
            }
            time += execute(first, first);
            int second = readRom(tableAddress + 1);
            time += execute(second, first);
        } else {
            int nested = readRomBytes(tableAddress, bytes);
            time += execute(first, nested);
        }
        return time;
    }

    private int callFar(int opcode) {
        stackPush((pc >> 4) & 15); stackPush(pc & 15);
        stackPush((mbe << 3) | (rbe << 2) | ((pc >> 12) & 3)); stackPush((pc >> 8) & 15);
        pc = opcode & ROM_MASK;
        return 2;
    }

    private int moveAImmediate(int value) {
        setReg(REG_A, value);
        int cycles = 1;
        int first = readRom(pc);
        while ((first >> 4) == 7 || first == 0x89) {
            cycles++;
            pc = (pc + instructionBytes(first)) & ROM_MASK;
            first = readRom(pc);
        }
        return cycles;
    }

    private int incMem(int address) {
        int value = (getMem(address) + 1) & 15;
        setMem(address, value);
        return value == 0 ? 2 + skipNext() : 2;
    }

    private int memoryBit(int opcode, int operation) {
        int address = opcode & 255;
        int bit = 1 << ((opcode >> 12) & 3);
        int value = getMem(address);
        if (operation == 0) setMem(address, value & ~bit);
        else if (operation == 1) setMem(address, value | bit);
        else if (operation == 2) return (value & bit) == 0 ? 2 + skipNext() : 2;
        else return (value & bit) != 0 ? 2 + skipNext() : 2;
        return 2;
    }

    private int moveXaImmediate(int value) {
        setRp(RPE_XA, value);
        int cycles = 2;
        int first = readRom(pc);
        while ((first >> 4) == 7 || first == 0x89) {
            cycles++;
            pc = (pc + instructionBytes(first)) & ROM_MASK;
            first = readRom(pc);
        }
        return cycles;
    }

    private int moveHlImmediate(int value) {
        setRp(RPE_HL, value);
        int cycles = 2;
        int first = readRom(pc);
        while (first == 0x8B) {
            cycles++;
            pc = (pc + instructionBytes(first)) & ROM_MASK;
            first = readRom(pc);
        }
        return cycles;
    }

    private int incRp(int rp) {
        int value = (getRp(rp) + 1) & 255;
        setRp(rp, value);
        return value == 0 ? 1 + skipNext() : 1;
    }

    private int execute99(int sub) {
        if (sub == 0) { pc = ((pc & 0xFF00) | getRp(RPE_XA)) & ROM_MASK; return 3; }
        if (sub == 1) { pc = ((getReg(REG_B) << 12) | (getReg(REG_C) << 8) | getRp(RPE_XA)) & ROM_MASK; return 3; }
        if (sub == 2) { int value = (getAhl() + 1) & 15; setAhl(value); return value == 0 ? 2 + skipNext() : 2; }
        if (sub == 4) { pc = ((pc & 0xFF00) | getRp(RPE_DE)) & ROM_MASK; return 3; }
        if (sub == 5) { pc = ((getReg(REG_B) << 12) | (getReg(REG_C) << 8) | getRp(RPE_DE)) & ROM_MASK; return 3; }
        if (sub == 6) { rbs = stackPop(); mbs = stackPop(); return 2; }
        if (sub == 7) { stackPush(mbs); stackPush(rbs); return 2; }
        if (sub >= 8 && sub < 16) return getReg(REG_A) == getReg(sub & 7) ? 2 + skipNext() : 2;
        if (sub >= 0x10 && sub < 0x20) { mbs = sub & 15; return 2; }
        if (sub >= 0x20 && sub < 0x24) { rbs = sub & 3; return 2; }
        if (sub >= 0x30 && sub < 0x40) { setReg(REG_A, getReg(REG_A) & sub); return 2; }
        if (sub >= 0x40 && sub < 0x50) { setReg(REG_A, getReg(REG_A) | sub); return 2; }
        if (sub >= 0x50 && sub < 0x60) { setReg(REG_A, getReg(REG_A) ^ sub); return 2; }
        if (sub >= 0x60 && sub < 0x70) return getAhl() == (sub & 15) ? 2 + skipNext() : 2;
        if (sub >= 0x70 && sub < 0x78) { setReg(sub & 7, getReg(REG_A)); return 2; }
        if (sub >= 0x78) { setReg(REG_A, getReg(sub & 7)); return 2; }
        return 0;
    }

    private int execute9A(int sub) {
        int reg = sub & 7;
        if ((sub & 8) == 0) return getReg(reg) == ((sub >> 4) & 15) ? 2 + skipNext() : 2;
        setReg(reg, (sub >> 4) & 15);
        return 2;
    }

    private int executeBitFamily(int opcode, int family) {
        int kind = (opcode >> 6) & 3;
        if (kind == 0) return bitHmem(opcode, family);
        if (kind == 1) return bitPmem(opcode, family);
        return bitFmem(opcode, family);
    }

    private int bitHmem(int opcode, int family) {
        int bit = 1 << ((opcode >> 4) & 3);
        int value = getHmem(opcode);
        if (family == 0) setHmem(opcode, cy != 0 ? value | bit : value & ~bit);
        else if (family == 1) setHmem(opcode, value & ~bit);
        else if (family == 2) setHmem(opcode, value | bit);
        else if ((value & bit) != 0) { setHmem(opcode, value & ~bit); return 2 + skipNext(); }
        return 2;
    }

    private int bitPmem(int opcode, int family) {
        int bit = 1 << (getReg(REG_L) & 3);
        int value = getPmeml(opcode);
        if (family == 0) setPmeml(opcode, cy != 0 ? value | bit : value & ~bit);
        else if (family == 1) setPmeml(opcode, value & ~bit);
        else if (family == 2) setPmeml(opcode, value | bit);
        else if ((value & bit) != 0) { setPmeml(opcode, value & ~bit); return 2 + skipNext(); }
        return 2;
    }

    private int bitFmem(int opcode, int family) {
        int bit = 1 << ((opcode >> 4) & 3);
        int value = getFmem(opcode);
        if (family == 0) setFmem(opcode, cy != 0 ? value | bit : value & ~bit);
        else if (family == 1) setFmem(opcode, value & ~bit);
        else if (family == 2) setFmem(opcode, value | bit);
        else if ((value & bit) != 0) { setFmem(opcode, value & ~bit); return 2 + skipNext(); }
        return 2;
    }

    private int subSkipAhl(boolean carryIn) {
        int result = getReg(REG_A) - getAhl() - (carryIn ? cy : 0);
        setReg(REG_A, result);
        return result < 0 ? 1 + skipNext() : 1;
    }

    private int addCarryAhl() {
        int result = getReg(REG_A) + getAhl() + cy;
        setReg(REG_A, result);
        cy = result > 15 ? 1 : 0;
        int first = readRom(pc);
        if ((first >> 4) == 6) {
            if (cy != 0) return 1 + skipNext();
            pc = (pc + 1) & ROM_MASK;
            setReg(REG_A, getReg(REG_A) + (first & 15));
            return 2;
        }
        return 1;
    }

    private int executeAA(int sub) {
        if (sub == 0x10) { setAhlByte(getRp(RPE_XA)); return 2; }
        if (sub == 0x11) { int value = getRp(RPE_XA); setRp(RPE_XA, getAhlByte()); setAhlByte(value); return 2; }
        if (sub == 0x18) { setRp(RPE_XA, getAhlByte()); return 2; }
        if (sub == 0x19) return getRp(RPE_XA) == getAhlByte() ? 2 + skipNext() : 2;
        int group = sub >> 3;
        int rp = sub & 7;
        if (group == 8) { int value = getRp(RPE_XA); setRp(RPE_XA, getRp(rp)); setRp(rp, value); return 2; }
        if (group == 9) return getRp(RPE_XA) == getRp(rp) ? 2 + skipNext() : 2;
        if (group == 10) { setRp(rp, getRp(RPE_XA)); return 2; }
        if (group == 11) { setRp(RPE_XA, getRp(rp)); return 2; }
        if (group == 13) { int value = (getRp(rp) - 1) & 255; setRp(rp, value); return value == 255 ? 2 + skipNext() : 2; }
        if (group >= 18 && group <= 31) return rpArithmetic(group, rp);
        return 0;
    }

    private int rpArithmetic(int group, int rp) {
        int left = getRp(rp);
        int xa = getRp(RPE_XA);
        int value;
        switch (group) {
            case 18: setRp(rp, left & xa); break;
            case 19: setRp(RPE_XA, left & xa); break;
            case 20: setRp(rp, left | xa); break;
            case 21: setRp(RPE_XA, left | xa); break;
            case 22: setRp(rp, left ^ xa); break;
            case 23: setRp(RPE_XA, left ^ xa); break;
            case 24: value = left + xa; setRp(rp, value); return value > 255 ? 2 + skipNext() : 2;
            case 25: value = left + xa; setRp(RPE_XA, value); return value > 255 ? 2 + skipNext() : 2;
            case 26: value = left + xa + cy; setRp(rp, value); cy = value > 255 ? 1 : 0; break;
            case 27: value = left + xa + cy; setRp(RPE_XA, value); cy = value > 255 ? 1 : 0; break;
            case 28: value = left - xa; setRp(rp, value); return value < 0 ? 2 + skipNext() : 2;
            case 29: value = xa - left; setRp(RPE_XA, value); return value < 0 ? 2 + skipNext() : 2;
            case 30: value = left - xa - cy; setRp(rp, value); cy = value < 0 ? 1 : 0; break;
            default: value = xa - left - cy; setRp(RPE_XA, value); cy = value < 0 ? 1 : 0; break;
        }
        return 2;
    }

    private int branchAbsolute(int opcode) { pc = opcode & ROM_MASK; return 1; }
    private int callAbsolute(int opcode) { callFar(opcode); return 3; }

    private int executeLogicBit(int opcode, int operation) {
        int kind = (opcode >> 6) & 3;
        int bit = kind == 1 ? getReg(REG_L) & 3 : (opcode >> 4) & 3;
        int source = kind == 0 ? getHmem(opcode) : kind == 1 ? getPmeml(opcode) : getFmem(opcode);
        int value = (source >> bit) & 1;
        if (operation == 0) cy &= value;
        else if (operation == 1) cy |= value;
        else cy ^= value;
        return 2;
    }

    private int exchangeXaMem(int opcode) {
        int address = opcode & 254;
        int oldA = getReg(REG_A), oldX = getReg(REG_X);
        setReg(REG_A, getMem(address)); setReg(REG_X, getMem(address + 1));
        setMem(address, oldA); setMem(address + 1, oldX);
        return 2;
    }

    private int subCarryAhl() {
        int result = getReg(REG_A) - getAhl() - cy;
        setReg(REG_A, result);
        cy = result < 0 ? 1 : 0;
        int first = readRom(pc);
        if ((first >> 4) == 6) {
            if (cy != 0) {
                pc = (pc + 1) & ROM_MASK;
                setReg(REG_A, getReg(REG_A) + (first & 15));
                return 2;
            }
            return 1 + skipNext();
        }
        return 1;
    }

    private int addXaImmediate(int opcode) {
        int value = getRp(RPE_XA) + (opcode & 255);
        setRp(RPE_XA, value);
        return value > 255 ? 2 + skipNext() : 2;
    }

    private int executeMoveCarryBit(int opcode) {
        int kind = (opcode >> 6) & 3;
        int bit = kind == 1 ? getReg(REG_L) & 3 : (opcode >> 4) & 3;
        int source = kind == 0 ? getHmem(opcode) : kind == 1 ? getPmeml(opcode) : getFmem(opcode);
        cy = (source >> bit) & 1;
        return 2;
    }

    private int executeSkipBit(int opcode, boolean set) {
        int kind = (opcode >> 6) & 3;
        int bit = kind == 1 ? getReg(REG_L) & 3 : (opcode >> 4) & 3;
        int source = kind == 0 ? getHmem(opcode) : kind == 1 ? getPmeml(opcode) : getFmem(opcode);
        boolean condition = ((source >> bit) & 1) != 0;
        return condition == set ? 2 + skipNext() : 2;
    }

    private int incReg(int reg) {
        int value = (getReg(reg) + 1) & 15;
        setReg(reg, value);
        return value == 0 ? 1 + skipNext() : 1;
    }

    private int decReg(int reg) {
        int value = (getReg(reg) - 1) & 15;
        setReg(reg, value);
        return value == 15 ? 1 + skipNext() : 1;
    }

    private int moveTable(int address) { setRp(RPE_XA, readRom(address)); return 3; }

    private int exchangeAReg(int reg) {
        int old = getReg(REG_A);
        setReg(REG_A, getReg(reg));
        setReg(reg, old);
        return 1;
    }

    private int returnsAndSkip() {
        restoreReturn();
        return 3 + skipNext();
    }

    private int moveAhlAndChangeL(int direction, boolean exchange) {
        setReg(REG_A, getAhl());
        int value = (getReg(REG_L) + direction) & 15;
        setReg(REG_L, value);
        if ((direction > 0 && value == 0) || (direction < 0 && value == 15)) return 2 + skipNext();
        return 1;
    }

    private int exchangeAhl(int direction) {
        int old = getReg(REG_A);
        setReg(REG_A, getAhl());
        setAhl(old);
        if (direction != 0) {
            int value = (getReg(REG_L) + direction) & 15;
            setReg(REG_L, value);
            if ((direction > 0 && value == 0) || (direction < 0 && value == 15)) return 2 + skipNext();
        }
        return 1;
    }

    private int exchangeDirect(int address) {
        int old = getReg(REG_A);
        setReg(REG_A, ram[address] & 15);
        ram[address] = (byte) old;
        return 1;
    }

    private void restoreReturn() {
        pc = stackPop() << 8;
        int value = stackPop();
        pc |= (value << 12) & 0x1000;
        mbe = value >> 3;
        rbe = (value >> 2) & 1;
        pc |= stackPop();
        pc |= stackPop() << 4;
        pc &= ROM_MASK;
    }

    private int returnNormal(boolean interruptReturn) {
        restoreReturn();
        if (interruptReturn) {
            int value = stackPop();
            ist = value >> 2;
            mbe = (value >> 1) & 1;
            rbe = value & 1;
            value = stackPop();
            cy = (value >> 3) & 1;
            sk = value & 7;
        }
        return 3;
    }

    private int readIo(int address) {
        switch (address) {
            case 0xF80: return sp & 15;
            case 0xF81: return sp >> 4;
            case 0xF82: return rbs;
            case 0xF83: return mbs;
            case 0xF84: return sbs;
            case 0xF86: return bt & 15;
            case 0xF87: return bt >> 4;
            case 0xF88: return tmod2h & 15;
            case 0xF89: return tmod2h >> 4;
            case 0xF98: return wml;
            case 0xF99: return wmh;
            case 0xFA0: return tm0l;
            case 0xFA1: return tm0h;
            case 0xFA4: return t0 & 15;
            case 0xFA5: return t0 >> 4;
            case 0xFA6: return tmod0 & 15;
            case 0xFA7: return tmod0 >> 4;
            case 0xFB0: return (ist << 2) | (mbe << 1) | rbe;
            case 0xFB1: return (cy << 3) | sk;
            case 0xFB2: return (ime << 3) | ips;
            case 0xFB3: return (pccMode << 2) | pccClock;
            case 0xFB4: return im0;
            case 0xFB5: return im1;
            case 0xFB6: return im2;
            case 0xFB7: return scc;
            case 0xFB8: return inta;
            case 0xFBA: return intc;
            case 0xFBC: return inte;
            case 0xFBD: return intf;
            case 0xFBE: return intg;
            case 0xFBF: return inth;
            case 0xFDC: return poga & 15;
            case 0xFDD: return (poga >> 4) & 15;
            case 0xFE8: return pm3;
            case 0xFE9: return pm6;
            case 0xFF0: return readPort(0);
            case 0xFF1: return readPort(1);
            case 0xFF2: return readPort(2);
            case 0xFF3: return readPort(3);
            case 0xFF5: return readPort(5);
            case 0xFF6: return readPort(6);
            case 0xFF8: return readPort(8);
            case 0xFF9: return readPort(9);
            default: return 0;
        }
    }

    private void writeIo(int address, int value) {
        value &= 15;
        switch (address) {
            case 0xF80: sp = (sp & 0xF0) | value; break;
            case 0xF81: sp = (sp & 15) | (value << 4); break;
            case 0xF84: sbs = value; break;
            case 0xF85:
                btm = value & 7;
                if ((value & 8) != 0) { inta &= ~INTA_IRQBT; bt = 0; }
                break;
            case 0xF88: tmod2h = (tmod2h & 0xF0) | value; break;
            case 0xF89: tmod2h = (tmod2h & 15) | (value << 4); break;
            case 0xF8B: wdtm = (value >> 3) & 1; break;
            case 0xF98: wml = value; break;
            case 0xF99: wmh = value & 0xB; break;
            case 0xFA0:
                tm0l = value & 0xC;
                if ((value & 8) != 0) { inte &= ~INTE_IRQT0; t0 = 0; }
                break;
            case 0xFA1: tm0h = value & 7; break;
            case 0xFA6: tmod0 = (tmod0 & 0xF0) | value; break;
            case 0xFA7: tmod0 = (tmod0 & 15) | (value << 4); break;
            case 0xFB0: ist = (value >> 2) & 3; mbe = (value >> 1) & 1; rbe = value & 1; break;
            case 0xFB1: cy = (value >> 3) & 1; sk = value & 7; break;
            case 0xFB2: ime = (value >> 3) & 1; ips = value & 7; break;
            case 0xFB3: pccMode = (value >> 2) & 3; pccClock = value & 3; break;
            case 0xFB4: im0 = value; break;
            case 0xFB5: im1 = value & 1; break;
            case 0xFB6: im2 = value & 3; break;
            case 0xFB7: scc = value & 9; break;
            case 0xFB8: inta = value; break;
            case 0xFBA: intc = value; break;
            case 0xFBC: inte = value; break;
            case 0xFBD: intf = value; break;
            case 0xFBE: intg = value; break;
            case 0xFBF: inth = value; break;
            case 0xFDC: poga = (poga & 0xF0) | value; break;
            case 0xFDD: poga = (poga & 15) | (value << 4); break;
            case 0xFE8: pm3 = value; break;
            case 0xFE9: pm6 = value; break;
            case 0xFF2: portLatch[2] = value; break;
            case 0xFF3: portLatch[3] = value; break;
            case 0xFF5: portLatch[5] = value; break;
            case 0xFF6: portLatch[6] = value; break;
            case 0xFF8: portLatch[8] = value; break;
            case 0xFF9: portLatch[9] = value; break;
            default: break;
        }
    }

    private int readPort(int port) {
        int pullup = 0;
        if (port == 0) pullup = (poga & 1) != 0 ? 15 : 0;
        else if (port == 1) pullup = (poga & 2) != 0 ? 15 : 0;
        else if (port == 2) pullup = (poga & 4) != 0 ? 15 : 0;
        else if (port == 3) pullup = (poga & 8) != 0 ? 15 : 0;
        else if (port == 6) pullup = (poga & 0x32) != 0 ? 15 : 0;
        else if (port == 8) pullup = (pogb & 1) != 0 ? 15 : 0;
        else if (port == 9) pullup = (pogb & 2) != 0 ? 15 : 0;
        int external = (~portInput[port][0]) & (portInput[port][1] | pullup) & 15;
        if (port == 3) return (external & ~pm3) | (portLatch[3] & pm3);
        if (port == 6) return (external & ~pm6) | (portLatch[6] & pm6);
        if (port == 5) return (~portInput[5][0]) & portInput[5][1] & 15;
        return external;
    }

    public byte[] vram() {
        System.arraycopy(ram, VRAM_OFFSET, displayRam, 0, VRAM_SIZE);
        return displayRam;
    }

    public boolean displayEnabled() { return true; }
    public int programCounter() { return pc & ROM_MASK; }
}
