package com.dmc.initiativetracker.ui.screen.round_selector

import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmc.initiativetracker.di.AppModule
import com.dmc.initiativetracker.domain.model.RoundListItem
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundSelectorScreen(
    onOpenRound: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val vm: RoundSelectorViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return RoundSelectorViewModel(
                    repo = AppModule.provideRoundRepository(context),
                    sortPreferences = com.dmc.initiativetracker.ui.preferences.SortPreferences(context)
                ) as T
            }
        }
    )

    val state by vm.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.toast) {
        state.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeToast()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cargar ronda") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = vm::openSortMenu) {
                            Icon(
                                imageVector = Icons.Default.Sort,
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
                                                index = currentIndex.coerceAtMost((state.rounds.size - 1).coerceAtLeast(0)),
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
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay rondas guardadas.")
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
private fun RoundCard(
    round: RoundListItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = round.name,
                    style = MaterialTheme.typography.titleMedium
                )

                val countLabel = if (round.characterCount == 1) {
                    "1 personaje"
                } else {
                    "${round.characterCount} personajes"
                }

                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Creada: ${formatCreatedAt(round.createdAt)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            TextButton(onClick = onDelete) {
                Text("Eliminar")
            }
        }
    }
}

@Composable
private fun formatCreatedAt(createdAt: Long): String {
    return remember(createdAt) {
        DateFormat.format("dd/MM/yyyy HH:mm", createdAt).toString()
    }
}