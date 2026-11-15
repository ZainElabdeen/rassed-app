@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.injaz.starter.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.injaz.starter.R
import app.injaz.starter.data.PayMethod
import app.injaz.starter.domain.filterSales
import app.injaz.starter.domain.formatDateTime
import app.injaz.starter.ui.common.StatusPill
import app.injaz.starter.ui.common.amountText
import app.injaz.starter.ui.common.methodLabel
import app.injaz.starter.ui.common.pillTone
import app.injaz.starter.ui.common.statusLabel
import app.injaz.starter.ui.kit.AppTopBar
import app.injaz.starter.ui.kit.ChipRow
import app.injaz.starter.ui.kit.ConfirmDialog
import app.injaz.starter.ui.kit.EmptyState
import app.injaz.starter.ui.kit.ErrorState
import app.injaz.starter.ui.kit.ListRow
import app.injaz.starter.ui.kit.LoadingState
import app.injaz.starter.ui.kit.Spacing

/** Labels of the three sale payment methods, resolved in the current language. Shared by the sales screens. */
@Composable
internal fun saleMethodLabels(): Map<String, String> = mapOf(
    PayMethod.CASH to methodLabel(PayMethod.CASH),
    PayMethod.BANKAK to methodLabel(PayMethod.BANKAK),
    PayMethod.CREDIT to methodLabel(PayMethod.CREDIT),
)

@Composable
fun SalesScreen(onNewSale: () -> Unit, onOpenSearch: () -> Unit, viewModel: SalesViewModel = viewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val productFilter by viewModel.productFilter.collectAsStateWithLifecycle()
    val methodFilter by viewModel.methodFilter.collectAsStateWithLifecycle()
    val actionFailed by viewModel.actionFailed.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val failedText = stringResource(R.string.sales_delete_failed)
    LaunchedEffect(actionFailed) {
        if (actionFailed) {
            snackbar.showSnackbar(failedText)
            viewModel.clearActionFailed()
        }
    }

    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

    val current = ui
    val hasSales = current != null && !current.failed && current.all.isNotEmpty()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.sales_title),
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_open))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (hasSales) {
                ExtendedFloatingActionButton(
                    onClick = onNewSale,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.sales_new)) },
                )
            }
        },
    ) { inner ->
        when {
            current == null -> LoadingState(modifier = Modifier.padding(inner))
            current.failed -> ErrorState(
                title = stringResource(R.string.error_generic_title),
                message = stringResource(R.string.error_generic_message),
                modifier = Modifier.padding(inner),
                retryLabel = stringResource(R.string.action_retry),
                onRetry = viewModel::retry,
            )
            current.all.isEmpty() -> EmptyState(
                title = stringResource(R.string.sales_empty_title),
                message = stringResource(R.string.sales_empty_message),
                modifier = Modifier.padding(inner),
                icon = Icons.Filled.ShoppingCart,
                actionLabel = stringResource(R.string.sales_new),
                onAction = onNewSale,
            )
            else -> {
                val allLabel = stringResource(R.string.sales_filter_all)
                val methodLabels = saleMethodLabels()
                // A filter whose product has no sales any more falls back to "all".
                val effectiveProduct = if (productFilter in current.productIds) productFilter else ""
                val productOptions = listOf("") + current.productIds
                val methodOptions = listOf("") + PayMethod.forSale
                val filtered = filterSales(
                    current.all,
                    effectiveProduct.ifEmpty { null },
                    methodFilter.ifEmpty { null },
                )
                val total = filtered.sumOf { it.amount }

                Column(modifier = Modifier.padding(inner)) {
                    ChipRow(
                        options = productOptions,
                        selected = effectiveProduct,
                        onSelect = viewModel::setProductFilter,
                        label = { id -> if (id.isEmpty()) allLabel else current.productNames[id].orEmpty() },
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                    ChipRow(
                        options = methodOptions,
                        selected = methodFilter,
                        onSelect = viewModel::setMethodFilter,
                        label = { m -> if (m.isEmpty()) allLabel else methodLabels[m].orEmpty() },
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                    Text(
                        text = stringResource(R.string.sales_summary, filtered.size.toString(), amountText(total)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    )
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(Spacing.xl), contentAlignment = Alignment.TopCenter) {
                            Text(
                                text = stringResource(R.string.sales_filter_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 96.dp),
                        ) {
                            items(filtered, key = { it.id }) { sale ->
                                val settlement = current.settlements[sale.id]
                                val deletedLabel = stringResource(R.string.deleted_customer)
                                val customerName = sale.customerId?.let { current.customerNames[it] ?: deletedLabel }
                                val method = methodLabel(sale.method)
                                val supporting = listOfNotNull(formatDateTime(sale.createdAt), customerName, method)
                                    .joinToString(" · ")
                                ListRow(
                                    title = "${sale.productName} — ${amountText(sale.amount)}",
                                    supporting = supporting,
                                    trailing = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                                        ) {
                                            if (sale.method == PayMethod.CREDIT && settlement != null) {
                                                StatusPill(
                                                    text = statusLabel(settlement.status),
                                                    tone = settlement.status.pillTone(),
                                                )
                                            }
                                            IconButton(onClick = { pendingDelete = sale.id }) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = stringResource(R.string.sales_delete_desc),
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { id ->
        ConfirmDialog(
            title = stringResource(R.string.sales_delete_title),
            message = stringResource(R.string.sales_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                viewModel.deleteSale(id)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
            destructive = true,
        )
    }
}
