#!/usr/bin/env python3
"""Check a real, delivered nudge without reading any captured screen content."""
import json
import os
from pathlib import Path
import shlex
import shutil
import subprocess

PACKAGE = "org.localhackathon.elsewhere"
sdk = Path(os.environ.get("ANDROID_HOME", Path.home() / ".local/share/productivity-android-sdk"))
adb = shutil.which("adb") or str(sdk / "platform-tools/adb")


def shell(command):
    return subprocess.check_output([adb, "-d", "shell", command], text=True)


state = json.loads(shell(f"run-as {PACKAGE} cat files/diagnostics.json"))
assert state["nudged"], "No model-triggered nudge has been delivered in this session."
assert state["decision"] == "NUDGE", "The nudge did not follow Gemma's NUDGE verdict."
assert state["nudgeTitle"] and state["nudgeBody"], "Generated copy is missing."
keys = shell("cmd notification list").splitlines()
key = next((key for key in keys if f"|{PACKAGE}|2|null|" in key), None)
assert key, "Nudge is no longer posted. Run this check before tapping it."
record = shell("cmd notification get " + shlex.quote(key))
assert "vis=PUBLIC" in record, "Nudge text would be hidden during screen sharing."
assert "publicNotification=" in record, "Public notification version is missing."
assert f"android.title=String ({state['nudgeTitle']})" in record, "Posted title differs from Gemma output."
assert f"android.text=String ({state['nudgeBody']})" in record, "Posted body differs from Gemma output."
print("PASS: rule-triggered nudge contains Gemma's title/body and is public during capture.")
print(f"Vision: {state['latencyMs']} ms; copy generation: {state['copyLatencyMs']} ms.")
