package com.dmc.initiativetracker.ui.screen.round_prep

import android.Manifest
import android.content.ClipData
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.CharacterType
import com.dmc.initiativetracker.domain.model.Status
import com.dmc.initiativetracker.domain.model.StatusTickTiming
import com.dmc.initiativetracker.domain.model.StatusType
import com.dmc.initiativetracker.export.RoundCodeCodec
import com.dmc.initiativetracker.export.RoundItrExportPayload
import com.dmc.initiativetracker.export.RoundItrFile
import com.dmc.initiativetracker.export.buildRoundTransfer
import com.dmc.initiativetracker.ui.theme.statusContainerColor
import com.dmc.initiativetracker.ui.theme.statusContentColor
import com.dmc.initiativetracker.util.ImageStorage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundPrepScreen(
    vm: RoundPrepViewModel,
    onBack: () -> Unit,
    onStartCombat: () -> Unit,
    onOpenImageLibrary: () -> Unit,
    selectedLibraryImageUri: String?,
    onConsumeLibraryImageSelection: () -> Unit,
    onSaveImageToLibrary: (String, String) -> Unit
){
    val state by vm.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var scrollToTopAfterSort by rememberSaveable { mutableStateOf(false) }
    var pendingLibraryCharacterId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingSearchQuery by rememberSaveable { mutableStateOf("") }
    var showDiscardEditDialog by rememberSaveable { mutableStateOf(false) }
    var pendingScrollToNewCharacter by rememberSaveable { mutableStateOf(false) }

    fun requestLeaveScreen() {
        if (state.isSaving) return

        if (state.isEditing) {
            showDiscardEditDialog = true
        } else {
            onBack()
        }
    }

    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        vm.clearError()
    }

    LaunchedEffect(state.toast) {
        val msg = state.toast ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        vm.consumeToast()
    }

    LaunchedEffect(selectedLibraryImageUri) {
        val imageUri = selectedLibraryImageUri ?: return@LaunchedEffect
        val characterId = pendingLibraryCharacterId ?: return@LaunchedEffect

        val target = state.draft.firstOrNull { it.id == characterId } ?: return@LaunchedEffect

        val copiedUri = runCatching {
            ImageStorage.copyToInternalStorage(
                context = context,
                sourceUri = imageUri.toUri()
            )
        }.getOrElse {
            pendingLibraryCharacterId = null
            onConsumeLibraryImageSelection()
            Toast.makeText(
                context,
                "No se pudo copiar la imagen de la biblioteca",
                Toast.LENGTH_SHORT
            ).show()
            return@LaunchedEffect
        }

        vm.updateDraftCharacter(
            target.copy(imageUri = copiedUri)
        )

        pendingLibraryCharacterId = null
        onConsumeLibraryImageSelection()
    }

    LaunchedEffect(state.sortOption, state.shownCharacters.firstOrNull()?.id) {
        if (scrollToTopAfterSort) {
            listState.scrollToItem(0)
            scrollToTopAfterSort = false
        }
    }

    LaunchedEffect(state.isEditing) {
        if (!state.isEditing) {
            editingSearchQuery = ""
            pendingScrollToNewCharacter = false
            showDiscardEditDialog = false
        }
    }

    LaunchedEffect(
        pendingScrollToNewCharacter,
        state.draft.size,
        state.shownCharacters.size,
        state.isEditing
    ) {
        if (pendingScrollToNewCharacter && state.isEditing) {
            val newCharacterIndex = state.shownCharacters.indexOfLast { it.id < 0 }

            if (newCharacterIndex >= 0) {
                // +1 porque en modo edición el primer item del LazyColumn es la barra de búsqueda.
                listState.animateScrollToItem(newCharacterIndex + 1)
                pendingScrollToNewCharacter = false
            }
        }
    }

    BackHandler(enabled = state.isEditing) {
        if (!state.isSaving) {
            showDiscardEditDialog = true
        }
    }

    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember(state.roundName) { mutableStateOf(state.roundName) }
    val canRename = renameText.trim().isNotBlank() && !state.isSaving
    var selectedStatusCharacterId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showAddStatusDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteCharacter by rememberSaveable { mutableStateOf<Character?>(null) }
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var pendingItrPayload by remember { mutableStateOf<RoundItrExportPayload?>(null) }
    var pendingShareCode by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingShareItrUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingShareItrName by rememberSaveable { mutableStateOf("") }

    if (renameOpen) {
        AlertDialog(
            onDismissRequest = { if (!state.isSaving) renameOpen = false },
            title = { Text("Renombrar ronda") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    enabled = !state.isSaving,
                    label = { Text("Nombre") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!canRename) return@KeyboardActions
                            focusManager.clearFocus()
                            vm.renameRound(renameText)
                            renameOpen = false
                        }
                    )
                )
            },
            confirmButton = {
                Button(
                    enabled = canRename,
                    onClick = {
                        focusManager.clearFocus()
                        vm.renameRound(renameText)
                        renameOpen = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isSaving,
                    onClick = { renameOpen = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showAddStatusDialog && selectedStatusCharacterId != null) {
        val availableConcentrations = buildConcentrationOptions(
            statuses = state.statuses,
            characters = state.shownCharacters
        )

        AddPreCombatStatusDialog(
            availableConcentrations = availableConcentrations,
            onDismiss = {
                showAddStatusDialog = false
                selectedStatusCharacterId = null
            },
            onConfirm = { name, type, duration, tickTiming, isConcentration, linkedConcentration ->
                vm.addPreCombatStatus(
                    characterId = selectedStatusCharacterId ?: return@AddPreCombatStatusDialog,
                    name = name,
                    type = type,
                    durationRounds = duration,
                    tickTiming = tickTiming,
                    isConcentration = isConcentration,
                    linkedConcentrationGroupId = linkedConcentration?.groupId,
                    linkedOriginCharacterId = linkedConcentration?.originCharacterId,
                    linkedOriginLabel = linkedConcentration?.originLabel
                )
                showAddStatusDialog = false
                selectedStatusCharacterId = null
            }
        )
    }

    state.confirmRemoveStatusId?.let { statusId ->
        val statusToRemove = state.statuses.firstOrNull { it.id == statusId }

        if (statusToRemove != null) {
            AlertDialog(
                onDismissRequest = { if (!state.isSaving) vm.cancelRemovePreCombatStatus() },
                title = { Text("Quitar estado") },
                text = {
                    Text("Se eliminará ${formatRoundPrepStatus(statusToRemove)}. ¿Continuar?")
                },
                confirmButton = {
                    TextButton(
                        enabled = !state.isSaving,
                        onClick = { vm.confirmRemovePreCombatStatus() }
                    ) {
                        Text("Quitar")
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !state.isSaving,
                        onClick = vm::cancelRemovePreCombatStatus
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }

    pendingDeleteCharacter?.let { character ->
        AlertDialog(
            onDismissRequest = {
                if (!state.isSaving) pendingDeleteCharacter = null
            },
            title = { Text("Eliminar personaje") },
            text = {
                Text(
                    "¿Seguro que querés eliminar a ${
                        character.characterName.ifBlank { character.playerName.ifBlank { "este personaje" } }
                    }?"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isSaving,
                    onClick = {
                        vm.removeDraftCharacter(character.id)
                        pendingDeleteCharacter = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isSaving,
                    onClick = { pendingDeleteCharacter = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    fun shareText(text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        context.startActivity(
            Intent.createChooser(sendIntent, "Compartir código de ronda")
        )
    }

    fun shareItr(uriString: String, fileName: String) {
        val uri = uriString.toUri()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            putExtra(Intent.EXTRA_TITLE, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(sendIntent, "Compartir archivo .itr")
        )
    }

    val createItrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val payload = pendingItrPayload
        pendingItrPayload = null

        if (uri == null || payload == null) return@rememberLauncherForActivityResult

        runCatching {
            RoundItrFile.writeItr(
                context = context,
                destinationUri = uri,
                payload = payload
            )
        }.onSuccess {
            val fileName = "${safeItrFileName(state.roundName)}.itr"
            val shareUri = RoundItrFile.writeItrToCache(
                context = context,
                fileName = fileName,
                payload = payload
            )

            pendingShareItrUri = shareUri.toString()
            pendingShareItrName = fileName

            Toast.makeText(
                context,
                "Archivo .itr exportado",
                Toast.LENGTH_SHORT
            ).show()
        }.onFailure { error ->
            Toast.makeText(
                context,
                error.message ?: "No se pudo exportar el archivo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exportar ronda") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Elegí cómo querés exportar esta ronda.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "El código no incluye imágenes. El archivo .itr incluye imágenes si existen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val transfer = buildRoundTransfer(
                                roundName = state.roundName,
                                characters = state.characters,
                                statuses = state.statuses
                            )

                            val code = RoundCodeCodec.encode(transfer)

                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(
                                        ClipData.newPlainText("Código de ronda", code)
                                    )
                                )
                            }

                            Toast.makeText(
                                context,
                                "Código de ronda copiado",
                                Toast.LENGTH_SHORT
                            ).show()

                            pendingShareCode = code
                            showExportDialog = false
                        }
                    ) {
                        Text("Copiar código sin imágenes")
                    }

                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val payload = RoundItrFile.buildExportPayload(
                                roundName = state.roundName,
                                characters = state.characters,
                                statuses = state.statuses
                            )

                            pendingItrPayload = payload

                            createItrLauncher.launch(
                                "${safeItrFileName(state.roundName)}.itr"
                            )

                            showExportDialog = false
                        }
                    ) {
                        Text("Exportar archivo .itr")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showExportDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDiscardEditDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isSaving) {
                    showDiscardEditDialog = false
                }
            },
            title = { Text("Descartar cambios") },
            text = {
                Text(
                    text = "Tenés cambios sin guardar en esta ronda.\n\n" +
                            "Si volvés al modo normal, se descartará lo que editaste."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isSaving,
                    onClick = {
                        focusManager.clearFocus(force = true)
                        showDiscardEditDialog = false
                        vm.cancelEdit()
                    }
                ) {
                    Text("Descartar cambios")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isSaving,
                    onClick = { showDiscardEditDialog = false }
                ) {
                    Text("Seguir editando")
                }
            }
        )
    }

    pendingShareCode?.let { code ->
        AlertDialog(
            onDismissRequest = { pendingShareCode = null },
            title = { Text("Código copiado") },
            text = {
                Text("El código de ronda ya está en el portapapeles. ¿Querés compartirlo ahora?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        shareText(code)
                        pendingShareCode = null
                    }
                ) {
                    Text("Compartir")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingShareCode = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    pendingShareItrUri?.let { uriString ->
        AlertDialog(
            onDismissRequest = {
                pendingShareItrUri = null
                pendingShareItrName = ""
            },
            title = { Text("Archivo exportado") },
            text = {
                Text("El archivo $pendingShareItrName fue creado. ¿Querés compartirlo ahora?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        shareItr(
                            uriString = uriString,
                            fileName = pendingShareItrName
                        )
                        pendingShareItrUri = null
                        pendingShareItrName = ""
                    }
                ) {
                    Text("Compartir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingShareItrUri = null
                        pendingShareItrName = ""
                    }
                ) {
                    Text("Cerrar")
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
                            text = state.roundName.ifBlank { "Ronda" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (state.isEditing) "Modo edición" else "Preparar ronda",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { requestLeaveScreen() },
                        enabled = !state.isSaving
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    if (!state.isEditing) {
                        IconButton(
                            onClick = {
                                renameText = state.roundName
                                renameOpen = true
                            },
                            enabled = !state.isSaving
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Renombrar")
                        }

                        IconButton(
                            enabled = state.characters.isNotEmpty() && !state.isSaving,
                            onClick = { showExportDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Exportar ronda"
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = vm::openSortMenu,
                            enabled = !state.isSaving
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Ordenar")
                        }

                        DropdownMenu(
                            expanded = state.isSortMenuOpen,
                            onDismissRequest = vm::closeSortMenu
                        ) {
                            RoundPrepSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        scrollToTopAfterSort = true
                                        vm.selectSort(option)
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
        },

        floatingActionButton = {
            if (state.isEditing) {
                FloatingActionButton(
                    onClick = {
                        if (!state.isSaving) {
                            editingSearchQuery = ""
                            pendingScrollToNewCharacter = true
                            vm.addCharacterToDraft()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar personaje"
                    )
                }
            }
        },


        bottomBar = {
            RoundPrepBottomBar(
                state = state,
                onEnterEdit = vm::enterEditMode,
                onPlay = onStartCombat,
                onCancelEdit = vm::cancelEdit,
                onSave = {
                    focusManager.clearFocus(force = true)
                    vm.confirmEdit()
                }
            )
        }
    ) { padding ->
        RoundPrepContent(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            state = state,
            listState = listState,
            searchQuery = editingSearchQuery,
            onSearchQueryChange = { editingSearchQuery = it },
            onUpdateDraft = vm::updateDraftCharacter,
            onDeleteDraft = { characterId ->
                pendingDeleteCharacter = state.draft.firstOrNull { it.id == characterId }
            },
            onAddStatus = { characterId ->
                selectedStatusCharacterId = characterId
                showAddStatusDialog = true
            },
            onRemoveStatus = vm::requestRemovePreCombatStatus,
            onOpenLibrary = { characterId ->
                pendingLibraryCharacterId = characterId
                onOpenImageLibrary()
            },
            onSaveImageToLibrary = onSaveImageToLibrary
        )
    }
}

@Composable
private fun RoundPrepContent(
    modifier: Modifier = Modifier,
    state: RoundPrepUiState,
    listState: LazyListState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onUpdateDraft: (Character) -> Unit,
    onDeleteDraft: (Long) -> Unit,
    onAddStatus: (Long) -> Unit,
    onRemoveStatus: (Long) -> Unit,
    onOpenLibrary: (Long) -> Unit,
    onSaveImageToLibrary: (String, String) -> Unit
) {
    val sourceList = state.shownCharacters

    val query = searchQuery.trim()

    val list = remember(sourceList, query, state.isEditing) {
        if (state.isEditing && query.isNotBlank()) {
            sourceList.filter { character ->
                character.characterName.contains(query, ignoreCase = true) ||
                        character.playerName.contains(query, ignoreCase = true)
            }
        } else {
            sourceList
        }
    }

    val statusesByCharacter = remember(state.statuses) {
        state.statuses.groupBy { it.characterId }
    }

    Box(modifier) {
        when {
            state.roundName.isBlank() && state.characters.isEmpty() && !state.isEditing -> {
                LoadingCentered()
            }

            sourceList.isEmpty() -> {
                EmptyCentered(
                    title = "No hay personajes",
                    subtitle = if (state.isEditing) {
                        "Agregá personajes con el botón +"
                    } else {
                        "Entrá en edición para cargar integrantes"
                    }
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.isEditing) {
                        item(key = "round_prep_search") {
                            RoundPrepSearchField(
                                query = searchQuery,
                                onQueryChange = onSearchQueryChange
                            )
                        }
                    }

                    if (list.isEmpty()) {
                        item(key = "empty_search_result") {
                            EmptySearchResult(query = query)
                        }
                    } else {
                        items(
                            items = list,
                            key = { it.id }
                        ) { character ->
                            CharacterCard(
                                character = character,
                                statuses = statusesByCharacter[character.id].orEmpty(),
                                isEditing = state.isEditing,
                                onUpdate = onUpdateDraft,
                                onDelete = { onDeleteDraft(character.id) },
                                onAddStatus = { onAddStatus(character.id) },
                                onRemoveStatus = onRemoveStatus,
                                onOpenLibrary = { onOpenLibrary(character.id) },
                                onSaveImageToLibrary = onSaveImageToLibrary
                            )
                        }
                    }
                }
            }
        }

        if (state.isSaving) {
            SavingOverlay()
        }
    }
}

@Composable
private fun RoundPrepSearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("Buscar") },
        placeholder = { Text("Personaje o jugador") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpiar búsqueda"
                    )
                }
            }
        }
    )
}

@Composable
private fun EmptySearchResult(
    query: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Sin resultados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "No encontré personajes o jugadores que coincidan con “$query”.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CharacterCard(
    character: Character,
    statuses: List<Status>,
    isEditing: Boolean,
    onUpdate: (Character) -> Unit,
    onDelete: () -> Unit,
    onAddStatus: () -> Unit,
    onRemoveStatus: (Long) -> Unit,
    onOpenLibrary: () -> Unit,
    onSaveImageToLibrary: (String, String) -> Unit
) {
    var expanded by rememberSaveable(character.id, isEditing) {
        mutableStateOf(!isEditing || character.id < 0)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isEditing) {
                EditableCharacterSummaryHeader(
                    character = character,
                    statusCount = statuses.size,
                    expanded = expanded,
                    onToggleExpanded = { expanded = !expanded },
                    onDelete = onDelete
                )

                if (expanded) {
                    HorizontalDivider()

                    CharacterEditNamesSection(
                        character = character,
                        onUpdate = onUpdate
                    )

                    CharacterEditSection(
                        character = character,
                        onUpdate = onUpdate,
                        onOpenLibrary = onOpenLibrary,
                        onSaveImageToLibrary = onSaveImageToLibrary
                    )

                    HorizontalDivider()

                    CharacterStatusSection(
                        statuses = statuses,
                        isEditing = true,
                        onAddStatus = onAddStatus,
                        onRemoveStatus = onRemoveStatus
                    )
                }
            } else {
                CharacterCardHeader(character = character)

                CharacterInfoSection(character = character)

                HorizontalDivider()

                CharacterStatusSection(
                    statuses = statuses,
                    isEditing = false,
                    onAddStatus = onAddStatus,
                    onRemoveStatus = onRemoveStatus
                )
            }
        }
    }
}

@Composable
private fun EditableCharacterSummaryHeader(
    character: Character,
    statusCount: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val fallbackPainter = rememberVectorPainter(Icons.Default.Person)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(character.imageUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
            placeholder = fallbackPainter,
            error = fallbackPainter,
            fallback = fallbackPainter
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = character.characterName.ifBlank { "Sin nombre" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = character.playerName.ifBlank { "Sin jugador" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoBadge(
                    label = "Init ${formatInitiative(character.initiative)}",
                    highlighted = true
                )

                InfoBadge(
                    label = "HP ${(character.currentHp?.toString() ?: "?")}/${character.maxHp?.toString() ?: "?"}"
                )

                if (character.tempHp > 0) {
                    InfoBadge(label = "HP temp ${character.tempHp}")
                }

                InfoBadge(
                    label = if (character.type == CharacterType.PLAYER) "PLAYER" else "NPC"
                )

                InfoBadge(label = "Estados $statusCount")

                if (!character.isActive) {
                    InfoBadge(label = "Inactivo")
                }

                if (character.isDead) {
                    InfoBadge(label = "Muerto", danger = true)
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onToggleExpanded) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Contraer personaje" else "Expandir personaje"
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar personaje"
                )
            }
        }
    }
}

@Composable
private fun CharacterEditNamesSection(
    character: Character,
    onUpdate: (Character) -> Unit
) {
    val focusManager = LocalFocusManager.current

    var playerNameText by rememberSaveable(character.id) {
        mutableStateOf(character.playerName)
    }

    var characterNameText by rememberSaveable(character.id) {
        mutableStateOf(character.characterName)
    }

    fun commitNames() {
        if (
            playerNameText != character.playerName ||
            characterNameText != character.characterName
        ) {
            onUpdate(
                character.copy(
                    playerName = playerNameText,
                    characterName = characterNameText
                )
            )
        }
    }

    Column(
        modifier = Modifier.onFocusChanged { focusState ->
            if (!focusState.hasFocus) {
                commitNames()
            }
        },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = playerNameText,
            onValueChange = { playerNameText = it },
            label = { Text("Jugador") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    commitNames()
                    focusManager.moveFocus(FocusDirection.Next)
                }
            )
        )

        OutlinedTextField(
            value = characterNameText,
            onValueChange = { characterNameText = it },
            label = { Text("Personaje") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    commitNames()
                    focusManager.moveFocus(FocusDirection.Next)
                }
            )
        )
    }
}

@Composable
private fun CharacterCardHeader(
    character: Character
) {
    val context = LocalContext.current
    val fallbackPainter = rememberVectorPainter(Icons.Default.Person)

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(character.imageUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop,
            placeholder = fallbackPainter,
            error = fallbackPainter,
            fallback = fallbackPainter
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = character.characterName.ifBlank { "Sin nombre" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = character.playerName.ifBlank { "Sin jugador" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoBadge(
                    label = "Init ${formatInitiative(character.initiative)}",
                    highlighted = true
                )

                InfoBadge(
                    label = if (character.type == CharacterType.PLAYER) "PLAYER" else "NPC"
                )

                if (!character.isActive) {
                    InfoBadge(label = "Inactivo")
                }

                if (character.isDead) {
                    InfoBadge(label = "Muerto", danger = true)
                }
            }
        }
    }
}

@Composable
private fun CharacterInfoSection(
    character: Character
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoBadge(
                label = "HP ${(character.currentHp?.toString() ?: "?")}/${character.maxHp?.toString() ?: "?"}",
                highlighted = true
            )

            if (character.tempHp > 0) {
                InfoBadge(label = "HP temp ${character.tempHp}")
            }
        }
    }
}

@Composable
private fun CharacterEditSection(
    character: Character,
    onUpdate: (Character) -> Unit,
    onOpenLibrary: () -> Unit,
    onSaveImageToLibrary: (String, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var currentHpText by remember(character.id) { mutableStateOf(character.currentHp?.toString() ?: "") }
    var maxHpText by remember(character.id) { mutableStateOf(character.maxHp?.toString() ?: "") }
    var pendingCameraUriString by rememberSaveable(character.id) { mutableStateOf<String?>(null) }

    var initText by remember(character.id) {
        val initial = formatInitiative(character.initiative)

        mutableStateOf(
            TextFieldValue(
                text = initial,
                selection = TextRange(initial.length)
            )
        )
    }

    var tempHpText by remember(character.id) {
        val initial = character.tempHp.toString()

        mutableStateOf(
            TextFieldValue(
                text = initial,
                selection = TextRange(initial.length)
            )
        )
    }

    fun commitStats() {
        val normalizedInitiative = initText.text
            .trim()
            .replace(',', '.')
            .toDoubleOrNull()
            ?: character.initiative

        val updated = character.copy(
            initiative = normalizedInitiative,
            currentHp = currentHpText.trim().toIntOrNull(),
            maxHp = maxHpText.trim().toIntOrNull(),
            tempHp = tempHpText.text.trim().toIntOrNull() ?: 0
        )

        if (updated != character) {
            onUpdate(updated)
        }
    }

    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val pending = pendingCameraUriString

        if (success && pending != null) {
            val libraryUri = runCatching {
                ImageStorage.copyToInternalStorage(
                    context = context,
                    sourceUri = pending.toUri()
                )
            }.getOrNull()

            ImageStorage.deleteIfInternal(context, character.imageUri)
            onUpdate(character.copy(imageUri = pending))

            libraryUri?.let {
                onSaveImageToLibrary(
                    suggestedLibraryImageName(character),
                    it
                )
            }
        } else {
            ImageStorage.deleteFileUri(pending)
        }

        pendingCameraUriString = null
    }

    val requestCameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val outUri = ImageStorage.createCameraOutputUri(context)
            pendingCameraUriString = outUri.toString()
            takePicture.launch(outUri)
        }
    }

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val characterUri = ImageStorage.copyToInternalStorage(context, uri)
                val libraryUri = ImageStorage.copyToInternalStorage(context, uri)

                ImageStorage.deleteIfInternal(context, character.imageUri)

                onUpdate(character.copy(imageUri = characterUri))
                onSaveImageToLibrary(
                    suggestedLibraryImageName(character),
                    libraryUri
                )
            }.onFailure {
                Toast.makeText(
                    context,
                    "No se pudo cargar la imagen",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val launchCameraWithPermission = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            val outUri = ImageStorage.createCameraOutputUri(context)
            pendingCameraUriString = outUri.toString()
            takePicture.launch(outUri)
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier.onFocusChanged { focusState ->
            if (!focusState.hasFocus) {
                commitStats()
            }
        },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = initText,
                onValueChange = { field ->
                    val normalized = field.text.replace(',', '.')

                    if (isValidDecimalInput(normalized)) {
                        initText = field
                    }
                },
                label = { Text("Iniciativa") },
                placeholder = {
                    Text(formatInitiative(character.initiative))
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        val original = formatInitiative(character.initiative)

                        if (focusState.isFocused && initText.text == original) {
                            initText = TextFieldValue(
                                text = "",
                                selection = TextRange(0)
                            )
                        }

                        if (!focusState.isFocused && initText.text.isBlank()) {
                            initText = TextFieldValue(
                                text = original,
                                selection = TextRange(original.length)
                            )
                        }
                    },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        commitStats()
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                )
            )

            OutlinedTextField(
                value = currentHpText,
                onValueChange = { txt ->
                    if (txt.isBlank() || txt.all(Char::isDigit)) {
                        currentHpText = txt
                    }
                },
                label = { Text("HP actual") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        commitStats()
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = maxHpText,
                onValueChange = { txt ->
                    if (txt.isBlank() || txt.all(Char::isDigit)) {
                        maxHpText = txt
                    }
                },
                label = { Text("HP máximo") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        commitStats()
                        focusManager.moveFocus(FocusDirection.Next)
                    }
                )
            )

            OutlinedTextField(
                value = tempHpText,
                onValueChange = { field ->
                    if (field.text.isBlank() || field.text.all(Char::isDigit)) {
                        tempHpText = field
                    }
                },
                label = { Text("HP temporal") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && tempHpText.text == character.tempHp.toString()) {
                            tempHpText = tempHpText.copy(
                                selection = TextRange(0, tempHpText.text.length)
                            )
                        }
                    },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        commitStats()
                        focusManager.clearFocus()
                    }
                )
            )
        }
        Text(
            text = "HP vacío se muestra como “?”",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        CollapsibleEditSection(
            title = "Foto de personaje",
            subtitle = if (character.imageUri == null) "Sin foto" else "Foto cargada",
            initiallyExpanded = false
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = launchCameraWithPermission) {
                    Text("📸 Tomar foto")
                }

                TextButton(
                    onClick = {
                        pickMedia.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text("🖼 Galería")
                }

                TextButton(onClick = onOpenLibrary) {
                    Text("🗂 Biblioteca")
                }

                if (character.imageUri != null) {
                    TextButton(
                        onClick = {
                            ImageStorage.deleteIfInternal(context, character.imageUri)
                            onUpdate(character.copy(imageUri = null))
                        }
                    ) {
                        Text("Quitar")
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tipo y combate",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CharacterTypeToggle(
                        value = character.type,
                        onChange = { onUpdate(character.copy(type = it)) }
                    )

                    Spacer(Modifier.weight(1f))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Activo")
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = character.isActive,
                            onCheckedChange = { onUpdate(character.copy(isActive = it)) }
                        )
                    }
                }
            }
        }
    }
}

@Suppress("SameParameterValue")
@Composable
private fun CollapsibleEditSection(
    title: String,
    subtitle: String? = null,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Ocultar sección" else "Mostrar sección"
                    )
                }
            }

            if (expanded) {
                content()
            }
        }
    }
}

@Composable
private fun CharacterStatusSection(
    statuses: List<Status>,
    isEditing: Boolean,
    onAddStatus: () -> Unit,
    onRemoveStatus: (Long) -> Unit
) {
    if (statuses.isEmpty() && !isEditing) return

    var expanded by rememberSaveable(isEditing) {
        mutableStateOf(!isEditing)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Estados (${statuses.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            if (isEditing) {
                AssistChip(
                    onClick = onAddStatus,
                    label = { Text("+Estado") }
                )

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Ocultar estados" else "Mostrar estados"
                    )
                }
            }
        }

        if (!isEditing || expanded) {
            when {
                statuses.isEmpty() && isEditing -> {
                    Text(
                        text = "Sin estados activos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                statuses.isNotEmpty() -> {
                    val scrollState = rememberScrollState()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = if (isEditing) 130.dp else 180.dp)
                            .verticalScroll(scrollState)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            statuses.forEach { status ->
                                RoundPrepStatusChip(
                                    status = status,
                                    isEditing = isEditing,
                                    onRemove = { onRemoveStatus(status.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundPrepStatusChip(
    status: Status,
    isEditing: Boolean,
    onRemove: () -> Unit
) {
    val container = statusContainerColor(status.type)
    val content = statusContentColor(status.type)

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = formatRoundPrepStatus(status),
                style = MaterialTheme.typography.labelLarge,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isEditing) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Quitar estado",
                        tint = content,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterTypeToggle(
    value: CharacterType,
    onChange: (CharacterType) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = value == CharacterType.PLAYER,
            onClick = { onChange(CharacterType.PLAYER) },
            label = { Text("PLAYER") }
        )
        FilterChip(
            selected = value == CharacterType.NPC,
            onClick = { onChange(CharacterType.NPC) },
            label = { Text("NPC") }
        )
    }
}

@Composable
private fun RoundPrepBottomBar(
    state: RoundPrepUiState,
    onEnterEdit: () -> Unit,
    onPlay: () -> Unit,
    onCancelEdit: () -> Unit,
    onSave: () -> Unit
) {
    BottomAppBar {
        if (!state.isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onEnterEdit,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isSaving
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Editar", maxLines = 1)
                }

                Button(
                    onClick = { if (state.canPlay) onPlay() },
                    modifier = Modifier.weight(1f),
                    enabled = state.canPlay && !state.isSaving
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Iniciar", maxLines = 1)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancelEdit,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Cancelar", maxLines = 1)
                }

                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun InfoBadge(
    label: String,
    highlighted: Boolean = false,
    danger: Boolean = false
) {
    val background = when {
        danger -> MaterialTheme.colorScheme.errorContainer
        highlighted -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val content = when {
        danger -> MaterialTheme.colorScheme.onErrorContainer
        highlighted -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = background
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
private fun LoadingCentered() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Suppress("SameParameterValue")
@Composable
private fun EmptyCentered(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatRoundPrepStatus(status: Status): String {
    val prefix = when (status.type) {
        StatusType.POSITIVE -> "+"
        StatusType.NEGATIVE -> "-"
        StatusType.NEUTRAL -> ""
    }
    val base = if (prefix.isBlank()) {
        "${status.name} (${status.durationRounds})"
    } else {
        "$prefix ${status.name} (${status.durationRounds})"
    }

    val origin = status.originLabel
        ?.takeIf { it.isNotBlank() && !isConcentrationStatus(status) }
        ?.let { " por $it" }
        ?: ""

    return base + origin
}

@Composable
private fun AddPreCombatStatusDialog(
    availableConcentrations: List<ConcentrationOption>,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        type: StatusType,
        duration: Int,
        tickTiming: StatusTickTiming,
        isConcentration: Boolean,
        linkedConcentration: ConcentrationOption?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("1") }
    var selectedType by remember { mutableStateOf(StatusType.NEUTRAL) }
    var isConcentration by rememberSaveable { mutableStateOf(false) }
    var selectedConcentrationGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTickTiming by rememberSaveable {
        mutableStateOf(StatusTickTiming.TURN_END)
    }

    val selectedConcentration = availableConcentrations
        .firstOrNull { it.groupId == selectedConcentrationGroupId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar estado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Nombre") }
                )

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Duración en rondas") }
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == StatusType.POSITIVE,
                        enabled = !isConcentration,
                        onClick = { selectedType = StatusType.POSITIVE },
                        label = { Text("Buff") }
                    )
                    FilterChip(
                        selected = selectedType == StatusType.NEGATIVE,
                        enabled = !isConcentration,
                        onClick = { selectedType = StatusType.NEGATIVE },
                        label = { Text("Debuff") }
                    )
                    FilterChip(
                        selected = selectedType == StatusType.NEUTRAL || isConcentration,
                        enabled = !isConcentration,
                        onClick = { selectedType = StatusType.NEUTRAL },
                        label = { Text("Neutral") }
                    )
                }

                FilterChip(
                    selected = isConcentration,
                    onClick = {
                        isConcentration = !isConcentration
                        if (isConcentration) {
                            selectedType = StatusType.NEUTRAL
                            selectedConcentrationGroupId = null
                            selectedTickTiming = StatusTickTiming.TURN_START
                        }
                    },
                    label = { Text("Es concentración") }
                )

                Text(
                    text = "Expira",
                    style = MaterialTheme.typography.titleSmall
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTickTiming == StatusTickTiming.TURN_START,
                        enabled = !isConcentration,
                        onClick = { selectedTickTiming = StatusTickTiming.TURN_START },
                        label = { Text("Inicio del turno") }
                    )

                    FilterChip(
                        selected = selectedTickTiming == StatusTickTiming.TURN_END,
                        enabled = !isConcentration,
                        onClick = { selectedTickTiming = StatusTickTiming.TURN_END },
                        label = { Text("Final del turno") }
                    )
                }

                if (!isConcentration && availableConcentrations.isNotEmpty()) {
                    Text(
                        text = "Vincular a concentración",
                        style = MaterialTheme.typography.titleSmall
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableConcentrations.forEach { option ->
                            FilterChip(
                                selected = selectedConcentrationGroupId == option.groupId,
                                onClick = {
                                    selectedConcentrationGroupId = option.groupId
                                    name = option.spellName
                                    durationText = option.durationRounds.toString()
                                    selectedTickTiming = option.tickTiming
                                },
                                label = {
                                    Text("${option.spellName} · ${option.originLabel}")
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val safeDuration = durationText.trim().toIntOrNull() ?: 1
                    if (name.trim().isNotBlank() && safeDuration > 0) {
                        onConfirm(
                            name.trim(),
                            selectedType,
                            safeDuration,
                            selectedTickTiming,
                            isConcentration,
                            selectedConcentration
                        )
                    }
                }
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun safeItrFileName(name: String): String {
    val cleaned = name
        .ifBlank { "ronda" }
        .trim()
        .replace(Regex("[^a-zA-Z0-9_-]+"), "_")
        .trim('_')

    return cleaned.ifBlank { "ronda" }
}

@Composable
private fun SavingOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text("Guardando…")
            }
        }
    }
}

private fun formatInitiative(value: Double): String {
    val text = value.toString()
    return if (text.endsWith(".0")) {
        text.removeSuffix(".0")
    } else {
        text.trimEnd('0').trimEnd('.')
    }
}

private fun isValidDecimalInput(value: String): Boolean {
    if (value.isBlank()) return true

    val dotCount = value.count { it == '.' }

    return dotCount <= 1 && value.all { it.isDigit() || it == '.' }
}

private fun suggestedLibraryImageName(character: Character): String {
    return character.characterName.trim()
        .ifBlank { character.playerName.trim() }
        .ifBlank { "Imagen" }
}

private data class ConcentrationOption(
    val groupId: String,
    val spellName: String,
    val durationRounds: Int,
    val tickTiming: StatusTickTiming,
    val originCharacterId: Long,
    val originLabel: String
)

private fun isConcentrationStatus(status: Status): Boolean {
    return status.concentrationGroupId != null &&
            status.name.startsWith("Concentrando:")
}

private fun concentrationSpellName(status: Status): String {
    return status.name.removePrefix("Concentrando:").trim()
}

private fun characterDisplayName(character: Character): String {
    return character.characterName.trim()
        .ifBlank { character.playerName.trim() }
        .ifBlank { "Sin nombre" }
}

private fun buildConcentrationOptions(
    statuses: List<Status>,
    characters: List<Character>
): List<ConcentrationOption> {
    return statuses
        .filter { isConcentrationStatus(it) }
        .mapNotNull { status ->
            val groupId = status.concentrationGroupId ?: return@mapNotNull null
            val origin = characters.firstOrNull { it.id == status.characterId }

            ConcentrationOption(
                groupId = groupId,
                spellName = concentrationSpellName(status).ifBlank { status.name },
                durationRounds = status.durationRounds,
                tickTiming = status.tickTiming,
                originCharacterId = status.characterId,
                originLabel = origin?.let { characterDisplayName(it) } ?: "Desconocido"
            )
        }
        .distinctBy { it.groupId }
}