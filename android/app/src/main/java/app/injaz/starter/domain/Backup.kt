package app.injaz.starter.domain

import app.injaz.starter.data.CustomerEntity
import app.injaz.starter.data.PayMethod
import app.injaz.starter.data.PaymentEntity
import app.injaz.starter.data.ProductEntity
import app.injaz.starter.data.SaleEntity
import app.injaz.starter.data.StockEntryEntity
import app.injaz.starter.data.StockKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest

@Serializable
data class BackupData(
    val products: List<ProductEntity>,
    val customers: List<CustomerEntity>,
    val sales: List<SaleEntity>,
    val payments: List<PaymentEntity>,
    val stockEntries: List<StockEntryEntity>,
)

@Serializable
data class BackupCounts(val products: Int, val customers: Int, val sales: Int, val payments: Int, val stockEntries: Int)

@Serializable
data class BackupFile(
    val format: String,
    val schemaVersion: Int,
    val exportedAt: Long,
    val counts: BackupCounts,
    val checksum: String,
    val data: BackupData,
)

enum class BackupError { UNREADABLE, WRONG_FORMAT, NEWER_VERSION, BAD_CHECKSUM, BAD_COUNTS, DUPLICATE_ID, BAD_REFERENCE, BAD_VALUE }

sealed interface BackupResult {
    data class Valid(val file: BackupFile) : BackupResult
    data class Invalid(val error: BackupError) : BackupResult
}

/** Writes and checks the single backup file. Nothing here touches the database: import validates first, then replaces. */
object BackupCodec {
    const val FORMAT = "raseed-backup"
    const val SCHEMA_VERSION = 1

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = false }

    fun countsOf(data: BackupData) = BackupCounts(data.products.size, data.customers.size, data.sales.size, data.payments.size, data.stockEntries.size)

    fun checksum(data: BackupData): String = sha256Hex(json.encodeToString(data))

    fun encode(data: BackupData, exportedAt: Long): String =
        json.encodeToString(BackupFile(FORMAT, SCHEMA_VERSION, exportedAt, countsOf(data), checksum(data), data))

    fun parseAndValidate(text: String): BackupResult {
        // Read only the header first, so a file from a newer app is named as such instead of "unreadable".
        val header = try {
            json.parseToJsonElement(text).jsonObject
        } catch (e: Exception) {
            return BackupResult.Invalid(BackupError.UNREADABLE)
        }
        val format = try { header["format"]?.jsonPrimitive?.content } catch (e: Exception) { null }
        if (format != FORMAT) return BackupResult.Invalid(BackupError.WRONG_FORMAT)
        val version = try { header["schemaVersion"]?.jsonPrimitive?.int } catch (e: Exception) { null }
            ?: return BackupResult.Invalid(BackupError.UNREADABLE)
        if (version > SCHEMA_VERSION) return BackupResult.Invalid(BackupError.NEWER_VERSION)
        if (version < 1) return BackupResult.Invalid(BackupError.WRONG_FORMAT)

        val file = try {
            json.decodeFromString<BackupFile>(text)
        } catch (e: Exception) {
            return BackupResult.Invalid(BackupError.UNREADABLE)
        }
        val error = check(file)
        return if (error == null) BackupResult.Valid(file) else BackupResult.Invalid(error)
    }

    private fun check(file: BackupFile): BackupError? {
        val d = file.data
        if (checksum(d) != file.checksum) return BackupError.BAD_CHECKSUM
        if (countsOf(d) != file.counts) return BackupError.BAD_COUNTS

        if (d.products.map { it.id }.toSet().size != d.products.size) return BackupError.DUPLICATE_ID
        if (d.customers.map { it.id }.toSet().size != d.customers.size) return BackupError.DUPLICATE_ID
        if (d.sales.map { it.id }.toSet().size != d.sales.size) return BackupError.DUPLICATE_ID
        if (d.payments.map { it.id }.toSet().size != d.payments.size) return BackupError.DUPLICATE_ID
        if (d.stockEntries.map { it.id }.toSet().size != d.stockEntries.size) return BackupError.DUPLICATE_ID

        val productIds = d.products.map { it.id }.toSet()

        if (d.products.any { it.id.isBlank() || it.name.isBlank() }) return BackupError.BAD_VALUE
        if (d.customers.any { it.id.isBlank() || it.name.isBlank() }) return BackupError.BAD_VALUE
        for (s in d.sales) {
            if (s.amount < 1 || s.amount > MAX_AMOUNT || s.method !in PayMethod.forSale) return BackupError.BAD_VALUE
            if (s.method == PayMethod.CREDIT && s.customerId == null) return BackupError.BAD_VALUE
            if (s.productId !in productIds) return BackupError.BAD_REFERENCE
            // A customer id may point at a customer who was deleted later: their history stays.
        }
        for (p in d.payments) {
            if (p.amount < 1 || p.amount > MAX_AMOUNT || p.method !in PayMethod.forPayment) return BackupError.BAD_VALUE
        }
        for (e in d.stockEntries) {
            if (e.amount < 0 || e.amount > MAX_AMOUNT || e.kind !in StockKind.all) return BackupError.BAD_VALUE
            if (e.productId !in productIds) return BackupError.BAD_REFERENCE
        }
        return null
    }

    fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
