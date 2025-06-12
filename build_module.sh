#!/bin/bash

echo "[i] Compiling Haruka (Signature Spoofing only)..."
./build_haruka.sh -ss


echo "[i] Building Haruka module..."
rm -rf module/patcher/haruka*.dex
cp build/haruka*.dex module/patcher/

cd module
zip -r ../build/haruka.zip ./*
cd - > /dev/null

echo "[i] Building Haruka module done."