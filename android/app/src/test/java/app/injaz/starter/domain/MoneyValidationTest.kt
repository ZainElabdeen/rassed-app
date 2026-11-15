package app.injaz.starter.domain

import app.injaz.starter.data.PayMethod
import app.injaz.starter.data.SaleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test fun formatsWithThousandsSeparators() {
        assertEquals("0", formatAmount(0))
        assertEquals("999", formatAmount(999))
        assertEquals("1,000", formatAmount(1000))
        assertEquals("1,500,000", formatAmount(1_500_000))
        assertEquals("-2,500", formatAmount(-2500))
    }

    @Test fun parsesPlainAndGroupedNumbers() {
        assertEquals(1500L, parseAmount("1500"))
        assertEquals(1500L, parseAmount(" 1,500 "))
    }

    @Test fun parsesArabicIndicDigits() {
        assertEquals(1500L, parseAmount("١٥٠٠"))
        assertEquals(250L, parseAmount("۲۵۰"))
    }

    @Test fun rejectsEmptyZeroNegativeAndText() {
        assertNull(parseAmount(""))
        assertNull(parseAmount("0"))
        assertNull(parseAmount("-5"))
        assertNull(parseAmount("12a"))
        assertNull(parseAmount("1.5"))
    }

    @Test fun rejectsTooLongNumbers() {
        assertNull(parseAmount("1234567890123"))
    }

    @Test fun nonNegativeAllowsZero() {
        assertEquals(0L, parseNonNegativeAmount("0"))
        assertNull(parseNonNegativeAmount("x"))
    }
}

class ValidationTest {
    private fun sale(id: String, product: String, method: String) =
        SaleEntity(id, product, product, 100, null, 1L, method)

    @Test fun validCashSaleNeedsNoCustomer() {
        assertNull(validateSale("zain", 500, null, PayMethod.CASH))
        assertNull(validateSale("zain", 500, null, PayMethod.BANKAK))
    }

    @Test fun payLaterNeedsACustomer() {
        assertEquals(SaleError.CUSTOMER_REQUIRED, validateSale("zain", 500, null, PayMethod.CREDIT))
        assertNull(validateSale("zain", 500, "c1", PayMethod.CREDIT))
    }

    @Test fun rejectsMissingProductBadAmountAndUnknownMethod() {
        assertEquals(SaleError.PRODUCT, validateSale(null, 500, null, PayMethod.CASH))
        assertEquals(SaleError.AMOUNT, validateSale("zain", null, null, PayMethod.CASH))
        assertEquals(SaleError.AMOUNT, validateSale("zain", 0, null, PayMethod.CASH))
        assertEquals(SaleError.METHOD, validateSale("zain", 5, null, "CARD"))
    }

    @Test fun stockWarningOnlyWhenAmountIsMore() {
        assertTrue(exceedsStock(100, 101))
        assertFalse(exceedsStock(100, 100))
    }

    @Test fun filtersByProductAndMethod() {
        val sales = listOf(sale("1", "zain", PayMethod.CASH), sale("2", "mtn", PayMethod.CASH), sale("3", "zain", PayMethod.CREDIT))
        assertEquals(3, filterSales(sales, null, null).size)
        assertEquals(listOf("1", "3"), filterSales(sales, "zain", null).map { it.id })
        assertEquals(listOf("3"), filterSales(sales, "zain", PayMethod.CREDIT).map { it.id })
        assertEquals(emptyList<String>(), filterSales(sales, "mtn", PayMethod.CREDIT).map { it.id })
    }
}
