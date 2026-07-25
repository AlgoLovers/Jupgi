package com.jupgi.origami.domain.model

/**
 * 접기 배정 — 요시자와·랜들렛 규약. 렌더러가 가이드선 표기(계곡=파선, 산=일점쇄선)를 고른다.
 * (docs/DESIGN.md 다이어그램 규약)
 */
enum class FoldAssignment { VALLEY, MOUNTAIN }

/**
 * 한 개의 접기 단계 = "어떤 정점들을 어느 힌지(크리스 선) 기준으로 몇 도 회전하는가" + 학습 주석.
 *
 * 이것이 이 앱의 폴딩 모델 원전이다. 물리 시뮬이 아니라 **오서링된 키프레임**이라 결정적이고
 * 유닛테스트로 고정된다(docs/FOLD_MODEL.md). FOLD 저작 포맷은 data 계층 임포터가 이 타입으로 변환한다.
 *
 * @param hingeStart 힌지(회전축) 선의 한 끝점 — 현재까지 접힌 좌표계 기준.
 * @param hingeEnd 힌지 선의 다른 끝점.
 * @param movingVertexIndices 이 힌지 기준으로 회전하는 정점 인덱스 집합(나머지는 고정).
 * @param foldAngleDeg 부호 있는 목표 접힘각(도). 양수=계곡, 음수=산 (FOLD edges_foldAngle 규약과 동일).
 * @param assignment 산/계곡 — 가이드선 표기용.
 * @param instruction 이 단계에서 학습자에게 보여줄 한국어 팁.
 */
data class FoldStep(
    val id: String,
    val hingeStart: Vec3,
    val hingeEnd: Vec3,
    val movingVertexIndices: Set<Int>,
    val foldAngleDeg: Float,
    val assignment: FoldAssignment,
    val instruction: String,
)
