package com.dmc.initiativetracker.domain.model

data class RoundListItem(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val characterCount: Int
)