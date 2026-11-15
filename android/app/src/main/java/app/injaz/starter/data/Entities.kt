package app.injaz.starter.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Payment methods, stored as text so no converter is needed and a backup file stays readable. */
object PayMethod {
    const val CASH = "CASH"
    const val BANKAK = "BANKAK"
    const val CREDIT = "CREDIT"
    val forSale = listOf(CASH, BANKAK, CREDIT)
    val forPayment = listOf(CASH, BANKAK)
}

object StockKind {
    const val OPENING = "OPENING"
    const val ADD = "ADD"
    const val DEDUCT = "DEDUCT"
    val all = listOf(OPENING, ADD, DEDUCT)
}

/** A product the person sells. Removing one only sets [archived], so old sales keep their product. */
@Serializable
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val createdAt: Long,
)

@Serializable
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String? = null,
    val createdAt: Long,
)

/** [productName] is a copy taken at the time of the sale, so a removed product keeps its name in history. */
@Serializable
@Entity(tableName = "sales", indices = [Index("productId"), Index("customerId"), Index("createdAt")])
data class SaleEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val productName: String,
    val amount: Long,
    val customerId: String? = null,
    val createdAt: Long,
    val method: String,
)

@Serializable
@Entity(tableName = "payments", indices = [Index("customerId"), Index("paidAt")])
data class PaymentEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val amount: Long,
    val paidAt: Long,
    val method: String,
    val note: String? = null,
)

@Serializable
@Entity(tableName = "stock_entries", indices = [Index("productId"), Index("createdAt")])
data class StockEntryEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val kind: String,
    val amount: Long,
    val reason: String = "",
    val createdAt: Long,
)

/** Everything the app knows, read as one snapshot. Screens compute their numbers from it with the domain functions. */
data class AppData(
    val products: List<ProductEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val stock: List<StockEntryEntity> = emptyList(),
) {
    /** Products not removed, in display order. */
    val activeProducts: List<ProductEntity> get() = products.filter { !it.archived }
}
