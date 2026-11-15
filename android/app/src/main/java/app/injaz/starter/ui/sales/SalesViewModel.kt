package app.injaz.starter.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.injaz.starter.data.AppData
import app.injaz.starter.data.AppGraph
import app.injaz.starter.data.SaleEntity
import app.injaz.starter.domain.Settlement
import app.injaz.starter.domain.settleCreditSales
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the sales list shows. [failed] means the data could not be read. */
data class SalesUi(
    val all: List<SaleEntity> = emptyList(),
    val customerNames: Map<String, String> = emptyMap(),
    val settlements: Map<String, Settlement> = emptyMap(),
    /** Product ids found in the sales (removed products included), by name. */
    val productIds: List<String> = emptyList(),
    val productNames: Map<String, String> = emptyMap(),
    val failed: Boolean = false,
)

internal fun buildSalesUi(data: AppData): SalesUi {
    val sales = data.sales.sortedByDescending { it.createdAt }
    val names = LinkedHashMap<String, String>()
    // The newest sale of a product decides the name shown on its chip.
    sales.forEach { if (!names.containsKey(it.productId)) names[it.productId] = it.productName }
    return SalesUi(
        all = sales,
        customerNames = data.customers.associate { it.id to it.name },
        settlements = settleCreditSales(data.sales, data.payments),
        productIds = names.keys.sortedBy { names[it].orEmpty() },
        productNames = names,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class SalesViewModel : ViewModel() {
    private val repo = AppGraph.repository
    private val retryTick = MutableStateFlow(0)

    /** Null while loading. */
    val state: StateFlow<SalesUi?> = retryTick
        .flatMapLatest {
            repo.data.map { buildSalesUi(it) }.catch { emit(SalesUi(failed = true)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** "" means all. */
    private val _productFilter = MutableStateFlow("")
    val productFilter: StateFlow<String> = _productFilter.asStateFlow()

    private val _methodFilter = MutableStateFlow("")
    val methodFilter: StateFlow<String> = _methodFilter.asStateFlow()

    private val _actionFailed = MutableStateFlow(false)
    val actionFailed: StateFlow<Boolean> = _actionFailed.asStateFlow()

    fun setProductFilter(id: String) { _productFilter.value = id }

    fun setMethodFilter(method: String) { _methodFilter.value = method }

    fun retry() { retryTick.value = retryTick.value + 1 }

    fun clearActionFailed() { _actionFailed.value = false }

    fun deleteSale(id: String) {
        viewModelScope.launch {
            try {
                repo.deleteSale(id)
            } catch (e: Exception) {
                _actionFailed.value = true
            }
        }
    }
}
