package brickgame;

final class E0C6200Cpu implements BrickCpu {
    private static final int OSC1_CLOCK = 32768;
    private static final int RAM_SIZE = 0x300;
    private static final int VRAM_SIZE = 0x0A0;
    private static final int VRAM_PART1_OFFSET = 0xE00;
    private static final int VRAM_PART2_OFFSET = 0xE80;
    private static final int VRAM_PART_SIZE = 0x050;
    private static final int IORAM_OFFSET = 0xF00;
    private static final int IORAM_SIZE = 0x07F;

    private static final int IO_ALOFF = 8;
    private static final int IO_ALON = 4;
    private static final int IO_CLKCHG = 8;
    private static final int IO_TMRST = 2;
    private static final int IO_SWRST = 2;
    private static final int IO_SWRUN = 1;
    private static final int IO_PTRST = 2;
    private static final int IO_PTRUN = 1;
    private static final int IO_PTCOUT = 8;
    private static final int IO_PTC = 7;

    private static final int PORT_K0 = 0;
    private static final int PORT_K1 = 1;
    private static final int PORT_R0 = 2;
    private static final int PORT_R1 = 3;
    private static final int PORT_R2 = 4;
    private static final int PORT_R3 = 5;
    private static final int PORT_R4 = 6;
    private static final int PORT_P0 = 7;
    private static final int PORT_P1 = 8;
    private static final int PORT_P2 = 9;
    private static final int PORT_P3 = 10;
    private static final int PORT_COUNT = 11;

    private static final int[] BUTTON_PORTS = {
        PORT_K0, PORT_K0, PORT_K0, PORT_K0,
        PORT_K1, PORT_K1, PORT_K1, -1
    };
    private static final int[] BUTTON_MASKS = {1, 4, 2, 8, 2, 1, 4, 0};
    private static final int[] PTIMER_DIV = {0, 0, 128, 64, 32, 16, 8, 4};

    private final MachineProfile profile;
    private final byte[] rom;
    private final byte[] ram = new byte[RAM_SIZE];
    private final byte[] lcdRam = new byte[VRAM_SIZE];
    private final byte[] displayRam = new byte[VRAM_SIZE + 9];
    private final boolean[] buttons = new boolean[MachineProfile.BUTTON_COUNT];
    private final int[] portDirection = new int[PORT_COUNT];
    private final int[] portPushPull = new int[PORT_COUNT];
    private final int[] portLatch = new int[PORT_COUNT];
    private final int[] portPullup = new int[PORT_COUNT];
    private final int[] externalPullup = new int[PORT_COUNT];
    private final int[] inputLow = new int[PORT_COUNT];
    private final int[] inputHigh = new int[PORT_COUNT];

    private int a;
    private int b;
    private int ix;
    private int iy;
    private int sp;
    private int pc;
    private int nextPc;
    private int carry;
    private int zero;
    private int decimal;
    private int interruptFlag;
    private boolean halted;
    private boolean ifDelay;
    private boolean resetLine;

    private int it;
    private int isw;
    private int ipt;
    private int isio;
    private int ik0;
    private int ik1;
    private int eit;
    private int eisw;
    private int eipt;
    private int eisio;
    private int eik0;
    private int eik1;
    private int timer;
    private int stopwatchLow;
    private int stopwatchHigh;
    private int programmableTimer;
    private int reloadData;
    private int serialData;
    private int dfk0;
    private int ctrlOsc;
    private int ctrlLcd;
    private int lcdContrast;
    private int ctrlSvd;
    private int ctrlStopwatch;
    private int ctrlProgrammableTimer;
    private int programmableTimerControl;
    private int serialControl;
    private int hzr;
    private int ioControl;
    private int pullupControl;

    private long oscCounterUnits;
    private int timerCounter;
    private int programmableTimerCounter;
    private int stopwatchCounter100;
    private int instructionCounter;

    E0C6200Cpu(MachineProfile profile, byte[] rom) {
        if (rom == null || rom.length == 0) {
            throw new IllegalArgumentException("Empty E0C6200 ROM");
        }
        this.profile = profile;
        this.rom = rom;
        reset();
    }

    private void reset() {
        a = 0;
        b = 0;
        ix = 0;
        iy = 0;
        sp = 0;
        pc = 0x100;
        nextPc = 0x100;
        carry = 0;
        zero = 0;
        decimal = 0;
        interruptFlag = 0;
        halted = false;
        ifDelay = false;
        clear(ram);
        clear(lcdRam);
        clear(displayRam);
        for (int i = 0; i < PORT_COUNT; i++) {
            portDirection[i] = 0;
            portPushPull[i] = 0;
            portLatch[i] = 0;
            portPullup[i] = 0;
            externalPullup[i] = 0;
            inputLow[i] = 0;
            inputHigh[i] = 0;
        }
        portDirection[PORT_R0] = 15;
        portDirection[PORT_R1] = 15;
        portDirection[PORT_R2] = 15;
        portDirection[PORT_R3] = 15;
        portDirection[PORT_R4] = 15;
        portPushPull[PORT_P0] = 15;
        portPushPull[PORT_P1] = 15;
        portPushPull[PORT_P2] = 15;
        portPushPull[PORT_P3] = 15;
        portPullup[PORT_P0] = 15;
        portPullup[PORT_P1] = 15;
        portPullup[PORT_P2] = 15;
        portPullup[PORT_P3] = 15;
        externalPullup[PORT_K0] = 15;
        externalPullup[PORT_K1] = 15;

        it = 0;
        isw = 0;
        ipt = 0;
        isio = 0;
        ik0 = 0;
        ik1 = 0;
        eit = 0;
        eisw = 0;
        eipt = 0;
        eisio = 0;
        eik0 = 0;
        eik1 = 0;
        timer = 0;
        stopwatchLow = 0;
        stopwatchHigh = 0;
        programmableTimer = 0;
        reloadData = 0;
        serialData = 0;
        dfk0 = 15;
        ctrlOsc = 0;
        ctrlLcd = IO_ALOFF;
        lcdContrast = 0;
        ctrlSvd = 8;
        ctrlStopwatch = 0;
        ctrlProgrammableTimer = 0;
        programmableTimerControl = 0;
        serialControl = 0;
        hzr = 0;
        ioControl = 0;
        pullupControl = 0;
        oscCounterUnits = 0;
        timerCounter = 0;
        programmableTimerCounter = 0;
        stopwatchCounter100 = 0;
        instructionCounter = 0;
        resetLine = false;
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = false;
        }
    }

    private static void clear(byte[] values) {
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
                resetLine = true;
                buttons[button] = true;
            } else {
                resetLine = false;
            }
            return;
        }
        int port = BUTTON_PORTS[button];
        int mask = BUTTON_MASKS[button];
        int previous = readPort(port) & mask;
        inputLow[port] &= ~mask;
        inputHigh[port] &= ~mask;
        if (down) {
            inputLow[port] |= mask;
        }
        int current = readPort(port) & mask;
        if (port == PORT_K0 && (previous & eik0) != (current & eik0)) {
            if ((dfk0 & mask) != current) {
                ik0 |= 1;
                if ((mask & 8) != 0) {
                    processProgrammableTimer();
                }
            }
        } else if (port == PORT_K1 && (previous & eik1) != (current & eik1)) {
            if (current != mask) {
                ik1 |= 1;
            }
        }
    }

    public synchronized int runCycles(int budget) {
        long target = (long) budget * OSC1_CLOCK;
        long used = 0;
        while (used < target) {
            used += clockUnits();
        }
        return (int) ((used + OSC1_CLOCK - 1) / OSC1_CLOCK);
    }

    private long clockUnits() {
        int logicalCycles = 7;
        if (!resetLine) {
            if (!halted) {
                ifDelay = false;
                int opcode = readRomWord(pc * 2);
                logicalCycles = execute(opcode);
                instructionCounter++;
            }
            if (interruptFlag != 0 && !ifDelay) {
                if ((ipt & eipt) != 0) {
                    logicalCycles += interrupt(0xC);
                } else if ((isio & eisio) != 0) {
                    logicalCycles += interrupt(0xA);
                } else if (ik1 != 0) {
                    logicalCycles += interrupt(0x8);
                } else if (ik0 != 0) {
                    logicalCycles += interrupt(0x6);
                } else if ((isw & eisw) != 0) {
                    logicalCycles += interrupt(0x4);
                } else if ((it & eit) != 0) {
                    logicalCycles += interrupt(0x2);
                }
            }
            long units = (ctrlOsc & IO_CLKCHG) == 0
                    ? (long) logicalCycles * profile.clockHz
                    : (long) logicalCycles * OSC1_CLOCK;
            oscCounterUnits -= units;
            while (oscCounterUnits <= 0) {
                oscCounterUnits += profile.clockHz;
                clockOsc1();
            }
            return units;
        }
        return (long) logicalCycles * OSC1_CLOCK;
    }

    private int interrupt(int vector) {
        setMemory((sp - 1) & 255, (pc >> 8) & 15);
        setMemory((sp - 2) & 255, (pc >> 4) & 15);
        sp = (sp - 3) & 255;
        setMemory(sp, pc & 15);
        interruptFlag = 0;
        halted = false;
        pc = nextPc = (nextPc & 0x1000) | 0x100 | vector;
        return 13;
    }

    private void clockOsc1() {
        int dividerSelect = programmableTimerControl & IO_PTC;
        if (dividerSelect > 1) {
            programmableTimerCounter--;
            if (programmableTimerCounter <= 0) {
                programmableTimerCounter += PTIMER_DIV[dividerSelect];
                processProgrammableTimer();
            }
        }
        stopwatchCounter100 -= 100;
        if (stopwatchCounter100 <= 0) {
            stopwatchCounter100 += OSC1_CLOCK;
            processStopwatch();
        }
        timerCounter--;
        if (timerCounter <= 0) {
            timerCounter += 128;
            processTimer();
        }
    }

    private void processProgrammableTimer() {
        programmableTimer = (programmableTimer - 1) & 255;
        if (programmableTimer == 0) {
            programmableTimer = reloadData;
            ipt |= 1;
        }
        if ((programmableTimerControl & IO_PTCOUT) != 0) {
            portLatch[PORT_R3] ^= 8;
        }
    }

    private void processStopwatch() {
        if ((ctrlStopwatch & IO_SWRUN) != 0) {
            stopwatchLow = (stopwatchLow + 1) % 10;
            if (stopwatchLow == 0) {
                stopwatchHigh = (stopwatchHigh + 1) % 10;
                isw |= 1;
                if (stopwatchHigh == 0) {
                    isw |= 2;
                }
            }
        }
    }

    private void processTimer() {
        int newTimer = (timer + 1) & 255;
        if ((newTimer & 4) < (timer & 4)) {
            it |= 1;
        }
        if (((newTimer >> 4) & 1) < ((timer >> 4) & 1)) {
            it |= 2;
        }
        if (((newTimer >> 4) & 4) < ((timer >> 4) & 4)) {
            it |= 4;
        }
        if (((newTimer >> 4) & 8) < ((timer >> 4) & 8)) {
            it |= 8;
        }
        timer = newTimer;
    }

    private int readRomByte(int address) {
        int index = address % rom.length;
        if (index < 0) {
            index += rom.length;
        }
        return rom[index] & 255;
    }

    private int readRomWord(int address) {
        return (readRomByte(address) << 8) | readRomByte(address + 1);
    }

    private int getMemory(int address) {
        if (address < RAM_SIZE) {
            return ram[address] & 15;
        }
        if (address >= VRAM_PART1_OFFSET && address < VRAM_PART1_OFFSET + VRAM_PART_SIZE) {
            return lcdRam[address - VRAM_PART1_OFFSET] & 15;
        }
        if (address >= VRAM_PART2_OFFSET && address < VRAM_PART2_OFFSET + VRAM_PART_SIZE) {
            return lcdRam[address - VRAM_PART2_OFFSET + VRAM_PART_SIZE] & 15;
        }
        if (address >= IORAM_OFFSET && address < IORAM_OFFSET + IORAM_SIZE) {
            return readIo(address);
        }
        return 0;
    }

    private void setMemory(int address, int value) {
        value &= 15;
        if (address < RAM_SIZE) {
            ram[address] = (byte) value;
        } else if (address >= VRAM_PART1_OFFSET && address < VRAM_PART1_OFFSET + VRAM_PART_SIZE) {
            lcdRam[address - VRAM_PART1_OFFSET] = (byte) value;
        } else if (address >= VRAM_PART2_OFFSET && address < VRAM_PART2_OFFSET + VRAM_PART_SIZE) {
            lcdRam[address - VRAM_PART2_OFFSET + VRAM_PART_SIZE] = (byte) value;
        } else if (address >= IORAM_OFFSET && address < IORAM_OFFSET + IORAM_SIZE) {
            writeIo(address, value);
        }
    }

    private int getRegister(int register) {
        switch (register & 3) {
            case 0: return a;
            case 1: return b;
            case 2: return getMemory(ix);
            default: return getMemory(iy);
        }
    }

    private void setRegister(int register, int value) {
        value &= 15;
        switch (register & 3) {
            case 0: a = value; break;
            case 1: b = value; break;
            case 2: setMemory(ix, value); break;
            default: setMemory(iy, value); break;
        }
    }

    private int readPort(int port) {
        int pull = externalPullup[port] | portPullup[port];
        int inputPart = (~portDirection[port])
                & (inputHigh[port] | (pull & ~inputLow[port]));
        int outputPart = portDirection[port] & portLatch[port]
                & (pull | portPushPull[port]);
        return (inputPart | outputPart) & 15;
    }

    private int readIo(int address) {
        switch (address) {
            case 0xF00: int value = it; it = 0; return value;
            case 0xF01: value = isw; isw = 0; return value;
            case 0xF02: value = ipt; ipt = 0; return value;
            case 0xF03: value = isio; isio = 0; return value;
            case 0xF04: value = ik0; ik0 = 0; return value;
            case 0xF05: value = ik1; ik1 = 0; return value;
            case 0xF10: return eit;
            case 0xF11: return eisw;
            case 0xF12: return eipt;
            case 0xF13: return eisio;
            case 0xF14: return eik0;
            case 0xF15: return eik1;
            case 0xF20: return timer & 15;
            case 0xF21: return (timer >> 4) & 15;
            case 0xF22: return stopwatchLow & 15;
            case 0xF23: return stopwatchHigh & 15;
            case 0xF24: return programmableTimer & 15;
            case 0xF25: return (programmableTimer >> 4) & 15;
            case 0xF26: return reloadData & 15;
            case 0xF27: return (reloadData >> 4) & 15;
            case 0xF30: return serialData & 15;
            case 0xF31: return (serialData >> 4) & 15;
            case 0xF40: return readPort(PORT_K0);
            case 0xF41: return dfk0;
            case 0xF42: return readPort(PORT_K1);
            case 0xF50: return portLatch[PORT_R0];
            case 0xF51: return portLatch[PORT_R1];
            case 0xF52: return portLatch[PORT_R2];
            case 0xF53: return portLatch[PORT_R3];
            case 0xF54: return portLatch[PORT_R4];
            case 0xF60: return readPort(PORT_P0);
            case 0xF61: return readPort(PORT_P1);
            case 0xF62: return readPort(PORT_P2);
            case 0xF63: return readPort(PORT_P3);
            case 0xF70: return ctrlOsc;
            case 0xF71: return ctrlLcd;
            case 0xF72: return lcdContrast;
            case 0xF73: return 0;
            case 0xF74:
            case 0xF75:
                return 0;
            case 0xF77: return ctrlStopwatch & 1;
            case 0xF78: return ctrlProgrammableTimer & 1;
            case 0xF79: return programmableTimerControl;
            case 0xF7D: return ioControl;
            case 0xF7E: return pullupControl;
            default: return 0;
        }
    }

    private void writeIo(int address, int value) {
        switch (address) {
            case 0xF10: eit = value; break;
            case 0xF11: eisw = value & 3; break;
            case 0xF12: eipt = value & 1; break;
            case 0xF13: eisio = value & 1; break;
            case 0xF14: eik0 = value; break;
            case 0xF15: eik1 = value; break;
            case 0xF26: reloadData = (reloadData & 0xF0) | value; break;
            case 0xF27: reloadData = (reloadData & 15) | (value << 4); break;
            case 0xF30: serialData = (serialData & 0xF0) | value; break;
            case 0xF31: serialData = (serialData & 15) | (value << 4); break;
            case 0xF41: dfk0 = value; break;
            case 0xF50: setOutputPort(PORT_R0, value); break;
            case 0xF51: setOutputPort(PORT_R1, value); break;
            case 0xF52: setOutputPort(PORT_R2, value); break;
            case 0xF53: setOutputPort(PORT_R3, value); break;
            case 0xF54:
                setOutputPort(PORT_R4, value);
                break;
            case 0xF60: setOutputPort(PORT_P0, value); break;
            case 0xF61: setOutputPort(PORT_P1, value); break;
            case 0xF62: setOutputPort(PORT_P2, value); break;
            case 0xF63: setOutputPort(PORT_P3, value); break;
            case 0xF70: ctrlOsc = value; break;
            case 0xF71: ctrlLcd = value; break;
            case 0xF72: lcdContrast = value; break;
            case 0xF74:
            case 0xF75:
                break;
            case 0xF76: if ((value & IO_TMRST) != 0) timer = 0; break;
            case 0xF77:
                if ((value & IO_SWRST) != 0) { stopwatchLow = 0; stopwatchHigh = 0; }
                ctrlStopwatch = value & IO_SWRUN;
                break;
            case 0xF78:
                if ((value & IO_PTRST) != 0) programmableTimer = reloadData;
                ctrlProgrammableTimer = value & IO_PTRUN;
                break;
            case 0xF79: programmableTimerControl = value; break;
            case 0xF7D: setIoControl(value); break;
            case 0xF7E: setPullupControl(value); break;
            default: break;
        }
    }

    private void setOutputPort(int port, int value) {
        portLatch[port] = value & 15;
    }

    private void setIoControl(int value) {
        ioControl = value;
        portDirection[PORT_P0] = (value & 1) != 0 ? 15 : 0;
        portDirection[PORT_P1] = (value & 2) != 0 ? 15 : 0;
        portDirection[PORT_P2] = (value & 4) != 0 ? 15 : 0;
        portDirection[PORT_P3] = (value & 8) != 0 ? 15 : 0;
    }

    private void setPullupControl(int value) {
        pullupControl = value;
        portPullup[PORT_P0] = (value & 1) == 0 ? 15 : 0;
        portPullup[PORT_P1] = (value & 2) == 0 ? 15 : 0;
        portPullup[PORT_P2] = (value & 4) == 0 ? 15 : 0;
        portPullup[PORT_P3] = (value & 8) == 0 ? 15 : 0;
    }

    private void nextInstruction() {
        pc = nextPc = (pc & 0x1000) | ((pc + 1) & 0xFFF);
    }

    private int execute(int opcode) {
        int group = opcode >>> 8;
        if (group <= 7) return executeBranchGroup(group, opcode);
        if (group == 8) {
            iy = (iy & 0xF00) | (opcode & 255);
            nextInstruction();
            return 5;
        }
        if (group == 9) {
            setMemory(ix, opcode & 15);
            setMemory((ix & 0xF00) | ((ix + 1) & 255), (opcode >> 4) & 15);
            ix = (ix & 0xF00) | ((ix + 2) & 255);
            nextInstruction();
            return 5;
        }
        if (group == 0xA) return executeGroupA(opcode);
        if (group == 0xB) {
            ix = (ix & 0xF00) | (opcode & 255);
            nextInstruction();
            return 5;
        }
        if (group >= 0xC && group <= 0xE) return executeImmediateGroup(opcode);
        return executeGroupF(opcode);
    }

    private int executeBranchGroup(int group, int opcode) {
        int target = opcode & 255;
        switch (group) {
            case 0:
                pc = (nextPc & 0x1F00) | target;
                return 5;
            case 1:
                pc = nextPc = (pc & 0x1000) | (getMemory(sp + 2) << 8)
                        | (getMemory(sp + 1) << 4) | getMemory(sp);
                sp = (sp + 3) & 255;
                setMemory(ix, opcode & 15);
                setMemory((ix & 0xF00) | ((ix + 1) & 255), (opcode >> 4) & 15);
                ix = (ix & 0xF00) | ((ix + 2) & 255);
                return 12;
            case 2:
                if (carry != 0) pc = (nextPc & 0x1F00) | target; else nextInstruction();
                return 5;
            case 3:
                if (carry == 0) pc = (nextPc & 0x1F00) | target; else nextInstruction();
                return 5;
            case 4:
                pushReturn();
                pc = (nextPc & 0x1F00) | target;
                return 7;
            case 5:
                pushReturn();
                pc = nextPc = (nextPc & 0x1000) | target;
                return 7;
            case 6:
                if (zero != 0) pc = (nextPc & 0x1F00) | target; else nextInstruction();
                return 5;
            default:
                if (zero == 0) pc = (nextPc & 0x1F00) | target; else nextInstruction();
                return 5;
        }
    }

    private void pushReturn() {
        int returnPc = pc + 1;
        setMemory((sp - 1) & 255, (returnPc >> 8) & 15);
        setMemory((sp - 2) & 255, (returnPc >> 4) & 15);
        sp = (sp - 3) & 255;
        setMemory(sp, returnPc & 15);
    }

    private int executeGroupA(int opcode) {
        int low = opcode & 255;
        if (low < 0x80) {
            int sub = low >> 4;
            int immediate = low & 15;
            int value;
            switch (sub) {
                case 0: value = ((ix >> 4) & 15) + immediate + carry; zero = (value & 15) == 0 ? 1 : 0; carry = value > 15 ? 1 : 0; ix = (ix & 0xF0F) | ((value << 4) & 0xF0); break;
                case 1: value = (ix & 15) + immediate + carry; zero = (value & 15) == 0 ? 1 : 0; carry = value > 15 ? 1 : 0; ix = (ix & 0xFF0) | (value & 15); break;
                case 2: value = ((iy >> 4) & 15) + immediate + carry; zero = (value & 15) == 0 ? 1 : 0; carry = value > 15 ? 1 : 0; iy = (iy & 0xF0F) | ((value << 4) & 0xF0); break;
                case 3: value = (iy & 15) + immediate + carry; zero = (value & 15) == 0 ? 1 : 0; carry = value > 15 ? 1 : 0; iy = (iy & 0xFF0) | (value & 15); break;
                case 4: compareNibble((ix >> 4) & 15, immediate); break;
                case 5: compareNibble(ix & 15, immediate); break;
                case 6: compareNibble((iy >> 4) & 15, immediate); break;
                default: compareNibble(iy & 15, immediate); break;
            }
            nextInstruction();
            return 7;
        }
        int operation = (low >> 4) & 7;
        int r = (low >> 2) & 3;
        int q = low & 3;
        int left = getRegister(r);
        int right = getRegister(q);
        int result;
        switch (operation) {
            case 0: result = addNibbles(left, right, 0); setRegister(r, result); break;
            case 1: result = addNibbles(left, right, carry); setRegister(r, result); break;
            case 2: result = subtractNibbles(left, right, 0); setRegister(r, result); break;
            case 3: result = subtractNibbles(left, right, carry); setRegister(r, result); break;
            case 4: result = left & right; zero = result == 0 ? 1 : 0; setRegister(r, result); break;
            case 5: result = left | right; zero = result == 0 ? 1 : 0; setRegister(r, result); break;
            case 6: result = left ^ right; zero = result == 0 ? 1 : 0; setRegister(r, result); break;
            default:
                result = (getRegister(low & 3) << 1) + carry;
                carry = result > 15 ? 1 : 0;
                setRegister(low & 3, result);
                break;
        }
        nextInstruction();
        return 7;
    }

    private void compareNibble(int left, int right) {
        int value = left - right;
        zero = value == 0 ? 1 : 0;
        carry = value < 0 ? 1 : 0;
    }

    private int addNibbles(int left, int right, int carryIn) {
        int result = left + right + carryIn;
        carry = result > 15 ? 1 : 0;
        if (decimal != 0 && result > 9) {
            result += 6;
            carry = 1;
        }
        zero = (result & 15) == 0 ? 1 : 0;
        return result & 15;
    }

    private int subtractNibbles(int left, int right, int borrow) {
        int result = left - right - borrow;
        carry = result < 0 ? 1 : 0;
        if (decimal != 0 && result < 0) result += 10;
        zero = (result & 15) == 0 ? 1 : 0;
        return result & 15;
    }

    private int executeImmediateGroup(int opcode) {
        int high = opcode >>> 8;
        int low = opcode & 255;
        if (high == 0xE && low >= 0x40) return executeGroupEUpper(opcode);
        int operation;
        if (high == 0xC) operation = low < 0x40 ? 0 : low < 0x80 ? 1 : low < 0xC0 ? 2 : 3;
        else if (high == 0xD) operation = low < 0x40 ? 4 : low < 0x80 ? 5 : low < 0xC0 ? 6 : 7;
        else operation = 8;
        int r = (low >> 4) & 3;
        int immediate = low & 15;
        int current = getRegister(r);
        int result;
        switch (operation) {
            case 0: result = addNibbles(current, immediate, 0); setRegister(r, result); break;
            case 1: result = addNibbles(current, immediate, carry); setRegister(r, result); break;
            case 2: result = current & immediate; zero = result == 0 ? 1 : 0; setRegister(r, result); break;
            case 3: result = current | immediate; zero = result == 0 ? 1 : 0; setRegister(r, result); break;
            case 4: result = current ^ immediate; zero = result == 0 ? 1 : 0; setRegister(r, result); break;
            case 5: result = subtractNibbles(current, immediate, carry); setRegister(r, result); break;
            case 6: zero = (current & immediate) == 0 ? 1 : 0; break;
            case 7: compareNibble(current, immediate); break;
            default: setRegister(r, immediate); break;
        }
        nextInstruction();
        return operation == 8 ? 5 : 7;
    }

    private int executeGroupEUpper(int opcode) {
        int low = opcode & 255;
        if (low < 0x60) {
            ifDelay = true;
            nextPc = (opcode << 8) & 0x1F00;
            pc = (pc & 0x1000) | ((pc + 1) & 0xFFF);
            return 5;
        }
        if (low < 0x70) {
            setMemory(ix, opcode & 15);
            ix = (ix & 0xF00) | ((ix + 1) & 255);
            nextInstruction();
            return 5;
        }
        if (low < 0x80) {
            setMemory(iy, opcode & 15);
            iy = (iy & 0xF00) | ((iy + 1) & 255);
            nextInstruction();
            return 5;
        }
        int r = opcode & 3;
        if (low < 0x84) ix = (getRegister(r) << 8) | (ix & 255);
        else if (low < 0x88) ix = (getRegister(r) << 4) | (ix & 0xF0F);
        else if (low < 0x8C) ix = getRegister(r) | (ix & 0xFF0);
        else if (low < 0x90) {
            int value = getRegister(r) + (carry << 4);
            carry = value & 1;
            setRegister(r, value >> 1);
        } else if (low < 0x94) iy = (getRegister(r) << 8) | (iy & 255);
        else if (low < 0x98) iy = (getRegister(r) << 4) | (iy & 0xF0F);
        else if (low < 0x9C) iy = getRegister(r) | (iy & 0xFF0);
        else if (low < 0xA0) return 5;
        else if (low < 0xA4) setRegister(r, ix >> 8);
        else if (low < 0xA8) setRegister(r, (ix >> 4) & 15);
        else if (low < 0xAC) setRegister(r, ix & 15);
        else if (low < 0xB0) return 5;
        else if (low < 0xB4) setRegister(r, iy >> 8);
        else if (low < 0xB8) setRegister(r, (iy >> 4) & 15);
        else if (low < 0xBC) setRegister(r, iy & 15);
        else if (low < 0xC0) return 5;
        else if (low < 0xD0) setRegister((low >> 2) & 3, getRegister(low & 3));
        else if (low < 0xE0) return 5;
        else if (low < 0xF0) {
            setRegister((low >> 2) & 3, getRegister(low & 3));
            ix = (ix & 0xF00) | ((ix + 1) & 255);
        } else {
            setRegister((low >> 2) & 3, getRegister(low & 3));
            iy = (iy & 0xF00) | ((iy + 1) & 255);
        }
        nextInstruction();
        return 5;
    }

    private int executeGroupF(int opcode) {
        int low = opcode & 255;
        if (low < 0x20) {
            int r = (low >> 2) & 3;
            int q = low & 3;
            if (low < 0x10) compareNibble(getRegister(r), getRegister(q));
            else zero = (getRegister(r) & getRegister(q)) == 0 ? 1 : 0;
            nextInstruction();
            return 7;
        }
        if (low >= 0x28 && low < 0x30) return addMemoryAndAdvance(opcode, low < 0x2C, true);
        if (low >= 0x38 && low < 0x40) return addMemoryAndAdvance(opcode, low < 0x3C, false);
        if (low >= 0x40 && low < 0x50) {
            carry |= low & 1;
            zero |= (low >> 1) & 1;
            decimal |= (low >> 2) & 1;
            int newInterrupt = (low >> 3) & 1;
            ifDelay = newInterrupt != 0 && interruptFlag == 0;
            interruptFlag |= newInterrupt;
            nextInstruction();
            return 7;
        }
        if (low >= 0x50 && low < 0x60) {
            carry &= low;
            zero &= low >> 1;
            decimal &= low >> 2;
            interruptFlag &= low >> 3;
            nextInstruction();
            return 7;
        }
        if (low >= 0x60 && low < 0x70) {
            int address = low & 15;
            int result = getMemory(address) + 1;
            zero = result == 16 ? 1 : 0;
            carry = result > 15 ? 1 : 0;
            setMemory(address, result);
            nextInstruction();
            return 7;
        }
        if (low >= 0x70 && low < 0x80) {
            int address = low & 15;
            int result = getMemory(address) - 1;
            zero = result == 0 ? 1 : 0;
            carry = result < 0 ? 1 : 0;
            setMemory(address, result);
            nextInstruction();
            return 7;
        }
        if (low >= 0x80 && low < 0x90) { setMemory(low & 15, a); nextInstruction(); return 5; }
        if (low >= 0x90 && low < 0xA0) { setMemory(low & 15, b); nextInstruction(); return 5; }
        if (low >= 0xA0 && low < 0xB0) { a = getMemory(low & 15); nextInstruction(); return 5; }
        if (low >= 0xB0 && low < 0xC0) { b = getMemory(low & 15); nextInstruction(); return 5; }
        return executeStackGroup(low);
    }

    private int addMemoryAndAdvance(int opcode, boolean xPointer, boolean add) {
        int r = opcode & 3;
        int pointer = xPointer ? ix : iy;
        int value;
        if (add) value = addNibbles(getMemory(pointer), getRegister(r), carry);
        else value = subtractNibbles(getMemory(pointer), getRegister(r), carry);
        setMemory(pointer, value);
        if (xPointer) ix = (ix & 0xF00) | ((ix + 1) & 255);
        else iy = (iy & 0xF00) | ((iy + 1) & 255);
        nextInstruction();
        return 7;
    }

    private int executeStackGroup(int low) {
        if (low >= 0xC0 && low < 0xC4) {
            sp = (sp - 1) & 255; setMemory(sp, getRegister(low & 3)); nextInstruction(); return 5;
        }
        if (low >= 0xD0 && low < 0xD4) {
            setRegister(low & 3, getMemory(sp)); sp = (sp + 1) & 255; nextInstruction(); return 5;
        }
        switch (low) {
            case 0xC4: return pushValue(ix >> 8);
            case 0xC5: return pushValue((ix >> 4) & 15);
            case 0xC6: return pushValue(ix & 15);
            case 0xC7: return pushValue(iy >> 8);
            case 0xC8: return pushValue((iy >> 4) & 15);
            case 0xC9: return pushValue(iy & 15);
            case 0xCA: return pushValue((interruptFlag << 3) | (decimal << 2) | (zero << 1) | carry);
            case 0xCB: sp = (sp - 1) & 255; nextInstruction(); return 5;
            case 0xD4: ix = (getMemory(sp) << 8) | (ix & 255); return finishPop();
            case 0xD5: ix = (getMemory(sp) << 4) | (ix & 0xF0F); return finishPop();
            case 0xD6: ix = getMemory(sp) | (ix & 0xFF0); return finishPop();
            case 0xD7: iy = (getMemory(sp) << 8) | (iy & 255); return finishPop();
            case 0xD8: iy = (getMemory(sp) << 4) | (iy & 0xF0F); return finishPop();
            case 0xD9: iy = getMemory(sp) | (iy & 0xFF0); return finishPop();
            case 0xDA:
                int flags = getMemory(sp);
                carry = flags & 1; zero = (flags >> 1) & 1; decimal = (flags >> 2) & 1;
                int newInterrupt = (flags >> 3) & 1;
                ifDelay = newInterrupt != 0 && interruptFlag == 0;
                interruptFlag = newInterrupt;
                return finishPop();
            case 0xDB: sp = (sp + 1) & 255; nextInstruction(); return 5;
            case 0xDE:
                pc = (pc & 0x1000) | getMemory(sp) | (getMemory(sp + 1) << 4) | (getMemory(sp + 2) << 8);
                sp = (sp + 3) & 255;
                nextInstruction();
                return 12;
            case 0xDF:
                pc = nextPc = (pc & 0x1000) | getMemory(sp) | (getMemory(sp + 1) << 4) | (getMemory(sp + 2) << 8);
                sp = (sp + 3) & 255;
                return 7;
            default:
                if (low >= 0xE0 && low < 0xE4) { sp = (getRegister(low & 3) << 4) | (sp & 15); nextInstruction(); return 5; }
                if (low >= 0xE4 && low < 0xE8) { setRegister(low & 3, sp >> 4); nextInstruction(); return 5; }
                if (low == 0xE8) { pc = (nextPc & 0x1F00) | (b << 4) | a; return 5; }
                if (low >= 0xF0 && low < 0xF4) { sp = getRegister(low & 3) | (sp & 0xF0); nextInstruction(); return 5; }
                if (low >= 0xF4 && low < 0xF8) { setRegister(low & 3, sp & 15); nextInstruction(); return 5; }
                if (low == 0xF8) { halted = true; nextInstruction(); return 5; }
                if (low == 0xFB) { nextInstruction(); return 5; }
                if (low == 0xFF) { nextInstruction(); return 7; }
                return 5;
        }
    }

    private int pushValue(int value) {
        sp = (sp - 1) & 255;
        setMemory(sp, value);
        nextInstruction();
        return 5;
    }

    private int finishPop() {
        sp = (sp + 1) & 255;
        nextInstruction();
        return 5;
    }

    public byte[] vram() {
        if ((ctrlLcd & IO_ALOFF) != 0 || resetLine) {
            clear(displayRam);
            return displayRam;
        }
        if ((ctrlLcd & IO_ALON) != 0) {
            for (int i = 0; i < displayRam.length; i++) displayRam[i] = 1;
            return displayRam;
        }
        System.arraycopy(lcdRam, 0, displayRam, 0, lcdRam.length);
        displayRam[160] = (byte) portLatch[PORT_P0];
        displayRam[161] = (byte) portLatch[PORT_P1];
        displayRam[162] = (byte) portLatch[PORT_P2];
        displayRam[163] = (byte) portLatch[PORT_P3];
        displayRam[164] = (byte) readPort(PORT_R0);
        displayRam[165] = (byte) readPort(PORT_R1);
        displayRam[166] = (byte) readPort(PORT_R2);
        displayRam[167] = (byte) readPort(PORT_R3);
        displayRam[168] = (byte) readPort(PORT_R4);
        return displayRam;
    }

    public boolean displayEnabled() {
        return (ctrlLcd & IO_ALOFF) == 0 && !resetLine;
    }

    public int programCounter() {
        return pc & 0x1FFF;
    }
}
