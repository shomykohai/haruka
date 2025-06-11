#!/bin/bash

only_sig_spoof=false
only_platform_spoof=false
sdk_range=()  # Leave empty to build all SDKs found in sdk/

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        -ss|--only-sig-spoof)
            only_sig_spoof=true
            shift
            ;;
        -ps|--only-platform-spoof)
            only_platform_spoof=true
            shift
            ;;
        --sdk)
            sdk_range+=("$2")
            shift 2
            ;;
        *)
            echo "Unknown argument: $1"
            exit 1
            ;;
    esac
done

# If no SDKs were explicitly passed, build all found
if [[ ${#sdk_range[@]} -eq 0 ]]; then
    for sdk_file in sdk/android*.jar; do
        sdk_num=$(basename "$sdk_file" | grep -oP '\d+')
        sdk_range+=("$sdk_num")
    done
fi

# Cleanup and prepare
rm -rf out build
mkdir -p out build

echo "[i] Starting build process..."
echo "Compiling haruka core"
mkdir -p out/haruka_core
javac -d out/haruka_core \
    $(find src/lanchon/dexpatcher/annotation -name "Dex*.java") \
    src/io/github/shomy/haruka/Haruka.java \
    src/io/github/shomy/haruka/Reflector.java

jar cvf build/haruka_core.jar -C out/haruka_core .
d2j-jar2dex build/haruka_core.jar -o build/haruka_core.dex
echo "[i] haruka core compiled successfully."

# Loop through each SDK and compile
for sdk_value in "${sdk_range[@]}"; do
    android_jar="sdk/android$sdk_value.jar"

    if [[ ! -f "$android_jar" ]]; then
        echo "Skipping SDK $sdk_value: android$sdk_value.jar not found at $android_jar"
        continue
    fi

    echo "[i] Building for SDK $sdk_value..."

    rm -rf out/*
    mkdir -p out

    if [[ "$only_sig_spoof" == true ]]; then
        echo "Compiling only signature spoofing files for SDK $sdk_value..."
        javac -cp "$android_jar:build/haruka_core.jar" -Xlint:unchecked -d out/ \
            $(find src/lanchon/dexpatcher/annotation -name "Dex*.java") \
            src/sdk$sdk_value/com/android/server/pm/ComputerEngine.java \
            src/io/github/shomy/haruka/HarukaPackageWrapper.java \
            src/io/github/shomy/haruka/HarukaSignatureSpoofingCore.java
    elif [[ "$only_platform_spoof" == true ]]; then
        echo "Compiling only platform spoofing files for SDK $sdk_value..."
        javac -cp "$android_jar" -Xlint:unchecked -d out \
            $(find src/lanchon/dexpatcher/annotation -name "Dex*.java") \
            $(find src/sdk$sdk_value -name "*.java") \
            src/io/github/shomy/haruka/HarukaPlatformSpoofingCore.java
    else
        echo "Compiling all source files for SDK $sdk_value..."
        javac -cp "$android_jar" -Xlint:unchecked -d out \
            $(find src/io/github/shomy/haruka -name "*.java") \
            $(find src/lanchon/dexpatcher/annotation -name "Dex*.java") \
            $(find src/sdk$sdk_value -name "*.java")
    fi

    jar_name="build/haruka_${sdk_value}.jar"
    dex_name="build/haruka_${sdk_value}.dex"
    
    jar cvf "$jar_name" -C ./out .
    d2j-jar2dex "$jar_name" -o "$dex_name"

    echo "[i] Built finished for SDK $sdk_value -> $jar_name and $dex_name"
done
