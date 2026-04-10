package com.dmc.initiativetracker.ui.navigation

object Routes {
    const val HOME = "home"
    const val ROUND_SELECTOR = "round_selector"
    const val ROUND_PREP = "round_prep/{roundId}"
    const val COMBAT = "combat/{roundId}"
    const val SETTINGS = "settings"

    fun roundPrep(roundId: Long) = "round_prep/$roundId"
    fun combat(roundId: Long) = "combat/$roundId"
}