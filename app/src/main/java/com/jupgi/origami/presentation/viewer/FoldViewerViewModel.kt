package com.jupgi.origami.presentation.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jupgi.origami.domain.model.OrigamiModel
import com.jupgi.origami.domain.repository.OrigamiRepository
import com.jupgi.origami.domain.usecase.FoldMeshAtUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.floor

/**
 * 뷰어 재생 상태를 관리한다. 카메라 회전(yaw/pitch)은 순수 뷰 관심사라 화면 로컬 상태로 두고,
 * 여기서는 progress(재생 위치)와 자동 재생만 다룬다. ViewModel 은 UseCase 만 호출한다
 * (Repository 직접 호출은 로드 시 1회 모델 획득에 한정).
 */
@HiltViewModel
class FoldViewerViewModel
    @Inject
    constructor(
        repository: OrigamiRepository,
        private val foldMeshAt: FoldMeshAtUseCase,
    ) : ViewModel() {
        private val model: OrigamiModel = repository.models().first()
        private var playJob: Job? = null

        private val _uiState = MutableStateFlow(stateAt(INITIAL_PROGRESS))
        val uiState: StateFlow<FoldViewerUiState> = _uiState.asStateFlow()

        /** 슬라이더 스크럽: 임의 지점으로. 사용자가 직접 만지면 자동 재생은 멈춘다. */
        fun onProgressChange(progress: Float) {
            pause()
            setProgress(progress)
        }

        /** 다음 단계 하나만큼 접기(다음 정수 지점으로). */
        fun nextStep() {
            pause()
            setProgress(floor(_uiState.value.progress + 1f).coerceAtMost(model.stepCount.toFloat()))
        }

        /** 이전 단계로 되돌리기(이전 정수 지점으로). */
        fun prevStep() {
            pause()
            val current = _uiState.value.progress
            // 진행 중(소수)이면 현재 단계 시작으로, 딱 맞으면 한 단계 뒤로.
            val target = if (current > floor(current)) floor(current) else floor(current) - 1f
            setProgress(target.coerceAtLeast(0f))
        }

        fun reset() {
            pause()
            setProgress(0f)
        }

        /** 재생 ↔ 일시정지. 끝에 도달한 상태에서 재생하면 처음부터 다시 접는다. */
        fun togglePlay() {
            if (_uiState.value.isPlaying) pause() else play()
        }

        private fun play() {
            val end = model.stepCount.toFloat()
            if (_uiState.value.progress >= end) setProgress(0f)
            playJob?.cancel()
            _uiState.value = _uiState.value.copy(isPlaying = true)
            playJob =
                viewModelScope.launch {
                    while (isActive && _uiState.value.progress < end) {
                        delay(FRAME_MS)
                        val next = _uiState.value.progress + FRAME_MS.toFloat() / STEP_DURATION_MS
                        setProgress(next.coerceAtMost(end))
                    }
                    pause()
                }
        }

        private fun pause() {
            playJob?.cancel()
            playJob = null
            if (_uiState.value.isPlaying) {
                _uiState.value = _uiState.value.copy(isPlaying = false)
            }
        }

        private fun setProgress(progress: Float) {
            _uiState.value = stateAt(progress).copy(isPlaying = _uiState.value.isPlaying)
        }

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
            private const val FRAME_MS = 16L // ~60fps
            private const val STEP_DURATION_MS = 2200f // 한 단계를 접는 데 걸리는 시간
        }
    }
