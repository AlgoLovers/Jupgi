# FOLD_MODEL.md — 폴딩 모델 스펙

폴딩 계산의 원전 정의. 코드 원전은 `domain/model/`·`domain/usecase/FoldMeshAtUseCase.kt`이며,
여기 옮겨 적지 않는다 — 이 문서는 "왜 이렇게 되어 있는가"와 불변식을 설명한다.

## 런타임 표현 (domain)

```
OrigamiModel { id, title, difficulty, base: PaperMesh, steps: List<FoldStep> }
PaperMesh    { vertices: List<Vec3>, faces: List<Face(a,b,c)> }   // 삼각형, 정점 CCW
FoldStep     { hingeStart, hingeEnd, movingVertexIndices, foldAngleDeg, assignment, instruction }
```

- **progress ∈ [0, stepCount]**: 정수부 = 완전히 적용된 단계, 소수부 = 현재 단계 보간 t.
  이 스칼라 하나로 일시정지·전/후진·스크럽이 전부 표현된다.
- `FoldMeshAtUseCase(model, progress)`는 base부터 시작해 완료 단계를 t=1로, 현재 단계를 t=frac로
  누적 적용한다. **순수 함수·결정적** → 유닛테스트로 고정(`FoldMeshAtUseCaseTest`).

## 접힘 수학 — 로드리게스 회전

움직이는 정점 p를, `hingeStart`를 지나고 방향이 단위벡터 `k = normalize(hingeEnd − hingeStart)`인
축 기준으로 각 φ = θ·t 만큼 회전:

```
v = p − hingeStart
p' = hingeStart + v·cosφ + (k×v)·sinφ + k·(k·v)·(1−cosφ)
```

- **부호 규약**: θ(=`foldAngleDeg`) 양수=계곡(valley), 음수=산(mountain). FOLD `edges_foldAngle`와 동일.
- 회전축이 y축과 평행이면 정점의 y가 보존되는 등, 축 선택이 다음 단계의 힌지 유효성에 영향을 준다.

## 불변식 (rules/fold-model.md 와 동일 — 감사 대상)

1. **힌지 끝점은 접히지 않는 정점 위**에 둔다. 스텝은 순서대로 누적되며, 각 힌지는 "직전 단계까지
   접힌 좌표계"에서 실제 크리스 선 위에 있어야 한다. (데모는 x=0 열·y=0 행 고정 정점에 둠.)
2. `movingVertexIndices`는 힌지 한쪽 반평면의 정점만. 힌지 위 공유 정점은 넣지 않는다.
3. 스텝은 `vertices`만 바꾸고 `faces` 위상은 보존한다(종이는 찢어지지 않는다).
4. `foldAngleDeg` 부호와 `assignment`가 모순되지 않는다.

## FOLD → OrigamiModel 임포트 (data 계층, M1 예정)

FOLD(JSON)는 저작·검증·교환용. 임포터가 읽는 필드:

| FOLD 필드 | 쓰임 |
|---|---|
| `vertices_coords` | 정점 좌표 → PaperMesh.vertices (삼각화 후) |
| `edges_vertices` | 에지 위상 |
| `faces_vertices` | 면(반시계) → PaperMesh.faces (삼각형 분할) |
| `edges_assignment` (M/V/B/F) | 산/계곡/경계/평면 → 가이드선 표기 |
| `edges_foldAngle` (−180..180) | 접힘각 부호·크기 |
| `faceOrders` `[f,g,s]` | 겹친 레이어 앞뒤 순서 (자기관통·z-fighting 대응의 핵심) |
| `file_frames` + `frame_inherit` | 접기 단계 시퀀스(상속으로 중복 제거) |

**왜 raw FOLD를 런타임에 쓰지 않나**: 프레임 간 정점 좌표 선형 보간은 종이를 늘리고 관통시킨다.
올바른 보간은 힌지 회전이라, "어떤 정점 집합을 어느 힌지로 t·θ 회전"이라는 정보(FoldStep)가 필요하다 —
이는 FOLD보다 한 층 위다. 임포터가 FOLD의 프레임 차이에서 이 힌지·각·움직임 집합을 추출한다.

## 지원 범위 (점진 확대)

- **지금**: 단순 산·계곡접기(반접기·대각접기). 힌지 회전으로 결정적.
- **다음**: 안팎뒤집기·스쿼시·싱크·꽃잎접기. 각 종류는 (a) `origami-researcher` 근거 조사,
  (b) 결정적 솔버·레이어 순서 처리, (c) 테스트, (d) `fold-auditor` 감사 후에 넣는다.
- 지원 범위를 좁게 유지하는 것이 틀리게 접히는 작품을 내는 것보다 낫다.
