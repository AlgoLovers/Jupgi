---
name: wrap-up
description: 턴 종료 의식 — 포맷·정적분석·테스트·커밋·(원격 있으면)푸시로 트리를 깨끗이 남긴다. 작업을 마칠 때마다, 특히 다른 세션(터미널/텔레그램)에 바통을 넘기기 전 실행한다.
---

# /wrap-up — 턴 종료 의식

두 Claude 세션이 같은 저장소를 공유하므로, 미커밋 파일을 남기면 다른 세션이 물려받아 충돌한다
(딱풀 6파일 충돌 전례). 이 의식으로 턴을 닫는다.

## 절차

1. `git status --porcelain`으로 변경 파악. keystore/AAB/APK/local.properties가 보이면 **절대 커밋 금지**
   (`.gitignore` 확인 — 권한 deny로도 막혀 있음).
2. Kotlin 변경이 있으면: `./gradlew -q spotlessApply detekt` → 위반은 지금 고친다.
   - `domain/` 로직(특히 `FoldMeshAtUseCase`)을 건드렸으면 `./gradlew -q testDebugUnitTest`까지.
   - 3D/UI를 건드렸으면 가능하면 `/emu-qa`로 눈 검증(라이트/다크).
   - 문서·에셋만 변경이면 gradle은 건너뛴다.
3. Conventional Commits로 커밋: `feat|fix|refactor|test|docs|chore(scope): 한국어 요약`.
   한 커밋 = 한 논리적 변경. 여러 주제가 섞였으면 나눠 커밋.
4. 원격이 연결돼 있으면 `git push origin develop`(또는 현재 feature 브랜치). main 직푸시 금지 —
   릴리스는 PR로. **원격이 아직 없으면 로컬 커밋까지만** 하고 그 사실을 보고한다.
5. 최종 확인: `git status --porcelain`이 비어 있어야 종료. Stop 훅이 더티 트리를 경고한다.

## 주의

- 커밋 메시지 마지막 줄: `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`.
- develop이 원격보다 뒤처져 있으면 pull 먼저(SessionStart 훅 컨텍스트 참고).
