#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BUILD_DIR="$ROOT/build/j2me"
DIST_DIR="$ROOT/dist"
CLASSES_DIR="$BUILD_DIR/classes"
CORE_JAR="$BUILD_DIR/BrickGame-core.jar"
FINAL_JAR="$DIST_DIR/BrickGame.jar"
FINAL_JAD="$DIST_DIR/BrickGame.jad"

CLDC_JAR=${CLDC_JAR:-/Users/duypham/Developer/MIDPlay/lib/cldc_1.1.jar}
MIDP_JAR=${MIDP_JAR:-/Users/duypham/Developer/MIDPlay/lib/midp_2.0.jar}
PROGUARD_JAR=${PROGUARD_JAR:-/Users/duypham/Developer/MIDPlay/lib/proguard-ant.jar}

for file in "$CLDC_JAR" "$MIDP_JAR" "$PROGUARD_JAR"; do
    if [ ! -f "$file" ]; then
        echo "Missing build dependency: $file" >&2
        exit 1
    fi
done

rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR" "$DIST_DIR"

# Modern javac is used only as a parser/compiler. ProGuard below converts the
# output to Java 1.1 bytecode and writes CLDC StackMap attributes.
javac --release 8 -encoding UTF-8 \
    -classpath "$CLDC_JAR:$MIDP_JAR" \
    -d "$CLASSES_DIR" \
    $(find "$ROOT/src" -name '*.java' -print)

java -cp "$PROGUARD_JAR" proguard.ProGuard \
    @"$ROOT/proguard-j2me.pro" \
    -injars "$CLASSES_DIR" \
    -outjars "$CORE_JAR" \
    -libraryjars "$CLDC_JAR" \
    -libraryjars "$MIDP_JAR(!java/io/ByteArrayOutputStream.class)"

if [ -d "$ROOT/resources" ]; then
    jar uf "$CORE_JAR" -C "$ROOT/resources" .
fi

cp "$CORE_JAR" "$FINAL_JAR"
jar ufm "$FINAL_JAR" "$ROOT/manifest.mf"

# Old KVM/MicroEmulator verifiers reject ldc CONSTANT_Class even when the
# class file has been downgraded to Java 1.1. Resource loading must use
# getClass() rather than Foo.class.
for class_file in $(jar tf "$FINAL_JAR" | grep '^brickgame/.*\.class$'); do
    class_name=$(echo "$class_file" | tr '/' '.' | sed 's/\.class$//')
    if javap -classpath "$FINAL_JAR" -c -p "$class_name" \
        | grep -q 'ldc .*// class'; then
        echo "Unsupported CLDC class literal in $class_name" >&2
        exit 1
    fi
done

JAR_SIZE=$(wc -c < "$FINAL_JAR" | tr -d ' ')
cat > "$FINAL_JAD" <<EOF
MIDlet-Name: BrickGame
MIDlet-Version: 1.0.0
MIDlet-Vendor: BrickGame Port
MIDlet-1: Brick Game, , brickgame.Midlet
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.0
MIDlet-Jar-URL: BrickGame.jar
MIDlet-Jar-Size: $JAR_SIZE
EOF

echo "Built: $FINAL_JAR"
echo "Built: $FINAL_JAD"
