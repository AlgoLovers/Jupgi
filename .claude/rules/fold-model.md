---
paths:
  - "app/src/main/java/com/jupgi/origami/domain/model/**"
  - "app/src/main/java/com/jupgi/origami/data/sample/**"
  - "app/src/main/assets/**"
---

# 폴딩 모델 · 작품 데이터 불변식 (이 파일들을 만질 때 필수)

작품(OrigamiModel)은 이 앱의 콘텐츠다. 여기가 깨지면 렌더가 아니라 "접기가 틀린" 것이라 치명적이다.
작품을 추가·수정하면 `fold-auditor` 에이전트로 감사하고, 가능하면 `/emu-qa`로 각 단계를 눈으로 본다.

## 스텝 인코딩 불변식

- 각 `FoldStep`은 `(hingeStart, hingeEnd, movingVertexIndices, foldAngleDeg, assignment, instruction)`.
- **힌지 끝점은 접히지 않는 정점 위에 둔다.** 힌지가 이전 단계에서 움직인 정점 위에 있으면 base
  좌표계 기준으로 무효가 된다(스텝은 순서대로 누적 적용되며, 각 힌지는 "직전 단계까지 접힌
  좌표계"에서 유효해야 한다). 데모(`DemoOrigami`)는 힌지를 x=0 열·y=0 행의 고정 정점에 둬서 이를 지킨다.
- **부호 규약**: `foldAngleDeg` 양수=계곡(valley), 음수=산(mountain). FOLD `edges_foldAngle`와 동일.
  `assignment`(가이드선 표기용)와 부호가 모순되지 않게 한다.
- `movingVertexIndices`는 힌지 한쪽 반평면의 정점만. 힌지 위 정점(양쪽 공유)은 넣지 않는다.
- 종이는 찢어지지 않는다 — 스텝은 `vertices`만 바꾸고 `faces` 위상은 보존한다.

## FOLD 포맷 (저작/교환 — docs/FOLD_MODEL.md)

- 정식 작품은 FOLD(JSON)로 저작·검증하고, **data 계층 임포터가 `OrigamiModel`(런타임)로 변환**한다.
  FOLD를 런타임 애니메이션 포맷으로 직접 쓰지 않는다(프레임 간 정점 선형보간은 종이를 늘리고 관통시킴).
- 임포터는 FOLD의 `vertices_coords`·`edges_vertices`·`faces_vertices`·`edges_assignment`·
  `edges_foldAngle`·`faceOrders`·`file_frames`를 읽어 힌지+각+주석 스텝으로 변환한다.
- **레이어 순서(faceOrders)와 자기관통**은 이 프로젝트의 최대 난제다(CLAUDE.md). 새 접기 종류를
  지원 범위에 넣기 전에 `origami-researcher`로 근거를 조사하고, 단순 산·계곡접기부터 점진 확대한다.

## i18n

- 사용자 노출 문자열(작품 제목·팁·`instruction`)은 최종적으로 `res/values*/strings.xml` 또는
  작품 데이터의 언어별 필드로 관리한다(ko 우선, en 확장 대비). 도메인 모델에 하드코딩된 한국어는
  데모 골격에 한함 — 콘텐츠 파이프라인 도입 시 외부화한다.
