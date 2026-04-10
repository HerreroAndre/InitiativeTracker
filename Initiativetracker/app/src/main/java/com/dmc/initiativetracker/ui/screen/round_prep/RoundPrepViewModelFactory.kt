package com.dmc.initiativetracker.ui.screen.round_prep

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dmc.initiativetracker.repository.RoundRepository
import com.dmc.initiativetracker.ui.preferences.SortPreferences

class RoundPrepViewModelFactory(
    private val roundId: Long,
    private val repo: RoundRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoundPrepViewModel::class.java)) {
            return RoundPrepViewModel(
                roundId = roundId,
                repo = repo,
                sortPreferences = SortPreferences(context)
            ) as T
        }
        error("Unknown ViewModel: ${modelClass.name}")
    }
}