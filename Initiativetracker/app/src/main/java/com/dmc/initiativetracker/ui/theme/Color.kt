package com.dmc.initiativetracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.dmc.initiativetracker.domain.model.StatusType

@Composable
fun statusContainerColor(type: StatusType): Color {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    return when (type) {
        StatusType.POSITIVE ->
            if (isLight) Color(0xFFDDEFE3) else Color(0xFF2F6E4F)

        StatusType.NEGATIVE ->
            if (isLight) Color(0xFFF3DEDE) else Color(0xFF7A3E3E)

        StatusType.NEUTRAL ->
            if (isLight) Color(0xFFE3E8F0) else Color(0xFF4E5D73)
    }
}

@Composable
fun statusContentColor(type: StatusType): Color {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    return when (type) {
        StatusType.POSITIVE ->
            if (isLight) Color(0xFF1F5B3B) else Color.White

        StatusType.NEGATIVE ->
            if (isLight) Color(0xFF7A3E3E) else Color.White

        StatusType.NEUTRAL ->
            if (isLight) Color(0xFF435569) else Color.White
    }
}

@Composable
fun statusInlineTextColor(type: StatusType): Color {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f

    return when (type) {
        StatusType.POSITIVE ->
            if (isLight) Color(0xFF2F6E4F) else Color(0xFF8FD7B1)

        StatusType.NEGATIVE ->
            if (isLight) Color(0xFFA04C4C) else Color(0xFFF0A7A7)

        StatusType.NEUTRAL ->
            if (isLight) Color(0xFF586A7D) else Color(0xFFAEBFD3)
    }
}