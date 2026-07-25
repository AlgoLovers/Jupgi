# ARCHITECTURE.md — 3D 폴딩 아키텍처 결정 (ADR)

> 이 결정들은 리서치 근거로 확정됐다. 재논의하려면 여기 근거를 반박하는 새 근거부터 가져온다.

## 한 문장 아키텍처

**폴딩 기하는 순수 Kotlin domain에서 결정적으로 계산하고(오서링된 키프레임 + 로드리게스 힌지 회전),
3D 엔진은 그 메시를 그리기만 하는 얇은 뷰다.** 이 경계가 렌더러 무손실 교체를 가능케 한다.

```
domain(순수 Kotlin)                    presentation(뷰)
OrigamiModel ──FoldMeshAtUseCase──▶ PaperMesh ──▶ [렌더러: Canvas → Filament]
(progress)     (로드리게스, 결정적)   (정점+면)      (그리기 + 카메라 오빗)
```

## ADR-1 · 3D 엔진 = SceneView(Google Filament), MVP는 Compose Canvas 소프트 3D

**결정**: 프로덕션 3D 엔진은 **SceneView(Filament)**. 단 M0/M1 렌더러는 **Compose Canvas 소프트웨어
3D**로 시작하고, 도메인이 렌더러 독립적이므로 이후 Filament로 무손실 교체한다.

**비교 요약** (리서치):

| 옵션 | 판정 | 이유 |
|---|---|---|
| **SceneView / Filament** ✅ | 채택(프로덕션) | 유일한 Compose-네이티브 3D. 동적 VertexBuffer로 매 프레임 정점 갱신 → 스크럽·역재생 직접 제어. 활발히 유지보수(2026). 얇은 렌더러 계획에 정확히 부합. |
| **Compose Canvas 소프트 3D** ✅ | 채택(MVP 시작점) | 의존성 0, 전부 Kotlin, 가이드선/주석을 같은 Canvas에 즉시. 저면수 모델에 충분. 도메인과 분리돼 있어 Filament로 무손실 교체 가능. |
| Three.js WebView (+Ghassaei) ❌ | 반려 | 시뮬레이터는 크리스를 **동시에** 접음(순차 단계 아님) → 튜토리얼과 패러다임 불일치. 지오메트리가 JS로 이동해 순수-Kotlin 도메인·테스트 계획을 깸. |
| OpenGL ES 직접 | 반려 | Filament를 재발명. (a)의 하부일 뿐. |
| Godot / Unity 임베드 ❌ | 반려 | 단일 인스턴스·풀스크린·앱 용량 급증(Unity 빈 프로젝트도 ~60MB). Compose+Clean Architecture 하네스와 단절. 뷰어에 과함. |

**폴백**: (1) SceneView 추상화/버스팩터 문제 시 **Filament 직접 사용**(도메인 그대로). (2) 최경량은
**Canvas 소프트 3D 유지**. LibGDX는 Compose 통합 손해를 감수할 때만.

## ADR-2 · 폴딩 모델 = 오서링된 키프레임 (물리 시뮬 아님)

**결정**: 각 단계를 `(힌지축, 움직이는 정점 집합, 부호 있는 접힘각 θ, 산/계곡, 팁)`으로 인코딩하고,
파라미터 t∈[0,1]에서 움직이는 정점을 힌지 기준 **로드리게스 회전**으로 변환한다.

**근거**: 물리 솔버(Ghassaei)는 완성된 크리스 패턴 전체를 동시에 접어 평면접기 가능성을 탐색하는
도구다. 비결정적 성향이라 "의미 있는 단계에서 일시정지 · 이 접기만 강조 · 정밀 스크럽 · 역재생"이
어렵고 GPU 물리가 과하다 — **튜토리얼의 이산 단계와 입도가 어긋난다.** 키프레임 방식은 결정적·
순수 Kotlin·유닛테스트 가능이라 도메인 순수성 계획에 1:1로 부합한다.

## ADR-3 · 저작 포맷 = FOLD(JSON), 런타임은 슬림 OrigamiStep

**결정**: 정식 작품은 FOLD(Demaine 외)로 저작·검증·교환하고, **data 계층 임포터가 런타임
`OrigamiModel`로 변환**한다. FOLD를 런타임 애니메이션 포맷으로 직접 쓰지 않는다.

**근거**: FOLD 프레임은 "접힌 상태(정점 좌표)"만 준다. 프레임 간 정점 좌표를 선형 보간하면 종이가
늘어나고 관통한다. 올바른 보간은 힌지 회전이라 "어떤 정점을 어느 힌지로 t·θ 회전"이라는 한 층 위
정보가 필요하다. FOLD의 표준·툴링 이점(Oriedita·ORIPA·Rabbit Ear 저작)은 얻되 런타임은 애니메이션
최적화 표현을 쓴다. 상세: [`FOLD_MODEL.md`](FOLD_MODEL.md).

## 가장 큰 기술 리스크 (반복 명시)

**렌더링이 아니라 다층 종이 지오메트리.** (1) 역접기·안팎뒤집기·스쿼시·꽃잎접기에서 "어느 정점이
어느 힌지로 움직이는가"의 정확한 인코딩, (2) 겹친 레이어 순서(FOLD `faceOrders`)·동일평면 z-fighting,
(3) 자기관통 방지, (4) 이 모두를 결정적·테스트 가능하게 유지. **완화책**: 복잡한 접기를 FOLD로
저작·검증하고, 단순 산·계곡접기부터 지원 범위를 명시해 점진 확대한다(`fold-auditor`가 감사).

## 근거 출처 (리서치)

- SceneView: github.com/SceneView/sceneview-android · Filament: github.com/google/filament
- FOLD 스펙: github.com/edemaine/fold · Rabbit Ear: rabbitear.org · Oriedita: oriedita.github.io
- Origami Simulator(Ghassaei): github.com/amandaghassaei/OrigamiSimulator · origamisimulator.org
- 요시자와·랜들렛 규약: langorigami.com/article/origami-diagramming-conventions
