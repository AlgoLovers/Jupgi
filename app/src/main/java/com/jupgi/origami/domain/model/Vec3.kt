package com.jupgi.origami.domain.model

import kotlin.math.sqrt

/**
 * 순수 Kotlin 3D 벡터. domain 계층은 android.* / androidx.* 에 의존하지 않는다
 * (.claude/rules/domain-purity.md). 폴딩 기하가 결정적이라 JVM 유닛테스트로 고정된다.
 */
data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float,
) {
    operator fun plus(o: Vec3): Vec3 = Vec3(x + o.x, y + o.y, z + o.z)

    operator fun minus(o: Vec3): Vec3 = Vec3(x - o.x, y - o.y, z - o.z)

    operator fun times(s: Float): Vec3 = Vec3(x * s, y * s, z * s)

    fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z

    fun cross(o: Vec3): Vec3 =
        Vec3(
            y * o.z - z * o.y,
            z * o.x - x * o.z,
            x * o.y - y * o.x,
        )

    fun length(): Float = sqrt(x * x + y * y + z * z)

    /** 영벡터면 그대로 반환(0으로 나누기 방지). */
    fun normalized(): Vec3 {
        val len = length()
        return if (len < EPS) this else this * (1f / len)
    }

    companion object {
        const val EPS: Float = 1e-6f
        val ZERO: Vec3 = Vec3(0f, 0f, 0f)
    }
}
