package app.injaz.starter.domain

import app.injaz.starter.data.PayMethod
import app.injaz.starter.data.SaleEntity
import app.injaz.starter.data.StockEntryEntity
import app.injaz.starter.data.StockKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun entry(id: String, product: String, kind: String, amount: Long, at: Long) =
    StockEntryEntity(id, product, kind, amount, "r", at)

private fun sale(id: String, product: String, amount: Long, at: Long) =
    SaleEntity(id, product, product, amount, null, at, PayMethod.CASH)

class InventoryTest {
    private val jan = ts(2025, 1, 1)
    private val feb = ts(2025, 2, 1)

    @Test fun noRecordsIsZero() {
        val s = computeStock("zain", emptyList(), emptyList())
        assertEquals(0L, s.remaining)
        assertFalse(s.hasOpening)
        assertEquals(0f, s.fraction, 0f)
    }

    @Test fun openingPlusAddsMinusDeductionsMinusSales() {
        val entries = listOf(
            entry("1", "zain", StockKind.OPENING, 10_000, jan),
            entry("2", "zain", StockKind.ADD, 2_000, ts(2025, 1, 5)),
            entry("3", "zain", StockKind.DEDUCT, 500, ts(2025, 1, 6)),
        )
        val sales = listOf(sale("a", "zain", 1_500, ts(2025, 1, 7)), sale("b", "mtn", 9_999, ts(2025, 1, 7)))
        val s = computeStock("zain", sales, entries)
        assertEquals(10_000L, s.opening)
        assertEquals(10_000L + 2_000 - 500 - 1_500, s.remaining)
        assertEquals(12_000L, s.total)
    }

    @Test fun newerOpeningReplacesEverythingBeforeIt() {
        val entries = listOf(
            entry("1", "zain", StockKind.OPENING, 10_000, jan),
            entry("2", "zain", StockKind.ADD, 2_000, ts(2025, 1, 5)),
            entry("3", "zain", StockKind.OPENING, 4_000, feb),
        )
        val sales = listOf(sale("a", "zain", 3_000, ts(2025, 1, 20)), sale("b", "zain", 1_000, ts(2025, 2, 3)))
        assertEquals(3_000L, computeStock("zain", sales, entries).remaining)
    }

    @Test fun withoutOpeningSalesGoNegative() {
        val s = computeStock("zain", listOf(sale("a", "zain", 100, 5)), emptyList())
        assertEquals(-100L, s.remaining)
    }

    @Test fun lowStockAtOrBelowTenPercent() {
        val entries = listOf(entry("1", "zain", StockKind.OPENING, 1_000, jan))
        assertFalse(computeStock("zain", listOf(sale("a", "zain", 800, feb)), entries).isLow)
        assertTrue(computeStock("zain", listOf(sale("a", "zain", 900, feb)), entries).isLow)
    }

    @Test fun fractionIsClamped() {
        val entries = listOf(entry("1", "zain", StockKind.OPENING, 1_000, jan))
        assertEquals(0.5f, computeStock("zain", listOf(sale("a", "zain", 500, feb)), entries).fraction, 0.001f)
        assertEquals(0f, computeStock("zain", listOf(sale("a", "zain", 5_000, feb)), entries).fraction, 0f)
    }

    @Test fun needsOpeningWhenNeverSetOrFromAnEarlierMonth() {
        val now = ts(2025, 3, 10)
        assertTrue(needsOpening(computeStock("zain", emptyList(), emptyList()), now))
        val old = computeStock("zain", emptyList(), listOf(entry("1", "zain", StockKind.OPENING, 5, feb)))
        assertTrue(needsOpening(old, now))
        val fresh = computeStock("zain", emptyList(), listOf(entry("1", "zain", StockKind.OPENING, 5, ts(2025, 3, 1))))
        assertFalse(needsOpening(fresh, now))
    }

    @Test fun stockAddedThisMonthWithoutOpeningCountsAsSet() {
        val now = ts(2025, 3, 10)
        val added = computeStock("zain", emptyList(), listOf(entry("1", "zain", StockKind.ADD, 100_000, ts(2025, 3, 2))))
        assertFalse(needsOpening(added, now))
        assertTrue(added.hasStock)
        assertEquals(100_000L, added.remaining)
        val oldAdd = computeStock("zain", emptyList(), listOf(entry("1", "zain", StockKind.ADD, 100_000, feb)))
        assertTrue(needsOpening(oldAdd, now))
    }
}
