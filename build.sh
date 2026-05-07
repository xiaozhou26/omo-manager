#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== OMO Manager Build Script ==="

if [ -z "$ANDROID_NDK_HOME" ]; then
    echo "ERROR: ANDROID_NDK_HOME is not set"
    echo "Please set it to your NDK installation path, e.g.:"
    echo "  export ANDROID_NDK_HOME=\$HOME/Android/Sdk/ndk/26.1.10909125"
    exit 1
fi

echo "[1/3] Building Rust native library..."
cd rust

rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android 2>/dev/null || true

if command -v cargo-ndk &> /dev/null; then
    cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -t x86 -o ../app/src/main/jniLibs build --release
else
    echo "cargo-ndk not found, building manually..."
    mkdir -p ../app/src/main/jniLibs/arm64-v8a
    mkdir -p ../app/src/main/jniLibs/armeabi-v7a
    mkdir -p ../app/src/main/jniLibs/x86_64
    mkdir -p ../app/src/main/jniLibs/x86

    cargo build --release --target aarch64-linux-android
    cp target/aarch64-linux-android/release/libomo_native.so ../app/src/main/jniLibs/arm64-v8a/

    cargo build --release --target armv7-linux-androideabi
    cp target/armv7-linux-androideabi/release/libomo_native.so ../app/src/main/jniLibs/armeabi-v7a/

    cargo build --release --target x86_64-linux-android
    cp target/x86_64-linux-android/release/libomo_native.so ../app/src/main/jniLibs/x86_64/

    cargo build --release --target i686-linux-android
    cp target/i686-linux-android/release/libomo_native.so ../app/src/main/jniLibs/x86/
fi

echo "[2/3] Rust build complete!"

cd ..
echo "[3/3] Building Android APK..."
chmod +x gradlew
./gradlew assembleDebug

echo "=== Build complete! ==="
echo "Debug APK: app/build/outputs/apk/debug/app-debug.apk"
