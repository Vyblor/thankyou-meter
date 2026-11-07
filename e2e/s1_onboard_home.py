"""S1 합격선: 첫 실행 A1→A2→A3→A4→A5(홈 0). A4는 "고마워" 2번이면 통과. 다시 켜면 홈. 밤 모드 바탕 #131417."""
import time
from lib import *


def run(install_apk=True):
    if install_apk:
        install()
    park()
    stop(); clear(); grant_all(); logclear()
    start(theme="light")

    # A1
    check("A1 첫 화면", wait("screen_onboard_1", timeout=15) is not None)
    check("A1 해치 hello", find("haechi_hello") is not None)
    t = screen_text()
    check("A1 약속 3줄", all(s in t for s in ("주행 중엔 화면 없이 목소리로만", "대답은 하지 않아도 돼요", "위치는 동네(시군구)까지만 써요")))
    screenshot("s1_a1")
    tap("onboard_start")

    # A2
    check("A2 이렇게 동작해요", wait("screen_onboard_2") is not None)
    tap("preview_ask")
    check("A2 질문 들어 보기: 알림음", wait_log(r"VOICE earcon ASK", 5) is not None)
    check("A2 질문 들어 보기: TTS", wait_log(r"VOICE say '양보받았어요\?'", 5) is not None)
    screenshot("s1_a2")
    tap("onboard_next2")

    # A3
    check("A3 권한 화면", wait("screen_onboard_3") is not None)
    tap("onboard_permit")
    check("A3 권한 결과 → A4", wait("screen_onboard_4", timeout=10) is not None and wait_log(r"PERM result", 5) is not None)

    # A4: 세 번 중 두 번
    check("A4 듣기 시작", wait_log(r"VOICE listen 4000ms", 6) is not None)
    check("A4 해치 listen", find("haechi_listen") is not None)
    say("고마워")
    wait_count(r"VOICE listen 4000ms", 2, 6)
    say("감사합니다")
    check("A4 1번째 들었어요", wait("voice_pill_0", desc="1번째 들었어요", timeout=6) is not None)
    check("A4 2번째 들었어요", wait("voice_pill_1", desc="2번째 들었어요", timeout=6) is not None)
    n = wait("voice_title", text="잘 들려요", timeout=4)
    check("A4 통과 문구", n is not None)
    check("A4 해치 cheer", find("haechi_cheer") is not None)
    screenshot("s1_a4")
    tap("voice_next")

    # A5 = 홈 0
    check("A5 홈", wait("screen_home", timeout=8) is not None)
    check("첫 실행 끝 기록", wait_log(r"ONBOARD done", 3) is not None)
    nodes = dump()
    check("홈 받은 0", find("count_recv", desc="받은 양보 0", nodes=nodes) is not None)
    check("홈 한 0", find("count_give", desc="내가 한 양보 0", nodes=nodes) is not None)
    check("홈 준비 끝", find("home_ready", nodes=nodes) is not None)
    check("홈 해치 idle", find("haechi_idle", nodes=nodes) is not None)
    check("홈 레일 5칸", all(find(t, nodes=nodes) for t in ("tab_home", "tab_records", "tab_region", "tab_manner", "tab_settings")))
    check("기어 칩", find("gear_chip", nodes=nodes) is not None)
    check("낮 바탕 #f7f8fa", pixel(2200, 1000) == "#f7f8fa", pixel(2200, 1000))
    screenshot("s1_home_light")

    # 다시 켜면 홈
    stop(); time.sleep(0.5); start(theme="light")
    check("다시 켜면 A1 생략", wait("screen_home", timeout=10) is not None and find("screen_onboard_1") is None)

    # 밤
    start(theme="dark")
    time.sleep(1.5)
    px = pixel(2200, 1000)
    check("밤 바탕 #131417", px == "#131417", px)
    screenshot("s1_home_dark")
    start(theme="auto")

    # A4 실패 경로: 두 번 못 들으면 대체 모드 제안
    stop(); clear(); grant_all(); logclear(); start(theme="light")
    tap("onboard_start", timeout=15); tap("onboard_next2"); tap("onboard_permit")
    wait("screen_onboard_4")
    wait_log(r"VOICE listen 4000ms", 6)
    say("음")
    wait_count(r"VOICE listen 4000ms", 2, 6)
    say("뭐라고")
    check("A4 1번째 못 들음", wait("voice_pill_0", desc="1번째 못 들었어요", timeout=6) is not None)
    check("A4 대체 모드 제안", wait("voice_fallback", timeout=6) is not None)
    tap("voice_anyyes")
    dbg(cmd="dump")
    check("아무 말이나 예로 듣기 켜짐", wait_log(r"anyYes=true", 4) is not None)
    tap("voice_skip")
    check("건너뛰기 → 홈", wait("screen_home", timeout=8) is not None)


if __name__ == "__main__":
    run()
    summary()
