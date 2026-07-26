package com.jupgi.origami.data.sample

import com.jupgi.origami.domain.authoring.FoldSequenceBuilder
import com.jupgi.origami.domain.model.Face
import com.jupgi.origami.domain.model.OrigamiCategory
import com.jupgi.origami.domain.model.OrigamiModel
import com.jupgi.origami.domain.model.PaperMesh
import com.jupgi.origami.domain.model.Vec3

/**
 * 종이비행기(다트, 전통) — 5단계: 코 접기 2회 → 반 접기 → 날개 2회.
 *
 * 첫 대각 접기 작품. 메시는 모든 접는 선(대각 D1·D2, 세로 중심, 날개선)이 정점으로
 * 분할되도록 수동 설계했다. 특히 **플랩 위의 y=0.7 가로선(정점 14·15)** 이 핵심인데,
 * 코를 접으면 그 선이 세로 날개선 위로 이동해 — 날개를 접을 때 플랩이 함께 꺾이는
 * 자리가 된다. 이 정점이 없으면 날개 접기에서 플랩이 늘어난다(FoldInvariants 가 검출).
 *
 * 날개 힌지는 반 접기 뒤의 좌표라 손으로 못 쓴다 — [FoldSequenceBuilder] 가 추적한다.
 */
object PaperPlaneOrigami {
    private const val WING_X = 0.3f

    @Suppress("LongMethod")
    fun model(): OrigamiModel {
        val v =
            listOf(
                Vec3(-1f, -1f, 0f), // 0
                Vec3(-WING_X, -1f, 0f), // 1  날개선 아래끝
                Vec3(0f, -1f, 0f), // 2  중심 아래
                Vec3(WING_X, -1f, 0f), // 3  날개선 아래끝(우)
                Vec3(1f, -1f, 0f), // 4
                Vec3(-1f, 0f, 0f), // 5  대각 D1 끝
                Vec3(1f, 0f, 0f), // 6  대각 D2 끝
                Vec3(-WING_X, 1f - WING_X, 0f), // 7  D1∩날개선
                Vec3(WING_X, 1f - WING_X, 0f), // 8  D2∩날개선
                Vec3(-1f, 1f, 0f), // 9  좌상 코너
                Vec3(-WING_X, 1f, 0f), // 10
                Vec3(0f, 1f, 0f), // 11 코 꼭짓점
                Vec3(WING_X, 1f, 0f), // 12
                Vec3(1f, 1f, 0f), // 13 우상 코너
                Vec3(-1f, 1f - WING_X, 0f), // 14 플랩 가로선(좌) — 접으면 날개선 위로 온다
                Vec3(1f, 1f - WING_X, 0f), // 15 플랩 가로선(우)
            )
        val faces =
            listOf(
                // 좌상 플랩 (대각 D1: 5→11 위쪽) — y=0.7 선으로 분할
                Face(5, 7, 14),
                Face(14, 7, 10),
                Face(14, 10, 9),
                Face(7, 11, 10),
                // 우상 플랩 (대각 D2: 11→6 위쪽)
                Face(6, 15, 8),
                Face(15, 13, 12),
                Face(15, 12, 8),
                Face(8, 12, 11),
                // 본체 (대각 아래) — 세로선들로 분할
                Face(0, 1, 7),
                Face(0, 7, 5),
                Face(1, 2, 11),
                Face(1, 11, 7),
                Face(2, 3, 8),
                Face(2, 8, 11),
                Face(3, 4, 6),
                Face(3, 6, 8),
            )
        val mesh = PaperMesh(v, faces)

        val steps =
            FoldSequenceBuilder(mesh)
                .fold(
                    id = "plane-nose-left",
                    hingeVertexA = 5,
                    hingeVertexB = 11,
                    movingVertexIndices = setOf(9, 10, 14),
                    foldAngleDeg = 180f,
                    instruction = "왼쪽 위 모서리를 세로 중심선에 맞춰 접어 내립니다. (계곡접기)",
                ).fold(
                    id = "plane-nose-right",
                    hingeVertexA = 11,
                    hingeVertexB = 6,
                    movingVertexIndices = setOf(13, 12, 15),
                    foldAngleDeg = 180f,
                    instruction = "오른쪽 위 모서리도 중심선에 맞춰 접어 뾰족한 코를 만듭니다.",
                ).fold(
                    id = "plane-half",
                    hingeVertexA = 2,
                    hingeVertexB = 11,
                    movingVertexIndices = setOf(0, 1, 5, 7, 9, 10, 14),
                    foldAngleDeg = 180f,
                    instruction = "세로 중심선을 따라 반으로 접어 동체를 만듭니다.",
                ).fold(
                    id = "plane-wing-left",
                    hingeVertexA = 1,
                    hingeVertexB = 7,
                    movingVertexIndices = setOf(0, 5),
                    foldAngleDeg = -90f,
                    instruction = "위쪽 날개를 동체 선에 맞춰 젖혀 폅니다. (산접기)",
                ).fold(
                    id = "plane-wing-right",
                    hingeVertexA = 3,
                    hingeVertexB = 8,
                    movingVertexIndices = setOf(4, 6),
                    foldAngleDeg = 90f,
                    instruction = "반대쪽 날개도 똑같이 젖혀 펴면 비행기 완성입니다.",
                ).build()

        return OrigamiModel(
            id = "paper-plane-dart",
            title = "종이비행기",
            difficulty = 3,
            category = OrigamiCategory.VEHICLES,
            base = mesh,
            steps = steps,
        )
    }
}
