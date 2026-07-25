package com.jupgi.origami.presentation.viewer

import androidx.lifecycle.ViewModel
import com.jupgi.origami.domain.model.OrigamiModel
import com.jupgi.origami.domain.repository.OrigamiRepository
import com.jupgi.origami.domain.usecase.FoldMeshAtUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.math.floor

/**
 * 뷰어 재생 상태를 관리한다. 카메라 회전(yaw/pitch)은 순수 뷰 관심사라 화면 로컬 상태로 두고,
 * 여기서는 progress(재생 위치)만 다룬다. ViewModel 은 UseCase 만 호출한다(Repository 직접 호출은
 * 로드 시 1회 모델 획득에 한정).
 */
@HiltViewModel
class FoldViewerViewModel
    @Inject
    constructor(
        repository: OrigamiRepository,
        private val foldMeshAt: FoldMeshAtUseCase,
    ) : ViewModel() {
        private val model: OrigamiModel = repository.models().first()

        private val _uiState = MutableStateFlow(stateAt(INITIAL_PROGRESS))
        val uiState: StateFlow<FoldViewerUiState> = _uiState.asStateFlow()

        /** 슬라이더 스크럽: 임의 지점으로. */
        fun onProgressChange(progress: Float) {
            _uiState.value = stateAt(progress)
        }

        /** 다음 단계 하나만큼 접기(다음 정수 지점으로). */
        fun nextStep() {
            val next = floor(_uiState.value.progress + 1f)
            onProgressChange(next.coerceAtMost(model.stepCount.toFloat()))
        }

        /** 이전 단계로 되돌리기(이전 정수 지점으로). */
        fun prevStep() {
            val current = _uiState.value.progress
            // 진행 중(소수)이면 현재 단계 시작으로, 딱 맞으면 한 단계 뒤로.
            val target = if (current > floor(current)) floor(current) else floor(current) - 1f
            onProgressChange(target.coerceAtLeast(0f))
        }

        fun reset() = onProgressChange(0f)

        private fun stateAt(progress: Float): FoldViewerUiState {
            val clamped = progress.coerceIn(0f, model.stepCount.toFloat())
            val stepIdx = floor(clamped).toInt().coerceIn(0, (model.stepCount - 1).coerceAtLeast(0))
            val showStep = clamped > 0f || model.stepCount > 0
            return FoldViewerUiState(
                title = model.title,
                difficulty = model.difficulty,
                stepCount = model.stepCount,
                progress = clamped,
                mesh = foldMeshAt(model, clamped),
                currentStep = if (showStep && model.stepCount > 0) model.steps[stepIdx] else null,
                currentStepNumber = stepIdx + 1,
            )
        }

        companion object {
            private const val INITIAL_PROGRESS = 0.6f // 시작부터 3D로 반쯤 접힌 상태를 보여준다
        }
    }
