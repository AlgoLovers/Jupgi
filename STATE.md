# STATE.md — 접기(Jupgi) 현재 상태

> 작업 시작 전 이 파일을 읽는다. 지금 어느 마일스톤인지, 마지막 결정과 다음 액션이 뭔지.
> 턴 종료 시 "결정 로그"·"다음 액션"을 갱신한다.

## 지금 어디인가

- **마일스톤: M0 (골격) — 완료.** 2026-07-25 하네스 + 빌드되는 스켈레톤 세팅.
- 검증 게이트 초록불: `testDebugUnitTest`(10개 통과) + `:app:assembleDebug`(APK 16MB). 에뮬레이터
  라이트/다크 스샷으로 3D 뷰어 눈 검증 완료.

## 무엇이 있나 (M0 산출물)

- **순수 Kotlin 폴딩 도메인**: `Vec3`·`PaperMesh`·`FoldStep`·`OrigamiModel` + `FoldMeshAtUseCase`
  (progress → 접힌 메시, 로드리게스 회전, 결정적). 유닛테스트 `Vec3Test`·`FoldMeshAtUseCaseTest`.
- **Compose Canvas 소프트웨어 3D 뷰어**(`presentation/viewer/`): 화가 알고리즘 + 램버트 음영 +
  드래그 카메라 오빗 + progress 스크럽 슬라이더 + 계곡 파선 가이드선 + 단계 팁.
- **데모 작품** `DemoOrigami`("반으로 두 번 접기") — Hilt DI(Repository)로 주입.
- **하네스**: CLAUDE.md, 스킬 4(`/wrap-up`·`/emu-qa`·`/add-model`·`/release-aab`), 에이전트 2
  (`fold-auditor`·`origami-researcher`), rules 2(`domain-purity`·`fold-model`), 훅 2(SessionStart·Stop),
  워크트리 규칙, CI, 문서(ARCHITECTURE·FOLD_MODEL·DESIGN·ROADMAP).

## 결정 로그 (최신 위)

- **2026-07-25 · 3D 엔진 = SceneView(Filament) 채택, 단 M0/M1 렌더러는 Compose Canvas 소프트 3D로 시작.**
  근거: 리서치(docs/ARCHITECTURE.md) — 물리 시뮬(Ghassaei)은 튜토리얼 이산 단계와 입도 불일치,
  오서링된 키프레임 + 힌지 회전이 정답. 도메인이 렌더러 독립적이라 Canvas→Filament 무손실 교체.
- **2026-07-25 · 폴딩 모델 = 오서링된 키프레임(힌지 회전 보간), 물리 시뮬 아님.**
- **2026-07-25 · 저작 포맷 = FOLD(JSON) 채택, 런타임은 슬림 OrigamiStep으로 임포트.**
- **2026-07-25 · 프로젝트 위치 = ~/claude/Jupgi, 패키지 = com.jupgi.origami.** (딱풀·체스메이트와 동일 위치 컨벤션)

## 다음 액션 (우선순위 순)

1. **로컬 git 최초 커밋** — M0 골격 전체를 커밋(`feat: M0 골격 …`). 이후 GitHub 원격(AlgoLovers/Jupgi) 연결 여부 결정.
2. **작품 라이브러리 화면(M1)** — 작품 목록 → 뷰어 네비게이션(navigation-compose). 데모 외 작품 1~2개 추가(`/add-model`).
3. **FOLD 임포터(M1)** — `data`에 FOLD(JSON) → OrigamiModel 변환기 + 임포트 테스트.
4. **뷰어 폴리시** — 다크에서 뒷면색 대비 개선, 태블릿 side-by-side 레이아웃(WindowSizeClass), 재생(자동 접기) 버튼.

## 알려진 이슈 / 폴리시 백로그

- 다크 모드에서 종이 뒷면색(surface)이 배경과 가까워 접힌 면 대비가 약함 — 렌더러에서 뒷면 전용 색 지정 필요.
- 완전히 접힌 상태(각 180°)는 레이어가 동일평면이라 z-fighting 소지 — faceOrders 기반 레이어 오프셋은 복합 접기 도입 시 대응.
- 런처 아이콘은 임시 종이비행기 글리프 — 정식 브랜딩 필요.
