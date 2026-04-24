package com.dmc.initiativetracker.ui.screen.round_selector

import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmc.initiativetracker.di.AppModule
import com.dmc.initiativetracker.domain.model.RoundListItem
import com.dmc.initiativetracker.ui.preferences.SortPreferences
import kotlinx.coroutines.launch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.dmc.initiativetracker.export.RoundCodeCodec
import com.dmc.initiativetracker.export.RoundTransfer
import com.dmc.initiativetracker.export.summary
import androidx.compose.foundation.layout.heightIn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.ContentPaste
import com.dmc.initiativetracker.export.RoundItrFile
import com.dmc.initiativetracker.util.ImageStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundSelectorScreen(
    onOpenRound: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val factory = remember(context) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return RoundSelectorViewModel(
                    repo = AppModule.provideRoundRepository(context),
                    sortPreferences = SortPreferences(context)
                ) as T
            }
        }
    }

    val vm: RoundSelectorViewModel = viewModel(factory = factory)
    val state by vm.uiState.collectAsState()
    var showImportCodeDialog by rememberSaveable { mutableStateOf(false) }
    var importCodeText by rememberSaveable { mutableStateOf("") }
    var pendingImportTransfer by remember { mutableStateOf<RoundTransfer?>(null) }
    var showImportSummaryDialog by rememberSaveable { mutableStateOf(false) }
    var showReplaceRoundDialog by rememberSaveable { mutableStateOf(false) }
    var pendingReplaceRoundId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showImportOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var pendingImportImageBytes by remember { mutableStateOf<Map<String, ByteArray>>(emptyMap()) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.toast) {
        state.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeToast()
        }
    }

    val openItrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        runCatching {
            RoundItrFile.readItr(
                context = context,
                sourceUri = uri
            )
        }.onSuccess { payload ->
            pendingImportTransfer = payload.transfer
            pendingImportImageBytes = payload.images
            showImportSummaryDialog = true
        }.onFailure { error ->
            Toast.makeText(
                context,
                error.message ?: "No se pudo leer el archivo .itr",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    state.confirmDeleteRoundId?.let {
        AlertDialog(
            onDismissRequest = { if (!state.isWorking) vm.cancelDelete() },
            title = { Text("Eliminar ronda") },
            text = { Text("¿Seguro? Esto borra la ronda y sus personajes.") },
            confirmButton = {
                TextButton(
                    enabled = !state.isWorking,
                    onClick = { vm.confirmDelete() }
                ) {
                    Text(if (state.isWorking) "Eliminando..." else "Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isWorking,
                    onClick = vm::cancelDelete
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showImportCodeDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isWorking) showImportCodeDialog = false
            },
            title = { Text("Importar código") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pegá el código exportado desde Round Prep.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Después vas a poder crear una ronda nueva o reemplazar una existente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = importCodeText,
                        onValueChange = { importCodeText = it },
                        enabled = !state.isWorking,
                        label = { Text("Código ITR1") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isWorking,
                    onClick = {
                        try {
                            val transfer = RoundCodeCodec.decode(importCodeText)

                            pendingImportTransfer = transfer
                            showImportCodeDialog = false
                            showImportSummaryDialog = true
                        } catch (t: Throwable) {
                            Toast.makeText(
                                context,
                                t.message ?: "Código inválido",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text(if (state.isWorking) "Importando..." else "Importar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isWorking,
                    onClick = { showImportCodeDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showImportSummaryDialog && pendingImportTransfer != null) {
        val transfer = pendingImportTransfer!!
        val summary = transfer.summary()

        AlertDialog(
            onDismissRequest = {
                if (!state.isWorking) {
                    showImportSummaryDialog = false
                    pendingImportTransfer = null
                    pendingImportImageBytes = emptyMap()
                }
            },
            title = { Text("Importar ronda") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nombre: ${summary.roundName}")
                    Text("Personajes: ${summary.characterCount}")
                    Text("Estados: ${summary.statusCount}")
                    Text("Imágenes: ${summary.imageCount}")

                    Text(
                        text = if (pendingImportImageBytes.isEmpty()) {
                            "El código no incluye imágenes. Los personajes importados aparecerán sin foto."
                        } else {
                            "Este archivo incluye imágenes. Se copiarán al almacenamiento interno de la app."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isWorking,
                        onClick = {
                            val imageUrisByFileName = pendingImportImageBytes.mapValues { entry ->
                                ImageStorage.copyBytesToInternalStorage(
                                    context = context,
                                    bytes = entry.value,
                                    originalFileName = entry.key
                                )
                            }

                            vm.importRoundTransfer(
                                transfer = transfer,
                                replaceRoundId = null,
                                imageUrisByFileName = imageUrisByFileName,
                                onImported = { newRoundId ->
                                    showImportSummaryDialog = false
                                    pendingImportTransfer = null
                                    pendingImportImageBytes = emptyMap()
                                    importCodeText = ""
                                    onOpenRound(newRoundId)
                                }
                            )
                        }
                    ) {
                        Text("Crear ronda nueva")
                    }

                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isWorking && state.rounds.isNotEmpty(),
                        onClick = {
                            showImportSummaryDialog = false
                            showReplaceRoundDialog = true
                        }
                    ) {
                        Text("Reemplazar ronda existente")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isWorking,
                    onClick = {
                        showImportSummaryDialog = false
                        pendingImportTransfer = null
                        pendingImportImageBytes = emptyMap()
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showReplaceRoundDialog && pendingImportTransfer != null) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isWorking) {
                    showReplaceRoundDialog = false
                    showImportSummaryDialog = true
                }
            },
            title = { Text("Reemplazar ronda") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Elegí qué ronda querés reemplazar.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.rounds, key = { it.id }) { round ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            pendingReplaceRoundId = round.id
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = round.name.ifBlank { "Sin nombre" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        text = "${round.characterCount} personajes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    pendingReplaceRoundId?.let { selectedId ->
                        val selectedRound = state.rounds.firstOrNull { it.id == selectedId }

                        Text(
                            text = "Seleccionada: ${selectedRound?.name ?: "Ronda"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isWorking && pendingReplaceRoundId != null,
                    onClick = {
                        val transfer = pendingImportTransfer ?: return@TextButton
                        val replaceId = pendingReplaceRoundId ?: return@TextButton

                        val imageUrisByFileName = pendingImportImageBytes.mapValues { entry ->
                            ImageStorage.copyBytesToInternalStorage(
                                context = context,
                                bytes = entry.value,
                                originalFileName = entry.key
                            )
                        }

                        vm.importRoundTransfer(
                            transfer = transfer,
                            replaceRoundId = replaceId,
                            imageUrisByFileName = imageUrisByFileName,
                            onImported = { importedRoundId ->
                                showReplaceRoundDialog = false
                                pendingImportTransfer = null
                                pendingImportImageBytes = emptyMap()
                                pendingReplaceRoundId = null
                                importCodeText = ""
                                onOpenRound(importedRoundId)
                            }
                        )
                    }
                ) {
                    Text(if (state.isWorking) "Reemplazando..." else "Confirmar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isWorking,
                    onClick = {
                        showReplaceRoundDialog = false
                        pendingReplaceRoundId = null
                        showImportSummaryDialog = true
                    }
                ) {
                    Text("Volver")
                }
            }
        )
    }

    if (showImportOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showImportOptionsDialog = false },
            title = { Text("Importar ronda") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Elegí cómo querés importar la ronda.")

                    Text(
                        text = "El código no incluye imágenes. El archivo .itr puede incluirlas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showImportOptionsDialog = false
                            importCodeText = ""
                            pendingImportImageBytes = emptyMap()
                            showImportCodeDialog = true
                        }
                    ) {
                        Text("Pegar código")
                    }

                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showImportOptionsDialog = false
                            openItrLauncher.launch(
                                arrayOf(
                                    "application/octet-stream",
                                    "application/zip",
                                    "*/*"
                                )
                            )
                        }
                    ) {
                        Text("Elegir archivo .itr")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportOptionsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cargar ronda",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (state.rounds.isEmpty()) {
                                "Sin rondas guardadas"
                            } else {
                                "${state.rounds.size} rondas disponibles"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(
                        enabled = !state.isWorking,
                        onClick = {
                            importCodeText = ""
                            pendingImportTransfer = null
                            pendingImportImageBytes = emptyMap()
                            pendingReplaceRoundId = null
                            showImportOptionsDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Importar ronda"
                        )
                    }
                    Box {
                        IconButton(onClick = vm::openSortMenu) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Ordenar"
                            )
                        }

                        DropdownMenu(
                            expanded = state.isSortMenuOpen,
                            onDismissRequest = vm::closeSortMenu
                        ) {
                            RoundSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        val currentIndex = listState.firstVisibleItemIndex
                                        val currentOffset = listState.firstVisibleItemScrollOffset

                                        vm.selectSort(option)

                                        scope.launch {
                                            listState.scrollToItem(
                                                index = currentIndex.coerceAtMost(
                                                    (state.rounds.size - 1).coerceAtLeast(0)
                                                ),
                                                scrollOffset = currentOffset
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (option == state.sortOption) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Seleccionado"
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.rounds.isEmpty()) {
            EmptyRoundsState(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.rounds, key = { it.id }) { round ->
                    RoundCard(
                        round = round,
                        onOpen = { onOpenRound(round.id) },
                        onDelete = { vm.requestDelete(round.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRoundsState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No hay rondas guardadas",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Volvé atrás y creá una ronda nueva.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RoundCard(
    round: RoundListItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = round.name.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Creada: ${formatCreatedAt(round.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoBadge(
                        label = if (round.characterCount == 1) {
                            "1 personaje"
                        } else {
                            "${round.characterCount} personajes"
                        },
                        highlighted = true
                    )
                }

                Spacer(Modifier.width(8.dp))

                FilledTonalButton(onClick = onDelete) {
                    Text("Eliminar")
                }
            }
        }
    }
}

@Composable
private fun InfoBadge(
    label: String,
    highlighted: Boolean = false
) {
    val background = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val content = if (highlighted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        color = background,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = content
        )
    }
}

@Composable
private fun formatCreatedAt(createdAt: Long): String {
    return remember(createdAt) {
        DateFormat.format("dd/MM/yyyy HH:mm", createdAt).toString()
    }
}