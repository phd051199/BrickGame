#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BUILD_DIR="$ROOT/build/j2me"
DIST_DIR="$ROOT/dist"
CLASSES_DIR="$BUILD_DIR/classes"
CORE_JAR="$BUILD_DIR/BrickGame-core.jar"
FINAL_JAR="$DIST_DIR/BrickGame.jar"
FINAL_JAD="$DIST_DIR/BrickGame.jad"
API_LIST="$BUILD_DIR/java-api.txt"

# A real CLDC 1.0 API jar can be supplied here. The locally installed 1.1 jar
# is a compile-time superset; the strict guard below rejects every API/opcode
# used by this MIDlet that is not part of the CLDC 1.0 subset.
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

if [ -d "$ROOT/resources/game" ]; then
    jar uf "$CORE_JAR" -C "$ROOT/resources" game
fi
if [ -d "$ROOT/resources/ui" ]; then
    jar uf "$CORE_JAR" -C "$ROOT/resources" ui
fi

cp "$CORE_JAR" "$FINAL_JAR"
jar ufm "$FINAL_JAR" "$ROOT/manifest.mf"

# Old KVM/MicroEmulator verifiers reject ldc CONSTANT_Class even after the
# bytecode is lowered to Java 1.1.
for class_file in $(jar tf "$FINAL_JAR" | grep '^brickgame/.*\.class$'); do
    class_name=$(echo "$class_file" | tr '/' '.' | sed 's/\.class$//')
    bytecode=$(javap -classpath "$FINAL_JAR" -c -p "$class_name" 2>/dev/null || true)
    if echo "$bytecode" | grep -q 'ldc .*// class'; then
        echo "Unsupported CLDC class literal in $class_name" >&2
        exit 1
    fi
    if echo "$bytecode" | grep -Eq '\b(fadd|fsub|fmul|fdiv|frem|fneg|freturn|dadd|dsub|dmul|ddiv|drem|dneg|dreturn|i2f|i2d|l2f|l2d|f2i|f2l|f2d|d2i|d2l|d2f)\b'; then
        echo "Floating-point bytecode is not allowed by CLDC 1.0: $class_name" >&2
        exit 1
    fi
done

if javap -classpath "$FINAL_JAR" -verbose brickgame.Midlet 2>/dev/null \
    | grep -Eq 'java/lang/(Float|Double)|java/lang/ref/|java/math/'; then
    echo "CLDC 1.1-only class reference found" >&2
    exit 1
fi

# Exact java.* classes currently required by this MIDlet and available in
# CLDC 1.0. Any accidental use of a wider J2SE/CLDC 1.1 API fails the build.
jdeps -verbose:class "$FINAL_JAR" 2>/dev/null \
    | awk '/ -> java\.(lang|util|io)\./ {print $3}' \
    | sort -u > "$API_LIST" || true
ALLOWED_JAVA_CLASSES="
java.io.ByteArrayOutputStream
java.io.IOException
java.io.InputStream
java.lang.Class
java.lang.IllegalArgumentException
java.lang.IllegalStateException
java.lang.InterruptedException
java.lang.Object
java.lang.Runnable
java.lang.String
java.lang.StringBuffer
java.lang.System
java.lang.Thread
java.lang.Throwable
java.util.Calendar
java.util.Random
"
while IFS= read -r class_name; do
    [ -z "$class_name" ] && continue
    if ! printf '%s\n' "$ALLOWED_JAVA_CLASSES" | grep -qx "$class_name"; then
        echo "Java API outside the CLDC 1.0 whitelist: $class_name" >&2
        exit 1
    fi
done < "$API_LIST"

JAR_SIZE=$(wc -c < "$FINAL_JAR" | tr -d ' ')
cat > "$FINAL_JAD" <<EOF
MIDlet-Name: BrickGame
MIDlet-Version: 1.0.0
MIDlet-Vendor: Duy Pham
MIDlet-1: Brick Game, , brickgame.Midlet
MicroEdition-Configuration: CLDC-1.0
MicroEdition-Profile: MIDP-2.0
MIDlet-Jar-URL: BrickGame.jar
MIDlet-Jar-Size: $JAR_SIZE
EOF

echo "Built CLDC 1.0: $FINAL_JAR"
echo "Built CLDC 1.0: $FINAL_JAD"
