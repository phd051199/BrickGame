# Runtime assets

The `runtime-assets.b64.part*` files form one validated base64-encoded archive used by `build-j2me.sh`.

The archive contains:

- 10 program ROMs;
- 10 compact playfield maps;
- two bitmap UI fonts;
- source LCD layouts retained as vendored reference data.

The MIDlet packages only the ROMs, playfield maps and bitmap fonts. The build does not read from another repository or from an older distribution JAR.
