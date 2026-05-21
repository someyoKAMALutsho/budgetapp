package com.gorib.app.ui.utils

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

fun formatRelativeDate(timestampMs: Long): String {
    val date = Instant.ofEpochMilli(timestampMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    val today = LocalDate.now(ZoneId.systemDefault())
    
    val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeStr = timeFormatter.format(Date(timestampMs))

    return when {
        date == today -> "Today, $timeStr"
        date == today.minusDays(1) -> "Yesterday, $timeStr"
        date.isAfter(today.minusDays(7)) -> {
            val dayOfWeek = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
            "$dayOfWeek, $timeStr"
        }
        else -> {
            val dayOfMonth = date.dayOfMonth
            val monthName = date.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
            "$dayOfMonth $monthName"
        }
    }
}

// Project Owner Signature Reference: someyo, kamal, utsho

