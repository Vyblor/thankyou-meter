"""S6 통합 시나리오: 첫 실행(목소리 시험 통과) → 주행 → 질문·대답 → 감속 후보 → P단 확인 → 월간 카드.
한 번에, 앱을 지운 상태에서 시작해 끝까지 간다.
"""
import sys
import time
from lib import *


def run(install_apk=True):
    if install_apk:
        install()
    park(); stop(); clear(); grant_all(); logclear()
    start(theme="light")

    # 첫 실행
    tap("onboard_start", timeout=15); tap("onboard_next2"); tap("onboard_permit")
    wait("screen_onboard_4", timeout=10)
    wait_log(r"VOICE listen 4000ms", 6); say("고마워")
    wait_count(r"VOICE listen 4000ms", 2, 6); say("고마워요")
    check("목소리 시험 통과", wait("voice_title", text="잘 들려요", timeout=8) is not None)
    tap("voice_next")
    check("홈 0", wait("count_recv", desc="받은 양보 0", timeout=8) is not None)
    check("서비스 시작", wait_log(r"DET start", 10) is not None)
    dbg(min_gap_ms=1000)

    # 주행
    home_key(); time.sleep(1)
    gear("Drive"); drive(60, 0); time.sleep(3)
    turn("BlinkRight"); time.sleep(1)
    ramp([8, 20, 15, 2, -10, -6, 0], 0.5); turn("Off")
    check("차선 변경 감지", wait_log(r"DET lane signaled=true side=RIGHT", 12) is not None)
    check("질문", wait_log(r"ASK question side=오른쪽", 12) is not None)
    end = time.time() + 6
    while time.time() < end and count_log(r"VOICE listen 4000ms") < 3:
        time.sleep(0.2)
    say("고마워")
    check("받은 기록", wait_log(r"STORE add RECEIVED .*오른쪽", 8) is not None)
    check("확인 말", wait_log(r"VOICE say '기록했어요'", 4) is not None)
    time.sleep(31)
    v = 60
    while v > 38:
        v -= 3; drive(v, 0); time.sleep(0.5)
    time.sleep(2)
    while v < 60:
        v += 3; drive(v, 0); time.sleep(0.5)
    check("양보 후보", wait_log(r"STORE candidate", 6) is not None)

    # 정차
    park(); time.sleep(1)
    start(theme="light")
    check("홈 받은 1", wait("count_recv", desc="받은 양보 1", timeout=8) is not None)
    tap("pending_confirm")
    y = find_all("yc_yes_")
    check("후보 1개", len(y) == 1)
    tap(y[0].rid.split("/")[-1])
    tap("confirm_later")
    check("홈 한 1", wait("count_give", desc="내가 한 양보 1", timeout=5) is not None)
    tap("home_month")
    nodes = dump()
    check("카드 받은 1", find("card_recv", desc="양보받았어요 1", nodes=nodes) is not None)
    check("카드 한 1", find("card_give", desc="양보했어요 1", nodes=nodes) is not None)
    screenshot("s6_card")
    tap("tab_manner")
    check("매너 100%", wait("gauge_signal", desc="100%", timeout=5) is not None)

    # 밤 모드·세로에서 주요 화면이 뜨는지
    for theme, w in (("dark", None), ("light", 840), ("dark", 840)):
        start(theme=theme, width_dp=w if w else 0)
        for tab, scr in (("tab_home", "screen_home"), ("tab_records", "screen_records"), ("tab_region", "screen_region"), ("tab_manner", "screen_manner")):
            tap(tab, timeout=6)
            ok = wait(scr, timeout=5) is not None
            check(f"{theme}{'/' + str(w) if w else ''} {scr}", ok)
        screenshot(f"s6_{theme}_{w or 'land'}")
    start(theme="light", width_dp=0)


if __name__ == "__main__":
    run(install_apk="--no-install" not in sys.argv)
    summary()
