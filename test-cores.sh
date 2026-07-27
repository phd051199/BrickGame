#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ASSETS=${1:-/Users/duypham/Developer/BrickEmuPy/assets}
OUT="$ROOT/build/test-cores"

rm -rf "$OUT"
mkdir -p "$OUT"

javac -d "$OUT" \
  "$ROOT/src/brickgame/BrickCpu.java" \
  "$ROOT/src/brickgame/MachineProfile.java" \
  "$ROOT/src/brickgame/Resources.java" \
  "$ROOT/src/brickgame/CommonLcdMap.java" \
  "$ROOT/src/brickgame/LayoutMetrics.java" \
  "$ROOT/src/brickgame/Ht943Cpu.java" \
  "$ROOT/src/brickgame/Spl0XCpu.java" \
  "$ROOT/src/brickgame/Em73000Cpu.java" \
  "$ROOT/src/brickgame/E0C6200Cpu.java" \
  "$ROOT/src/brickgame/Ks56Cpu.java" \
  "$ROOT/test/brickgame/Ht943CoreSmokeTest.java" \
  "$ROOT/test/brickgame/Spl0XCoreSmokeTest.java" \
  "$ROOT/test/brickgame/Em73000CoreSmokeTest.java" \
  "$ROOT/test/brickgame/E0C6200CoreSmokeTest.java" \
  "$ROOT/test/brickgame/Ks56CoreSmokeTest.java" \
  "$ROOT/test/brickgame/InputWiringSmokeTest.java" \
  "$ROOT/test/brickgame/CommonMapSmokeTest.java" \
  "$ROOT/test/brickgame/CommonLayoutBoundsTest.java"

java -cp "$OUT" brickgame.Ht943CoreSmokeTest "$ASSETS"
java -cp "$OUT" brickgame.Spl0XCoreSmokeTest "$ASSETS"
java -cp "$OUT" brickgame.Em73000CoreSmokeTest "$ASSETS"
java -cp "$OUT" brickgame.E0C6200CoreSmokeTest "$ASSETS"
java -cp "$OUT" brickgame.Ks56CoreSmokeTest "$ASSETS"
java -cp "$OUT" brickgame.InputWiringSmokeTest "$ASSETS"
java -cp "$OUT:$ROOT/dist/BrickGame.jar" brickgame.CommonMapSmokeTest
java -cp "$OUT" brickgame.CommonLayoutBoundsTest
