#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BUILD_DIR="$ROOT/build/e23"
DIST_DIR="$ROOT/dist"
CLASSES_DIR="$BUILD_DIR/classes"
RESOURCE_DIR="$BUILD_DIR/resources"
CORE_JAR="$BUILD_DIR/E23-core.jar"
FINAL_JAR="$DIST_DIR/E23.jar"
FINAL_JAD="$DIST_DIR/E23.jad"
CLDC_JAR=${CLDC_JAR:-$ROOT/lib/cldc_1.0.jar}
MIDP_JAR=${MIDP_JAR:-$ROOT/lib/midp_2.0.jar}
PROGUARD_JAR=${PROGUARD_JAR:-$ROOT/lib/proguard-ant.jar}
ASSET_DIR="$ROOT/assets/e23"
ICON_ASSET="$ROOT/assets/e23-icon.png"

for file in "$CLDC_JAR" "$MIDP_JAR" "$PROGUARD_JAR" "$ICON_ASSET" \
            "$ASSET_DIR/program.bin" "$ASSET_DIR/display.map" \
            "$ASSET_DIR/regular.bmf" "$ASSET_DIR/bold.bmf"; do
    if [ ! -f "$file" ]; then
        echo "Missing build dependency: $file" >&2
        exit 1
    fi
done

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$RESOURCE_DIR/e23" "$DIST_DIR"

cp "$ASSET_DIR/program.bin" "$ASSET_DIR/display.map" \
   "$ASSET_DIR/regular.bmf" "$ASSET_DIR/bold.bmf" "$RESOURCE_DIR/e23/"
cp "$ICON_ASSET" "$RESOURCE_DIR/e23-icon.png"

SOURCE_FILES=$(find "$ROOT/src" -name '*.java' -type f -size +0c -print)

javac --release 8 -encoding UTF-8 \
    -classpath "$CLDC_JAR:$MIDP_JAR" \
    -d "$CLASSES_DIR" \
    $SOURCE_FILES

java -cp "$PROGUARD_JAR" proguard.ProGuard \
    @"$ROOT/proguard-e23.pro" \
    -injars "$CLASSES_DIR" \
    -outjars "$CORE_JAR" \
    -libraryjars "$CLDC_JAR" \
    -libraryjars "$MIDP_JAR(!java/io/ByteArrayOutputStream.class)"

(
    cd "$RESOURCE_DIR"
    jar uf "$CORE_JAR" \
        e23/program.bin \
        e23/display.map \
        e23/regular.bmf \
        e23/bold.bmf \
        e23-icon.png
)

cp "$CORE_JAR" "$FINAL_JAR"
jar ufm "$FINAL_JAR" "$ROOT/manifest.mf"

for class_file in $(jar tf "$FINAL_JAR" | grep '^e23/.*\.class$'); do
    class_name=$(echo "$class_file" | tr '/' '.' | sed 's/\.class$//')
    bytecode=$(javap -classpath "$FINAL_JAR" -c -p "$class_name" 2>/dev/null || true)
    if echo "$bytecode" | grep -q 'ldc .*// class'; then
        echo "Unsupported CLDC class literal in $class_name" >&2
        exit 1
    fi
done

JAR_SIZE=$(wc -c < "$FINAL_JAR" | tr -d ' ')
cat > "$FINAL_JAD" <<EOF
MIDlet-Name: E23 96 in 1
MIDlet-Version: 3.2.7
MIDlet-Vendor: Duy Pham
MIDlet-1: E23 96 in 1, /e23-icon.png, e23.E23Midlet
MIDlet-Icon: /e23-icon.png
MicroEdition-Configuration: CLDC-1.0
MicroEdition-Profile: MIDP-2.0
MIDlet-Jar-URL: E23.jar
MIDlet-Jar-Size: $JAR_SIZE
EOF

echo "Built $FINAL_JAR"
echo "Built $FINAL_JAD"
