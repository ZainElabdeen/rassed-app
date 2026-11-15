package app.injaz.starter.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import app.injaz.starter.R
import app.injaz.starter.data.PayMethod
import app.injaz.starter.domain.PaidStatus
import app.injaz.starter.domain.ReportData
import app.injaz.starter.domain.formatAmount
import app.injaz.starter.domain.formatDate
import app.injaz.starter.domain.formatDateTime
import java.io.File
import java.io.FileOutputStream

/** A file inside the cache folder the FileProvider shares (`shared/`). Older reports there are tidied away. */
fun reportFile(context: Context, name: String): File {
    val dir = File(context.cacheDir, "shared")
    dir.mkdirs()
    val dayAgo = System.currentTimeMillis() - 24L * 60 * 60 * 1000
    dir.listFiles()?.forEach { if (it.name != name && it.isFile && it.lastModified() < dayAgo) it.delete() }
    return File(dir, name)
}

/**
 * Draws a report into an A4 PDF. Every piece of text goes through StaticLayout, which shapes Arabic
 * letters with the phone's own font and lays the paragraph out right to left.
 */
object ReportPdf {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40
    private const val FOOTER_H = 24
    private const val CELL_PAD = 3
    private const val CELL_VPAD = 4

    private val GREY_TEXT = Color.rgb(90, 90, 90)
    private val GREY_RULE = Color.rgb(190, 190, 190)
    private val GREY_FILL = Color.rgb(238, 238, 238)

    private fun paint(size: Float, bold: Boolean, color: Int = Color.BLACK): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    fun write(context: Context, report: ReportData, title: String, rtl: Boolean, file: File) {
        val w = Writer(rtl) { n -> context.getString(R.string.rep_pdf_page, n.toString()) }
        try {
            build(context, report, title, w)
            w.finish()
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out -> w.doc.writeTo(out) }
        } finally {
            w.doc.close()
        }
    }

    private fun amount(context: Context, value: Long): String =
        context.getString(R.string.amount_fmt, formatAmount(value))

    private fun methodName(context: Context, method: String): String = context.getString(
        when (method) {
            PayMethod.CASH -> R.string.method_cash
            PayMethod.BANKAK -> R.string.method_bankak
            else -> R.string.method_credit
        },
    )

    private fun statusName(context: Context, status: PaidStatus): String = context.getString(
        when (status) {
            PaidStatus.PAID -> R.string.status_paid
            PaidStatus.PARTIAL -> R.string.status_partial
            PaidStatus.UNPAID -> R.string.status_unpaid
        },
    )

    private fun build(context: Context, report: ReportData, title: String, w: Writer) {
        val titlePaint = paint(20f, true)
        val headingPaint = paint(14f, true)
        val bodyPaint = paint(10f, false)
        val boldPaint = paint(10f, true)
        val mutedPaint = paint(10f, false, GREY_TEXT)
        val dash = "-"

        w.newPage()
        w.paragraph(title, titlePaint)
        w.gap(4)
        w.paragraph(
            context.getString(R.string.rep_pdf_period, formatDate(report.range.from), formatDate(report.range.to - 1)),
            bodyPaint,
        )
        w.paragraph(context.getString(R.string.rep_pdf_generated, formatDateTime(System.currentTimeMillis())), mutedPaint)
        w.gap(6)
        w.rule()

        // Summary
        w.heading(context.getString(R.string.rep_pdf_summary), headingPaint)
        val kv = listOf(0.6f, 0.4f)
        listOf(
            context.getString(R.string.rep_pdf_total_sales) to amount(context, report.totalSales),
            context.getString(R.string.rep_pdf_sales_count) to report.salesCount.toString(),
            context.getString(R.string.rep_pdf_credit_given) to amount(context, report.creditGiven),
            context.getString(R.string.rep_pdf_payments_received) to amount(context, report.paymentsReceived),
            context.getString(R.string.rep_pdf_outstanding) to amount(context, report.outstandingNow),
        ).forEach { (label, value) ->
            w.row(listOf(label, value), kv, bodyPaint, boldPaint, rule = true)
        }

        // By product
        w.heading(context.getString(R.string.rep_pdf_by_product), headingPaint)
        if (report.byProduct.isEmpty()) {
            w.paragraph(context.getString(R.string.rep_pdf_no_sales), mutedPaint)
        } else {
            w.table(
                headers = listOf(
                    context.getString(R.string.rep_pdf_col_product),
                    context.getString(R.string.rep_pdf_col_count),
                    context.getString(R.string.rep_pdf_col_amount),
                ),
                fractions = listOf(0.5f, 0.2f, 0.3f),
                rows = report.byProduct.map { listOf(it.name, it.count.toString(), amount(context, it.total)) },
                body = bodyPaint,
                head = boldPaint,
            )
        }

        // By method
        w.heading(context.getString(R.string.rep_pdf_by_method), headingPaint)
        w.table(
            headers = listOf(
                context.getString(R.string.rep_pdf_col_method),
                context.getString(R.string.rep_pdf_col_count),
                context.getString(R.string.rep_pdf_col_amount),
            ),
            fractions = listOf(0.5f, 0.2f, 0.3f),
            rows = report.byMethod.map { listOf(methodName(context, it.key), it.count.toString(), amount(context, it.total)) },
            body = bodyPaint,
            head = boldPaint,
        )

        // Sales
        w.heading(context.getString(R.string.rep_pdf_sales), headingPaint)
        if (report.sales.isEmpty()) {
            w.paragraph(context.getString(R.string.rep_pdf_no_sales), mutedPaint)
        } else {
            w.table(
                headers = listOf(
                    context.getString(R.string.rep_pdf_col_date),
                    context.getString(R.string.rep_pdf_col_product),
                    context.getString(R.string.rep_pdf_col_customer),
                    context.getString(R.string.rep_pdf_col_method),
                    context.getString(R.string.rep_pdf_col_amount),
                    context.getString(R.string.rep_pdf_col_status),
                ),
                fractions = listOf(0.15f, 0.17f, 0.20f, 0.12f, 0.20f, 0.16f),
                rows = report.sales.map { s ->
                    listOf(
                        formatDate(s.sale.createdAt),
                        s.sale.productName,
                        s.customerName ?: dash,
                        methodName(context, s.sale.method),
                        amount(context, s.sale.amount),
                        s.status?.let { statusName(context, it) } ?: "",
                    )
                },
                body = bodyPaint,
                head = boldPaint,
            )
        }

        // Payments
        if (report.payments.isNotEmpty()) {
            w.heading(context.getString(R.string.rep_pdf_payments), headingPaint)
            w.table(
                headers = listOf(
                    context.getString(R.string.rep_pdf_col_date),
                    context.getString(R.string.rep_pdf_col_customer),
                    context.getString(R.string.rep_pdf_col_method),
                    context.getString(R.string.rep_pdf_col_amount),
                    context.getString(R.string.rep_pdf_col_note),
                ),
                fractions = listOf(0.16f, 0.22f, 0.14f, 0.22f, 0.26f),
                rows = report.payments.map { p ->
                    listOf(
                        formatDate(p.payment.paidAt),
                        p.customerName ?: dash,
                        methodName(context, p.payment.method),
                        amount(context, p.payment.amount),
                        p.payment.note ?: "",
                    )
                },
                body = bodyPaint,
                head = boldPaint,
            )
        }
    }

    /** Pages, a cursor, and the few drawing moves the report needs. */
    private class Writer(private val rtl: Boolean, private val footer: (Int) -> String) {
        val doc = PdfDocument()
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = MARGIN.toFloat()
        private var pageNo = 0
        private val contentW = PAGE_W - 2 * MARGIN
        private val bottom = (PAGE_H - MARGIN - FOOTER_H).toFloat()
        private val direction = if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR
        private val footerPaint = paint(9f, false, GREY_TEXT)
        private val rulePaint = Paint().apply { color = GREY_RULE; strokeWidth = 0.7f }
        private val fillPaint = Paint().apply { color = GREY_FILL; style = Paint.Style.FILL }

        private fun layout(text: String, paint: TextPaint, width: Int, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
                .setAlignment(align)
                .setTextDirection(direction)
                .setIncludePad(false)
                .build()

        private fun draw(l: StaticLayout, x: Float, top: Float) {
            val c = canvas ?: return
            c.save()
            c.translate(x, top)
            l.draw(c)
            c.restore()
        }

        fun newPage() {
            finish()
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            page = p
            canvas = p.canvas
            y = MARGIN.toFloat()
        }

        fun finish() {
            val p = page ?: return
            val l = layout(footer(pageNo), footerPaint, contentW, Layout.Alignment.ALIGN_CENTER)
            draw(l, MARGIN.toFloat(), (PAGE_H - MARGIN - 12).toFloat())
            doc.finishPage(p)
            page = null
            canvas = null
        }

        private fun ensure(height: Float) {
            if (y + height > bottom) newPage()
        }

        fun gap(h: Int) { y += h }

        fun rule() {
            ensure(8f)
            canvas?.drawLine(MARGIN.toFloat(), y + 2f, (MARGIN + contentW).toFloat(), y + 2f, rulePaint)
            y += 8f
        }

        fun paragraph(text: String, paint: TextPaint) {
            val l = layout(text, paint, contentW)
            ensure(l.height.toFloat() + 2f)
            draw(l, MARGIN.toFloat(), y)
            y += l.height + 2f
        }

        fun heading(text: String, paint: TextPaint) {
            val l = layout(text, paint, contentW)
            // keep a heading with at least a line of what follows it
            ensure(l.height.toFloat() + 60f)
            y += 10f
            draw(l, MARGIN.toFloat(), y)
            y += l.height + 6f
        }

        private fun columnX(fractions: List<Float>, index: Int): Float {
            var before = 0f
            for (i in 0 until index) before += fractions[i] * contentW
            val width = fractions[index] * contentW
            return if (rtl) MARGIN + contentW - before - width else MARGIN + before
        }

        private fun measure(cells: List<String>, fractions: List<Float>, paint: TextPaint): List<StaticLayout> =
            cells.mapIndexed { i, text ->
                layout(text, paint, (fractions[i] * contentW).toInt() - 2 * CELL_PAD)
            }

        /** One table row; the first cell is the right-most column in RTL. */
        fun row(
            cells: List<String>,
            fractions: List<Float>,
            paint: TextPaint,
            firstCellPaint: TextPaint = paint,
            shade: Boolean = false,
            rule: Boolean = false,
        ) {
            val layouts = cells.mapIndexed { i, text ->
                layout(text, if (i == 0) firstCellPaint else paint, (fractions[i] * contentW).toInt() - 2 * CELL_PAD)
            }
            drawRow(layouts, fractions, shade, rule)
        }

        private fun drawRow(layouts: List<StaticLayout>, fractions: List<Float>, shade: Boolean, rule: Boolean) {
            val h = (layouts.maxOfOrNull { it.height } ?: 0) + 2f * CELL_VPAD
            ensure(h)
            val c = canvas ?: return
            if (shade) c.drawRect(MARGIN.toFloat(), y, (MARGIN + contentW).toFloat(), y + h, fillPaint)
            layouts.forEachIndexed { i, l -> draw(l, columnX(fractions, i) + CELL_PAD, y + CELL_VPAD) }
            y += h
            if (rule) c.drawLine(MARGIN.toFloat(), y, (MARGIN + contentW).toFloat(), y, rulePaint)
        }

        fun table(
            headers: List<String>,
            fractions: List<Float>,
            rows: List<List<String>>,
            body: TextPaint,
            head: TextPaint,
        ) {
            val headerLayouts = measure(headers, fractions, head)
            drawRow(headerLayouts, fractions, shade = true, rule = false)
            for (cells in rows) {
                val layouts = measure(cells, fractions, body)
                val h = (layouts.maxOfOrNull { it.height } ?: 0) + 2f * CELL_VPAD
                if (y + h > bottom) {
                    newPage()
                    drawRow(measure(headers, fractions, head), fractions, shade = true, rule = false)
                }
                drawRow(layouts, fractions, shade = false, rule = true)
            }
        }
    }
}
