# BrickGame J2ME ROM Emulator

Java ME Brick Game emulator for Nokia E72 and other 320×240 landscape devices.

## Runtime

- 10 original program ROMs
- HT943, SPL02, SPL03, EM73000, E0C6200 and KS56 cores
- QWERTY and directional-key input
- Shared sharp Brick Game renderer
- SCORE, SPEED and LEVEL decoded from LCD RAM
- Self-contained vendored assets

The CPU, timers, interrupts, input ports and LCD RAM behavior are ported from the BrickEmuPy reference implementation. The MIDlet uses a compact custom display instead of drawing the original device face.

The playfield keeps each machine's native grid size. Twenty-row machines use nearly the full 240-pixel height. Machines with fewer rows use larger centered cells while preserving the right information panel. The inner marker of every inactive cell is centered and one pixel smaller than the previous build.

## Build

```sh
cd /Users/duypham/Developer/BrickGame
sh build-j2me.sh
```

Outputs:

```text
dist/BrickGame.jar
dist/BrickGame.jad
```

The repository contains every resource required by the build. No file is copied from BrickEmuPy, another repository or an older output JAR.

## Controls

- Left/right/down: D-pad or `A/D/S`
- Rotate/action: D-pad up, Enter, Space or `W`
- Start/pause input: `1` or `P`
- Auxiliary input: `2` or `O`
- Option input: `3` or `M`
- Reset input: `*` or `R`
- Emulator pause: left softkey or `#`
- ROM list: right softkey

## Verification

```sh
sh test-cores.sh /Users/duypham/Developer/BrickEmuPy/assets
```

The checks compare all CPU families and representative input wiring with the Python reference. They also validate all ten board maps and the native-height 320×240 layouts.

## Packaged resources

The final MIDlet contains:

- 10 `.bin` ROM files;
- 10 compact `.map` files;
- two bitmap UI fonts.
