@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.injaz.starter.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.injaz.starter.R
import app.injaz.starter.domain.DateRanges
import app.injaz.starter.domain.NamedTotal
import app.injaz.starter.domain.ReportData
import app.injaz.starter.domain.ReportSale
import app.injaz.starter.domain.formatDate
import app.injaz.starter.pdf.printPdf
import app.injaz.starter.pdf.sharePdf
import app.injaz.starter.ui.common.DateField
import app.injaz.starter.ui.common.StatusPill
import app.injaz.starter.ui.common.amountText
import app.injaz.starter.ui.common.findActivity
import app.injaz.starter.ui.common.methodLabel
import app.injaz.starter.ui.common.pillTone
import app.injaz.starter.ui.common.statusLabel
import app.injaz.starter.ui.kit.AppBottomSheet
import app.injaz.starter.ui.kit.AppTopBar
import app.injaz.starter.ui.kit.ChipRow
import app.injaz.starter.ui.kit.EmptyState
import app.injaz.starter.ui.kit.ErrorState
import app.injaz.starter.ui.kit.InfoCard
import app.injaz.starter.ui.kit.LoadingState
import app.injaz.starter.ui.kit.PrimaryButton
import app.injaz.starter.ui.kit.SectionHeader
import app.injaz.starter.ui.kit.Spacing

private val monthNames = listOf(
    R.string.rep_month_1, R.string.rep_month_2, R.string.rep_month_3, R.string.rep_month_4,
    R.string.rep_month_5, R.string.rep_month_6, R.string.rep_month_7, R.string.rep_month_8,
    R.string.rep_month_9, R.string.rep_month_10, R.string.rep_month_11, R.string.rep_month_12,
)

@Composable
fun ReportsScreen(vm: ReportsViewModel = viewModel()) {
    val params by vm.params.collectAsStateWithLifecycle()
    val state by vm.state.collectAsStateWithLifecycle()
    val export by vm.export.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val pdfTitle = stringResource(R.string.rep_pdf_title)
    val shareTitle = stringResource(R.string.rep_share_title)
    val printJob = stringResource(R.string.rep_print_job)
    val modeMonth = stringResource(R.string.rep_mode_month)
    val modeCustom = stringResource(R.string.rep_mode_custom)
    val current = state
    val canNext = remember(params.month) { params.month < DateRanges.startOfMonth(System.currentTimeMillis()) }

    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.rep_title)) }) { inner ->
        Column(modifier = Modifier.padding(inner).fillMaxSize()) {
            ChipRow(
                options = listOf(false, true),
                selected = params.custom,
                onSelect = { vm.setCustom(it) },
                label = { if (it) modeCustom else modeMonth },
            )
            Spacer(Modifier.height(Spacing.sm))

            if (!params.custom) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { vm.shiftMonth(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.rep_prev_month))
                    }
                    Text(
                        text = stringResource(monthNames[DateRanges.month(params.month) - 1]) + " " + DateRanges.year(params.month).toString(),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { vm.shiftMonth(1) }, enabled = canNext) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.rep_next_month))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    DateField(
                        label = stringResource(R.string.rep_from),
                        dayMillis = params.from,
                        onChange = { vm.setFrom(it) },
                        modifier = Modifier.weight(1f),
                    )
                    DateField(
                        label = stringResource(R.string.rep_to),
                        dayMillis = params.to,
                        onChange = { vm.setTo(it) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (current?.rangeError == true) {
                    Text(
                        text = stringResource(R.string.rep_range_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    current == null -> LoadingState(label = stringResource(R.string.loading))
                    current.failed -> ErrorState(
                        title = stringResource(R.string.error_generic_title),
                        message = stringResource(R.string.error_generic_message),
                        retryLabel = stringResource(R.string.action_retry),
                        onRetry = { vm.retry() },
                    )
                    current.report != null -> ReportBody(current.report)
                }
            }

            if (export is ExportState.Failed) {
                Text(
                    text = stringResource(R.string.rep_pdf_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(Spacing.lg)) {
                PrimaryButton(
                    text = stringResource(R.string.rep_export_pdf),
                    onClick = { vm.exportPdf(context, pdfTitle, rtl) },
                    enabled = current?.report != null,
                    loading = export is ExportState.Working,
                )
            }
        }
    }

    val ready = export
    if (ready is ExportState.Ready) {
        val target = context.findActivity() ?: context
        AppBottomSheet(onDismiss = { vm.dismissExport() }) {
            Text(text = stringResource(R.string.rep_pdf_ready), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.rep_pdf_ready_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.lg),
            )
            PrimaryButton(
                text = stringResource(R.string.rep_share),
                onClick = { sharePdf(target, ready.file, shareTitle) },
            )
            Spacer(Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick = {
                    printPdf(target, ready.file, printJob)
                    vm.dismissExport()
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp),
            ) {
                Text(stringResource(R.string.rep_print), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ReportBody(report: ReportData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        item {
            SummaryCard(report, modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm))
        }
        if (report.sales.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().height(260.dp)) {
                    EmptyState(
                        title = stringResource(R.string.rep_empty_title),
                        message = stringResource(R.string.rep_empty_message),
                        icon = Icons.Filled.DateRange,
                    )
                }
            }
        } else {
            item { SectionHeader(title = stringResource(R.string.rep_by_product)) }
            items(report.byProduct, key = { "p-" + it.key }) { TotalRow(label = it.name, total = it) }
            item { SectionHeader(title = stringResource(R.string.rep_by_method)) }
            items(report.byMethod, key = { "m-" + it.key }) { TotalRow(label = methodLabel(it.key), total = it) }
            item { SectionHeader(title = stringResource(R.string.rep_sales_list)) }
            items(report.sales, key = { "s-" + it.sale.id }) { SaleRow(it) }
        }
    }
}

@Composable
private fun SummaryCard(report: ReportData, modifier: Modifier = Modifier) {
    InfoCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.rep_summary_total),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = amountText(report.totalSales), style = MaterialTheme.typography.headlineMedium)
        Text(
            text = stringResource(R.string.rep_summary_count, report.salesCount.toString()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xs))
        LabelValue(stringResource(R.string.rep_credit_given), amountText(report.creditGiven))
        LabelValue(stringResource(R.string.rep_payments_received), amountText(report.paymentsReceived))
        LabelValue(stringResource(R.string.rep_owed_now), amountText(report.outstandingNow))
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun TotalRow(label: String, total: NamedTotal) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.rep_row_count, total.count.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(text = amountText(total.total), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun SaleRow(item: ReportSale) {
    val sale = item.sale
    val line = formatDate(sale.createdAt) + " · " +
        (item.customerName ?: stringResource(R.string.no_customer)) + " · " +
        methodLabel(sale.method)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = sale.productName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(text = amountText(sale.amount), style = MaterialTheme.typography.titleSmall)
            val status = item.status
            if (status != null) {
                StatusPill(text = statusLabel(status), tone = status.pillTone())
            }
        }
    }
}
