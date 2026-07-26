package com.jupgi.origami.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jupgi.origami.domain.repository.PlaySpeed
import com.jupgi.origami.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) : ViewModel() {
        val playSpeed: StateFlow<PlaySpeed> =
            settings.playSpeed.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = PlaySpeed.NORMAL,
            )

        fun onPlaySpeedSelected(speed: PlaySpeed) {
            viewModelScope.launch { settings.setPlaySpeed(speed) }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
