package com.jupgi.origami.data.sample

import com.google.common.truth.Truth.assertThat
import com.jupgi.origami.domain.usecase.FoldMeshAtUseCase
import com.jupgi.origami.domain.validation.FoldInvariants
import org.junit.Test

/**
 * 실제 작품이 "진짜 접히는가"를 검사하는 관문. 작품을 추가할 때마다 여기에 한 줄 추가한다
 * (LOOP-2 계약: 솔버 검증 통과 없이는 작품을 지원 범위에 넣지 않는다).
 */
class DemoOrigamiInvariantTest {
    private val foldMeshAt = FoldMeshAtUseCase()

    @Test
    fun demoModelSurvivesFullProgressSweep() {
        val model = DemoOrigami.model()
        val violations = FoldInvariants.sweep(model) { foldMeshAt(model, it) }
        assertThat(violations.joinToString("\n") { "${it.kind}: ${it.detail}" }).isEmpty()
    }

    @Test
    fun demoStepEncodingFollowsRules() {
        assertThat(FoldInvariants.checkStepEncoding(DemoOrigami.model())).isEmpty()
    }
}
