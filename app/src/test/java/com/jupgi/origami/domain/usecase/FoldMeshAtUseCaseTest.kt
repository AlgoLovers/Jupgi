package com.jupgi.origami.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.jupgi.origami.domain.model.Face
import com.jupgi.origami.domain.model.FoldAssignment
import com.jupgi.origami.domain.model.FoldStep
import com.jupgi.origami.domain.model.OrigamiCategory
import com.jupgi.origami.domain.model.OrigamiModel
import com.jupgi.origami.domain.model.PaperMesh
import com.jupgi.origami.domain.model.Vec3
import org.junit.Test

class FoldMeshAtUseCaseTest {
    private val foldMeshAt = FoldMeshAtUseCase()

    // 정사각형(코너 4개), 오른쪽 절반(정점 1,2)을 x=0 힌지로 접는 1단계 모델.
    private fun model(angleDeg: Float): OrigamiModel {
        val base =
            PaperMesh(
                vertices =
                    listOf(
                        Vec3(-1f, -1f, 0f), // 0 (고정)
                        Vec3(1f, -1f, 0f), // 1 (움직임)
                        Vec3(1f, 1f, 0f), // 2 (움직임)
                        Vec3(-1f, 1f, 0f), // 3 (고정)
                    ),
                faces = listOf(Face(0, 1, 2), Face(0, 2, 3)),
            )
        val step =
            FoldStep(
                id = "s",
                hingeStart = Vec3(0f, -1f, 0f),
                hingeEnd = Vec3(0f, 1f, 0f),
                movingVertexIndices = setOf(1, 2),
                foldAngleDeg = angleDeg,
                assignment = FoldAssignment.VALLEY,
                instruction = "",
            )
        return OrigamiModel("m", "t", 1, OrigamiCategory.BASICS, base, listOf(step))
    }

    @Test
    fun progressZeroReturnsBaseUnchanged() {
        val m = model(180f)
        val mesh = foldMeshAt(m, 0f)
        m.base.vertices.forEachIndexed { i, expected -> assertVec(mesh.vertices[i], expected) }
    }

    @Test
    fun foldInHalfMirrorsMovingVerticesAcrossHinge() {
        val mesh = foldMeshAt(model(180f), 1f)
        // 움직이는 정점은 x=0 기준으로 반사, 고정 정점은 그대로.
        assertVec(mesh.vertices[0], Vec3(-1f, -1f, 0f))
        assertVec(mesh.vertices[3], Vec3(-1f, 1f, 0f))
        assertVec(mesh.vertices[1], Vec3(-1f, -1f, 0f))
        assertVec(mesh.vertices[2], Vec3(-1f, 1f, 0f))
    }

    @Test
    fun ninetyDegreeFoldLiftsMovingVerticesOutOfPlane() {
        val mesh = foldMeshAt(model(90f), 1f)
        // (1,-1,0) → (0,-1,-1), (1,1,0) → (0,1,-1): x가 z 깊이로 접혀 들어간다.
        assertVec(mesh.vertices[1], Vec3(0f, -1f, -1f))
        assertVec(mesh.vertices[2], Vec3(0f, 1f, -1f))
        // 고정 정점은 불변.
        assertVec(mesh.vertices[0], Vec3(-1f, -1f, 0f))
    }

    @Test
    fun partialProgressIsBetweenFlatAndFolded() {
        val mesh = foldMeshAt(model(90f), 0.5f) // 45도까지만
        // z 는 0과 -1(90도 완료) 사이여야 한다.
        assertThat(mesh.vertices[1].z).isLessThan(0f)
        assertThat(mesh.vertices[1].z).isGreaterThan(-1f)
    }

    @Test
    fun progressBeyondStepCountIsClampedNotExtrapolated() {
        val full = foldMeshAt(model(180f), 1f)
        val over = foldMeshAt(model(180f), 5f)
        full.vertices.forEachIndexed { i, v -> assertVec(over.vertices[i], v) }
    }

    @Test
    fun isDeterministic() {
        val a = foldMeshAt(model(123f), 0.7f)
        val b = foldMeshAt(model(123f), 0.7f)
        a.vertices.forEachIndexed { i, v -> assertVec(b.vertices[i], v) }
    }

    private fun assertVec(
        actual: Vec3,
        expected: Vec3,
    ) {
        assertThat(actual.x).isWithin(TOL).of(expected.x)
        assertThat(actual.y).isWithin(TOL).of(expected.y)
        assertThat(actual.z).isWithin(TOL).of(expected.z)
    }

    companion object {
        private const val TOL = 1e-4f
    }
}
