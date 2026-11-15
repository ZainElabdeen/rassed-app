package app.injaz.starter.domain

import app.injaz.starter.data.AppData
import app.injaz.starter.data.CustomerEntity
import app.injaz.starter.data.PayMethod
import app.injaz.starter.data.PaymentEntity
import app.injaz.starter.data.ProductEntity
import app.injaz.starter.data.SaleEntity
import app.injaz.starter.data.StockEntryEntity
import app.injaz.starter.data.StockKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {
    private val data = AppData(
        products = listOf(
            ProductEntity("zain", "زين", 0, false, 1),
            ProductEntity("mtn", "MTN", 1, true, 1),
        ),
        customers = listOf(
            CustomerEntity("c1", "أحمد علي", "0912345678", 1),
            CustomerEntity("c2", "Bana", null, 1),
        ),
        sales = listOf(
            SaleEntity("s2", "zain", "زين", 1500, "c1", 20, PayMethod.CREDIT),
            SaleEntity("s1", "zain", "زين", 200, null, 10, PayMethod.CASH),
        ),
        payments = listOf(PaymentEntity("p1", "c1", 500, 30, PayMethod.BANKAK, "دفعة أولى")),
        stock = listOf(StockEntryEntity("e1", "zain", StockKind.ADD, 3000, "شراء من الموزع", 5)),
    )
    private val labels = mapOf(PayMethod.CASH to "نقدي", PayMethod.BANKAK to "بنكك", PayMethod.CREDIT to "آجل")

    @Test fun blankQueryFindsNothing() {
        assertTrue(searchAll("", data).isEmpty)
        assertTrue(searchAll("   ", data).isEmpty)
    }

    @Test fun emptyDataFindsNothing() {
        assertTrue(searchAll("زين", AppData()).isEmpty)
    }

    @Test fun findsCustomerByNameWithAlefVariants() {
        val r = searchAll("احمد", data)
        assertEquals(listOf("c1"), r.customers.map { it.id })
    }

    @Test fun findsCustomerByPhoneAndByArabicDigits() {
        assertEquals(listOf("c1"), searchAll("0912", data).customers.map { it.id })
        assertEquals(listOf("c1"), searchAll("٠٩١٢", data).customers.map { it.id })
    }

    @Test fun englishSearchIgnoresCase() {
        assertEquals(listOf("c2"), searchAll("BANA", data).customers.map { it.id })
    }

    @Test fun removedProductsAreNotListedButTheirSalesRemain() {
        assertEquals(listOf("zain"), searchAll("زين", data).products.map { it.id })
        assertTrue(searchAll("mtn", data).products.isEmpty())
    }

    @Test fun findsSalesByProductAmountCustomerAndMethod() {
        assertEquals(2, searchAll("زين", data).sales.size)
        assertEquals(listOf("s2"), searchAll("1500", data).sales.map { it.id })
        assertEquals(listOf("s2"), searchAll("1,500", data).sales.map { it.id })
        assertEquals(listOf("s2"), searchAll("احمد", data).sales.map { it.id })
        assertEquals(listOf("s2"), searchAll("آجل", data, labels).sales.map { it.id })
    }

    @Test fun everyWordMustMatch() {
        assertEquals(listOf("s2"), searchAll("زين 1500", data).sales.map { it.id })
        assertTrue(searchAll("زين 99999", data).sales.isEmpty())
    }

    @Test fun findsPaymentsByNoteAndCustomer() {
        assertEquals(listOf("p1"), searchAll("اولى", data).payments.map { it.id })
        assertEquals(listOf("p1"), searchAll("احمد بنكك", data, labels).payments.map { it.id })
    }

    @Test fun findsStockEntriesByReason() {
        assertEquals(listOf("e1"), searchAll("الموزع", data).stock.map { it.id })
    }

    @Test fun salesOfADeletedCustomerStillMatchOnOtherFields() {
        val afterDelete = data.copy(customers = data.customers.filter { it.id != "c1" })
        assertTrue(searchAll("احمد", afterDelete).sales.isEmpty())
        assertEquals(listOf("s2"), searchAll("1500", afterDelete).sales.map { it.id })
    }

    @Test fun noMatchGivesEmptyResults() {
        val r = searchAll("zzz", data)
        assertTrue(r.isEmpty)
        assertEquals(0, r.total)
    }

    @Test fun normalizeFoldsLetters() {
        assertEquals("احمد", normalizeForSearch("أَحْمَد"))
        assertEquals("1500", normalizeForSearch("١٥٠٠"))
        assertFalse(normalizeForSearch("مدرسة").contains('\u0629'))
    }
}

class DeleteCustomerRuleTest {
    @Test fun customerWhoOwesCannotBeDeleted() {
        assertFalse(canDeleteCustomer(1))
        assertFalse(canDeleteCustomer(5000))
    }

    @Test fun settledOrPaidAheadCustomerCanBeDeleted() {
        assertTrue(canDeleteCustomer(0))
        assertTrue(canDeleteCustomer(-200))
    }

    @Test fun balanceAfterFullPaymentAllowsDelete() {
        val sales = listOf(SaleEntity("s", "zain", "زين", 300, "c", 1, PayMethod.CREDIT))
        assertFalse(canDeleteCustomer(customerBalance("c", sales, emptyList())))
        val paid = listOf(PaymentEntity("p", "c", 300, 2, PayMethod.CASH))
        assertTrue(canDeleteCustomer(customerBalance("c", sales, paid)))
    }
}
