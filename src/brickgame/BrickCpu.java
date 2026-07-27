package brickgame;

interface BrickCpu {
    int runCycles(int budget);

    void setButton(int button, boolean down);

    byte[] vram();

    boolean displayEnabled();

    int programCounter();
}
