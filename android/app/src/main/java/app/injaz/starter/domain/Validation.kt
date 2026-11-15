package app.injaz.starter.domain

import app.injaz.starter.data.PayMethod
import app.injaz.starter.data.SaleEntity

enum class SaleError { AMOUNT, PRODUCT, CUSTOMER_REQUIRED, METHOD }

/** What is wrong with a new sale, or null when it can be saved. A pay-later sale needs a customer. */
fun validateSale(productId: String?, amount: Long?, customerId: String?, method: String): SaleError? = when {
    productId.isNullOrEmpty() -> SaleError.PRODUCT
    amount == null || amount < 1 || amount > MAX_AMOUNT -> SaleError.AMOUNT
    method !in PayMethod.forSale -> SaleError.METHOD
    method == PayMethod.CREDIT && customerId.isNullOrEmpty() -> SaleError.CUSTOMER_REQUIRED
    else -> null
}

/** True when selling [amount] would take more than the [available] stock: a warning, not a block. */
fun exceedsStock(available: Long, amount: Long): Boolean = amount > available

/** The sales that match the chosen product and payment method; null means "all". Order is kept. */
fun filterSales(sales: List<SaleEntity>, productId: String?, method: String?): List<SaleEntity> =
    sales.filter { (productId == null || it.productId == productId) && (method == null || it.method == method) }
