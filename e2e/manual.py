"""수동 시험용 차 신호 넣기. 앱 화면은 건드리지 않고 버추얼 콘솔(HTTP 8088)로 신호만 보낸다.

사용: python3 e2e/manual.py <명령>
  drive        D단, 60 km/h 직진
  left|right   깜빡이 켜고 왼쪽/오른쪽 차선 변경 (끝나고 약 4~5초 뒤 "양보받았어요?" 질문)
  flash        원터치 깜빡이로 왼쪽 차선 변경
  nosignal     깜빡이 없이 차선 변경 (질문 없음, 매너 통계에만)
  turn         교차로 우회전 (질문 없음)
  blink        깜빡이만 3초 켰다 끔 (질문 없음)
  decel        직진 중 60→40→60 감속 (P단 홈에 "확인할 양보" 후보)
  brake        급제동 60→35 (매너 급제동 +1)
  park         정지, 깜빡이 끔, P단
  log          앱 로그에서 감지·질문 줄만 계속 보여 준다 (Ctrl+C로 끝)
"""
import subprocess
import sys
import time
from lib import ADB, drive, gear, park, ramp, turn

LANE = [8, 20, 15, 2, -10, -6, 0]


def say(msg):
    print(msg, flush=True)


def lane(mode):
    turn(mode); time.sleep(1.0)
    ramp(LANE, 0.5)
    if not mode.startswith("Flash"):
        turn("Off")
    else:
        time.sleep(0.5); turn("BlinkLeft"); time.sleep(0.3); turn("Off")  # 에뮬레이터 원터치는 꺼짐을 안 보내서 끈다


def main(cmd):
    if cmd == "drive":
        gear("Drive"); drive(60, 0); say("D단 60 km/h")
    elif cmd in ("left", "right"):
        drive(60, 0); lane("BlinkLeft" if cmd == "left" else "BlinkRight")
        say("차선 변경 끝. 약 4~5초 뒤 질문이 와요 (같은 날 두 번째부터는 3분 간격)")
    elif cmd == "flash":
        drive(60, 0); lane("FlashLeft"); say("원터치 차선 변경 끝")
    elif cmd == "nosignal":
        drive(60, 0); ramp(LANE, 0.5); say("깜빡이 없는 차선 변경 끝 (질문 없음)")
    elif cmd == "turn":
        for v in range(57, 24, -3):
            drive(v, 0); time.sleep(0.5)
        turn("BlinkRight"); time.sleep(1)
        ramp([30, 120, 200, 150, 60, 10, 0], 0.5, speed=25); turn("Off")
        for v in range(28, 61, 3):
            drive(v, 0); time.sleep(0.5)
        say("우회전 끝 (질문 없음)")
    elif cmd == "blink":
        turn("BlinkLeft"); time.sleep(3); turn("Off"); say("깜빡이만 켰다 끔 (질문 없음)")
    elif cmd == "decel":
        v = 60
        while v > 40:
            v -= 3; drive(v, 0); time.sleep(0.5)
        time.sleep(2)
        while v < 60:
            v += 3; drive(v, 0); time.sleep(0.5)
        say("감속 끝. P단에서 홈에 '확인할 양보' 띠 (차선 변경 직후 30초 안이면 후보 아님)")
    elif cmd == "brake":
        drive(60, 0); time.sleep(2); drive(35, 0, brake=60); time.sleep(2); drive(60, 0); say("급제동 끝")
    elif cmd == "park":
        park(); say("P단")
    elif cmd == "log":
        subprocess.run([ADB, "logcat", "-v", "time", "-s", "TYM:I", "-e", "VEH turn|VEH gear|DET |ASK |STORE add|STORE candidate"])
    else:
        print(__doc__); sys.exit(1)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "")
