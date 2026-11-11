"""출시 APK(R8 축소·릴리스 서명) 기본 동작 시험.

디버그 시험 방송(say, min_gap_ms 등)은 릴리스에서 꺼져 있으므로 전체 회귀(run_all.py) 대신
심사위원이 밟을 기본 흐름만 확인한다. 목소리 대답은 넣을 수 없으니 "묻기까지"와 "대답 없음이면 기록 안 함"을 보고,
기록 경로는 내가 한 양보(감속 → P단 확인)로 확인한다:
설치 → 첫 실행 → 차량·음성 SDK 연결 → 차선 변경 감지 → TTS 질문 → 무응답 무기록 → 감속 후보 →
P단 홈 받은 0·확인할 양보 → 양보했어요 → 홈 한 1 → 월간 카드 → 매너 → 크래시 없음.

  python3 e2e/release_smoke.py [apk 경로]   # 기본: apk/Haechi_ThankYouMeter_release.apk
에뮬레이터와 버추얼 콘솔(포트 8088 + 하트비트)이 켜져 있어야 한다. 기존 앱은 지우고 새로 설치한다.
"""
import os
import sys
import time

import lib
from lib import check, wait, wait_log, count_log, tap, find, find_all, dump, gear, turn, drive, ramp, park

ROOT = os.path.dirname(lib.HERE)
APK = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "apk", "Haechi_ThankYouMeter_release.apk")


def main():
    print("APK:", APK, f"{os.path.getsize(APK) / 1e6:.1f} MB")
    lib.sh("uninstall", lib.PKG, check=False)  # 디버그 빌드와 서명이 달라 덮어쓰기가 안 된다
    lib.install(APK)
    pk = lib.shell(f"dumpsys package {lib.PKG}")
    flags = pk.split("pkgFlags=")[1].split("\n")[0] if "pkgFlags=" in pk else "DEBUGGABLE"
    check("릴리스 빌드(디버그 불가)", "DEBUGGABLE" not in flags)

    park(); lib.stop(); lib.clear(); lib.grant_all(); lib.sh("logcat", "-b", "crash", "-c", check=False); lib.logclear()
    lib.start()
    lib.finish_onboarding()
    check("첫 실행 → 홈", wait("count_recv", desc="받은 양보 0", timeout=8) is not None)
    check("차량 신호 구독(SDK)", wait_log(r"VEH signals registered", 10) is not None)
    check("TTS 준비(SDK)", wait_log(r"VOICE tts ready", 15) is not None)
    check("STT 준비(SDK)", wait_log(r"VOICE stt ready", 15) is not None)
    check("감지 서비스 시작", wait_log(r"DET start", 10) is not None)

    # 주행: 오른쪽 차선 변경
    lib.home_key(); time.sleep(1)
    gear("Drive"); drive(60, 0); time.sleep(3)
    turn("BlinkRight"); time.sleep(1)
    ramp([8, 20, 15, 2, -10, -6, 0], 0.5); turn("Off")
    check("차선 변경 감지", wait_log(r"DET lane signaled=true side=RIGHT", 12) is not None)
    check("질문 시작", wait_log(r"ASK question side=오른쪽", 12) is not None)
    check("TTS로 묻기", wait_log(r"VOICE tts start", 6) is not None)
    check("5초 듣기", wait_log(r"VOICE listen 5000ms", 10) is not None)
    check("질문 끝", wait_log(r"ASK done", 15) is not None)
    check("대답 없으면 기록 안 함", count_log(r"STORE add RECEIVED") == 0)

    # 내가 한 양보: 감지 뒤 30초는 감속을 받지 않는다
    time.sleep(31)
    v = 60
    while v > 38:
        v -= 3; drive(v, 0); time.sleep(0.5)
    time.sleep(2)
    while v < 60:
        v += 3; drive(v, 0); time.sleep(0.5)
    check("감속 → 양보 후보", wait_log(r"STORE candidate", 6) is not None)

    # 정차 → 확인
    park(); time.sleep(1)
    lib.start()
    check("홈 받은 0", wait("count_recv", desc="받은 양보 0", timeout=8) is not None)
    tap("pending_confirm")
    y = find_all("yc_yes_")
    check("확인할 후보 1개", len(y) == 1)
    if y:
        tap(y[0].rid.split("/")[-1])
    tap("confirm_later")
    check("양보했어요 → 홈 한 1", wait("count_give", desc="내가 한 양보 1", timeout=5) is not None)
    tap("home_month")
    nodes = dump()
    check("월간 카드 한 1", find("card_give", desc="양보했어요 1", nodes=nodes) is not None)
    tap("tab_manner")
    check("매너 화면", wait("screen_manner", timeout=5) is not None)

    crash = lib.sh("logcat", "-d", "-b", "crash", check=False)
    check("크래시 없음", lib.PKG not in crash, crash[-300:])
    fails = [l for l in lib.logs().splitlines() if "fail" in l.lower() or "exception" in l.lower()]
    check("SDK 실패 로그 없음", not fails, "\n".join(fails[:5]))
    lib.summary()


if __name__ == "__main__":
    main()
