package com.jupgi.origami.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.jupgi.origami.data.sample.DemoOrigami
import com.jupgi.origami.domain.model.Face
import com.jupgi.origami.domain.model.FoldAssignment
import com.jupgi.origami.domain.model.FoldStep
import com.jupgi.origami.domain.model.OrigamiModel
import com.jupgi.origami.domain.model.PaperMesh
import com.jupgi.origami.domain.model.Vec3
import org.junit.Test

class ComputeLayerOrderUseCaseTest {
    private val computeLayerOrder = ComputeLayerOrderUseCase()

    /**
     * 사분면 하나씩 면이 있는 종이. 인덱스: 0=좌하, 1=우하, 2=우상, 3=좌상.
     * 정점은 사분면마다 독립으로 둬(공유 없음) 어느 면이 어느 단계에서 움직이는지 명확히 한다.
     */
    private fun quadrantModel(): OrigamiModel {
        val v = mutableListOf<Vec3>()
        val faces = mutableListOf<Face>()
        // 각 사분면의 대표 정점 3개씩 (좌하, 우하, 우상, 좌상 순)
        val corners =
            listOf(
                Triple(-1f, -1f, 0f), // 좌하
                Triple(1f, -1f, 0f), // 우하
                Triple(1f, 1f, 0f), // 우상
                Triple(-1f, 1f, 0f), // 좌상
            )
        corners.forEach { (x, y, _) ->
            val base = v.size
            v += Vec3(x, y, 0f)
            v += Vec3(x / 2f, y, 0f)
            v += Vec3(x, y / 2f, 0f)
            faces += Face(base, base + 1, base + 2)
        }
        val leftVertices = v.indices.filter { v[it].x < 0f }.toSet()
        val topVertices = v.indices.filter { v[it].y > 0f }.toSet()
        return OrigamiModel(
            id = "q",
            title = "q",
            difficulty = 1,
            base = PaperMesh(v, faces),
            steps =
                listOf(
                    FoldStep(
                        "s0",
                        Vec3(0f, -1f, 0f),
                        Vec3(0f, 1f, 0f),
                        leftVertices,
                        180f,
                        FoldAssignment.VALLEY,
                        "",
                    ),
                    FoldStep(
                        "s1",
                        Vec3(0f, 0f, 0f),
                        Vec3(1f, 0f, 0f),
                        topVertices,
                        180f,
                        FoldAssignment.VALLEY,
                        "",
                    ),
                ),
        )
    }

    @Test
    fun stacksLaterFoldsOnTop() {
        val layers = computeLayerOrder(quadrantModel())
        // 면 순서: 0=좌하, 1=우하, 2=우상, 3=좌상.
        // 실제 종이의 겹은 아래에서부터 우하 → 좌하 → 우상 → 좌상.
        assertThat(layers[1]).isEqualTo(0) // 우하: 한 번도 안 움직임
        assertThat(layers[0]).isEqualTo(1) // 좌하: 1단계에서 움직임
        assertThat(layers[2]).isEqualTo(2) // 우상: 2단계에서 움직임(더 위)
        assertThat(layers[3]).isEqualTo(3) // 좌상: 두 단계 모두
    }

    @Test
    fun neverMovedFaceStaysAtBottom() {
        val layers = computeLayerOrder(quadrantModel())
        assertThat(layers.min()).isEqualTo(0)
    }

    @Test
    fun returnsOneEntryPerFace() {
        val model = DemoOrigami.model()
        assertThat(computeLayerOrder(model)).hasSize(model.base.faces.size)
    }

    @Test
    fun demoHasFourDistinctLayers() {
        // 두 번 접으면 4겹이 된다.
        val layers = computeLayerOrder(DemoOrigami.model())
        assertThat(layers.toSet()).hasSize(4)
    }

    @Test
    fun modelWithoutStepsIsAllZero() {
        val model = quadrantModel().copy(steps = emptyList())
        assertThat(computeLayerOrder(model).toSet()).containsExactly(0)
    }
}
