package app.injaz.starter.domain

import app.injaz.starter.data.SaleEntity
import app.injaz.starter.data.StockEntryEntity
import app.injaz.starter.data.StockKind

/**
 * One product's stock as it stands now. [total] is the opening plus what was added since;
 * the bar on a product card shows [remaining] against it.
 */
data class ProductStock(
    val productId: String,
    val remaining: Long,
    val opening: Long,
    val added: Long,
    val deducted: Long,
    val sold: Long,
    val hasOpening: Boolean,
    val openingAt: Long?,
    val lastAddAt: Long? = null,
) {
    val total: Long get() = opening + added

    /** True when the product has a starting amount: an opening, or stock added since there was none. */
    val hasStock: Boolean get() = hasOpening || added > 0

    /** Share of the total still in stock, 0..1, for the progress bar. */
    val fraction: Float get() = if (total > 0) (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f

    /** At or below a tenth of the total, once there is a total to compare with. */
    val isLow: Boolean get() = total > 0 && remaining * 10 <= total
}

/**
 * Stock = the latest opening, plus later additions, minus later deductions, minus sales of the product since
 * that opening. With no opening it counts everything from zero.
 */
fun computeStock(productId: String, sales: List<SaleEntity>, entries: List<StockEntryEntity>): ProductStock {
    val mine = entries.filter { it.productId == productId }
    val opening = mine.filter { it.kind == StockKind.OPENING }.maxByOrNull { it.createdAt }
    val since = opening?.createdAt ?: Long.MIN_VALUE
    val added = mine.filter { it.kind == StockKind.ADD && it.createdAt >= since }.sumOf { it.amount }
    val deducted = mine.filter { it.kind == StockKind.DEDUCT && it.createdAt >= since }.sumOf { it.amount }
    val sold = sales.filter { it.productId == productId && it.createdAt >= since }.sumOf { it.amount }
    val base = opening?.amount ?: 0L
    val lastAdd = mine.filter { it.kind == StockKind.ADD && it.createdAt >= since }.maxOfOrNull { it.createdAt }
    return ProductStock(productId, base + added - deducted - sold, base, added, deducted, sold, opening != null, opening?.createdAt, lastAdd)
}

/**
 * True when this month has no stock set yet for the product: no opening and no stock added this month.
 * Adding stock counts, so the seller is not nagged after putting a balance in either way.
 */
fun needsOpening(stock: ProductStock, now: Long): Boolean {
    val monthStart = DateRanges.startOfMonth(now)
    val openedThisMonth = (stock.openingAt ?: Long.MIN_VALUE) >= monthStart
    val addedThisMonth = (stock.lastAddAt ?: Long.MIN_VALUE) >= monthStart
    return !(openedThisMonth || addedThisMonth)
}

/** Total sold across all sales, and the part sold inside [range]. */
fun totalSales(sales: List<SaleEntity>): Long = sales.sumOf { it.amount }

fun salesInRange(sales: List<SaleEntity>, range: DateRange): List<SaleEntity> = sales.filter { it.createdAt in range }
