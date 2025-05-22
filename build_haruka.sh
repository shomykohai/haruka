#!/bin/bash

only_sig_spoof=false
only_platform_spoof=false
android_jar=""
# Parse arguments
for arg in "$@"; do
    case $arg in
        -ss|--only-sig-spoof)
            only_sig_spoof=true
            shift
            ;;
        -ps|--only-platform-spoof)
            only_platform_spoof=true
            shift
            ;;
        --sdk)
            sdk_value="$2"
            shift 2
            ;;
        *)
            if [[ -f "$arg" ]]; then
                android_jar="$arg"
            fi
            ;;
    esac
done

# Check for android.jar
if [ ! -f "$android_jar" ]; then
    if [ -z "$sdk_value" ]; then
        echo "SDK value is not provided. Please provide --sdk <SDK number> or copy android.jar to the current directory."
        exit 1
    fi
    
    sdk_jar_path="sdk/android$sdk_value.jar"

    if [ ! -f "$sdk_jar_path" ]; then
        echo "android$sdk_value.jar not found at $sdk_jar_path. Please provide it or ensure it exists at that location."
        exit 1
    fi

    android_jar="$sdk_jar_path"
fi

# Setup build directories
rm -rf out/*
rm -rf build/*
mkdir -p out
mkdir -p build

echo Current SDK is $sdk

# Compile
if [ "$only_sig_spoof" = true ]; then
    echo "Compiling only signature spoofing files..."
    javac -cp "$android_jar" -Xlint:unchecked -d out \
        $(find src/lanchon/dexpatcher/annotation -name "Dex*.java") \
        src/sdk$sdk_value/com/android/server/pm/ComputerEngine.java \
        src/io/github/shomy/haruka/Haruka.java \
        src/io/github/shomy/haruka/HarukaPackageWrapper.java \
        src/io/github/shomy/haruka/HarukaSignatureSpoofingCore.java \
        # src/com/android/server/pm/ReconcilePackageUtils.java \
        # src/com/android/server/pm/InstallRequest.java \
else
    echo "Compiling all source files..."
    javac -cp "$android_jar" -d out \
        $(find src/io/github/shomy/haruka -name "*.java") \
        $(find src/lanchon/dexpatcher/annotation -name "Dex*.java") \
        $(find src/sdk$sdk_value -name "*.java") \

fi

# Build JAR and DEX
jar cvf build/haruka.jar -C ./out .
d2j-jar2dex build/haruka.jar -o build/haruka.dex
