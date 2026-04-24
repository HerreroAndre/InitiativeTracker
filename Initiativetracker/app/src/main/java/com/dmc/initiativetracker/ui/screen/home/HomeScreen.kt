package com.dmc.initiativetracker.ui.screen.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dmc.initiativetracker.di.AppModule
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.dmc.initiativetracker.R

@Composable
fun HomeScreen(
    onCreateRound: (Long) -> Unit,
    onLoadRound: () -> Unit,
    onSettings: () -> Unit,
    onImageLibrary: () -> Unit
) {
    val context = LocalContext.current

    val factory = remember(context) {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(
                    AppModule.provideRoundRepository(context)
                ) as T
            }
        }
    }

    val vm: HomeViewModel = viewModel(factory = factory)
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

    Scaffold{ padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxWidth(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(0.22f))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Initiative Tracker",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Gestor de rondas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(90.dp))

                Image(
                    painter = painterResource(id = R.drawable.placeholder_logo),
                    contentDescription = "Logo de Initiative Tracker",
                    modifier = Modifier.size(160.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.weight(0.16f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        onClick = vm::openCreateDialog
                    ) {
                        Text("Crear ronda")
                    }

                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        onClick = onLoadRound
                    ) {
                        Text("Cargar ronda")
                    }

                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        onClick = onSettings
                    ) {
                        Text("Ajustes")
                    }

                    FilledTonalButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        onClick = onImageLibrary
                    ) {
                        Text("Biblioteca de imágenes")
                    }
                }

                Spacer(Modifier.weight(0.28f))
            }
        }
    }
}