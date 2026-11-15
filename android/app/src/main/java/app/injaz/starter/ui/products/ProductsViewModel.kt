package app.injaz.starter.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.injaz.starter.data.AppData
import app.injaz.starter.data.AppGraph
import app.injaz.starter.data.ProductEntity
import app.injaz.starter.data.StockEntryEntity
import app.injaz.starter.domain.ProductStock
import app.injaz.starter.domain.computeStock
import app.injaz.starter.domain.needsOpening
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One product on the products screen: its stock now, and whether this month still lacks an opening. */
data class ProductRow(val product: ProductEntity, val stock: ProductStock, val needsOpening: Boolean)

sealed interface ProductsUi {
    data class Content(val rows: List<ProductRow>) : ProductsUi
    data object Failed : ProductsUi
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsViewModel : ViewModel() {
    private val repo = AppGraph.repository
    private val tick = MutableStateFlow(0)
    private val _writeFailed = MutableStateFlow(false)

    /** True after a write failed, until [clearFailed]. */
    val writeFailed: StateFlow<Boolean> = _writeFailed

    /** Null while loading. */
    val state: StateFlow<ProductsUi?> = tick.flatMapLatest {
        repo.data
            .map<AppData, ProductsUi> { data ->
                val now = System.currentTimeMillis()
                ProductsUi.Content(
                    data.activeProducts.sortedBy { it.sortOrder }.map { p ->
                        val stock = computeStock(p.id, data.sales, data.stock)
                        ProductRow(p, stock, needsOpening(stock, now))
                    },
                )
            }
            .catch { emit(ProductsUi.Failed) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun retry() {
        tick.value = tick.value + 1
    }

    fun clearFailed() {
        _writeFailed.value = false
    }

    fun addProduct(name: String) = write { repo.addProduct(name) }

    fun removeProduct(id: String) = write { repo.removeProduct(id) }

    fun setOpening(productId: String, amount: Long) = write { repo.setOpening(productId, amount) }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _writeFailed.value = true
            }
        }
    }
}

sealed interface StockLogUi {
    data class Content(
        val product: ProductEntity,
        val stock: ProductStock,
        val entries: List<StockEntryEntity>,
    ) : StockLogUi

    data object NotFound : StockLogUi
    data object Failed : StockLogUi
}

@OptIn(ExperimentalCoroutinesApi::class)
class StockLogViewModel : ViewModel() {
    private val repo = AppGraph.repository
    private val selected = MutableStateFlow<String?>(null)
    private val tick = MutableStateFlow(0)
    private val _writeFailed = MutableStateFlow(false)

    val writeFailed: StateFlow<Boolean> = _writeFailed

    /** Null while loading (or before [select] is called). */
    val state: StateFlow<StockLogUi?> = combine(selected, tick) { id, _ -> id }
        .flatMapLatest { id ->
            if (id == null) {
                flowOf<StockLogUi?>(null)
            } else {
                repo.data
                    .map<AppData, StockLogUi?> { data ->
                        val product = data.products.firstOrNull { it.id == id && !it.archived }
                        if (product == null) {
                            StockLogUi.NotFound
                        } else {
                            StockLogUi.Content(
                                product = product,
                                stock = computeStock(id, data.sales, data.stock),
                                entries = data.stock.filter { it.productId == id }.sortedByDescending { it.createdAt },
                            )
                        }
                    }
                    .catch { emit(StockLogUi.Failed) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun select(id: String) {
        selected.value = id
    }

    fun retry() {
        tick.value = tick.value + 1
    }

    fun clearFailed() {
        _writeFailed.value = false
    }

    fun adjust(kind: String, amount: Long, reason: String) {
        val id = selected.value ?: return
        write { repo.adjustStock(id, kind, amount, reason) }
    }

    fun setOpening(amount: Long) {
        val id = selected.value ?: return
        write { repo.setOpening(id, amount) }
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                _writeFailed.value = true
            }
        }
    }
}
