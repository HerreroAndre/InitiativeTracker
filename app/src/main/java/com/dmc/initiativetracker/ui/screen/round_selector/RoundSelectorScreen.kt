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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.foundation.layout.FlowRow

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
                    imageLibraryRepo = AppModule.provideImageLibraryRepository(context),
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

    state.confirmDeleteRoundId?.let { roundId ->
        val roundToDelete = state.rounds.firstOrNull { it.id == roundId }
        val roundName = roundToDelete
            ?.name
            ?.ifBlank { "Sin nombre" }
            ?: "esta ronda"

        AlertDialog(
            onDismissRequest = { if (!state.isWorking) vm.cancelDelete() },
            title = { Text("Eliminar ronda") },
            text = {
                Text(
                    text = "¿Seguro que querés eliminar “$roundName”?\n\n" +
                            "Se borrarán la ronda, sus personajes y sus estados. Esta acción no se puede deshacer."
                )
            },
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
                        text = "Después vas a elegir si crear una ronda nueva o reemplazar una existente.",
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
                    Text(if (state.isWorking) "Importando..." else "Revisar")
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
        val hasImages = pendingImportImageBytes.isNotEmpty()

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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = summary.roundName.ifBlank { "Ronda importada" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfoBadge(
                            label = if (summary.characterCount == 1) {
                                "1 personaje"
                            } else {
                                "${summary.characterCount} personajes"
                            },
                            highlighted = true
                        )

                        InfoBadge(
                            label = if (summary.statusCount == 1) {
                                "1 estado"
                            } else {
                                "${summary.statusCount} estados"
                            }
                        )

                        InfoBadge(
                            label = if (summary.imageCount == 1) {
                                "1 imagen"
                            } else {
                                "${summary.imageCount} imágenes"
                            }
                        )
                    }

                    Text(
                        text = if (hasImages) {
                            "Este archivo incluye imágenes. Se copiarán al almacenamiento interno y a la biblioteca de la app."
                        } else {
                            "Este código no incluye imágenes. Los personajes importados aparecerán sin foto."
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

                            val libraryImageUrisByFileName = pendingImportImageBytes.mapValues { entry ->
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
                                libraryImageUrisByFileName = libraryImageUrisByFileName,
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
                        Text(if (state.isWorking) "Importando..." else "Crear nueva ronda")
                    }

                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isWorking && state.rounds.isNotEmpty(),
                        onClick = {
                            showImportSummaryDialog = false
                            showReplaceRoundDialog = true
                        }
                    ) {
                        Text("Reemplazar ronda")
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Elegí qué ronda será reemplazada.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Su contenido actual se borrará y será sustituido por la ronda importada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.rounds, key = { it.id }) { round ->
                            val selected = pendingReplaceRoundId == round.id

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            pendingReplaceRoundId = round.id
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = round.name.ifBlank { "Sin nombre" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = if (round.characterCount == 1) {
                                                "1 personaje"
                                            } else {
                                                "${round.characterCount} personajes"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Seleccionada",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    pendingReplaceRoundId?.let { selectedId ->
                        val selectedRound = state.rounds.firstOrNull { it.id == selectedId }

                        Text(
                            text = "Seleccionada: ${selectedRound?.name?.ifBlank { "Sin nombre" } ?: "Ronda"}",
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

                        val libraryImageUrisByFileName = pendingImportImageBytes.mapValues { entry ->
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
                            libraryImageUrisByFileName = libraryImageUrisByFileName,
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
                    Text(if (state.isWorking) "Reemplazando..." else "Reemplazar")
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Elegí el método de importación.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    ImportOptionCard(
                        title = "Código",
                        description = "Importa personajes, HP, estados y orden. No incluye imágenes.",
                        buttonText = "Importar código",
                        onClick = {
                            showImportOptionsDialog = false
                            importCodeText = ""
                            pendingImportImageBytes = emptyMap()
                            showImportCodeDialog = true
                        }
                    )

                    ImportOptionCard(
                        title = "Archivo .itr",
                        description = "Importa la ronda completa y puede incluir imágenes.",
                        buttonText = "Importar archivo .itr",
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
                    )
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
                    .fillMaxSize(),
                onImport = {
                    importCodeText = ""
                    pendingImportTransfer = null
                    pendingImportImageBytes = emptyMap()
                    pendingReplaceRoundId = null
                    showImportOptionsDialog = true
                }
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
    modifier: Modifier = Modifier,
    onImport: () -> Unit
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "No hay rondas guardadas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Creá una ronda nueva desde el inicio o importá una existente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FilledTonalButton(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Importar ronda")
                }
            }
        }
    }
}

@Composable
private fun ImportOptionCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FilledTonalButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = round.name.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                InfoBadge(
                    label = if (round.characterCount == 1) {
                        "1 personaje"
                    } else {
                        "${round.characterCount} personajes"
                    },
                    highlighted = true
                )

                Spacer(Modifier.weight(1f))

                TextButton(onClick = onOpen) {
                    Text("Abrir")
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar ronda"
                    )
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
        shape = RoundedCornerShape(999.dp)
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