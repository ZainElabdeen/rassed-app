@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.injaz.starter.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.injaz.starter.R
import app.injaz.starter.data.StockKind
import app.injaz.starter.domain.parseAmount
import app.injaz.starter.domain.parseNonNegativeAmount
import app.injaz.starter.ui.common.AmountField
import app.injaz.starter.ui.kit.AppBottomSheet
import app.injaz.starter.ui.kit.ChipRow
import app.injaz.starter.ui.kit.PrimaryButton
import app.injaz.starter.ui.kit.Spacing

/** Add to or deduct from a product's stock. [kind] is [StockKind.ADD] or [StockKind.DEDUCT]. */
@Composable
fun AdjustStockSheet(
    kind: String,
    onSave: (amount: Long, reason: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val amount = parseAmount(amountText)
    val suggestions = listOf(
        stringResource(R.string.prod_reason_purchase),
        stringResource(R.string.prod_reason_correction),
        stringResource(R.string.prod_reason_loss),
    )
    val title = if (kind == StockKind.DEDUCT) R.string.prod_deduct_title else R.string.prod_add_title

    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = Spacing.md),
        )
        AmountField(
            value = amountText,
            onValueChange = { amountText = it },
            label = stringResource(R.string.field_amount),
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(Spacing.md))
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it.take(120) },
            label = { Text(stringResource(R.string.prod_reason_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.sm))
        ChipRow(
            options = suggestions,
            selected = suggestions.firstOrNull { it == reason },
            onSelect = { reason = it },
            label = { it },
            modifier = Modifier.padding(horizontal = 0.dp),
        )
        Spacer(Modifier.height(Spacing.lg))
        SheetButtons(
            saveEnabled = amount != null && reason.isNotBlank(),
            onSave = { if (amount != null && reason.isNotBlank()) onSave(amount, reason.trim()) },
            onCancel = onDismiss,
        )
    }
}

/** Sets the month's opening stock. Zero is allowed. [current] is the amount already set, if any. */
@Composable
fun OpeningSheet(
    current: Long?,
    onSave: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf(current?.toString() ?: "") }
    val amount = parseNonNegativeAmount(amountText)

    AppBottomSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.prod_opening_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = Spacing.xs),
        )
        Text(
            text = stringResource(R.string.prod_opening_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Spacing.md),
        )
        AmountField(
            value = amountText,
            onValueChange = { amountText = it },
            label = stringResource(R.string.field_amount),
        )
        Spacer(Modifier.height(Spacing.lg))
        SheetButtons(
            saveEnabled = amount != null,
            onSave = { if (amount != null) onSave(amount) },
            onCancel = onDismiss,
        )
    }
}

@Composable
internal fun SheetButtons(saveEnabled: Boolean, onSave: () -> Unit, onCancel: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        PrimaryButton(
            text = stringResource(R.string.action_save),
            onClick = onSave,
            enabled = saveEnabled,
            modifier = Modifier.weight(1f),
        )
    }
}
