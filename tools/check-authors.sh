#!/usr/bin/env bash
# 커밋 작성자·커미터가 모두 팀 익명 신원인지 검사한다.
# 사용: tools/check-authors.sh [rev-range]   (인자 없으면 전체 기록)
# 공모전 양식: 공유 링크·첨부에 실명·연락처·이메일·소속이 드러나면 안 된다.
set -euo pipefail
NAME="Haechi"
EMAIL="225294644+Vyblor@users.noreply.github.com"
RANGE="${1:-HEAD}"
bad=$(git log --format='%h|%an|%ae|%cn|%ce' $RANGE 2>/dev/null | awk -F'|' -v n="$NAME" -v e="$EMAIL" '$2!=n || $3!=e || $4!=n || $5!=e')
if [ -n "$bad" ]; then
  echo "익명 규칙 위반 커밋이 있습니다(해시|작성자|메일|커미터|메일):" >&2
  echo "$bad" >&2
  echo "고치는 법은 README.md \"처음 한 번\" 절을 보세요." >&2
  exit 1
fi
echo "OK: 모든 커밋이 $NAME <$EMAIL> 입니다."
