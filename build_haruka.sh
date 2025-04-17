#!/bin/bash

only_sig_spoof=false
android_jar="android.jar"

# Parse arguments
for arg in "$@"; do
    case $arg in
        -ss|--only-sig-spoof)
            only_sig_spoof=true
            shift
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
    echo "Please, copy android.jar to the current directory or call $0 path/to/android.jar"
    exit 1
fi

# Setup build directories
mkdir -p out
mkdir -p build
rm -rf out/*
rm -rf build/*

# Compile
if [ "$only_sig_spoof" = true ]; then
    echo "Compiling only signature spoofing files..."
    javac -cp "$android_jar" -d out \
        $(find src/lanchon/dexpatcher/annotation -name "Dex*.java") \
        src/com/android/server/pm/ComputerEngine.java \
        src/io/github/shomy/haruka/Haruka.java
else
    echo "Compiling all source files..."
    javac -cp "$android_jar" -d out $(find src -name "*.java")
fi

# Build JAR and DEX
jar cvf build/haruka.jar -C ./out .
d2j-jar2dex build/haruka.jar -o build/haruka.dex
