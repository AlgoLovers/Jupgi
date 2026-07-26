package com.jupgi.origami.domain.model

/**
 * 하나의 종이접기 작품 = 시작 종이(base) + 순서 있는 접기 단계들 + 메타데이터.
 *
 * @param difficulty 1(입문)~5(고급). 난이도별 스텝 수·접기 종류 배치는 fold-auditor 가 감사한다.
 */
data class OrigamiModel(
    val id: String,
    val title: String,
    val difficulty: Int,
    val category: OrigamiCategory,
    val base: PaperMesh,
    val steps: List<FoldStep>,
) {
    val stepCount: Int get() = steps.size
}
