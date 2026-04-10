package com.dmc.initiativetracker.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID
import androidx.core.content.FileProvider

object ImageStorage {
    private const val DIR_NAME = "character_images"

    fun copyToInternalStorage(context: Context, sourceUri: Uri): String {
        val imagesDir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        val destFile = File(imagesDir, "${UUID.randomUUID()}.jpg")

        context.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "No se pudo abrir InputStream para $sourceUri" }
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return destFile.toURI().toString() // "file://..."
    }

    fun deleteIfInternal(context: Context, uriString: String?) {
        if (uriString.isNullOrBlank()) return
        if (!uriString.startsWith("file:")) return

        runCatching {
            val path = Uri.parse(uriString).path ?: return
            val file = File(path)

            val imagesDir = File(context.filesDir, DIR_NAME).canonicalPath
            val filePath = file.canonicalPath

            if (filePath.startsWith(imagesDir)) {
                file.delete()
            }
        }
    }

    fun createCameraOutputUri(context: Context): Uri {
        val imagesDir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        val destFile = File(imagesDir, "${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            destFile
        )
    }

    fun deleteFileUri(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        if (!uriString.startsWith("file:")) return
        runCatching {
            val path = Uri.parse(uriString).path ?: return
            File(path).delete()
        }
    }


}