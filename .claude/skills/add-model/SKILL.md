---
name: add-model
description: 새 종이접기 작품을 저작·검증·등록하는 콘텐츠 파이프라인. FOLD/OrigamiModel 스텝을 만들고, 폴딩 솔버로 기하를 검증하고, fold-auditor로 감사하고, /emu-qa로 각 단계를 눈으로 본 뒤 라이브러리에 추가한다.
---

# /add-model — 작품 저작·검증 파이프라인

작품(OrigamiModel)은 이 앱의 콘텐츠다. "접기가 틀린" 작품은 렌더 버그보다 치명적이라, 저작은
반드시 **솔버 검증 + 감사 + 눈 검증**을 통과해야 라이브러리에 들어간다. 규칙: `.claude/rules/fold-model.md`,
설계: `docs/FOLD_MODEL.md`.

## 절차

1. **저작**: 작품을 FOLD(JSON, 권장 — Oriedita/ORIPA/Rabbit Ear로 저작·검증 가능) 또는 직접
   `OrigamiModel` 스텝으로 만든다. 각 스텝 = `(hingeStart, hingeEnd, movingVertexIndices, foldAngleDeg,
   assignment, instruction)`. 힌지 끝점은 접히지 않는 정점 위에 둔다(불변식).
2. **임포트**(FOLD인 경우): data 계층 임포터로 `OrigamiModel`로 변환. 정점 수·스텝 수가 원본과
   정합하는지 확인.
3. **솔버 검증**: `FoldMeshAtUseCase(model, progress)`를 progress 0→stepCount로 훑어
   (a) 예외 없이 계산되는지, (b) 접힘각 부호·움직임 집합이 의도대로인지, (c) 눈에 띄는 자기관통이
   없는지 확인한다. 검증 로직이 있는 영역에서만 배치 루프를 돌린다. **결정적 솔버가 없는 접기 종류는
   먼저 `origami-researcher`로 근거 조사 후 지원 범위에 넣는다.**
4. **감사**: `fold-auditor` 에이전트로 스텝 인코딩·따라접기 가능성·난이도 배치·레이어 순서 리스크를
   감사한다(작품 추가·수정 후 필수).
5. **눈 검증**: `/emu-qa`로 각 단계(0/중간/완료)를 라이트·다크에서 스샷 확인 — 접힘·양면색·깊이·
   가이드선(계곡 파선/산 일점쇄선)이 맞는지.
6. **등록**: 작품을 `app/src/main/assets/`(FOLD/JSON) 또는 리포지토리에 추가하고, 목록·난이도 배치를
   갱신한다. 커밋은 `/wrap-up`.

## 지원 범위 (점진 확대 — 무리하지 말 것)

- 지금: 단순 **산·계곡접기**(반접기·대각접기). 힌지 회전 보간으로 결정적.
- 다음 후보: 안팎뒤집기(inside/outside reverse)·스쿼시. **레이어 순서(faceOrders)·자기관통이
  진짜 난제**(CLAUDE.md) — 각 종류를 넣기 전에 리서치·솔버 검증·테스트를 먼저 세운다.
- 지원 범위를 좁힌 채 두는 것이, 틀리게 접히는 작품을 내는 것보다 낫다.
