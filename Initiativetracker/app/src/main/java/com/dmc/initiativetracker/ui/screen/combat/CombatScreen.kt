package com.dmc.initiativetracker.ui.screen.combat

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.CharacterType
import com.dmc.initiativetracker.domain.model.Status
import com.dmc.initiativetracker.domain.model.StatusType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombatScreen(
    vm: CombatViewModel,
    onExit: () -> Unit
) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    var showEditHpDialog by rememberSaveable { mutableStateOf(false) }
    var showEditTempHpDialog by rememberSaveable { mutableStateOf(false) }
    var showApplyDamageDialog by rememberSaveable { mutableStateOf(false) }
    var showAddStatusDialog by rememberSaveable { mutableStateOf(false) }

    var selectedSheetCharacterId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showSheetActionsDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.toast) {
        val msg = state.toast ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        vm.consumeToast()
    }

    val current = state.current

    val currentStatuses = current
        ?.let { currentCharacter -> state.statuses.filter { it.characterId == currentCharacter.id } }
        .orEmpty()

    val selectedSheetCharacter = state.ordered.firstOrNull { it.id == selectedSheetCharacterId }

    val selectedSheetStatuses = selectedSheetCharacter
        ?.let { selected -> state.statuses.filter { it.characterId == selected.id } }
        .orEmpty()

    val targetCharacter = selectedSheetCharacter ?: current

    if (state.isBottomSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = {
                vm.closeSheet()
                selectedSheetCharacterId = null
                showSheetActionsDialog = false
            }
        ) {
            SheetContent(
                ordered = state.ordered,
                statuses = state.statuses,
                onToggleActive = { vm.toggleActive(it) },
                onPreview = { uri -> vm.openPreview(uri) },
                onOpenActions = { character ->
                    selectedSheetCharacterId = character.id
                    showSheetActionsDialog = true
                },
                onClose = {
                    vm.closeSheet()
                    selectedSheetCharacterId = null
                    showSheetActionsDialog = false
                }
            )
        }
    }

    if (state.previewImageUri != null) {
        Dialog(onDismissRequest = { vm.closePreview() }) {
            PreviewOverlay(
                uri = state.previewImageUri,
                onClose = { vm.closePreview() }
            )
        }
    }

    if (showSheetActionsDialog && selectedSheetCharacter != null) {
        SheetCharacterActionsDialog(
            character = selectedSheetCharacter,
            statuses = selectedSheetStatuses,
            onDismiss = {
                showSheetActionsDialog = false
                selectedSheetCharacterId = null
            },
            onEditHp = {
                showSheetActionsDialog = false
                showEditHpDialog = true
            },
            onEditTempHp = {
                showSheetActionsDialog = false
                showEditTempHpDialog = true
            },
            onApplyDamage = {
                showSheetActionsDialog = false
                showApplyDamageDialog = true
            },
            onAddStatus = {
                showSheetActionsDialog = false
                showAddStatusDialog = true
            },
            onRemoveStatus = { statusId ->
                vm.removeStatus(statusId)
            }
        )
    }

    if (showEditHpDialog && targetCharacter != null) {
        EditHpDialog(
            currentHp = targetCharacter.currentHp,
            onDismiss = {
                showEditHpDialog = false
                selectedSheetCharacterId = null
            },
            onConfirm = { newHp ->
                vm.setHp(targetCharacter, newHp)
                showEditHpDialog = false
                selectedSheetCharacterId = null
            }
        )
    }

    if (showEditTempHpDialog && targetCharacter != null) {
        EditTempHpDialog(
            currentTempHp = targetCharacter.tempHp,
            onDismiss = {
                showEditTempHpDialog = false
                selectedSheetCharacterId = null
            },
            onConfirm = { newTempHp ->
                vm.setTempHp(targetCharacter, newTempHp)
                showEditTempHpDialog = false
                selectedSheetCharacterId = null
            }
        )
    }

    if (showApplyDamageDialog && targetCharacter != null) {
        ApplyDamageDialog(
            onDismiss = {
                showApplyDamageDialog = false
                selectedSheetCharacterId = null
            },
            onConfirm = { damage ->
                vm.applyDamage(targetCharacter, damage)
                showApplyDamageDialog = false
                selectedSheetCharacterId = null
            }
        )
    }

    if (showAddStatusDialog && targetCharacter != null) {
        AddStatusDialog(
            onDismiss = {
                showAddStatusDialog = false
                selectedSheetCharacterId = null
            },
            onConfirm = { name, type, duration ->
                vm.addStatus(
                    characterId = targetCharacter.id,
                    name = name,
                    type = type,
                    durationRounds = duration
                )
                showAddStatusDialog = false
                selectedSheetCharacterId = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ronda ${state.roundCounter}") },
                actions = {
                    IconButton(
                        onClick = {
                            vm.endCombat()
                            onExit()
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Terminar combate")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                TextButton(onClick = { vm.prev() }) { Text("◀") }
                TextButton(onClick = { vm.openSheet() }) { Text("📋") }
                Spacer(Modifier.weight(1f))
                Button(onClick = { vm.next() }) { Text("▶") }
            }
        }
    ) { padding ->
        CombatBody(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            current = current,
            currentStatuses = currentStatuses,
            onOpenPreview = { vm.openPreview(current?.imageUri) },
            onEditHp = {
                selectedSheetCharacterId = null
                showEditHpDialog = true
            },
            onEditTempHp = {
                selectedSheetCharacterId = null
                showEditTempHpDialog = true
            },
            onApplyDamage = {
                selectedSheetCharacterId = null
                showApplyDamageDialog = true
            },
            onAddStatus = {
                selectedSheetCharacterId = null
                showAddStatusDialog = true
            },
            onRemoveStatus = { vm.removeStatus(it) },
            onAddDeathSuccess = {
                current?.let { vm.addDeathSuccess(it) }
            },
            onAddDeathFailure = {
                current?.let { vm.addDeathFailure(it) }
            }
        )
    }
}

@Composable
private fun CombatBody(
    modifier: Modifier = Modifier,
    current: Character?,
    currentStatuses: List<Status>,
    onOpenPreview: () -> Unit,
    onEditHp: () -> Unit,
    onEditTempHp: () -> Unit,
    onApplyDamage: () -> Unit,
    onAddStatus: () -> Unit,
    onRemoveStatus: (Long) -> Unit,
    onAddDeathSuccess: () -> Unit,
    onAddDeathFailure: () -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val context = LocalContext.current
        val fallbackPainter = rememberVectorPainter(Icons.Default.Person)

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clickable { onOpenPreview() },
            shape = RoundedCornerShape(18.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(current?.imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = fallbackPainter,
                error = fallbackPainter,
                fallback = fallbackPainter
            )
        }

        if (current == null) {
            Text("No hay personaje actual", style = MaterialTheme.typography.titleMedium)
            Text("Activá al menos uno en la lista 📋", style = MaterialTheme.typography.bodyMedium)
            return
        }

        Text(
            text = "${current.playerName} • ${current.characterName}",
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = buildHpLine(current),
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            AssistChip(
                onClick = onEditHp,
                label = { Text("HP") }
            )
            AssistChip(
                onClick = onEditTempHp,
                label = { Text("Temp") }
            )
            AssistChip(
                onClick = onApplyDamage,
                label = { Text("Daño") }
            )
            AssistChip(
                onClick = onAddStatus,
                label = { Text("+Estado") }
            )
        }

        if (current.type == CharacterType.PLAYER && current.currentHp == 0) {
            DeathSavesCard(
                successes = current.deathSuccesses,
                failures = current.deathFailures,
                onAddSuccess = onAddDeathSuccess,
                onAddFailure = onAddDeathFailure
            )
        }

        if (currentStatuses.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Estados",
                    style = MaterialTheme.typography.titleMedium
                )

                currentStatuses.forEach { status ->
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
                                text = formatStatus(status),
                                style = MaterialTheme.typography.bodyLarge,
                                color = statusColor(status),
                                modifier = Modifier.weight(1f)
                            )

                            TextButton(onClick = { onRemoveStatus(status.id) }) {
                                Text("✕")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetContent(
    ordered: List<Character>,
    statuses: List<Status>,
    onToggleActive: (Character) -> Unit,
    onPreview: (String?) -> Unit,
    onOpenActions: (Character) -> Unit,
    onClose: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Lista", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Cerrar") }
        }

        Spacer(Modifier.height(8.dp))

        if (ordered.isEmpty()) {
            Text("No hay personajes.")
            Spacer(Modifier.height(24.dp))
            return
        }

        ordered.forEach { c ->
            val characterStatuses = statuses.filter { it.characterId == c.id }

            SheetRow(
                character = c,
                statuses = characterStatuses,
                onToggleActive = { onToggleActive(c) },
                onPreview = { onPreview(c.imageUri) },
                onOpenActions = { onOpenActions(c) }
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SheetRow(
    character: Character,
    statuses: List<Status>,
    onToggleActive: () -> Unit,
    onPreview: () -> Unit,
    onOpenActions: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onPreview() },
                    onLongClick = { onOpenActions() }
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            val fallbackPainter = rememberVectorPainter(Icons.Default.Person)

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(character.imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Crop,
                placeholder = fallbackPainter,
                error = fallbackPainter,
                fallback = fallbackPainter
            )

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = "${character.playerName} • ${character.characterName}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = buildHpLine(character),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (statuses.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        statuses.take(3).forEach { status ->
                            Text(
                                text = formatStatusCompact(status),
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor(status),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (statuses.size > 3) {
                            Text(
                                text = "+${statuses.size - 3} más",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            val label = if (character.isActive && !character.isDead) "Activo" else "Inactivo"
            AssistChip(
                onClick = onToggleActive,
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun PreviewOverlay(
    uri: String?,
    onClose: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            val context = LocalContext.current
            val fallbackPainter = rememberVectorPainter(Icons.Default.Person)

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    placeholder = fallbackPainter,
                    error = fallbackPainter,
                    fallback = fallbackPainter
                )

                if (uri == null) {
                    Text(
                        text = "Sin imagen",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
        }
    }
}

@Composable
private fun DeathSavesCard(
    successes: Int,
    failures: Int,
    onAddSuccess: () -> Unit,
    onAddFailure: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Tiradas de salvación contra muerte",
                style = MaterialTheme.typography.titleMedium
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Éxitos", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        DeathSaveDot(filled = index < successes)
                    }
                }

                Text("Fallos", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        DeathSaveDot(filled = index < failures, isFailure = true)
                    }
                }
            }

            val locked = successes >= 3 || failures >= 3

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = onAddSuccess,
                    enabled = !locked,
                    label = { Text("+ Éxito") }
                )
                AssistChip(
                    onClick = onAddFailure,
                    enabled = !locked,
                    label = { Text("+ Fallo") }
                )
            }

            if (successes >= 3) {
                Text(
                    text = "Estable",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (failures >= 3) {
                Text(
                    text = "Muerto",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DeathSaveDot(
    filled: Boolean,
    isFailure: Boolean = false
) {
    val color = when {
        !filled -> MaterialTheme.colorScheme.surfaceVariant
        isFailure -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .size(18.dp)
            .background(
                color = color,
                shape = RoundedCornerShape(50)
            )
    )
}

@Composable
private fun EditHpDialog(
    currentHp: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    var text by remember(currentHp) { mutableStateOf(currentHp?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar HP actual") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("HP actual") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(text.trim().toIntOrNull())
                }
            ) {
                Text("Guardar")
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
private fun EditTempHpDialog(
    currentTempHp: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember(currentTempHp) { mutableStateOf(currentTempHp.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar HP temporal") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("HP temporal") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(text.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0)
                }
            ) {
                Text("Guardar")
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
private fun ApplyDamageDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aplicar daño") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("Daño") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = text.trim().toIntOrNull()
                    if (value != null && value > 0) {
                        onConfirm(value)
                    }
                }
            ) {
                Text("Aplicar")
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
private fun AddStatusDialog(
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
private fun SheetCharacterActionsDialog(
    character: Character,
    statuses: List<Status>,
    onDismiss: () -> Unit,
    onEditHp: () -> Unit,
    onEditTempHp: () -> Unit,
    onApplyDamage: () -> Unit,
    onAddStatus: () -> Unit,
    onRemoveStatus: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${character.playerName} • ${character.characterName}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(buildHpLine(character))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = onEditHp,
                        label = { Text("HP") }
                    )
                    AssistChip(
                        onClick = onEditTempHp,
                        label = { Text("Temp") }
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = onApplyDamage,
                        label = { Text("Daño") }
                    )
                    AssistChip(
                        onClick = onAddStatus,
                        label = { Text("+Estado") }
                    )
                }

                if (statuses.isNotEmpty()) {
                    Text(
                        text = "Estados",
                        style = MaterialTheme.typography.titleSmall
                    )

                    statuses.forEach { status ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatStatus(status),
                                color = statusColor(status),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { onRemoveStatus(status.id) }) {
                                Text("✕")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

private fun buildHpLine(character: Character): String {
    val current = character.currentHp?.toString() ?: "?"
    val max = character.maxHp?.toString() ?: "?"
    val tempSuffix = if (character.tempHp > 0) " • Temp: ${character.tempHp}" else ""
    return "HP: $current/$max$tempSuffix"
}

@Composable
private fun statusColor(status: Status) = when {
    status.concentrationGroupId != null -> MaterialTheme.colorScheme.onSurface
    status.type == StatusType.POSITIVE -> MaterialTheme.colorScheme.primary
    status.type == StatusType.NEGATIVE -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun formatStatus(status: Status): String {
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

private fun formatStatusCompact(status: Status): String {
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