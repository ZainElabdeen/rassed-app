package app.injaz.starter.domain

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

fun ts(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0): Long =
    Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, min, 0) }.timeInMillis

class DateRangesTest {
    @Test fun startOfMonthIsFirstMidnight() {
        assertEquals(ts(2025, 3, 1), DateRanges.startOfMonth(ts(2025, 3, 17, 14, 30)))
    }

    @Test fun monthRangeIsHalfOpenAndCoversTheMonth() {
        val r = DateRanges.monthRange(ts(2025, 2, 10))
        assertEquals(ts(2025, 2, 1), r.from)
        assertEquals(ts(2025, 3, 1), r.to)
        assertTrue(ts(2025, 2, 28, 23, 59) in r)
        assertFalse(ts(2025, 3, 1) in r)
    }

    @Test fun shiftMonthCrossesYears() {
        assertEquals(ts(2024, 12, 1), DateRanges.shiftMonth(ts(2025, 1, 20), -1))
        assertEquals(ts(2026, 1, 1), DateRanges.shiftMonth(ts(2025, 12, 5), 1))
    }

    @Test fun customRangeIncludesTheLastDay() {
        val r = DateRanges.customRange(ts(2025, 5, 3, 15), ts(2025, 5, 5, 9))!!
        assertEquals(ts(2025, 5, 3), r.from)
        assertTrue(ts(2025, 5, 5, 23, 59) in r)
        assertFalse(ts(2025, 5, 6) in r)
    }

    @Test fun customRangeRejectsEndBeforeStart() {
        assertNull(DateRanges.customRange(ts(2025, 5, 5), ts(2025, 5, 4)))
    }

    @Test fun sameDayCustomRangeIsOneDay() {
        val r = DateRanges.customRange(ts(2025, 5, 5), ts(2025, 5, 5))!!
        assertEquals(ts(2025, 5, 6), r.to)
    }

    @Test fun pickerRoundTripKeepsTheCalendarDay() {
        val day = ts(2025, 9, 14)
        assertEquals(day, DateRanges.pickerUtcToLocalDay(DateRanges.localDayToPickerUtc(day)))
        assertEquals(day, DateRanges.pickerUtcToLocalDay(DateRanges.localDayToPickerUtc(ts(2025, 9, 14, 22, 10))))
    }

    @Test fun atTimeOfDayKeepsClockTime() {
        val result = DateRanges.atTimeOfDay(ts(2025, 1, 10), ts(2025, 2, 20, 13, 45))
        assertEquals(ts(2025, 1, 10, 13, 45), result)
    }
}
