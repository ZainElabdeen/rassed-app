package app.injaz.starter.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.injaz.starter.R
import app.injaz.starter.data.AppData
import app.injaz.starter.data.StockKind
import app.injaz.starter.domain.SearchResults
import app.injaz.starter.domain.customerBalance
import app.injaz.starter.domain.formatDateTime
import app.injaz.starter.domain.searchAll
import app.injaz.starter.ui.common.amountText
import app.injaz.starter.ui.common.methodLabel
import app.injaz.starter.ui.kit.AppTopBar
import app.injaz.starter.ui.kit.EmptyState
import app.injaz.starter.ui.kit.ErrorState
import app.injaz.starter.ui.kit.ListRow
import app.injaz.starter.ui.kit.LoadingState
import app.injaz.starter.ui.kit.SectionHeader
import app.injaz.starter.ui.kit.Spacing
import app.injaz.starter.ui.sales.saleMethodLabels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Searches everything the app holds: customers, products, sales, payments and the stock log.
 * A tap opens the customer a record belongs to, or the product's stock log.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenCustomer: (String) -> Unit,
    onOpenStock: (String) -> Unit,
) {
    val vm: SearchViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    val labels = saleMethodLabels()
    val current = state

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.search_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationLabel = stringResource(R.string.action_back),
                onNavigate = onBack,
            )
        },
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.search_clear))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                    .focusRequester(focus),
            )
            when (current) {
                null -> LoadingState(label = stringResource(R.string.loading))
                SearchUiState.Failed -> ErrorState(
                    title = stringResource(R.string.error_generic_title),
                    message = stringResource(R.string.error_generic_message),
                    retryLabel = stringResource(R.string.action_retry),
                    onRetry = vm::retry,
                )
                is SearchUiState.Ready -> {
                    if (query.isBlank()) {
                        EmptyState(
                            icon = Icons.Filled.Search,
                            title = stringResource(R.string.search_idle_title),
                            message = stringResource(R.string.search_idle_message),
                        )
                    } else {
                        val results by produceState<SearchResults?>(null, current.data, query, labels) {
                            value = withContext(Dispatchers.Default) { searchAll(query, current.data, labels) }
                        }
                        val found = results
                        when {
                            found == null -> Unit
                            found.isEmpty -> EmptyState(
                                icon = Icons.Filled.Search,
                                title = stringResource(R.string.search_none_title),
                                message = stringResource(R.string.search_none_message, query.trim()),
                            )
                            else -> ResultsList(
                                data = current.data,
                                results = found,
                                onOpenCustomer = onOpenCustomer,
                                onOpenStock = onOpenStock,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsList(
    data: AppData,
    results: SearchResults,
    onOpenCustomer: (String) -> Unit,
    onOpenStock: (String) -> Unit,
) {
    val customerNames = remember(data.customers) { data.customers.associate { it.id to it.name } }
    val productNames = remember(data.products) { data.products.associate { it.id to it.name } }
    val activeProductIds = remember(data.products) { data.activeProducts.map { it.id }.toSet() }
    val deletedLabel = stringResource(R.string.deleted_customer)
    val openingLabel = stringResource(R.string.search_stock_opening)
    val addLabel = stringResource(R.string.search_stock_add)
    val deductLabel = stringResource(R.string.search_stock_deduct)

    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.xl)) {
        if (results.customers.isNotEmpty()) {
            item(key = "h-customers") {
                SectionHeader(title = stringResource(R.string.search_section_customers, results.customers.size))
            }
            items(results.customers, key = { "c-" + it.id }) { customer ->
                val balance = customerBalance(customer.id, data.sales, data.payments)
                ListRow(
                    title = customer.name,
                    supporting = customer.phone,
                    leadingIcon = Icons.Filled.Person,
                    onClick = { onOpenCustomer(customer.id) },
                    trailing = if (balance != 0L) {
                        {
                            Text(
                                text = amountText(balance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (balance > 0) FontWeight.Bold else null,
                                color = if (balance > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
        if (results.products.isNotEmpty()) {
            item(key = "h-products") {
                SectionHeader(title = stringResource(R.string.search_section_products, results.products.size))
            }
            items(results.products, key = { "pr-" + it.id }) { product ->
                ListRow(
                    title = product.name,
                    supporting = stringResource(R.string.search_product_hint),
                    leadingIcon = Icons.AutoMirrored.Filled.List,
                    onClick = { onOpenStock(product.id) },
                )
            }
        }
        if (results.sales.isNotEmpty()) {
            item(key = "h-sales") {
                SectionHeader(title = stringResource(R.string.search_section_sales, results.sales.size))
            }
            items(results.sales, key = { "s-" + it.id }) { sale ->
                val customerId = sale.customerId
                val known = customerId != null && customerId in customerNames
                val who = customerId?.let { customerNames[it] ?: deletedLabel }
                ListRow(
                    title = stringResource(R.string.cust_sale_title, sale.productName, amountText(sale.amount)),
                    supporting = listOfNotNull(formatDateTime(sale.createdAt), who, methodLabel(sale.method)).joinToString(" · "),
                    leadingIcon = Icons.Filled.ShoppingCart,
                    onClick = if (known) ({ onOpenCustomer(customerId!!) }) else null,
                )
            }
        }
        if (results.payments.isNotEmpty()) {
            item(key = "h-payments") {
                SectionHeader(title = stringResource(R.string.search_section_payments, results.payments.size))
            }
            items(results.payments, key = { "p-" + it.id }) { payment ->
                val known = payment.customerId in customerNames
                val who = customerNames[payment.customerId] ?: deletedLabel
                ListRow(
                    title = stringResource(R.string.cust_payment_title, amountText(payment.amount)),
                    supporting = listOfNotNull(formatDateTime(payment.paidAt), who, methodLabel(payment.method), payment.note?.takeIf { it.isNotBlank() })
                        .joinToString(" · "),
                    leadingIcon = Icons.Filled.Check,
                    onClick = if (known) ({ onOpenCustomer(payment.customerId) }) else null,
                )
            }
        }
        if (results.stock.isNotEmpty()) {
            item(key = "h-stock") {
                SectionHeader(title = stringResource(R.string.search_section_stock, results.stock.size))
            }
            items(results.stock, key = { "e-" + it.id }) { entry ->
                val kind = when (entry.kind) {
                    StockKind.OPENING -> openingLabel
                    StockKind.ADD -> addLabel
                    else -> deductLabel
                }
                ListRow(
                    title = stringResource(R.string.search_stock_title, productNames[entry.productId].orEmpty(), kind, amountText(entry.amount)),
                    supporting = listOfNotNull(formatDateTime(entry.createdAt), entry.reason.takeIf { it.isNotBlank() }).joinToString(" · "),
                    leadingIcon = Icons.AutoMirrored.Filled.List,
                    onClick = if (entry.productId in activeProductIds) ({ onOpenStock(entry.productId) }) else null,
                )
            }
        }
    }
}
