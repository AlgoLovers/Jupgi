# LOOP.md — 접기(Jupgi) 루프 계약 & 런북

> 루프 엔지니어링의 "무엇을 언제 왜 반복하는가"를 명시적으로 적는 곳.
> 현재 상태/진행은 [`STATE.md`](STATE.md), 안전 경계는 [`loop-constraints.md`](loop-constraints.md).

## 루프 계약(Loop Contract)이란

신뢰할 수 있는 루프는 아래 요소를 명시해야 한다:

| 요소 | 설계 질문 | 이 프로젝트의 산출물 |
|---|---|---|
| Objective | 무엇을 최적화? | 마일스톤 / STATE.md 다음 액션 |
| Trigger | 언제 도는가? | `/loop`, 수동 지정 |
| Discover | 일이 어떻게 들어오나? | STATE.md, (이후) GitHub 이슈·CI 실패 |
| Workspace | 어디서 안전하게? | `feature/*` 브랜치 / git worktree |
| Context | 어떤 지식이 로드? | CLAUDE.md, docs/, STATE.md |
| Delegation | 누가 무슨 역할? | maker(구현) / checker(검증·감사) 분리 |
| Verification | 무엇이 "됐다"를? | testDebugUnitTest, assembleDebug, detekt, /emu-qa |
| State | 무엇이 남나? | 커밋, STATE.md 갱신 |
| Budget | 언제 멈추나? | 최대 턴/토큰 (loop-constraints) |
| Escalation | 언제 사람에게? | 아키텍처 분기·검증 신호 없음 |
| Exit | 언제 끝났다고? | 수용 기준 + 게이트 초록불 |

## 루프 성숙도

- **L1 보고 전용** ← 현재. 루프는 찾아서 보고만, 사람이 승인.
- **L2 저위험 자동수정**. 허용목록 안만 자동, 애매하면 에스컬레이션.
- **L3 무인**. 엄격한 denylist 하에 자율 실행.

---

## 이 프로젝트의 루프 (초기)

### LOOP-1 · Feature Build Loop (핵심 개발 루프)
- **Trigger**: 사람이 `/loop` 또는 다음 마일스톤 지정
- **Discover**: `STATE.md`의 "다음 액션"
- **Workspace**: `feature/<name>` 브랜치 (동시 편집이면 git worktree)
- **Delegation**: maker=구현, checker=별도 검증(테스트/detekt/빌드/emu-qa)
- **Verification**: `./gradlew testDebugUnitTest :app:assembleDebug detekt` (+UI면 `/emu-qa` 라이트/다크)
- **State**: 커밋 + `STATE.md` "결정 로그"·"다음 액션" 갱신
- **Escalation**: 아키텍처가 갈리면 멈추고 옵션 2~3개 제시
- **Exit**: 마일스톤 수용 기준 충족 + 게이트 초록불 (+원격 연결 시 PR 머지)
- **성숙도**: L1~L2

### LOOP-2 · Content Loop (작품 저작 — `/add-model`)
- **Trigger**: 새 작품 추가/수정
- **Discover**: 만들 작품 목록(ROADMAP·요청)
- **Verification**: 폴딩 솔버 검증(progress 훑기, 예외/자기관통) + `fold-auditor` 감사 + `/emu-qa` 단계별 눈 검증
- **State**: assets/리포지토리에 작품 추가, 목록·난이도 갱신
- **Exit**: 솔버 통과 + 감사 통과 + 각 단계 눈 검증 통과
- **주의**: 결정적 솔버가 없는 접기 종류는 지원 범위에 넣지 않는다(먼저 `origami-researcher`).

### LOOP-3 · Daily Triage (보고 전용, L1, 나중)
- **Trigger**: 스케줄 또는 수동
- **Discover**: (원격 연결 후) 새 이슈, 실패 CI, 스테일 브랜치
- **Exit**: STATE.md에 요약. 자동수정 없음.

> 새 루프를 추가할 땐 위 요소를 모두 채운다.
