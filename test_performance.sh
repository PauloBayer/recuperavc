#!/bin/bash

# Whisper.cpp Android Performance Test Script
# This script helps test and validate the performance optimizations

echo "=== Whisper.cpp Android Performance Test ==="
echo "Building optimized version..."

# Build the project
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✓ Build successful"
else
    echo "✗ Build failed"
    exit 1
fi

# Install the APK
echo "Installing APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -eq 0 ]; then
    echo "✓ APK installed successfully"
else
    echo "✗ APK installation failed"
    exit 1
fi

echo ""
echo "=== Performance Test Instructions ==="
echo "1. Open the app on your Android device"
echo "2. Load the whisper small model"
echo "3. Test with the phrase: 'O rato roeu a roupa do rei de Roma'"
echo "4. Check the logs for performance metrics:"
echo "   - Thread count used"
echo "   - Memory optimization"
echo "   - Total processing time"
echo "   - System information"
echo ""
echo "To view logs in real-time:"
echo "adb logcat -s LibWhisper WhisperCpuConfig"
echo ""
echo "Expected improvements:"
echo "- Processing time should be reduced from 11-12s to 4-6s"
echo "- Maximum CPU utilization"
echo "- Optimized memory usage"
echo ""
echo "=== System Information ==="
echo "Device CPU cores:"
adb shell cat /proc/cpuinfo | grep processor | wc -l
echo ""
echo "Device memory:"
adb shell cat /proc/meminfo | grep MemTotal
echo ""
echo "CPU frequencies:"
adb shell "cat /sys/devices/system/cpu/cpu*/cpufreq/cpuinfo_max_freq 2>/dev/null | sort -nr | head -5"
echo ""
echo "Test completed! Check the app performance on your device."