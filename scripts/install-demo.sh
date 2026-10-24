#!/bin/sh
set -eu
if [ "$#" -ne 1 ]; then
    echo "Usage: scripts/install-demo.sh /path/to/gemma-4-E2B-it.litertlm" >&2
    exit 2
fi
model_path=$1
test -f "$model_path"
script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$script_dir/.."
if [ -z "${ANDROID_HOME:-}" ]; then . scripts/android-env.sh; fi
adb -d get-state
./gradlew :app:assembleDebug
adb -d install -r app/build/outputs/apk/debug/app-debug.apk
adb -d push "$model_path" /data/local/tmp/elsewhere-gemma-4-E2B-it.litertlm
adb -d shell run-as org.localhackathon.elsewhere mkdir -p files
adb -d shell "cat /data/local/tmp/elsewhere-gemma-4-E2B-it.litertlm | run-as org.localhackathon.elsewhere sh -c 'cat > files/gemma-4-E2B-it.litertlm'"
adb -d shell run-as org.localhackathon.elsewhere stat -c %s files/gemma-4-E2B-it.litertlm
adb -d shell rm /data/local/tmp/elsewhere-gemma-4-E2B-it.litertlm
adb -d shell am start -n org.localhackathon.elsewhere/.MainActivity
