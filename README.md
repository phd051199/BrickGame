# BrickGame J2ME

CLDC 1.0 / MIDP 2.0 Brick Game collection for Nokia phones, optimized for
`320×240` landscape and `240×320` portrait displays.

> **Forked from** [`vitalibo/Brick-Game-9999-in-1`](https://github.com/vitalibo/Brick-Game-9999-in-1)
> by Vitaliy Boyarsky — the original desktop *Brick Game 9999-in-1*, ported to
> CLDC 1.0 / MIDP 2.0 for Nokia feature phones.

## Programs

The selector contains twenty independent programs:

| Code | Program | Code | Program |
|---|---|---|---|
| A-01 | Tanks | K-11 | Snake |
| B-02 | Breakout | L-12 | Frogger |
| C-03 | Double Breakout | M-13 | Match |
| D-04 | Wall Ball | N-14 | Tetris |
| E-05 | Race | O-15 | Pong |
| F-06 | Highway | P-16 | Dodge |
| G-07 | Tunnel | Q-17 | Pinball |
| H-08 | Shoot | R-18 | Maze |
| I-09 | Stack Shoot | S-19 | Bomber |
| J-10 | Invaders | T-20 | Pentris |

## Controls

- D-pad or `2`, `4`, `6`, `8`: move or select.
- Fire or `5`: start, rotate, shoot or game-specific action.
- `0`: pause/resume.
- `#`: return to program selector.

Input repeat is generated internally so movement timing is stable across Nokia
models. Tanks limits every tank to one active projectile and applies a shot
cooldown.

## Source structure

The physical source tree is grouped by responsibility while all classes remain
in package `brickgame` to avoid public-accessor overhead on CLDC:

```text
src/brickgame/
├── app/                 MIDlet, Canvas and frame loop
├── core/                engine, menu, catalog, preview, snapshot and pools
├── data/                Snake map loader
├── ui/                  bitmap fonts, adaptive layout and LCD renderer
└── games/
    ├── action/          Tanks, Shoot, Stack Shoot, Invaders, Bomber
    ├── block/           Breakout, Wall Ball, Match, Tetris/Pentris
    ├── classic/         Snake, Frogger, Pong, Pinball, Maze
    └── driving/         Race, Highway, Tunnel, Dodge
```

Adding a program is localized to:

1. A game class under the appropriate `games/` group.
2. Metadata/factory entry in `core/GameCatalog.java`.
3. A representative selector snapshot in `core/GamePreview.java`.

The board uses twenty `short` row masks and the 4×4 runtime preview uses four
`byte` row masks. Snapshots are copied and frames are redrawn only when the
revision changes. Static LCD grids and labels are cached in a mutable MIDP image
when memory permits. Gameplay and selector hot paths use fixed buffers instead
of allocating temporary arrays.

## CLDC 1.0 build

```sh
cd /Users/duypham/Developer/BrickGame
sh build-j2me.sh
```

Outputs:

- `dist/BrickGame.jar`
- `dist/BrickGame.jad`

Both manifest and JAD declare:

```text
MicroEdition-Configuration: CLDC-1.0
MicroEdition-Profile: MIDP-2.0
```

The local SDK only provides a CLDC 1.1 API jar, so the default build uses it as
a compile-time superset and then applies a strict CLDC 1.0 guard. The build
fails on:

- Float/double bytecode.
- `Float`, `Double`, reference or math classes unavailable in CLDC 1.0.
- Java classes outside the explicit CLDC 1.0 whitelist.
- Old-KVM-incompatible class literals.

A real CLDC 1.0 API jar can be supplied directly:

```sh
CLDC_JAR=/path/to/cldc_1.0.jar \
MIDP_JAR=/path/to/midp_2.0.jar \
PROGUARD_JAR=/path/to/proguard.jar \
sh build-j2me.sh
```

ProGuard converts the classes to Java 1.1 format and writes CLDC `StackMap`
attributes.

## Tests

```sh
rm -rf build/test
mkdir -p build/test
javac --release 8 -encoding UTF-8 \
  -classpath /Users/duypham/Developer/MIDPlay/lib/cldc_1.1.jar:/Users/duypham/Developer/MIDPlay/lib/midp_2.0.jar \
  -d build/test $(find src test -name '*.java' -print)
java -cp build/test:resources brickgame.EngineSmokeTest
java -cp build/test:resources brickgame.LongRunSmokeTest
java -cp build/test:resources brickgame.LayoutBoundsTest
java -cp build/test:resources brickgame.PreviewRegressionTest
```
