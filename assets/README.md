# E23 assets

Plain binary assets used by `build-e23.sh`:

- `e23/program.bin` — E23 program ROM
- `e23/display.map` — E23 LCD map
- `e23/regular.bmf` — regular bitmap font
- `e23/bold.bmf` — bold bitmap font
- `e23-icon.png` — 48×48 launcher icon (opaque 256-color palette PNG)

`icon.png` is the 512×512 master artwork; `e23-icon.png` is generated from it
(LANCZOS downscale, MAXCOVERAGE 256-color quantize, oxipng). Regenerate with:

    python3 -c "from PIL import Image; Image.open('assets/icon.png').convert('RGBA').resize((48,48),Image.LANCZOS).convert('RGB').quantize(256,1,dither=0).save('assets/e23-icon.png')"
    oxipng -o4 --strip safe assets/e23-icon.png

No other ROM, LCD layout or CPU asset is stored in the runtime bundle.
