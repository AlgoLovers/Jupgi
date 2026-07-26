package com.jupgi.origami.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.jupgi.origami.data.sample.DemoOrigami
import com.jupgi.origami.domain.model.Face
import com.jupgi.origami.domain.model.FoldAssignment
import com.jupgi.origami.domain.model.FoldStep
import com.jupgi.origami.domain.model.OrigamiCategory
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
            category = OrigamiCategory.BASICS,
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

    /**
     * 실기기에서 발견된 버그를 고정한다 — 겹 순서는 progress 의 함수다. 1단계만 접힌 시점에
     * **아직 접지 않은 2단계의 재배치가 미리 반영되면** 한 번 접힌 종이의 위쪽 절반만
     * 앞면색으로 그려진다(최종 순서에서 우상이 최상단이므로).
     */
    @Test
    fun layerOrderAtStepOneIgnoresFutureSteps() {
        val model = quadrantModel()
        val layers = computeLayerOrder(model, progress = 1.0f)
        // 1단계 완료 시점: 왼쪽 뭉치(0=좌하, 3=좌상)가 통째로 오른쪽(1=우하, 2=우상) 위에 있다.
        assertThat(layers[0]).isGreaterThan(layers[1])
        assertThat(layers[0]).isGreaterThan(layers[2])
        assertThat(layers[3]).isGreaterThan(layers[1])
        assertThat(layers[3]).isGreaterThan(layers[2])
        // 아직 2단계가 없으므로 좌상/좌하는 같은 높이, 우상/우하도 같은 높이다.
        assertThat(layers[0]).isEqualTo(layers[3])
        assertThat(layers[1]).isEqualTo(layers[2])
    }

    @Test
    fun inProgressStepAppliesOnlyAfterNinetyDegrees() {
        val model = quadrantModel() // 두 스텝 모두 180° 접기 → 반영 시점은 t=0.5
        val before = computeLayerOrder(model, progress = 1.4f) // 72° — 아직 1단계 기준
        val after = computeLayerOrder(model, progress = 1.6f) // 108° — 2단계 반영
        assertThat(before).isEqualTo(computeLayerOrder(model, progress = 1.0f))
        assertThat(after).isEqualTo(computeLayerOrder(model, progress = 2.0f))
    }

    @Test
    fun ninetyDegreeFoldNeverFlipsMidStep() {
        // 90° 접기는 회전각이 90°를 넘지 않으므로 진행 중엔 겹 재배치가 일어나지 않는다.
        val model =
            quadrantModel().let { base ->
                base.copy(steps = listOf(base.steps[0].copy(foldAngleDeg = 90f)))
            }
        assertThat(computeLayerOrder(model, progress = 0.99f))
            .isEqualTo(computeLayerOrder(model, progress = 0f))
    }

    /** 오프셋 방향의 원천 — 뒤집힘 홀짝이 접기 이력과 일치해야 겹이 옳은 쪽으로 띄워진다. */
    @Test
    fun flipParityCountsMovedStepsPastNinetyDegrees() {
        val model = quadrantModel()
        // 면: 0=좌하(1단계만), 1=우하(안 움직임), 2=우상(2단계만), 3=좌상(두 번)
        assertThat(computeLayerOrder.flipParity(model, progress = 2.0f))
            .containsExactly(true, false, true, false)
            .inOrder()
        // 1단계만 접힌 시점: 왼쪽만 뒤집혀 있다.
        assertThat(computeLayerOrder.flipParity(model, progress = 1.0f))
            .containsExactly(true, false, false, true)
            .inOrder()
        // 2단계 90° 이전에는 아직 1단계 기준.
        assertThat(computeLayerOrder.flipParity(model, progress = 1.4f))
            .isEqualTo(computeLayerOrder.flipParity(model, progress = 1.0f))
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
