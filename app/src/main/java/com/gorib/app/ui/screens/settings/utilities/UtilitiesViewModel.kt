package com.gorib.app.ui.screens.settings.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorib.app.data.db.entity.UtilityBillGroupWithItems
import com.gorib.app.domain.repository.UtilityLineItemInput
import com.gorib.app.domain.repository.UtilityRepository
import com.gorib.app.domain.usecase.GetUtilitiesUseCase
import com.gorib.app.domain.usecase.MarkUtilityPaidUseCase
import com.gorib.app.domain.usecase.SaveUtilityBillUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UtilitiesUiState(
    val utilityGroups: List<UtilityBillGroupWithItems> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class UtilitiesViewModel @Inject constructor(
    private val getUtilitiesUseCase: GetUtilitiesUseCase,
    private val saveUtilityBillUseCase: SaveUtilityBillUseCase,
    private val markUtilityPaidUseCase: MarkUtilityPaidUseCase,
    private val utilityRepository: UtilityRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UtilitiesUiState())
    val uiState: StateFlow<UtilitiesUiState> = _uiState.asStateFlow()

    init {
        loadUtilities()
    }

    private fun loadUtilities() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            utilityRepository.getAllUtilities()
                .catch { t ->
                    _uiState.update { it.copy(error = t.localizedMessage, isLoading = false) }
                }
                .collect { list ->
                    _uiState.update { it.copy(utilityGroups = list, isLoading = false) }
                }
        }
    }

    fun markAsPaid(groupId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            markUtilityPaidUseCase(groupId, paid = true)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { t ->
                    _uiState.update { it.copy(isLoading = false, error = t.localizedMessage) }
                }
        }
    }

    fun markAsUnpaid(groupId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            markUtilityPaidUseCase(groupId, paid = false)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { t ->
                    _uiState.update { it.copy(isLoading = false, error = t.localizedMessage) }
                }
        }
    }

    fun deleteBill(groupId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                utilityRepository.deleteBill(groupId)
                _uiState.update { it.copy(isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    fun saveSingleBill(type: String, customName: String?, amount: Double, note: String?, receiptPath: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            saveUtilityBillUseCase.saveSingle(type, customName, amount, note, receiptPath)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                .onFailure { t ->
                    _uiState.update { it.copy(isLoading = false, error = t.localizedMessage) }
                }
        }
    }

    fun saveCombinedBill(lineItems: List<UtilityLineItemInput>, note: String?, receiptPath: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            saveUtilityBillUseCase.saveCombined(lineItems, note, receiptPath)
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
