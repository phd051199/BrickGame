#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BUILD_DIR="$ROOT/build/j2me"
DIST_DIR="$ROOT/dist"
CLASSES_DIR="$BUILD_DIR/classes"
RESOURCE_DIR="$BUILD_DIR/resources"
CORE_JAR="$BUILD_DIR/BrickGame-core.jar"
FINAL_JAR="$DIST_DIR/BrickGame.jar"
FINAL_JAD="$DIST_DIR/BrickGame.jad"
ASSET_PARTS="$ROOT/assets/runtime-assets.b64.part1 $ROOT/assets/runtime-assets.b64.part2 $ROOT/assets/runtime-assets.b64.part3 $ROOT/assets/runtime-assets.b64.part4 $ROOT/assets/runtime-assets.b64.part5 $ROOT/assets/runtime-assets.b64.part6"

CLDC_JAR=${CLDC_JAR:-$ROOT/lib/cldc_1.1.jar}
MIDP_JAR=${MIDP_JAR:-$ROOT/lib/midp_2.0.jar}
PROGUARD_JAR=${PROGUARD_JAR:-$ROOT/lib/proguard-ant.jar}

for file in "$CLDC_JAR" "$MIDP_JAR" "$PROGUARD_JAR" $ASSET_PARTS; do
    if [ ! -f "$file" ]; then
        echo "Missing build dependency: $file" >&2
        exit 1
    fi
done

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$RESOURCE_DIR" "$DIST_DIR"

python3 "$ROOT/tools/extract_vendored_assets.py" \
    --parts $ASSET_PARTS \
    --output "$RESOURCE_DIR"

SOURCE_FILES=$(find "$ROOT/src" -name '*.java' -type f -size +0c -print)

javac --release 8 -encoding UTF-8 \
    -classpath "$CLDC_JAR:$MIDP_JAR" \
    -d "$CLASSES_DIR" \
    $SOURCE_FILES

java -cp "$PROGUARD_JAR" proguard.ProGuard \
    @"$ROOT/proguard-j2me.pro" \
    -injars "$CLASSES_DIR" \
    -outjars "$CORE_JAR" \
    -libraryjars "$CLDC_JAR" \
    -libraryjars "$MIDP_JAR(!java/io/ByteArrayOutputStream.class)"

(
    cd "$RESOURCE_DIR"
    RESOURCE_FILES=$(find brickrom ui -type f \
        \( -name '*.bin' -o -name '*.map' -o -name '*.bmf' \) \
        -print | sort)
    jar uf "$CORE_JAR" $RESOURCE_FILES
)
cp "$CORE_JAR" "$FINAL_JAR"
jar ufm "$FINAL_JAR" "$ROOT/manifest.mf"

for class_file in $(jar tf "$FINAL_JAR" | grep '^brickgame/.*\.class$'); do
    class_name=$(echo "$class_file" | tr '/' '.' | sed 's/\.class$//')
    bytecode=$(javap -classpath "$FINAL_JAR" -c -p "$class_name" 2>/dev/null || true)
    if echo "$bytecode" | grep -q 'ldc .*// class'; then
        echo "Unsupported CLDC class literal in $class_name" >&2
        exit 1
    fi
done

JAR_SIZE=$(wc -c < "$FINAL_JAR" | tr -d ' ')
cat > "$FINAL_JAD" <<EOF
MIDlet-Name: BrickGame
MIDlet-Version: 2.6.1
MIDlet-Vendor: Duy Pham
MIDlet-1: Brick ROM Emulator, , brickgame.Midlet
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.0
MIDlet-Jar-URL: BrickGame.jar
MIDlet-Jar-Size: $JAR_SIZE
Nokia-MIDlet-App-Orientation: landscape
EOF

echo "Built CLDC 1.1 Brick renderer emulator: $FINAL_JAR"
echo "Built CLDC 1.1 Brick renderer emulator: $FINAL_JAD"
echo "Bundled assets: 10 ROMs / 10 board maps / bitmap UI"
