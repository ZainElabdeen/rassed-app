package app.injaz.starter.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.injaz.starter.data.AppData
import app.injaz.starter.data.AppGraph
import app.injaz.starter.data.PayMethod
import app.injaz.starter.domain.parseAmount
import app.injaz.starter.domain.validateSale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The data the form reads from. [failed] means it could not be loaded. */
data class NewSaleData(val app: AppData = AppData(), val failed: Boolean = false)

/** What the person has typed and chosen so far. */
data class NewSaleForm(
    val productId: String? = null,
    val amountText: String = "",
    val method: String = PayMethod.CASH,
    val customerId: String? = null,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: Boolean = false,
    val customerFailed: Boolean = false,
)

/** True when the form can be saved as it is. */
fun canSaveSale(form: NewSaleForm): Boolean =
    validateSale(form.productId, parseAmount(form.amountText), form.customerId, form.method) == null

@OptIn(ExperimentalCoroutinesApi::class)
class NewSaleViewModel : ViewModel() {
    private val repo = AppGraph.repository
    private val retryTick = MutableStateFlow(0)

    /** Null while loading. */
    val data: StateFlow<NewSaleData?> = retryTick
        .flatMapLatest {
            repo.data.map { NewSaleData(it) }.catch { emit(NewSaleData(failed = true)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _form = MutableStateFlow(NewSaleForm())
    val form: StateFlow<NewSaleForm> = _form.asStateFlow()

    fun retry() { retryTick.value = retryTick.value + 1 }

    fun selectProduct(id: String) = _form.update { it.copy(productId = id, saveFailed = false) }

    fun setAmount(text: String) = _form.update { it.copy(amountText = text, saveFailed = false) }

    fun setMethod(method: String) = _form.update { it.copy(method = method, saveFailed = false) }

    fun setCustomer(id: String?) = _form.update { it.copy(customerId = id, saveFailed = false, customerFailed = false) }

    fun save() {
        val f = _form.value
        if (f.saving || !canSaveSale(f)) return
        val amount = parseAmount(f.amountText) ?: return
        val productId = f.productId ?: return
        _form.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                repo.addSale(productId, amount, f.customerId, f.method)
                _form.update { it.copy(saving = false, saved = true) }
            } catch (e: Exception) {
                _form.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }

    /** Adds a customer and selects it; [onDone] runs after it was saved. */
    fun addCustomer(name: String, phone: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val customer = repo.addCustomer(name, phone.ifBlank { null })
                _form.update { it.copy(customerId = customer.id, customerFailed = false) }
                onDone()
            } catch (e: Exception) {
                _form.update { it.copy(customerFailed = true) }
            }
        }
    }
}
