package com.jupgi.origami.presentation.viewer.render

import com.jupgi.origami.domain.model.Face
import com.jupgi.origami.domain.model.PaperMesh
import com.jupgi.origami.domain.model.Vec3

/**
 * 겹(shell) 구성과 그리기 순서 계산 — 렌더러의 "무엇을 어떤 순서로 그리는가".
 *
 * **순수 Kotlin 으로 분리한 이유**: 이 로직의 오류는 화면에서만 드러나 눈 검증으로는
 * 각도×진행도 조합을 다 덮을 수 없다. 유닛테스트가 픽셀 단위 z-buffer 레퍼런스와
 * 수천 프레임을 자동 비교해(ShellStackReferenceTest) 가림 순서 오류를 기계적으로 잡는다.
 *
 * 파이프라인:
 * 1. 겹 묶기 — 같은 (겹 값, 법선 부호) 면들을 한 셸로(색·음영 단일).
 * 2. 평면 클러스터 오프셋 — **같은 평면에 포개진 셸들 사이에서만** 겹 순위에 비례한
 *    미세 오프셋(polygon offset 계열)으로 동일평면 z-fighting 을 원천 제거. 평면이 다른
 *    셸(수직으로 선 날개 등)에 스택 겹 값으로 오프셋을 주면 다른 셸을 침투하는 인공
 *    교차가 생기므로(교차하면 어떤 그리기 순서도 일부 픽셀이 틀린다) 주지 않는다.
 * 3. 뉴웰(Newell) 순서 — 화면에서 실제로 겹치는(SAT) 삼각형 쌍마다 평면 부호 검사로
 *    앞뒤 간선을 만들어 위상 정렬. centroid 깊이 정렬은 원리적으로 틀린다(접힌 겹은
 *    거울상이라 대각선 방향이 반대 — 기운 카메라에서 centroid 위치 차이가 오프셋을 압도).
 */
object ShellStack {
    /** 겹당 종이 두께(모델 좌표, 종이 한 변 = 2). */
    const val PAPER_THICKNESS: Float = 0.008f

    /** 한 겹의 렌더 단위 — 평면(법선+기준점)과 카메라 공간 xy 범위를 든다. */
    data class Shell(
        val faceIndices: List<Int>,
        val offset: Vec3,
        val normal: Vec3,
        val refPoint: Vec3,
        val minX: Float,
        val maxX: Float,
        val minY: Float,
        val maxY: Float,
        val avgDepth: Float,
    )

    /** 그리기 한 단위 — 오프셋이 적용된 삼각형과 소속 셸. */
    data class DrawTriangle(
        val shellIndex: Int,
        val a: Vec3,
        val b: Vec3,
        val c: Vec3,
        val depth: Float,
    )

    /** 카메라 공간 정점에서 겹들을 구성한다(문서는 클래스 KDoc 참고). */
    fun build(
        mesh: PaperMesh,
        camVerts: List<Vec3>,
        layerOrder: List<Int>,
        flipParity: List<Boolean>,
    ): List<Shell> {
        val groups =
            mesh.faces.indices.groupBy {
                layerOrder.getOrElse(it) { 0 } to (faceNormal(camVerts, mesh.faces[it]).z >= 0f)
            }

        data class Proto(
            val layer: Int,
            val faceIndices: List<Int>,
            val normal: Vec3,
            val stackUp: Vec3,
            val planeKey: Long,
        )

        val protos =
            groups.map { (key, faceIndices) ->
                val representative = mesh.faces[faceIndices.first()]
                val normal = faceNormal(camVerts, representative)
                val flipped = flipParity.getOrElse(faceIndices.first()) { false }
                val stackUp = if (flipped) normal * -1f else normal
                Proto(
                    layer = key.first,
                    faceIndices = faceIndices,
                    normal = normal,
                    stackUp = stackUp,
                    planeKey = planeKey(stackUp, camVerts[representative.a]),
                )
            }

        // 같은 평면 클러스터 안에서 겹 값 순위 → 오프셋. 순위를 중앙 정렬해 평면 주위에 대칭 배치.
        val byPlane = protos.groupBy { it.planeKey }
        return protos.map { proto ->
            val cluster = byPlane.getValue(proto.planeKey).sortedBy { it.layer }
            val rank = cluster.indexOfFirst { it === proto }
            val centered = rank - (cluster.size - 1) / 2f
            val offset = proto.stackUp * (centered * PAPER_THICKNESS)
            buildShell(mesh, camVerts, proto.faceIndices, offset, proto.normal)
        }
    }

    /**
     * 삼각형 그리기 순서 — 뉴웰 방식(문서는 클래스 KDoc 참고). 이 순서는 픽셀 z-buffer
     * 레퍼런스와 전 작품 × 진행도 × 각도에서 자동 비교 검증된다.
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
    fun triangleDrawOrder(
        mesh: PaperMesh,
        camVerts: List<Vec3>,
        shells: List<Shell>,
    ): List<DrawTriangle> {
        val tris = ArrayList<DrawTriangle>(mesh.faces.size)
        shells.forEachIndexed { shellIndex, shell ->
            for (index in shell.faceIndices) {
                val f = mesh.faces[index]
                val a = camVerts[f.a] + shell.offset
                val b = camVerts[f.b] + shell.offset
                val c = camVerts[f.c] + shell.offset
                tris += DrawTriangle(shellIndex, a, b, c, depth = (a.z + b.z + c.z) / 3f)
            }
        }
        val n = tris.size
        val minX = FloatArray(n)
        val maxX = FloatArray(n)
        val minY = FloatArray(n)
        val maxY = FloatArray(n)
        for (i in 0 until n) {
            val t = tris[i]
            minX[i] = minOf(t.a.x, t.b.x, t.c.x)
            maxX[i] = maxOf(t.a.x, t.b.x, t.c.x)
            minY[i] = minOf(t.a.y, t.b.y, t.c.y)
            maxY[i] = maxOf(t.a.y, t.b.y, t.c.y)
        }
        val edges = Array(n) { mutableListOf<Int>() }
        val indegree = IntArray(n)
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                if (minX[i] >= maxX[j] || minX[j] >= maxX[i]) continue
                if (minY[i] >= maxY[j] || minY[j] >= maxY[i]) continue
                // bbox 는 과잉 판정 — 실제로 겹치지 않는 쌍의 간선은 가짜 사이클을 만들어
                // 폴백(순서 오류)을 유발한다. 정밀(SAT) 겹침일 때만 간선을 만든다(뉴웰 원전).
                if (!trianglesOverlap2D(tris[i], tris[j])) continue
                val jSide = sideOfPlane(tris[i], tris[j])
                val order =
                    when {
                        jSide > 0 -> {
                            1
                        }

                        // j 가 i 평면의 카메라 쪽 → i 먼저(뒤)
                        jSide < 0 -> {
                            -1
                        }

                        else -> {
                            val iSide = sideOfPlane(tris[j], tris[i])
                            when {
                                iSide > 0 -> -1

                                iSide < 0 -> 1

                                // 양방향 모호(교차·동일평면) — centroid 폴백
                                else -> if (tris[i].depth <= tris[j].depth) 1 else -1
                            }
                        }
                    }
                if (order > 0) {
                    edges[i].add(j)
                    indegree[j]++
                } else {
                    edges[j].add(i)
                    indegree[i]++
                }
            }
        }
        val ready = ArrayDeque((0 until n).filter { indegree[it] == 0 }.sortedBy { tris[it].depth })
        val result = ArrayList<DrawTriangle>(n)
        val done = BooleanArray(n)
        while (ready.isNotEmpty()) {
            val i = ready.removeFirst()
            done[i] = true
            result += tris[i]
            for (next in edges[i]) {
                indegree[next]--
                if (indegree[next] == 0) ready.addLast(next)
            }
        }
        if (result.size < n) { // 사이클 폴백 — 남은 것은 centroid 순
            result += (0 until n).filter { !done[it] }.sortedBy { tris[it].depth }.map { tris[it] }
        }
        return result
    }

    /** 면의 법선(카메라 좌표계). 한 겹은 한 평면이라 대표 면 하나로 겹 전체의 향을 정할 수 있다. */
    fun faceNormal(
        camVerts: List<Vec3>,
        face: Face,
    ): Vec3 {
        val p0 = camVerts[face.a]
        return (camVerts[face.b] - p0).cross(camVerts[face.c] - p0).normalized()
    }

    /** 평면 식별 키 — 스택 방향(부호 정규화된 법선)과 원점 거리로 양자화. */
    private fun planeKey(
        stackUp: Vec3,
        point: Vec3,
    ): Long {
        val n = if (stackUp.z < 0 || (stackUp.z == 0f && stackUp.x < 0)) stackUp * -1f else stackUp
        val qx = Math.round(n.x / NORMAL_QUANTUM).toLong()
        val qy = Math.round(n.y / NORMAL_QUANTUM).toLong()
        val qz = Math.round(n.z / NORMAL_QUANTUM).toLong()
        val d = Math.round(n.dot(point) / PLANE_D_QUANTUM).toLong()
        return ((qx and BITS) shl SHIFT_X) or ((qy and BITS) shl SHIFT_Y) or
            ((qz and BITS) shl SHIFT_Z) or (d and BITS)
    }

    /** 두 삼각형이 화면(xy)에서 실제로 겹치는가 — 분리축(SAT) 검사. 접하기만 하면 겹침 아님. */
    private fun trianglesOverlap2D(
        t1: DrawTriangle,
        t2: DrawTriangle,
    ): Boolean {
        val a1 = floatArrayOf(t1.a.x, t1.a.y, t1.b.x, t1.b.y, t1.c.x, t1.c.y)
        val a2 = floatArrayOf(t2.a.x, t2.a.y, t2.b.x, t2.b.y, t2.c.x, t2.c.y)
        return !hasSeparatingAxis(a1, a2) && !hasSeparatingAxis(a2, a1)
    }

    /** [poly] 의 세 변을 분리축 후보로 [other] 와의 분리 여부를 검사한다. */
    private fun hasSeparatingAxis(
        poly: FloatArray,
        other: FloatArray,
    ): Boolean {
        for (e in 0 until 3) {
            val x0 = poly[e * 2]
            val y0 = poly[e * 2 + 1]
            val x1 = poly[(e * 2 + 2) % 6]
            val y1 = poly[(e * 2 + 3) % 6]
            val axisX = y0 - y1
            val axisY = x1 - x0
            var min1 = Float.MAX_VALUE
            var max1 = -Float.MAX_VALUE
            for (v in 0 until 3) {
                val p = axisX * poly[v * 2] + axisY * poly[v * 2 + 1]
                min1 = minOf(min1, p)
                max1 = maxOf(max1, p)
            }
            var min2 = Float.MAX_VALUE
            var max2 = -Float.MAX_VALUE
            for (v in 0 until 3) {
                val p = axisX * other[v * 2] + axisY * other[v * 2 + 1]
                min2 = minOf(min2, p)
                max2 = maxOf(max2, p)
            }
            if (max1 <= min2 + OVERLAP_EPS || max2 <= min1 + OVERLAP_EPS) return true
        }
        return false
    }

    /**
     * [other] 의 세 정점이 [base] 평면의 카메라(+z) 쪽에 있으면 +1, 반대쪽이면 -1,
     * 걸치면 0. 공유 변·정점은 허용 오차로 흡수한다.
     */
    private fun sideOfPlane(
        base: DrawTriangle,
        other: DrawTriangle,
    ): Int {
        var normal = (base.b - base.a).cross(base.c - base.a)
        val len = normal.length()
        if (len < Vec3.EPS) return 0
        normal = normal * (1f / len)
        if (normal.z < 0f) normal = normal * -1f // 카메라 쪽(+z) 기준으로 정렬
        var pos = 0
        var neg = 0
        for (v in arrayOf(other.a, other.b, other.c)) {
            val d = normal.dot(v - base.a)
            if (d > PLANE_EPS) {
                pos++
            } else if (d < -PLANE_EPS) {
                neg++
            }
        }
        return when {
            pos > 0 && neg == 0 -> 1
            neg > 0 && pos == 0 -> -1
            else -> 0
        }
    }

    private fun buildShell(
        mesh: PaperMesh,
        camVerts: List<Vec3>,
        faceIndices: List<Int>,
        offset: Vec3,
        normal: Vec3,
    ): Shell {
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var sumZ = 0f
        var count = 0
        for (index in faceIndices) {
            val f = mesh.faces[index]
            for (vi in intArrayOf(f.a, f.b, f.c)) {
                val v = camVerts[vi] + offset
                if (v.x < minX) minX = v.x
                if (v.x > maxX) maxX = v.x
                if (v.y < minY) minY = v.y
                if (v.y > maxY) maxY = v.y
                sumZ += v.z
                count++
            }
        }
        val first = mesh.faces[faceIndices.first()]
        return Shell(
            faceIndices = faceIndices,
            offset = offset,
            normal = normal,
            refPoint = camVerts[first.a] + offset,
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            avgDepth = sumZ / count,
        )
    }

    /** 평면 부호 판정 허용 오차 — 공유 변/정점과 부동소수 오차 흡수. 겹 오프셋(0.008)보다 작게. */
    private const val PLANE_EPS = 1.5e-3f

    /** 접하는(변·정점 공유) 삼각형을 "겹침 아님"으로 보는 허용 오차. */
    private const val OVERLAP_EPS = 1e-5f

    /** 법선 양자화 단위 — 이보다 가까운 방향은 같은 평면 방향으로 본다. */
    private const val NORMAL_QUANTUM = 0.02f

    /** 평면 거리 양자화 — 도메인 겹 간격(정확히 0)과 다른 평면(≥격자 간격)을 가른다. */
    private const val PLANE_D_QUANTUM = 0.05f

    private const val BITS = 0xFFFFL
    private const val SHIFT_X = 48
    private const val SHIFT_Y = 32
    private const val SHIFT_Z = 16
}
