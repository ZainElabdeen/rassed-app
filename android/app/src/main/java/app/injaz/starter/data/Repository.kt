package app.injaz.starter.data

import androidx.room.withTransaction
import app.injaz.starter.domain.BackupCodec
import app.injaz.starter.domain.BackupData
import app.injaz.starter.domain.DateRanges
import app.injaz.starter.domain.canDeleteCustomer
import app.injaz.starter.domain.customerBalance
import app.injaz.starter.domain.validateSale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

/** The one place the screens read and write records. Every list comes back as one [AppData] snapshot that updates by itself. */
class Repository(private val db: AppDatabase) {
    private val dao = db.dao()

    val data: Flow<AppData> = combine(
        dao.observeProducts(),
        dao.observeCustomers(),
        dao.observeSales(),
        dao.observePayments(),
        dao.observeStock(),
    ) { products, customers, sales, payments, stock ->
        AppData(products, customers, sales, payments, stock)
    }

    private fun newId(): String = UUID.randomUUID().toString()

    suspend fun addProduct(name: String): String {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "name" }
        val id = newId()
        dao.upsertProduct(ProductEntity(id = id, name = clean, sortOrder = dao.maxSortOrder() + 1, createdAt = System.currentTimeMillis()))
        return id
    }

    /** Soft delete: the row stays, so past sales and the stock log still point at it. */
    suspend fun removeProduct(id: String) = dao.archiveProduct(id)

    suspend fun addCustomer(name: String, phone: String?): CustomerEntity {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "name" }
        val customer = CustomerEntity(newId(), clean, phone?.trim()?.ifEmpty { null }, System.currentTimeMillis())
        dao.upsertCustomer(customer)
        return customer
    }

    suspend fun updateCustomer(id: String, name: String, phone: String?) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "name" }
        val old = dao.allCustomers().firstOrNull { it.id == id } ?: return
        dao.upsertCustomer(old.copy(name = clean, phone = phone?.trim()?.ifEmpty { null }))
    }

    /**
     * Deletes a customer who owes nothing; returns false, changing nothing, when they still owe.
     * Their past sales and payments stay, so stock, totals and reports do not change.
     */
    suspend fun deleteCustomer(id: String): Boolean = db.withTransaction {
        val balance = customerBalance(id, dao.allSales(), dao.allPayments())
        if (canDeleteCustomer(balance)) {
            dao.deleteCustomer(id)
            true
        } else {
            false
        }
    }

    /** Records a sale. [customerId] is required for a pay-later sale. Stock drops by itself: it is computed from the sales. */
    suspend fun addSale(productId: String, amount: Long, customerId: String?, method: String, at: Long = System.currentTimeMillis()) {
        require(validateSale(productId, amount, customerId, method) == null) { "invalid sale" }
        val product = dao.allProducts().firstOrNull { it.id == productId && !it.archived }
        require(product != null) { "product" }
        dao.insertSale(SaleEntity(newId(), product.id, product.name, amount, customerId, at, method))
    }

    suspend fun deleteSale(id: String) = dao.deleteSale(id)

    suspend fun addPayment(customerId: String, amount: Long, paidAt: Long, method: String, note: String?) {
        require(amount > 0 && method in PayMethod.forPayment) { "invalid payment" }
        dao.insertPayment(PaymentEntity(newId(), customerId, amount, paidAt, method, note?.trim()?.ifEmpty { null }))
    }

    /** Sets the month's opening stock. It is dated the first of the month and replaces that month's earlier opening. */
    suspend fun setOpening(productId: String, amount: Long, at: Long = System.currentTimeMillis()) {
        require(amount >= 0) { "amount" }
        val monthStart = DateRanges.startOfMonth(at)
        db.withTransaction {
            dao.deleteOpening(productId, monthStart)
            dao.insertStockEntry(StockEntryEntity(newId(), productId, StockKind.OPENING, amount, "", monthStart))
        }
    }

    /** Adds to or deducts from a product's stock ([kind] is [StockKind.ADD] or [StockKind.DEDUCT]), with a reason. */
    suspend fun adjustStock(productId: String, kind: String, amount: Long, reason: String, at: Long = System.currentTimeMillis()) {
        require(kind == StockKind.ADD || kind == StockKind.DEDUCT) { "kind" }
        require(amount > 0) { "amount" }
        require(reason.isNotBlank()) { "reason" }
        dao.insertStockEntry(StockEntryEntity(newId(), productId, kind, amount, reason.trim(), at))
    }

    /** All data as one backup file's text. */
    suspend fun exportJson(): String {
        val snapshot = db.withTransaction {
            BackupData(dao.allProducts(), dao.allCustomers(), dao.allSales(), dao.allPayments(), dao.allStockEntries())
        }
        return BackupCodec.encode(snapshot, System.currentTimeMillis())
    }

    /** Replaces everything with a backup that already passed validation, in one transaction: a failure leaves the phone's data as it was. */
    suspend fun importBackup(backup: BackupData) {
        db.withTransaction {
            dao.clearSales()
            dao.clearPayments()
            dao.clearStockEntries()
            dao.clearCustomers()
            dao.clearProducts()
            dao.insertProducts(backup.products)
            dao.insertCustomers(backup.customers)
            dao.insertSales(backup.sales)
            dao.insertPayments(backup.payments)
            dao.insertStockEntries(backup.stockEntries)
        }
    }
}
