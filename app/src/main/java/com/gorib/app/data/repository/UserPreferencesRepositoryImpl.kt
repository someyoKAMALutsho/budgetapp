package com.gorib.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.gorib.app.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// DataStore delegate
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gorib_user_preferences")

/**
 * Implementation of [UserPreferencesRepository] utilizing Jetpack DataStore.
 */
@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val KEY_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val KEY_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val KEY_MONTHLY_RENT = doublePreferencesKey("monthly_rent")
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_HAS_SHOWN_GEMINI_INTRO = booleanPreferencesKey("has_shown_gemini_intro")
    }

    override val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.KEY_FIRST_LAUNCH] ?: true // Defaults to true (first launch)
        }

    override val isDarkMode: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.KEY_DARK_MODE] ?: true // Defaults to true (dark theme premium feel)
        }

    override val monthlyRent: Flow<Double> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.KEY_MONTHLY_RENT] ?: 0.0
        }

    override val geminiApiKey: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.KEY_GEMINI_API_KEY] ?: ""
        }

    override val hasShownGeminiIntro: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.KEY_HAS_SHOWN_GEMINI_INTRO] ?: false
        }

    override suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_FIRST_LAUNCH] = false
        }
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_DARK_MODE] = enabled
        }
    }

    override suspend fun setMonthlyRent(rent: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_MONTHLY_RENT] = rent
        }
    }

    override suspend fun resetFirstLaunch() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_FIRST_LAUNCH] = true
            preferences[PreferencesKeys.KEY_HAS_SHOWN_GEMINI_INTRO] = false
        }
    }

    override suspend fun setGeminiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_GEMINI_API_KEY] = apiKey
        }
    }

    override suspend fun setGeminiIntroShown() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEY_HAS_SHOWN_GEMINI_INTRO] = true
        }
    }
}
