package com.jupgi.origami.domain.authoring

import com.jupgi.origami.domain.model.FoldAssignment
import com.jupgi.origami.domain.model.FoldStep
import com.jupgi.origami.domain.model.PaperMesh
import com.jupgi.origami.domain.model.Vec3
import com.jupgi.origami.domain.usecase.FoldMeshAtUseCase
import kotlin.math.abs

/**
 * 작품 저작 도구 — **접힌 좌표계 힌지 문제**를 해결한다.
 *
 * `FoldStep` 의 힌지는 "직전 단계까지 접힌 좌표계" 기준이어야 한다(rules/fold-model.md).
 * 부채(아코디언)의 두 번째 접기부터, 비행기의 날개 접기처럼 **이미 접힌 종이 위에서 접는**
 * 단계는 힌지의 현재 위치를 손으로 계산하기 어렵고 실수하면 종이가 늘어난다.
 *
 * 이 빌더는 스텝을 추가할 때마다 메시를 실제로 접어 정점 위치를 추적하고, 힌지를
 * **base 정점 인덱스 두 개**로 지정하면 그 정점의 "접힌 현재 위치"를 힌지로 쓴다.
 * 원본 종이의 선(정점 쌍)만 알면 되므로 저작이 선언적이 된다.
 *
 * 순수 Kotlin — 유닛테스트에서 그대로 실행되고, `FoldInvariants.sweep()` 으로 결과를 검증한다.
 */
class FoldSequenceBuilder(
    private val base: PaperMesh,
) {
    private val steps = mutableListOf<FoldStep>()
    private var current: List<Vec3> = base.vertices

    /**
     * 접기 한 단계 추가.
     *
     * @param hingeVertexA base 정점 인덱스 — 접는 선의 한 끝. 이 스텝에서 움직이면 안 된다.
     * @param hingeVertexB base 정점 인덱스 — 접는 선의 다른 끝.
     * @param movingVertexIndices 이 힌지 기준으로 회전할 base 정점 인덱스 집합.
     * @param foldAngleDeg 부호 있는 접힘각(양수=계곡, 음수=산 — FOLD 규약).
     */
    fun fold(
        id: String,
        hingeVertexA: Int,
        hingeVertexB: Int,
        movingVertexIndices: Set<Int>,
        foldAngleDeg: Float,
        instruction: String,
    ): FoldSequenceBuilder {
        require(hingeVertexA !in movingVertexIndices && hingeVertexB !in movingVertexIndices) {
            "힌지 정점($hingeVertexA, $hingeVertexB)은 움직이는 집합에 넣을 수 없다"
        }
        val step =
            FoldStep(
                id = id,
                hingeStart = current[hingeVertexA],
                hingeEnd = current[hingeVertexB],
                movingVertexIndices = movingVertexIndices,
                foldAngleDeg = foldAngleDeg,
                assignment = if (foldAngleDeg >= 0f) FoldAssignment.VALLEY else FoldAssignment.MOUNTAIN,
                instruction = instruction,
            )
        steps += step
        current = applyFully(current, step)
        return this
    }

    /** 지금까지의 스텝 목록. `OrigamiModel(steps = …)` 에 넘긴다. */
    fun build(): List<FoldStep> = steps.toList()

    /** 다음 접기의 힌지·이동 집합을 정할 때 참고할, 현재까지 접힌 정점 위치. */
    fun currentPosition(vertexIndex: Int): Vec3 = current[vertexIndex]

    /** base 정점 중 접힌 현재 위치가 조건을 만족하는 인덱스 집합 — 이동 집합 저작용. */
    fun verticesWhereNow(predicate: (Vec3) -> Boolean): Set<Int> = current.indices.filter { predicate(current[it]) }.toSet()

    private fun applyFully(
        verts: List<Vec3>,
        step: FoldStep,
    ): List<Vec3> {
        val axis = (step.hingeEnd - step.hingeStart)
        val axisDir = axis.normalized()
        require(axis.length() > Vec3.EPS) { "힌지 길이가 0이다: ${step.id}" }
        val angleRad = Math.toRadians(step.foldAngleDeg.toDouble()).toFloat()
        return verts.mapIndexed { i, v ->
            if (i in step.movingVertexIndices) {
                FoldMeshAtUseCase.rotateAboutAxis(v, step.hingeStart, axisDir, angleRad)
            } else {
                v
            }
        }
    }

    companion object {
        /** 두 좌표가 같은 위치인가(저작 시 정점 찾기 헬퍼). */
        fun near(
            a: Vec3,
            b: Vec3,
            eps: Float = 1e-4f,
        ): Boolean = abs(a.x - b.x) < eps && abs(a.y - b.y) < eps && abs(a.z - b.z) < eps
    }
}
