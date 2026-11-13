package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.data.model.AiVideoModel
import com.example.data.model.PromptLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore by preferencesDataStore(name = "vasan_studio_settings")

data class StudioSettings(
    val backendUrl: String = BuildConfig.VASAN_DEFAULT_BACKEND_URL,
    val preferredLanguage: String = PromptLanguage.ENGLISH.label,
    val defaultModel: String = AiVideoModel.VEO.displayName
)

class SettingsDataStore(private val context: Context) {
    private val appContext = context.applicationContext

    val settingsFlow: Flow<StudioSettings> = appContext.studioDataStore.data.map { prefs: Preferences ->
        StudioSettings(
            backendUrl = prefs[KEY_BACKEND_URL] ?: BuildConfig.VASAN_DEFAULT_BACKEND_URL,
            preferredLanguage = prefs[KEY_LANGUAGE] ?: PromptLanguage.ENGLISH.label,
            defaultModel = prefs[KEY_DEFAULT_MODEL] ?: AiVideoModel.VEO.displayName
        )
    }

    suspend fun saveBackendUrl(url: String) {
        appContext.studioDataStore.edit { prefs ->
            prefs[KEY_BACKEND_URL] = url.trim()
        }
    }

    suspend fun savePreferredLanguage(language: String) {
        appContext.studioDataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language
        }
    }

    suspend fun saveDefaultModel(model: String) {
        appContext.studioDataStore.edit { prefs ->
            prefs[KEY_DEFAULT_MODEL] = model
        }
    }

    private companion object {
        val KEY_BACKEND_URL = stringPreferencesKey("backend_url")
        val KEY_LANGUAGE = stringPreferencesKey("preferred_language")
        val KEY_DEFAULT_MODEL = stringPreferencesKey("default_model")
    }
}
