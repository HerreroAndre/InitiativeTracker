package com.dmc.initiativetracker.ui.screen.image_library

import android.Manifest
import android.content.pm.PackageManager
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dmc.initiativetracker.util.ImageStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageLibraryScreen(
    vm: ImageLibraryViewModel,
    selectable: Boolean = false,
    onSelectImage: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current
    var pendingDeleteImageId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDeleteImageName by rememberSaveable { mutableStateOf<String?>(null) }
    var renameImageId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renameImageText by rememberSaveable { mutableStateOf("") }
    var pendingNewImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingNewImageName by rememberSaveable { mutableStateOf("") }
    var previewImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    var previewImageName by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddImageDialog by rememberSaveable { mutableStateOf(false) }
    var pendingCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val internalUri = ImageStorage.copyToInternalStorage(context, uri)
                val suggestedName = guessDisplayName(context, uri)

                pendingNewImageUri = internalUri
                pendingNewImageName = suggestedName
            }.onFailure {
                Toast.makeText(
                    context,
                    "No se pudo cargar la imagen",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val pending = pendingCameraUriString

        if (success && pending != null) {
            runCatching {
                val copiedUri = ImageStorage.copyToInternalStorage(
                    context = context,
                    sourceUri = pending.toUri()
                )

                pendingNewImageUri = copiedUri
                pendingNewImageName = "Foto"
            }.onFailure {
                Toast.makeText(
                    context,
                    "No se pudo cargar la foto",
                    Toast.LENGTH_SHORT
                ).show()
            }

            ImageStorage.deleteAnyUri(context, pending)
        } else {
            ImageStorage.deleteAnyUri(context, pending)
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
        } else {
            Toast.makeText(
                context,
                "Se necesita permiso de cámara",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    if (pendingDeleteImageId != null) {
        AlertDialog(
            onDismissRequest = {
                pendingDeleteImageId = null
                pendingDeleteImageName = null
            },
            title = { Text("Borrar imagen") },
            text = {
                Text(
                    "¿Estás seguro de que querés borrar ${
                        pendingDeleteImageName?.ifBlank { "esta imagen" } ?: "esta imagen"
                    }?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteImage(pendingDeleteImageId!!)
                        pendingDeleteImageId = null
                        pendingDeleteImageName = null
                    }
                ) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingDeleteImageId = null
                        pendingDeleteImageName = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (renameImageId != null) {
        AlertDialog(
            onDismissRequest = {
                renameImageId = null
                renameImageText = ""
            },
            title = { Text("Renombrar imagen") },
            text = {
                OutlinedTextField(
                    value = renameImageText,
                    onValueChange = { renameImageText = it },
                    singleLine = true,
                    label = { Text("Nombre") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.renameImage(
                            id = renameImageId!!,
                            name = renameImageText
                        )
                        renameImageId = null
                        renameImageText = ""
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        renameImageId = null
                        renameImageText = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (pendingNewImageUri != null) {
        AlertDialog(
            onDismissRequest = {
                ImageStorage.deleteFileUri(pendingNewImageUri)
                pendingNewImageUri = null
                pendingNewImageName = ""
            },
            title = { Text("Guardar imagen") },
            text = {
                OutlinedTextField(
                    value = pendingNewImageName,
                    onValueChange = { pendingNewImageName = it },
                    singleLine = true,
                    label = { Text("Nombre") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val imageUri = pendingNewImageUri ?: return@TextButton

                        vm.addImage(
                            name = pendingNewImageName,
                            imageUri = imageUri
                        )

                        pendingNewImageUri = null
                        pendingNewImageName = ""
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        ImageStorage.deleteFileUri(pendingNewImageUri)
                        pendingNewImageUri = null
                        pendingNewImageName = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (previewImageUri != null) {
        Dialog(
            onDismissRequest = {
                previewImageUri = null
                previewImageName = null
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(previewImageUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = previewImageName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    if (showAddImageDialog) {
        AlertDialog(
            onDismissRequest = { showAddImageDialog = false },
            title = { Text("Agregar imagen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showAddImageDialog = false

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
                    ) {
                        Text("📸 Sacar foto")
                    }

                    TextButton(
                        onClick = {
                            showAddImageDialog = false
                            pickMedia.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Text("🖼 Elegir de galería")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddImageDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectable) "Elegir imagen" else "Biblioteca de imágenes"
                    )
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
                    if (!selectable) {
                        IconButton(
                            onClick = { showAddImageDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Agregar imagen"
                            )
                        }
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
                            ImageLibrarySortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = { vm.selectSort(option) },
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = vm::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                singleLine = true,
                label = { Text("Buscar imagen") }
            )

            if (state.images.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.searchQuery.isBlank()) {
                            "Todavía no hay imágenes guardadas"
                        } else {
                            "No se encontraron imágenes"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.images, key = { it.id }) { image ->
                        val fallbackPainter = rememberVectorPainter(Icons.Default.Person)

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (selectable) {
                                            onSelectImage(image.imageUri)
                                        }
                                    },
                                    onLongClick = {
                                        if (!selectable) {
                                            previewImageUri = image.imageUri
                                            previewImageName = image.name
                                        }
                                    }
                                )
                        ) {
                            Box {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(image.imageUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = image.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .size(160.dp),
                                    contentScale = ContentScale.Crop,
                                    placeholder = fallbackPainter,
                                    error = fallbackPainter,
                                    fallback = fallbackPainter
                                )

                                if (!selectable) {
                                    IconButton(
                                        onClick = {
                                            pendingDeleteImageId = image.id
                                            pendingDeleteImageName = image.name
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar imagen"
                                        )
                                    }
                                }
                            }

                            Text(
                                text = image.name,
                                modifier = Modifier.padding(12.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (!selectable) {
                                TextButton(
                                    onClick = {
                                        renameImageId = image.id
                                        renameImageText = image.name
                                    }
                                ) {
                                    Text("Renombrar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun guessDisplayName(
    context: android.content.Context,
    uri: android.net.Uri
): String {
    val resolver = context.contentResolver

    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
                    ?.substringBeforeLast('.')
                    ?.ifBlank { "Imagen" }
                    ?: "Imagen"
            }
        }

    return "Imagen"
}