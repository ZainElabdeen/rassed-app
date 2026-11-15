package app.injaz.starter.domain

import app.injaz.starter.data.CustomerEntity
import app.injaz.starter.data.PayMethod
import app.injaz.starter.data.PaymentEntity
import app.injaz.starter.data.SaleEntity

data class NamedTotal(val key: String, val name: String, val total: Long, val count: Int)

data class ReportSale(val sale: SaleEntity, val customerName: String?, val status: PaidStatus?)

data class ReportPayment(val payment: PaymentEntity, val customerName: String?)

/** Everything a report shows, for one span of time. Built from plain records so it can be tested and drawn on screen or into a PDF. */
data class ReportData(
    val range: DateRange,
    val totalSales: Long,
    val salesCount: Int,
    val byProduct: List<NamedTotal>,
    /** One entry for each of cash, Bankak and pay later, in that order, zeros included. [NamedTotal.key] is the method. */
    val byMethod: List<NamedTotal>,
    val creditGiven: Long,
    val paymentsReceived: Long,
    /** What all customers owe at the time of issue, whatever the range. */
    val outstandingNow: Long,
    val sales: List<ReportSale>,
    val payments: List<ReportPayment>,
)

fun buildReport(
    range: DateRange,
    sales: List<SaleEntity>,
    payments: List<PaymentEntity>,
    customers: List<CustomerEntity>,
): ReportData {
    val names = customers.associate { it.id to it.name }
    val inRange = sales.filter { it.createdAt in range }.sortedBy { it.createdAt }
    val paymentsIn = payments.filter { it.paidAt in range }.sortedBy { it.paidAt }
    val settlements = settleCreditSales(sales, payments)
    val byProduct = inRange.groupBy { it.productId }
        .map { (id, list) -> NamedTotal(id, list.last().productName, list.sumOf { it.amount }, list.size) }
        .sortedByDescending { it.total }
    val byMethod = PayMethod.forSale.map { method ->
        val list = inRange.filter { it.method == method }
        NamedTotal(method, method, list.sumOf { it.amount }, list.size)
    }
    return ReportData(
        range = range,
        totalSales = inRange.sumOf { it.amount },
        salesCount = inRange.size,
        byProduct = byProduct,
        byMethod = byMethod,
        creditGiven = inRange.filter { it.method == PayMethod.CREDIT }.sumOf { it.amount },
        paymentsReceived = paymentsIn.sumOf { it.amount },
        outstandingNow = totalOutstanding(customers, sales, payments),
        sales = inRange.map { ReportSale(it, it.customerId?.let(names::get), settlements[it.id]?.status) },
        payments = paymentsIn.map { ReportPayment(it, names[it.customerId]) },
    )
}
