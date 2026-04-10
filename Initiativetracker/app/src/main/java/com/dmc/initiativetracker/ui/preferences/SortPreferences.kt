package com.dmc.initiativetracker.ui.preferences

import android.content.Context
import com.dmc.initiativetracker.ui.screen.round_prep.RoundPrepSortOption
import com.dmc.initiativetracker.ui.screen.round_selector.RoundSortOption

class SortPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ROUND_SELECTOR_SORT = "round_selector_sort"
        private const val KEY_ROUND_PREP_SORT = "round_prep_sort"
    }

    fun getRoundSelectorSort(): RoundSortOption {
        val raw = prefs.getString(
            KEY_ROUND_SELECTOR_SORT,
            RoundSortOption.CREATED_DESC.name
        ) ?: RoundSortOption.CREATED_DESC.name

        return RoundSortOption.entries.firstOrNull { it.name == raw }
            ?: RoundSortOption.CREATED_DESC
    }

    fun setRoundSelectorSort(option: RoundSortOption) {
        prefs.edit()
            .putString(KEY_ROUND_SELECTOR_SORT, option.name)
            .apply()
    }

    fun getRoundPrepSort(): RoundPrepSortOption {
        val raw = prefs.getString(
            KEY_ROUND_PREP_SORT,
            RoundPrepSortOption.INITIATIVE_DESC.name
        ) ?: RoundPrepSortOption.INITIATIVE_DESC.name

        return RoundPrepSortOption.entries.firstOrNull { it.name == raw }
            ?: RoundPrepSortOption.INITIATIVE_DESC
    }

    fun setRoundPrepSort(option: RoundPrepSortOption) {
        prefs.edit()
            .putString(KEY_ROUND_PREP_SORT, option.name)
            .apply()
    }
}