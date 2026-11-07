"""S3 합격선: 소리 흐름 B3~B5. 앱 화면은 뒤로(홈 키), 서비스만으로.
차선 변경 → 질문 → "고마워" → 받은 기록 1. "아니" → 회색 기록, 숫자 그대로. 무응답 → 기록 없음.
"오늘은 그만" → 그날 질문 0. 무응답 3번 → 쉼. 간격·상한 안에서는 묻지 않음.
"""
import sys
import time
from lib import *


def lane_change(mode="BlinkLeft"):
    turn(mode); time.sleep(1.0)
    ramp([8, 20, 15, 2, -10, -6, 0], 0.5)
    turn("Off")


def ask_and(answer, n_before):
    """차선 변경을 하고 질문을 기다린 뒤 answer를 넣는다(None이면 말하지 않음). 질문이 왔으면 True."""
    lane_change()
    if not wait_count(r"ASK question", n_before + 1, 20):
        return False
    # 듣기 시작을 기다린다
    end = time.time() + 6
    while time.time() < end and count_log(r"VOICE listen 4000ms") < count_log(r"ASK question"):
        time.sleep(0.2)
    if answer is not None:
        say(answer)
    wait_count(r"ASK done", n_before + 1, 15)
    return True


def received():
    return sum(1 for l in logs().splitlines() if "STORE add RECEIVED" in l)


def run(install_apk=True):
    fresh(install_apk=install_apk, theme="light")
    wait_log(r"DET start", 10)
    dbg(min_gap_ms=1000)
    home_key(); time.sleep(1)
    gear("Drive"); drive(60, 0); time.sleep(3)

    # 1. 고마워 → 받은 기록
    q = count_log(r"ASK question")
    check("질문이 온다", ask_and("고마워", q))
    seq = [l for l in logs().splitlines() if "TYM" in l]
    def idx(p, after=0):
        for i, l in enumerate(seq):
            if p in l and i >= after:
                return i
        return -1
    i_lane, i_sched, i_ask = idx("DET lane signaled=true"), idx("ASK scheduled in 6000ms"), idx("ASK question")
    check("차선 변경 뒤 6초 예약", 0 <= i_lane < i_sched < i_ask)
    check("띵 → 질문 → 듣기 순서", i_ask < idx("VOICE earcon ASK", i_ask) < idx("VOICE say '양보받았어요?'", i_ask) < idx("VOICE listen 4000ms", i_ask))
    check("포커스 요청", wait_log(r"VOICE focus result=1", 2) is not None)
    check("예로 분류", last_log(r"ASK answer") and "YES" in last_log(r"ASK answer"))
    check("받은 기록 1", received() == 1, last_log(r"STORE add"))
    check("기록했어요 + 두 음", wait_log(r"VOICE say '기록했어요'", 3) is not None and wait_log(r"VOICE earcon SAVED", 4) is not None)
    check("왼쪽 기록", "차선 변경 왼쪽" in (last_log(r"STORE add RECEIVED") or ""))
    time.sleep(2)

    # 2. 아니 → 회색 기록, 받은 수 그대로
    q = count_log(r"ASK question")
    ask_and("아니", q)
    check("아니 → NO", "NO" in (last_log(r"ASK answer") or ""))
    check("회색 기록", wait_log(r"STORE add NO", 3) is not None)
    check("받은 수 그대로", received() == 1)
    check("한 음 하강", count_log(r"VOICE earcon CANCEL") >= 1)
    time.sleep(2)

    # 3. 무응답 → 기록 없음
    q = count_log(r"ASK question"); adds = count_log(r"STORE add ")
    ask_and(None, q)
    check("무응답 → UNKNOWN", "UNKNOWN" in (last_log(r"ASK answer") or ""))
    check("무응답 → 기록 없음", count_log(r"STORE add ") == adds)
    time.sleep(2)

    # 4. 오늘은 그만 → 그날 질문 없음
    q = count_log(r"ASK question")
    ask_and("오늘은 그만", q)
    check("그만 → STOP", "STOP" in (last_log(r"ASK answer") or ""))
    check("그만 안내", wait_log(r"VOICE say '오늘은 그만 물을게요'", 3) is not None)
    time.sleep(2)
    q = count_log(r"ASK question")
    lane_change(); wait_log(r"ASK skip paused_user", 12)
    check("그만 뒤 질문 없음", count_log(r"ASK question") == q and wait_log(r"ASK skip paused_user", 1) is not None)

    # 5. 무응답 3번 → 쉼
    dbg(cmd="reset_day"); time.sleep(1)
    for k in range(3):
        q = count_log(r"ASK question"); ask_and(None, q); time.sleep(2)
    check("무응답 3번 → 쉼", wait_log(r"ASK pause noanswer", 3) is not None)
    q = count_log(r"ASK question")
    lane_change(); time.sleep(10)
    check("쉼 뒤 질문 없음", count_log(r"ASK question") == q and wait_log(r"ASK skip paused_noanswer", 1) is not None)

    # 6. 간격 180초
    dbg(cmd="reset_day"); dbg(min_gap_ms=0); time.sleep(1)
    q = count_log(r"ASK question"); ask_and("고마워", q); time.sleep(2)
    lane_change(); time.sleep(10)
    check("180초 안에는 묻지 않음", count_log(r"ASK question") == q + 1 and wait_log(r"ASK skip gap", 1) is not None)

    # 7. 하루 상한
    dbg(cmd="reset_day"); dbg(min_gap_ms=1000); dbg(daily_max=2); time.sleep(1)
    for k in range(2):
        q = count_log(r"ASK question"); ask_and("네", q); time.sleep(2)
    q = count_log(r"ASK question")
    lane_change(); time.sleep(10)
    check("상한 2번 뒤 묻지 않음", count_log(r"ASK question") == q and wait_log(r"ASK skip daily_max", 1) is not None)

    # 8. 깜빡이 없는 차선 변경은 묻지 않음 / 질문 끄기
    dbg(cmd="reset_day"); dbg(daily_max=10); time.sleep(1)
    q = count_log(r"ASK question")
    ramp([8, 20, 15, 2, -10, -6, 0], 0.5); time.sleep(12)
    check("깜빡이 없으면 묻지 않음", count_log(r"ASK question") == q)
    dbg(ask_enabled=False); time.sleep(0.5)
    lane_change(); time.sleep(10)
    check("질문 끄면 묻지 않음", count_log(r"ASK question") == q and wait_log(r"ASK skip off", 1) is not None)
    dbg(ask_enabled=True)

    # 9. 마이크 권한 없음 → 묻지 않음
    n = count_log(r"DET start")
    revoke_mic(); time.sleep(1)   # 권한을 빼면 프로세스가 죽는다 → 앱을 다시 켜서 서비스를 띄운다
    start(); wait_count(r"DET start", n + 1, 15); home_key(); time.sleep(4)
    lane_change(); time.sleep(10)
    check("마이크 없으면 묻지 않음", count_log(r"ASK question") == q and wait_log(r"ASK skip mic", 1) is not None)
    park()


if __name__ == "__main__":
    run(install_apk="--no-install" not in sys.argv)
    summary()
