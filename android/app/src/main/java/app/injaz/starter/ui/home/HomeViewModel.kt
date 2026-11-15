package app.injaz.starter.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.injaz.starter.data.AppData
import app.injaz.starter.data.AppGraph
import app.injaz.starter.data.ProductEntity
import app.injaz.starter.data.SaleEntity
import app.injaz.starter.domain.DateRanges
import app.injaz.starter.domain.ProductStock
import app.injaz.starter.domain.computeStock
import app.injaz.starter.domain.needsOpening
import app.injaz.starter.domain.salesInRange
import app.injaz.starter.domain.totalOutstanding
import app.injaz.starter.domain.totalSales
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Everything the home screen shows, computed from one snapshot of the data. */
data class HomeUiState(
    val monthSales: Long,
    val allSales: Long,
    val owed: Long,
    val products: List<Pair<ProductEntity, ProductStock>>,
    val needsOpeningCount: Int,
    val latest: List<SaleEntity>,
    val customerNames: Map<String, String>,
)

/** A plain function, so the numbers can be tested without a ViewModel. */
fun buildHomeState(data: AppData, now: Long): HomeUiState {
    val products = data.activeProducts.map { it to computeStock(it.id, data.sales, data.stock) }
    return HomeUiState(
        monthSales = totalSales(salesInRange(data.sales, DateRanges.monthRange(now))),
        allSales = totalSales(data.sales),
        owed = totalOutstanding(data.customers, data.sales, data.payments),
        products = products,
        needsOpeningCount = products.count { (_, stock) -> needsOpening(stock, now) },
        latest = data.sales.sortedByDescending { it.createdAt }.take(5),
        customerNames = data.customers.associate { it.id to it.name },
    )
}

class HomeViewModel : ViewModel() {
    private val repo = AppGraph.repository
    private val retryTick = MutableStateFlow(0)
    private val _error = MutableStateFlow(false)

    /** True when reading the data failed; [retry] clears it and reads again. */
    val error: StateFlow<Boolean> = _error

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState?> = retryTick
        .flatMapLatest {
            repo.data
                .map { buildHomeState(it, System.currentTimeMillis()) }
                .catch { _error.value = true }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun retry() {
        _error.value = false
        retryTick.value = retryTick.value + 1
    }
}
