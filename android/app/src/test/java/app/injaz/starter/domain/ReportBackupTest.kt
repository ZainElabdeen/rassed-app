package app.injaz.starter.domain

import app.injaz.starter.data.CustomerEntity
import app.injaz.starter.data.PayMethod
import app.injaz.starter.data.PaymentEntity
import app.injaz.starter.data.ProductEntity
import app.injaz.starter.data.SaleEntity
import app.injaz.starter.data.StockEntryEntity
import app.injaz.starter.data.StockKind
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportBuilderTest {
    private val customers = listOf(CustomerEntity("a", "Ali", null, 1))
    private val march = DateRanges.monthRange(ts(2025, 3, 15))

    private val sales = listOf(
        SaleEntity("1", "zain", "Zain", 1000, null, ts(2025, 3, 2), PayMethod.CASH),
        SaleEntity("2", "mtn", "MTN", 3000, "a", ts(2025, 3, 5), PayMethod.CREDIT),
        SaleEntity("3", "zain", "Zain", 500, null, ts(2025, 3, 9), PayMethod.BANKAK),
        SaleEntity("4", "zain", "Zain", 777, null, ts(2025, 4, 1), PayMethod.CASH),
        SaleEntity("5", "zain", "Zain", 888, null, ts(2025, 2, 28), PayMethod.CASH),
    )
    private val payments = listOf(
        PaymentEntity("p1", "a", 1000, ts(2025, 3, 20), PayMethod.CASH),
        PaymentEntity("p2", "a", 100, ts(2025, 4, 2), PayMethod.CASH),
    )

    @Test fun emptyRangeGivesZeros() {
        val r = buildReport(march, emptyList(), emptyList(), customers)
        assertEquals(0L, r.totalSales)
        assertEquals(3, r.byMethod.size)
        assertTrue(r.byMethod.all { it.total == 0L })
        assertTrue(r.byProduct.isEmpty())
    }

    @Test fun onlySalesInsideTheRangeCount() {
        val r = buildReport(march, sales, payments, customers)
        assertEquals(4500L, r.totalSales)
        assertEquals(3, r.salesCount)
        assertEquals(listOf("1", "2", "3"), r.sales.map { it.sale.id })
    }

    @Test fun groupsByProductBiggestFirstAndByMethodInFixedOrder() {
        val r = buildReport(march, sales, payments, customers)
        assertEquals(listOf("mtn", "zain"), r.byProduct.map { it.key })
        assertEquals(1500L, r.byProduct[1].total)
        assertEquals(listOf(PayMethod.CASH, PayMethod.BANKAK, PayMethod.CREDIT), r.byMethod.map { it.key })
        assertEquals(listOf(1000L, 500L, 3000L), r.byMethod.map { it.total })
    }

    @Test fun creditPaymentsAndOutstanding() {
        val r = buildReport(march, sales, payments, customers)
        assertEquals(3000L, r.creditGiven)
        assertEquals(1000L, r.paymentsReceived)
        // outstanding is as of now, over all payments: 3000 - 1000 - 100
        assertEquals(1900L, r.outstandingNow)
    }

    @Test fun saleLinesCarryCustomerNameAndStatus() {
        val r = buildReport(march, sales, payments, customers)
        val credit = r.sales.first { it.sale.id == "2" }
        assertEquals("Ali", credit.customerName)
        assertEquals(PaidStatus.PARTIAL, credit.status)
        assertEquals(null, r.sales.first { it.sale.id == "1" }.status)
    }
}

class BackupTest {
    private val json = Json { encodeDefaults = true }

    private fun data(
        sales: List<SaleEntity> = listOf(SaleEntity("s1", "zain", "Zain", 500, "c1", 10, PayMethod.CREDIT)),
        payments: List<PaymentEntity> = listOf(PaymentEntity("p1", "c1", 100, 20, PayMethod.CASH, "x")),
        stock: List<StockEntryEntity> = listOf(StockEntryEntity("e1", "zain", StockKind.OPENING, 5000, "", 1)),
    ) = BackupData(
        products = listOf(ProductEntity("zain", "Zain", 0, false, 1), ProductEntity("old", "Removed", 1, true, 1)),
        customers = listOf(CustomerEntity("c1", "Ali", null, 1)),
        sales = sales,
        payments = payments,
        stockEntries = stock,
    )

    private fun invalid(text: String): BackupError = (BackupCodec.parseAndValidate(text) as BackupResult.Invalid).error

    @Test fun roundTripKeepsEverythingIncludingRemovedProducts() {
        val original = data()
        val result = BackupCodec.parseAndValidate(BackupCodec.encode(original, 123L))
        assertTrue(result is BackupResult.Valid)
        val file = (result as BackupResult.Valid).file
        assertEquals(original, file.data)
        assertEquals(2, file.counts.products)
        assertEquals(123L, file.exportedAt)
    }

    @Test fun emptyDataRoundTrips() {
        val empty = BackupData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertTrue(BackupCodec.parseAndValidate(BackupCodec.encode(empty, 1)) is BackupResult.Valid)
    }

    @Test fun garbageIsUnreadable() {
        assertEquals(BackupError.UNREADABLE, invalid("not json at all"))
        assertEquals(BackupError.UNREADABLE, invalid(""))
    }

    @Test fun otherJsonIsTheWrongFormat() {
        assertEquals(BackupError.WRONG_FORMAT, invalid("""{"hello":1}"""))
        assertEquals(BackupError.WRONG_FORMAT, invalid("""{"format":"other","schemaVersion":1}"""))
    }

    @Test fun newerSchemaIsRefused() {
        val text = BackupCodec.encode(data(), 1).replace("\"schemaVersion\":1", "\"schemaVersion\":2")
        assertEquals(BackupError.NEWER_VERSION, invalid(text))
    }

    @Test fun editedContentFailsTheChecksum() {
        val text = BackupCodec.encode(data(), 1).replace("\"amount\":500", "\"amount\":900")
        assertEquals(BackupError.BAD_CHECKSUM, invalid(text))
    }

    private fun signed(d: BackupData, counts: BackupCounts = BackupCodec.countsOf(d)): String =
        json.encodeToString(BackupFile(BackupCodec.FORMAT, 1, 1, counts, BackupCodec.checksum(d), d))

    @Test fun wrongCountsAreRefused() {
        val d = data()
        assertEquals(BackupError.BAD_COUNTS, invalid(signed(d, BackupCodec.countsOf(d).copy(sales = 9))))
    }

    @Test fun saleOfAnUnknownProductIsRefused() {
        val d = data(sales = listOf(SaleEntity("s1", "ghost", "Ghost", 500, null, 10, PayMethod.CASH)))
        assertEquals(BackupError.BAD_REFERENCE, invalid(signed(d)))
    }

    @Test fun paymentOfADeletedCustomerStaysValid() {
        val d = data(payments = listOf(PaymentEntity("p1", "nobody", 100, 20, PayMethod.CASH)))
        assertTrue(BackupCodec.parseAndValidate(signed(d)) is BackupResult.Valid)
    }

    @Test fun negativeOrZeroAmountsAndUnknownMethodsAreRefused() {
        val negative = data(sales = listOf(SaleEntity("s1", "zain", "Zain", -5, null, 10, PayMethod.CASH)))
        assertEquals(BackupError.BAD_VALUE, invalid(signed(negative)))
        val badMethod = data(payments = listOf(PaymentEntity("p1", "c1", 100, 20, "CARD")))
        assertEquals(BackupError.BAD_VALUE, invalid(signed(badMethod)))
        val badKind = data(stock = listOf(StockEntryEntity("e1", "zain", "STEAL", 5, "", 1)))
        assertEquals(BackupError.BAD_VALUE, invalid(signed(badKind)))
    }

    @Test fun creditSaleWithoutCustomerIsRefused() {
        val d = data(sales = listOf(SaleEntity("s1", "zain", "Zain", 500, null, 10, PayMethod.CREDIT)))
        assertEquals(BackupError.BAD_VALUE, invalid(signed(d)))
    }

    @Test fun duplicateIdsAreRefused() {
        val sale = SaleEntity("s1", "zain", "Zain", 500, null, 10, PayMethod.CASH)
        assertEquals(BackupError.DUPLICATE_ID, invalid(signed(data(sales = listOf(sale, sale)))))
    }

    @Test fun sha256MatchesKnownVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", BackupCodec.sha256Hex("abc"))
    }
}
