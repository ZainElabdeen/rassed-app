package app.injaz.starter.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import app.injaz.starter.data.PayMethod
import app.injaz.starter.domain.DateRanges
import app.injaz.starter.domain.PaidStatus
import app.injaz.starter.domain.ProductStock
import app.injaz.starter.domain.formatAmount
import app.injaz.starter.domain.formatDate
import app.injaz.starter.domain.normalizeDigits
import app.injaz.starter.ui.kit.InfoCard
import app.injaz.starter.ui.kit.Spacing

/** "1,500 ج.س" — the amount with the currency, Latin digits. */
@Composable
fun amountText(value: Long): String = stringResource(R.string.amount_fmt, formatAmount(value))

@Composable
fun methodLabel(method: String): String = stringResource(
    when (method) {
        PayMethod.CASH -> R.string.method_cash
        PayMethod.BANKAK -> R.string.method_bankak
        else -> R.string.method_credit
    },
)

@Composable
fun statusLabel(status: PaidStatus): String = stringResource(
    when (status) {
        PaidStatus.PAID -> R.string.status_paid
        PaidStatus.PARTIAL -> R.string.status_partial
        PaidStatus.UNPAID -> R.string.status_unpaid
    },
)

/** The activity behind a context, through any wrappers (the theme wraps the context to change language). */
fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

enum class PillTone { Neutral, Good, Warning, Bad }

/** A small status label: always text, tinted by tone (green paid, amber partly/low, red unpaid). */
@Composable
fun StatusPill(text: String, tone: PillTone, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when (tone) {
        PillTone.Neutral -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
        PillTone.Good -> scheme.primaryContainer to scheme.onPrimaryContainer
        PillTone.Warning -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        PillTone.Bad -> scheme.errorContainer to scheme.onErrorContainer
    }
    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs))
    }
}

@Composable
fun PaidStatus.pillTone(): PillTone = when (this) {
    PaidStatus.PAID -> PillTone.Good
    PaidStatus.PARTIAL -> PillTone.Warning
    PaidStatus.UNPAID -> PillTone.Bad
}

/**
 * The signature card: a product, what is left of its stock, and a thin bar against the month's opening.
 * Used on Home, in Products and when choosing a product for a sale.
 */
@Composable
fun ProductBalanceCard(
    name: String,
    stock: ProductStock,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    InfoCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (stock.remaining < 0) {
                StatusPill(stringResource(R.string.stock_negative), PillTone.Bad)
            } else if (stock.isLow) {
                StatusPill(stringResource(R.string.stock_low), PillTone.Warning)
            }
        }
        Text(text = amountText(stock.remaining), style = MaterialTheme.typography.headlineSmall)
        LinearProgressIndicator(
            progress = { stock.fraction },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = if (stock.isLow) scheme.tertiary else scheme.primary,
            trackColor = scheme.surfaceContainerHighest,
        )
        Text(
            text = if (stock.hasStock) stringResource(R.string.stock_of_total, amountText(stock.total)) else stringResource(R.string.stock_no_opening),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
    }
}

/** A whole-pounds field: digits only (Arabic digits are turned into Latin), with the currency after it. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(normalizeDigits(it).filter { c -> c in '0'..'9' }.take(12)) },
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { text -> { Text(text) } },
        suffix = { Text(stringResource(R.string.currency_sdg)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    )
}

/** A date shown as a button that opens a calendar. [dayMillis] and the result are the local start of the chosen day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    dayMillis: Long,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { open = true },
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
    ) {
        Icon(Icons.Filled.DateRange, contentDescription = null)
        Spacer(Modifier.width(Spacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDate(dayMillis), style = MaterialTheme.typography.bodyLarge)
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = DateRanges.localDayToPickerUtc(dayMillis))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onChange(DateRanges.pickerUtcToLocalDay(it)) }
                    open = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) {
            DatePicker(state = state)
        }
    }
}
