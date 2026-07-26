package com.jupgi.origami.data.sample

import com.jupgi.origami.domain.model.Face
import com.jupgi.origami.domain.model.PaperMesh
import com.jupgi.origami.domain.model.Vec3

/**
 * 샘플 작품용 메시 생성기. 접기가 유효하려면 **모든 접는 선 위에 정점이 있어야 한다**
 * (없으면 변이 꺾이지 못하고 늘어난다 — `FoldInvariants.EDGE_CROSSES_HINGE_UNSPLIT`).
 */
object SampleMeshes {
    /**
     * [-1,1]² 정사각형을 n×n 격자로 세분. 힌지가 격자선(x·y = -1 + 2k/n) 위에 있는 작품용.
     */
    fun square(n: Int): PaperMesh {
        val verts = ArrayList<Vec3>((n + 1) * (n + 1))
        for (j in 0..n) {
            for (i in 0..n) {
                verts.add(Vec3(-1f + 2f * i / n, -1f + 2f * j / n, 0f))
            }
        }
        val faces = ArrayList<Face>(n * n * 2)
        for (j in 0 until n) {
            for (i in 0 until n) {
                val v00 = j * (n + 1) + i
                val v10 = v00 + 1
                val v01 = v00 + (n + 1)
                val v11 = v01 + 1
                faces.add(Face(v00, v10, v11)) // +z 에서 볼 때 반시계(CCW)
                faces.add(Face(v00, v11, v01))
            }
        }
        return PaperMesh(verts, faces)
    }

    /**
     * 세로 접기 전용 스트립 — 주어진 x 좌표들에 세로선 정점을 두고 y 는 [-1,1] 한 칸.
     * 아코디언(부채)처럼 힌지가 전부 세로선인 작품용.
     * 인덱스: 아래 행 `0..xs.size-1`, 위 행 `xs.size..2*xs.size-1`.
     */
    fun verticalStrip(xs: List<Float>): PaperMesh {
        require(xs.size >= 2) { "세로선이 최소 2개 필요하다" }
        val verts =
            buildList {
                xs.forEach { add(Vec3(it, -1f, 0f)) }
                xs.forEach { add(Vec3(it, 1f, 0f)) }
            }
        val n = xs.size
        val faces =
            buildList {
                for (i in 0 until n - 1) {
                    add(Face(i, i + 1, n + i + 1))
                    add(Face(i, n + i + 1, n + i))
                }
            }
        return PaperMesh(verts, faces)
    }
}
