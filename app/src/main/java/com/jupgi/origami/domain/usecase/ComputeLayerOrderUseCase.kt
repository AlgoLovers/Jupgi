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
 * 반으로 접으면 겹친 종이가 완전히 동일평면이 된다. 이때 렌더러의 깊이 정렬(화가 알고리즘)은
 * 어느 겹이 위인지 알 수 없어 삼각형마다 앞뒤가 뒤바뀐다(체커보드). 깊이를 양자화해 정렬을
 * 결정적으로 만들어도, **그 순서가 옳다는 보장은 없다** — 옳은 순서를 알려면 접기 이력이 필요하다.
 *
 * ## 규칙
 *
 * 계곡접기는 움직인 종이를 고정된 종이 **위에** 올린다. 그리고 **나중 단계일수록 더 위**에 쌓인다.
 * 그래서 스텝 i에서 움직인 면에 `2^i` 을 더하면, 합의 크기가 곧 겹 순서가 된다.
 *
 * 예) 정사각형을 (1) 왼쪽→오른쪽, (2) 위→아래로 접으면 실제 종이의 겹은 아래에서부터
 * 오른쪽아래(0) · 왼쪽아래(1) · 오른쪽위(2) · 왼쪽위(3) 순이고, 이 규칙이 그대로 재현한다.
 *
 * ## 한계
 *
 * 단순 산·계곡접기 기준의 근사다. 역접기·스쿼시처럼 한 단계 안에서 레이어가 서로 파고드는
 * 접기는 이 규칙으로 표현되지 않는다 — 그때는 FOLD `faceOrders` 가 필요하다(M2).
 * 스텝 수가 [MAX_STEPS_FOR_BITS] 를 넘으면 상위 비트가 넘치므로 순서 보장이 깨진다.
 */
class ComputeLayerOrderUseCase
    @Inject
    constructor() {
        /** @return 면 인덱스 → 겹 높이(클수록 위). [OrigamiModel.base] 의 면 개수와 길이가 같다. */
        operator fun invoke(model: OrigamiModel): List<Int> {
            val steps = model.steps.take(MAX_STEPS_FOR_BITS)
            return model.base.faces.map { face ->
                var layer = 0
                steps.forEachIndexed { i, step ->
                    if (movesWith(face, step)) layer += 1 shl i
                }
                layer
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
            /** Int 비트 수 한계. 이보다 많은 단계는 겹 순서를 이 방식으로 표현할 수 없다. */
            const val MAX_STEPS_FOR_BITS: Int = 30
        }
    }
