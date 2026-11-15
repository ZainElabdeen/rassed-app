@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.injaz.starter.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.injaz.starter.R
import app.injaz.starter.data.StockEntryEntity
import app.injaz.starter.data.StockKind
import app.injaz.starter.domain.formatDateTime
import app.injaz.starter.ui.common.ProductBalanceCard
import app.injaz.starter.ui.common.amountText
import app.injaz.starter.ui.kit.AppTopBar
import app.injaz.starter.ui.kit.EmptyState
import app.injaz.starter.ui.kit.ErrorState
import app.injaz.starter.ui.kit.ListRow
import app.injaz.starter.ui.kit.LoadingState
import app.injaz.starter.ui.kit.SectionHeader
import app.injaz.starter.ui.kit.Spacing

@Composable
fun StockLogScreen(
    productId: String,
    onBack: () -> Unit,
    vm: StockLogViewModel = viewModel(),
) {
    LaunchedEffect(productId) { vm.select(productId) }

    val ui by vm.state.collectAsStateWithLifecycle()
    val failed by vm.writeFailed.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val failedMessage = stringResource(R.string.prod_save_failed)
    var sheet by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(failed) {
        if (failed) {
            snackbar.showSnackbar(failedMessage)
            vm.clearFailed()
        }
    }

    val current = ui
    val title = (current as? StockLogUi.Content)?.product?.name ?: stringResource(R.string.prod_title)

    Scaffold(
        topBar = {
            AppTopBar(
                title = title,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationLabel = stringResource(R.string.action_back),
                onNavigate = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        androidx.compose.foundation.layout.Box(Modifier.padding(inner)) {
            when (current) {
                null -> LoadingState(label = stringResource(R.string.loading))
                is StockLogUi.Failed -> ErrorState(
                    title = stringResource(R.string.error_generic_title),
                    message = stringResource(R.string.error_generic_message),
                    retryLabel = stringResource(R.string.action_retry),
                    onRetry = vm::retry,
                )
                is StockLogUi.NotFound -> EmptyState(
                    title = stringResource(R.string.prod_not_found_title),
                    message = stringResource(R.string.prod_not_found_message),
                    icon = Icons.Filled.Warning,
                    actionLabel = stringResource(R.string.action_back),
                    onAction = onBack,
                )
                is StockLogUi.Content -> {
                    LazyColumn(
                        contentPadding = PaddingValues(top = Spacing.md, bottom = Spacing.xl),
                    ) {
                        item(key = "card") {
                            ProductBalanceCard(
                                name = current.product.name,
                                stock = current.stock,
                                modifier = Modifier.padding(horizontal = Spacing.lg),
                            )
                        }
                        item(key = "buttons") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                Button(
                                    onClick = { sheet = StockKind.ADD },
                                    shape = MaterialTheme.shapes.medium,
                                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.md),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = stringResource(R.string.prod_add_stock),
                                        style = MaterialTheme.typography.labelLarge,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { sheet = StockKind.DEDUCT },
                                    shape = MaterialTheme.shapes.medium,
                                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.md),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = stringResource(R.string.prod_deduct_stock),
                                        style = MaterialTheme.typography.labelLarge,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { sheet = StockKind.OPENING },
                                    shape = MaterialTheme.shapes.medium,
                                    contentPadding = PaddingValues(horizontal = Spacing.sm, vertical = Spacing.md),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = stringResource(R.string.prod_opening_stock),
                                        style = MaterialTheme.typography.labelLarge,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                        item(key = "sold") {
                            val sold = amountText(current.stock.sold)
                            Text(
                                text = if (current.stock.hasOpening) {
                                    stringResource(R.string.prod_sold_since_opening, sold)
                                } else {
                                    stringResource(R.string.prod_sold_total, sold)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = Spacing.lg),
                            )
                        }
                        item(key = "header") {
                            SectionHeader(
                                title = stringResource(R.string.prod_log_title),
                                modifier = Modifier.padding(top = Spacing.md),
                            )
                        }
                        if (current.entries.isEmpty()) {
                            item(key = "empty") {
                                Text(
                                    text = stringResource(R.string.prod_log_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                                )
                            }
                        } else {
                            items(current.entries, key = { it.id }) { entry ->
                                EntryRow(entry)
                            }
                        }
                    }
                }
            }
        }
    }

    val content = current as? StockLogUi.Content
    when (sheet) {
        StockKind.ADD, StockKind.DEDUCT -> {
            val kind = sheet.orEmpty()
            AdjustStockSheet(
                kind = kind,
                onSave = { amount, reason ->
                    vm.adjust(kind, amount, reason)
                    sheet = null
                },
                onDismiss = { sheet = null },
            )
        }
        StockKind.OPENING -> OpeningSheet(
            current = content?.stock?.takeIf { it.hasOpening }?.opening,
            onSave = { amount ->
                vm.setOpening(amount)
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        else -> Unit
    }
}

@Composable
private fun EntryRow(entry: StockEntryEntity) {
    val kindLabel = stringResource(
        when (entry.kind) {
            StockKind.ADD -> R.string.prod_kind_add
            StockKind.DEDUCT -> R.string.prod_kind_deduct
            else -> R.string.prod_kind_opening
        },
    )
    val amount = amountText(entry.amount)
    val signed = when (entry.kind) {
        StockKind.ADD -> "+$amount"
        StockKind.DEDUCT -> "-$amount"
        else -> amount
    }
    val tone = when (entry.kind) {
        StockKind.ADD -> MaterialTheme.colorScheme.primary
        StockKind.DEDUCT -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val date = formatDateTime(entry.createdAt)
    ListRow(
        title = kindLabel,
        supporting = if (entry.reason.isBlank()) date else "$date · ${entry.reason}",
        trailing = {
            Text(text = signed, style = MaterialTheme.typography.titleMedium, color = tone)
        },
    )
}
