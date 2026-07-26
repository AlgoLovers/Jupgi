package com.jupgi.origami.domain.usecase

import com.jupgi.origami.domain.model.Face
import com.jupgi.origami.domain.model.FoldStep
import com.jupgi.origami.domain.model.OrigamiModel
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.floor

/**
 * **주어진 progress 시점의** 각 면의 겹 높이를 계산한다. 값이 클수록 위(뷰어 쪽) 레이어.
 *
 * ## 왜 progress 의 함수인가 (실기기 버그의 원인)
 *
 * 겹 순서는 시간에 따라 변한다. 1단계만 접힌 시점의 겹은 [왼쪽(위) / 오른쪽(아래)] 2겹인데,
 * **모든 스텝이 끝난 최종 순서를 전 구간에 적용하면** 아직 접지도 않은 2단계의 재배치(우상이
 * 최상단)가 미리 반영되어, 한 번 접힌 종이의 위쪽 절반만 앞면색으로 그려지는 버그가 났다.
 *
 * ## 규칙: 접는 쪽은 **순서가 뒤집힌 채로** 반대쪽에 얹힌다
 *
 * 종이 뭉치를 통째로 넘기면 맨 위 장이 맨 아래로 간다. 각 단계마다:
 * - 계곡접기(각 ≥ 0)는 움직이는 쪽을 뒤집어 고정된 쪽 **위에** 쌓는다.
 * - 산접기(각 < 0)는 뒤집어 고정된 쪽 **아래에** 쌓는다.
 *
 * ## 진행 중인 스텝의 반영 시점
 *
 * 회전 중인 뭉치의 상하가 실제로 뒤집히는 것은 **회전각이 90°를 넘는 순간**이다. 그 전에는
 * 직전 스텝까지의 순서가 유효하다(뭉치 내부 조각들은 서로 같은 평면이라 깊이로는 갈리지 않고,
 * 이 순서가 화면을 결정한다). 그래서 `t·|foldAngleDeg| ≥ 90°` 부터 해당 스텝을 반영한다.
 *
 * ## 한계
 *
 * 단순 산·계곡접기 기준의 근사다. 역접기·스쿼시처럼 한 단계 안에서 레이어가 서로 파고드는
 * 접기는 이 규칙으로 표현되지 않는다 — 그때는 FOLD `faceOrders` 가 필요하다(M2).
 */
class ComputeLayerOrderUseCase
    @Inject
    constructor() {
        /**
         * @param progress 전역 진행도(0..stepCount). 생략하면 최종 상태.
         * @return 면 인덱스 → 겹 높이(클수록 위). [OrigamiModel.base] 의 면 개수와 길이가 같다.
         */
        operator fun invoke(
            model: OrigamiModel,
            progress: Float = model.stepCount.toFloat(),
        ): List<Int> {
            val faces = model.base.faces
            val layers = IntArray(faces.size)
            model.steps.take(effectiveStepCount(model, progress)).forEach { step ->
                val movingSet = faces.indices.filter { movesWith(faces[it], step) }.toSet()
                if (movingSet.isEmpty() || movingSet.size == faces.size) return@forEach
                val fixed = faces.indices.filter { it !in movingSet }
                applyStep(layers, movingSet.toList(), fixed, isValley = step.foldAngleDeg >= 0f)
            }
            return layers.toList()
        }

        /** progress 시점에 겹 재배치가 반영되어야 할 스텝 수(0..stepCount). */
        fun effectiveStepCount(
            model: OrigamiModel,
            progress: Float,
        ): Int {
            val clamped = progress.coerceIn(0f, model.stepCount.toFloat())
            val full = floor(clamped).toInt()
            if (full >= model.stepCount) return full
            val t = clamped - full
            val angle = abs(model.steps[full].foldAngleDeg)
            return if (t * angle >= FLIP_THRESHOLD_DEG) full + 1 else full
        }

        /**
         * 움직이는 쪽의 겹 순서를 뒤집어(`max - layer`) 고정된 쪽의 위(계곡) 또는 아래(산)로 옮긴다.
         */
        private fun applyStep(
            layers: IntArray,
            moving: List<Int>,
            fixed: List<Int>,
            isValley: Boolean,
        ) {
            val movingMax = moving.maxOf { layers[it] }
            val movingMin = moving.minOf { layers[it] }
            if (isValley) {
                val top = fixed.maxOfOrNull { layers[it] } ?: 0
                moving.forEach { layers[it] = top + 1 + (movingMax - layers[it]) }
            } else {
                val bottom = fixed.minOfOrNull { layers[it] } ?: 0
                moving.forEach { layers[it] = bottom - 1 - (layers[it] - movingMin) }
            }
        }

        /**
         * 면이 이 단계에서 움직이는가. 힌지 위 정점은 `movingVertexIndices` 에 넣지 않는 규약이라
         * (rules/fold-model.md), 셋 중 하나라도 포함되면 움직이는 쪽 반평면의 면이다.
         * 힌지를 가로지르는 면은 `FoldInvariants.checkHingeSplitting` 이 미리 걸러낸다.
         */
        private fun movesWith(
            face: Face,
            step: FoldStep,
        ): Boolean =
            face.a in step.movingVertexIndices ||
                face.b in step.movingVertexIndices ||
                face.c in step.movingVertexIndices

        companion object {
            /** 회전 중인 뭉치의 상하가 실제로 반전되는 회전각. */
            const val FLIP_THRESHOLD_DEG: Float = 90f
        }
    }
