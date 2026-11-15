@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.injaz.starter.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.injaz.starter.R
import app.injaz.starter.data.SaleEntity
import app.injaz.starter.domain.formatDateTime
import app.injaz.starter.ui.common.ProductBalanceCard
import app.injaz.starter.ui.common.amountText
import app.injaz.starter.ui.common.methodLabel
import app.injaz.starter.ui.kit.AppTopBar
import app.injaz.starter.ui.kit.EmptyState
import app.injaz.starter.ui.kit.ErrorState
import app.injaz.starter.ui.kit.ListRow
import app.injaz.starter.ui.kit.LoadingState
import app.injaz.starter.ui.kit.PrimaryButton
import app.injaz.starter.ui.kit.SectionHeader
import app.injaz.starter.ui.kit.Spacing
import app.injaz.starter.ui.kit.StatTile

/** The first screen: this month's numbers, what customers owe, each product's remaining credit and the big "New sale" button. */
@Composable
fun HomeScreen(
    onNewSale: () -> Unit,
    onOpenProducts: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenSales: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    val vm: HomeViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val failed by vm.error.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_open))
                    }
                },
            )
        },
    ) { inner ->
        val current = state
        when {
            failed -> Box(Modifier.padding(inner).fillMaxSize()) {
                ErrorState(
                    title = stringResource(R.string.error_generic_title),
                    message = stringResource(R.string.error_generic_message),
                    retryLabel = stringResource(R.string.action_retry),
                    onRetry = vm::retry,
                )
            }
            current == null -> Box(Modifier.padding(inner).fillMaxSize()) {
                LoadingState(label = stringResource(R.string.loading))
            }
            else -> Column(Modifier.padding(inner).fillMaxSize()) {
                HomeContent(
                    ui = current,
                    onOpenProducts = onOpenProducts,
                    onOpenCustomers = onOpenCustomers,
                    onOpenSales = onOpenSales,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = stringResource(R.string.home_new_sale),
                    onClick = onNewSale,
                    modifier = Modifier.padding(Spacing.lg),
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    ui: HomeUiState,
    onOpenProducts: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenSales: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item(key = "stats") {
            Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    StatTile(
                        label = stringResource(R.string.home_sales_month),
                        value = amountText(ui.monthSales),
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.large)
                            .clickable(role = Role.Button, onClick = onOpenCustomers),
                    ) {
                        StatTile(
                            label = stringResource(R.string.home_owed),
                            value = amountText(ui.owed),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.home_all_time, amountText(ui.allSales)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (ui.needsOpeningCount > 0) {
            item(key = "notice") {
                Card(
                    modifier = Modifier.padding(horizontal = Spacing.lg).fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null)
                        Text(
                            text = stringResource(R.string.home_opening_notice, ui.needsOpeningCount),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f).padding(horizontal = Spacing.md),
                        )
                        TextButton(onClick = onOpenProducts) { Text(stringResource(R.string.home_opening_action)) }
                    }
                }
            }
        }

        item(key = "balance_header") { SectionHeader(title = stringResource(R.string.home_balance_title)) }

        if (ui.products.isEmpty()) {
            item(key = "no_products") {
                Box(Modifier.fillMaxWidth().height(300.dp)) {
                    EmptyState(
                        title = stringResource(R.string.home_empty_title),
                        message = stringResource(R.string.home_empty_message),
                        icon = Icons.Filled.Home,
                        actionLabel = stringResource(R.string.home_empty_action),
                        onAction = onOpenProducts,
                    )
                }
            }
        } else {
            items(ui.products, key = { it.first.id }) { (product, stock) ->
                ProductBalanceCard(
                    name = product.name,
                    stock = stock,
                    onClick = onOpenProducts,
                    modifier = Modifier.padding(horizontal = Spacing.lg).fillMaxWidth(),
                )
            }
        }

        if (ui.latest.isNotEmpty()) {
            item(key = "latest_header") {
                SectionHeader(
                    title = stringResource(R.string.home_latest_title),
                    actionLabel = stringResource(R.string.home_see_all),
                    onAction = onOpenSales,
                )
            }
            items(ui.latest, key = { it.id }) { sale ->
                LatestSaleRow(sale = sale, customerName = sale.customerId?.let { ui.customerNames[it] })
            }
        }

        item(key = "end_space") { Box(Modifier.height(Spacing.sm)) }
    }
}

@Composable
private fun LatestSaleRow(sale: SaleEntity, customerName: String?) {
    val who = customerName ?: methodLabel(sale.method)
    ListRow(
        title = stringResource(R.string.home_sale_title, sale.productName, amountText(sale.amount)),
        supporting = stringResource(R.string.home_sale_supporting, formatDateTime(sale.createdAt), who),
        leadingIcon = Icons.Filled.ShoppingCart,
    )
}
