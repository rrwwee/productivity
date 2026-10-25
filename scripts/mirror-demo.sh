#!/bin/sh
# Mirror the USB-connected phone for presenting from the laptop.
set -eu

if ! command -v adb >/dev/null 2>&1; then
    PATH="${ANDROID_HOME:-$HOME/.local/share/productivity-android-sdk}/platform-tools:$PATH"
    export PATH
fi
if ! command -v scrcpy >/dev/null 2>&1; then
    echo "Install scrcpy first: brew install scrcpy" >&2
    exit 1
fi

# Operate the physical phone; do not forward laptop input or audio.
exec scrcpy --select-usb --no-audio --no-control \
    --max-size=1600 --max-fps=30 --video-bit-rate=6M \
    --window-title="Elsewhere — Live demo" "$@"
