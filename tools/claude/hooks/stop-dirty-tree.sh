#!/bin/bash
# Stop 훅 — 턴이 끝날 때 트리가 더러우면 조용히 경고한다.
# 근거: 미커밋 파일을 남기고 세션이 바뀌면 충돌한다(CLAUDE.md 세션 조율 절, 딱풀 6파일 충돌 전례).
# 작업 도중의 정상적인 더티 상태에도 뜨는 건 감수 — 한 줄짜리 리마인더라 소음이 작다.
cd "$(dirname "$0")/../../.." || exit 0
n=$(git status --porcelain 2>/dev/null | grep -cv keystore)
if [ "$n" -gt 0 ]; then
  printf '{"systemMessage":"🧹 미커밋 변경 %s개 — 두 세션 규약: 턴 끝엔 spotless→detekt→commit (/wrap-up)"}\n' "$n"
fi
