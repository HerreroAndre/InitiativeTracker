package com.dmc.initiativetracker.ui.screen.round_prep

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.CharacterType
import com.dmc.initiativetracker.viewmodel.RoundPrepUiState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Sort
import android.widget.Toast
import com.dmc.initiativetracker.domain.model.Status
import com.dmc.initiativetracker.domain.model.StatusType
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundPrepScreen(
    vm: RoundPrepViewModel,
    onBack: () -> Unit,
    onStartCombat: () -> Unit,
) {
    val state by vm.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

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

    var renameOpen by remember { mutableStateOf(false) }
    var renameText by remember(state.roundName) { mutableStateOf(state.roundName) }
    val canRename = renameText.trim().isNotBlank() && !state.isSaving
    var selectedStatusCharacterId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showAddStatusDialog by rememberSaveable { mutableStateOf(false) }


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
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isSaving,
                    onClick = { renameOpen = false }
                ) { Text("Cancelar") }
            }
        )
    }

    if (showAddStatusDialog && selectedStatusCharacterId != null) {
        AddPreCombatStatusDialog(
            onDismiss = {
                showAddStatusDialog = false
                selectedStatusCharacterId = null
            },
            onConfirm = { name, type, duration ->
                vm.addPreCombatStatus(
                    characterId = selectedStatusCharacterId ?: return@AddPreCombatStatusDialog,
                    name = name,
                    type = type,
                    durationRounds = duration
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.roundName.ifBlank { "Ronda" }) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!state.isSaving) onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = vm::openSortMenu,
                            enabled = !state.isSaving
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Ordenar")
                        }

                        DropdownMenu(
                            expanded = state.isSortMenuOpen,
                            onDismissRequest = vm::closeSortMenu
                        ) {
                            RoundPrepSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        val currentIndex = listState.firstVisibleItemIndex
                                        val currentOffset = listState.firstVisibleItemScrollOffset

                                        vm.selectSort(option)

                                        scope.launch {
                                            listState.scrollToItem(
                                                index = currentIndex.coerceAtMost((state.shownCharacters.size - 1).coerceAtLeast(0)),
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

                    IconButton(
                        onClick = {
                            renameText = state.roundName
                            renameOpen = true
                        },
                        enabled = !state.isEditing && !state.isSaving
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Renombrar")
                    }
                }
            )

        },
        bottomBar = {
            RoundPrepBottomBar(
                state = state,
                onEnterEdit = vm::enterEditMode,
                onPlay = onStartCombat,
                onCancelEdit = vm::cancelEdit,
                onAdd = vm::addCharacterToDraft,
                onSave = vm::confirmEdit
            )
        }
    ) { padding ->
        RoundPrepContent(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            state = state,
            listState = listState,
            onUpdateDraft = vm::updateDraftCharacter,
            onDeleteDraft = vm::removeDraftCharacter,
            onAddStatus = { characterId ->
                selectedStatusCharacterId = characterId
                showAddStatusDialog = true
            },
            onRemoveStatus = vm::requestRemovePreCombatStatus
        )
    }
}

@Composable
private fun RoundPrepContent(
    modifier: Modifier = Modifier,
    state: RoundPrepUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onUpdateDraft: (Character) -> Unit,
    onDeleteDraft: (Long) -> Unit,
    onAddStatus: (Long) -> Unit,
    onRemoveStatus: (Long) -> Unit
) {
    val list: List<Character> = state.shownCharacters

    Box(modifier) {
        when {
            state.roundName.isBlank() && state.characters.isEmpty() && !state.isEditing -> {
                LoadingCentered()
            }

            list.isEmpty() -> {
                EmptyCentered(
                    title = "No hay personajes",
                    subtitle = if (state.isEditing) "Agregá personajes con ➕" else "Entrá a editar para agregarlos"
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = list, // ya viene ordenada en VM
                        key = { it.id }
                    ) { c ->
                        CharacterCard(
                            character = c,
                            statuses = state.statuses.filter { it.characterId == c.id },
                            isEditing = state.isEditing,
                            onUpdate = onUpdateDraft,
                            onDelete = { onDeleteDraft(c.id) },
                            onAddStatus = { onAddStatus(c.id) },
                            onRemoveStatus = onRemoveStatus
                        )
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
private fun CharacterCard(
    character: Character,
    statuses: List<Status>,
    isEditing: Boolean,
    onUpdate: (Character) -> Unit,
    onDelete: () -> Unit,
    onAddStatus: () -> Unit,
    onRemoveStatus: (Long) -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            val focusManager = LocalFocusManager.current
            val playerFR = remember { FocusRequester() }
            val nameFR = remember { FocusRequester() }
            val initFR = remember { FocusRequester() }
            val currentHpFR = remember { FocusRequester() }
            val maxHpFR = remember { FocusRequester() }
            val tempHpFR = remember { FocusRequester() }

            // Header: thumbnail placeholder + nombre (o campos)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val context = LocalContext.current
                val fallbackPainter = rememberVectorPainter(Icons.Default.Person)

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(character.imageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Crop,
                    placeholder = fallbackPainter,
                    error = fallbackPainter,
                    fallback = fallbackPainter
                )

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    if (!isEditing) {
                        Text(
                            text = "${character.playerName} • ${character.characterName}",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Init: ${character.initiative}")
                            Text(
                                "HP: ${(character.currentHp?.toString() ?: "?")}/${character.maxHp?.toString() ?: "?"}"
                            )
                            if (character.tempHp > 0) {
                                Text("Temp: ${character.tempHp}")
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = character.playerName,
                            onValueChange = { onUpdate(character.copy(playerName = it)) },
                            label = { Text("Jugador") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().focusRequester(playerFR),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { nameFR.requestFocus() })
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = character.characterName,
                            onValueChange = { onUpdate(character.copy(characterName = it)) },
                            label = { Text("Personaje") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().focusRequester(nameFR),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { initFR.requestFocus() })
                        )
                    }
                }

                if (isEditing) {
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            }

            if (isEditing) {
                Spacer(Modifier.height(12.dp))

                // Initiative + HP (numéricos)
                var initText by remember(character.id) { mutableStateOf(character.initiative.toString()) }
                var currentHpText by remember(character.id) { mutableStateOf(character.currentHp?.toString() ?: "") }
                var maxHpText by remember(character.id) { mutableStateOf(character.maxHp?.toString() ?: "") }
                var tempHpText by remember(character.id) { mutableStateOf(character.tempHp.toString()) }

                // Iniciativa + HP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = initText,
                        onValueChange = { txt: String ->
                            if (txt.isBlank() || txt.all { ch: Char -> ch.isDigit() }) {
                                initText = txt
                                txt.toIntOrNull()?.let { onUpdate(character.copy(initiative = it)) }
                            }
                        },
                        label = { Text("Iniciativa") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).focusRequester(initFR),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                initText.toIntOrNull()?.let { onUpdate(character.copy(initiative = it)) }
                                currentHpFR.requestFocus()
                            }
                        )
                    )

                    OutlinedTextField(
                        value = currentHpText,
                        onValueChange = { txt ->
                            if (txt.isBlank() || txt.all { it.isDigit() }) {
                                currentHpText = txt
                                val currentHp = txt.toIntOrNull()
                                onUpdate(character.copy(currentHp = currentHp))
                            }
                        },
                        label = { Text("HP actual") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).focusRequester(currentHpFR),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                maxHpFR.requestFocus()
                            }
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = maxHpText,
                        onValueChange = { txt ->
                            if (txt.isBlank() || txt.all { it.isDigit() }) {
                                maxHpText = txt
                                val maxHp = txt.toIntOrNull()
                                onUpdate(character.copy(maxHp = maxHp))
                            }
                        },
                        label = { Text("HP máximo") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).focusRequester(maxHpFR),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                tempHpFR.requestFocus()
                            }
                        )
                    )

                    OutlinedTextField(
                        value = tempHpText,
                        onValueChange = { txt ->
                            if (txt.isBlank() || txt.all { it.isDigit() }) {
                                tempHpText = txt
                                val tempHp = txt.toIntOrNull() ?: 0
                                onUpdate(character.copy(tempHp = tempHp))
                            }
                        },
                        label = { Text("Temp HP") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).focusRequester(tempHpFR),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                            }
                        )
                    )
                }

                Spacer(Modifier.height(12.dp))

                val context = LocalContext.current


                // Guardamos el uri pendiente entre recomposiciones (por si cambia estado)
                var pendingCameraUriString by rememberSaveable(character.id) { mutableStateOf<String?>(null) }

                val takePicture = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { success ->
                    val pending = pendingCameraUriString

                    if (success && pending != null) {
                        // Si sacó foto: borramos anterior interna y guardamos la nueva
                        com.dmc.initiativetracker.util.ImageStorage
                            .deleteIfInternal(context, character.imageUri)

                        onUpdate(character.copy(imageUri = pending))
                    } else {
                        // Si canceló: borramos el archivo que habíamos creado (por si quedó basura)
                        com.dmc.initiativetracker.util.ImageStorage.deleteFileUri(pending)
                    }

                    pendingCameraUriString = null
                }

                val requestCameraPermission = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        // lanzar cámara (mismo flujo que ya tenés)
                        val outUri = com.dmc.initiativetracker.util.ImageStorage.createCameraOutputUri(context)
                        pendingCameraUriString = outUri.toString()
                        takePicture.launch(outUri)
                    } else {
                        // opcional: mostrar mensaje
                        // Podés dejarlo vacío o usar Toast si querés.
                    }
                }

                val pickMedia = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        // Copiar a storage interno y guardar esa ruta estable
                        val internalUri = com.dmc.initiativetracker.util.ImageStorage
                            .copyToInternalStorage(context, uri)

                        // borrar la anterior si era interna
                        com.dmc.initiativetracker.util.ImageStorage
                            .deleteIfInternal(context, character.imageUri)

                        onUpdate(character.copy(imageUri = internalUri))
                    }
                }

                val launchCameraWithPermission = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        val outUri = com.dmc.initiativetracker.util.ImageStorage.createCameraOutputUri(context)
                        pendingCameraUriString = outUri.toString()
                        takePicture.launch(outUri)
                    } else {
                        requestCameraPermission.launch(Manifest.permission.CAMERA)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                    if (character.imageUri != null) {
                        TextButton(onClick = {
                            com.dmc.initiativetracker.util.ImageStorage
                                .deleteIfInternal(context, character.imageUri)
                            onUpdate(character.copy(imageUri = null))
                        }) {
                            Text("Quitar")
                        }
                    }
                }

                // Type + Active
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
            Spacer(Modifier.height(12.dp))

            if (statuses.isNotEmpty() || isEditing) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estados",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )

                        if (isEditing) {
                            AssistChip(
                                onClick = onAddStatus,
                                label = { Text("+Estado") }
                            )
                        }
                    }

                    if (statuses.isNotEmpty()) {
                        statuses.forEach { status ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatRoundPrepStatus(status),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = roundPrepStatusColor(status),
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isEditing) {
                                        TextButton(onClick = { onRemoveStatus(status.id) }) {
                                            Text("✕")
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isEditing) {
                        Text(
                            text = "Sin estados",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
    // Simple y robusto para MVP
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
    onAdd: () -> Unit,
    onSave: () -> Unit
) {
    BottomAppBar {
        Spacer(Modifier.weight(1f))

        if (!state.isEditing) {
            IconButton(onClick = onEnterEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }
            Spacer(Modifier.width(8.dp))
            FloatingActionButton(
                onClick = { if (state.canPlay) onPlay() },
                containerColor = if (state.canPlay) {
                    FloatingActionButtonDefaults.containerColor
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar combate")
            }
        } else {
            IconButton(onClick = onCancelEdit, enabled = !state.isSaving) {
                Icon(Icons.Default.Close, contentDescription = "Cancelar cambios")
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onAdd, enabled = !state.isSaving) {
                Icon(Icons.Default.Add, contentDescription = "Agregar personaje")
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onSave, enabled = !state.isSaving) {
                Icon(Icons.Default.Check, contentDescription = "Guardar cambios")
            }
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun LoadingCentered() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyCentered(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun roundPrepStatusColor(status: Status) = when {
    status.concentrationGroupId != null -> MaterialTheme.colorScheme.onSurface
    status.type == StatusType.POSITIVE -> MaterialTheme.colorScheme.primary
    status.type == StatusType.NEGATIVE -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
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

    return if (status.concentrationGroupId != null) {
        "$base • Conc."
    } else {
        base
    }
}

@Composable
private fun AddPreCombatStatusDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: StatusType, duration: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("1") }
    var selectedType by remember { mutableStateOf(StatusType.NEUTRAL) }

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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == StatusType.POSITIVE,
                        onClick = { selectedType = StatusType.POSITIVE },
                        label = { Text("Buff") }
                    )
                    FilterChip(
                        selected = selectedType == StatusType.NEGATIVE,
                        onClick = { selectedType = StatusType.NEGATIVE },
                        label = { Text("Debuff") }
                    )
                    FilterChip(
                        selected = selectedType == StatusType.NEUTRAL,
                        onClick = { selectedType = StatusType.NEUTRAL },
                        label = { Text("Neutral") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val safeDuration = durationText.trim().toIntOrNull() ?: 1
                    if (name.trim().isNotBlank() && safeDuration > 0) {
                        onConfirm(name.trim(), selectedType, safeDuration)
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