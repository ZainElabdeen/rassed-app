package app.injaz.starter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM products ORDER BY sortOrder, createdAt")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE")
    fun observeCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun observeSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM payments ORDER BY paidAt DESC")
    fun observePayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM stock_entries ORDER BY createdAt DESC")
    fun observeStock(): Flow<List<StockEntryEntity>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM products")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockEntry(entry: StockEntryEntity)

    @Query("UPDATE products SET archived = 1 WHERE id = :id")
    suspend fun archiveProduct(id: String)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: String)

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteSale(id: String)

    @Query("DELETE FROM stock_entries WHERE productId = :productId AND kind = 'OPENING' AND createdAt = :at")
    suspend fun deleteOpening(productId: String, at: Long)

    // Whole-table access for backup export and import.
    @Query("SELECT * FROM products")
    suspend fun allProducts(): List<ProductEntity>

    @Query("SELECT * FROM customers")
    suspend fun allCustomers(): List<CustomerEntity>

    @Query("SELECT * FROM sales")
    suspend fun allSales(): List<SaleEntity>

    @Query("SELECT * FROM payments")
    suspend fun allPayments(): List<PaymentEntity>

    @Query("SELECT * FROM stock_entries")
    suspend fun allStockEntries(): List<StockEntryEntity>

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM customers")
    suspend fun clearCustomers()

    @Query("DELETE FROM sales")
    suspend fun clearSales()

    @Query("DELETE FROM payments")
    suspend fun clearPayments()

    @Query("DELETE FROM stock_entries")
    suspend fun clearStockEntries()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(items: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(items: List<CustomerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(items: List<SaleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(items: List<PaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockEntries(items: List<StockEntryEntity>)
}
