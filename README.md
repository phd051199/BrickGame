# E23 96 in 1

Java ME implementation dedicated to the E-23 96-in-1 handheld for 320×240 landscape and 240×320 portrait devices.

## Runtime

- Boots directly into E23
- HT943-compatible E23 CPU implementation only
- Responsive 320×240 landscape and 240×320 portrait renderer
- Portrait board fills the full 320-pixel height with 16-pixel cells
- Portrait mode removes BAT/TIME/UP and uses the side panel for advanced key hints
- Preview pieces are vertically centered inside the 4×4 preview grid
- E23 LCD map and HUD decoder only
- QWERTY and directional-key input
- No audio
- No ROM selector or multi-core abstraction

## Build

```sh
cd /Users/duypham/Developer/BrickGame
sh build-e23.sh
```

Outputs:

```text
dist/E23.jar
dist/E23.jad
```

## Controls

- Left/right/down: D-pad or `A/D/S`
- Rotate/action: D-pad up, Enter, Space or `W`
- Start: `1` or `P`
- Auxiliary: `2` or `O`
- Option: `3` or `M`
- Reset: `*` or `R`
- Pause: left softkey or `#`
- Exit: right softkey

## Packaged resources

- `e23/program.bin`
- `e23/display.map`
- `e23/regular.bmf`
- `e23/bold.bmf`
