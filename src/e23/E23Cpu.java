package e23;

final class E23Cpu {
    static final int BUTTON_LEFT = 0;
    static final int BUTTON_RIGHT = 1;
    static final int BUTTON_DOWN = 2;
    static final int BUTTON_ROTATE = 3;
    static final int BUTTON_START = 4;
    static final int BUTTON_AUX = 5;
    static final int BUTTON_OPTION = 6;
    static final int BUTTON_RESET = 7;
    static final int BUTTON_COUNT = 8;

    private static final int PORT_PP = 0;
    private static final int PORT_PM = 1;
    private static final int PORT_PS = 2;
    private static final int PORT_RESET = 3;
    private static final int TIMER_INTERRUPT = 4;
    private static final int EXTERNAL_INTERRUPT = 8;
    private static final int TIMER_DIV = 16;
    private static final byte[] BUTTON_PORTS = {
        PORT_PP, PORT_PP, PORT_PP, PORT_PP,
        PORT_PS, PORT_PS, PORT_PS, PORT_RESET
    };
    private static final byte[] BUTTON_PINS = {3, 2, 1, 0, 0, 2, 1, 0};

    private final byte[] rom;
    private final byte[] ram = new byte[256];
    private final int[] wr = new int[5];
    private final boolean[] buttons = new boolean[BUTTON_COUNT];

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

    E23Cpu(byte[] rom) {
        if (rom == null || rom.length != 4096) {
            throw new IllegalArgumentException("Invalid E23 ROM");
        }
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
        pp = 15;
        pm = 15;
        ps = 15;
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = false;
        }
    }

    synchronized void setButton(int button, boolean down) {
        if (button < 0 || button >= buttons.length || buttons[button] == down) {
            return;
        }
        buttons[button] = down;
        int port = BUTTON_PORTS[button];
        int pin = BUTTON_PINS[button];
        if (port == PORT_RESET) {
            if (down) {
                resetRegistersForResetPin();
                resetLine = true;
            } else {
                resetLine = false;
            }
            return;
        }

        int bit = 1 << pin;
        if (port == PORT_PP) {
            pp = down ? pp & ~bit : pp | bit;
        } else if (port == PORT_PM) {
            pm = down ? pm & ~bit : pm | bit;
        } else {
            ps = down ? ps & ~bit : ps | bit;
        }
        if (down && halted && port == PORT_PS && pin == 2) {
            externalFlag = 1;
            halted = false;
        }
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
        pp = 15;
        pm = 15;
        ps = 15;
    }

    synchronized int runCycles(int budget) {
        int consumed = 0;
        while (consumed < budget) {
            int cycles = 8;
            if (!halted && !resetLine) {
                if (interruptEnable != 0 && stack == 0) {
                    if (externalFlag != 0) {
                        externalFlag = 0;
                        interrupt(EXTERNAL_INTERRUPT);
                    } else if (timerFlag != 0) {
                        timerFlag = 0;
                        interrupt(TIMER_INTERRUPT);
                    }
                }
                cycles = execute(rom[pc & 4095] & 255);
                timerClockCounter -= cycles;
                if (timerClockCounter <= 0) {
                    timerClockCounter += TIMER_DIV;
                    if (timerEnabled) {
                        timerCounter = (timerCounter + 1) & 255;
                        if (timerCounter == 0) {
                            timerFlag = 1;
                        }
                    }
                }
            }
            consumed += cycles;
        }
        return consumed;
    }

    private void interrupt(int location) {
        stack = (carry << 12) | (pc & 4095);
        pc = (pc & 61440) | location;
    }

    private int execute(int opcode) {
        int value;
        int oldCarry;
        int low;
        int group;
        int address;
        boolean condition;

        if (opcode >= 0x80 && opcode <= 0x9F) {
            low = rom[(pc + 1) & 4095] & 255;
            pc += 2;
            if ((acc & (1 << ((opcode >> 3) & 3))) != 0) {
                pc = (pc & 0xF800) | ((opcode & 7) << 8) | low;
            }
            return 8;
        }
        if (opcode >= 0xA0 && opcode <= 0xDF) {
            group = (opcode - 0xA0) >> 3;
            switch (group) {
                case 0:
                    condition = wr[0] != 0;
                    break;
                case 1:
                    condition = wr[1] != 0;
                    break;
                case 2:
                    condition = acc == 0;
                    break;
                case 3:
                    condition = acc != 0;
                    break;
                case 4:
                    condition = carry != 0;
                    break;
                case 5:
                    condition = carry == 0;
                    break;
                case 6:
                    condition = timerFlag != 0;
                    break;
                default:
                    condition = wr[4] != 0;
                    break;
            }
            low = rom[(pc + 1) & 4095] & 255;
            pc += 2;
            if (condition) {
                pc = (pc & 0xF800) | ((opcode & 7) << 8) | low;
                if (group == 6) {
                    timerFlag = 0;
                }
            }
            return 8;
        }
        if (opcode >= 0xE0 && opcode <= 0xEF) {
            pc = (pc & 0xF000) | ((opcode & 15) << 8)
                    | (rom[(pc + 1) & 4095] & 255);
            return 8;
        }
        if (opcode >= 0xF0) {
            stack = (pc + 2) & 4095;
            pc = (pc & 0xF000) | ((opcode & 15) << 8)
                    | (rom[(pc + 1) & 4095] & 255);
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
                address = (wr[1] << 4) | wr[0];
                acc = ram[address] & 15;
                pc++;
                return 4;
            case 0x05:
                address = (wr[1] << 4) | wr[0];
                ram[address] = (byte) acc;
                pc++;
                return 4;
            case 0x06:
                address = (wr[3] << 4) | wr[2];
                acc = ram[address] & 15;
                pc++;
                return 4;
            case 0x07:
                address = (wr[3] << 4) | wr[2];
                ram[address] = (byte) acc;
                pc++;
                return 4;
            case 0x08:
                address = (wr[1] << 4) | wr[0];
                value = acc + (ram[address] & 15) + carry;
                carry = value > 15 ? 1 : 0;
                acc = value & 15;
                pc++;
                return 4;
            case 0x09:
                address = (wr[1] << 4) | wr[0];
                value = acc + (ram[address] & 15);
                carry = value > 15 ? 1 : 0;
                acc = value & 15;
                pc++;
                return 4;
            case 0x0A:
                address = (wr[1] << 4) | wr[0];
                value = acc + ((~ram[address]) & 15) + carry;
                carry = value > 15 ? 1 : 0;
                acc = value & 15;
                pc++;
                return 4;
            case 0x0B:
                address = (wr[1] << 4) | wr[0];
                value = acc + ((~ram[address]) & 15) + 1;
                carry = value > 15 ? 1 : 0;
                acc = value & 15;
                pc++;
                return 4;
            case 0x0C:
                address = (wr[1] << 4) | wr[0];
                ram[address] = (byte) ((ram[address] + 1) & 15);
                pc++;
                return 4;
            case 0x0D:
                address = (wr[1] << 4) | wr[0];
                ram[address] = (byte) ((ram[address] - 1) & 15);
                pc++;
                return 4;
            case 0x0E:
                address = (wr[3] << 4) | wr[2];
                ram[address] = (byte) ((ram[address] + 1) & 15);
                pc++;
                return 4;
            case 0x0F:
                address = (wr[3] << 4) | wr[2];
                ram[address] = (byte) ((ram[address] - 1) & 15);
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
                address = (wr[1] << 4) | wr[0];
                acc &= ram[address] & 15;
                pc++;
                return 4;
            case 0x1B:
                address = (wr[1] << 4) | wr[0];
                acc ^= ram[address] & 15;
                pc++;
                return 4;
            case 0x1C:
                address = (wr[1] << 4) | wr[0];
                acc |= ram[address] & 15;
                pc++;
                return 4;
            case 0x1D:
                address = (wr[1] << 4) | wr[0];
                ram[address] = (byte) ((ram[address] & 15) & acc);
                pc++;
                return 4;
            case 0x1E:
                address = (wr[1] << 4) | wr[0];
                ram[address] = (byte) ((ram[address] & 15) ^ acc);
                pc++;
                return 4;
            case 0x1F:
                address = (wr[1] << 4) | wr[0];
                ram[address] = (byte) ((ram[address] & 15) | acc);
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
                value = acc + (rom[(pc + 1) & 4095] & 15);
                carry = value > 15 ? 1 : 0;
                acc = value & 15;
                pc += 2;
                return 8;
            case 0x41:
                value = acc + ((~rom[(pc + 1) & 4095]) & 15) + 1;
                carry = value > 15 ? 1 : 0;
                acc = value & 15;
                pc += 2;
                return 8;
            case 0x42:
                acc &= rom[(pc + 1) & 4095] & 15;
                pc += 2;
                return 8;
            case 0x43:
                acc ^= rom[(pc + 1) & 4095] & 15;
                pc += 2;
                return 8;
            case 0x44:
                acc |= rom[(pc + 1) & 4095] & 15;
                pc += 2;
                return 8;
            case 0x45:
                pc += 2;
                return 8;
            case 0x46:
                wr[4] = rom[(pc + 1) & 4095] & 15;
                pc += 2;
                return 8;
            case 0x47:
                timerCounter = rom[(pc + 1) & 4095] & 255;
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
                address = (wr[1] << 4) | wr[0];
                value = rom[(pc & 0xFF00) | (acc << 4)
                        | (ram[address] & 15)] & 255;
                acc = value & 15;
                wr[4] = (value >> 4) & 15;
                return 8;
            case 0x4D:
                pc++;
                address = (wr[1] << 4) | wr[0];
                value = rom[(pc & 0xF000) | 0xF00 | (acc << 4)
                        | (ram[address] & 15)] & 255;
                acc = value & 15;
                wr[4] = (value >> 4) & 15;
                return 8;
            case 0x4E:
                pc++;
                value = rom[(pc & 0xFF00) | (acc << 4) | wr[4]] & 255;
                acc = value & 15;
                address = (wr[1] << 4) | wr[0];
                ram[address] = (byte) ((value >> 4) & 15);
                return 8;
            case 0x4F:
                pc++;
                value = rom[(pc & 0xF000) | 0xF00 | (acc << 4) | wr[4]] & 255;
                acc = value & 15;
                address = (wr[1] << 4) | wr[0];
                ram[address] = (byte) ((value >> 4) & 15);
                return 8;
            default:
                if (opcode >= 0x50 && opcode <= 0x5F) {
                    wr[0] = opcode & 15;
                    wr[1] = rom[(pc + 1) & 4095] & 15;
                    pc += 2;
                    return 8;
                }
                if (opcode >= 0x60 && opcode <= 0x6F) {
                    wr[2] = opcode & 15;
                    wr[3] = rom[(pc + 1) & 4095] & 15;
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

    byte[] lcdRam() {
        return ram;
    }

    boolean lcdEnabled() {
        return !halted && !resetLine;
    }

    int programCounter() {
        return pc & 4095;
    }
}
