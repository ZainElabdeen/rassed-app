@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.injaz.starter.ui.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.injaz.starter.R
import app.injaz.starter.data.CustomerEntity
import app.injaz.starter.data.PayMethod
import app.injaz.starter.domain.computeStock
import app.injaz.starter.domain.exceedsStock
import app.injaz.starter.domain.parseAmount
import app.injaz.starter.ui.common.AmountField
import app.injaz.starter.ui.common.ProductBalanceCard
import app.injaz.starter.ui.common.amountText
import app.injaz.starter.ui.kit.AppBottomSheet
import app.injaz.starter.ui.kit.AppTopBar
import app.injaz.starter.ui.kit.ChipRow
import app.injaz.starter.ui.kit.EmptyState
import app.injaz.starter.ui.kit.ErrorState
import app.injaz.starter.ui.kit.InfoCard
import app.injaz.starter.ui.kit.ListRow
import app.injaz.starter.ui.kit.LoadingState
import app.injaz.starter.ui.kit.PrimaryButton
import app.injaz.starter.ui.kit.SectionHeader
import app.injaz.starter.ui.kit.Spacing

@Composable
fun NewSaleScreen(onBack: () -> Unit, viewModel: NewSaleViewModel = viewModel()) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    var showSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(form.saved) {
        if (form.saved) onBack()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.sales_new_title),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationLabel = stringResource(R.string.action_back),
                onNavigate = onBack,
            )
        },
    ) { inner ->
        val current = data
        val app = current?.app
        when {
            current == null || app == null -> LoadingState(modifier = Modifier.padding(inner))
            current.failed -> ErrorState(
                title = stringResource(R.string.error_generic_title),
                message = stringResource(R.string.error_generic_message),
                modifier = Modifier.padding(inner),
                retryLabel = stringResource(R.string.action_retry),
                onRetry = viewModel::retry,
            )
            app.activeProducts.isEmpty() -> EmptyState(
                title = stringResource(R.string.sales_no_products_title),
                message = stringResource(R.string.sales_no_products_message),
                modifier = Modifier.padding(inner),
                icon = Icons.Filled.ShoppingCart,
            )
            else -> {
                val products = app.activeProducts
                val names = products.associate { it.id to it.name }
                val methodLabels = saleMethodLabels()
                val amount = parseAmount(form.amountText)
                val selectedName = form.productId?.let { names[it] }
                val stock = if (selectedName != null) computeStock(form.productId.orEmpty(), app.sales, app.stock) else null
                val customer = form.customerId?.let { id -> app.customers.firstOrNull { it.id == id } }
                val overStock = stock != null && amount != null && exceedsStock(stock.remaining, amount)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner)
                        .consumeWindowInsets(inner)
                        .imePadding(),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        SectionHeader(title = stringResource(R.string.sales_section_product))
                        ChipRow(
                            options = products.map { it.id },
                            selected = form.productId,
                            onSelect = viewModel::selectProduct,
                            label = { id -> names[id].orEmpty() },
                        )
                        if (selectedName != null && stock != null) {
                            ProductBalanceCard(
                                name = selectedName,
                                stock = stock,
                                modifier = Modifier.padding(horizontal = Spacing.lg),
                            )
                        }

                        AmountField(
                            value = form.amountText,
                            onValueChange = viewModel::setAmount,
                            label = stringResource(R.string.field_amount),
                            modifier = Modifier.padding(horizontal = Spacing.lg),
                            isError = form.amountText.isNotEmpty() && amount == null,
                            supportingText = if (form.amountText.isNotEmpty() && amount == null) {
                                stringResource(R.string.sales_amount_invalid)
                            } else {
                                null
                            },
                        )
                        if (overStock && stock != null) {
                            NoticeBox(
                                text = stringResource(R.string.sales_over_stock, amountText(stock.remaining)),
                                modifier = Modifier.padding(horizontal = Spacing.lg),
                            )
                        }

                        SectionHeader(title = stringResource(R.string.sales_section_method))
                        ChipRow(
                            options = PayMethod.forSale,
                            selected = form.method,
                            onSelect = viewModel::setMethod,
                            label = { m -> methodLabels[m].orEmpty() },
                        )

                        SectionHeader(title = stringResource(R.string.sales_section_customer))
                        InfoCard(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(Spacing.sm))
                                Text(
                                    text = customer?.name ?: stringResource(R.string.no_customer),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                if (customer != null && form.method != PayMethod.CREDIT) {
                                    TextButton(onClick = { viewModel.setCustomer(null) }) {
                                        Icon(Icons.Filled.Close, contentDescription = null)
                                        Spacer(Modifier.width(Spacing.xs))
                                        Text(stringResource(R.string.sales_clear_customer))
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { showSheet = true },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 52.dp),
                            ) {
                                Text(
                                    stringResource(
                                        if (customer == null) R.string.sales_choose_customer else R.string.sales_change_customer,
                                    ),
                                )
                            }
                        }
                        if (form.method == PayMethod.CREDIT) {
                            NoticeBox(
                                text = stringResource(R.string.sales_credit_note),
                                modifier = Modifier.padding(horizontal = Spacing.lg),
                            )
                        }
                        Spacer(Modifier.width(Spacing.sm))
                    }

                    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            if (form.saveFailed) {
                                Text(
                                    text = stringResource(R.string.sales_save_failed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            PrimaryButton(
                                text = stringResource(R.string.sales_save),
                                onClick = viewModel::save,
                                enabled = canSaveSale(form),
                                loading = form.saving,
                            )
                        }
                    }
                }

                if (showSheet) {
                    CustomerPickerSheet(
                        customers = app.customers.sortedBy { it.name },
                        failed = form.customerFailed,
                        onPick = { id ->
                            viewModel.setCustomer(id)
                            showSheet = false
                        },
                        onCreate = { name, phone ->
                            viewModel.addCustomer(name, phone) { showSheet = false }
                        },
                        onDismiss = { showSheet = false },
                    )
                }
            }
        }
    }
}

/** An amber note: a warning that does not block anything. */
@Composable
private fun NoticeBox(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}

/** Choose an existing customer, or add a new one in the same sheet. */
@Composable
private fun CustomerPickerSheet(
    customers: List<CustomerEntity>,
    failed: Boolean,
    onPick: (String) -> Unit,
    onCreate: (name: String, phone: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var nameMissing by rememberSaveable { mutableStateOf(false) }

    AppBottomSheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.imePadding(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(R.string.sales_sheet_title),
                style = MaterialTheme.typography.titleLarge,
            )
            if (customers.isEmpty()) {
                Text(
                    text = stringResource(R.string.sales_no_customers),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(customers, key = { it.id }) { c ->
                        ListRow(
                            title = c.name,
                            supporting = c.phone,
                            leadingIcon = Icons.Filled.Person,
                            onClick = { onPick(c.id) },
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.sales_new_customer),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = Spacing.sm),
            )
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameMissing = false
                },
                label = { Text(stringResource(R.string.sales_customer_name)) },
                singleLine = true,
                isError = nameMissing,
                supportingText = if (nameMissing) {
                    { Text(stringResource(R.string.sales_customer_name_required)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.sales_customer_phone)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            if (failed) {
                Text(
                    text = stringResource(R.string.sales_customer_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            PrimaryButton(
                text = stringResource(R.string.sales_customer_add),
                onClick = {
                    if (name.isBlank()) {
                        nameMissing = true
                    } else {
                        onCreate(name, phone)
                    }
                },
            )
        }
    }
}
