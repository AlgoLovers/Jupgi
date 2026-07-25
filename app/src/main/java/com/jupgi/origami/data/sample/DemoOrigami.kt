package com.jupgi.origami.data.sample

import com.jupgi.origami.domain.model.Face
import com.jupgi.origami.domain.model.FoldAssignment
import com.jupgi.origami.domain.model.FoldStep
import com.jupgi.origami.domain.model.OrigamiModel
import com.jupgi.origami.domain.model.PaperMesh
import com.jupgi.origami.domain.model.Vec3

/**
 * M1 데모 작품 — 정사각형을 "반으로 두 번" 접는 연습.
 *
 * 정식 콘텐츠는 이후 assets 의 FOLD/JSON 을 임포트해 만든다(docs/FOLD_MODEL.md, /add-model 스킬).
 * 이 데모는 폴딩 도메인 + 렌더러 파이프라인을 관통해 보여주는 골격용이다.
 *
 * 좌표계: 종이는 [-1,1]×[-1,1], z=0 평면. N×N 격자로 세분해 접힘이 종이처럼 보이게 한다.
 * 두 힌지 모두 접히지 않는 정점(x=0 열, y=0 행) 위에 있어 base 좌표계에서 그대로 유효하다.
 */
object DemoOrigami {
    private const val N = 8 // 격자 분할 수 (정점 (N+1)²개)

    fun model(): OrigamiModel {
        val mesh = buildSquareMesh(N)

        // 1단계: 왼쪽 절반(x<0)을 세로 중심선(x=0) 기준으로 오른쪽으로 접는다.
        val leftHalf = verticesWhere(N) { x, _ -> x < -1e-4f }
        val step1 =
            FoldStep(
                id = "half-1",
                hingeStart = Vec3(0f, -1f, 0f),
                hingeEnd = Vec3(0f, 1f, 0f),
                movingVertexIndices = leftHalf,
                foldAngleDeg = 180f,
                assignment = FoldAssignment.VALLEY,
                instruction = "왼쪽 절반을 세로 중심선을 따라 오른쪽으로 접어 반으로 만듭니다. (계곡접기)",
            )

        // 2단계: 위쪽 절반(y>0)을 가로 중심선(y=0) 기준으로 아래로 접는다.
        // 1단계 회전축이 y축과 평행이라 정점의 y좌표는 보존된다 → base 기준 y>0 집합이 그대로 유효.
        val topHalf = verticesWhere(N) { _, y -> y > 1e-4f }
        val step2 =
            FoldStep(
                id = "half-2",
                hingeStart = Vec3(0f, 0f, 0f),
                hingeEnd = Vec3(1f, 0f, 0f),
                movingVertexIndices = topHalf,
                foldAngleDeg = 180f,
                assignment = FoldAssignment.VALLEY,
                instruction = "위쪽 절반을 가로 중심선을 따라 아래로 접어 4등분을 만듭니다. (계곡접기)",
            )

        return OrigamiModel(
            id = "demo-two-halves",
            title = "반으로 두 번 접기 (연습)",
            difficulty = 1,
            base = mesh,
            steps = listOf(step1, step2),
        )
    }

    /** 행 우선 인덱스: index(i, j) = j*(N+1) + i, i=열(x), j=행(y). */
    private fun index(
        i: Int,
        j: Int,
    ): Int = j * (N + 1) + i

    private fun buildSquareMesh(n: Int): PaperMesh {
        val verts = ArrayList<Vec3>((n + 1) * (n + 1))
        for (j in 0..n) {
            for (i in 0..n) {
                val x = -1f + 2f * i / n
                val y = -1f + 2f * j / n
                verts.add(Vec3(x, y, 0f))
            }
        }
        val faces = ArrayList<Face>(n * n * 2)
        for (j in 0 until n) {
            for (i in 0 until n) {
                val v00 = index(i, j)
                val v10 = index(i + 1, j)
                val v11 = index(i + 1, j + 1)
                val v01 = index(i, j + 1)
                // +z 에서 볼 때 반시계(CCW)
                faces.add(Face(v00, v10, v11))
                faces.add(Face(v00, v11, v01))
            }
        }
        return PaperMesh(verts, faces)
    }

    /** 조건을 만족하는 정점 인덱스 집합(x,y 는 base 좌표). */
    private fun verticesWhere(
        n: Int,
        predicate: (x: Float, y: Float) -> Boolean,
    ): Set<Int> {
        val result = HashSet<Int>()
        for (j in 0..n) {
            for (i in 0..n) {
                val x = -1f + 2f * i / n
                val y = -1f + 2f * j / n
                if (predicate(x, y)) result.add(index(i, j))
            }
        }
        return result
    }
}
