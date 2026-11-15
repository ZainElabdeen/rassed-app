@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.injaz.starter.ui.customers

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.injaz.starter.R
import app.injaz.starter.ui.common.amountText
import app.injaz.starter.ui.kit.AppTopBar
import app.injaz.starter.ui.kit.EmptyState
import app.injaz.starter.ui.kit.ErrorState
import app.injaz.starter.ui.kit.InfoCard
import app.injaz.starter.ui.kit.ListRow
import app.injaz.starter.ui.kit.LoadingState
import app.injaz.starter.ui.kit.Spacing

/** The customers tab: the total owed, then every customer with their balance. The add button opens a sheet. */
@Composable
fun CustomersScreen(onOpenCustomer: (String) -> Unit, onOpenSearch: () -> Unit) {
    val vm: CustomersViewModel = viewModel()
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
    var showAdd by remember { mutableStateOf(false) }
    val current = state

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.cust_title),
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_open))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (current is CustomersUiState.Ready && current.rows.isNotEmpty()) {
                FloatingActionButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cust_add))
                }
            }
        },
    ) { inner ->
        when (current) {
            null -> LoadingState(modifier = Modifier.padding(inner), label = stringResource(R.string.loading))
            CustomersUiState.Failed -> ErrorState(
                modifier = Modifier.padding(inner),
                title = stringResource(R.string.error_generic_title),
                message = stringResource(R.string.error_generic_message),
                retryLabel = stringResource(R.string.action_retry),
                onRetry = { vm.retry() },
            )
            is CustomersUiState.Ready -> {
                if (current.rows.isEmpty()) {
                    EmptyState(
                        modifier = Modifier.padding(inner),
                        icon = Icons.Filled.Person,
                        title = stringResource(R.string.cust_empty_title),
                        message = stringResource(R.string.cust_empty_message),
                        actionLabel = stringResource(R.string.cust_add),
                        onAction = { showAdd = true },
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(inner),
                        contentPadding = PaddingValues(top = Spacing.sm, bottom = 88.dp),
                    ) {
                        item(key = "total") {
                            InfoCard(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
                                Text(
                                    text = stringResource(R.string.cust_total_owed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = amountText(current.totalOwed),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (current.totalOwed > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        items(current.rows, key = { it.customer.id }) { row ->
                            ListRow(
                                title = row.customer.name,
                                supporting = row.customer.phone,
                                leadingIcon = Icons.Filled.Person,
                                onClick = { onOpenCustomer(row.customer.id) },
                                trailing = { BalanceLabel(row.balance) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        CustomerFormSheet(
            initialName = "",
            initialPhone = "",
            titleRes = R.string.cust_form_add_title,
            onSave = { name, phone ->
                vm.addCustomer(name, phone)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }
}

/** Owed: bold amber amount. Settled: a quiet note. Paid ahead (negative): the amount in the primary green. */
@Composable
private fun BalanceLabel(balance: Long) {
    when {
        balance > 0 -> Text(
            text = amountText(balance),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
        )
        balance < 0 -> Text(
            text = amountText(balance),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        else -> Text(
            text = stringResource(R.string.cust_no_balance),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
