package com.jupgi.origami.data.sample

import com.jupgi.origami.domain.authoring.FoldSequenceBuilder
import com.jupgi.origami.domain.model.OrigamiCategory
import com.jupgi.origami.domain.model.OrigamiModel

/**
 * 부채(전통) — 8등분 아코디언(계곡·산 교대) 접기.
 *
 * 두 번째 접기부터는 힌지가 "이미 접힌 종이 위"에 있다 — 원래 x=-0.5 선은 첫 접기로
 * x=-1.0 에 가 있다. [FoldSequenceBuilder] 가 접힘을 추적해 힌지의 현재 위치를 계산한다.
 *
 * 부호 규약: 계곡(+)은 움직이는 쪽이 위(+z)로 넘어와야 한다. 축 방향에 따라 회전 방향이
 * 뒤집히므로, 계곡 접기는 힌지를 위→아래로, 산 접기는 아래→위로 준다.
 */
object FanOrigami {
    private const val SEGMENTS = 8

    fun model(): OrigamiModel {
        val xs = List(SEGMENTS + 1) { -1f + 2f * it / SEGMENTS }
        val mesh = SampleMeshes.verticalStrip(xs)
        val n = xs.size // 아래 행 0..n-1, 위 행 n..2n-1

        val builder = FoldSequenceBuilder(mesh)
        for (k in 1 until SEGMENTS) {
            val bottom = k
            val top = n + k
            val moving = ((k + 1) until n).flatMap { listOf(it, n + it) }.toSet()
            val isValley = k % 2 == 1
            builder.fold(
                id = "fan-$k",
                // 계곡은 위→아래 축(+180 이 위로), 산은 아래→위 축(-180 이 아래로).
                hingeVertexA = if (isValley) top else bottom,
                hingeVertexB = if (isValley) bottom else top,
                movingVertexIndices = moving,
                foldAngleDeg = if (isValley) 180f else -180f,
                instruction =
                    if (isValley) {
                        "세로 ${k}번째 선에서 오른쪽 부분을 앞으로 접습니다. (계곡접기)"
                    } else {
                        "이번엔 뒤로 접습니다. 계곡·산을 번갈아 주름을 만듭니다. (산접기)"
                    },
            )
        }

        return OrigamiModel(
            id = "fan-accordion",
            title = "부채",
            difficulty = 2,
            category = OrigamiCategory.PRACTICAL,
            base = mesh,
            steps = builder.build(),
        )
    }
}
