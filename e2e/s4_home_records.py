"""S4 합격선: 주행 시험 뒤 P단 홈에 받은 수·후보 띠. C4 "양보했어요" → 한 수 +1, "아니에요" → 사라짐.
기록 지우기 → 토스트 → 되돌리기로 복구. 필터 "양보받음"·"양보함" 개수 일치. 840에서 넘침 없음.
"""
import sys
import time
from lib import *


def lane_change():
    turn("BlinkLeft"); time.sleep(1.0)
    ramp([8, 20, 15, 2, -10, -6, 0], 0.5)
    turn("Off")


def decel(to=40):
    v = 60
    while v > to:
        v = max(to, v - 3); drive(v, 0); time.sleep(0.5)
    time.sleep(2)
    while v < 60:
        v = min(60, v + 3); drive(v, 0); time.sleep(0.5)


def count_desc(tag):
    n = find(tag)
    return n.desc if n else None


def overflow(limit_right):
    bad = [n for n in dump() if n.rid and ":id/" not in n.rid and n.bounds[2] > limit_right + 2]
    return bad


def run(install_apk=True):
    fresh(install_apk=install_apk, theme="light")
    wait_log(r"DET start", 10)
    dbg(min_gap_ms=1000)
    home_key(); time.sleep(1)
    gear("Drive"); drive(60, 0); time.sleep(3)

    # 주행: 받은 양보 1, 감속 후보 2
    lane_change()
    wait_log(r"ASK question", 20)
    end = time.time() + 6
    while time.time() < end and count_log(r"VOICE listen 5000ms") < 1:
        time.sleep(0.2)
    say("고마워")
    check("주행 중 받은 기록", wait_log(r"STORE add RECEIVED", 10) is not None)
    time.sleep(31)  # 차선 변경 직후 감속은 후보가 아니다
    decel(40); wait_log(r"DET give", 5); time.sleep(2)
    decel(35)
    check("감속 후보 2개", wait_count(r"STORE candidate", 2, 8))
    park(); time.sleep(1)

    # P단 홈
    start(theme="light")
    check("홈", wait("screen_home", timeout=10) is not None)
    check("홈 받은 1", wait("count_recv", desc="받은 양보 1", timeout=5) is not None, count_desc("count_recv"))
    check("홈 한 0", find("count_give", desc="내가 한 양보 0") is not None)
    check("확인할 양보 띠", find("banner_pending") is not None)
    check("띠 문구 2개", "확인할 양보 2개" in screen_text())
    screenshot("s4_home_pending")

    # C4
    tap("pending_confirm")
    check("C4 화면", wait("screen_confirm", timeout=5) is not None)
    yes = find_all("yc_yes_")
    check("후보 줄 2개", len(yes) == 2, f"{len(yes)}")
    screenshot("s4_confirm")
    id1 = yes[0].rid.split("_")[-1]; id2 = yes[1].rid.split("_")[-1]
    tap(f"yc_yes_{id1}")
    check("양보했어요 → 알약", wait(f"yc_done_{id1}", timeout=4) is not None and "양보했어요로 기록함" in (find(f"yc_done_{id1}").all_text()))
    tap(f"yc_no_{id2}")
    check("아니에요 → 알약", wait(f"yc_done_{id2}", timeout=4) is not None)
    check("저장 resolve", count_log(r"STORE resolve id=\d+ gave=true") == 1 and count_log(r"STORE resolve id=\d+ gave=false") == 1)
    check("모두 고르면 홈으로 버튼", find("confirm_later") is not None and "홈으로" in find("confirm_later").all_text())
    tap("confirm_later")
    check("홈 한 1", wait("count_give", desc="내가 한 양보 1", timeout=5) is not None, count_desc("count_give"))
    check("띠 사라짐", find("banner_pending") is None)

    # C3
    tap("tab_records")
    check("C3 화면", wait("screen_records", timeout=5) is not None)
    check("오늘 2개", wait("records_count", desc="기록 2개", timeout=4) is not None, count_desc("records_count"))
    tap("kind_1"); check("양보받음 1", wait("records_count", desc="기록 1개", timeout=4) is not None)
    check("양보받음 줄 = 받은", all("RECEIVED" in n.desc for n in find_all("moment_")))
    tap("kind_2"); check("양보함 1", wait("records_count", desc="기록 1개", timeout=4) is not None)
    check("양보함 줄 = 한", all("GIVEN" in n.desc for n in find_all("moment_")))
    tap("kind_0"); tap("period_2"); check("이번 달 2개", wait("records_count", desc="기록 2개", timeout=4) is not None)
    tap("period_0")
    screenshot("s4_records")

    # 지우기 → 되돌리기
    d = find_all("delete_")
    tap(d[0].rid.split("/")[-1])
    nodes = dump()
    check("지우면 토스트", find("toast", nodes=nodes) is not None and "기록 1개를 지웠어요" in find("toast", nodes=nodes).all_text())
    check("지운 뒤 1개", find("records_count", desc="기록 1개", nodes=nodes) is not None)
    screenshot("s4_toast")
    x, y = find("toast_action", nodes=nodes).center; shell(f"input tap {x} {y}")
    check("되돌리기 저장", wait_log(r"STORE restore id=", 3) is not None)
    check("되돌리면 2개", wait("records_count", desc="기록 2개", timeout=3) is not None)
    check("토스트 닫힘", gone("toast", 3))
    # 8초 지나면 되돌릴 수 없다
    d = find_all("delete_")
    tap(d[0].rid.split("/")[-1]); wait("toast", timeout=3)
    time.sleep(9)
    check("8초 뒤 토스트 사라짐", find("toast") is None)
    check("8초 뒤에도 1개", find("records_count", desc="기록 1개") is not None)

    # 아니 회색 기록도 보임(숫자 외)
    # C2 세로 840
    start(theme="light", width_dp=840)
    tap("tab_home", timeout=6)
    h = wait("screen_home", timeout=6)
    check("840 홈", h is not None)
    right = h.bounds[2] if h else 0
    check("840 아래 탭", find("tab_records") is not None and find("tab_records").bounds[1] > 1000)
    bad = overflow(right)
    check("840 홈 넘침 없음", not bad, f"{bad[:3]}")
    screenshot("s4_home_840")
    tap("tab_records")
    wait("screen_records", timeout=5)
    bad = overflow(right)
    check("840 기록 넘침 없음", not bad, f"{bad[:3]}")
    screenshot("s4_records_840")
    start(theme="light", width_dp=0)


if __name__ == "__main__":
    run(install_apk="--no-install" not in sys.argv)
    summary()
