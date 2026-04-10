package com.dmc.initiativetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dmc.initiativetracker.di.AppModule
import androidx.compose.ui.platform.LocalContext
import com.dmc.initiativetracker.ui.screen.combat.CombatRoute
import com.dmc.initiativetracker.ui.screen.home.HomeScreen
import com.dmc.initiativetracker.ui.screen.round_prep.RoundPrepRoute
import com.dmc.initiativetracker.ui.screen.round_selector.RoundSelectorScreen
import com.dmc.initiativetracker.ui.screen.settings.SettingsRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.dmc.initiativetracker.ui.theme.ThemeViewModel

@Composable
fun AppNavHost(
    themeViewModel: ThemeViewModel
) {
    val nav = rememberNavController()
    val context = LocalContext.current

    NavHost(navController = nav, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onCreateRound = { roundId ->
                    nav.navigate(Routes.roundPrep(roundId))
                },
                onLoadRound = { nav.navigate(Routes.ROUND_SELECTOR) },
                onSettings = { nav.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.ROUND_SELECTOR) {
            RoundSelectorScreen(
                onOpenRound = { id -> nav.navigate(Routes.roundPrep(id)) },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsRoute(
                onBack = { nav.popBackStack() },
                themeViewModel = themeViewModel
            )
        }

        composable(
            route = Routes.ROUND_PREP,
            arguments = listOf(navArgument("roundId") { type = NavType.LongType })
        ) { backStack ->
            val roundId = backStack.arguments?.getLong("roundId") ?: 0L

            RoundPrepRoute(
                roundId = roundId,
                repo = AppModule.provideRoundRepository(context),
                onBack = { nav.popBackStack() },
                onStartCombat = { id -> nav.navigate(Routes.combat(id)) }
            )
        }

        composable(
            route = Routes.COMBAT,
            arguments = listOf(navArgument("roundId") { type = NavType.LongType })
        ) { backStack ->
            val roundId = backStack.arguments?.getLong("roundId") ?: 0L

            CombatRoute(
                roundId = roundId,
                repo = AppModule.provideRoundRepository(context),
                combatRepo = AppModule.provideCombatRepository(),
                onExit = { nav.popBackStack() }
            )
        }
    }
}