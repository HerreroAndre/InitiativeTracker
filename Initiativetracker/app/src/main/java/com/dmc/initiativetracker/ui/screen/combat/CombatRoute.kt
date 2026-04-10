package com.dmc.initiativetracker.ui.screen.combat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmc.initiativetracker.repository.CombatRepository
import com.dmc.initiativetracker.repository.RoundRepository

@Composable
fun CombatRoute(
    roundId: Long,
    repo: RoundRepository,
    combatRepo: CombatRepository,
    onExit: () -> Unit
) {
    val factory = remember(roundId, repo, combatRepo) {
        CombatViewModelFactory(roundId, repo, combatRepo)
    }
    val vm: CombatViewModel = viewModel(factory = factory)

    LaunchedEffect(Unit) {
        vm.startIfNeeded()
    }

    CombatScreen(
        vm = vm,
        onExit = onExit
    )
}

private class CombatViewModelFactory(
    private val roundId: Long,
    private val repo: RoundRepository,
    private val combatRepo: CombatRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CombatViewModel::class.java)) {
            return CombatViewModel(roundId, repo, combatRepo) as T
        }
        error("Unknown ViewModel: ${modelClass.name}")
    }
}