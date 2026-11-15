package app.injaz.starter.domain

import java.util.Calendar

/** A half-open span of time: [from] inclusive, [to] exclusive, in epoch milliseconds. */
data class DateRange(val from: Long, val to: Long) {
    operator fun contains(ts: Long): Boolean = ts >= from && ts < to
}

object DateRanges {
    private fun cal(ts: Long): Calendar = Calendar.getInstance().apply { timeInMillis = ts }

    fun startOfDay(ts: Long): Long = cal(ts).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun startOfNextDay(ts: Long): Long = cal(startOfDay(ts)).apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

    fun startOfMonth(ts: Long): Long = cal(startOfDay(ts)).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

    fun startOfNextMonth(ts: Long): Long = cal(startOfMonth(ts)).apply { add(Calendar.MONTH, 1) }.timeInMillis

    /** The whole calendar month that contains [ts]. */
    fun monthRange(ts: Long): DateRange = DateRange(startOfMonth(ts), startOfNextMonth(ts))

    /** The first day of the month [delta] months from the one containing [ts] (negative goes back). */
    fun shiftMonth(ts: Long, delta: Int): Long = cal(startOfMonth(ts)).apply { add(Calendar.MONTH, delta) }.timeInMillis

    /** From the start of [fromDay] to the end of [toDay], both days included; null when the end is before the start. */
    fun customRange(fromDay: Long, toDay: Long): DateRange? {
        val from = startOfDay(fromDay)
        if (startOfDay(toDay) < from) return null
        return DateRange(from, startOfNextDay(toDay))
    }

    fun year(ts: Long): Int = cal(ts).get(Calendar.YEAR)

    /** Month number 1..12. */
    fun month(ts: Long): Int = cal(ts).get(Calendar.MONTH) + 1

    /** [dayStart] moved to the clock time of [now], so a payment dated to another day still carries a time. */
    fun atTimeOfDay(dayStart: Long, now: Long): Long = startOfDay(dayStart) + (now - startOfDay(now))

    /** The date picker speaks in UTC midnight; this is the local start of that same calendar day. */
    fun pickerUtcToLocalDay(utcMillis: Long): Long {
        val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        return Calendar.getInstance().apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        }.timeInMillis
    }

    /** The UTC midnight the date picker expects for the local day containing [localMillis]. */
    fun localDayToPickerUtc(localMillis: Long): Long {
        val local = cal(localMillis)
        return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        }.timeInMillis
    }
}
