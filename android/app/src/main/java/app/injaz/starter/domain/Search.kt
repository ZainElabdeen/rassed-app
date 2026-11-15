package app.injaz.starter.domain

import app.injaz.starter.data.AppData
import app.injaz.starter.data.CustomerEntity
import app.injaz.starter.data.PaymentEntity
import app.injaz.starter.data.ProductEntity
import app.injaz.starter.data.SaleEntity
import app.injaz.starter.data.StockEntryEntity

/** What one search found, section by section. Sales, payments and stock entries keep their newest-first order. */
data class SearchResults(
    val customers: List<CustomerEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val stock: List<StockEntryEntity> = emptyList(),
) {
    val total: Int get() = customers.size + products.size + sales.size + payments.size + stock.size
    val isEmpty: Boolean get() = total == 0
}

/**
 * Lower-cases, turns Arabic-Indic and Persian digits into Latin ones, drops Arabic diacritics and
 * tatweel, and folds alef, ya and ta-marbuta variants, so "أحمد" finds "احمد" and "١٥٠٠" finds "1500".
 */
fun normalizeForSearch(text: String): String {
    val sb = StringBuilder(text.length)
    for (c in text) {
        when (c) {
            in '\u0660'..'\u0669' -> sb.append('0' + (c - '\u0660'))
            in '\u06F0'..'\u06F9' -> sb.append('0' + (c - '\u06F0'))
            '\u0640', '\u0670' -> Unit
            in '\u064B'..'\u065F' -> Unit
            '\u0623', '\u0625', '\u0622', '\u0671' -> sb.append('\u0627')
            '\u0649' -> sb.append('\u064A')
            '\u0629' -> sb.append('\u0647')
            else -> sb.append(c.lowercaseChar())
        }
    }
    return sb.toString()
}

/**
 * Searches customers (name, phone), active products (name), sales (product, customer, amount, method, date),
 * payments (customer, note, amount, method, date) and stock entries (reason, product, amount).
 * Every word of [query] must appear somewhere in a record. A blank query finds nothing.
 * [methodLabels] maps CASH / BANKAK / CREDIT to the words shown on screen, so "آجل" finds pay-later sales.
 */
fun searchAll(query: String, data: AppData, methodLabels: Map<String, String> = emptyMap()): SearchResults {
    val tokens = normalizeForSearch(query)
        .split(' ', '\t', '\n')
        .map { it.replace(",", "") }
        .filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return SearchResults()

    fun matches(vararg parts: String?): Boolean {
        val haystack = parts.filterNotNull().joinToString(" ") { normalizeForSearch(it) }
        return tokens.all { haystack.contains(it) }
    }

    val customerNames = data.customers.associate { it.id to it.name }
    val productNames = data.products.associate { it.id to it.name }

    return SearchResults(
        customers = data.customers.filter { matches(it.name, it.phone) },
        products = data.activeProducts.filter { matches(it.name) },
        sales = data.sales.filter {
            matches(
                it.productName,
                it.customerId?.let { id -> customerNames[id] },
                it.amount.toString(),
                formatAmount(it.amount),
                methodLabels[it.method] ?: it.method,
                formatDate(it.createdAt),
            )
        },
        payments = data.payments.filter {
            matches(
                customerNames[it.customerId],
                it.note,
                it.amount.toString(),
                formatAmount(it.amount),
                methodLabels[it.method] ?: it.method,
                formatDate(it.paidAt),
            )
        },
        stock = data.stock.filter {
            matches(it.reason, productNames[it.productId], it.amount.toString(), formatAmount(it.amount))
        },
    )
}
