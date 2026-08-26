package com.example.todo_list.security

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Password
import androidx.compose.ui.graphics.vector.ImageVector

enum class LockType(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val length: Int = 0
) {
    PIN_4(
        id = "pin_4",
        displayName = "4-Digit PIN",
        subtitle = "Quick numerical passcode with 4 numbers",
        length = 4
    ),
    PIN_6(
        id = "pin_6",
        displayName = "6-Digit PIN",
        subtitle = "Standard secure numerical passcode with 6 numbers",
        length = 6
    ),
    PATTERN(
        id = "pattern",
        displayName = "Pattern Lock",
        subtitle = "Connect at least 4 dots on a 3x3 grid",
        length = 4 // minimum nodes
    ),
    PASSWORD(
        id = "password",
        displayName = "Alphanumeric Password",
        subtitle = "Complex password with letters, numbers & symbols",
        length = 4 // minimum characters
    );

    val icon: ImageVector
        get() = when (this) {
            PIN_4 -> Icons.Outlined.Dialpad
            PIN_6 -> Icons.Outlined.Pin
            PATTERN -> Icons.Outlined.Gesture
            PASSWORD -> Icons.Outlined.Password
        }

    companion object {
        val DEFAULT = PIN_4
        fun fromId(id: String?): LockType =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
    }
}
