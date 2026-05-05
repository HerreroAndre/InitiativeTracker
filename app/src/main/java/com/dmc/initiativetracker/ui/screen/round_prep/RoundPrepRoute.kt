package com.dmc.initiativetracker.ui.screen.round_prep

import androidx.compose.runtime.rememberCoroutineScope
import com.dmc.initiativetracker.di.AppModule
import kotlinx.coroutines.launch
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
    onStartCombat: (Long) -> Unit,
    onOpenImageLibrary: () -> Unit,
    selectedLibraryImageUri: String?,
    onConsumeLibraryImageSelection: () -> Unit
) {
    val context = LocalContext.current
    val imageLibraryRepo = remember(context) {
        AppModule.provideImageLibraryRepository(context)
    }
    val scope = rememberCoroutineScope()

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
        onStartCombat = { onStartCombat(roundId) },
        onOpenImageLibrary = onOpenImageLibrary,
        selectedLibraryImageUri = selectedLibraryImageUri,
        onConsumeLibraryImageSelection = onConsumeLibraryImageSelection,
        onSaveImageToLibrary = { name, imageUri ->
            scope.launch {
                imageLibraryRepo.addImage(name, imageUri)
            }
        }
    )
}