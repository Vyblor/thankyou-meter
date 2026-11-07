"""S5 합격선: 카드 숫자 = 이번 달 기록 수. QR 표시. 순위에 우리 동네 줄. 매너 값 = 감지 통계.
설정 질문 끄기 → 차선 변경해도 질문 없음. 상한 변경 반영. 모두 지우기 → 0. 마이크 권한 없음 → X2 띠. 오늘은 쉼 → X3 띠. 오프라인 → X4 띠.
"""
import re
import sys
import time
from lib import *


def lane_change():
    turn("BlinkLeft"); time.sleep(1.0)
    ramp([8, 20, 15, 2, -10, -6, 0], 0.5)
    turn("Off")


def ask_answer(text):
    n = count_log(r"ASK question")
    lane_change()
    if not wait_count(r"ASK question", n + 1, 20):
        return False
    end = time.time() + 6
    while time.time() < end and count_log(r"VOICE listen 4000ms") < count_log(r"ASK question") + 1:
        time.sleep(0.2)
    if text:
        say(text)
    wait_count(r"ASK done", n + 1, 15)
    return True


def dump_state():
    n = count_log(r"DEBUG dump")
    dbg(cmd="dump")
    wait_count(r"DEBUG dump", n + 1, 5)
    return last_log(r"DEBUG dump") or ""


def run(install_apk=True):
    fresh(install_apk=install_apk, theme="light")
    wait_log(r"DET start", 10)
    dbg(cmd="seed")
    check("예시 기록", wait_log(r"STORE seedDemo added=\d+", 5) is not None)
    d = dump_state()
    kinds = re.search(r"moments=\[([^\]]*)\]", d).group(1)
    recv, give = kinds.count("RECEIVED"), kinds.count("GIVEN")
    print(f"  seed recv={recv} give={give}")
    stop(); start(theme="light")
    wait("screen_home", timeout=10)

    # D1 · D2
    tap("home_month")
    check("D1 카드", wait("screen_month", timeout=5) is not None)
    nodes = dump()
    check("카드 받은 = 이번 달", find("card_recv", desc=f"양보받았어요 {recv}", nodes=nodes) is not None, find("card_recv", nodes=nodes).desc if find("card_recv", nodes=nodes) else None)
    check("카드 한 = 이번 달", find("card_give", desc=f"양보했어요 {give}", nodes=nodes) is not None)
    check("가장 고마웠던 시간", find("month_time", nodes=nodes) is not None and "시대" in find("month_time", nodes=nodes).all_text())
    screenshot("s5_month")
    tap("month_qr")
    check("D2 QR", wait("qr_image", timeout=5) is not None)
    check("10분 안내", "링크는 10분 뒤 사라져요" in screen_text())
    screenshot("s5_qr")
    tap("qr_close")
    check("닫으면 카드", wait("screen_month", timeout=4) is not None)

    # E1 · X4
    tap("tab_region")
    check("E1 동네", wait("screen_region", timeout=5) is not None)
    me = find("rank_me")
    check("우리 동네 줄", me is not None and "성동구 (우리 동네)" in me.desc, me.desc if me else None)
    check("상위 3줄", all(find(f"rank_{i}") for i in (1, 2, 3)))
    check("온라인이면 오프라인 띠 없음", find("banner_offline") is None)
    dbg(offline=True)
    check("X4 오프라인 띠", wait("banner_offline", timeout=5) is not None)
    screenshot("s5_region_offline")
    dbg(offline=False)
    check("다시 연결되면 띠 사라짐", gone("banner_offline", 5))

    # F1
    m = last_log(r"STORE manner") or ""
    lc = int(re.search(r"laneChanges=(\d+)", m).group(1)); sg = int(re.search(r"signaled=(\d+)", m).group(1))
    hb = int(re.search(r"hardBrakes=(\d+)", m).group(1)); ab = int(re.search(r"abrupt=(\d+)", m).group(1))
    tap("tab_manner")
    check("F1 매너", wait("screen_manner", timeout=5) is not None)
    nodes = dump()
    check("깜빡이 사용률 = 통계", find("gauge_signal", desc=f"{sg * 100 // lc}%", nodes=nodes) is not None, find("gauge_signal", nodes=nodes).desc)
    check("급제동 = 통계", find("gauge_brake", desc=f"{hb}번", nodes=nodes) is not None)
    check("급한 차선 변경 = 통계", find("gauge_abrupt", desc=f"{ab}번", nodes=nodes) is not None)
    screenshot("s5_manner")

    # G1
    tap("tab_settings")
    check("G1 설정", wait("screen_settings", timeout=5) is not None)
    screenshot("s5_settings")
    tap("set_ask")
    check("질문 끄기 저장", wait_log(r"STORE settings .*askEnabled=false", 3) is not None)
    tap("set_voice_2")
    check("목소리 크게 저장", wait_log(r"STORE settings .*voiceLevel=2", 3) is not None)
    tap("set_max_plus")
    check("상한 +1 → 11", wait("set_max_value", desc="11번", timeout=3) is not None)
    for _ in range(9):
        tap("set_max_minus")
    check("상한 최소 3", wait("set_max_value", desc="3번", timeout=3) is not None)
    home_key(); time.sleep(1)

    dbg(min_gap_ms=1000)
    gear("Drive"); drive(60, 0); time.sleep(3)
    n = count_log(r"ASK question")
    lane_change(); time.sleep(10)
    check("질문 끄면 묻지 않음", count_log(r"ASK question") == n and wait_log(r"ASK skip off", 1) is not None)
    park(); time.sleep(1)
    start(theme="light"); tap("tab_settings", timeout=8); tap("set_ask")
    check("질문 켜기 저장", wait_log(r"STORE settings .*askEnabled=true", 3) is not None)
    home_key(); time.sleep(1)
    gear("Drive"); drive(60, 0); time.sleep(3)
    for k in range(3):
        ask_answer("네"); time.sleep(2)
    n = count_log(r"ASK question")
    lane_change(); time.sleep(10)
    check("상한 3 반영", count_log(r"ASK question") == n and wait_log(r"ASK skip daily_max", 1) is not None)

    # X3 오늘은 쉼: 상한을 풀고 "오늘은 그만"
    dbg(cmd="reset_day"); dbg(daily_max=10); time.sleep(1)
    ask_answer("오늘은 그만")
    park(); time.sleep(1)
    start(theme="light"); tap("tab_home", timeout=8)
    check("X3 쉼 띠", wait("banner_paused", timeout=5) is not None)
    check("X3 해치 sleep", find("haechi_sleep") is not None)
    screenshot("s5_x3")
    tap("resume_today")
    check("다시 켜기 → 띠 사라짐", gone("banner_paused", 4) and wait_log(r"STORE day .*paused=null", 3) is not None)

    # G2 모두 지우기
    tap("tab_settings")
    tap("set_clear")
    check("G2 팝업", wait("popup", timeout=3) is not None and "기록을 모두 지울까요?" in screen_text())
    screenshot("s5_g2")
    tap("popup_cancel")
    check("취소 → 그대로", gone("popup", 3) and count_log(r"STORE clearAll") == 0)
    tap("set_clear"); tap("popup_confirm")
    check("모두 지우기", wait_log(r"STORE clearAll", 3) is not None)
    tap("tab_home")
    check("지운 뒤 홈 0", wait("count_recv", desc="받은 양보 0", timeout=5) is not None and find("count_give", desc="내가 한 양보 0") is not None)

    # X2 마이크 꺼짐
    revoke_mic(); time.sleep(1)
    start(theme="light")
    check("X2 마이크 띠", wait("banner_mic", timeout=8) is not None)
    check("X2 해치 sleep", find("haechi_sleep") is not None)
    screenshot("s5_x2")
    grant_all(); time.sleep(1)
    home_key(); time.sleep(1); start(theme="light")
    check("권한 주면 띠 사라짐", gone("banner_mic", 6))


if __name__ == "__main__":
    run(install_apk="--no-install" not in sys.argv)
    summary()
