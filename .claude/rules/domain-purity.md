---
paths:
  - "app/src/main/java/com/jupgi/origami/domain/**"
---

# Domain 계층 규칙 (이 파일들을 만질 때 필수)

- **`android.*`, `androidx.*` import 절대 금지** — 순수 Kotlin/JVM만. 의존 방향은
  Presentation/Data → Domain 단방향(역방향 금지). 폴딩 기하가 순수하므로 `testDebugUnitTest`로
  빠르게 검증된다.
- 폴딩 계산은 전부 **결정적 순수 함수**. 같은 (model, progress)면 항상 같은 메시 → 유닛테스트로 고정.
  회전에 `Math`/`kotlin.math`는 허용(플랫폼 무관). 시간·난수·전역 상태 금지.
- UseCase는 단일 책임 + `operator fun invoke` 하나만 노출. 생성자 주입만.
- **UseCase를 추가·수정하면 단위 테스트 동반 필수.** 특히 `FoldMeshAtUseCase`는 회전(로드리게스)·
  단계 누적·보간·progress 클램프가 각각 테스트로 고정돼 있다 — 로직을 바꾸면 테스트도,
  `CLAUDE.md` 폴딩 엔진 절도, `docs/FOLD_MODEL.md`도 함께 갱신한다.
- 매직값 금지: 상수/enum으로. `!!` 금지, 불변 우선(`val`).
- **3D 엔진 타입(Filament/SceneView/OpenGL)·Compose 타입을 domain에 들이지 말 것.** 렌더러는
  domain이 낸 `PaperMesh`(정점+면)만 받는다 — 이 경계가 렌더러 교체(Canvas→Filament)를 가능케 한다.
