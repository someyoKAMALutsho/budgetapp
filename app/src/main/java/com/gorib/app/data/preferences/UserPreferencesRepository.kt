package com.gorib.app.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Interface defining Datastore preference controls (e.g. Theme, Rent, Wizard completed).
 */
interface UserPreferencesRepository {
    val isFirstLaunch: Flow<Boolean>
    val isDarkMode: Flow<Boolean>
    val monthlyRent: Flow<Double>
    val geminiApiKey: Flow<String>
    val hasShownGeminiIntro: Flow<Boolean>

    suspend fun setFirstLaunchCompleted()
    suspend fun setDarkMode(enabled: Boolean)
    suspend fun setMonthlyRent(rent: Double)
    suspend fun setGeminiApiKey(apiKey: String)
    suspend fun setGeminiIntroShown()
    suspend fun resetFirstLaunch()
}
