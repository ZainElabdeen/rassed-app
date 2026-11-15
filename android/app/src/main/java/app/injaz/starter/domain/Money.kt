package app.injaz.starter.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val MAX_AMOUNT = 999_999_999_999L

/** 1500 -> "1,500". Always Latin digits, so figures read the same in Arabic and English. */
fun formatAmount(value: Long): String {
    val negative = value < 0
    val digits = if (negative) (-value).toString() else value.toString()
    val sb = StringBuilder()
    digits.forEachIndexed { i, c ->
        if (i > 0 && (digits.length - i) % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return if (negative) "-$sb" else sb.toString()
}

/** Turns Arabic-Indic and Persian digits into Latin ones and drops spaces and thousands separators. */
fun normalizeDigits(text: String): String {
    val sb = StringBuilder()
    for (c in text) {
        when (c) {
            in '\u0660'..'\u0669' -> sb.append('0' + (c - '\u0660'))
            in '\u06F0'..'\u06F9' -> sb.append('0' + (c - '\u06F0'))
            ',', '\u066C', '\u060C', ' ', '\u00A0' -> Unit
            else -> sb.append(c)
        }
    }
    return sb.toString()
}

/** A whole amount of at least 1, or null when the text is empty, not a number, zero or too large. */
fun parseAmount(text: String): Long? {
    val value = parseNonNegativeAmount(text) ?: return null
    return if (value >= 1) value else null
}

/** Like [parseAmount] but zero is allowed (an opening stock of 0). */
fun parseNonNegativeAmount(text: String): Long? {
    val clean = normalizeDigits(text.trim())
    if (clean.isEmpty() || clean.length > 12 || !clean.all { it in '0'..'9' }) return null
    val value = clean.toLongOrNull() ?: return null
    return if (value in 0..MAX_AMOUNT) value else null
}

fun formatDate(ts: Long): String = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(ts))

fun formatTime(ts: Long): String = SimpleDateFormat("HH:mm", Locale.US).format(Date(ts))

fun formatDateTime(ts: Long): String = "${formatDate(ts)}  ${formatTime(ts)}"
