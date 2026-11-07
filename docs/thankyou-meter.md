---
name: thankyou-meter
title: thankyou-meter (고마워 운전)
app: thankyou-meter
status: prd
owner: cofounder
created: 2026-11-07
updated: 2026-11-07
---

# thankyou-meter (고마워 운전)

## 한 줄

차선을 바꾼 뒤 차가 "양보받았어요?"라고 묻고, 운전자가 "고마워"라고 말하면 기록한다. 기획은 공동창업자의 PRD를 따른다.

## 문서

- PRD: [[2026-11-03-prd-gomawo-driving]] (공동창업자 작성, 2026-11-03 원문 그대로 복사)
- Pleos 플랫폼 사실·에뮬레이터 실행법: [[pleos-platform]] (2026-11-07 기준 복사본. 방향지시등·조향각 구독, 온디바이스 STT·TTS, 주행 제한 DO 시험 결과 포함)

- 감지 설계: [[thankyou-meter-detection-design]] (2026-11-07 v0.1, 신호 실측과 회전 구분 기준)

## 지금 상태

- 2026-11-07: 앱 이름을 **땡큐미터**로 정했다(공동창업자 "고마워 운전은 땡큐미터로 이름을 정했고 지금부터 개발을 시작할꺼야"). 개발 시작. `EXTRACTED`
- 2026-11-07: 공동창업자 PC에 Pleos 에뮬레이터 설치·부팅 완료, 감지 신호 실측. 다음은 감지 시제품 코드. `EXTRACTED`

- 2026-11-07: 저장소만 만들었다(코드 없음). 개발은 공동창업자가 자기 PC에서 진행한다(창업자 지시 "개발은 공동창업자가 진행할거야 그 pc에서"). `EXTRACTED`
- 저장소는 GitHub `Vyblor/thankyou-meter` 비공개다. 마감 전에 공개로 바꾼다(carflash와 같은 방침). `EXTRACTED`
- 커밋 신원 규칙과 처음 설정은 README.md "처음 한 번".

## 다음 행동

| 날짜 | 누가 | 할 일 | 합격선(미리 고정) |
|---|---|---|---|
| ~~첫 작업 전~~ | 공동창업자 | README "처음 한 번" 실행 | 2026-11-07 확인: 로컬 신원 Haechi, 훅 켜짐, 위반 0건 |
| 다음 세션 | Claude + 공동창업자 | 감지 시제품(포그라운드 서비스 + 감지기 + 로그 화면) | [[thankyou-meter-detection-design]] 합격선 표 |
| 공개 전환 직전 | 창업자 | 전체 기록 익명 검사 | `tools/check-authors.sh`가 OK, 문서·코드에 실명·개인 메일·회사명 0건 |

## Open Questions

- `AMBIGUOUS` 공개 저장소에서 푸시한 계정(Activity 기록)이 외부에 어디까지 보이는지.
- `AMBIGUOUS` 고마워 운전의 주행 중 사용(DO) 카테고리 허용, 온디바이스 STT의 "고마워" 인식률([[pleos-platform]]).
- `AMBIGUOUS` 실차 조향 부호 규약, 실제 차선 변경·회전 각도 분포, 원터치 차선 변경과 자동 차선 변경의 신호 모양([[thankyou-meter-detection-design]]).

## Log

- 2026-11-07 — 저장소를 만들었다. `.gitignore`, README(공동창업자 처음 설정), CLAUDE.md/AGENTS.md(에이전트 규칙), 푸시 전 익명 검사(`.githooks/pre-push`, `tools/check-authors.sh`), PRD와 플랫폼 페이지 복사본.
- 2026-11-07 — 앱 이름 땡큐미터 확정, 익명 설정 점검 통과. 공동창업자 PC에 에뮬레이터 설치(JDK 17, emulator 37.2.13, 이미지 sha1 일치). 신호 실측: Flash도 LEFT/RIGHT로 옴, 조향 ±540도 수신, 100 ms 변화 19회 중 16회 수신(최대값 유지), 속도 m/s. 회전 구분 기준(90도 상한, 되돌림, 30 km/h) 설계 v0.1 → [[thankyou-meter-detection-design]].
- 2026-11-07 — 디자인 v0.1(테마·컴포넌트 23종·와이어프레임 25장)과 해치 캐릭터 시안 A/B/C를 워크스페이스 `ideas/thankyou-meter-design/`, `ideas/thankyou-meter-mascot/`에 만들었다(공동창업자 지시로 ideas 폴더에만). 구현 시작 때 이 저장소 `docs/design/`으로 옮긴다.
- 2026-11-07 — "내가 한 양보"를 넣기로 했다(공동창업자 요청). 차는 다른 차를 볼 수 없어(SDK에 레이더·거리 도메인 없음) 직진 중 감속을 후보로 모으고 정차 후 운전자가 고른다. 감지 조건·합격선 → [[thankyou-meter-detection-design]]. 디자인 v0.2(하트 로즈 `#E8365F`/`#FF7A98`, 와이어프레임 27장, 컴포넌트 24종) → [[thankyou-meter-design]].
- 2026-11-07 — 개발 S1~S6을 마쳤다. 공동창업자 지시: 스프린트별로 개발 → e2e → 버그 수정 → 커밋·푸시하고, 서버 없이 목업 데이터를 기기에만 쌓는다. 단위 시험 11개와 에뮬레이터 e2e 167개 항목이 모두 통과했다(`EXTRACTED`).
  - 만든 것: 첫 실행, 감지 서비스, 소리 질문, 홈, 기록, 양보 확인, 월간 카드·QR, 동네(목업), 매너, 설정, 예외 화면.
  - 시험 중 찾은 버그: `CAR_SPEED` 런타임 권한을 요청하지 않음, 감속 시작점 선택, 에뮬레이터 원터치는 OFF를 보내지 않음, 늦은 STT 종료 이벤트.
  - 다음은 공동창업자 수동 시험 → [[thankyou-meter-sprints]].
- 2026-11-07 — 수동 시험 1차 피드백을 반영했다(공동창업자: 질문이 너무 늦음, "고마워/땡큐"가 기록되지 않음, 인식하면 "확인했어요"가 나와야 함). 반영한 것:
  - 질문을 1.5초 뒤로 당김.
  - "확인했어요"로 바꿈.
  - 듣기 5초.
  - STT 결과 처리.
  - 땡큐 변형.
  - 오래된 조향 사건 리셋(버그).

  기록이 안 된 원인은 에뮬레이터 맥 마이크 입력이 꺼져 있던 것으로 본다(`INFERRED`). 단위 12개와 e2e 167개가 모두 통과해 커밋 `164b99e`로 푸시했다.

*Part of the Hyundai App Dev Challenge wiki. See [[index]] to navigate.*
