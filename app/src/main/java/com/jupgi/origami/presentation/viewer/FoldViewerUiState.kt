package com.jupgi.origami.presentation.viewer

import com.jupgi.origami.domain.model.FoldStep
import com.jupgi.origami.domain.model.PaperMesh

/**
 * 뷰어 화면의 단일 불변 UI 상태. progress 하나로 재생 위치가 정해지고, mesh 는 그에 대응하는
 * (도메인이 계산한) 종이 형상이다.
 */
data class FoldViewerUiState(
    val title: String = "",
    val difficulty: Int = 1,
    val stepCount: Int = 0,
    /** 전역 진행도 0..stepCount. 정수부=완료 단계 수, 소수부=현재 단계 보간 t. */
    val progress: Float = 0f,
    val mesh: PaperMesh? = null,
    /** 지금 설명을 보여줄 단계(없으면 null — 완전히 펼쳐진 시작 상태). */
    val currentStep: FoldStep? = null,
    val currentStepNumber: Int = 0,
)
