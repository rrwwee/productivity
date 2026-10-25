#!/usr/bin/env python3
"""Requires a connected phone with the auto-resume Accessibility permission enabled."""
import subprocess,time,json,os,shutil,argparse
from pathlib import Path
parser=argparse.ArgumentParser(description='Exercise an enabled auto-resume session. Locks and wakes the phone, and opens YouTube Shorts.')
parser.add_argument('--output', default='/tmp/elsewhere-auto-resume.json')
args=parser.parse_args()
sdk=Path(os.environ.get('ANDROID_HOME',Path.home()/'.local/share/productivity-android-sdk'))
adb=shutil.which('adb') or str(sdk/'platform-tools/adb');pkg='org.localhackathon.elsewhere'
evidence={};target=Path(args.output)
def sh(c):return subprocess.check_output([adb,'shell',c],text=True)
def state():
 try:return json.loads(sh(f'run-as {pkg} cat files/diagnostics.json'))
 except (json.JSONDecodeError,subprocess.CalledProcessError):return {}
def wait(fn,seconds,label):
 end=time.monotonic()+seconds
 while time.monotonic()<end:
  s=state()
  if fn(s):
   evidence[label]=s;target.write_text(json.dumps(evidence,indent=2));print('PASS '+label,flush=True);return s
  time.sleep(.5)
 raise RuntimeError(label+': '+str(state()))
wait(lambda s:s.get('autoResume') and s.get('monitoring'),240,'auto_service_started')
assert 'null' in sh('dumpsys media_projection'), 'Unexpected MediaProjection session'
before=state()
sh('am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.google.android.youtube')
a=wait(lambda s:s.get('autoResume') and s.get('totalNudges',0)>before.get('totalNudges',0) and s.get('decision')=='NUDGE',100,'accessibility_capture_nudge')
sh('input keyevent KEYCODE_SLEEP')
locked=wait(lambda s:s.get('monitoring') and s.get('pausedForLock') and not s.get('ready') and not s.get('busy'),20,'locked_paused_model_released')
time.sleep(6)
s=state();assert s['checks']==locked['checks'] and s['totalNudges']==locked['totalNudges'],(locked,s)
evidence['no_locked_checks']=True;target.write_text(json.dumps(evidence,indent=2))
sh('input keyevent KEYCODE_WAKEUP');sh('wm dismiss-keyguard')
wait(lambda s:s.get('monitoring') and s.get('autoResume') and not s.get('pausedForLock') and s.get('checks',0)>locked['checks'],100,'unlock_resumed_without_prompt')
wait(lambda s:s.get('totalNudges',0)>locked['totalNudges'],80,'nudge_after_unlock')
assert 'null' in sh('dumpsys media_projection')
print('PASS auto-resume sequence, no MediaProjection approval used',flush=True)
