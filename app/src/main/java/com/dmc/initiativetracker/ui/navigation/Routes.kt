package com.dmc.initiativetracker.ui.navigation

object Routes {
    const val HOME = "home"
    const val ROUND_SELECTOR = "round_selector"
    const val ROUND_PREP = "round_prep/{roundId}"
    const val COMBAT = "combat/{roundId}"
    const val SETTINGS = "settings"
    const val IMAGE_LIBRARY = "image_library?selectable={selectable}"

    fun roundPrep(roundId: Long) = "round_prep/$roundId"
    fun combat(roundId: Long) = "combat/$roundId"
    fun imageLibrary(selectable: Boolean = false) = "image_library?selectable=$selectable"
}