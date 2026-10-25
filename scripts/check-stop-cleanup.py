#!/usr/bin/env python3
"""Requires a connected debug build with an active, approved monitoring session."""
import subprocess,time,json,os,shutil
import argparse
from pathlib import Path
parser=argparse.ArgumentParser(description='Stop a real monitoring session during Gemma generation and check cleanup.')
parser.add_argument('--output', default='/tmp/elsewhere-stop-check.json')
args=parser.parse_args()
sdk=Path(os.environ.get('ANDROID_HOME',Path.home()/'.local/share/productivity-android-sdk'))
adb=shutil.which('adb') or str(sdk/'platform-tools/adb');pkg='org.localhackathon.elsewhere'
def sh(c):return subprocess.check_output([adb,'shell',c],text=True)
def state():
 try:return json.loads(sh(f'run-as {pkg} cat files/diagnostics.json'))
 except json.JSONDecodeError:return {}
def wait(fn,seconds):
 deadline=time.monotonic()+seconds
 while time.monotonic()<deadline:
  s=state()
  if fn(s):return s
  time.sleep(.25)
 raise RuntimeError(str(state()))
assert state().get('monitoring'), 'Start a monitoring session and approve Entire screen first.'
sh('am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.google.android.youtube')
sh(f'run-as {pkg} am startservice --user 0 -n {pkg}/.MonitoringService -a {pkg}.SOON')
wait(lambda s:s.get('monitoring') and s.get('status')=='Finding a more interesting next thing…',65)
time.sleep(.8)
before=state();assert before['monitoring'] and before['status']=='Finding a more interesting next thing…'
started=time.monotonic()
subprocess.run([adb,'shell',f'run-as {pkg} am stopservice --user 0 -n {pkg}/.MonitoringService'],capture_output=True,text=True)
after=wait(lambda s:not s.get('ready') and not s.get('busy') and not s.get('monitoring'),12)
elapsed=time.monotonic()-started
assert before['totalNudges']==after['totalNudges']
assert after['episodeId']==0 and not after['nudged']
assert f'|{pkg}|2|null|' not in sh('cmd notification list')
assert 'null' in sh('dumpsys media_projection')
Path(args.output).write_text(json.dumps({'before':before,'after':after,'releaseSeconds':round(elapsed,2),'noLateNotification':True},indent=2))
print(f'PASS: inference, model and capture stopped in {elapsed:.2f}s; no late notification',flush=True)
