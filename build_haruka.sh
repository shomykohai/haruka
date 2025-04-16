if [ -z "$1" ] && [ ! -f "android.jar" ]; then
    echo "Please, copy android.jar to the current directory or call $0 path/to/android.jar"
    exit 1
fi

android_jar="android.jar"
if [ -n "$1" ]; then
    android_jar=$1
fi


mkdir -p out
mkdir -p build
rm -rf out/*
rm -rf build/*
javac -cp $android_jar -d out $(find src -name "*.java")
jar cvf build/haruka.jar -C ./out .
d2j-jar2dex build/haruka.jar -o build/haruka.dex
