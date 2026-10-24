#!/bin/sh
# Source this file before invoking ./gradlew or adb on the hackathon Mac.
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/.local/share/productivity-android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/native/bin:$PATH"
