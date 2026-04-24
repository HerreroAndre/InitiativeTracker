package com.dmc.initiativetracker.export

data class RoundTransfer(
    val version: Int = 1,
    val roundName: String,
    val characters: List<RoundCharacterTransfer>
)

data class RoundCharacterTransfer(
    val playerName: String,
    val characterName: String,
    val initiative: Double,
    val imageFileName: String? = null,

    val currentHp: Int?,
    val maxHp: Int?,
    val tempHp: Int,

    val isActive: Boolean,
    val type: String,
    val deathSuccesses: Int,
    val deathFailures: Int,
    val isDead: Boolean,

    val statuses: List<RoundStatusTransfer> = emptyList()
)

data class RoundStatusTransfer(
    val name: String,
    val type: String,
    val durationRounds: Int,
    val originLabel: String? = null,
    val concentrationGroupId: String? = null
)

data class RoundTransferSummary(
    val roundName: String,
    val characterCount: Int,
    val statusCount: Int,
    val imageCount: Int
)

fun RoundTransfer.summary(): RoundTransferSummary =
    RoundTransferSummary(
        roundName = roundName,
        characterCount = characters.size,
        statusCount = characters.sumOf { it.statuses.size },
        imageCount = characters.count { !it.imageFileName.isNullOrBlank() }
    )