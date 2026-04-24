package com.dmc.initiativetracker.export

import android.content.Context
import android.net.Uri
import com.dmc.initiativetracker.domain.model.Character
import com.dmc.initiativetracker.domain.model.Status
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class RoundItrImageSource(
    val fileName: String,
    val imageUri: String
)

data class RoundItrExportPayload(
    val transfer: RoundTransfer,
    val images: List<RoundItrImageSource>
)

data class RoundItrImportPayload(
    val transfer: RoundTransfer,
    val images: Map<String, ByteArray>
)

object RoundItrFile {
    private const val ROUND_JSON = "round.json"
    private const val IMAGES_DIR = "images/"

    fun buildExportPayload(
        roundName: String,
        characters: List<Character>,
        statuses: List<Status>
    ): RoundItrExportPayload {
        val imageSources = mutableListOf<RoundItrImageSource>()
        val imageFileNamesByCharacterId = mutableMapOf<Long, String>()

        characters.forEachIndexed { index, character ->
            val imageUri = character.imageUri ?: return@forEachIndexed

            val fileName = "character_${index + 1}.jpg"

            imageFileNamesByCharacterId[character.id] = fileName
            imageSources += RoundItrImageSource(
                fileName = fileName,
                imageUri = imageUri
            )
        }

        val transfer = buildRoundTransfer(
            roundName = roundName,
            characters = characters,
            statuses = statuses,
            imageFileNameForCharacter = { character ->
                imageFileNamesByCharacterId[character.id]
            }
        )

        return RoundItrExportPayload(
            transfer = transfer,
            images = imageSources
        )
    }

    fun writeItr(
        context: Context,
        destinationUri: Uri,
        payload: RoundItrExportPayload
    ) {
        val resolver = context.contentResolver

        resolver.openOutputStream(destinationUri)?.use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry(ROUND_JSON))
                zip.write(
                    RoundCodeCodec
                        .toJsonString(payload.transfer)
                        .toByteArray(Charsets.UTF_8)
                )
                zip.closeEntry()

                payload.images.forEach { image ->
                    val bytes = resolver.openInputStream(Uri.parse(image.imageUri))
                        ?.use { input -> input.readBytes() }
                        ?: return@forEach

                    zip.putNextEntry(ZipEntry(IMAGES_DIR + image.fileName))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        } ?: error("No se pudo crear el archivo .itr")
    }

    fun readItr(
        context: Context,
        sourceUri: Uri
    ): RoundItrImportPayload {
        val resolver = context.contentResolver

        var transfer: RoundTransfer? = null
        val images = mutableMapOf<String, ByteArray>()

        resolver.openInputStream(sourceUri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry

                while (entry != null) {
                    val entryName = entry.name

                    when {
                        entryName == ROUND_JSON -> {
                            val jsonText = zip.readCurrentEntryBytes()
                                .toString(Charsets.UTF_8)

                            transfer = RoundCodeCodec.fromJsonString(jsonText)
                        }

                        entryName.startsWith(IMAGES_DIR) && !entry.isDirectory -> {
                            val fileName = entryName.removePrefix(IMAGES_DIR)

                            if (fileName.isNotBlank()) {
                                images[fileName] = zip.readCurrentEntryBytes()
                            }
                        }
                    }

                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("No se pudo leer el archivo .itr")

        return RoundItrImportPayload(
            transfer = transfer ?: error("El archivo seleccionado es incorrecto"),
            images = images
        )
    }

    private fun ZipInputStream.readCurrentEntryBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        copyTo(output)
        return output.toByteArray()
    }
}