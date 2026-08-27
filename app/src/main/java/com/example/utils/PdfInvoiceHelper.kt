package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.models.Customer
import com.example.data.models.Sale
import com.example.ui.components.InvoicePreviewData
import com.example.ui.components.InvoicePreviewItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfInvoiceHelper {

    /**
     * Generate text receipt for thermal printer or simple messaging
     */
    fun generateReceiptText(
        sales: List<Sale>,
        storeName: String = "AF store للتجارة والتوزيع",
        cashierName: String = "المدير",
        customerName: String? = null,
        isDebt: Boolean = false,
        invoiceNumber: String = (System.currentTimeMillis() % 1000000).toString(),
        discountPercent: Double = 0.0,
        discountAmount: Double = 0.0,
        finalTotal: Double = sales.sumOf { it.totalPrice }
    ): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
        val dateStr = dateFormat.format(Date())
        val sb = StringBuilder()
        sb.append("================================\n")
        sb.append("         $storeName             \n")
        sb.append("      فاتورة مبيعات معتمدة       \n")
        sb.append("================================\n")
        sb.append("رقم الفاتورة: #$invoiceNumber\n")
        sb.append("التاريخ: $dateStr\n")
        sb.append("الكاشير: $cashierName\n")
        if (!customerName.isNullOrBlank()) {
            sb.append("العميل: $customerName\n")
        }
        sb.append("نوع الدفع: ${if (isDebt) "آجل على الحساب (دين) ⚠️" else "نقدي كاش 💵"}\n")
        sb.append("--------------------------------\n")
        sb.append(String.format(Locale("ar"), "%-14s %-4s %-8s\n", "الصنف", "الكمية", "السعر"))
        sb.append("--------------------------------\n")

        var totalQty = 0
        for (sale in sales) {
            val name = if (sale.productName.length > 14) sale.productName.take(12) + ".." else sale.productName
            sb.append(String.format(Locale("ar"), "%-14s %-4d %-8.2f\n", name, sale.quantitySold, sale.totalPrice))
            totalQty += sale.quantitySold
        }

        sb.append("================================\n")
        sb.append("إجمالي الكميات: $totalQty قطعة\n")
        val subtotal = sales.sumOf { it.totalPrice }
        sb.append(String.format(Locale("ar"), "المجموع الفرعي: %.2f ج.م\n", subtotal))
        if (discountPercent > 0 || discountAmount > 0) {
            sb.append(String.format(Locale("ar"), "الخصم: -%.2f ج.م (%.0f%%)\n", discountAmount, discountPercent))
        }
        sb.append(String.format(Locale("ar"), "الإجمالي النهائي: %.2f ج.م\n", finalTotal))
        sb.append("================================\n")
        sb.append("   شكراً لتعاملكم معنا ونسعد بزيارتكم!   \n")
        sb.append("================================\n")
        return sb.toString()
    }

    /**
     * Create high-fidelity, Arabic-formatted vector PDF invoice
     */
    fun createPdfInvoice(
        context: Context,
        sales: List<Sale>,
        storeName: String = "AF store للتجارة والتوزيع",
        cashier: String = "المدير",
        customerName: String? = null,
        invoiceNumber: String = (System.currentTimeMillis() % 1000000).toString(),
        discountPercent: Double = 0.0,
        discountAmount: Double = 0.0,
        finalTotal: Double = sales.sumOf { it.totalPrice },
        isDebt: Boolean = false,
        paymentMethod: String = if (isDebt) "DEBT" else "CASH"
    ): File? {
        return try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595 // Standard A4 points width (approx 8.27 in * 72)
            val baseHeight = 842 // Standard A4 points height

            // Calculate dynamic height if items are many, or single page
            val rowsCount = sales.size
            val neededHeight = (380 + (rowsCount * 36) + 160).coerceAtLeast(baseHeight)
            val pageHeight = neededHeight

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.DKGRAY
            }

            // 1. Header Banner & Branding
            val headerPaint = Paint().apply {
                color = Color.parseColor("#064E3B") // Emerald Brand Dark
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, headerPaint)

            // Header Gold Accent Strip
            val goldPaint = Paint().apply {
                color = Color.parseColor("#F59E0B") // Amber Gold
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 106f, pageWidth.toFloat(), 110f, goldPaint)

            // Store Name
            paint.color = Color.WHITE
            paint.textSize = 24f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(storeName, (pageWidth / 2).toFloat(), 48f, paint)

            // Subtitle
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("فاتورة مبيعات ضريبية وتجارية رسمية", (pageWidth / 2).toFloat(), 75f, paint)
            paint.textSize = 11f
            paint.color = Color.parseColor("#A7F3D0")
            canvas.drawText("AF Store POS & Inventory Management System", (pageWidth / 2).toFloat(), 95f, paint)

            // 2. Invoice Meta Cards (Left & Right)
            val dateFormat = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))
            val dateStr = dateFormat.format(Date())

            var yPos = 140f

            // Meta Background Box
            val metaBgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val metaRect = RectF(28f, yPos - 15f, (pageWidth - 28).toFloat(), yPos + 75f)
            canvas.drawRoundRect(metaRect, 12f, 12f, metaBgPaint)

            val metaBorderPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }
            canvas.drawRoundRect(metaRect, 12f, 12f, metaBorderPaint)

            // Column 1 (Right Aligned in Arabic)
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.RIGHT

            canvas.drawText("رقم الفاتورة: #$invoiceNumber", (pageWidth - 45).toFloat(), yPos + 10f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#475569")
            canvas.drawText("التاريخ والوقت: $dateStr", (pageWidth - 45).toFloat(), yPos + 32f, paint)
            canvas.drawText("الكاشير المسؤول: $cashier", (pageWidth - 45).toFloat(), yPos + 54f, paint)

            // Column 2 (Left Aligned details)
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            if (!customerName.isNullOrBlank()) {
                paint.color = Color.parseColor("#064E3B")
                canvas.drawText("العميل: $customerName", 45f, yPos + 10f, paint)
            } else {
                paint.color = Color.parseColor("#64748B")
                canvas.drawText("العميل: زبون عام (نقدي)", 45f, yPos + 10f, paint)
            }

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = if (isDebt) Color.parseColor("#DC2626") else Color.parseColor("#16A34A")
            val payStatus = if (isDebt) "طريقة السداد: آجل على الحساب (دين ⚠️)" else "طريقة السداد: نقدي كاش (مدفوع بالكامل 💵)"
            canvas.drawText(payStatus, 45f, yPos + 32f, paint)

            paint.color = Color.parseColor("#475569")
            canvas.drawText("حالة الفاتورة: معتمدة ومؤكدة ✅", 45f, yPos + 54f, paint)

            // 3. Table Header
            yPos += 110f
            val tableHeaderPaint = Paint().apply {
                color = Color.parseColor("#064E3B")
                style = Paint.Style.FILL
            }
            val thRect = RectF(28f, yPos - 18f, (pageWidth - 28).toFloat(), yPos + 14f)
            canvas.drawRoundRect(thRect, 8f, 8f, tableHeaderPaint)

            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("الصنف والمواصفات", (pageWidth - 45).toFloat(), yPos, paint)

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("الكمية", 280f, yPos, paint)
            canvas.drawText("سعر الوحدة", 175f, yPos, paint)

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("الإجمالي (ج.م)", 45f, yPos, paint)

            // 4. Table Rows
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val rowBgAlt = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
            }

            var subtotal = 0.0
            var totalCount = 0
            var rowIndex = 0

            for (sale in sales) {
                yPos += 30f
                if (rowIndex % 2 == 1) {
                    canvas.drawRect(28f, yPos - 20f, (pageWidth - 28).toFloat(), yPos + 10f, rowBgAlt)
                }

                paint.color = Color.parseColor("#0F172A")
                paint.textAlign = Paint.Align.RIGHT
                val name = if (sale.productName.length > 32) sale.productName.take(30) + ".." else sale.productName
                canvas.drawText(name, (pageWidth - 45).toFloat(), yPos, paint)

                paint.textAlign = Paint.Align.CENTER
                paint.color = Color.parseColor("#334155")
                canvas.drawText("${sale.quantitySold}", 280f, yPos, paint)

                val unitPrice = if (sale.quantitySold > 0) sale.totalPrice / sale.quantitySold else sale.totalPrice
                canvas.drawText(String.format(Locale.getDefault(), "%.2f", unitPrice), 175f, yPos, paint)

                paint.textAlign = Paint.Align.LEFT
                paint.color = Color.parseColor("#0F172A")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(String.format(Locale.getDefault(), "%.2f", sale.totalPrice), 45f, yPos, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                canvas.drawLine(28f, yPos + 10f, (pageWidth - 28).toFloat(), yPos + 10f, linePaint)

                subtotal += sale.totalPrice
                totalCount += sale.quantitySold
                rowIndex++
            }

            // 5. Financial Summary Card Box
            yPos += 35f
            val summaryCardRect = RectF(28f, yPos, (pageWidth - 28).toFloat(), yPos + 110f)
            val summaryCardBg = Paint().apply {
                color = Color.parseColor("#F1F5F9")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(summaryCardRect, 12f, 12f, summaryCardBg)
            canvas.drawRoundRect(summaryCardRect, 12f, 12f, metaBorderPaint)

            // Right column of summary: counts & stats
            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.parseColor("#475569")
            paint.textSize = 12f
            canvas.drawText("إجمالي عدد الأصناف المباعة: ${sales.size} صنف ($totalCount قطعة)", (pageWidth - 45).toFloat(), yPos + 28f, paint)
            canvas.drawText("المجموع الفرعي قبل الخصومات:", (pageWidth - 45).toFloat(), yPos + 52f, paint)
            if (discountPercent > 0 || discountAmount > 0) {
                paint.color = Color.parseColor("#DC2626")
                canvas.drawText(String.format(Locale.getDefault(), "الخصم المطبق (%.0f%%):", discountPercent), (pageWidth - 45).toFloat(), yPos + 76f, paint)
            }

            // Left column of summary: amounts & total
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f ج.م", subtotal), 45f, yPos + 52f, paint)
            if (discountPercent > 0 || discountAmount > 0) {
                paint.color = Color.parseColor("#DC2626")
                canvas.drawText(String.format(Locale.getDefault(), "-%.2f ج.م", discountAmount), 45f, yPos + 76f, paint)
            }

            // Grand Total Ribbon
            val grandTotalRect = RectF(28f, yPos + 82f, (pageWidth - 28).toFloat(), yPos + 124f)
            val grandTotalBg = Paint().apply {
                color = Color.parseColor("#064E3B")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(grandTotalRect, 8f, 8f, grandTotalBg)

            paint.color = Color.WHITE
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("المبلغ الإجمالي النهائي المستحق:", (pageWidth - 45).toFloat(), yPos + 108f, paint)

            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 16f
            canvas.drawText(String.format(Locale.getDefault(), "%.2f ج.م", finalTotal), 45f, yPos + 108f, paint)

            // 6. QR Code / Barcode Simulation & Footer Notice
            yPos += 165f

            // Footer dashed line
            val dashedPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
            }
            canvas.drawLine(28f, yPos, (pageWidth - 28).toFloat(), yPos, dashedPaint)

            yPos += 24f
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("شكراً لتسوقكم واختياركم $storeName - يسعدنا دائماً خدمتكم!", (pageWidth / 2).toFloat(), yPos, paint)

            yPos += 18f
            paint.textSize = 10f
            paint.color = Color.parseColor("#94A3B8")
            canvas.drawText("رقم الفاتورة: #$invoiceNumber | تم الإنشاء بواسطة نظام AF Store المعتمد", (pageWidth / 2).toFloat(), yPos, paint)

            pdfDoc.finishPage(page)

            // Save PDF to cache dir
            val invoicesDir = File(context.cacheDir, "invoices")
            if (!invoicesDir.exists()) invoicesDir.mkdirs()
            val pdfFile = File(invoicesDir, "Invoice_$invoiceNumber.pdf")
            val fos = FileOutputStream(pdfFile)
            pdfDoc.writeTo(fos)
            fos.close()
            pdfDoc.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Create high-fidelity, Arabic-formatted vector PDF invoice directly from InvoicePreviewData
     */
    fun createPdfFromInvoiceData(
        context: Context,
        invoiceData: InvoicePreviewData
    ): File? {
        return try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595 // Standard A4 points width
            val baseHeight = 842 // Standard A4 points height

            val itemsCount = invoiceData.items.size
            val neededHeight = (420 + (itemsCount * 42) + 180).coerceAtLeast(baseHeight)
            val pageHeight = neededHeight

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.DKGRAY
            }

            // 1. Header Banner & Emerald Branding
            val headerPaint = Paint().apply {
                color = Color.parseColor("#064E3B") // Emerald Brand Dark
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 115f, headerPaint)

            // Header Gold Accent Strip
            val goldPaint = Paint().apply {
                color = Color.parseColor("#F59E0B") // Amber Gold
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 111f, pageWidth.toFloat(), 115f, goldPaint)

            // Store Name
            paint.color = Color.WHITE
            paint.textSize = 24f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(invoiceData.storeName, (pageWidth / 2).toFloat(), 48f, paint)

            // Subtitle
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("فاتورة مبيعات إلكترونية معتمدة", (pageWidth / 2).toFloat(), 75f, paint)
            paint.textSize = 11f
            paint.color = Color.parseColor("#A7F3D0")
            canvas.drawText("AF Store POS & Management System", (pageWidth / 2).toFloat(), 95f, paint)

            // 2. Invoice Meta Cards (Left & Right)
            val dateFormat = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))
            val dateStr = dateFormat.format(Date(invoiceData.dateMillis))

            var yPos = 145f

            // Meta Background Box
            val metaBgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val metaRect = RectF(28f, yPos - 15f, (pageWidth - 28).toFloat(), yPos + 80f)
            canvas.drawRoundRect(metaRect, 12f, 12f, metaBgPaint)

            val metaBorderPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }
            canvas.drawRoundRect(metaRect, 12f, 12f, metaBorderPaint)

            // Column 1 (Right Aligned in Arabic)
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.RIGHT

            canvas.drawText("رقم الفاتورة: #${invoiceData.invoiceNumber}", (pageWidth - 45).toFloat(), yPos + 10f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#475569")
            canvas.drawText("التاريخ والوقت: $dateStr", (pageWidth - 45).toFloat(), yPos + 34f, paint)
            canvas.drawText("الكاشير المسؤول: ${invoiceData.cashierName}", (pageWidth - 45).toFloat(), yPos + 58f, paint)

            // Column 2 (Left Aligned details)
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            if (!invoiceData.customerName.isNullOrBlank()) {
                paint.color = Color.parseColor("#064E3B")
                canvas.drawText("العميل: ${invoiceData.customerName}", 45f, yPos + 10f, paint)
            } else {
                paint.color = Color.parseColor("#64748B")
                canvas.drawText("العميل: زبون عام (نقدي)", 45f, yPos + 10f, paint)
            }

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = if (invoiceData.isDebt) Color.parseColor("#DC2626") else Color.parseColor("#16A34A")
            val payStatus = if (invoiceData.isDebt) "طريقة السداد: آجل على الحساب (دين ⚠️)" else "طريقة السداد: نقدي كاش (مدفوع بالكامل 💵)"
            canvas.drawText(payStatus, 45f, yPos + 34f, paint)

            paint.color = Color.parseColor("#475569")
            canvas.drawText("حالة الفاتورة: معتمدة ومؤكدة ✅", 45f, yPos + 58f, paint)

            // 3. Table Header
            yPos += 115f
            val tableHeaderPaint = Paint().apply {
                color = Color.parseColor("#064E3B")
                style = Paint.Style.FILL
            }
            val thRect = RectF(28f, yPos - 18f, (pageWidth - 28).toFloat(), yPos + 16f)
            canvas.drawRoundRect(thRect, 8f, 8f, tableHeaderPaint)

            paint.color = Color.WHITE
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("الصنف والمواصفات", (pageWidth - 45).toFloat(), yPos, paint)

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("الكمية والعبوات", 280f, yPos, paint)
            canvas.drawText("سعر الوحدة", 165f, yPos, paint)

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("الإجمالي (ج.م)", 45f, yPos, paint)

            // 4. Table Rows
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val rowBgAlt = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
            }

            var rowIndex = 0

            for (item in invoiceData.items) {
                yPos += 36f
                if (rowIndex % 2 == 1) {
                    canvas.drawRect(28f, yPos - 24f, (pageWidth - 28).toFloat(), yPos + 12f, rowBgAlt)
                }

                // Item Name
                paint.color = Color.parseColor("#0F172A")
                paint.textAlign = Paint.Align.RIGHT
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val name = if (item.name.length > 28) item.name.take(26) + ".." else item.name
                canvas.drawText(name, (pageWidth - 45).toFloat(), yPos - 4f, paint)

                // Secondary line for barcode / carton note if any
                paint.textSize = 9f
                paint.color = Color.parseColor("#64748B")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                if (!item.barcode.isNullOrBlank()) {
                    canvas.drawText("باركود: ${item.barcode}", (pageWidth - 45).toFloat(), yPos + 8f, paint)
                }

                // Quantity & Carton detail
                paint.textAlign = Paint.Align.CENTER
                paint.color = Color.parseColor("#0F172A")
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val qtyText = if (item.unitsPerCarton > 1 && item.cartonCount > 0) {
                    "${item.cartonCount} عبوة + ${item.looseCount} ق (${item.quantity})"
                } else {
                    "${item.quantity} قطعة"
                }
                canvas.drawText(qtyText, 280f, yPos - 2f, paint)

                // Unit Price
                paint.color = Color.parseColor("#334155")
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.unitPrice), 165f, yPos - 2f, paint)

                // Total Price
                paint.textAlign = Paint.Align.LEFT
                paint.color = Color.parseColor("#0F172A")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.totalPrice), 45f, yPos - 2f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                canvas.drawLine(28f, yPos + 12f, (pageWidth - 28).toFloat(), yPos + 12f, linePaint)
                rowIndex++
            }

            // 5. Financial Summary Card Box
            yPos += 35f
            val summaryCardRect = RectF(28f, yPos, (pageWidth - 28).toFloat(), yPos + 115f)
            val summaryCardBg = Paint().apply {
                color = Color.parseColor("#F1F5F9")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(summaryCardRect, 12f, 12f, summaryCardBg)
            canvas.drawRoundRect(summaryCardRect, 12f, 12f, metaBorderPaint)

            // Right column of summary: counts & stats
            paint.textAlign = Paint.Align.RIGHT
            paint.color = Color.parseColor("#475569")
            paint.textSize = 12f
            val cartonsSummary = if (invoiceData.totalCartonsCount > 0) " (${invoiceData.totalCartonsCount} عبوة)" else ""
            canvas.drawText("إجمالي الأصناف المباعة: ${invoiceData.totalItemsCount} صنف [${invoiceData.totalQuantitySold} قطعة]$cartonsSummary", (pageWidth - 45).toFloat(), yPos + 26f, paint)
            canvas.drawText("المجموع الفرعي قبل الخصومات:", (pageWidth - 45).toFloat(), yPos + 48f, paint)
            if (invoiceData.discountPercent > 0 || invoiceData.discountAmount > 0) {
                paint.color = Color.parseColor("#DC2626")
                canvas.drawText(String.format(Locale.getDefault(), "الخصم المطبق (%.0f%%):", invoiceData.discountPercent), (pageWidth - 45).toFloat(), yPos + 70f, paint)
            }

            // Left column of summary: amounts & total
            paint.textAlign = Paint.Align.LEFT
            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f ج.م", invoiceData.subtotal), 45f, yPos + 48f, paint)
            if (invoiceData.discountPercent > 0 || invoiceData.discountAmount > 0) {
                paint.color = Color.parseColor("#DC2626")
                canvas.drawText(String.format(Locale.getDefault(), "-%.2f ج.م", invoiceData.discountAmount), 45f, yPos + 70f, paint)
            }

            // Grand Total Ribbon
            val grandTotalRect = RectF(28f, yPos + 80f, (pageWidth - 28).toFloat(), yPos + 122f)
            val grandTotalBg = Paint().apply {
                color = Color.parseColor("#064E3B")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(grandTotalRect, 8f, 8f, grandTotalBg)

            paint.color = Color.WHITE
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("المبلغ الإجمالي النهائي المستحق:", (pageWidth - 45).toFloat(), yPos + 106f, paint)

            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 15f
            canvas.drawText(String.format(Locale.getDefault(), "%.2f ج.م", invoiceData.finalTotal), 45f, yPos + 106f, paint)

            // 6. Footer Notice & Verification Line
            yPos += 155f

            val dashedPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                pathEffect = DashPathEffect(floatArrayOf(6f, 6f), 0f)
            }
            canvas.drawLine(28f, yPos, (pageWidth - 28).toFloat(), yPos, dashedPaint)

            yPos += 22f
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("شكراً لتسوقكم واختياركم ${invoiceData.storeName} - يسعدنا دائماً خدمتكم!", (pageWidth / 2).toFloat(), yPos, paint)

            yPos += 16f
            paint.textSize = 10f
            paint.color = Color.parseColor("#94A3B8")
            canvas.drawText("رقم الفاتورة: #${invoiceData.invoiceNumber} | تم التصدير عبر نظام AF Store المعتمد", (pageWidth / 2).toFloat(), yPos, paint)

            pdfDoc.finishPage(page)

            // Save PDF to cache dir
            val invoicesDir = File(context.cacheDir, "invoices")
            if (!invoicesDir.exists()) invoicesDir.mkdirs()
            val pdfFile = File(invoicesDir, "Invoice_${invoiceData.invoiceNumber}.pdf")
            val fos = FileOutputStream(pdfFile)
            pdfDoc.writeTo(fos)
            fos.close()
            pdfDoc.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Helper to generate and immediately trigger the share sheet for an invoice
     */
    fun shareInvoiceAsPdf(context: Context, invoiceData: InvoicePreviewData) {
        val file = createPdfFromInvoiceData(context, invoiceData)
        if (file != null && file.exists()) {
            sharePdfFile(context, file, "مشاركة فاتورة #${invoiceData.invoiceNumber}")
        } else {
            Toast.makeText(context, "تعذر إنشاء ملف PDF للفاتورة", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share PDF file directly via WhatsApp, Email, Telegram, Bluetooth, etc.
     */
    fun sharePdfFile(context: Context, file: File, title: String = "مشاركة فاتورة المبيعات") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newRawUri("Invoice PDF", uri)
                putExtra(Intent.EXTRA_SUBJECT, "فاتورة مبيعات ${file.nameWithoutExtension}")
                putExtra(Intent.EXTRA_TEXT, "مرفق فاتورة المبيعات الإلكترونية بصيغة PDF من AF store.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "تعذر مشاركة الملف: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open / View PDF with any installed PDF viewer
     */
    fun viewPdfFile(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                clipData = android.content.ClipData.newRawUri("Invoice PDF", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "عرض الفاتورة").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "لا يوجد تطبيق لعرض ملفات PDF", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Print PDF using Android System Print Framework (to Wi-Fi / Mopria / Cloud Printers / Save as PDF)
     */
    fun printPdfViaSystem(context: Context, file: File, jobName: String = "AF_Store_Invoice") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter: PrintDocumentAdapter = PdfPrintAdapter(file)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print(jobName, printAdapter, printAttributes)
            } else {
                Toast.makeText(context, "خدمة الطباعة غير متوفرة على هذا الجهاز", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطأ في تشغيل الطباعة: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share plain text receipt directly
     */
    fun shareTextReceipt(context: Context, receiptText: String, title: String = "مشاركة الإيصال") {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, receiptText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
