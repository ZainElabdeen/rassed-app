@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.injaz.starter.ui.settings

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.injaz.starter.R
import app.injaz.starter.domain.BackupError
import app.injaz.starter.domain.formatDateTime
import app.injaz.starter.ui.common.findActivity
import app.injaz.starter.ui.kit.AppTopBar
import app.injaz.starter.ui.kit.ChipRow
import app.injaz.starter.ui.kit.ConfirmDialog
import app.injaz.starter.ui.kit.InfoCard
import app.injaz.starter.ui.kit.PrimaryButton
import app.injaz.starter.ui.kit.Spacing
import app.injaz.starter.ui.theme.AppLanguage

private fun rejectedRes(error: BackupError): Int = when (error) {
    BackupError.UNREADABLE -> R.string.set_err_unreadable
    BackupError.WRONG_FORMAT -> R.string.set_err_wrong_format
    BackupError.NEWER_VERSION -> R.string.set_err_newer_version
    BackupError.BAD_CHECKSUM -> R.string.set_err_bad_checksum
    BackupError.BAD_COUNTS -> R.string.set_err_bad_counts
    BackupError.DUPLICATE_ID -> R.string.set_err_duplicate_id
    BackupError.BAD_REFERENCE -> R.string.set_err_bad_reference
    BackupError.BAD_VALUE -> R.string.set_err_bad_value
}

private fun messageRes(message: SettingsMessage): Int = when (message) {
    SettingsMessage.Saved -> R.string.set_msg_saved
    SettingsMessage.WriteFailed -> R.string.set_msg_write_failed
    SettingsMessage.ReadFailed -> R.string.set_msg_read_failed
    SettingsMessage.TooLarge -> R.string.set_msg_too_large
    is SettingsMessage.Rejected -> rejectedRes(message.error)
    SettingsMessage.ImportDone -> R.string.set_msg_import_done
    SettingsMessage.ImportFailed -> R.string.set_msg_import_failed
}

private fun isSuccess(message: SettingsMessage): Boolean =
    message == SettingsMessage.Saved || message == SettingsMessage.ImportDone

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm: SettingsViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val language by vm.language.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resolver = context.contentResolver

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.saveTo(resolver, uri)
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.readBackup(resolver, uri)
    }

    val chooserTitle = stringResource(R.string.set_share_chooser)
    val arabicLabel = stringResource(R.string.set_language_ar)
    val englishLabel = stringResource(R.string.set_language_en)
    val busy = state.busy

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.more_settings),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationLabel = stringResource(R.string.action_back),
                onNavigate = onBack,
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = stringResource(R.string.set_busy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val message = state.message
            if (message != null) {
                MessageCard(
                    text = stringResource(messageRes(message)),
                    success = isSuccess(message),
                    onDismiss = vm::dismissMessage,
                )
            }

            InfoCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.set_language_title), style = MaterialTheme.typography.titleMedium)
                ChipRow(
                    options = listOf(AppLanguage.AR, AppLanguage.EN),
                    selected = language,
                    onSelect = { vm.setLanguage(it) },
                    label = { code -> if (code == AppLanguage.AR) arabicLabel else englishLabel },
                    modifier = Modifier.padding(horizontal = 0.dp),
                )
            }

            InfoCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.set_backup_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.set_backup_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(
                    text = stringResource(R.string.set_backup_save),
                    onClick = { saveLauncher.launch(vm.suggestedFileName()) },
                    enabled = !busy,
                )
                OutlinedButton(
                    onClick = {
                        vm.prepareShare(context.cacheDir) { file ->
                            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                clipData = ClipData.newRawUri("", uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val chooser = Intent.createChooser(send, chooserTitle)
                            if (context.findActivity() == null) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(chooser)
                        }
                    },
                    enabled = !busy,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                ) {
                    Text(text = stringResource(R.string.set_backup_share), style = MaterialTheme.typography.labelLarge)
                }
            }

            InfoCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.set_restore_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.set_restore_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = {
                        openLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
                    },
                    enabled = !busy,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                ) {
                    Text(text = stringResource(R.string.set_restore_pick), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    val pending = state.pendingImport
    if (pending != null) {
        ConfirmDialog(
            title = stringResource(R.string.set_import_confirm_title),
            message = stringResource(
                R.string.set_import_confirm_message,
                formatDateTime(pending.exportedAt),
                pending.counts.products.toString(),
                pending.counts.customers.toString(),
                pending.counts.sales.toString(),
                pending.counts.payments.toString(),
                pending.counts.stockEntries.toString(),
            ),
            confirmLabel = stringResource(R.string.set_import_confirm_action),
            dismissLabel = stringResource(R.string.action_cancel),
            onConfirm = vm::confirmImport,
            onDismiss = vm::cancelImport,
            destructive = true,
        )
    }
}

@Composable
private fun MessageCard(text: String, success: Boolean, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val container = if (success) scheme.primaryContainer else scheme.errorContainer
    val content = if (success) scheme.onPrimaryContainer else scheme.onErrorContainer
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.lg, top = Spacing.xs, bottom = Spacing.xs, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = Spacing.sm),
            )
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
            }
        }
    }
}
