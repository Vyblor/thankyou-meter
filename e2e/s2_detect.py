"""S2 합격선(감지 설계 표 8줄). 앱 화면을 뒤로 보낸 채(홈 키) 서비스만으로 감지하는지 본다.
차선 변경 10/10, 깜빡이만 0/5, 회전 0/3, Flash 3/3, 감속 후보 5/5, 정지 0/3, 차선 변경 직후 감속 0/3, 작은 감속 0/3.
"""
import sys
import time
from lib import *

LANE = r"DET lane signaled=true"
GIVE = r"DET give "
QUICK = "--quick" in sys.argv


def lane_change(mode="BlinkLeft", flash=False):
    turn(mode)
    time.sleep(1.0)
    if flash:
        pass  # 원터치는 콘솔이 스스로 끈다
    ramp([8, 20, 15, 2, -10, -6, 0], 0.5)
    if not flash:
        turn("Off")


def wait_more(pattern, before, timeout):
    return wait_count(pattern, before + 1, timeout)


def decel(to, back=60, step=3):
    v = 60
    while v > to:
        v = max(to, v - step); drive(v, 0); time.sleep(0.5)
    time.sleep(2.0)
    while v < back:
        v = min(back, v + step); drive(v, 0); time.sleep(0.5)


def run(install_apk=True):
    fresh(install_apk=install_apk, theme="light")
    check("서비스 포그라운드", wait_log(r"SVC foreground=special", 10) is not None, last_log(r"SVC foreground"))
    check("감지 시작", wait_log(r"DET start", 10) is not None)
    home_key()  # 앱 화면을 뒤로
    time.sleep(1)
    gear("Drive"); drive(60, 0)
    check("기어 D 수신", wait_log(r"VEH gear .*mapped=D", 8) is not None)
    time.sleep(3)

    n_lane = 3 if QUICK else 10
    for i in range(n_lane):
        b = count_log(LANE)
        lane_change("BlinkLeft" if i % 2 == 0 else "BlinkRight")
        ok = wait_more(LANE, b, 12)
        print(f"  lane {i+1}: {'ok' if ok else 'MISS'} {last_log(r'DET (lane|reject)')}")
    sides = [l for l in logs().splitlines() if "DET lane signaled=true" in l]
    check(f"차선 변경 {n_lane}/{n_lane}", len(sides) == n_lane, f"{len(sides)}")
    check("좌우 = 깜빡이", any("side=LEFT" in l for l in sides) and any("side=RIGHT" in l for l in sides))
    time.sleep(2)

    b = count_log(r"DET lane")
    for i in range(2 if QUICK else 5):
        turn("BlinkLeft"); time.sleep(3); turn("Off"); time.sleep(1)
    time.sleep(11)
    check("깜빡이만 0/5", count_log(r"DET lane") == b)

    b = count_log(r"DET lane"); bt = count_log(r"DET reject turn")
    for v in range(57, 24, -3):
        drive(v, 0); time.sleep(0.5)
    for i in range(3):
        drive(25, 0); turn("BlinkRight"); time.sleep(1)
        ramp([30, 120, 200, 150, 60, 10, 0], 0.5, speed=25)
        turn("Off"); time.sleep(5)
    for v in range(28, 61, 3):
        drive(v, 0); time.sleep(0.5)
    time.sleep(1)
    check("회전 0/3", count_log(r"DET lane") == b, f"turn rejects={count_log(r'DET reject turn') - bt}")

    b = count_log(LANE)
    for i in range(3):
        lane_change("FlashLeft", flash=True)
        wait_more(LANE, b + i, 12)
    check("Flash 3/3", count_log(LANE) - b == 3, f"{count_log(LANE) - b}")
    # 에뮬레이터 원터치는 꺼짐을 보내지 않는다. 다음 시험 전에 깜빡이를 끈다.
    turn("BlinkLeft"); time.sleep(0.5); turn("Off")
    check("깜빡이 꺼짐 수신", wait_log(r"VEH turn raw=OFF", 3) is not None)

    # 감속: 차선 변경 뒤 30초를 비운다
    drive(60, 0); time.sleep(31)
    b = count_log(GIVE)
    for i in range(2 if QUICK else 5):
        decel(40)
        wait_more(GIVE, b + i, 5)
        time.sleep(1)
    check(f"감속 후보 {2 if QUICK else 5}/{2 if QUICK else 5}", count_log(GIVE) - b == (2 if QUICK else 5), f"{count_log(GIVE) - b}")
    check("급제동 아님", count_log(r"DET hardbrake") == 0)

    b = count_log(GIVE)
    for i in range(3):
        decel(0, step=4); time.sleep(3)
    check("정지 0/3", count_log(GIVE) == b, f"stopped={count_log(r'reject give_stopped')}")

    b = count_log(GIVE)
    for i in range(3):
        lane_change("BlinkLeft"); time.sleep(4)
        decel(40); time.sleep(2)
    check("차선 변경 직후 감속 0/3", count_log(GIVE) == b)

    drive(60, 0); time.sleep(31)
    b = count_log(GIVE)
    for i in range(3):
        decel(54, step=2); time.sleep(3)
    check("작은 감속 0/3", count_log(GIVE) == b)

    # 급제동: 1초 안에 20 km/h
    drive(60, 0); time.sleep(3)
    drive(40, 0, brake=50); time.sleep(2); drive(60, 0)
    check("급제동 집계", wait_log(r"DET hardbrake", 5) is not None)

    # 깜빡이 없는 차선 변경: 묻지 않고 통계만
    time.sleep(3)
    ramp([8, 20, 15, 2, -10, -6, 0], 0.5)
    check("깜빡이 없는 차선 변경 집계", wait_log(r"DET lane signaled=false", 12) is not None)

    park()
    check("P단 수신", wait_log(r"VEH gear .*mapped=P", 8) is not None)
    time.sleep(1)
    dbg(cmd="dump")
    d = wait_log(r"DEBUG dump", 5) or ""
    print("  ", d[d.find("candidates"):][:300])
    check("후보가 저장소에", "40:null" in d)
    m = last_log(r"STORE manner") or ""
    check("매너 통계 저장", "hardBrakes=1" in m and "laneChanges=" in m, m[m.find("Manner"):])


if __name__ == "__main__":
    run(install_apk="--no-install" not in sys.argv)
    summary()
