package brickgame;

final class Em73000Cpu implements BrickCpu {
    private static final int RAM_SIZE = 384;
    private static final int STACK_OFFSET = 0x0C0;

    private static final int INT0_ID = 5;
    private static final int TRGA_ID = 3;
    private static final int TRGB_ID = 2;
    private static final int TBI_ID = 1;
    private static final int INT1_ID = 0;

    private static final int[] INTERRUPT_ENTRIES = {0x00C, 0x00A, 0x008, 0x006, 0x004, 0x002};
    private static final int[] TIMER_DIV = {1 << 10, 1 << 14, 1 << 18, 1 << 22};
    private static final int[] TIME_BASE_DIV = {
        0, 0, 0, 0, 1 << 10, 1 << 11, 1 << 12, 1 << 13,
        0, 0, 0, 0, 1 << 9, 1 << 8, 1 << 15, 1 << 17
    };
    private static final int[] WARMUP_TIME = {1 << 18, 1 << 14, 1 << 16, 8};
    private static final int[] MASK_TO_IL = {
        0x20, 0x21, 0x26, 0x27, 0x28, 0x29, 0x2E, 0x2F,
        0x30, 0x31, 0x36, 0x37, 0x38, 0x39, 0x3E, 0x3F
    };

    private static final int P9_RAM_BANK = 8;
    private static final int P16_SWWT = 3;
    private static final int P16_SE = 4;
    private static final int P27_LDC = 0xC;
    private static final int P28_IPSA = 3;
    private static final int P28_TMSA = 0xC;
    private static final int P29_IPSB = 3;
    private static final int P29_TMSB = 0xC;

    private static final int[] BUTTON_PORTS = {7, 7, 7, 7, 8, 0, 8, -1};
    private static final int[] BUTTON_PINS = {2, 3, 1, 0, 1, 0, 3, 0};

    private final byte[] rom;
    private final byte[] ram = new byte[RAM_SIZE];
    private final byte[] emptyVram = new byte[RAM_SIZE];
    private final boolean[] buttons = new boolean[MachineProfile.BUTTON_COUNT];

    private int acc;
    private int pc;
    private int sp;
    private int dp;
    private int hl;
    private int interruptEnable;
    private int carry;
    private int zero;
    private int status;
    private int timerA;
    private int timerB;
    private int interruptLatch;
    private int interruptMask;
    private int p0;
    private int p4;
    private int p5;
    private int p6;
    private int p7;
    private int p8;
    private int p9;
    private int p16;
    private int p23;
    private int p24;
    private int p25;
    private int p27;
    private int p28;
    private int p29;
    private int p30;
    private int timerACounter;
    private int timerBCounter;
    private int timeBaseCounter;
    private int ramBank;
    private int instructionCounter;

    Em73000Cpu(MachineProfile profile, byte[] rom) {
        if (rom == null || rom.length == 0) {
            throw new IllegalArgumentException("Empty EM73000 ROM");
        }
        this.rom = rom;
        reset();
    }

    private void reset() {
        acc = 0;
        pc = 0;
        sp = 0;
        dp = 0;
        hl = 0;
        interruptEnable = 0;
        carry = 0;
        zero = 0;
        status = 1;
        timerA = 0;
        timerB = 0;
        interruptLatch = 0;
        interruptMask = 0;
        for (int i = 0; i < ram.length; i++) {
            ram[i] = 0;
        }
        p0 = 15;
        p4 = 0;
        p5 = 0;
        p6 = 0;
        p7 = 15;
        p8 = 15;
        p9 = 0;
        p16 = 0;
        p23 = 15;
        p24 = 15;
        p25 = 0;
        p27 = 0;
        p28 = 0;
        p29 = 0;
        p30 = 0;
        timerACounter = 0;
        timerBCounter = 0;
        timeBaseCounter = 0;
        ramBank = 0;
        instructionCounter = 0;
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = false;
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
        int port = BUTTON_PORTS[button];
        int pin = BUTTON_PINS[button];
        if (down) {
            pinSet(port, pin, 0);
        } else {
            pinRelease(port, pin);
        }
    }

    private void pinSet(int port, int pin, int level) {
        int mask = 1 << pin;
        if (port == 0) {
            int previous = p0;
            p0 = (~mask & (p0 | (level << pin))) & 15;
            if ((1 & previous & mask) != 0 && (p0 & mask) == 0) {
                p16 &= ~P16_SE;
            }
        } else if (port == 7) {
            p7 = ((~mask & p7) | (level << pin)) & 15;
        } else if (port == 8) {
            int previous = p8;
            p8 = ((~mask & p8) | (level << pin)) & 15;
            if ((0 & previous & mask) != 0 && (p8 & mask) == 0) {
                p16 &= ~P16_SE;
            }
            if ((previous & mask) != 0 && (p8 & mask) == 0) {
                if (pin == 0) {
                    interruptLatch |= 1 << INT1_ID;
                }
                if (pin == 2) {
                    interruptLatch |= 1 << INT0_ID;
                }
            }
        }
    }

    private void pinRelease(int port, int pin) {
        int mask = 1 << pin;
        if (port == 0) {
            int previous = p0;
            p0 = ((p0 & ~mask) | (15 & mask)) & 15;
            if ((1 & previous & mask) != 0 && (p0 & mask) == 0) {
                p16 &= ~P16_SE;
            }
        } else if (port == 7) {
            p7 = ((p7 & ~mask) | (15 & mask)) & 15;
        } else if (port == 8) {
            int previous = p8;
            p8 = ((p8 & ~mask) | (15 & mask)) & 15;
            if ((0 & previous & mask) != 0 && (p8 & mask) == 0) {
                p16 &= ~P16_SE;
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
        if (interruptEnable != 0) {
            interrupt();
        }
        if ((p16 & P16_SE) == 0) {
            int opcode = readRom(pc);
            int cycles = execute(opcode);
            instructionCounter++;
            processTimer(cycles);
            return cycles;
        }
        return WARMUP_TIME[p16 & P16_SWWT];
    }

    private int readRom(int address) {
        int index = address % rom.length;
        if (index < 0) {
            index += rom.length;
        }
        return rom[index] & 255;
    }

    private int readRomWord(int address) {
        return (readRom(address) << 8) | readRom(address + 1);
    }

    private int ramValue(int address) {
        return ram[address] & 15;
    }

    private void setRam(int address, int value) {
        ram[address] = (byte) (value & 15);
    }

    private int memoryAtHl() {
        return ramValue(ramBank + hl);
    }

    private void setMemoryAtHl(int value) {
        setRam(ramBank + hl, value);
    }

    private void processTimer(int cycles) {
        if ((p28 & P28_TMSA) == 0x8) {
            timerACounter -= cycles;
            if (timerACounter <= 0) {
                timerACounter += TIMER_DIV[p28 & P28_IPSA];
                timerA = (timerA + 1) & 0xFFF;
                if (timerA == 0) {
                    interruptLatch |= 1 << TRGA_ID;
                }
            }
        }
        if ((p29 & P29_TMSB) == 0x8) {
            timerBCounter -= cycles;
            if (timerBCounter <= 0) {
                timerBCounter += TIMER_DIV[p29 & P29_IPSB];
                timerB = (timerB + 1) & 0xFFF;
                if (timerB == 0) {
                    interruptLatch |= 1 << TRGB_ID;
                }
            }
        }
        if (TIME_BASE_DIV[p25 & 15] > 0) {
            timeBaseCounter -= cycles;
            if (timeBaseCounter <= 0) {
                timerBCounter += TIME_BASE_DIV[p25 & 15];
                interruptLatch |= 1 << TBI_ID;
            }
        }
    }

    private void interrupt() {
        for (int id = 0; id < 6; id++) {
            int bit = 0x20 >> id;
            if ((interruptLatch & bit & MASK_TO_IL[interruptMask & 15]) != 0) {
                int oldPc = pc;
                int stackAddress = STACK_OFFSET + (sp << 2);
                setRam(stackAddress, (carry << 3) | (zero << 2) | (status << 1) | ((oldPc >> 12) & 1));
                setRam(stackAddress + 1, (oldPc >> 8) & 15);
                setRam(stackAddress + 2, (oldPc >> 4) & 15);
                setRam(stackAddress + 3, oldPc & 15);
                sp = (sp - 1) & 15;
                pc = INTERRUPT_ENTRIES[5 - id];
                status = 1;
                interruptEnable = 0;
                interruptLatch &= ~bit;
                return;
            }
        }
    }

    private int readPort(int port) {
        switch (port) {
            case 0: return p0;
            case 4: return p4;
            case 7: return p7;
            case 8: return p8;
            default: return 0;
        }
    }

    private void writePort(int port, int value) {
        value &= 15;
        switch (port) {
            case 4:
                p4 = value;
                break;
            case 5:
                p5 = value;
                break;
            case 6:
                p6 = value;
                break;
            case 9:
                ramBank = (value >> P9_RAM_BANK) * 256;
                p9 = value;
                break;
            case 16:
                p16 = value;
                break;
            case 23:
                p23 = value;
                break;
            case 24:
                p24 = value;
                break;
            case 25:
                p25 = value;
                break;
            case 27:
                p27 = value;
                break;
            case 28:
                p28 = value;
                timerACounter = 0;
                break;
            case 29:
                p29 = value;
                timerBCounter = 0;
                break;
            case 30:
                p30 = value;
                break;
            default:
                break;
        }
    }

    private int execute(int opcode) {
        if (opcode < 0x40) {
            pc = (pc + 1) & 0x1FFF;
            if (status != 0) {
                pc = (pc & 0x1FC0) | (opcode & 0x3F);
            }
            status = 1;
            return 8;
        }
        if (opcode < 0x48) {
            int returnPc = pc + 2;
            int stackAddress = STACK_OFFSET + (sp << 2);
            setRam(stackAddress, returnPc >> 12);
            setRam(stackAddress + 1, (returnPc >> 8) & 15);
            setRam(stackAddress + 2, (returnPc >> 4) & 15);
            setRam(stackAddress + 3, returnPc & 15);
            pc = ((opcode & 7) << 8) | readRom(pc + 1);
            sp = (sp - 1) & 15;
            return 16;
        }
        if (opcode >= 0x80 && opcode <= 0x8F) {
            hl = (hl & 0xF0) | (opcode & 15);
            status = 1;
            advance(1);
            return 8;
        }
        if (opcode >= 0x90 && opcode <= 0x9F) {
            hl = (hl & 15) | ((opcode & 15) << 4);
            status = 1;
            advance(1);
            return 8;
        }
        if (opcode >= 0xA0 && opcode <= 0xAF) {
            int k = opcode & 15;
            setMemoryAtHl(k);
            hl = (hl & 0xF0) | ((hl + 1) & 15);
            zero = (hl & 15) == 0 ? 1 : 0;
            status = (hl & 15) != 0 ? 1 : 0;
            advance(1);
            return 8;
        }
        if (opcode >= 0xB0 && opcode <= 0xBF) {
            int k = opcode & 15;
            carry = k >= acc ? 1 : 0;
            zero = k == acc ? 1 : 0;
            status = 1 - zero;
            advance(1);
            return 8;
        }
        if (opcode >= 0xC0 && opcode <= 0xCF) {
            pc = (pc + 2) & 0x1FFF;
            if (status != 0) {
                int address = ((opcode << 8) | readRom(pc - 1)) & 0xFFF;
                pc = (pc & 0x1000) | address;
            }
            status = 1;
            return 16;
        }
        if (opcode >= 0xD0 && opcode <= 0xDF) {
            acc = opcode & 15;
            zero = acc == 0 ? 1 : 0;
            status = 1;
            advance(1);
            return 8;
        }
        if (opcode >= 0xE0 && opcode <= 0xEF) {
            int returnPc = pc + 1;
            int stackAddress = STACK_OFFSET + (sp << 2);
            setRam(stackAddress, returnPc >> 12);
            setRam(stackAddress + 1, (returnPc >> 8) & 15);
            setRam(stackAddress + 2, (returnPc >> 4) & 15);
            setRam(stackAddress + 3, returnPc & 15);
            sp = (sp - 1) & 15;
            int n = opcode & 15;
            pc = n * 8 + 6 + (n == 0 ? 0x80 : 0);
            return 16;
        }
        if (opcode >= 0xF0 && opcode <= 0xF3) {
            setMemoryAtHl(memoryAtHl() & ~(1 << (opcode & 3)));
            status = 1;
            advance(1);
            return 8;
        }
        if (opcode >= 0xF4 && opcode <= 0xF7) {
            setMemoryAtHl(memoryAtHl() | (1 << (opcode & 3)));
            status = 1;
            advance(1);
            return 8;
        }
        if (opcode >= 0xF8 && opcode <= 0xFB) {
            status = (acc & (1 << (opcode & 3))) == 0 ? 1 : 0;
            advance(1);
            return 8;
        }
        if (opcode >= 0xFC) {
            status = (memoryAtHl() & (1 << (opcode & 3))) == 0 ? 1 : 0;
            advance(1);
            return 8;
        }

        switch (opcode) {
            case 0x48:
                int ky = readRom(pc + 1);
                setRam(ky & 15, (ky >> 4) & 15);
                status = 1;
                advance(2);
                return 16;
            case 0x49:
                ky = readRom(pc + 1);
                int k = (ky >> 4) & 15;
                int y = ky & 15;
                setRam(y, ramValue(y) + k);
                zero = ramValue(y) == 0 ? 1 : 0;
                status = ramValue(y) >= k ? 1 : 0;
                advance(2);
                return 16;
            case 0x4A:
                int kp = readRom(pc + 1);
                writePort(kp & 15, (kp >> 4) & 15);
                status = 1;
                advance(2);
                return 16;
            case 0x4B:
                ky = readRom(pc + 1);
                k = (ky >> 4) & 15;
                y = ky & 15;
                carry = k >= ramValue(y) ? 1 : 0;
                zero = k == ramValue(y) ? 1 : 0;
                status = 1 - zero;
                advance(2);
                return 16;
            case 0x4C:
                int index = readRom(pc + 1);
                int oldHl = hl;
                hl = (ramValue(ramBank + index + 1) << 4) | ramValue(ramBank + index);
                setRam(ramBank + index, oldHl & 15);
                setRam(ramBank + index + 1, (oldHl >> 4) & 15);
                status = 1;
                advance(2);
                return 16;
            case 0x4D:
                sp = (sp + 1) & 15;
                int stackAddress = STACK_OFFSET + (sp << 2);
                pc = ((ramValue(stackAddress) & 1) << 12)
                        | (ramValue(stackAddress + 1) << 8)
                        | (ramValue(stackAddress + 2) << 4)
                        | ramValue(stackAddress + 3);
                carry = (ramValue(stackAddress) & 8) != 0 ? 1 : 0;
                zero = (ramValue(stackAddress) & 4) != 0 ? 1 : 0;
                status = (ramValue(stackAddress) & 2) != 0 ? 1 : 0;
                interruptEnable = 1;
                return 16;
            case 0x4E:
                index = readRom(pc + 1);
                hl = (ramValue(ramBank + index + 1) << 4) | ramValue(ramBank + index);
                status = 1;
                advance(2);
                return 16;
            case 0x4F:
                sp = (sp + 1) & 15;
                stackAddress = STACK_OFFSET + (sp << 2);
                pc = ((ramValue(stackAddress) & 1) << 12)
                        | (ramValue(stackAddress + 1) << 8)
                        | (ramValue(stackAddress + 2) << 4)
                        | ramValue(stackAddress + 3);
                return 16;
            case 0x50:
                int newCarry = (acc & 8) != 0 ? 1 : 0;
                acc = ((acc << 1) & 14) | carry;
                carry = newCarry;
                zero = acc == 0 ? 1 : 0;
                status = 1 - newCarry;
                advance(1);
                return 8;
            case 0x51:
                newCarry = (acc & 1) != 0 ? 1 : 0;
                acc = ((acc >> 1) & 7) | ((carry << 3) & 8);
                carry = newCarry;
                zero = acc == 0 ? 1 : 0;
                status = 1 - newCarry;
                advance(1);
                return 8;
            case 0x52:
                status = carry;
                carry = 1;
                advance(1);
                return 8;
            case 0x53:
                status = 1 - carry;
                carry = 0;
                advance(1);
                return 8;
            case 0x55:
                pc = (pc + 3) & 0x1FFF;
                if (status != 0) {
                    pc = 0x1000 | (readRomWord(pc - 2) & 0xFFF);
                }
                status = 1;
                return 24;
            case 0x56:
                advance(1);
                return 8;
            case 0x57:
                pc = (pc + 3) & 0x1FFF;
                if (status != 0) {
                    pc = readRomWord(pc - 2) & 0xFFF;
                }
                status = 1;
                return 24;
            case 0x58:
                int oldAcc = acc;
                acc = memoryAtHl();
                setMemoryAtHl(oldAcc);
                zero = acc == 0 ? 1 : 0;
                status = 1;
                advance(1);
                return 8;
            case 0x59:
                setMemoryAtHl(acc);
                status = 1;
                advance(1);
                return 8;
            case 0x5A:
                acc = memoryAtHl();
                zero = acc == 0 ? 1 : 0;
                status = 1;
                advance(1);
                return 8;
            case 0x5B:
                status = zero;
                advance(1);
                return 8;
            case 0x5C:
                acc = (acc - 1) & 15;
                zero = acc == 0 ? 1 : 0;
                status = acc != 15 ? 1 : 0;
                advance(1);
                return 8;
            case 0x5D:
                setMemoryAtHl(memoryAtHl() - 1);
                zero = memoryAtHl() == 0 ? 1 : 0;
                status = memoryAtHl() != 15 ? 1 : 0;
                advance(1);
                return 8;
            case 0x5E:
                acc = (acc + 1) & 15;
                zero = acc == 0 ? 1 : 0;
                status = acc != 0 ? 1 : 0;
                advance(1);
                return 8;
            case 0x5F:
                setMemoryAtHl(memoryAtHl() + 1);
                zero = memoryAtHl() == 0 ? 1 : 0;
                status = memoryAtHl() != 0 ? 1 : 0;
                advance(1);
                return 8;
            case 0x60:
                int bit = hl & 3;
                int port = ((hl & 12) >> 2) + 4;
                writePort(port, readPort(port) & ~(1 << bit));
                status = 1;
                advance(1);
                return 16;
            case 0x61:
                bit = hl & 3;
                port = ((hl & 12) >> 2) + 4;
                status = (readPort(port) & (1 << bit)) == 0 ? 1 : 0;
                advance(1);
                return 16;
            case 0x62:
                bit = hl & 3;
                port = ((hl & 12) >> 2) + 4;
                writePort(port, readPort(port) | (1 << bit));
                status = 1;
                advance(1);
                return 16;
            case 0x63:
                return executeCil();
            case 0x64:
                oldAcc = acc;
                acc = hl & 15;
                hl = (hl & 0xF0) | oldAcc;
                zero = acc == 0 ? 1 : 0;
                status = 1;
                advance(1);
                return 16;
            case 0x65:
                acc = readRom(0x1000 | dp) & 15;
                zero = acc == 0 ? 1 : 0;
                status = 1;
                advance(1);
                return 16;
            case 0x66:
                oldAcc = acc;
                acc = (hl >> 4) & 15;
                hl = (hl & 15) | (oldAcc << 4);
                zero = acc == 0 ? 1 : 0;
                status = 1;
                advance(1);
                return 16;
            case 0x67:
                acc = (readRom(0x1000 | dp) >> 4) & 15;
                dp = (dp + 1) & 0xFFF;
                zero = acc == 0 ? 1 : 0;
                status = 1;
                advance(1);
                return 16;
            case 0x68:
                index = readRom(pc + 1);
                oldAcc = acc;
                acc = ramValue(ramBank + index);
                setRam(ramBank + index, oldAcc);
                zero = acc == 0 ? 1 : 0;
                status = 1;
                advance(2);
                return 16;
            case 0x69:
                index = readRom(pc + 1);
                storeSpecial(index);
                status = 1;
                advance(2);
                return 16;
            case 0x6A:
                index = readRom(pc + 1);
                loadSpecial(index);
                zero = acc == 0 ? 1 : 0;
                status = 1;
                advance(2);
                return 16;
            case 0x6B:
                index = readRom(pc + 1);
                carry = ramValue(ramBank + index) >= acc ? 1 : 0;
                zero = ramValue(ramBank + index) == acc ? 1 : 0;
                status = 1 - zero;
                advance(2);
                return 16;
            case 0x6C:
                return executeBitRam();
            case 0x6D:
                return executeBitPort();
            case 0x6E:
                return executeMath();
            case 0x6F:
                return executeIo();
            case 0x70:
                int oldCarry = carry;
                acc = (acc + memoryAtHl() + oldCarry) & 15;
                carry = acc < (memoryAtHl() + oldCarry) ? 1 : 0;
                zero = acc == 0 ? 1 : 0;
                status = 1 - carry;
                advance(1);
                return 8;
            case 0x71:
                acc = (acc + memoryAtHl()) & 15;
                zero = acc == 0 ? 1 : 0;
                status = acc >= memoryAtHl() ? 1 : 0;
                advance(1);
                return 8;
            case 0x72:
                int borrow = carry == 0 ? 1 : 0;
                acc = (memoryAtHl() - acc - borrow) & 15;
                carry = memoryAtHl() >= acc + borrow ? 1 : 0;
                zero = acc == 0 ? 1 : 0;
                status = carry;
                advance(1);
                return 8;
            case 0x73:
                carry = memoryAtHl() >= acc ? 1 : 0;
                zero = memoryAtHl() == acc ? 1 : 0;
                status = 1 - zero;
                advance(1);
                return 8;
            case 0x74:
                acc = hl & 15;
                zero = acc == 0 ? 1 : 0;
                status = 1;
                advance(1);
                return 8;
            case 0x75:
                oldAcc = acc;
                acc = interruptMask;
                interruptMask = oldAcc;
                status = 1;
                advance(1);
                return 8;
            case 0x76:
                acc = hl >> 4;
                zero = acc == 0 ? 1 : 0;
                status = 1;
                advance(1);
                return 8;
            case 0x78:
                acc |= memoryAtHl();
                zero = acc == 0 ? 1 : 0;
                status = 1 - zero;
                advance(1);
                return 8;
            case 0x79:
                acc ^= memoryAtHl();
                zero = acc == 0 ? 1 : 0;
                status = 1 - zero;
                advance(1);
                return 8;
            case 0x7B:
                acc &= memoryAtHl();
                zero = acc == 0 ? 1 : 0;
                status = 1 - zero;
                advance(1);
                return 8;
            case 0x7C:
                hl = (hl & 0xF0) | ((hl - 1) & 15);
                zero = (hl & 15) == 0 ? 1 : 0;
                status = (hl & 15) != 15 ? 1 : 0;
                advance(1);
                return 8;
            case 0x7D:
                setMemoryAtHl(acc);
                hl = (hl & 0xF0) | ((hl - 1) & 15);
                zero = (hl & 15) == 0 ? 1 : 0;
                status = (hl & 15) != 15 ? 1 : 0;
                advance(1);
                return 8;
            case 0x7E:
                hl = (hl & 0xF0) | ((hl + 1) & 15);
                zero = (hl & 15) == 0 ? 1 : 0;
                status = (hl & 15) != 0 ? 1 : 0;
                advance(1);
                return 8;
            case 0x7F:
                setMemoryAtHl(acc);
                hl = (hl & 0xF0) | ((hl + 1) & 15);
                zero = (hl & 15) == 0 ? 1 : 0;
                status = (hl & 15) != 0 ? 1 : 0;
                advance(1);
                return 8;
            default:
                advance(1);
                return 8;
        }
    }

    private void advance(int bytes) {
        pc = (pc + bytes) & 0x1FFF;
    }

    private void storeSpecial(int index) {
        if (index < 0xF4) {
            setRam(ramBank + index, acc);
        } else if (index == 0xF4) {
            timerA = (timerA & 0xFF0) | acc;
        } else if (index == 0xF5) {
            timerA = (timerA & 0xF0F) | (acc << 4);
        } else if (index == 0xF6) {
            timerA = (timerA & 0x0FF) | (acc << 8);
        } else if (index == 0xF8) {
            timerB = (timerB & 0xFF0) | acc;
        } else if (index == 0xF9) {
            timerB = (timerB & 0xF0F) | (acc << 4);
        } else if (index == 0xFA) {
            timerB = (timerB & 0x0FF) | (acc << 8);
        } else if (index == 0xFC) {
            dp = (dp & 0xFF0) | acc;
        } else if (index == 0xFD) {
            dp = (dp & 0xF0F) | (acc << 4);
        } else if (index == 0xFE) {
            dp = (dp & 0x0FF) | (acc << 8);
        } else if (index == 0xFF) {
            sp = acc;
        }
    }

    private void loadSpecial(int index) {
        if (index < 0xF4) {
            acc = ramValue(ramBank + index);
        } else if (index == 0xF4) {
            acc = timerA & 15;
        } else if (index == 0xF5) {
            acc = (timerA >> 4) & 15;
        } else if (index == 0xF6) {
            acc = (timerA >> 8) & 15;
        } else if (index == 0xF8) {
            acc = timerB & 15;
        } else if (index == 0xF9) {
            acc = (timerB >> 4) & 15;
        } else if (index == 0xFA) {
            acc = (timerB >> 8) & 15;
        } else if (index == 0xFC) {
            acc = dp & 15;
        } else if (index == 0xFD) {
            acc = (dp >> 4) & 15;
        } else if (index == 0xFE) {
            acc = (dp >> 8) & 15;
        } else if (index == 0xFF) {
            acc = sp;
        }
    }

    private int executeCil() {
        int value = readRom(pc + 1);
        int id = (value >> 6) & 3;
        int mask = value & 0x3F;
        if (id == 1) {
            interruptLatch &= mask;
            interruptEnable = 1;
            status = 1;
            advance(2);
            return 16;
        }
        if (id == 2) {
            interruptLatch &= mask;
            interruptEnable = 0;
            status = 1;
            advance(2);
            return 16;
        }
        if (id == 3) {
            interruptLatch &= mask;
            status = 1;
            advance(2);
            return 16;
        }
        advance(1);
        return 8;
    }

    private int executeBitRam() {
        int value = readRom(pc + 1);
        int id = (value >> 6) & 3;
        int bit = (value >> 4) & 3;
        int address = value & 15;
        int current = ramValue(address);
        if (id == 0) {
            status = (current & (1 << bit)) == 0 ? 1 : 0;
        } else if (id == 1) {
            setRam(address, current | (1 << bit));
            status = 1;
        } else if (id == 2) {
            status = (current & (1 << bit)) != 0 ? 1 : 0;
        } else {
            setRam(address, current & ~(1 << bit));
            status = 1;
        }
        advance(2);
        return 16;
    }

    private int executeBitPort() {
        int value = readRom(pc + 1);
        int id = (value >> 6) & 3;
        int bit = (value >> 4) & 3;
        int port = value & 15;
        int current = readPort(port);
        if (id == 0) {
            status = (current & (1 << bit)) == 0 ? 1 : 0;
        } else if (id == 1) {
            writePort(port, current | (1 << bit));
            status = 1;
        } else if (id == 2) {
            status = (current & (1 << bit)) != 0 ? 1 : 0;
        } else {
            writePort(port, current & ~(1 << bit));
            status = 1;
        }
        advance(2);
        return 16;
    }

    private int executeMath() {
        int value = readRom(pc + 1);
        int id = (value >> 4) & 15;
        int k = value & 15;
        switch (id) {
            case 1:
                hl = (hl & 0xF0) | ((hl + k) & 15);
                zero = (hl & 15) == 0 ? 1 : 0;
                status = (hl & 15) >= k ? 1 : 0;
                break;
            case 3:
                zero = k == (hl & 15) ? 1 : 0;
                status = k >= (hl & 15) ? 1 : 0;
                break;
            case 4:
                acc |= k;
                zero = acc == 0 ? 1 : 0;
                status = 1 - zero;
                break;
            case 5:
                acc = (acc + k) & 15;
                zero = acc == 0 ? 1 : 0;
                status = acc >= k ? 1 : 0;
                break;
            case 6:
                acc &= k;
                zero = acc == 0 ? 1 : 0;
                status = 1 - zero;
                break;
            case 7:
                acc = (k - acc) & 15;
                zero = acc == 0 ? 1 : 0;
                status = k >= acc ? 1 : 0;
                break;
            case 9:
                hl = (hl + (k << 4)) & 255;
                zero = (hl >> 4) == 0 ? 1 : 0;
                status = (hl >> 4) >= k ? 1 : 0;
                break;
            case 11:
                zero = (hl >> 4) == k ? 1 : 0;
                status = k >= (hl >> 4) ? 1 : 0;
                break;
            case 12:
                setMemoryAtHl(memoryAtHl() | k);
                zero = memoryAtHl() == 0 ? 1 : 0;
                status = 1 - zero;
                break;
            case 13:
                setMemoryAtHl(memoryAtHl() + k);
                zero = memoryAtHl() == 0 ? 1 : 0;
                status = memoryAtHl() >= k ? 1 : 0;
                break;
            case 14:
                setMemoryAtHl(memoryAtHl() & k);
                zero = memoryAtHl() == 0 ? 1 : 0;
                status = 1 - zero;
                break;
            case 15:
                setMemoryAtHl(k - memoryAtHl());
                zero = memoryAtHl() == 0 ? 1 : 0;
                status = k >= memoryAtHl() ? 1 : 0;
                break;
            default:
                advance(1);
                return 8;
        }
        advance(2);
        return 16;
    }

    private int executeIo() {
        int value = readRom(pc + 1);
        int id = (value >> 6) & 3;
        if (id == 0) {
            int port = value & 0x1F;
            writePort(port, acc);
            status = 1;
        } else if (id == 1) {
            int port = value & 15;
            acc = readPort(port);
            zero = acc == 0 ? 1 : 0;
            status = 1 - zero;
        } else if (id == 2) {
            int port = value & 0x1F;
            writePort(port, memoryAtHl());
            status = 1;
        } else {
            int port = value & 15;
            setMemoryAtHl(readPort(port));
            status = memoryAtHl() != 0 ? 1 : 0;
        }
        advance(2);
        return 16;
    }

    public byte[] vram() {
        return (p16 & P16_SE) == 0 ? ram : emptyVram;
    }

    public boolean displayEnabled() {
        return (p16 & P16_SE) == 0;
    }

    public int programCounter() {
        return pc & 0xFFF;
    }
}
