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
        val corners =
            listOf(
                -1f to -1f, // 0 좌하
                1f to -1f, // 1 우하
                1f to 1f, // 2 우상
                -1f to 1f, // 3 좌상
            )
        corners.forEach { (x, y) ->
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

    /** 그 면이 몇 번 뒤집혔는가 = 움직인 단계 수. 홀수면 뒷면이 위를 향한다. */
    private fun flipCount(
        model: OrigamiModel,
        faceIndex: Int,
    ): Int {
        val face = model.base.faces[faceIndex]
        return model.steps.count { step ->
            face.a in step.movingVertexIndices ||
                face.b in step.movingVertexIndices ||
                face.c in step.movingVertexIndices
        }
    }

    @Test
    fun foldedSideIsReversedAndStackedOnTop() {
        val layers = computeLayerOrder(quadrantModel())
        // 면 순서: 0=좌하, 1=우하, 2=우상, 3=좌상.
        // 실제 종이: 1단계에서 좌상이 우상 위로 갔다가, 2단계에서 위쪽 뭉치가 통째로 넘어가며
        // 둘의 상하가 뒤바뀐다 → 아래에서부터 우하 → 좌하 → 좌상 → 우상.
        assertThat(layers[1]).isEqualTo(0) // 우하: 한 번도 안 움직임
        assertThat(layers[0]).isEqualTo(1) // 좌하
        assertThat(layers[3]).isEqualTo(2) // 좌상 (뒤집히며 아래로)
        assertThat(layers[2]).isEqualTo(3) // 우상 (뒤집히며 위로) ← 최상단
    }

    /**
     * 실기기에서 발견된 버그를 고정한다 — 반으로 두 번 접으면 **겉면 두 장이 같은 면**이어야
     * 하는데(실제 색종이로 확인 가능) 겹 반전을 빠뜨려 한쪽이 앞면색으로 나왔다.
     */
    @Test
    fun bothOuterSurfacesShowTheSameSideAfterTwoHalfFolds() {
        val model = quadrantModel()
        val layers = computeLayerOrder(model)
        val top = layers.indices.maxBy { layers[it] }
        val bottom = layers.indices.minBy { layers[it] }
        // 위에서 보이는 면: 짝수 번 뒤집혔으면 앞면. 아래에서 보이는 면: 짝수면 뒷면(반대).
        val visibleAbove = if (flipCount(model, top) % 2 == 0) FRONT else BACK
        val visibleBelow = if (flipCount(model, bottom) % 2 == 0) BACK else FRONT
        assertThat(visibleAbove).isEqualTo(visibleBelow)
        assertThat(visibleAbove).isEqualTo(BACK) // 둘 다 뒷면(흰색)이 보인다
    }

    @Test
    fun mountainFoldStacksUnderneath() {
        val model =
            quadrantModel().let { base ->
                base.copy(
                    steps =
                        listOf(
                            base.steps[0].copy(
                                foldAngleDeg = -180f,
                                assignment = FoldAssignment.MOUNTAIN,
                            ),
                        ),
                )
            }
        val layers = computeLayerOrder(model)
        // 산접기는 아래로 접히므로 움직인 좌측이 고정된 우측보다 낮아야 한다.
        assertThat(layers[0]).isLessThan(layers[1])
        assertThat(layers[3]).isLessThan(layers[2])
    }

    @Test
    fun neverMovedFaceStaysAtBottom() {
        val layers = computeLayerOrder(quadrantModel())
        assertThat(layers[1]).isEqualTo(layers.min())
    }

    @Test
    fun returnsOneEntryPerFace() {
        val model = DemoOrigami.model()
        assertThat(computeLayerOrder(model)).hasSize(model.base.faces.size)
    }

    @Test
    fun demoHasFourDistinctLayers() {
        // 두 번 접으면 4겹이 된다.
        assertThat(computeLayerOrder(DemoOrigami.model()).toSet()).hasSize(4)
    }

    @Test
    fun modelWithoutStepsIsAllZero() {
        val model = quadrantModel().copy(steps = emptyList())
        assertThat(computeLayerOrder(model).toSet()).containsExactly(0)
    }

    private companion object {
        const val FRONT = "앞면"
        const val BACK = "뒷면"
    }
}
