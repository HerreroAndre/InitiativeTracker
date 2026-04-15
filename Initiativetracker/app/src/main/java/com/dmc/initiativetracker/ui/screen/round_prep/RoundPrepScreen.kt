package com.dmc.initiativetracker.ui.screen.round_prep

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.CharacterType
import com.dmc.initiativetracker.domain.model.Status
import com.dmc.initiativetracker.domain.model.StatusType
import com.dmc.initiativetracker.viewmodel.RoundPrepUiState
import kotlinx.coroutines.launch

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
                        onClick = { if (!state.isSaving) onBack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
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
                                                index = currentIndex.coerceAtMost(
                                                    (state.shownCharacters.size - 1).coerceAtLeast(0)
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
    listState: LazyListState,
    onUpdateDraft: (Character) -> Unit,
    onDeleteDraft: (Long) -> Unit,
    onAddStatus: (Long) -> Unit,
    onRemoveStatus: (Long) -> Unit
) {
    val list = state.shownCharacters
    val statusesByCharacter = remember(state.statuses) {
        state.statuses.groupBy { it.characterId }
    }

    Box(modifier) {
        when {
            state.roundName.isBlank() && state.characters.isEmpty() && !state.isEditing -> {
                LoadingCentered()
            }

            list.isEmpty() -> {
                EmptyCentered(
                    title = "No hay personajes",
                    subtitle = if (state.isEditing) {
                        "Agregá personajes con el botón de abajo"
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CharacterCardHeader(
                character = character,
                isEditing = isEditing,
                onUpdate = onUpdate,
                onDelete = onDelete
            )

            if (isEditing) {
                CharacterEditSection(
                    character = character,
                    onUpdate = onUpdate
                )
            } else {
                CharacterInfoSection(character = character)
            }

            HorizontalDivider()

            CharacterStatusSection(
                statuses = statuses,
                isEditing = isEditing,
                onAddStatus = onAddStatus,
                onRemoveStatus = onRemoveStatus
            )
        }
    }
}

@Composable
private fun CharacterCardHeader(
    character: Character,
    isEditing: Boolean,
    onUpdate: (Character) -> Unit,
    onDelete: () -> Unit
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
            if (!isEditing) {
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
                    InfoBadge(label = "Init ${character.initiative}", highlighted = true)

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
            } else {
                OutlinedTextField(
                    value = character.playerName,
                    onValueChange = { onUpdate(character.copy(playerName = it)) },
                    label = { Text("Jugador") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = character.characterName,
                    onValueChange = { onUpdate(character.copy(characterName = it)) },
                    label = { Text("Personaje") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                InfoBadge(label = "Temp ${character.tempHp}")
            }
        }
    }
}

@Composable
private fun CharacterEditSection(
    character: Character,
    onUpdate: (Character) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var initText by remember(character.id) { mutableStateOf(character.initiative.toString()) }
    var currentHpText by remember(character.id) { mutableStateOf(character.currentHp?.toString() ?: "") }
    var maxHpText by remember(character.id) { mutableStateOf(character.maxHp?.toString() ?: "") }
    var tempHpText by remember(character.id) { mutableStateOf(character.tempHp.toString()) }
    var pendingCameraUriString by rememberSaveable(character.id) { mutableStateOf<String?>(null) }

    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val pending = pendingCameraUriString

        if (success && pending != null) {
            com.dmc.initiativetracker.util.ImageStorage.deleteIfInternal(context, character.imageUri)
            onUpdate(character.copy(imageUri = pending))
        } else {
            com.dmc.initiativetracker.util.ImageStorage.deleteFileUri(pending)
        }

        pendingCameraUriString = null
    }

    val requestCameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val outUri = com.dmc.initiativetracker.util.ImageStorage.createCameraOutputUri(context)
            pendingCameraUriString = outUri.toString()
            takePicture.launch(outUri)
        }
    }

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val internalUri = com.dmc.initiativetracker.util.ImageStorage
                .copyToInternalStorage(context, uri)

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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = initText,
                onValueChange = { txt ->
                    if (txt.isBlank() || txt.all(Char::isDigit)) {
                        initText = txt
                        txt.toIntOrNull()?.let { onUpdate(character.copy(initiative = it)) }
                    }
                },
                label = { Text("Iniciativa") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = currentHpText,
                onValueChange = { txt ->
                    if (txt.isBlank() || txt.all(Char::isDigit)) {
                        currentHpText = txt
                        onUpdate(character.copy(currentHp = txt.toIntOrNull()))
                    }
                },
                label = { Text("HP actual") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
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
                        onUpdate(character.copy(maxHp = txt.toIntOrNull()))
                    }
                },
                label = { Text("HP máximo") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = tempHpText,
                onValueChange = { txt ->
                    if (txt.isBlank() || txt.all(Char::isDigit)) {
                        tempHpText = txt
                        onUpdate(character.copy(tempHp = txt.toIntOrNull() ?: 0))
                    }
                },
                label = { Text("Temp HP") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )
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
                    text = "Imagen y tipo",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        TextButton(
                            onClick = {
                                com.dmc.initiativetracker.util.ImageStorage
                                    .deleteIfInternal(context, character.imageUri)
                                onUpdate(character.copy(imageUri = null))
                            }
                        ) {
                            Text("Quitar")
                        }
                    }
                }

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

@Composable
private fun CharacterStatusSection(
    statuses: List<Status>,
    isEditing: Boolean,
    onAddStatus: () -> Unit,
    onRemoveStatus: (Long) -> Unit
) {
    if (statuses.isEmpty() && !isEditing) return

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
                text = "Estados",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            if (isEditing) {
                AssistChip(
                    onClick = onAddStatus,
                    label = { Text("+Estado") }
                )
            }
        }

        when {
            statuses.isEmpty() && isEditing -> {
                Text(
                    text = "Sin estados",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            statuses.isNotEmpty() -> {
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

@Composable
private fun RoundPrepStatusChip(
    status: Status,
    isEditing: Boolean,
    onRemove: () -> Unit
) {
    val container = roundPrepStatusContainerColor(status)
    val content = Color.White

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
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("✕", color = content)
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
    onAdd: () -> Unit,
    onSave: () -> Unit
) {
    BottomAppBar {
        Spacer(Modifier.weight(1f))

        if (!state.isEditing) {
            IconButton(onClick = onEnterEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }

            Spacer(Modifier.width(10.dp))

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

            Spacer(Modifier.width(10.dp))

            IconButton(onClick = onAdd, enabled = !state.isSaving) {
                Icon(Icons.Default.Add, contentDescription = "Agregar personaje")
            }

            Spacer(Modifier.width(10.dp))

            IconButton(onClick = onSave, enabled = !state.isSaving) {
                Icon(Icons.Default.Check, contentDescription = "Guardar cambios")
            }
        }

        Spacer(Modifier.weight(1f))
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

private fun roundPrepStatusContainerColor(status: Status): Color {
    return when {
        status.concentrationGroupId != null -> Color(0xFF54606E)
        status.type == StatusType.POSITIVE -> Color(0xFF2F6E4F)
        status.type == StatusType.NEGATIVE -> Color(0xFF7A3E3E)
        else -> Color(0xFF4E5D73)
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