"""모든 스프린트 시험을 차례로 돌린다. 각 시험은 앱을 지우고 새로 시작한다.
사용: python3 e2e/run_all.py   (APK는 미리 ./gradlew assembleDebug)
"""
import os
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
SUITES = ["s1_onboard_home.py", "s2_detect.py", "s3_voice.py", "s4_home_records.py", "s5_more.py", "s6_integration.py"]

results = []
ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
# 맥 마이크가 켜져 있으면 방 소리가 STT로 들어가 "무응답" 시험이 흔들린다. 시험 동안 끄고 끝나면 켠다(수동 시험용).
subprocess.run([ADB, "emu", "avd", "hostmicoff"], capture_output=True)
for i, s in enumerate(SUITES):
    t = time.time()
    args = [sys.executable, os.path.join(HERE, s)] + ([] if i == 0 else ["--no-install"])
    r = subprocess.run(args, cwd=HERE, capture_output=True, text=True)
    fails = [l for l in r.stdout.splitlines() if l.startswith("[FAIL]")]
    passes = sum(1 for l in r.stdout.splitlines() if l.startswith("[PASS]"))
    ok = r.returncode == 0
    results.append((s, ok, passes, fails, int(time.time() - t)))
    print(f"{'PASS' if ok else 'FAIL'} {s}: {passes} pass, {len(fails)} fail, {int(time.time() - t)}s", flush=True)
    for f in fails:
        print("   ", f, flush=True)
    if not ok and r.stderr.strip():
        print("   ", r.stderr.strip().splitlines()[-1], flush=True)

total = sum(p for _, _, p, _, _ in results)
bad = [s for s, ok, *_ in results if not ok]
subprocess.run([ADB, "emu", "avd", "hostmicon"], capture_output=True)
print(f"\nTOTAL {total} checks pass; suites failed: {bad or 'none'}")
sys.exit(1 if bad else 0)
