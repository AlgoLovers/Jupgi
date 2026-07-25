package com.jupgi.origami.domain.validation

import com.google.common.truth.Truth.assertThat
import com.jupgi.origami.domain.model.Face
import com.jupgi.origami.domain.model.FoldAssignment
import com.jupgi.origami.domain.model.FoldStep
import com.jupgi.origami.domain.model.OrigamiModel
import com.jupgi.origami.domain.model.PaperMesh
import com.jupgi.origami.domain.model.Vec3
import com.jupgi.origami.domain.usecase.FoldMeshAtUseCase
import org.junit.Test

/**
 * 검증 장치 자체를 검증한다 — **의도적으로 망가뜨린 스텝을 정말 잡아내는지**.
 * 이게 없으면 "항상 통과하는" 무의미한 검사가 되어 버린다.
 */
class FoldInvariantsTest {
    private val foldMeshAt = FoldMeshAtUseCase()

    /**
     * 힌지(x=0)를 따라 **분할된** 정사각형. 접는 선 위에 정점 1·4가 있어야 변이 거기서 꺾인다.
     *
     * ```
     * 3(-1,1) --- 4(0,1) --- 5(1,1)
     *    |          |          |
     * 0(-1,-1) -- 1(0,-1) -- 2(1,-1)
     * ```
     */
    private val splitSquare =
        PaperMesh(
            vertices =
                listOf(
                    Vec3(-1f, -1f, 0f), // 0
                    Vec3(0f, -1f, 0f), // 1 (힌지 위)
                    Vec3(1f, -1f, 0f), // 2
                    Vec3(-1f, 1f, 0f), // 3
                    Vec3(0f, 1f, 0f), // 4 (힌지 위)
                    Vec3(1f, 1f, 0f), // 5
                ),
            faces =
                listOf(
                    Face(0, 1, 4),
                    Face(0, 4, 3),
                    Face(1, 2, 5),
                    Face(1, 5, 4),
                ),
        )

    /** 힌지에서 분할되지 않은 정사각형 — 접으면 아래 모서리가 늘어난다. */
    private val unsplitSquare =
        PaperMesh(
            vertices =
                listOf(
                    Vec3(-1f, -1f, 0f), // 0
                    Vec3(1f, -1f, 0f), // 1
                    Vec3(1f, 1f, 0f), // 2
                    Vec3(-1f, 1f, 0f), // 3
                ),
            faces = listOf(Face(0, 1, 2), Face(0, 2, 3)),
        )

    private fun modelWith(
        base: PaperMesh = splitSquare,
        moving: Set<Int> = RIGHT_EDGE,
        angleDeg: Float = 180f,
        assignment: FoldAssignment = FoldAssignment.VALLEY,
        hingeStart: Vec3 = Vec3(0f, -1f, 0f),
        hingeEnd: Vec3 = Vec3(0f, 1f, 0f),
    ) = OrigamiModel(
        id = "t",
        title = "t",
        difficulty = 1,
        base = base,
        steps =
            listOf(
                FoldStep(
                    id = "s0",
                    hingeStart = hingeStart,
                    hingeEnd = hingeEnd,
                    movingVertexIndices = moving,
                    foldAngleDeg = angleDeg,
                    assignment = assignment,
                    instruction = "",
                ),
            ),
    )

    @Test
    fun validRigidFoldHasNoViolations() {
        val model = modelWith()
        val violations = FoldInvariants.sweep(model) { foldMeshAt(model, it) }
        assertThat(violations.joinToString("\n")).isEmpty()
    }

    @Test
    fun detectsUnsplitMeshBeforeFolding() {
        // 힌지가 면을 가로지르는데 그 자리에 정점이 없다 — 접어보지 않아도 알 수 있는 오류.
        val model = modelWith(base = unsplitSquare, moving = setOf(1, 2))
        val violations = FoldInvariants.checkHingeSplitting(model) { foldMeshAt(model, it) }
        assertThat(violations.map { it.kind }).contains(InvariantKind.EDGE_CROSSES_HINGE_UNSPLIT)
    }

    @Test
    fun detectsStretchedPaperWhenOnlyOneEndOfAnEdgeMoves() {
        // 정점 2만 움직이면 변(2,5)가 늘어난다 — 실제로는 접을 수 없는 종이.
        val model = modelWith(moving = setOf(2))
        val violations = FoldInvariants.sweep(model) { foldMeshAt(model, it) }
        assertThat(violations.map { it.kind }).contains(InvariantKind.EDGE_LENGTH_CHANGED)
    }

    @Test
    fun detectsAreaChange() {
        // 180°는 반사라 이 삼각형의 면적이 우연히 보존된다 — 90°로 접어 면을 실제로 찌그러뜨린다.
        val model = modelWith(moving = setOf(2), angleDeg = 90f)
        val folded = foldMeshAt(model, 1f)
        val violations = FoldInvariants.checkMesh(splitSquare, folded)
        assertThat(violations.map { it.kind }).contains(InvariantKind.FACE_AREA_CHANGED)
    }

    @Test
    fun detectsTopologyChange() {
        val fewer = splitSquare.copy(vertices = splitSquare.vertices.dropLast(1))
        val violations = FoldInvariants.checkMesh(splitSquare, fewer)
        assertThat(violations.map { it.kind }).containsExactly(InvariantKind.TOPOLOGY_CHANGED)
    }

    @Test
    fun detectsAssignmentSignMismatch() {
        // 음수 각(=산접기)인데 VALLEY로 배정했다.
        val model = modelWith(angleDeg = -90f, assignment = FoldAssignment.VALLEY)
        val violations = FoldInvariants.checkStepEncoding(model)
        assertThat(violations.map { it.kind }).contains(InvariantKind.ASSIGNMENT_SIGN_MISMATCH)
    }

    @Test
    fun acceptsMountainFoldWithNegativeAngle() {
        val model = modelWith(angleDeg = -90f, assignment = FoldAssignment.MOUNTAIN)
        assertThat(FoldInvariants.checkStepEncoding(model)).isEmpty()
    }

    @Test
    fun detectsDegenerateHinge() {
        val p = Vec3(0f, 0f, 0f)
        val model = modelWith(hingeStart = p, hingeEnd = p)
        val violations = FoldInvariants.checkStepEncoding(model)
        assertThat(violations.map { it.kind }).contains(InvariantKind.DEGENERATE_HINGE)
    }

    @Test
    fun detectsEmptyMovingSet() {
        val violations = FoldInvariants.checkStepEncoding(modelWith(moving = emptySet()))
        assertThat(violations.map { it.kind }).contains(InvariantKind.EMPTY_MOVING_SET)
    }

    @Test
    fun detectsVertexIndexOutOfRange() {
        val violations = FoldInvariants.checkStepEncoding(modelWith(moving = setOf(2, 99)))
        assertThat(violations.map { it.kind }).contains(InvariantKind.VERTEX_INDEX_OUT_OF_RANGE)
    }

    @Test
    fun detectsFoldAngleBeyondPhysicalLimit() {
        val violations = FoldInvariants.checkStepEncoding(modelWith(angleDeg = 270f))
        assertThat(violations.map { it.kind }).contains(InvariantKind.FOLD_ANGLE_OUT_OF_RANGE)
    }

    @Test
    fun sweepReportsWorstViolationOnlyOncePerKind() {
        val model = modelWith(moving = setOf(2))
        val violations = FoldInvariants.sweep(model) { foldMeshAt(model, it) }
        // 같은 종류가 수백 개 쏟아지지 않고 종류당 하나(최악)로 요약된다.
        assertThat(violations.map { it.kind }).containsNoDuplicates()
    }

    private companion object {
        /** splitSquare 에서 x=1 열(힌지 오른쪽) 정점들. */
        val RIGHT_EDGE = setOf(2, 5)
    }
}
