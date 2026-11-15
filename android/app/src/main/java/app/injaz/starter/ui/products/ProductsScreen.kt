@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.injaz.starter.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.injaz.starter.R
import app.injaz.starter.ui.common.PillTone
import app.injaz.starter.ui.common.ProductBalanceCard
import app.injaz.starter.ui.common.StatusPill
import app.injaz.starter.ui.kit.AppBottomSheet
import app.injaz.starter.ui.kit.AppTopBar
import app.injaz.starter.ui.kit.ConfirmDialog
import app.injaz.starter.ui.kit.EmptyState
import app.injaz.starter.ui.kit.ErrorState
import app.injaz.starter.ui.kit.LoadingState
import app.injaz.starter.ui.kit.PrimaryButton
import app.injaz.starter.ui.kit.Spacing
import kotlinx.coroutines.launch

@Composable
fun ProductsScreen(
    onBack: () -> Unit,
    onOpenStock: (String) -> Unit,
    vm: ProductsViewModel = viewModel(),
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    val failed by vm.writeFailed.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val failedMessage = stringResource(R.string.prod_save_failed)

    var showAdd by remember { mutableStateOf(false) }
    var openingFor by remember { mutableStateOf<String?>(null) }
    var removeFor by remember { mutableStateOf<ProductRow?>(null) }

    LaunchedEffect(failed) {
        if (failed) {
            snackbar.showSnackbar(failedMessage)
            vm.clearFailed()
        }
    }

    val current = ui
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.prod_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationLabel = stringResource(R.string.action_back),
                onNavigate = onBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (current is ProductsUi.Content && current.rows.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAdd = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.prod_add_product)) },
                )
            }
        },
    ) { inner ->
        Column(Modifier.padding(inner)) {
            when (current) {
                null -> LoadingState(label = stringResource(R.string.loading))
                is ProductsUi.Failed -> ErrorState(
                    title = stringResource(R.string.error_generic_title),
                    message = stringResource(R.string.error_generic_message),
                    retryLabel = stringResource(R.string.action_retry),
                    onRetry = vm::retry,
                )
                is ProductsUi.Content -> {
                    if (current.rows.isEmpty()) {
                        EmptyState(
                            title = stringResource(R.string.prod_empty_title),
                            message = stringResource(R.string.prod_empty_message),
                            icon = Icons.Filled.Add,
                            actionLabel = stringResource(R.string.prod_add_product),
                            onAction = { showAdd = true },
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = Spacing.lg,
                                end = Spacing.lg,
                                top = Spacing.md,
                                bottom = 96.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                        ) {
                            items(current.rows, key = { it.product.id }) { row ->
                                ProductItem(
                                    row = row,
                                    onOpen = { onOpenStock(row.product.id) },
                                    onSetOpening = { openingFor = row.product.id },
                                    onRemove = { removeFor = row },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddProductSheet(
            onSave = { name ->
                vm.addProduct(name)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }

    val openingRow = (current as? ProductsUi.Content)?.rows?.firstOrNull { it.product.id == openingFor }
    if (openingRow != null) {
        OpeningSheet(
            current = if (openingRow.stock.hasOpening) openingRow.stock.opening else null,
            onSave = { amount ->
                vm.setOpening(openingRow.product.id, amount)
                openingFor = null
            },
            onDismiss = { openingFor = null },
        )
    }

    removeFor?.let { row ->
        ConfirmDialog(
            title = stringResource(R.string.prod_remove_title, row.product.name),
            message = stringResource(R.string.prod_remove_message),
            confirmLabel = stringResource(R.string.prod_remove),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = {
                vm.removeProduct(row.product.id)
                removeFor = null
            },
            onDismiss = { removeFor = null },
            destructive = true,
        )
    }
}

@Composable
private fun ProductItem(
    row: ProductRow,
    onOpen: () -> Unit,
    onSetOpening: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        ProductBalanceCard(name = row.product.name, stock = row.stock, onClick = onOpen)
        if (row.needsOpening) {
            StatusPill(
                text = stringResource(R.string.prod_opening_missing),
                tone = PillTone.Warning,
                modifier = Modifier.padding(horizontal = Spacing.xs),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onSetOpening, modifier = Modifier.weight(1f, fill = false)) {
                Text(stringResource(R.string.prod_set_opening))
            }
            TextButton(onClick = onRemove) {
                Text(stringResource(R.string.prod_remove), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductSheet(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val valid = name.isNotBlank()

    AppBottomSheet(onDismiss = onDismiss, sheetState = sheetState) {
        Text(
            text = stringResource(R.string.prod_add_product),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = Spacing.md),
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(60) },
            label = { Text(stringResource(R.string.prod_name_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.lg))
        SheetButtons(
            saveEnabled = valid,
            onSave = {
                if (valid) {
                    val clean = name.trim()
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onSave(clean) }
                }
            },
            onCancel = onDismiss,
        )
    }
}
