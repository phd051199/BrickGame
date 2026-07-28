# E23 assets

The four `e23-assets.part*` files form one validated base64 archive used by `build-e23.sh`.

The build extracts only:

- E23 program ROM
- E23 LCD map
- regular bitmap font
- bold bitmap font

No other ROM, LCD layout or CPU asset is stored in the runtime bundle.
