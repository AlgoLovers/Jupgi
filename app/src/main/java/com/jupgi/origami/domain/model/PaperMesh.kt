package com.jupgi.origami.domain.model

/**
 * 삼각형 면 하나(정점 인덱스 3개). 정점은 반시계(CCW)로 감아 앞면 법선이 나오게 한다.
 */
data class Face(
    val a: Int,
    val b: Int,
    val c: Int,
)

/**
 * 종이를 나타내는 메시 — 정점 목록 + 삼각형 면 목록.
 * 접기는 [vertices] 만 갱신하고 [faces] 위상은 보존한다(종이는 찢어지지 않는다).
 */
data class PaperMesh(
    val vertices: List<Vec3>,
    val faces: List<Face>,
) {
    /** 면의 앞면 법선(정점 CCW 기준). 렌더러의 음영·앞/뒷면 판별에 쓴다. */
    fun normalOf(face: Face): Vec3 {
        val p0 = vertices[face.a]
        val p1 = vertices[face.b]
        val p2 = vertices[face.c]
        return (p1 - p0).cross(p2 - p0).normalized()
    }
}
