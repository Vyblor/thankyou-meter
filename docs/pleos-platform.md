---
name: pleos-platform
title: Pleos Connect 플랫폼 사실과 에뮬레이터 실행법
created: 2026-11-05
updated: 2026-11-07
type: topic
source: Pleos 개발·심사 문서 사본(`contest/sources/pleos-docs/`), 에뮬레이터 v2.1.0 직접 실행(2026-11-05), [[2026-11-05-pleos-emulator-signal-probe]]
verified: 2026-11-05
---

# Pleos Connect 플랫폼 사실과 에뮬레이터 실행법

앱을 만들 때 매번 다시 찾지 않도록 확인된 사실만 모은 페이지다. 공모전 규정은 [[2026-11-02-hmg-vehicle-app-challenge]], 시험 원자료는 [[2026-11-05-pleos-emulator-signal-probe]]에 있다.

## 플랫폼

- Pleos Connect는 Android Automotive OS(API 34) 위에서 돈다. 화면 2560×1440, 160 dpi(`EXTRACTED`, 에뮬레이터 기기 프로필 "Pleos Connect v2.1.0").
- 개발 요구사항: Android API 28 이상, Gradle 8.0 이상, Java 또는 Kotlin(`EXTRACTED`, 공모전 FAQ).
- LLM SDK는 2026-10-19 제공 종료(`EXTRACTED`, 에뮬레이터 릴리즈 노트). 쓰지 않는다.
- 같은 기능의 AAOS 표준 API가 있으면 Pleos SDK는 그 기능을 따로 주지 않는다(`EXTRACTED`, SDK API 문서 "표준 우선 정책"). 예: 속도는 AAOS `PERF_VEHICLE_SPEED`(m/s).

## 쓴 SDK와 버전 (공개 Maven `nexus-playground`)

| SDK | 좌표 | 쓰임 |
|---|---|---|
| Vehicle 2.3.2 | `ai.pleos.playground:Vehicle:2.3.2` | 방향지시등, 조향각, 기어, 조명, 창문, 와이퍼, 공조, 좌석 |
| SpeechToText 3.3.1 | `ai.pleos.playground:SpeechToText:3.3.1` | 음성 인식(ON_DEVICE는 등록 없이 동작) |
| TextToSpeech 3.3.1 | `ai.pleos.playground:TextToSpeech:3.3.1` | 음성 출력 |
| NaviHelper 3.0.5 | `ai.pleos.playground:NaviHelper:3.0.5` | 내비 상태, TBT, 경유지 |

빌드 설정 예시는 `contest/probe-app/settings.gradle.kts`, `contest/probe-app/app/build.gradle.kts`. `useLibrary("android.car")`가 있어야 AAOS `Car` API를 쓸 수 있다.

## 사용 패턴

`Vehicle(context).initialize()` 뒤 도메인 객체를 얻는다(`getTurnSignal()`, `getSteeringWheel()`, `getGear()`, `getLight()`, `getWindow()`, `getWiper()`, `getHVAC()`). 각 도메인은 check(지원 여부·범위), get, set, register/unregister를 갖고, 결과는 onComplete/onFailed 콜백으로 온다. 초기화 직후 바로 부르지 말고 약 2.5초 뒤에 등록하면 안정적이었다(`INFERRED`, 시험 앱 구성).

**영역(area)은 반드시 check 결과에서 받아 쓴다.** `SeatArea(SeatValue.ROW_1_LEFT)`를 직접 만들어 앰비언트·송풍에 넣으면 `IllegalArgumentException: area ID: 0x1 not supported`가 난다(`EXTRACTED`, 시험). check 콜백이 준 SeatArea를 저장해 쓰면 된다.

## 권한

권한은 설치 시 허용되는 `normal` 등급이 대부분이다(`EXTRACTED`, 에뮬레이터 `pm list permissions -f`). 다만 **앱 마켓 출시 때는 Vehicle SDK 권한을 사전 협의·승인받아야 한다**(`EXTRACTED`, 심사 가이드). 공모전 시연 단계에서는 승인이 필요 없다(`EXTRACTED`, 공모전 문서).

| 쓰임 | 권한 |
|---|---|
| 방향지시등 읽기 | `pleos.car.permission.CAR_LIGHTS` |
| 조향각·기어 읽기 | `pleos.car.permission.CAR_DRIVE` |
| 앰비언트 색·밝기 쓰기 | `CAR_LIGHTS`, `CONTROL_CAR_LIGHTS` 둘 다 선언 |
| 창문 쓰기 | `CAR_WINDOWS`, `CONTROL_CAR_WINDOWS` |
| 와이퍼 쓰기 | `READ_WINDSHIELD_WIPERS`, `CONTROL_WINDSHIELD_WIPERS` (+ `CONTROL_CAR_WINDOWS`) |
| 송풍·공조 쓰기 | `CAR_CLIMATE`, `CONTROL_CAR_CLIMATE` |
| STT / TTS | `STT_SERVICE` / `TTS_SERVICE` (+ `RECORD_AUDIO`) |
| NaviHelper | `NAVI_ROUTE` (없으면 `getRouteStateInfo`에서 앱이 죽음), `NAVI_NOTIFY` |
| 속도(AAOS) | `android.car.permission.CAR_SPEED` |
| 경고등·타이어 압력 | `PRIVATE_CAR_SAFETY` — 일반 앱 대상 아님 |

`android.car.permission.READ_CAR_STEERING`, `READ_WINDSHIELD_WIPERS`(android 쪽)는 `signature|privileged`라 일반 앱이 못 받는다(`EXTRACTED`, `pm list permissions`). 조향각은 AAOS가 아니라 Pleos SDK 경로로만 받았다.

**외부 조명(2026-11-07 시험, 에뮬레이터 + 시험 앱 `LightProbe.kt`).**

- Pleos Vehicle SDK의 Light 도메인에 전조등(LowBeam), 상향등(HighBeam), 미등(TailLamp), 앞뒤 안개등의 check/get/set/register 함수가 있다(`EXTRACTED`, document.pleos.ai 사이트맵의 API Reference 목록). 비상등 함수는 Light에도 TurnSignal에도 없다. TurnSignalValue는 LEFT/RIGHT/OFF뿐이다(`EXTRACTED`, 같은 목록).
- 권한은 `pleos.car.permission.CAR_LIGHTS`, `CONTROL_CAR_LIGHTS`(둘 다 `normal`)만으로 된다(`EXTRACTED`, 시험).
- 전조등 스위치 `setLowBeamLightSwitch(LightSwitch.ON)`이 P단에서 성공했고 콘솔 `headlamp_mode`가 `LowBeam`으로 바뀌었다. 미등도 같다. 스위치 값은 Off/미등/전조등 중 하나라 미등을 켜면 전조등이 꺼진다(`EXTRACTED`, 시험).
- 0.5초 간격 켜기·끄기 4회에서 명령 → 상태 이벤트가 4~6 ms였다(`EXTRACTED`, 시험). 깜빡이기 연출에 쓸 만한 속도다.
- D단에서는 같은 쓰기가 `IllegalArgumentException: 50001`로 거부된다(`EXTRACTED`, 시험). 정차 중에만 쓸 수 있다.
- 상향등은 스위치 쓰기는 성공하지만 상태는 OFF로 남는다(state capability가 READ). 콘솔에서 상향등을 켜면 앱이 상태 이벤트를 받는다(`EXTRACTED`, 시험). 실차에서 앱이 상향등을 켤 수 있는지는 `AMBIGUOUS`.
- 콘솔 스위치를 Auto로 두면 SDK `getLowBeamLightSwitch`·`getTailLampLightSwitch`가 둘 다 OFF를 돌려준다. 쓰기 capability도 OFF·ON뿐이다. 앱은 AUTO를 알아보지도 되돌리지도 못한다(`EXTRACTED`, 카플래시 S7 시험).
- 안개등은 에뮬레이터가 지원하지 않는다(`ERROR_NOT_SUPPORTED_PROPERTY`, `EXTRACTED`).
- 비상등은 콘솔에서 켜도 앱에 아무 이벤트가 오지 않았다. AAOS 표준 `HEADLIGHTS_STATE`, `HAZARD_LIGHTS_STATE/SWITCH`는 `SecurityException: requires android.car.permission.CAR_EXTERIOR_LIGHTS`(쓰기는 `CONTROL_CAR_EXTERIOR_LIGHTS`)로 막힌다. 두 권한은 `signature|privileged`라 일반 앱은 받을 수 없다(`EXTRACTED`, 시험 + `pm list permissions -f`). 비상등은 일반 앱으로 읽지도 켜지도 못한다.
- 실차 출시 때 외부 조명 제어 권한이 승인될지는 `AMBIGUOUS`. Vehicle SDK 권한은 출시 때 사전 협의가 필요하다(위 권한 항목).

## 주행 제한

- 한국은 "그외 지역" 정책이다. DO를 선언하지 않은 앱은 D단에서 쓸 수 없다(`EXTRACTED`, 주행 제한 정책 문서 + 시험: 갤러리 앱이 "주행 중에는 사용할 수 없습니다"로 막힘).
- DO 선언은 매니페스트의 application과 activity에 `<meta-data android:name="distractionOptimized" android:value="true"/>`.
- 동영상·게임·브라우저 같은 미디어 앱은 정차 중에만(`EXTRACTED`, 심사 가이드).
- 주행 중 기능이 일부 허용되는 카테고리: Music/Audio, Maps/Navigation, Mobility Services(`EXTRACTED`, 앱 카테고리 문서). 다른 카테고리가 DO를 받을 수 있는지는 `AMBIGUOUS`.
- 주행 상태는 `CarUxRestrictionsManager`로 받는다. D단 `requiresDO=true active=80`, P단 해제, 기어 변경 후 약 0.5초(`EXTRACTED`, 시험).

## 에뮬레이터 실행법

설치 상태(2026-11-05, 창업자 Mac):

- 시스템 이미지: `~/Library/Android/sdk/system-images/android-34/pleos_car_emulator/arm64-v8a/` (원본 zip은 삭제, sha1 `24d28eb2f18eba5e87c861a7eb2bdccebb049419`)
- AVD: `~/.android/avd/Pleos_Probe.avd` (RAM 8 GB, 콜드 부팅 강제)
- 설치하면 42dot SDK 라이선스에 동의한 것으로 본다(`EXTRACTED`, 설치 문서).

```bash
# 1. 부팅 (약 15초)
~/Library/Android/sdk/emulator/emulator -avd Pleos_Probe -no-snapshot -no-boot-anim -gpu host &
ADB=~/Library/Android/sdk/platform-tools/adb
$ADB wait-for-device; until [ "$($ADB shell getprop sys.boot_completed | tr -d '\r')" = 1 ]; do sleep 2; done

# 2. 버추얼 콘솔 연결 (브라우저로 http://127.0.0.1:8088 도 가능)
$ADB forward tcp:8088 tcp:8088
# 스크립트로 쓸 때는 하트비트가 필요하다. 없으면 쓰기가 503 {"error":"emulator_inactive"}.
~/hyundai-appdevchallenge/contest/probe-app/tools/console-heartbeat.sh &

# 3. 신호 넣기
V=http://127.0.0.1:8088
curl -X PUT "$V/vehicle/gear?gear=Drive"                       # Park | Reverse | Neutral | Drive
curl -X PUT "$V/vehicle/controls?speed=60&accelerator=20&brake=0&steering=-25"
curl -X PUT "$V/vehicle/turn-signal?mode=BlinkLeft"            # Off | BlinkLeft | BlinkRight | FlashLeft | FlashRight
curl "$V/vehicle/state"                                        # 현재 상태(조명 색 등)
curl "$V/vehicle/catalog"                                      # 콘솔이 지원하는 항목 전체

# 4. 끄기
$ADB emu kill
```

- 콘솔 조향각 부호와 앱이 받는 부호가 반대다(콘솔 -25 → 앱 25.0, `EXTRACTED`).
- 앱은 사용자 10으로 돈다. 파일을 앱 폴더에 넣을 때는 `run-as <pkg> --user 10`(`EXTRACTED`).
- 시험 앱 빌드·설치: `cd contest/probe-app && ./gradlew assembleDebug && $ADB install -r -g app/build/outputs/apk/debug/app-debug.apk`. 명령은 `adb shell am broadcast -a ai.probe.CMD -p ai.probe --es c <cmd> --es a <arg>`(명령 목록은 `contest/probe-app/app/src/main/java/ai/probe/ProbeService.kt`의 `cmdReceiver`).
- 로그: `$ADB logcat -s PROBE:I`.

## Open Questions

- 다른 카테고리(Social, Lifestyle 등)의 DO 허용 여부 (`AMBIGUOUS`)
- 실차의 Vehicle SDK 제어 권한 승인 범위 (`AMBIGUOUS`)
- 실차에서 앱이 전조등·상향등을 켤 수 있고 출시 때 승인되는지 (`AMBIGUOUS`)

*Part of the Hyundai App Dev Challenge wiki. See [[index]] to navigate.*
