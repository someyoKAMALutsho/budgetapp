package com.gorib.app.ui.screens.settings.rent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorib.app.data.db.entity.RentConfigEntity
import com.gorib.app.domain.repository.RentConfigRepository
import com.gorib.app.domain.usecase.GetCurrentRentUseCase
import com.gorib.app.domain.usecase.MarkRentPaidUseCase
import com.gorib.app.domain.usecase.SaveRentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RentSettingsUiState(
    val currentRent: RentConfigEntity? = null,
    val rentHistory: List<RentConfigEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RentSettingsViewModel @Inject constructor(
    private val getCurrentRentUseCase: GetCurrentRentUseCase,
    private val saveRentUseCase: SaveRentUseCase,
    private val markRentPaidUseCase: MarkRentPaidUseCase,
    private val rentConfigRepository: RentConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RentSettingsUiState())
    val uiState: StateFlow<RentSettingsUiState> = _uiState.asStateFlow()

    init {
        loadRentData()
    }

    private fun loadRentData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // Observe current active rent
            getCurrentRentUseCase()
                .catch { t ->
                    _uiState.update { it.copy(error = t.localizedMessage, isLoading = false) }
                }
                .collect { current ->
                    _uiState.update { it.copy(currentRent = current) }
                }
        }

        viewModelScope.launch {
            // Observe rent history
            rentConfigRepository.getRentHistory()
                .catch { t ->
                    _uiState.update { it.copy(error = t.localizedMessage, isLoading = false) }
                }
                .collect { history ->
                    _uiState.update { it.copy(rentHistory = history, isLoading = false) }
                }
        }
    }

    fun markAsPaid() {
        val current = _uiState.value.currentRent ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            markRentPaidUseCase(current.id, paid = true)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { t ->
                    _uiState.update { it.copy(isLoading = false, error = t.localizedMessage) }
                }
        }
    }

    fun markAsUnpaid() {
        val current = _uiState.value.currentRent ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                rentConfigRepository.markAsUnpaid(current.id)
                _uiState.update { it.copy(isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun saveNewRent(amount: Double, note: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            saveRentUseCase(amount, note)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { t ->
                    _uiState.update { it.copy(isLoading = false, error = t.localizedMessage) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
