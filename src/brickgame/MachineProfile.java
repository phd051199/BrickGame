package brickgame;

final class MachineProfile {
    static final int CORE_HT943 = 0;
    static final int CORE_SPL02 = 1;
    static final int CORE_SPL03 = 2;
    static final int CORE_EM73000 = 3;
    static final int CORE_E0C6200 = 4;
    static final int CORE_KS56 = 5;

    static final int PORT_PP = 0;
    static final int PORT_PM = 1;
    static final int PORT_PS = 2;
    static final int PORT_RESET = 3;

    static final int BUTTON_LEFT = 0;
    static final int BUTTON_RIGHT = 1;
    static final int BUTTON_DOWN = 2;
    static final int BUTTON_ROTATE = 3;
    static final int BUTTON_START = 4;
    static final int BUTTON_AUX = 5;
    static final int BUTTON_OPTION = 6;
    static final int BUTTON_RESET = 7;
    static final int BUTTON_COUNT = 8;

    final int coreType;
    final String id;
    final String name;
    final int clockHz;
    final int boardColumns;
    final int boardRows;
    final int boardColumnOffset;
    final int boardRowOffset;

    final int timerDiv;
    final int pullupPP;
    final int pullupPM;
    final int pullupPS;
    final int wakeupPP;
    final int wakeupPM;
    final int wakeupPS;
    final byte[] buttonPorts;
    final byte[] buttonPins;

    final int nonCrystalDiv;
    final int pullupPA;
    final int pullupPB;
    final int powerKeyPA;
    final int powerKeyPB;

    private MachineProfile(String id, String name, int clockHz,
            int boardColumns, int boardRows, int timerDiv,
            int pullupPP, int pullupPM, int pullupPS,
            int wakeupPP, int wakeupPM, int wakeupPS,
            byte[] buttonPorts, byte[] buttonPins) {
        coreType = CORE_HT943;
        this.id = id;
        this.name = name;
        this.clockHz = clockHz;
        this.boardColumns = boardColumns;
        this.boardRows = boardRows;
        boardColumnOffset = (10 - boardColumns) / 2;
        boardRowOffset = (20 - boardRows) / 2;
        this.timerDiv = timerDiv;
        this.pullupPP = pullupPP;
        this.pullupPM = pullupPM;
        this.pullupPS = pullupPS;
        this.wakeupPP = wakeupPP;
        this.wakeupPM = wakeupPM;
        this.wakeupPS = wakeupPS;
        this.buttonPorts = buttonPorts;
        this.buttonPins = buttonPins;
        nonCrystalDiv = 0;
        pullupPA = 0;
        pullupPB = 0;
        powerKeyPA = 0;
        powerKeyPB = 0;
    }

    private MachineProfile(int coreType, String id, String name, int clockHz,
            int boardColumns, int boardRows, int nonCrystalDiv,
            int pullupPA, int pullupPB, int powerKeyPA, int powerKeyPB) {
        this.coreType = coreType;
        this.id = id;
        this.name = name;
        this.clockHz = clockHz;
        this.boardColumns = boardColumns;
        this.boardRows = boardRows;
        boardColumnOffset = (10 - boardColumns) / 2;
        boardRowOffset = (20 - boardRows) / 2;
        this.nonCrystalDiv = nonCrystalDiv;
        this.pullupPA = pullupPA;
        this.pullupPB = pullupPB;
        this.powerKeyPA = powerKeyPA;
        this.powerKeyPB = powerKeyPB;
        timerDiv = 0;
        pullupPP = 0;
        pullupPM = 0;
        pullupPS = 0;
        wakeupPP = 0;
        wakeupPM = 0;
        wakeupPS = 0;
        buttonPorts = null;
        buttonPins = null;
    }

    String romPath() {
        return resourcePath(".bin");
    }

    String mapPath() {
        return resourcePath(".map");
    }

    private String resourcePath(String suffix) {
        StringBuffer buffer = new StringBuffer("/brickrom/");
        buffer.append(id);
        buffer.append(suffix);
        return buffer.toString();
    }

    private static byte[] standardPorts() {
        return new byte[] {
            PORT_PP, PORT_PP, PORT_PP, PORT_PP,
            PORT_PS, PORT_PS, PORT_PS, PORT_RESET
        };
    }

    private static byte[] standardPins() {
        return new byte[] {3, 2, 1, 0, 0, 2, 1, 0};
    }

    private static byte[] pmPorts() {
        return new byte[] {
            PORT_PM, PORT_PM, PORT_PM, PORT_PM,
            PORT_PS, PORT_RESET, PORT_PM, PORT_RESET
        };
    }

    private static byte[] ga888Pins() {
        return new byte[] {3, 2, 1, 0, 0, 0, 2, 0};
    }

    static final MachineProfile[] ALL = {
        new MachineProfile(
            "e23", "E-23 96 in 1", 1000000, 10, 20, 16,
            15, 15, 15, 0, 0, 4, standardPorts(), standardPins()),
        new MachineProfile(
            "e88", "E-88 8 in 1", 1000000, 10, 20, 16,
            15, 15, 15, 0, 0, 4, standardPorts(), standardPins()),
        new MachineProfile(
            "ga888", "GA888", 1000000, 8, 12, 16,
            15, 15, 15, 0, 0, 1, pmPorts(), ga888Pins()),
        new MachineProfile(
            "key55", "Keychain 55 in 1", 512000, 8, 12, 8,
            15, 15, 15, 0, 0, 1, pmPorts(), ga888Pins()),
        new MachineProfile(
            CORE_SPL02, "apollo126", "Apollo 126 in 1", 690000,
            10, 20, 16, 0, 0, 1, 0),
        new MachineProfile(
            CORE_SPL03, "apollo18", "Apollo 18 in 1", 690000,
            10, 16, 16, 0, 0, 1, 0),
        new MachineProfile(
            CORE_EM73000, "e33", "E-33 2 in 1", 2000000,
            10, 20, 0, 0, 0, 0, 0),
        new MachineProfile(
            CORE_E0C6200, "stack", "Stack Challenge", 2000000,
            9, 20, 0, 0, 0, 0, 0),
        new MachineProfile(
            CORE_KS56, "ga878", "GA878", 1000000,
            8, 11, 32, 0, 0, 0, 0),
        new MachineProfile(
            CORE_KS56, "micon", "Micon KC-32", 1000000,
            8, 14, 32, 0, 0, 0, 0)
    };

    private MachineProfile() {
        this(null, null, 0, 10, 20, 0, 0, 0, 0, 0, 0, 0, null, null);
    }
}
