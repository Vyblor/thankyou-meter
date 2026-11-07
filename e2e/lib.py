"""땡큐미터 e2e 도우미. adb + uiautomator + Pleos 버추얼 콘솔(HTTP 8088).

앱은 에뮬레이터의 사용자 10(운전자)으로 돈다. 화면 요소는 Compose testTag가 resource-id로 나온다.
판정은 앱 로그(태그 TYM)와 화면으로 한다. 카플래시 e2e/lib.py와 같은 구조다.
"""
import json
import os
import re
import subprocess
import time
import urllib.request
import xml.etree.ElementTree as ET

ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
PKG = "app.thankyoumeter"
USER = "10"
CONSOLE = "http://127.0.0.1:8088"
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
os.makedirs(OUT, exist_ok=True)
APK = os.path.join(os.path.dirname(HERE), "app", "build", "outputs", "apk", "debug", "app-debug.apk")

FAILS = []


def sh(*args, check=True, timeout=60):
    r = subprocess.run([ADB, *args], capture_output=True, text=True, timeout=timeout)
    if check and r.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)}: {r.stderr.strip()}")
    return r.stdout


def shell(cmd, **kw):
    return sh("shell", cmd, **kw)


# ---------- 앱 ----------
def install(apk=APK):
    sh("install", "-r", "-g", apk, timeout=300)


def clear():
    shell(f"pm clear --user {USER} {PKG}", check=False)


def grant_all():
    for p in ("android.permission.RECORD_AUDIO", "android.permission.POST_NOTIFICATIONS", "android.car.permission.CAR_SPEED"):
        shell(f"pm grant --user {USER} {PKG} {p}", check=False)


def revoke_mic():
    shell(f"pm revoke --user {USER} {PKG} android.permission.RECORD_AUDIO", check=False)


def logclear():
    sh("logcat", "-c", check=False)


def start(theme=None, width_dp=None):
    extra = ""
    if theme is not None:
        extra += f" --es tym_theme {theme or ''}"
    if width_dp is not None:
        extra += f" --ei tym_width_dp {width_dp}"
    shell(f"am start --user {USER} -W -n {PKG}/.MainActivity{extra}")


def stop():
    shell(f"am force-stop --user {USER} {PKG}", check=False)


def home_key():
    shell("input keyevent 3")


def dbg(**kw):
    """디버그 방송. 문자열은 --es, bool은 --ez, int는 --el."""
    parts = []
    for k, v in kw.items():
        if isinstance(v, bool):
            parts.append(f"--ez {k} {'true' if v else 'false'}")
        elif isinstance(v, int):
            parts.append(f"--el {k} {v}")
        else:
            parts.append(f"--es {k} '{v}'")
    shell(f"am broadcast --user {USER} -a app.thankyoumeter.DEBUG -p {PKG} {' '.join(parts)}")


def say(text):
    """STT가 이 글자를 들은 것으로 넣는다(디버그 빌드)."""
    dbg(say=text)


def logs():
    return sh("logcat", "-d", "-s", "TYM:I", check=False)


def wait_log(pattern, timeout=15):
    rx = re.compile(pattern)
    end = time.time() + timeout
    while time.time() < end:
        for line in logs().splitlines():
            if rx.search(line):
                return line
        time.sleep(0.4)
    return None


def count_log(pattern):
    rx = re.compile(pattern)
    return sum(1 for line in logs().splitlines() if rx.search(line))


def wait_count(pattern, n, timeout=15):
    end = time.time() + timeout
    while time.time() < end:
        if count_log(pattern) >= n:
            return True
        time.sleep(0.2)
    return False


def last_log(pattern):
    rx = re.compile(pattern)
    hit = None
    for line in logs().splitlines():
        if rx.search(line):
            hit = line
    return hit


# ---------- 화면 ----------
class Node:
    def __init__(self, e):
        self.e = e
        self.rid = e.get("resource-id", "")
        self.text = e.get("text", "")
        self.desc = e.get("content-desc", "")
        self.bounds = [int(x) for x in re.findall(r"\d+", e.get("bounds", "[0,0][0,0]"))]
        self.enabled = e.get("enabled") == "true"

    @property
    def center(self):
        x1, y1, x2, y2 = self.bounds
        return (x1 + x2) // 2, (y1 + y2) // 2

    def all_text(self):
        out = []
        for e in self.e.iter("node"):
            for k in ("text", "content-desc"):
                v = e.get(k)
                if v:
                    out.append(v)
        return " ".join(out)

    def __repr__(self):
        return f"<{self.rid or '-'} '{self.text or self.desc}' {self.bounds}>"


def dump():
    for _ in range(3):
        out = shell("uiautomator dump /sdcard/tym.xml", check=False)
        if "dumped" in out:
            xml = shell("cat /sdcard/tym.xml")
            try:
                nodes = []
                for e in ET.fromstring(xml).iter("node"):
                    n = Node(e)
                    if n.rid and not n.desc:
                        for ch in e.findall("node"):
                            if ch.get("content-desc") and ch.get("bounds") == e.get("bounds"):
                                n.desc = ch.get("content-desc")
                                break
                    nodes.append(n)
                return nodes
            except ET.ParseError:
                pass
        time.sleep(0.5)
    return []


def _match(n, tag):
    return n.rid == tag or n.rid.endswith(":id/" + tag)


def find(tag=None, text=None, desc=None, nodes=None):
    nodes = nodes if nodes is not None else dump()
    for n in nodes:
        if tag and not _match(n, tag):
            continue
        if text and text not in n.text:
            continue
        if desc and desc not in n.desc:
            continue
        return n
    return None


def find_all(prefix, nodes=None):
    nodes = nodes if nodes is not None else dump()
    return [n for n in nodes if n.rid.split("/")[-1].startswith(prefix)]


def wait(tag=None, text=None, desc=None, timeout=10):
    end = time.time() + timeout
    while time.time() < end:
        n = find(tag, text, desc)
        if n:
            return n
        time.sleep(0.4)
    return None


def gone(tag, timeout=10):
    end = time.time() + timeout
    while time.time() < end:
        if not find(tag):
            return True
        time.sleep(0.4)
    return False


def tap(tag=None, text=None, desc=None, timeout=10):
    n = wait(tag, text, desc, timeout)
    if not n:
        raise AssertionError(f"tap: not found tag={tag} text={text} desc={desc}")
    x, y = n.center
    shell(f"input tap {x} {y}")
    return n


def texts(nodes=None):
    nodes = nodes if nodes is not None else dump()
    return [n.text for n in nodes if n.text]


def screen_text():
    return " | ".join(x for n in dump() for x in (n.text, n.desc) if x)


def screenshot(name):
    path = os.path.join(OUT, f"{name}.png")
    with open(path, "wb") as f:
        f.write(subprocess.run([ADB, "exec-out", "screencap", "-p"], capture_output=True).stdout)
    return path


def pixel(x, y):
    """화면 픽셀 색 '#rrggbb'(raw screencap)."""
    raw = subprocess.run([ADB, "exec-out", "screencap"], capture_output=True).stdout
    w, h = int.from_bytes(raw[0:4], "little"), int.from_bytes(raw[4:8], "little")
    head = len(raw) - w * h * 4
    o = head + (y * w + x) * 4
    return "#%02x%02x%02x" % (raw[o], raw[o + 1], raw[o + 2])


def back():
    shell("input keyevent 4")


# ---------- 차(버추얼 콘솔) ----------
def console(method, path):
    req = urllib.request.Request(CONSOLE + path, method=method)
    with urllib.request.urlopen(req, timeout=5) as r:
        body = r.read().decode()
    try:
        return json.loads(body)
    except json.JSONDecodeError:
        return body


def gear(g):
    """g: Park | Drive | Reverse | Neutral"""
    return console("PUT", f"/vehicle/gear?gear={g}")


def turn(mode):
    """mode: Off | BlinkLeft | BlinkRight | FlashLeft | FlashRight"""
    return console("PUT", f"/vehicle/turn-signal?mode={mode}")


_cur = {"v": 0}


def drive(speed=60, steering=0, brake=0):
    """속도(km/h)와 조향각(도). 콘솔 조향 부호는 앱과 반대다(감지는 부호와 무관)."""
    acc = 0 if brake else 20
    _cur["v"] = speed
    return console("PUT", f"/vehicle/controls?speed={speed}&accelerator={acc}&brake={brake}&steering={steering}")


def ramp(path, step_s=0.5, speed=60):
    """조향각을 차례로 보낸다."""
    for a in path:
        drive(speed, a)
        time.sleep(step_s)


def park():
    # 실차처럼 서서히 멈춘다(0.5초에 4 km/h). 60→0을 한 번에 보내면 D단 급제동으로 셀 수 있다.
    v = _cur["v"]
    while v > 0:
        v = max(0, v - 4); drive(v, 0); time.sleep(0.5)
    drive(0, 0)
    turn("Off")
    gear("Park")


# ---------- 판정 ----------
def check(name, ok, detail=""):
    mark = "PASS" if ok else "FAIL"
    print(f"[{mark}] {name}" + (f" — {detail}" if detail else ""), flush=True)
    if not ok:
        FAILS.append(name)
    return ok


def summary():
    if FAILS:
        print(f"\n{len(FAILS)} FAIL: {FAILS}")
        raise SystemExit(1)
    print("\nALL PASS")


# ---------- 공통 흐름 ----------
def fresh(install_apk=True, onboard=True, theme=None):
    """앱을 지우고 새로 켠 뒤(선택) 첫 실행을 마치고 홈까지 간다."""
    if install_apk:
        install()
    park()
    stop()
    clear()
    grant_all()
    logclear()
    start(theme=theme)
    if onboard:
        finish_onboarding()
    wait_log(r"VEH signals registered", timeout=10)


def finish_onboarding():
    tap("onboard_start", timeout=15)
    tap("onboard_next2")
    tap("onboard_permit")
    wait("screen_onboard_4", timeout=10)
    tap("voice_skip")
    wait("screen_home", timeout=10)
