package brickgame;

final class Ht943Cpu implements BrickCpu {
    private static final int TIMER_INTERRUPT = 4;
    private static final int EXTERNAL_INTERRUPT = 8;

    private final MachineProfile profile;
    private final byte[] rom;
    private final byte[] ram = new byte[256];
    private final int[] wr = new int[5];
    private final boolean[] buttons = new boolean[MachineProfile.BUTTON_COUNT];

    private int acc;
    private int pc;
    private int stack;
    private int carry;
    private int externalFlag;
    private int timerFlag;
    private int interruptEnable;
    private boolean halted;
    private boolean resetLine;
    private boolean timerEnabled;
    private int timerCounter;
    private int timerClockCounter;
    private int pa;
    private int pp;
    private int pm;
    private int ps;

    Ht943Cpu(MachineProfile profile, byte[] rom) {
        if (rom == null || rom.length == 0) {
            throw new IllegalArgumentException("Empty HT943 ROM");
        }
        this.profile = profile;
        this.rom = rom;
        reset();
    }

    synchronized void reset() {
        acc = 0;
        for (int i = 0; i < wr.length; i++) {
            wr[i] = 0;
        }
        for (int i = 0; i < ram.length; i++) {
            ram[i] = 0;
        }
        pc = 0;
        stack = 0;
        interruptEnable = 0;
        carry = 0;
        timerFlag = 0;
        externalFlag = 0;
        halted = false;
        resetLine = false;
        timerEnabled = false;
        timerCounter = 0;
        timerClockCounter = 0;
        pa = 0;
        pp = profile.pullupPP;
        pm = profile.pullupPM;
        ps = profile.pullupPS;
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = false;
        }
    }

    public synchronized void setButton(int button, boolean down) {
        if (button < 0 || button >= buttons.length || buttons[button] == down) {
            return;
        }
        buttons[button] = down;
        int port = profile.buttonPorts[button];
        int pin = profile.buttonPins[button];
        if (down) {
            if (port == MachineProfile.PORT_RESET) {
                resetRegistersForResetPin();
                resetLine = true;
            } else if (halted && isWakeupPin(port, pin)) {
                externalFlag = 1;
                halted = false;
            }
        }
        recomputeInputPorts();
    }

    private void resetRegistersForResetPin() {
        acc = 0;
        for (int i = 0; i < wr.length; i++) {
            wr[i] = 0;
        }
        for (int i = 0; i < ram.length; i++) {
            ram[i] = 0;
        }
        pc = 0;
        stack = 0;
        interruptEnable = 0;
        carry = 0;
        timerFlag = 0;
        externalFlag = 0;
        halted = false;
        timerEnabled = false;
        timerCounter = 0;
        timerClockCounter = 0;
        pa = 0;
    }

    private boolean isWakeupPin(int port, int pin) {
        int mask = 1 << pin;
        if (port == MachineProfile.PORT_PP) {
            return (profile.wakeupPP & mask) != 0;
        }
        if (port == MachineProfile.PORT_PM) {
            return (profile.wakeupPM & mask) != 0;
        }
        return port == MachineProfile.PORT_PS && (profile.wakeupPS & mask) != 0;
    }

    private void recomputeInputPorts() {
        pp = profile.pullupPP;
        pm = profile.pullupPM;
        ps = profile.pullupPS;
        boolean resetPressed = false;
        for (int i = 0; i < buttons.length; i++) {
            if (!buttons[i]) {
                continue;
            }
            int port = profile.buttonPorts[i];
            int bit = 1 << profile.buttonPins[i];
            if (port == MachineProfile.PORT_PP) {
                pp &= ~bit;
            } else if (port == MachineProfile.PORT_PM) {
                pm &= ~bit;
            } else if (port == MachineProfile.PORT_PS) {
                ps &= ~bit;
            } else if (port == MachineProfile.PORT_RESET) {
                resetPressed = true;
            }
        }
        resetLine = resetPressed;
    }

    public synchronized int runCycles(int budget) {
        int consumed = 0;
        while (consumed < budget) {
            consumed += clock();
        }
        return consumed;
    }

    private int clock() {
        if (!halted || resetLine) {
            if (interruptEnable != 0 && stack == 0) {
                if (externalFlag != 0) {
                    externalFlag = 0;
                    interrupt(EXTERNAL_INTERRUPT);
                } else if (timerFlag != 0) {
                    timerFlag = 0;
                    interrupt(TIMER_INTERRUPT);
                }
            }
            int opcode = readRom(pc);
            int cycles = execute(opcode);
            timerClockCounter -= cycles;
            while (timerClockCounter <= 0) {
                timerClockCounter += profile.timerDiv;
                if (timerEnabled) {
                    timerCounter = (timerCounter + 1) & 255;
                    if (timerCounter == 0) {
                        timerFlag = 1;
                    }
                }
            }
            return cycles;
        }
        return 8;
    }

    private void interrupt(int location) {
        stack = (carry << 12) | (pc & 4095);
        pc = (pc & 61440) | location;
    }

    private int readRom(int address) {
        int index = address % rom.length;
        if (index < 0) {
            index += rom.length;
        }
        return rom[index] & 255;
    }

    private int readRam(int registerPair) {
        int address = (wr[registerPair + 1] << 4) | wr[registerPair];
        return ram[address] & 15;
    }

    private void writeRam(int registerPair, int value) {
        int address = (wr[registerPair + 1] << 4) | wr[registerPair];
        ram[address] = (byte) (value & 15);
    }

    private int execute(int opcode) {
        int value;
        int oldCarry;
        int low;

        if (opcode >= 0x80 && opcode <= 0x9F) {
            low = readRom(pc + 1);
            pc += 2;
            if ((acc & (1 << ((opcode >> 3) & 3))) != 0) {
                pc = (pc & 0xF800) | ((opcode & 7) << 8) | low;
            }
            return 8;
        }
        if (opcode >= 0xA0 && opcode <= 0xA7) {
            return conditionalJump(opcode, wr[0] != 0, false);
        }
        if (opcode >= 0xA8 && opcode <= 0xAF) {
            return conditionalJump(opcode, wr[1] != 0, false);
        }
        if (opcode >= 0xB0 && opcode <= 0xB7) {
            return conditionalJump(opcode, acc == 0, false);
        }
        if (opcode >= 0xB8 && opcode <= 0xBF) {
            return conditionalJump(opcode, acc != 0, false);
        }
        if (opcode >= 0xC0 && opcode <= 0xC7) {
            return conditionalJump(opcode, carry != 0, false);
        }
        if (opcode >= 0xC8 && opcode <= 0xCF) {
            return conditionalJump(opcode, carry == 0, false);
        }
        if (opcode >= 0xD0 && opcode <= 0xD7) {
            return conditionalJump(opcode, timerFlag != 0, true);
        }
        if (opcode >= 0xD8 && opcode <= 0xDF) {
            return conditionalJump(opcode, wr[4] != 0, false);
        }
        if (opcode >= 0xE0 && opcode <= 0xEF) {
            pc = (pc & 0xF000) | ((opcode & 15) << 8) | readRom(pc + 1);
            return 8;
        }
        if (opcode >= 0xF0) {
            stack = (pc + 2) & 4095;
            pc = (pc & 0xF000) | ((opcode & 15) << 8) | readRom(pc + 1);
            return 8;
        }

        switch (opcode) {
            case 0x00:
                carry = acc & 1;
                acc = (carry << 3) | (acc >> 1);
                pc++;
                return 4;
            case 0x01:
                carry = acc >> 3;
                acc = carry | ((acc << 1) & 15);
                pc++;
                return 4;
            case 0x02:
                oldCarry = carry;
                carry = acc & 1;
                acc = (oldCarry << 3) | (acc >> 1);
                pc++;
                return 4;
            case 0x03:
                oldCarry = carry;
                carry = acc >> 3;
                acc = oldCarry | ((acc << 1) & 15);
                pc++;
                return 4;
            case 0x04:
                acc = readRam(0);
                pc++;
                return 4;
            case 0x05:
                writeRam(0, acc);
                pc++;
                return 4;
            case 0x06:
                acc = readRam(2);
                pc++;
                return 4;
            case 0x07:
                writeRam(2, acc);
                pc++;
                return 4;
            case 0x08:
                value = acc + readRam(0) + carry;
                carry = value > 15 ? 1 : 0;
                acc = value & 15;
                pc++;
                return 4;
            case 0x09:
                value = acc + readRam(0);
                carry = value > 15 ? 1 : 0;
                acc = value & 15;
                pc++;
                return 4;
            case 0x0A:
                value = acc + ((~readRam(0)) & 15) + carry;
                carry = value > 15 ? 1 : 0;
                acc = value & 15;
                pc++;
                return 4;
            case 0x0B:
                value = acc + ((~readRam(0)) & 15) + 1;
                carry = value > 15 ? 1 : 0;
                acc = value & 15;
                pc++;
                return 4;
            case 0x0C:
                writeRam(0, readRam(0) + 1);
                pc++;
                return 4;
            case 0x0D:
                writeRam(0, readRam(0) - 1);
                pc++;
                return 4;
            case 0x0E:
                writeRam(2, readRam(2) + 1);
                pc++;
                return 4;
            case 0x0F:
                writeRam(2, readRam(2) - 1);
                pc++;
                return 4;
            case 0x10:
            case 0x12:
            case 0x14:
            case 0x16:
            case 0x18:
                value = (opcode >> 1) & 7;
                wr[value] = (wr[value] + 1) & 15;
                pc++;
                return 4;
            case 0x11:
            case 0x13:
            case 0x15:
            case 0x17:
            case 0x19:
                value = (opcode >> 1) & 7;
                wr[value] = (wr[value] - 1) & 15;
                pc++;
                return 4;
            case 0x1A:
                acc &= readRam(0);
                pc++;
                return 4;
            case 0x1B:
                acc ^= readRam(0);
                pc++;
                return 4;
            case 0x1C:
                acc |= readRam(0);
                pc++;
                return 4;
            case 0x1D:
                writeRam(0, readRam(0) & acc);
                pc++;
                return 4;
            case 0x1E:
                writeRam(0, readRam(0) ^ acc);
                pc++;
                return 4;
            case 0x1F:
                writeRam(0, readRam(0) | acc);
                pc++;
                return 4;
            case 0x20:
            case 0x22:
            case 0x24:
            case 0x26:
            case 0x28:
                wr[(opcode >> 1) & 7] = acc;
                pc++;
                return 4;
            case 0x21:
            case 0x23:
            case 0x25:
            case 0x27:
            case 0x29:
                acc = wr[(opcode >> 1) & 7];
                pc++;
                return 4;
            case 0x2A:
                carry = 0;
                pc++;
                return 4;
            case 0x2B:
                carry = 1;
                pc++;
                return 4;
            case 0x2C:
                interruptEnable = 1;
                pc++;
                return 4;
            case 0x2D:
                interruptEnable = 0;
                pc++;
                return 4;
            case 0x2E:
                pc = (pc & 0xF000) | (stack & 4095);
                stack = 0;
                return 4;
            case 0x2F:
                pc = (pc & 0xF000) | (stack & 4095);
                carry = stack >> 12;
                stack = 0;
                return 4;
            case 0x30:
                pa = acc;
                pc++;
                return 4;
            case 0x31:
                acc = (acc + 1) & 15;
                pc++;
                return 4;
            case 0x32:
                acc = pm;
                pc++;
                return 4;
            case 0x33:
                acc = ps;
                pc++;
                return 4;
            case 0x34:
                acc = pp;
                pc++;
                return 4;
            case 0x35:
                pc++;
                return 4;
            case 0x36:
                if (acc > 9 || carry != 0) {
                    acc = (acc + 6) & 15;
                    carry = 1;
                }
                pc++;
                return 4;
            case 0x37:
                pc += 2;
                halted = true;
                externalFlag = 0;
                return 8;
            case 0x38:
                timerEnabled = true;
                pc++;
                return 4;
            case 0x39:
                timerEnabled = false;
                pc++;
                return 4;
            case 0x3A:
                acc = timerCounter & 15;
                pc++;
                return 4;
            case 0x3B:
                acc = (timerCounter >> 4) & 15;
                pc++;
                return 4;
            case 0x3C:
                timerCounter = (timerCounter & 0xF0) | acc;
                pc++;
                return 4;
            case 0x3D:
                timerCounter = (timerCounter & 15) | (acc << 4);
                pc++;
                return 4;
            case 0x3E:
                pc++;
                return 4;
            case 0x3F:
                acc = (acc - 1) & 15;
                pc++;
                return 4;
            case 0x40:
                value = acc + (readRom(pc + 1) & 15);
                carry = value > 15 ? 1 : 0;
                acc = value & 15;
                pc += 2;
                return 8;
            case 0x41:
                value = acc + ((~readRom(pc + 1)) & 15) + 1;
                carry = value > 15 ? 1 : 0;
                acc = value & 15;
                pc += 2;
                return 8;
            case 0x42:
                acc &= readRom(pc + 1) & 15;
                pc += 2;
                return 8;
            case 0x43:
                acc ^= readRom(pc + 1) & 15;
                pc += 2;
                return 8;
            case 0x44:
                acc |= readRom(pc + 1) & 15;
                pc += 2;
                return 8;
            case 0x45:
                pc += 2;
                return 8;
            case 0x46:
                wr[4] = readRom(pc + 1) & 15;
                pc += 2;
                return 8;
            case 0x47:
                timerCounter = readRom(pc + 1);
                pc += 2;
                return 8;
            case 0x48:
                pc++;
                return 4;
            case 0x49:
                pc++;
                return 4;
            case 0x4A:
                pc++;
                return 4;
            case 0x4B:
                pc++;
                return 4;
            case 0x4C:
                pc++;
                value = readRom((pc & 0xFF00) | (acc << 4) | readRam(0));
                acc = value & 15;
                wr[4] = (value >> 4) & 15;
                return 8;
            case 0x4D:
                pc++;
                value = readRom((pc & 0xF000) | 0xF00 | (acc << 4) | readRam(0));
                acc = value & 15;
                wr[4] = (value >> 4) & 15;
                return 8;
            case 0x4E:
                pc++;
                value = readRom((pc & 0xFF00) | (acc << 4) | wr[4]);
                acc = value & 15;
                writeRam(0, (value >> 4) & 15);
                return 8;
            case 0x4F:
                pc++;
                value = readRom((pc & 0xF000) | 0xF00 | (acc << 4) | wr[4]);
                acc = value & 15;
                writeRam(0, (value >> 4) & 15);
                return 8;
            default:
                if (opcode >= 0x50 && opcode <= 0x5F) {
                    wr[0] = opcode & 15;
                    wr[1] = readRom(pc + 1) & 15;
                    pc += 2;
                    return 8;
                }
                if (opcode >= 0x60 && opcode <= 0x6F) {
                    wr[2] = opcode & 15;
                    wr[3] = readRom(pc + 1) & 15;
                    pc += 2;
                    return 8;
                }
                if (opcode >= 0x70 && opcode <= 0x7F) {
                    acc = opcode & 15;
                    pc++;
                    return 4;
                }
                pc++;
                return 4;
        }
    }

    private int conditionalJump(int opcode, boolean condition, boolean clearTimerFlag) {
        int low = readRom(pc + 1);
        pc += 2;
        if (condition) {
            pc = (pc & 0xF800) | ((opcode & 7) << 8) | low;
            if (clearTimerFlag) {
                timerFlag = 0;
            }
        }
        return 8;
    }

    public byte[] vram() {
        return ram;
    }

    public boolean displayEnabled() {
        return !halted && !resetLine;
    }

    public int programCounter() {
        return pc & 4095;
    }
}
