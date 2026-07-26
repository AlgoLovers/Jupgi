package com.jupgi.origami.domain.usecase

import com.jupgi.origami.domain.model.Face
import com.jupgi.origami.domain.model.FoldStep
import com.jupgi.origami.domain.model.OrigamiModel
import javax.inject.Inject

/**
 * 각 면이 **몇 번째 겹**인지 계산한다. 값이 클수록 위(뷰어 쪽) 레이어.
 *
 * ## 왜 필요한가
 *
 * 반으로 접으면 겹친 종이가 완전히 동일평면이 된다. 이때 깊이만으로는 어느 겹이 위인지 알 수
 * 없다 — 옳은 순서를 알려면 접기 이력이 필요하다.
 *
 * ## 규칙: 접는 쪽은 **순서가 뒤집힌 채로** 반대쪽 위에 얹힌다
 *
 * 종이 뭉치를 통째로 넘기면 **맨 위에 있던 장이 맨 아래로 간다.** 이 반전을 빠뜨리면 두 번
 * 접었을 때 겉면이 실제 종이와 달라진다(실증: 정사각형을 반으로 두 번 접으면 겉면 두 장이
 * 모두 뒷면색이어야 하는데, 반전을 무시하면 한쪽이 앞면색으로 나왔다).
 *
 * 그래서 각 단계마다:
 * - 계곡접기(각 ≥ 0)는 움직이는 쪽을 **뒤집어 고정된 쪽 위에** 쌓는다.
 * - 산접기(각 < 0)는 **뒤집어 고정된 쪽 아래에** 쌓는다.
 *
 * 예) 정사각형을 (1) 왼쪽→오른쪽, (2) 위→아래로 접으면 실제 종이의 겹은 아래에서부터
 * **우하 → 좌하 → 좌상 → 우상** 이고, 이 규칙이 그대로 재현한다. (1단계에서 좌상이 우상 위에
 * 올라갔다가, 2단계에서 위쪽 뭉치가 통째로 넘어가며 둘의 상하가 뒤바뀐다.)
 *
 * ## 한계
 *
 * 단순 산·계곡접기 기준의 근사다. 역접기·스쿼시처럼 한 단계 안에서 레이어가 서로 파고드는
 * 접기는 이 규칙으로 표현되지 않는다 — 그때는 FOLD `faceOrders` 가 필요하다(M2).
 */
class ComputeLayerOrderUseCase
    @Inject
    constructor() {
        /** @return 면 인덱스 → 겹 높이(클수록 위). [OrigamiModel.base] 의 면 개수와 길이가 같다. */
        operator fun invoke(model: OrigamiModel): List<Int> {
            val faces = model.base.faces
            val layers = IntArray(faces.size)
            model.steps.forEach { step ->
                val moving = faces.indices.filter { movesWith(faces[it], step) }
                if (moving.isEmpty() || moving.size == faces.size) return@forEach
                val fixed = faces.indices.filter { it !in moving.toSet() }
                applyStep(layers, moving, fixed, isValley = step.foldAngleDeg >= 0f)
            }
            return layers.toList()
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
    }
