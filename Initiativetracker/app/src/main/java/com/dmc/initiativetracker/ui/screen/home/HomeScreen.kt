package com.dmc.initiativetracker.ui.screen.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmc.initiativetracker.di.AppModule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreateRound: (Long) -> Unit,
    onLoadRound: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current

    val vm: HomeViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(
                    AppModule.provideRoundRepository(context)
                ) as T
            }
        }
    )

    val state by vm.uiState.collectAsState()

    LaunchedEffect(state.toast) {
        state.toast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            vm.consumeToast()
        }
    }

    if (state.isCreateDialogOpen) {
        AlertDialog(
            onDismissRequest = { if (!state.isWorking) vm.closeCreateDialog() },
            title = { Text("Crear ronda") },
            text = {
                OutlinedTextField(
                    value = state.newRoundName,
                    onValueChange = vm::onNewNameChange,
                    enabled = !state.isWorking,
                    singleLine = true,
                    label = { Text("Nombre") }
                )
            },
            confirmButton = {
                Button(
                    enabled = !state.isWorking,
                    onClick = {
                        vm.createRound(onCreated = onCreateRound)
                    }
                ) {
                    Text(if (state.isWorking) "Creando..." else "Crear")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isWorking,
                    onClick = vm::closeCreateDialog
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Iniciativa") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = vm::openCreateDialog
            ) {
                Text("Crear ronda")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onLoadRound
            ) {
                Text("Cargar ronda")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSettings
            ) {
                Text("Ajustes")
            }
        }
    }
}