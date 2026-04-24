package com.dmc.initiativetracker.ui.screen.combat

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.CharacterType
import com.dmc.initiativetracker.domain.model.Status
import com.dmc.initiativetracker.domain.model.StatusType
import com.dmc.initiativetracker.ui.theme.statusContainerColor
import com.dmc.initiativetracker.ui.theme.statusContentColor
import com.dmc.initiativetracker.ui.theme.statusInlineTextColor
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.IconButton

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
    var showHealDialog by rememberSaveable { mutableStateOf(false) }
    var showEndCombatDialog by rememberSaveable { mutableStateOf(false) }
    var quickActionsExpanded by rememberSaveable { mutableStateOf(true) }
    var statusSectionExpanded by rememberSaveable { mutableStateOf(true) }

    var selectedSheetCharacterId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showSheetActionsDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.toast) {
        val msg = state.toast ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        vm.consumeToast()
    }

    BackHandler {
        when {
            state.previewImageUri != null -> vm.closePreview()
            state.isBottomSheetOpen -> vm.closeSheet()
            else -> showEndCombatDialog = true
        }
    }

    val current = state.current

    val statusesByCharacter = remember(state.statuses) {
        state.statuses.groupBy { it.characterId }
    }
    val currentStatuses = current?.let { statusesByCharacter[it.id].orEmpty() }.orEmpty()

    val selectedSheetCharacter = state.ordered.firstOrNull { it.id == selectedSheetCharacterId }


    val selectedSheetStatuses = selectedSheetCharacter
        ?.let { selected -> statusesByCharacter[selected.id].orEmpty() }
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
                currentCharacterId = state.current?.id,
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
            onHeal = {
                showSheetActionsDialog = false
                showHealDialog = true
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

    if (showHealDialog && targetCharacter != null) {
        HealDialog(
            onDismiss = {
                showHealDialog = false
                selectedSheetCharacterId = null
            },
            onConfirm = { amount ->
                vm.heal(targetCharacter, amount)
                showHealDialog = false
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

    if (showEndCombatDialog) {
        AlertDialog(
            onDismissRequest = { showEndCombatDialog = false },
            title = { Text("Terminar combate") },
            text = {
                Text("¿Seguro que querés terminar el combate? Se reiniciará el estado actual.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndCombatDialog = false
                        vm.endCombat()
                        onExit()
                    }
                ) {
                    Text("Terminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEndCombatDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CombatTopBar(
                roundCounter = state.roundCounter,
                onEndCombat = {
                    showEndCombatDialog = true
                }
            )
        },
        bottomBar = {
            CombatBottomBar(
                onPrev = { vm.prev() },
                onOpenSheet = { vm.openSheet() },
                onNext = { vm.next() }
            )
        }
    ) { padding ->
        CombatBody(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            current = current,
            currentStatuses = currentStatuses,
            onOpenPreview = { vm.openPreview(current?.imageUri) },
            quickActionsExpanded = quickActionsExpanded,
            onToggleQuickActions = { quickActionsExpanded = !quickActionsExpanded },
            statusSectionExpanded = statusSectionExpanded,
            onToggleStatusSection = { statusSectionExpanded = !statusSectionExpanded },
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
            onHeal = {
                selectedSheetCharacterId = null
                showHealDialog = true
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CombatTopBar(
    roundCounter: Int,
    onEndCombat: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Combate",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Ronda $roundCounter",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onEndCombat) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Terminar combate"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
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
    onHeal: () -> Unit,
    onAddStatus: () -> Unit,
    onRemoveStatus: (Long) -> Unit,
    onAddDeathSuccess: () -> Unit,
    onAddDeathFailure: () -> Unit,
    quickActionsExpanded: Boolean,
    onToggleQuickActions: () -> Unit,
    statusSectionExpanded: Boolean,
    onToggleStatusSection: () -> Unit
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (current == null) {
            EmptyCombatState()
            return
        }

        CharacterHeroCard(
            character = current,
            onOpenPreview = onOpenPreview
        )

        CharacterSummaryCard(
            character = current
        )

        QuickActionsCard(
            expanded = quickActionsExpanded,
            onToggleExpanded = onToggleQuickActions,
            onEditHp = onEditHp,
            onEditTempHp = onEditTempHp,
            onApplyDamage = onApplyDamage,
            onHeal = onHeal
        )

        if (current.type == CharacterType.PLAYER && current.currentHp == 0) {
            DeathSavesCard(
                successes = current.deathSuccesses,
                failures = current.deathFailures,
                onAddSuccess = onAddDeathSuccess,
                onAddFailure = onAddDeathFailure
            )
        }

        StatusSection(
            statuses = currentStatuses,
            expanded = statusSectionExpanded,
            onToggleExpanded = onToggleStatusSection,
            onAddStatus = onAddStatus,
            onRemoveStatus = onRemoveStatus
        )

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun EmptyCombatState() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Text(
                text = "No hay personaje actual",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Activá al menos un personaje desde la lista del combate.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Usá el botón Lista para revisar quiénes están activos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CharacterHeroCard(
    character: Character,
    onOpenPreview: () -> Unit
) {
    val context = LocalContext.current
    val fallbackPainter = rememberVectorPainter(Icons.Default.Person)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clickable { onOpenPreview() },
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(character.imageUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = fallbackPainter,
                error = fallbackPainter,
                fallback = fallbackPainter
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.30f))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                    ) {
                        Text(
                            text = currentRoleLabel(character),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = character.characterName.ifBlank { "Sin nombre" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = character.playerName.ifBlank { "Sin jugador" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterSummaryCard(
    character: Character
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Resumen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoPill(
                    label = buildHpChipLabel(character),
                    highlighted = true
                )

                if (character.tempHp > 0) {
                    InfoPill(
                        label = "Temp: ${character.tempHp}"
                    )
                }

                if (!character.isActive || character.isDead) {
                    InfoPill(
                        label = if (character.isDead) "Muerto" else "Inactivo",
                        danger = character.isDead
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoPill(
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = content
        )
    }
}

@Composable
private fun QuickActionsCard(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onEditHp: () -> Unit,
    onEditTempHp: () -> Unit,
    onApplyDamage: () -> Unit,
    onHeal: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Acciones rápidas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Ocultar acciones" else "Mostrar acciones"
                    )
                }
            }

            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onEditHp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("HP")
                    }

                    FilledTonalButton(
                        onClick = onEditTempHp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Temp")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onApplyDamage,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Daño")
                    }

                    OutlinedButton(
                        onClick = onHeal,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Curar")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusSection(
    statuses: List<Status>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAddStatus: () -> Unit,
    onRemoveStatus: (Long) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estados",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                AssistChip(
                    onClick = onAddStatus,
                    label = { Text("+ Estado") }
                )

                Spacer(Modifier.width(6.dp))

                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Ocultar estados" else "Mostrar estados"
                    )
                }
            }

            if (expanded) {
                if (statuses.isEmpty()) {
                    Text(
                        text = "Sin estados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        statuses.forEach { status ->
                            StatusChip(
                                status = status,
                                onRemove = { onRemoveStatus(status.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    status: Status,
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
                text = formatStatus(status),
                style = MaterialTheme.typography.labelLarge,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CombatBottomBar(
    onPrev: () -> Unit,
    onOpenSheet: () -> Unit,
    onNext: () -> Unit
) {
    BottomAppBar(
        windowInsets = WindowInsets.navigationBars
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPrev,
                modifier = Modifier.weight(1f)
            ) {
                Text("Anterior")
            }

            FilledTonalButton(
                onClick = onOpenSheet,
                modifier = Modifier.weight(1f)
            ) {
                Text("Lista")
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(1.2f)
            ) {
                Text("Siguiente")
            }
        }
    }
}

@Composable
private fun SheetContent(
    ordered: List<Character>,
    statuses: List<Status>,
    currentCharacterId: Long?,
    onToggleActive: (Character) -> Unit,
    onPreview: (String?) -> Unit,
    onOpenActions: (Character) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lista de combate",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) {
                Text("Cerrar")
            }
        }

        Spacer(Modifier.height(88.dp))

        if (ordered.isEmpty()) {
            Text(
                text = "No hay personajes.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            return
        }

        val statusesByCharacter = statuses.groupBy { it.characterId }
        ordered.forEachIndexed { index, character ->
            val characterStatuses = statusesByCharacter[character.id].orEmpty()

            SheetRow(
                character = character,
                statuses = characterStatuses,
                isCurrent = character.id == currentCharacterId,
                onToggleActive = { onToggleActive(character) },
                onPreview = { onPreview(character.imageUri) },
                onOpenActions = { onOpenActions(character) }
            )

            if (index != ordered.lastIndex) {
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SheetRow(
    character: Character,
    statuses: List<Status>,
    isCurrent: Boolean,
    onToggleActive: () -> Unit,
    onPreview: () -> Unit,
    onOpenActions: () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)
    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.50f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        color = containerColor,
        tonalElevation = if (isCurrent) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onPreview,
                    onLongClick = onOpenActions
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
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp)),
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = character.characterName.ifBlank { "Sin nombre" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (isCurrent) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "Turno actual",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Text(
                    text = character.playerName.ifBlank { "Sin jugador" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = buildHpLine(character),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (statuses.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        statuses.take(2).forEach { status ->
                            Text(
                                text = formatStatus(status),
                                style = MaterialTheme.typography.bodySmall,
                                color = statusInlineTextColor(status.type),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (statuses.size > 2) {
                            Text(
                                text = "+${statuses.size - 2} más",
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
            .height(460.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            val context = LocalContext.current
            val fallbackPainter = rememberVectorPainter(Icons.Default.Person)

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
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

            FilledTonalIconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
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
    val locked = successes >= 3 || failures >= 3

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Tiradas de salvación contra muerte",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Éxitos", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { index ->
                            DeathSaveDot(filled = index < successes)
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Fallos", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { index ->
                            DeathSaveDot(filled = index < failures, isFailure = true)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onAddSuccess,
                    enabled = !locked,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+ Éxito")
                }

                OutlinedButton(
                    onClick = onAddFailure,
                    enabled = !locked,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+ Fallo")
                }
            }

            when {
                successes >= 3 -> {
                    Text(
                        text = "Estable",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                failures >= 3 -> {
                    Text(
                        text = "Muerto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
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
                shape = CircleShape
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
private fun HealDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Curar") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("Cantidad") }
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
                Text("Curar")
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
    onHeal: () -> Unit,
    onAddStatus: () -> Unit,
    onRemoveStatus: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = character.characterName.ifBlank { "Sin nombre" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = character.playerName.ifBlank { "Sin jugador" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                        onClick = onHeal,
                        label = { Text("Curar") }
                    )
                }
                AssistChip(
                    onClick = onAddStatus,
                    label = { Text("+Estado") }
                )

                if (statuses.isNotEmpty()) {
                    HorizontalDivider()

                    Text(
                        text = "Estados",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        statuses.forEach { status ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatStatus(status),
                                    color = statusInlineTextColor(status.type),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick={onRemoveStatus(status.id)},
                                    modifier = Modifier.size(32.dp)
                                ){
                                    Icon(imageVector=Icons.Default.Close,
                                        contentDescription = "Quitar estado",
                                        modifier = Modifier.size(18.dp))
                                }
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

private fun currentRoleLabel(character: Character): String {
    return when {
        character.isDead -> "Muerto"
        !character.isActive -> "Inactivo"
        character.type == CharacterType.PLAYER -> "Jugador"
        else -> "NPC"
    }
}

private fun buildHpLine(character: Character): String {
    val current = character.currentHp?.toString() ?: "?"
    val max = character.maxHp?.toString() ?: "?"
    val tempSuffix = if (character.tempHp > 0) " • Temp: ${character.tempHp}" else ""
    return "HP: $current/$max$tempSuffix"
}

private fun buildHpChipLabel(character: Character): String {
    val current = character.currentHp?.toString() ?: "?"
    val max = character.maxHp?.toString() ?: "?"
    return "HP: $current/$max"
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
    return base
}