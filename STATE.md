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
- **폴딩 불변식 검증 장치** `domain/validation/FoldInvariants` — 변 길이·면적 보존, 위상, NaN,
  가역성, 연속성, 스텝 인코딩(힌지 퇴화·인덱스·각 범위·부호↔산/계곡), 그리고
  `EDGE_CROSSES_HINGE_UNSPLIT`(접는 선 자리에 정점이 없는 결함을 접기 전에 검출).
  `sweep()`이 progress 전 구간을 훑는다 — 작품 추가 시 통과 필수(LOOP-2 관문).
- **하네스**: CLAUDE.md, 스킬 4(`/wrap-up`·`/emu-qa`·`/add-model`·`/release-aab`), 에이전트 2
  (`fold-auditor`·`origami-researcher`), rules 2(`domain-purity`·`fold-model`), 훅 2(SessionStart·Stop),
  워크트리 규칙, CI, 문서(ARCHITECTURE·FOLD_MODEL·DESIGN·ROADMAP).

## 결정 로그 (최신 위)

- **2026-07-25 · 작품 검증은 "종이 물리 불변식"으로 한다 — 회전 수학 테스트로는 부족하다.**
  접기는 강체 회전이라 변 길이·면적이 절대 불변이라는 점을 관문으로 삼는다. 스텝 데이터가
  틀리면(힌지 위치·movingVertexIndices) 수학은 통과하는데 종이가 늘어나는데, 이걸 `FoldInvariants`가
  잡는다. 실제로 도입 즉시 4정점 정사각형 픽스처의 "접을 수 없는 접기"를 검출했다.
- **2026-07-25 · 원격 = github.com/AlgoLovers/Jupgi (PRIVATE→PUBLIC) 연결·`main` 푸시 완료.**
  형제 리포(ddakpul·Checkmatey)는 PUBLIC이지만, 공개는 비가역(클론·인덱싱)이라 private으로 시작.
  공개 전환은 `gh repo edit AlgoLovers/Jupgi --visibility public --accept-visibility-change-consequences`.
- **2026-07-25 · 3D 엔진 = SceneView(Filament) 채택, 단 M0/M1 렌더러는 Compose Canvas 소프트 3D로 시작.**
  근거: 리서치(docs/ARCHITECTURE.md) — 물리 시뮬(Ghassaei)은 튜토리얼 이산 단계와 입도 불일치,
  오서링된 키프레임 + 힌지 회전이 정답. 도메인이 렌더러 독립적이라 Canvas→Filament 무손실 교체.
- **2026-07-25 · 폴딩 모델 = 오서링된 키프레임(힌지 회전 보간), 물리 시뮬 아님.**
- **2026-07-25 · 저작 포맷 = FOLD(JSON) 채택, 런타임은 슬림 OrigamiStep으로 임포트.**
- **2026-07-25 · 프로젝트 위치 = ~/claude/Jupgi, 패키지 = com.jupgi.origami.** (딱풀·체스메이트와 동일 위치 컨벤션)

## 다음 액션 (우선순위 순)

1. **M1 작품 5종 저작** — 계곡/산 접기만으로 완결되는 것부터: 종이비행기 · 컵 · 하트 · 부채 · 딱지.
   전부 전통(퍼블릭 도메인). **학·개구리는 꽃잎접기+역접기가 필요해 M2**로 미룬다.
   ⚠️ "끼우기(tuck)"는 힌지 회전으로 표현되지 않는다 — 딱지 마지막 단계 처리 방식을 먼저 정할 것.
   각 작품은 `FoldInvariants.sweep()` 통과가 수용 기준.
2. **작품 라이브러리 화면(M1)** — 작품 목록 → 뷰어 네비게이션(navigation-compose).
2. **FOLD 임포터(M1)** — `data`에 FOLD(JSON) → OrigamiModel 변환기 + 임포트 테스트.
3. **뷰어 폴리시** — 다크에서 뒷면색 대비 개선, 태블릿 side-by-side 레이아웃(WindowSizeClass), 재생(자동 접기) 버튼.
4. **브랜치 전략 확정** — 현재 `main` 단독. 딱풀식 `main`(배포)/`develop`(통합)/`feature/*`로 갈지 결정.

## 알려진 이슈 / 폴리시 백로그

- 다크 모드에서 종이 뒷면색(surface)이 배경과 가까워 접힌 면 대비가 약함 — 렌더러에서 뒷면 전용 색 지정 필요.
- 완전히 접힌 상태(각 180°)는 레이어가 동일평면이라 z-fighting 소지 — faceOrders 기반 레이어 오프셋은 복합 접기 도입 시 대응.
- 런처 아이콘은 임시 종이비행기 글리프 — 정식 브랜딩 필요.
- **`FoldMeshAtUseCaseTest`의 4정점 정사각형 픽스처는 물리적으로 접을 수 없는 모델이다** —
  힌지(x=0)가 면을 가로지르는데 그 자리에 정점이 없다. 회전 수학만 검증하므로 테스트는 통과하지만,
  `FoldInvariants.checkHingeSplitting`으로 보면 위반이다. 픽스처를 분할 메시로 교체할 것
  (`FoldInvariantsTest.splitSquare`가 올바른 형태).
- **작품 저작 시 도안 저작권 주의**: 전통 모델(학·개구리·배 등)은 퍼블릭 도메인이라 자유롭지만,
  **책·웹의 다이어그램은 그린 사람의 저작물**이다(OrigamiUSA). 단계 분할·그림을 옮기지 말고
  직접 접어 FOLD를 저작한다. 현대 작가 작품은 "변형해서 회피"가 불가 — 파생저작물이 되고,
  기하 제약(마에카와·카와사키 정리)을 깨서 접히지도 않는다. `/add-model`·`fold-auditor`에 반영 필요.
- **SceneView 교체(M2+) 선결 조건** (2026-07-25 Maven POM 직접 확인):
  `io.github.sceneview:sceneview:4.25.0`은 **`kotlin-stdlib 2.4.10`에 의존** — 현재 카탈로그가
  Kotlin 2.0.21이라 그대로 붙이면 메타데이터 비호환으로 깨진다. Kotlin·KSP·Hilt·AGP 동반 상향 필요
  (AGP 8.x 최신 8.13.2). Compose BOM은 SceneView가 참조하는 2026.06.01, 번들 Filament 1.72.1, minSdk 24.
  `LineNode`/`PathNode`가 1급 API라 3D 크리스 가이드선을 직접 구현할 필요 없고, 동적 폴딩 메시는
  `MeshNode(primitiveType, vertexBuffer, indexBuffer, …)`로 버퍼를 직접 제어한다.
  ⚠️ **함정**: sceneview 이슈 #1841 — 매 프레임 정점 업로드 시 ByteBuffer 캐시 재사용이 Filament의
  비동기 버퍼 수명 계약을 위반해 메시가 찢어진다(PR #1851 수정). `setBufferAt` 콜백 + 2개 링버퍼
  패턴을 `.claude/rules/`에 명문화할 것. SceneView는 MCP 서버·`llms.txt`·컴파일 샘플 33개를 제공한다.
