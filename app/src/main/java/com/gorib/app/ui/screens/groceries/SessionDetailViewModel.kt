package com.gorib.app.ui.screens.groceries

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gorib.app.data.db.entity.ShoppingSessionWithItems
import com.gorib.app.domain.usecase.grocery.GetSessionByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionDetailUiState(
    val sessionWithItems: ShoppingSessionWithItems? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val getSessionByIdUseCase: GetSessionByIdUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getSessionByIdUseCase(sessionId)
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.localizedMessage, isLoading = false) }
                }
                .collect { session ->
                    _uiState.update { it.copy(sessionWithItems = session, isLoading = false) }
                }
        }
    }
}
