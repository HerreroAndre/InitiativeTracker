package com.dmc.initiativetracker.ui.screen.round_prep

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmc.initiativetracker.repository.RoundRepository

@Composable
fun RoundPrepRoute(
    roundId: Long,
    repo: RoundRepository,
    onBack: () -> Unit,
    onStartCombat: (Long) -> Unit
) {
    val context = LocalContext.current

    val vm: RoundPrepViewModel = viewModel(
        factory = RoundPrepViewModelFactory(
            roundId = roundId,
            repo = repo,
            context = context
        )
    )

    RoundPrepScreen(
        vm = vm,
        onBack = onBack,
        onStartCombat = { onStartCombat(roundId) }
    )
}