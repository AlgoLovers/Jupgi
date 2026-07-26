package com.jupgi.origami.data.sample

import com.google.common.truth.Truth.assertThat
import com.jupgi.origami.data.repository.SampleOrigamiRepository
import com.jupgi.origami.domain.usecase.FoldMeshAtUseCase
import com.jupgi.origami.domain.validation.FoldInvariants
import org.junit.Test

/**
 * 번들 작품 전체의 관문 — "진짜 접히는 작품인가"(LOOP-2 계약).
 * 작품을 추가하면 리포지토리에 넣는 것만으로 자동으로 이 관문을 통과해야 한다.
 */
class DemoOrigamiInvariantTest {
    private val foldMeshAt = FoldMeshAtUseCase()
    private val repository = SampleOrigamiRepository()

    @Test
    fun everyBundledModelSurvivesFullProgressSweep() {
        val failures =
            repository.models().flatMap { model ->
                FoldInvariants
                    .sweep(model) { foldMeshAt(model, it) }
                    .map { "[${model.id}] ${it.kind}: ${it.detail}" }
            }
        assertThat(failures.joinToString("\n")).isEmpty()
    }

    @Test
    fun everyBundledModelHasValidStepEncoding() {
        repository.models().forEach { model ->
            assertThat(FoldInvariants.checkStepEncoding(model)).isEmpty()
        }
    }

    @Test
    fun bundleHasAtLeastFourModelsAcrossThreeCategories() {
        val models = repository.models()
        assertThat(models.size).isAtLeast(4)
        assertThat(models.map { it.category }.toSet().size).isAtLeast(3)
        assertThat(models.map { it.id }).containsNoDuplicates()
    }
}
