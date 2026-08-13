package com.notify.android.tv.ui.components

import java.util.Locale

fun formatDuration(seconds: Double?): String {
    val s = seconds ?: return "0:00"
    val total = s.toInt().coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
}

fun formatDurationMs(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
}

fun formatBytes(n: Long): String {
    if (n <= 0) return "0 B"
    if (n > 1024L * 1024 * 1024) return String.format(Locale.US, "%.2f GB", n / (1024.0 * 1024 * 1024))
    if (n > 1024 * 1024) return String.format(Locale.US, "%.1f MB", n / (1024.0 * 1024))
    return String.format(Locale.US, "%.0f KB", n / 1024.0)
}

/** Deterministic hue used for Spotify-style placeholder gradients. */
fun hashHue(input: String): Float {
    var h = 0
    for (c in input) h = (h * 31 + c.code) % 360
    return h.toFloat()
}
