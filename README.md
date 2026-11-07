# thankyou-meter

현대자동차그룹 차량용 앱 공모전(Pleos Connect) 두 번째 출품작 "고마워 운전"의 저장소입니다. 팀 Haechi.

- 기획(PRD): [docs/2026-11-03-prd-gomawo-driving.md](docs/2026-11-03-prd-gomawo-driving.md)
- 앱 상태·다음 할 일: [docs/thankyou-meter.md](docs/thankyou-meter.md)
- Pleos 플랫폼 사실과 에뮬레이터 실행법(2026-11-05~07 시험): [docs/pleos-platform.md](docs/pleos-platform.md)
- 접수 마감: 2026-11-15 23:59 KST

## 처음 한 번 (저장소를 받은 직후, 첫 커밋 전에)

공모전 양식은 공유 링크·첨부(이 저장소 포함)에 실명·연락처·이메일·소속이 드러나지 않게 하라고 합니다. 그래서 이 저장소의 모든 커밋은 팀 이름 `Haechi`와 noreply 메일로만 남깁니다. 아래는 **이 저장소에만** 적용되고 다른 저장소의 설정은 바꾸지 않습니다(`--global`을 쓰지 마세요).

```bash
git clone https://github.com/Vyblor/thankyou-meter.git
cd thankyou-meter
git config user.name  "Haechi"
git config user.email "225294644+Vyblor@users.noreply.github.com"
git config core.hooksPath .githooks     # 푸시 전 익명 검사를 켠다
tools/check-authors.sh                  # OK가 나오면 준비 끝
```

## 지킬 것

- **커밋은 PC에서만.** GitHub 웹에서 파일을 고치거나 PR을 병합하면 위 설정이 아니라 GitHub 계정 메일로 커밋됩니다.
- **실명·개인 메일·회사명을 코드, 문서, 커밋 메시지에 쓰지 않습니다.**
- 위 설정 전에 커밋했다면 푸시하지 말고 `git commit --amend --reset-author --no-edit`(마지막 1개) 등으로 작성자를 고친 뒤 `tools/check-authors.sh`로 확인하세요.
- Pleos 개발 약관: SDK 바이너리를 풀거나 역분석하지 않습니다. SDK 동작은 문서, 에뮬레이터 실행, 주최 측 문의로만 알아냅니다.
- 공모전 사이트 약관 9-3: 사이트를 자동으로 접속·수집하지 않습니다.
- 저장소는 지금 비공개이고, 마감 전에 공개로 바꿀 예정입니다. 공개 전에 `tools/check-authors.sh`를 전체 기록에 한 번 더 돌립니다.
