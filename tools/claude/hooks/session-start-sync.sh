#!/bin/bash
# SessionStart 훅 — 세션이 열릴 때 git 동기화 상태를 컨텍스트로 주입한다(CLAUDE.md 세션 조율 절).
# 원격이 아직 없으면(로컬 전용) fetch를 건너뛰고 브랜치·미커밋만 보고한다.
cd "$(dirname "$0")/../../.." || exit 0
branch=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "?")
if git remote 2>/dev/null | grep -q .; then
  git fetch -q origin 2>/dev/null
  upstream=$(git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>/dev/null || echo "")
  if [ -n "$upstream" ]; then
    behind=$(git rev-list --count "HEAD..$upstream" 2>/dev/null || echo "?")
    remote_note="$upstream 대비 뒤처짐 ${behind}커밋"
  else
    remote_note="원격 있음(업스트림 미설정)"
  fi
else
  remote_note="원격 미연결(로컬 전용)"
fi
dirty=$(git status --porcelain 2>/dev/null | grep -cv keystore)
printf '{"hookSpecificOutput":{"hookEventName":"SessionStart","additionalContext":"[세션 동기화] 브랜치 %s · %s · 미커밋(키스토어 제외) %s개. 뒤처졌으면 pull 먼저, 미커밋이 있으면 다른 세션이 남긴 것인지 확인."}}\n' "$branch" "$remote_note" "$dirty"
