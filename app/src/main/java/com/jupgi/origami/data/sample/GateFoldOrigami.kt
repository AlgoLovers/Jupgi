package com.jupgi.origami.data.sample

import com.jupgi.origami.domain.model.FoldAssignment
import com.jupgi.origami.domain.model.FoldStep
import com.jupgi.origami.domain.model.OrigamiCategory
import com.jupgi.origami.domain.model.OrigamiModel
import com.jupgi.origami.domain.model.Vec3

/**
 * 대문 접기(전통 기본형) — 양쪽 가장자리를 세로 1/4 선까지 접어 문짝 두 개를 만든다.
 * 두 힌지가 서로 독립(각각 base 좌표에서 유효)이라 저작이 단순한, 기본기 두 번째 작품.
 */
object GateFoldOrigami {
    private const val N = 8

    fun model(): OrigamiModel {
        val mesh = SampleMeshes.square(N)

        fun verticesWhere(predicate: (Vec3) -> Boolean): Set<Int> =
            mesh.vertices.indices
                .filter { predicate(mesh.vertices[it]) }
                .toSet()

        // 왼쪽 문짝: x=-0.5 힌지로 왼쪽 가장자리를 중심 쪽으로.
        val left =
            FoldStep(
                id = "gate-left",
                hingeStart = Vec3(-0.5f, -1f, 0f),
                hingeEnd = Vec3(-0.5f, 1f, 0f),
                movingVertexIndices = verticesWhere { it.x < -0.5f - EPS },
                foldAngleDeg = 180f,
                assignment = FoldAssignment.VALLEY,
                instruction = "왼쪽 가장자리를 세로 1/4 선에 맞춰 안쪽으로 접습니다. (계곡접기)",
            )
        // 오른쪽 문짝: 힌지 방향을 위→아래로 두어 +180(계곡)이 안쪽(위)으로 접히게 한다.
        val right =
            FoldStep(
                id = "gate-right",
                hingeStart = Vec3(0.5f, 1f, 0f),
                hingeEnd = Vec3(0.5f, -1f, 0f),
                movingVertexIndices = verticesWhere { it.x > 0.5f + EPS },
                foldAngleDeg = 180f,
                assignment = FoldAssignment.VALLEY,
                instruction = "오른쪽 가장자리도 세로 1/4 선에 맞춰 안쪽으로 접어 대문을 완성합니다.",
            )

        return OrigamiModel(
            id = "gate-fold",
            title = "대문 접기",
            difficulty = 1,
            category = OrigamiCategory.BASICS,
            base = mesh,
            steps = listOf(left, right),
        )
    }

    private const val EPS = 1e-4f
}
