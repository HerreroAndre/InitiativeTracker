package com.dmc.initiativetracker.export

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object RoundCodeCodec {
    private const val PREFIX = "ITR1:"

    fun encode(transfer: RoundTransfer): String {
        val json = toJsonString(transfer)
        val compressed = gzip(json.toByteArray(StandardCharsets.UTF_8))

        return PREFIX + Base64.encodeToString(
            compressed,
            Base64.NO_WRAP
        )
    }

    fun decode(code: String): RoundTransfer {
        val trimmed = code.trim()

        require(trimmed.startsWith(PREFIX)) {
            "Código de ronda inválido"
        }

        val payload = trimmed.removePrefix(PREFIX)
        val compressed = Base64.decode(payload, Base64.NO_WRAP)
        val jsonText = gunzip(compressed).toString(StandardCharsets.UTF_8)

        return fromJsonString(jsonText)
    }

    private fun RoundTransfer.toJson(): JSONObject =
        JSONObject().apply {
            put("version", version)
            put("roundName", roundName)
            put(
                "characters",
                JSONArray().apply {
                    characters.forEach { character ->
                        put(character.toJson())
                    }
                }
            )
        }

    private fun RoundCharacterTransfer.toJson(): JSONObject =
        JSONObject().apply {
            put("playerName", playerName)
            put("characterName", characterName)
            put("initiative", initiative)

            putNullable("imageFileName", imageFileName)

            putNullable("currentHp", currentHp)
            putNullable("maxHp", maxHp)
            put("tempHp", tempHp)

            put("isActive", isActive)
            put("type", type)
            put("deathSuccesses", deathSuccesses)
            put("deathFailures", deathFailures)
            put("isDead", isDead)

            put(
                "statuses",
                JSONArray().apply {
                    statuses.forEach { status ->
                        put(status.toJson())
                    }
                }
            )
        }

    private fun RoundStatusTransfer.toJson(): JSONObject =
        JSONObject().apply {
            put("name", name)
            put("type", type)
            put("durationRounds", durationRounds)
            putNullable("originLabel", originLabel)
            putNullable("concentrationGroupId", concentrationGroupId)
        }

    private fun JSONObject.toRoundTransfer(): RoundTransfer {
        val charactersArray = getJSONArray("characters")

        return RoundTransfer(
            version = optInt("version", 1),
            roundName = optString("roundName", "Ronda importada"),
            characters = charactersArray.toCharacterTransferList()
        )
    }

    private fun JSONArray.toCharacterTransferList(): List<RoundCharacterTransfer> {
        val result = mutableListOf<RoundCharacterTransfer>()

        for (index in 0 until length()) {
            result += getJSONObject(index).toCharacterTransfer()
        }

        return result
    }

    private fun JSONObject.toCharacterTransfer(): RoundCharacterTransfer {
        val statusesArray = optJSONArray("statuses") ?: JSONArray()

        return RoundCharacterTransfer(
            playerName = optString("playerName", ""),
            characterName = optString("characterName", ""),
            initiative = optDouble("initiative", 10.0),
            imageFileName = optNullableString("imageFileName"),

            currentHp = optNullableInt("currentHp"),
            maxHp = optNullableInt("maxHp"),
            tempHp = optInt("tempHp", 0),

            isActive = optBoolean("isActive", true),
            type = optString("type", "NPC"),
            deathSuccesses = optInt("deathSuccesses", 0),
            deathFailures = optInt("deathFailures", 0),
            isDead = optBoolean("isDead", false),

            statuses = statusesArray.toStatusTransferList()
        )
    }

    private fun JSONArray.toStatusTransferList(): List<RoundStatusTransfer> {
        val result = mutableListOf<RoundStatusTransfer>()

        for (index in 0 until length()) {
            result += getJSONObject(index).toStatusTransfer()
        }

        return result
    }

    private fun JSONObject.toStatusTransfer(): RoundStatusTransfer =
        RoundStatusTransfer(
            name = optString("name", ""),
            type = optString("type", "NEUTRAL"),
            durationRounds = optInt("durationRounds", 1),
            originLabel = optNullableString("originLabel"),
            concentrationGroupId = optNullableString("concentrationGroupId")
        )

    private fun JSONObject.putNullable(name: String, value: Any?) {
        if (value == null) {
            put(name, JSONObject.NULL)
        } else {
            put(name, value)
        }
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (!has(name) || isNull(name)) null else optString(name)

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (!has(name) || isNull(name)) null else optInt(name)

    private fun gzip(input: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()

        GZIPOutputStream(output).use { gzip ->
            gzip.write(input)
        }

        return output.toByteArray()
    }

    private fun gunzip(input: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()

        GZIPInputStream(ByteArrayInputStream(input)).use { gzip ->
            gzip.copyTo(output)
        }

        return output.toByteArray()
    }

    fun toJsonString(transfer: RoundTransfer): String {
        return transfer.toJson().toString()
    }

    fun fromJsonString(jsonText: String): RoundTransfer {
        val json = JSONObject(jsonText)
        val transfer = json.toRoundTransfer()

        require(transfer.version == 1) {
            "Versión de ronda no soportada"
        }

        return transfer
    }
}