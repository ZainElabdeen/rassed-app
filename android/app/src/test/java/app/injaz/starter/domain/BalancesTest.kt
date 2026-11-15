package app.injaz.starter.domain

import app.injaz.starter.data.CustomerEntity
import app.injaz.starter.data.PayMethod
import app.injaz.starter.data.PaymentEntity
import app.injaz.starter.data.SaleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun credit(id: String, customer: String, amount: Long, at: Long, product: String = "zain") =
    SaleEntity(id, product, product, amount, customer, at, PayMethod.CREDIT)

private fun pay(id: String, customer: String, amount: Long, at: Long) =
    PaymentEntity(id, customer, amount, at, PayMethod.CASH)

class BalancesTest {
    private val customers = listOf(CustomerEntity("a", "Ali", null, 1), CustomerEntity("b", "Bana", null, 1))

    @Test fun noRecordsMeansZeroBalance() {
        assertEquals(0L, customerBalance("a", emptyList(), emptyList()))
        assertEquals(0L, totalOutstanding(customers, emptyList(), emptyList()))
    }

    @Test fun balanceIsCreditSalesMinusPayments() {
        val sales = listOf(
            credit("1", "a", 300, 10),
            credit("2", "a", 200, 20),
            SaleEntity("3", "zain", "zain", 999, "a", 30, PayMethod.CASH),
        )
        assertEquals(500L, customerBalance("a", sales, emptyList()))
        assertEquals(350L, customerBalance("a", sales, listOf(pay("p", "a", 150, 40))))
    }

    @Test fun overpaymentGoesNegativeButIsNotCountedAsOwed() {
        val sales = listOf(credit("1", "a", 100, 10), credit("2", "b", 400, 10))
        val payments = listOf(pay("p", "a", 250, 20))
        assertEquals(-150L, customerBalance("a", sales, payments))
        assertEquals(400L, totalOutstanding(customers, sales, payments))
    }

    @Test fun paymentsSettleOldestSalesFirst() {
        val sales = listOf(credit("new", "a", 200, 20), credit("old", "a", 100, 10))
        val result = settleCreditSales(sales, listOf(pay("p", "a", 150, 30)))
        assertEquals(PaidStatus.PAID, result["old"]!!.status)
        assertEquals(PaidStatus.PARTIAL, result["new"]!!.status)
        assertEquals(50L, result["new"]!!.paid)
    }

    @Test fun unpaidAndFullyPaid() {
        val sales = listOf(credit("1", "a", 100, 10), credit("2", "b", 100, 10))
        val result = settleCreditSales(sales, listOf(pay("p", "b", 100, 20)))
        assertEquals(PaidStatus.UNPAID, result["1"]!!.status)
        assertEquals(PaidStatus.PAID, result["2"]!!.status)
    }

    @Test fun cashSalesAreNotSettled() {
        val cash = SaleEntity("c", "zain", "zain", 100, "a", 1, PayMethod.CASH)
        assertNull(settleCreditSales(listOf(cash), emptyList())["c"])
    }

    @Test fun purchasesGroupedByProductBiggestFirst() {
        val sales = listOf(
            credit("1", "a", 100, 10, "zain"),
            credit("2", "a", 700, 20, "mtn"),
            credit("3", "a", 50, 30, "zain"),
            credit("4", "b", 999, 30, "mtn"),
        )
        val result = purchasesByProduct("a", sales)
        assertEquals(listOf("mtn", "zain"), result.map { it.productId })
        assertEquals(150L, result[1].total)
        assertEquals(2, result[1].count)
    }

    @Test fun timelineMixesSalesAndPaymentsNewestFirst() {
        val sales = listOf(credit("1", "a", 100, 10), credit("x", "b", 100, 15))
        val payments = listOf(pay("p", "a", 40, 20))
        val items = customerTimeline("a", sales, payments)
        assertEquals(2, items.size)
        assertTrue(items[0] is TimelineItem.PaymentItem)
        assertTrue(items[1] is TimelineItem.SaleItem)
        assertEquals(PaidStatus.PARTIAL, (items[1] as TimelineItem.SaleItem).settlement!!.status)
    }
}
