package com.jupgi.origami.domain.usecase

import com.jupgi.origami.domain.model.OrigamiModel
import com.jupgi.origami.domain.model.PaperMesh
import com.jupgi.origami.domain.model.Vec3
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * 전역 진행도 [progress] 에서의 종이 메시를 결정적으로 계산한다.
 *
 * progress ∈ [0, stepCount]. 정수부 = 완전히 적용된 단계 수, 소수부 = 현재 단계의 보간 t.
 * 예) 1.5 = 0번 단계 완료 + 1번 단계를 50%까지 접은 상태.
 *
 * 이 값 하나로 **일시정지·한 단계씩 전/후진·임의 지점 스크럽**이 전부 해결된다
 * (progress 만 바꾸면 됨). 순수 함수라 같은 입력이면 항상 같은 출력 → 유닛테스트로 고정.
 */
class FoldMeshAtUseCase
    @Inject
    constructor() {
        operator fun invoke(
            model: OrigamiModel,
            progress: Float,
        ): PaperMesh {
            val clamped = progress.coerceIn(0f, model.stepCount.toFloat())
            val fullSteps = floor(clamped).toInt()
            val partial = clamped - fullSteps

            var verts = model.base.vertices
            for (i in 0 until fullSteps) {
                verts = applyStep(verts, model, i, t = 1f)
            }
            if (fullSteps < model.stepCount && partial > 0f) {
                verts = applyStep(verts, model, fullSteps, t = partial)
            }
            return model.base.copy(vertices = verts)
        }

        private fun applyStep(
            verts: List<Vec3>,
            model: OrigamiModel,
            stepIndex: Int,
            t: Float,
        ): List<Vec3> {
            val step = model.steps[stepIndex]
            val angleRad = Math.toRadians((step.foldAngleDeg * t).toDouble()).toFloat()
            val axisDir = (step.hingeEnd - step.hingeStart).normalized()
            return verts.mapIndexed { idx, v ->
                if (idx in step.movingVertexIndices) {
                    rotateAboutAxis(v, step.hingeStart, axisDir, angleRad)
                } else {
                    v
                }
            }
        }

        companion object {
            /**
             * 점 [p] 를, [axisPoint] 를 지나고 방향이 단위벡터 [axisDir] 인 축 기준으로
             * [angleRad] 만큼 회전(로드리게스 회전 공식). 축은 정규화되어 있다고 가정한다.
             */
            fun rotateAboutAxis(
                p: Vec3,
                axisPoint: Vec3,
                axisDir: Vec3,
                angleRad: Float,
            ): Vec3 {
                val v = p - axisPoint
                val cosA = cos(angleRad)
                val sinA = sin(angleRad)
                // v·cosθ + (k×v)·sinθ + k·(k·v)·(1−cosθ)
                val rotated =
                    v * cosA +
                        axisDir.cross(v) * sinA +
                        axisDir * (axisDir.dot(v) * (1f - cosA))
                return axisPoint + rotated
            }
        }
    }
