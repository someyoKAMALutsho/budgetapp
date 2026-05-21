package com.gorib.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorib.app.data.preferences.UserPreferencesRepository
import com.gorib.app.domain.model.Category
import com.gorib.app.domain.usecase.AddCategoryUseCase
import com.gorib.app.domain.usecase.GetAllCategoriesUseCase
import com.gorib.app.domain.usecase.UpdateCategoryBudgetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the Settings configurations.
 */
data class SettingsUiState(
    val isDarkMode: Boolean = true,
    val monthlyRent: Double = 0.0,
    val geminiApiKey: String = "",
    val isResetComplete: Boolean = false,
    val categories: List<Category> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val getAllCategoriesUseCase: GetAllCategoriesUseCase,
    private val updateCategoryBudgetUseCase: UpdateCategoryBudgetUseCase,
    private val addCategoryUseCase: AddCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadCategories()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            preferencesRepository.isDarkMode.collect { isDark ->
                _uiState.update { it.copy(isDarkMode = isDark) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.monthlyRent.collect { rent ->
                _uiState.update { it.copy(monthlyRent = rent) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.geminiApiKey.collect { key ->
                _uiState.update { it.copy(geminiApiKey = key) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getAllCategoriesUseCase().collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun updateCategoryBudget(categoryId: Long, budget: Double?) {
        viewModelScope.launch {
            updateCategoryBudgetUseCase(categoryId, budget)
        }
    }

    fun addCustomCategory(name: String, emoji: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            if (name.isBlank()) {
                onError("Category name cannot be empty")
                return@launch
            }
            if (emoji.isBlank()) {
                onError("Please select an emoji")
                return@launch
            }
            try {
                addCategoryUseCase(name, emoji)
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to add category")
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDarkMode(enabled)
            _uiState.update { it.copy(isDarkMode = enabled) }
        }
    }

    fun updateGeminiApiKey(key: String) {
        viewModelScope.launch {
            preferencesRepository.setGeminiApiKey(key)
            _uiState.update { it.copy(geminiApiKey = key) }
        }
    }

    /**
     * Resets the onboarding status, causing the setup wizard to prompt on app reload.
     */
    fun resetFirstLaunchWizard() {
        viewModelScope.launch {
            preferencesRepository.setMonthlyRent(0.0)
            preferencesRepository.resetFirstLaunch()
            _uiState.update { it.copy(isResetComplete = true) }
        }
    }
}
