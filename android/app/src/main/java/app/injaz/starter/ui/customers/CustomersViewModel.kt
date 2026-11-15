package app.injaz.starter.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.injaz.starter.data.AppData
import app.injaz.starter.data.AppGraph
import app.injaz.starter.data.CustomerEntity
import app.injaz.starter.domain.ProductTotal
import app.injaz.starter.domain.TimelineItem
import app.injaz.starter.domain.customerBalance
import app.injaz.starter.domain.customerTimeline
import app.injaz.starter.domain.purchasesByProduct
import app.injaz.starter.domain.totalOutstanding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CustomerRow(val customer: CustomerEntity, val balance: Long)

sealed interface CustomersUiState {
    data class Ready(val rows: List<CustomerRow>, val totalOwed: Long) : CustomersUiState
    data object Failed : CustomersUiState
}

/** Serves the customers list: each customer with what they owe, and the total owed. null state = still loading. */
@OptIn(ExperimentalCoroutinesApi::class)
class CustomersViewModel : ViewModel() {
    private val repo = AppGraph.repository
    private val retryTick = MutableStateFlow(0)

    val state: StateFlow<CustomersUiState?> = retryTick
        .flatMapLatest {
            repo.data
                .map<AppData, CustomersUiState> { data ->
                    val rows = data.customers
                        .map { CustomerRow(it, customerBalance(it.id, data.sales, data.payments)) }
                        .sortedWith(compareByDescending<CustomerRow> { it.balance > 0 }.thenBy { it.customer.name })
                    CustomersUiState.Ready(rows, totalOutstanding(data.customers, data.sales, data.payments))
                }
                .catch { emit(CustomersUiState.Failed) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _writeFailed = MutableStateFlow(false)
    val writeFailed: StateFlow<Boolean> = _writeFailed

    fun retry() {
        retryTick.value = retryTick.value + 1
    }

    fun clearFailure() {
        _writeFailed.value = false
    }

    fun addCustomer(name: String, phone: String) {
        viewModelScope.launch {
            try {
                repo.addCustomer(name, phone.ifBlank { null })
            } catch (e: Exception) {
                _writeFailed.value = true
            }
        }
    }
}

sealed interface CustomerDetailUiState {
    data class Ready(
        val customer: CustomerEntity,
        val balance: Long,
        val purchases: List<ProductTotal>,
        val timeline: List<TimelineItem>,
    ) : CustomerDetailUiState

    data object NotFound : CustomerDetailUiState
    data object Failed : CustomerDetailUiState
}

/** Serves one customer's page. Call [select] with the customer id; state stays null (loading) until then. */
@OptIn(ExperimentalCoroutinesApi::class)
class CustomerDetailViewModel : ViewModel() {
    private val repo = AppGraph.repository
    private val selected = MutableStateFlow<String?>(null)
    private val retryTick = MutableStateFlow(0)

    val state: StateFlow<CustomerDetailUiState?> = combine(selected.filterNotNull(), retryTick) { id, _ -> id }
        .flatMapLatest { id ->
            repo.data
                .map<AppData, CustomerDetailUiState> { data -> build(id, data) }
                .catch { emit(CustomerDetailUiState.Failed) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _writeFailed = MutableStateFlow(false)
    val writeFailed: StateFlow<Boolean> = _writeFailed

    fun select(id: String) {
        selected.value = id
    }

    fun retry() {
        retryTick.value = retryTick.value + 1
    }

    fun clearFailure() {
        _writeFailed.value = false
    }

    private fun build(id: String, data: AppData): CustomerDetailUiState {
        val customer = data.customers.firstOrNull { it.id == id } ?: return CustomerDetailUiState.NotFound
        return CustomerDetailUiState.Ready(
            customer = customer,
            balance = customerBalance(id, data.sales, data.payments),
            purchases = purchasesByProduct(id, data.sales),
            timeline = customerTimeline(id, data.sales, data.payments),
        )
    }

    /** Deletes the customer if they owe nothing, then calls [onDone]; otherwise shows the failure note. */
    fun deleteCustomer(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                if (repo.deleteCustomer(id)) onDone() else _writeFailed.value = true
            } catch (e: Exception) {
                _writeFailed.value = true
            }
        }
    }

    fun updateCustomer(id: String, name: String, phone: String) {
        viewModelScope.launch {
            try {
                repo.updateCustomer(id, name, phone.ifBlank { null })
            } catch (e: Exception) {
                _writeFailed.value = true
            }
        }
    }

    fun addPayment(customerId: String, amount: Long, paidAt: Long, method: String, note: String) {
        viewModelScope.launch {
            try {
                repo.addPayment(customerId, amount, paidAt, method, note.ifBlank { null })
            } catch (e: Exception) {
                _writeFailed.value = true
            }
        }
    }
}
