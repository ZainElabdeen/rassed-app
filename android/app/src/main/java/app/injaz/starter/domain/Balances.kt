package app.injaz.starter.domain

import app.injaz.starter.data.CustomerEntity
import app.injaz.starter.data.PayMethod
import app.injaz.starter.data.PaymentEntity
import app.injaz.starter.data.SaleEntity

enum class PaidStatus { PAID, PARTIAL, UNPAID }

/** How much of one pay-later sale has been covered by the customer's payments. */
data class Settlement(val saleId: String, val paid: Long, val status: PaidStatus)

/** What a customer owes: pay-later sales minus payments. Can go negative if they paid ahead. */
fun customerBalance(customerId: String, sales: List<SaleEntity>, payments: List<PaymentEntity>): Long {
    val credit = sales.filter { it.customerId == customerId && it.method == PayMethod.CREDIT }.sumOf { it.amount }
    val paid = payments.filter { it.customerId == customerId }.sumOf { it.amount }
    return credit - paid
}

/** A customer can be deleted only when they owe nothing (a zero or paid-ahead balance). */
fun canDeleteCustomer(balance: Long): Boolean = balance <= 0

/** Total of what customers owe: each customer's positive balance only. */
fun totalOutstanding(customers: List<CustomerEntity>, sales: List<SaleEntity>, payments: List<PaymentEntity>): Long =
    customers.sumOf { customerBalance(it.id, sales, payments).coerceAtLeast(0) }

/**
 * Spreads each customer's payments over their pay-later sales, oldest first, and says whether
 * every sale is paid, partly paid or unpaid. Keyed by sale id; only pay-later sales with a customer appear.
 */
fun settleCreditSales(sales: List<SaleEntity>, payments: List<PaymentEntity>): Map<String, Settlement> {
    val result = HashMap<String, Settlement>()
    val paidByCustomer = payments.groupBy { it.customerId }.mapValues { (_, list) -> list.sumOf { it.amount } }
    sales.filter { it.method == PayMethod.CREDIT && it.customerId != null }
        .groupBy { it.customerId!! }
        .forEach { (customerId, list) ->
            var remaining = paidByCustomer[customerId] ?: 0L
            list.sortedBy { it.createdAt }.forEach { sale ->
                val covered = minOf(remaining, sale.amount).coerceAtLeast(0)
                remaining -= covered
                val status = when {
                    covered >= sale.amount -> PaidStatus.PAID
                    covered > 0 -> PaidStatus.PARTIAL
                    else -> PaidStatus.UNPAID
                }
                result[sale.id] = Settlement(sale.id, covered, status)
            }
        }
    return result
}

/** The credit types a customer bought: total and count per product, biggest first. */
data class ProductTotal(val productId: String, val name: String, val total: Long, val count: Int)

fun purchasesByProduct(customerId: String, sales: List<SaleEntity>): List<ProductTotal> =
    sales.filter { it.customerId == customerId }
        .groupBy { it.productId }
        .map { (id, list) -> ProductTotal(id, list.maxByOrNull { it.createdAt }!!.productName, list.sumOf { it.amount }, list.size) }
        .sortedByDescending { it.total }

/** One line of a customer's history: a sale or a payment. */
sealed interface TimelineItem {
    val at: Long
    data class SaleItem(val sale: SaleEntity, val settlement: Settlement?) : TimelineItem {
        override val at: Long get() = sale.createdAt
    }
    data class PaymentItem(val payment: PaymentEntity) : TimelineItem {
        override val at: Long get() = payment.paidAt
    }
}

/** Every sale and payment of a customer, newest first. */
fun customerTimeline(customerId: String, sales: List<SaleEntity>, payments: List<PaymentEntity>): List<TimelineItem> {
    val settlements = settleCreditSales(sales.filter { it.customerId == customerId }, payments.filter { it.customerId == customerId })
    val saleItems = sales.filter { it.customerId == customerId }.map { TimelineItem.SaleItem(it, settlements[it.id]) }
    val paymentItems = payments.filter { it.customerId == customerId }.map { TimelineItem.PaymentItem(it) }
    return (saleItems + paymentItems).sortedByDescending { it.at }
}
