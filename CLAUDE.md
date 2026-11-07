# thankyou-meter — 에이전트 작업 규칙

이 저장소에서 일하는 Claude Code(그리고 AGENTS.md를 읽는 다른 에이전트)는 아래를 지킨다.

1. **익명 규칙.** 커밋 작성자·커미터는 `Haechi <225294644+Vyblor@users.noreply.github.com>`뿐이다. 커밋 전에 `git config user.name`/`user.email`이 이 값인지 확인하고, 아니면 커밋하지 말고 사용자에게 README.md "처음 한 번"을 안내한다. 실명·개인 메일·회사명을 파일과 커밋 메시지에 쓰지 않는다. 푸시 전에 `tools/check-authors.sh`.
2. **커밋·푸시는 사용자가 요청할 때만.**
3. **Pleos 약관.** SDK 바이너리(aar/jar/class)를 풀거나 `javap` 등으로 읽지 않는다. 동작은 문서(docs/pleos-platform.md, document.pleos.ai), 에뮬레이터 실행, 주최 측 문의로만 확인한다.
4. **공모전 사이트 자동 접속·수집 금지**(사이트 약관 9-3).
5. **기록.** 결정·시험 결과·바뀐 것은 `docs/thankyou-meter.md`의 `## Log`에 날짜와 함께 적는다. 근거에는 `EXTRACTED`(말했거나 출처·시험이 보여 줌) / `INFERRED`(추론, 근거를 적음) / `AMBIGUOUS`(미확인, Open Questions에도 적음)를 단다.
