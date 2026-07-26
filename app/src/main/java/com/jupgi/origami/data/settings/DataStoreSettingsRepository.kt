package com.jupgi.origami.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jupgi.origami.domain.repository.PlaySpeed
import com.jupgi.origami.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore(Preferences) 기반 설정 저장. enum 이름 문자열로 저장해 마이그레이션이 단순하다. */
@Singleton
class DataStoreSettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : SettingsRepository {
        override val playSpeed: Flow<PlaySpeed> =
            dataStore.data.map { prefs ->
                prefs[KEY_PLAY_SPEED]?.let { saved ->
                    PlaySpeed.entries.firstOrNull { it.name == saved }
                } ?: PlaySpeed.NORMAL
            }

        override suspend fun setPlaySpeed(speed: PlaySpeed) {
            dataStore.edit { it[KEY_PLAY_SPEED] = speed.name }
        }

        private companion object {
            val KEY_PLAY_SPEED = stringPreferencesKey("play_speed")
        }
    }
