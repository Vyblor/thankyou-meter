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

## 지금 상태

- 2026-11-07: 저장소만 만들었다(코드 없음). 개발은 공동창업자가 자기 PC에서 진행한다(창업자 지시 "개발은 공동창업자가 진행할거야 그 pc에서"). `EXTRACTED`
- 저장소는 GitHub `Vyblor/thankyou-meter` 비공개다. 마감 전에 공개로 바꾼다(carflash와 같은 방침). `EXTRACTED`
- 커밋 신원 규칙과 처음 설정은 README.md "처음 한 번".

## 다음 행동

| 날짜 | 누가 | 할 일 | 합격선(미리 고정) |
|---|---|---|---|
| 첫 작업 전 | 공동창업자 | README "처음 한 번" 실행 | `tools/check-authors.sh`가 OK |
| 공개 전환 직전 | 창업자 | 전체 기록 익명 검사 | `tools/check-authors.sh`가 OK, 문서·코드에 실명·개인 메일·회사명 0건 |

## Open Questions

- `AMBIGUOUS` 공개 저장소에서 푸시한 계정(Activity 기록)이 외부에 어디까지 보이는지.
- `AMBIGUOUS` 고마워 운전의 주행 중 사용(DO) 카테고리 허용, 온디바이스 STT의 "고마워" 인식률([[pleos-platform]]).

## Log

- 2026-11-07 — 저장소를 만들었다. `.gitignore`, README(공동창업자 처음 설정), CLAUDE.md/AGENTS.md(에이전트 규칙), 푸시 전 익명 검사(`.githooks/pre-push`, `tools/check-authors.sh`), PRD와 플랫폼 페이지 복사본.

*Part of the Hyundai App Dev Challenge wiki. See [[index]] to navigate.*
