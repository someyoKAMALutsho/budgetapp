package com.gorib.app.ui.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorib.app.domain.usecase.SaveRentSetupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for the Setup Wizard Rent Entry Screen.
 */
data class SetupWizardUiState(
    val rentInput: String = "",
    val payNow: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isComplete: Boolean = false
)

/**
 * ViewModel for coordinating the Setup Wizard onboarding.
 */
@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    private val saveRentSetupUseCase: SaveRentSetupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupWizardUiState())
    val uiState: StateFlow<SetupWizardUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SetupWizardEvent>()
    val eventFlow: SharedFlow<SetupWizardEvent> = _eventFlow.asSharedFlow()

    fun onRentInputChanged(input: String) {
        // Only allow numbers and decimal points
        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
            _uiState.update { it.copy(rentInput = input, errorMessage = null) }
        }
    }

    fun onPayNowChanged(checked: Boolean) {
        _uiState.update { it.copy(payNow = checked) }
    }

    fun saveRent() {
        val amount = _uiState.value.rentInput.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid rent amount greater than RM 0") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            saveRentSetupUseCase(amount, _uiState.value.payNow)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isComplete = true) }
                    _eventFlow.emit(SetupWizardEvent.OnboardingCompleted)
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = error.localizedMessage ?: "Failed to save rent configuration"
                        ) 
                    }
                }
        }
    }

    sealed interface SetupWizardEvent {
        object OnboardingCompleted : SetupWizardEvent
    }
}
