# loop-constraints.md — 안전 경계 & 거버넌스

> 루프가 "무엇을 해도 되고 안 되는지"의 기계적 경계. 자율성이 올라갈수록(L2→L3) 여기가 방어선이다.
> 사람 게이트가 항상 최종 통제점.

## 절대 금지 (Denylist — 사람 확인 없이 절대 X)

- 비밀정보/서명키 커밋: `*.keystore`, `*.jks`, `keystore.properties`, `.env*`, `local.properties`
- 파괴적 명령: `rm -rf`, `git reset --hard`, `git push --force`(공유 브랜치), 히스토리 재작성
- `main`에 직접 커밋/푸시 (항상 브랜치 → PR)
- `--no-verify`로 훅/CI 우회
- 대용량 바이너리 커밋(APK/AAB/모델). 필요 시 GitHub 릴리스 아티팩트나 LFS.
- 외부로 코드/데이터 전송(새 서드파티 서비스) — 사람 승인 필요
- **라이선스 불명확한 작품·도안 번들링** — 종이접기 도안은 저작권 대상. 출처·라이선스가 명확한
  것(직접 저작, CC, 퍼블릭 도메인, 저작자 허락)만 넣는다.

## 자동 허용 (Allowlist — L2에서 자동 진행 가능)

- `feature/**`, `docs/**`, `app/src/test/**` 안에서의 코드/문서/테스트 변경
- 브랜치 생성, 커밋, **PR 열기** (머지는 사람)
- `./gradlew testDebugUnitTest`, `assembleDebug`, `detekt`, `spotlessApply` 실행
- `/emu-qa` 스크린샷 검증 (에뮬레이터 부팅·설치·캡처)

## 예산 (Budget — 언제 멈추나)

- 단일 루프 런: 최대 **~10 턴** 또는 목표 1개 완료 중 먼저.
- 반복이 3회 연속 진전 없으면 멈추고 에스컬레이션(무한 재시도 금지).
- 토큰 급증은 서브에이전트·장기 런에서 발생 — 큰 팬아웃 전에 견적/확인.

## 에스컬레이션 (Escalation — 언제 사람에게)

- 아키텍처가 갈리거나 요구가 상충 → **멈추고** 옵션 2~3개 + 트레이드오프 제시.
- 검증 신호가 없거나 애매 → 사람에게. (특히 새 접기 종류의 결정적 솔버가 없을 때)
- Denylist에 닿는 작업이 필요 → 사람 승인 요청.

## 검증 게이트 (Verification Gate)

머지/완료 전 반드시 통과:

1. `./gradlew testDebugUnitTest` — 초록불 (폴딩 도메인 변경 시 특히)
2. `./gradlew :app:assembleDebug` — 빌드 성공
3. `./gradlew detekt` — 신규 심각 위반 없음
4. UI/3D 변경이면 `/emu-qa` 라이트·다크 눈 검증
5. 코드와 테스트가 같은 diff에 있음

> 이 파일과 [`STATE.md`](STATE.md)가 어긋나면 사람이 조정한다. 자율성은 여기 규칙이 신뢰될 때만 L2→L3로 올린다.
