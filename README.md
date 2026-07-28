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
- Numeric keypad and directional-key input
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

- Left/right/down: `4/6/8` or D-pad
- Rotate/action: `2` or `5`, D-pad up or center
- Start: `1`
- Auxiliary: `3`
- Option: `7`
- Reset: `*`
- Pause: `0`, `#` or left softkey
- Exit: right softkey

## Packaged resources

- `e23/program.bin`
- `e23/display.map`
- `e23/regular.bmf`
- `e23/bold.bmf`
