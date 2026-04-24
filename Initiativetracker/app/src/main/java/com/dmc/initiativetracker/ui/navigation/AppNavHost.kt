package com.dmc.initiativetracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.dmc.initiativetracker.ui.screen.image_library.ImageLibraryScreen
import com.dmc.initiativetracker.ui.screen.image_library.ImageLibraryViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

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
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onImageLibrary = { nav.navigate(Routes.imageLibrary()) }

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
            val selectedLibraryImageUri =
                backStack.savedStateHandle
                    .getStateFlow<String?>("selectedLibraryImageUri", null)
                    .collectAsState()
                    .value


            RoundPrepRoute(
                roundId = roundId,
                repo = AppModule.provideRoundRepository(context),
                onBack = { nav.popBackStack() },
                onStartCombat = { id -> nav.navigate(Routes.combat(id)) },
                onOpenImageLibrary = { nav.navigate(Routes.imageLibrary(selectable = true)) },
                selectedLibraryImageUri = selectedLibraryImageUri,
                onConsumeLibraryImageSelection = {
                    backStack.savedStateHandle["selectedLibraryImageUri"] = null
                }
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

        composable(
            route = Routes.IMAGE_LIBRARY,
            arguments = listOf(
                navArgument("selectable") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStack ->
            val selectable = backStack.arguments?.getBoolean("selectable") ?: false

            val factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ImageLibraryViewModel(
                        AppModule.provideImageLibraryRepository(context)
                    ) as T
                }
            }

            val vm: ImageLibraryViewModel = viewModel(factory = factory)

            ImageLibraryScreen(
                vm = vm,
                selectable = selectable,
                onSelectImage = { imageUri ->
                    nav.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selectedLibraryImageUri", imageUri)

                    nav.popBackStack()
                },
                onBack = { nav.popBackStack() }
            )
        }
    }
}