package com.jupgi.origami.domain.repository

import kotlinx.coroutines.flow.Flow

/** 자동 재생 배속. 학습자 페이스에 맞춰 접는 속도를 고른다. */
enum class PlaySpeed(
    val multiplier: Float,
) {
    SLOW(0.5f),
    NORMAL(1f),
    FAST(2f),
}

/**
 * 앱 설정. 구현은 data 계층(DataStore).
 * 의존 방향: presentation/data → domain (역방향 금지).
 */
interface SettingsRepository {
    val playSpeed: Flow<PlaySpeed>

    suspend fun setPlaySpeed(speed: PlaySpeed)
}
