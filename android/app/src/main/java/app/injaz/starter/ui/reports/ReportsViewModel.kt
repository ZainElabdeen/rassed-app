package app.injaz.starter.ui.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.injaz.starter.data.AppGraph
import app.injaz.starter.domain.DateRanges
import app.injaz.starter.domain.ReportData
import app.injaz.starter.domain.buildReport
import app.injaz.starter.pdf.ReportPdf
import app.injaz.starter.pdf.reportFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** What the person picked: a month (first-of-month millis) or a custom span of days (local day starts). */
data class ReportParams(
    val custom: Boolean,
    val month: Long,
    val from: Long,
    val to: Long,
    val nonce: Int = 0,
)

/** [report] is null when the custom range is backwards ([rangeError]) or building it failed ([failed]). */
data class ReportsUiState(
    val report: ReportData?,
    val rangeError: Boolean = false,
    val failed: Boolean = false,
)

sealed interface ExportState {
    data object Idle : ExportState
    data object Working : ExportState
    data class Ready(val file: File) : ExportState
    data object Failed : ExportState
}

class ReportsViewModel : ViewModel() {
    private val repo = AppGraph.repository

    private val _params = MutableStateFlow(
        System.currentTimeMillis().let { now ->
            ReportParams(
                custom = false,
                month = DateRanges.startOfMonth(now),
                from = DateRanges.startOfMonth(now),
                to = DateRanges.startOfDay(now),
            )
        },
    )
    val params: StateFlow<ReportParams> = _params.asStateFlow()

    val state: StateFlow<ReportsUiState?> = combine(_params, repo.data) { p, data ->
        val range = if (p.custom) DateRanges.customRange(p.from, p.to) else DateRanges.monthRange(p.month)
        if (range == null) {
            ReportsUiState(report = null, rangeError = true)
        } else {
            try {
                ReportsUiState(report = buildReport(range, data.sales, data.payments, data.customers))
            } catch (e: Exception) {
                ReportsUiState(report = null, failed = true)
            }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _export = MutableStateFlow<ExportState>(ExportState.Idle)
    val export: StateFlow<ExportState> = _export.asStateFlow()

    fun setCustom(custom: Boolean) = _params.update { it.copy(custom = custom) }

    /** Moves the month by [delta]; never past the current month. */
    fun shiftMonth(delta: Int) = _params.update {
        val next = DateRanges.shiftMonth(it.month, delta)
        if (next > DateRanges.startOfMonth(System.currentTimeMillis())) it else it.copy(month = next)
    }

    fun setFrom(day: Long) = _params.update { it.copy(from = day) }

    fun setTo(day: Long) = _params.update { it.copy(to = day) }

    fun retry() = _params.update { it.copy(nonce = it.nonce + 1) }

    /** Draws the current report into a PDF off the main thread; [context] is only used during this call. */
    fun exportPdf(context: Context, title: String, rtl: Boolean) {
        val report = state.value?.report ?: return
        if (_export.value is ExportState.Working) return
        _export.value = ExportState.Working
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                    val file = reportFile(context, "raseed-report-$stamp.pdf")
                    ReportPdf.write(context, report, title, rtl, file)
                    ExportState.Ready(file)
                } catch (e: Exception) {
                    ExportState.Failed
                }
            }
            _export.value = result
        }
    }

    fun dismissExport() {
        _export.value = ExportState.Idle
    }
}
