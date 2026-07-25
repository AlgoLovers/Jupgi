package com.jupgi.origami.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Vec3Test {
    @Test
    fun crossProductFollowsRightHandRule() {
        val c = Vec3(1f, 0f, 0f).cross(Vec3(0f, 1f, 0f))
        assertThat(c.x).isWithin(TOL).of(0f)
        assertThat(c.y).isWithin(TOL).of(0f)
        assertThat(c.z).isWithin(TOL).of(1f)
    }

    @Test
    fun dotProduct() {
        assertThat(Vec3(1f, 2f, 3f).dot(Vec3(4f, 5f, 6f))).isWithin(TOL).of(32f)
    }

    @Test
    fun normalizedHasUnitLength() {
        val n = Vec3(3f, 0f, 4f).normalized()
        assertThat(n.length()).isWithin(TOL).of(1f)
    }

    @Test
    fun normalizingZeroVectorIsSafe() {
        assertThat(Vec3.ZERO.normalized()).isEqualTo(Vec3.ZERO)
    }

    companion object {
        private const val TOL = 1e-5f
    }
}
