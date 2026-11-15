@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.injaz.starter.ui.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.injaz.starter.R
import app.injaz.starter.data.PayMethod
import app.injaz.starter.domain.TimelineItem
import app.injaz.starter.domain.PaidStatus
import app.injaz.starter.domain.formatDateTime
import app.injaz.starter.ui.common.PillTone
import app.injaz.starter.ui.common.StatusPill
import app.injaz.starter.ui.common.amountText
import app.injaz.starter.ui.common.methodLabel
import app.injaz.starter.ui.common.pillTone
import app.injaz.starter.ui.common.statusLabel
import app.injaz.starter.ui.kit.AppTopBar
import app.injaz.starter.ui.kit.ConfirmDialog
import app.injaz.starter.ui.kit.ErrorState
import app.injaz.starter.ui.kit.InfoCard
import app.injaz.starter.ui.kit.ListRow
import app.injaz.starter.ui.kit.LoadingState
import app.injaz.starter.ui.kit.PrimaryButton
import app.injaz.starter.ui.kit.SectionHeader
import app.injaz.starter.ui.kit.Spacing

/** One customer: what they owe, the credit types they bought, every sale and payment, and the button to record a payment. */
@Composable
fun CustomerDetailScreen(customerId: String, onBack: () -> Unit) {
    val vm: CustomerDetailViewModel = viewModel()
    LaunchedEffect(customerId) { vm.select(customerId) }
    val state by vm.state.collectAsStateWithLifecycle()
    val failed by vm.writeFailed.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val failMessage = stringResource(R.string.cust_save_failed)
    LaunchedEffect(failed) {
        if (failed) {
            snackbar.showSnackbar(failMessage)
            vm.clearFailure()
        }
    }

    var showEdit by remember { mutableStateOf(false) }
    var showPayment by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val current = state

    Scaffold(
        topBar = {
            AppTopBar(
                title = (current as? CustomerDetailUiState.Ready)?.customer?.name ?: stringResource(R.string.cust_detail_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationLabel = stringResource(R.string.action_back),
                onNavigate = onBack,
                actions = {
                    if (current is CustomerDetailUiState.Ready) {
                        IconButton(onClick = { showEdit = true }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cust_delete))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        when (current) {
            null -> LoadingState(modifier = Modifier.padding(inner), label = stringResource(R.string.loading))
            CustomerDetailUiState.Failed -> ErrorState(
                modifier = Modifier.padding(inner),
                title = stringResource(R.string.error_generic_title),
                message = stringResource(R.string.error_generic_message),
                retryLabel = stringResource(R.string.action_retry),
                onRetry = { vm.retry() },
            )
            CustomerDetailUiState.NotFound -> ErrorState(
                modifier = Modifier.padding(inner),
                title = stringResource(R.string.cust_not_found_title),
                message = stringResource(R.string.cust_not_found_message),
                retryLabel = stringResource(R.string.action_back),
                onRetry = onBack,
            )
            is CustomerDetailUiState.Ready -> {
                Column(Modifier.fillMaxSize().padding(inner)) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(top = Spacing.sm, bottom = Spacing.lg),
                    ) {
                        item(key = "balance") {
                            BalanceCard(
                                balance = current.balance,
                                phone = current.customer.phone,
                                modifier = Modifier.padding(horizontal = Spacing.lg),
                            )
                        }
                        item(key = "types-header") {
                            SectionHeader(title = stringResource(R.string.cust_types_title), modifier = Modifier.padding(top = Spacing.md))
                        }
                        if (current.purchases.isEmpty()) {
                            item(key = "types-empty") { QuietLine(stringResource(R.string.cust_types_empty)) }
                        } else {
                            items(current.purchases, key = { "type-" + it.productId }) { total ->
                                ListRow(
                                    title = total.name,
                                    supporting = stringResource(R.string.cust_purchases_count, total.count),
                                    trailing = {
                                        Text(
                                            text = amountText(total.total),
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                    },
                                )
                            }
                        }
                        item(key = "history-header") {
                            SectionHeader(title = stringResource(R.string.cust_history_title), modifier = Modifier.padding(top = Spacing.md))
                        }
                        if (current.timeline.isEmpty()) {
                            item(key = "history-empty") { QuietLine(stringResource(R.string.cust_history_empty)) }
                        } else {
                            items(
                                current.timeline,
                                key = {
                                    when (it) {
                                        is TimelineItem.SaleItem -> "s-" + it.sale.id
                                        is TimelineItem.PaymentItem -> "p-" + it.payment.id
                                    }
                                },
                            ) { item ->
                                when (item) {
                                    is TimelineItem.SaleItem -> SaleHistoryRow(item)
                                    is TimelineItem.PaymentItem -> PaymentHistoryRow(item)
                                }
                            }
                        }
                    }
                    PrimaryButton(
                        text = stringResource(R.string.cust_record_payment),
                        onClick = { showPayment = true },
                        modifier = Modifier.padding(Spacing.lg),
                    )
                }

                if (showEdit) {
                    CustomerFormSheet(
                        initialName = current.customer.name,
                        initialPhone = current.customer.phone.orEmpty(),
                        titleRes = R.string.cust_form_edit_title,
                        onSave = { name, phone ->
                            vm.updateCustomer(current.customer.id, name, phone)
                            showEdit = false
                        },
                        onDismiss = { showEdit = false },
                    )
                }
                if (showPayment) {
                    PaymentSheet(
                        balance = current.balance,
                        onSave = { amount, paidAt, method, note ->
                            vm.addPayment(current.customer.id, amount, paidAt, method, note)
                            showPayment = false
                        },
                        onDismiss = { showPayment = false },
                    )
                }
                if (showDelete) {
                    if (current.balance > 0) {
                        ConfirmDialog(
                            title = stringResource(R.string.cust_delete_blocked_title),
                            message = stringResource(R.string.cust_delete_blocked_message, amountText(current.balance)),
                            confirmLabel = stringResource(R.string.cust_record_payment),
                            dismissLabel = stringResource(R.string.action_close),
                            onConfirm = {
                                showDelete = false
                                showPayment = true
                            },
                            onDismiss = { showDelete = false },
                        )
                    } else {
                        ConfirmDialog(
                            title = stringResource(R.string.cust_delete_title),
                            message = stringResource(R.string.cust_delete_message, current.customer.name),
                            confirmLabel = stringResource(R.string.action_delete),
                            dismissLabel = stringResource(R.string.action_cancel),
                            onConfirm = {
                                showDelete = false
                                vm.deleteCustomer(current.customer.id, onBack)
                            },
                            onDismiss = { showDelete = false },
                            destructive = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: Long, phone: String?, modifier: Modifier = Modifier) {
    InfoCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.cust_balance_label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = amountText(balance),
            style = MaterialTheme.typography.headlineMedium,
            color = if (balance > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        )
        if (!phone.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.cust_phone_fmt, phone),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuietLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}

@Composable
private fun SaleHistoryRow(item: TimelineItem.SaleItem) {
    val sale = item.sale
    val settlement = item.settlement
    val isCredit = sale.method == PayMethod.CREDIT
    val whenAndMethod = stringResource(R.string.cust_meta_fmt, formatDateTime(sale.createdAt), methodLabel(sale.method))
    val supporting = if (isCredit && settlement != null && settlement.status == PaidStatus.PARTIAL) {
        stringResource(R.string.cust_meta_fmt, whenAndMethod, stringResource(R.string.cust_paid_so_far, amountText(settlement.paid)))
    } else {
        whenAndMethod
    }
    ListRow(
        title = stringResource(R.string.cust_sale_title, sale.productName, amountText(sale.amount)),
        supporting = supporting,
        leadingIcon = Icons.Filled.ShoppingCart,
        trailing = if (isCredit && settlement != null) {
            { StatusPill(text = statusLabel(settlement.status), tone = settlement.status.pillTone()) }
        } else {
            null
        },
    )
}

@Composable
private fun PaymentHistoryRow(item: TimelineItem.PaymentItem) {
    val payment = item.payment
    val whenAndMethod = stringResource(R.string.cust_meta_fmt, formatDateTime(payment.paidAt), methodLabel(payment.method))
    val supporting = if (!payment.note.isNullOrBlank()) {
        stringResource(R.string.cust_meta_fmt, whenAndMethod, payment.note)
    } else {
        whenAndMethod
    }
    ListRow(
        title = stringResource(R.string.cust_payment_title, amountText(payment.amount)),
        supporting = supporting,
        leadingIcon = Icons.Filled.Check,
        trailing = { StatusPill(text = stringResource(R.string.cust_received), tone = PillTone.Good) },
    )
}
