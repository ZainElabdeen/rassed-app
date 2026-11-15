@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.injaz.starter.ui.customers

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import app.injaz.starter.R
import app.injaz.starter.data.PayMethod
import app.injaz.starter.domain.DateRanges
import app.injaz.starter.domain.parseAmount
import app.injaz.starter.ui.common.AmountField
import app.injaz.starter.ui.common.DateField
import app.injaz.starter.ui.kit.AppBottomSheet
import app.injaz.starter.ui.kit.ChipRow
import app.injaz.starter.ui.kit.PrimaryButton
import app.injaz.starter.ui.kit.Spacing

/** Add or edit a customer: a name (required) and a phone (optional). Reused for both, only the title differs. */
@Composable
fun CustomerFormSheet(
    initialName: String,
    initialPhone: String,
    titleRes: Int,
    onSave: (name: String, phone: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var phone by rememberSaveable { mutableStateOf(initialPhone) }
    var triedSave by rememberSaveable { mutableStateOf(false) }
    val nameMissing = triedSave && name.isBlank()

    AppBottomSheet(onDismiss = onDismiss) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Spacing.lg))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.cust_field_name)) },
            singleLine = true,
            isError = nameMissing,
            supportingText = if (nameMissing) {
                { Text(stringResource(R.string.cust_name_required)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(stringResource(R.string.cust_field_phone)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.lg))
        PrimaryButton(
            text = stringResource(R.string.action_save),
            onClick = {
                if (name.isBlank()) {
                    triedSave = true
                } else {
                    onSave(name.trim(), phone.trim())
                }
            },
        )
    }
}

/** Record a later payment: amount, method, its own date and an optional note. */
@Composable
fun PaymentSheet(
    balance: Long,
    onSave: (amount: Long, paidAt: Long, method: String, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var method by rememberSaveable { mutableStateOf(PayMethod.CASH) }
    var day by rememberSaveable { mutableStateOf(DateRanges.startOfDay(System.currentTimeMillis())) }
    var note by rememberSaveable { mutableStateOf("") }
    val amount = parseAmount(amountText)
    val cashLabel = stringResource(R.string.method_cash)
    val bankakLabel = stringResource(R.string.method_bankak)

    AppBottomSheet(onDismiss = onDismiss) {
        Text(text = stringResource(R.string.cust_payment_sheet_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(Spacing.lg))
        AmountField(
            value = amountText,
            onValueChange = { amountText = it },
            label = stringResource(R.string.field_amount),
        )
        if (balance > 0) {
            TextButton(
                onClick = { amountText = balance.toString() },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.cust_full_balance))
            }
        } else {
            Spacer(Modifier.height(Spacing.md))
        }
        Text(
            text = stringResource(R.string.cust_field_method),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        ChipRow(
            options = PayMethod.forPayment,
            selected = method,
            onSelect = { method = it },
            label = { if (it == PayMethod.BANKAK) bankakLabel else cashLabel },
        )
        Spacer(Modifier.height(Spacing.md))
        DateField(
            label = stringResource(R.string.cust_payment_date),
            dayMillis = day,
            onChange = { day = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text(stringResource(R.string.cust_field_note)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.lg))
        PrimaryButton(
            text = stringResource(R.string.action_save),
            enabled = amount != null,
            onClick = { if (amount != null) onSave(amount, DateRanges.atTimeOfDay(day, System.currentTimeMillis()), method, note.trim()) },
        )
    }
}
