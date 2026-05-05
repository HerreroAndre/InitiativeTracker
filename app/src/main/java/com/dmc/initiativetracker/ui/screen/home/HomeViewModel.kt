package com.dmc.initiativetracker.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmc.initiativetracker.repository.RoundRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repo: RoundRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun openCreateDialog() {
        _uiState.value = _uiState.value.copy(
            isCreateDialogOpen = true,
            newRoundName = ""
        )
    }

    fun closeCreateDialog() {
        if (_uiState.value.isWorking) return
        _uiState.value = _uiState.value.copy(isCreateDialogOpen = false)
    }

    fun onNewNameChange(value: String) {
        _uiState.value = _uiState.value.copy(newRoundName = value)
    }

    fun createRound(onCreated: (Long) -> Unit) = viewModelScope.launch {
        val name = _uiState.value.newRoundName.trim()
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(toast = "Poné un nombre")
            return@launch
        }

        _uiState.value = _uiState.value.copy(isWorking = true)

        val id = repo.createRound(name)

        _uiState.value = _uiState.value.copy(
            isWorking = false,
            isCreateDialogOpen = false,
            toast = "Ronda creada"
        )

        onCreated(id)
    }

    fun consumeToast() {
        _uiState.value = _uiState.value.copy(toast = null)
    }
}