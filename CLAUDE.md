# CLAUDE.md — 접기(Jupgi) 프로젝트 헌법

**접기(Jupgi)** — **3D로 따라 접는 종이접기 학습 앱.**
유튜브 영상·책으로 종이접기를 배우면 복잡해질수록 따라 접기 어렵다. 접기는 **3D 종이 모델을
언제든 멈추고 · 한 단계씩 전/후진하고 · 자유롭게 돌려보며 · 단계마다 가이드선과 팁**을 제공해
이 페인 포인트를 해결한다.

- **타깃**: 종이접기를 배우고 싶은 누구나(입문~고급). 디바이스: 안드로이드 폰/태블릿.
- **핵심 UX**: 재생 위치(progress) 하나로 일시정지·스텝·스크럽이 통일된다. 카메라는 항상 자유 오빗.
- 패키지 `com.jupgi.origami` · 리포 github.com/AlgoLovers/Jupgi (원격 미연결 — 로컬 git 부터).

## 핵심 원칙 (절대 위반 금지)

1. **런타임 AI 호출 비용 0원** — 폴딩은 순수 규칙 기반(로드리게스 회전), 온디바이스 완결.
2. **작품은 사전 저작·앱 내장** — 실시간 물리 생성 금지. FOLD 저작 → 도메인 스텝으로 변환.
3. **오프라인 우선** — 네트워크 없이 핵심 기능 전부 동작.
4. **Domain 계층에 `android.*` import 금지** — 폴딩 기하는 순수 Kotlin, 유닛테스트로 고정
   (상세: `.claude/rules/domain-purity.md`).
5. 의존성 방향: Presentation/Data → Domain. 역방향 금지.
6. **폴딩 기하 = domain(계산), 3D 엔진 = 얇은 뷰(그리기만).** 이 분리 덕에 렌더러를 무손실
   교체할 수 있다(M1 Compose Canvas 소프트 3D → M2 SceneView/Filament). 지오메트리를 렌더러
   코드로 새어 나가게 하지 말 것.
7. **실행 코드(.so/dex)는 런타임 다운로드 금지**(Play 정책). Filament 도입 시 네이티브 .so는 앱과
   함께 배포한다 — 데이터(작품 FOLD/JSON)만 파일로 다룬다.

## 기술 스택 · 구조

Kotlin · Jetpack Compose(M3) · Clean Architecture 3계층 + MVVM · Hilt · (Room 예정) ·
WindowSizeClass 반응형 · minSdk 26 · JDK 17 · Gradle KTS + Version Catalog.

- **3D 렌더링**: M1 = Compose Canvas 소프트웨어 3D(의존성 0), M2 = SceneView(Google Filament).
  선택 근거·트레이드오프는 `docs/ARCHITECTURE.md`(ADR).
- **폴딩 모델**: 오서링된 키프레임 + 힌지(크리스 선) 회전 보간. 물리 시뮬 아님. `docs/FOLD_MODEL.md`.
- **저작 포맷**: FOLD(Demaine 외, JSON). 런타임은 슬림 `OrigamiStep` 으로 임포트해 쓴다.

```
app/src/main/java/com/jupgi/origami/
├── core/ (designsystem·di)          ├── data/ (sample·repository — 이후 assets FOLD 임포터)
├── domain/ (model·repository IF·usecase — 순수 Kotlin ⭐ 폴딩 기하)
└── presentation/ (viewer + 이후 목록·설정 …)
```

도메인 원전은 `domain/model/`(Vec3·PaperMesh·FoldStep·OrigamiModel) — 여기 옮겨 적지 않는다.
폴딩 계산의 심장은 `domain/usecase/FoldMeshAtUseCase.kt`(progress → 접힌 메시, 결정적·유닛테스트).

## 폴딩 엔진 (전부 순수 함수 + 단위 테스트)

- 각 단계 = `(힌지축=크리스 선, 움직이는 정점 집합, 부호 있는 접힘각 θ, 산/계곡, 팁)`.
- 전역 progress ∈ [0, stepCount]: 정수부=완료 단계, 소수부=현재 단계 보간 t. 이 값 하나로
  일시정지·전/후진·스크럽이 전부 해결된다.
- 회전은 **로드리게스 회전 공식**(`FoldMeshAtUseCase.rotateAboutAxis`). 규칙/스텝을 바꾸면
  `FoldMeshAtUseCaseTest`·이 문서·`docs/FOLD_MODEL.md`를 함께 갱신.
- ⚠️ **가장 큰 리스크는 렌더링이 아니라 다층 종이 지오메트리** — 레이어 순서(FOLD faceOrders)·
  동일평면 z-fighting·자기관통. 초기엔 단순 산·계곡접기부터 지원 범위를 명시하고 점진 확대한다.

## 코딩 규칙

- 불변 우선(`val`), `!!` 금지, 매직값 금지(상수/enum), 생성자 주입만.
- ViewModel은 UseCase만 호출(Repository 직접 호출은 로드 시 모델 획득에 한정). UI State =
  단일 불변 객체 + `StateFlow`. Compose에 비즈니스 로직 금지.
- 카메라(yaw/pitch)는 순수 뷰 관심사 → 화면 로컬 상태. 재생 위치(progress)는 ViewModel.
- Git: `main`(배포, PR+CI로만) / `develop`(통합) / `feature/*`.
  Conventional Commits(`feat|fix|refactor|test|docs|chore(scope): 한국어 요약`), 한 커밋 = 한 논리 변경.

## 하네스 (스킬·에이전트·훅·규칙)

- **스킬** `.claude/skills/`: `/wrap-up`(턴 종료 의식) · `/emu-qa`(에뮬 스샷 QA — 3D는 눈으로 봐야 안다) ·
  `/add-model`(작품 저작·검증 파이프라인) · `/release-aab`(서명 번들, 사용자 호출 전용). 반복 절차는 여기에 명문화.
- **에이전트** `.claude/agents/`: `fold-auditor`(작품 콘텐츠 감사, 읽기 전용 — 작품 추가·수정 후 필수) ·
  `origami-researcher`(접기 기법·전산 종이접기·FOLD 리서치). ⚠️ 에이전트 모델은 opus/sonnet/haiku만 — **fable 금지**(유료 크레딧).
- **훅** (`.claude/settings.json` → `tools/claude/hooks/`): SessionStart가 git 동기화 상태 주입,
  Stop이 더티 트리를 경고. 훅이 알려주는 상태를 무시하지 말 것.
- **경로 규칙** `.claude/rules/`: `domain-purity`(도메인 순수성) · `fold-model`(폴딩/작품 불변식).

## 세션 조율 (터미널 + 텔레그램 상주 세션이 한 리포 공유 — 딱풀·체스메이트와 동일)

- **턴 끝 = 깨끗한 트리**: `/wrap-up`으로 spotless→detekt→(도메인 변경 시)test→커밋→푸시까지.
- 무거운 다중 파일 작업은 한 창구에서. 동시 편집이 필요하면 한쪽은 **git worktree로 격리**
  (`.claude/worktrees/`는 gitignore, 필요한 로컬 파일은 `.worktreeinclude` — 키스토어는 절대 포함 금지).
- **커밋 금지**: `keystore.properties`, `*.keystore`(업로드 키 — 잃으면 업데이트 불가), `.aab`/`.apk`,
  `local.properties`. 권한 deny로도 막혀 있음.
- **큰 빌드 파일(.aab/.apk)은 텔레그램 첨부 금지**(봇 한계 50MB). GitHub 릴리스에 올려 링크만 전달.

## 작업 방식

- **수직 슬라이스**(한 기능 관통) 단위로 진행, 단계마다 커밋.
- Domain UseCase는 반드시 단위 테스트 동반 — 특히 `FoldMeshAtUseCase`는 회전/보간/클램프가
  테스트로 고정돼 있다.
- 3D·UI 변경은 `/emu-qa`로 스크린샷 자가 검증 — **라이트/다크 둘 다, 폰/태블릿 둘 다**(`docs/DESIGN.md`).
- 새 작품은 `/add-model`로 저작·검증하고 `fold-auditor`로 감사한다.
- 새 접기 기법·교육 방식은 `origami-researcher`로 근거 조사 후 설계.
- 현재 상태·다음 액션은 `STATE.md`, 루프 계약은 `LOOP.md`, 안전 경계는 `loop-constraints.md`, 방향은 `docs/ROADMAP.md`.

## 어떻게 소통하길 원하는가

- 직접적·짧게·구체적으로. 파일명·함수명·줄번호로 말한다. 깨진 건 깨졌다고 그대로 보고한다.
- 답변 끝은 요약이 아니라 **다음 액션**으로.
