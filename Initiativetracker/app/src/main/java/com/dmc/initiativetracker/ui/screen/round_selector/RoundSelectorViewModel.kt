package com.dmc.initiativetracker.ui.screen.round_selector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmc.initiativetracker.domain.model.RoundListItem
import com.dmc.initiativetracker.repository.RoundRepository
import com.dmc.initiativetracker.ui.preferences.SortPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoundSelectorViewModel(
    private val repo: RoundRepository,
    private val sortPreferences: SortPreferences
) : ViewModel() {

    private val sortOption = MutableStateFlow(sortPreferences.getRoundSelectorSort())
    private val isSortMenuOpen = MutableStateFlow(false)
    private val confirmDeleteId = MutableStateFlow<Long?>(null)
    private val isWorking = MutableStateFlow(false)
    private val toast = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RoundSelectorUiState> =
        combine(
            repo.observeRoundListItems(),
            sortOption
        ) { rounds, sort ->
            sortRounds(rounds, sort) to sort
        }.combine(isSortMenuOpen) { pair, sortMenuOpen ->
            Triple(pair.first, pair.second, sortMenuOpen)
        }.combine(confirmDeleteId) { triple, delId ->
            RoundSelectorUiState(
                rounds = triple.first,
                sortOption = triple.second,
                isSortMenuOpen = triple.third,
                confirmDeleteRoundId = delId,
                isWorking = isWorking.value,
                toast = toast.value
            )
        }.combine(isWorking) { state, working ->
            state.copy(isWorking = working)
        }.combine(toast) { state, toastMsg ->
            state.copy(toast = toastMsg)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            RoundSelectorUiState(sortOption = sortPreferences.getRoundSelectorSort())
        )

    fun openSortMenu() {
        isSortMenuOpen.value = true
    }

    fun closeSortMenu() {
        isSortMenuOpen.value = false
    }

    fun selectSort(option: RoundSortOption) {
        sortOption.value = option
        sortPreferences.setRoundSelectorSort(option)
        isSortMenuOpen.value = false
    }

    fun requestDelete(roundId: Long) {
        confirmDeleteId.value = roundId
    }

    fun cancelDelete() {
        confirmDeleteId.value = null
    }

    fun confirmDelete() = viewModelScope.launch {
        val id = confirmDeleteId.value ?: return@launch
        isWorking.value = true
        repo.deleteRound(id)
        isWorking.value = false
        confirmDeleteId.value = null
        toast.value = "Ronda eliminada"
    }

    fun consumeToast() {
        toast.value = null
    }

    private fun sortRounds(
        rounds: List<RoundListItem>,
        option: RoundSortOption
    ): List<RoundListItem> {
        return when (option) {
            RoundSortOption.CREATED_DESC -> rounds.sortedByDescending { it.createdAt }
            RoundSortOption.CREATED_ASC -> rounds.sortedBy { it.createdAt }
            RoundSortOption.NAME_ASC -> rounds.sortedBy { it.name.lowercase() }
            RoundSortOption.NAME_DESC -> rounds.sortedByDescending { it.name.lowercase() }
            RoundSortOption.PLAYERS_ASC -> rounds.sortedBy { it.characterCount }
            RoundSortOption.PLAYERS_DESC -> rounds.sortedByDescending { it.characterCount }
        }
    }
}