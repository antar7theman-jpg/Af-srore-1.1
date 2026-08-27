package com.example.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.InvoiceStyle
import com.example.data.models.Sale
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.SuccessGreen
import com.example.utils.BluetoothPrinterManager
import com.example.utils.PdfInvoiceHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data representation of a line item in the invoice preview
 */
data class InvoicePreviewItem(
    val productId: Int,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val unitsPerCarton: Int = 1,
    val barcode: String? = null,
    val cartonBarcode: String? = null
) {
    val cartonCount: Int get() = if (unitsPerCarton > 1) quantity / unitsPerCarton else 0
    val looseCount: Int get() = if (unitsPerCarton > 1) quantity % unitsPerCarton else quantity
}

/**
 * Complete data model for invoice preview before saving/printing
 */
data class InvoicePreviewData(
    val invoiceNumber: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val cashierName: String,
    val customerName: String? = null,
    val isDebt: Boolean = false,
    val paymentMethod: String = "CASH",
    val storeName: String = "AF store للتجارة والتوزيع",
    val items: List<InvoicePreviewItem>,
    val subtotal: Double,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val finalTotal: Double,
    val isDraft: Boolean = true // true = preview before save, false = finalized
) {
    val totalItemsCount: Int get() = items.size
    val totalQuantitySold: Int get() = items.sumOf { it.quantity }
    val totalCartonsCount: Int get() = items.sumOf { it.cartonCount }
}

@Composable
fun InvoicePreviewDialog(
    invoiceData: InvoicePreviewData,
    initialStyle: InvoiceStyle = InvoiceStyle.DETAILED,
    onDismiss: () -> Unit,
    onConfirmAndSave: (() -> Unit)? = null,
    onPrintSuccess: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }
    var selectedStyle by remember {
        mutableStateOf(BluetoothPrinterManager.getSavedInvoiceStyle(context) ?: initialStyle)
    }

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar")) }
    val formattedDate = remember(invoiceData.dateMillis) { dateFormat.format(Date(invoiceData.dateMillis)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .padding(vertical = 10.dp)
                .testTag("invoice_preview_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // 1. Dialog Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = if (invoiceData.isDraft) MaterialTheme.colorScheme.primaryContainer else SuccessGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (invoiceData.isDraft) Icons.Default.Visibility else Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = if (invoiceData.isDraft) MaterialTheme.colorScheme.primary else SuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (invoiceData.isDraft) "معاينة الفاتورة قبل الحفظ" else "فاتورة مبيعات معتمدة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (invoiceData.isDraft) "شاملة تفاصيل العبوات، الوحدات، والخصومات" else "تم الحفظ بنجاح وجاهزة للطباعة الحرارية",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                PdfInvoiceHelper.shareInvoiceAsPdf(context, invoiceData)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("invoice_top_share_pdf_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "مشاركة PDF",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Invoice Style Selector Bar (تغيير شكل الفاتورة)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "شكل الفاتورة:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            InvoiceStyle.values().forEach { style ->
                                val isSelected = selectedStyle == style
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .clickable {
                                            selectedStyle = style
                                            BluetoothPrinterManager.saveInvoiceStyle(context, style)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(text = style.iconEmoji, fontSize = 11.sp)
                                        Text(
                                            text = when (style) {
                                                InvoiceStyle.DETAILED -> "مفصل (عبوات)"
                                                InvoiceStyle.SIMPLE -> "مبسط"
                                                InvoiceStyle.THERMAL_POS -> "حراري POS"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Scrollable Realistic Thermal Paper Container
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("invoice_paper_content"),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFAF8F5), // Realistic off-white thermal paper tint
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Store Header & Logo
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = invoiceData.storeName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = when (selectedStyle) {
                                        InvoiceStyle.DETAILED -> "فاتورة مبيعات تجارية مفصلة (عبوات ووحدات)"
                                        InvoiceStyle.SIMPLE -> "إيصال مبيعات مبسط"
                                        InvoiceStyle.THERMAL_POS -> "POS RECEIPT - إيصال مبيعات حراري"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Status Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (invoiceData.isDraft) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text(
                                        text = if (invoiceData.isDraft) "📝 مسودة مبيعات للمراجعة" else "✅ فاتورة مؤكدة ومسجلة",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (invoiceData.isDraft) Color(0xFF92400E) else Color(0xFF166534),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }

                                // Invoice Metadata Box
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF1F5F9),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "رقم الفاتورة:", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Text(
                                                text = "#${invoiceData.invoiceNumber}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "التاريخ والوقت:", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Text(text = formattedDate, fontSize = 11.sp, color = Color(0xFF0F172A))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "الكاشير المسؤول:", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Text(text = invoiceData.cashierName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                        }
                                        if (invoiceData.customerName != null) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = "الزبون / العميل:", fontSize = 11.sp, color = Color(0xFF64748B))
                                                Text(text = invoiceData.customerName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "طريقة الدفع:", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Text(
                                                text = if (invoiceData.isDebt) "آجل على الحساب (دين) ⚠️" else "نقدي (Cash) 💵",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (invoiceData.isDebt) DangerRed else SuccessGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Items Table Header
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الصنف",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF334155),
                                        modifier = Modifier.weight(2f)
                                    )
                                    Text(
                                        text = "سعر الوحدة",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF334155),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1.3f)
                                    )
                                    Text(
                                        text = if (selectedStyle == InvoiceStyle.DETAILED) "العبوات / الوحدات" else "الكمية",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF334155),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1.5f)
                                    )
                                    Text(
                                        text = "الإجمالي",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF334155),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                            }
                        }

                        // Items List Rows
                        items(invoiceData.items) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Item name
                                    Column(modifier = Modifier.weight(2f)) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF0F172A),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (item.unitsPerCarton > 1) {
                                            Text(
                                                text = "سعة العبوة: ${item.unitsPerCarton} قطعة",
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    // Unit Price
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.2f", item.unitPrice),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1.3f)
                                    )

                                    // Qty / Cartons Breakdown
                                    Column(
                                        modifier = Modifier.weight(1.5f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        if (item.unitsPerCarton > 1 && item.cartonCount > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            ) {
                                                Text(
                                                    text = "${item.cartonCount} عبوة 📦",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                            Text(
                                                text = "(${item.quantity} قطعة)",
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        } else {
                                            Text(
                                                text = "${item.quantity} قطعة",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    }

                                    // Line Total
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.2f", item.totalPrice),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 4.dp),
                                    color = Color(0xFFE2E8F0),
                                    thickness = 0.5.dp
                                )
                            }
                        }

                        // Financial Summary Box (Subtotal, Cartons, Discount, Grand Total)
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Total Items & Units
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "إجمالي الأصناف والقطع:", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(
                                            text = "${invoiceData.totalItemsCount} أصناف (${invoiceData.totalQuantitySold} قطعة)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    // Total Cartons count
                                    if (invoiceData.totalCartonsCount > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "إجمالي عدد العبوات الكبرى:", fontSize = 12.sp, color = Color(0xFF64748B))
                                            Text(
                                                text = "${invoiceData.totalCartonsCount} عبوة 📦",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Subtotal
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "المجموع الفرعي:", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(text = String.format(Locale.getDefault(), "%.2f ج.م", invoiceData.subtotal), fontSize = 12.sp, color = Color(0xFF0F172A))
                                    }

                                    // Discount if present
                                    if (invoiceData.discountPercent > 0 || invoiceData.discountAmount > 0) {
                                        val discVal = if (invoiceData.discountAmount > 0) invoiceData.discountAmount else (invoiceData.subtotal * (invoiceData.discountPercent / 100.0))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (invoiceData.discountPercent > 0) "الخصم المطبق (${invoiceData.discountPercent.toInt()}%):" else "قيمة الخصم:",
                                                fontSize = 12.sp,
                                                color = DangerRed
                                            )
                                            Text(
                                                text = String.format(Locale.getDefault(), "-%.2f ج.م", discVal),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = DangerRed
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = Color(0xFFCBD5E1),
                                        thickness = 1.dp
                                    )

                                    // Grand Final Total
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "المبلغ الإجمالي المستحق:",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.2f ج.م", invoiceData.finalTotal),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Barcode & Footer
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier
                                        .width(180.dp)
                                        .height(26.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5, 8, 9, 7).forEach { width ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .width(width.dp)
                                                    .background(Color.White)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "* ${invoiceData.invoiceNumber} *",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "شكراً لتعاملكم معنا ونرحب بزيارتكم دائماً!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = "خدمة العملاء والاستفسار: 01012345678",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                // Printing progress indicator
                if (isPrinting) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "جاري الإرسال للطابعة الحرارية (${selectedStyle.name})...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Bottom Action Buttons: Print via Thermal Printer & Complete Sale & Share PDF
                if (invoiceData.isDraft) {
                    // Draft Actions: Print via Thermal Printer, Complete Sale, Edit, Share PDF
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Thermal Printer Button
                            Button(
                                onClick = {
                                    isPrinting = true
                                    BluetoothPrinterManager.printInvoice(
                                        context = context,
                                        invoice = invoiceData,
                                        coroutineScope = coroutineScope,
                                        style = selectedStyle
                                    ) { _, _ ->
                                        isPrinting = false
                                        onPrintSuccess?.invoke()
                                        onConfirmAndSave?.invoke()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("invoice_thermal_print_btn"),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isPrinting
                            ) {
                                if (isPrinting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("جاري الطباعة...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("طبع الفاتورة الحرارية", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // 2. Complete Sale Button
                            FilledTonalButton(
                                onClick = {
                                    onConfirmAndSave?.invoke()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("invoice_confirm_save_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إتمام البيع", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            // 3. Edit Button
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("invoice_edit_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تعديل", fontSize = 12.sp)
                            }
                        }

                        // Share as PDF Button
                        OutlinedButton(
                            onClick = {
                                PdfInvoiceHelper.shareInvoiceAsPdf(context, invoiceData)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("invoice_draft_share_pdf_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مشاركة الفاتورة كـ PDF 📄 (واتساب / بريد)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    // Finalized Invoice Actions: Print Thermal Receipt, Complete Sale (Done), Share PDF
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Direct Thermal Printer Button
                            Button(
                                onClick = {
                                    isPrinting = true
                                    BluetoothPrinterManager.printInvoice(
                                        context = context,
                                        invoice = invoiceData,
                                        coroutineScope = coroutineScope,
                                        style = selectedStyle
                                    ) { _, _ ->
                                        isPrinting = false
                                        onPrintSuccess?.invoke()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp)
                                    .testTag("finalized_thermal_print_btn"),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isPrinting
                            ) {
                                if (isPrinting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("جاري الطباعة...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("طبع الفاتورة الحرارية", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // 2. Complete / Finish Button
                            FilledTonalButton(
                                onClick = {
                                    onConfirmAndSave?.invoke() ?: onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("finalized_complete_sale_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = SuccessGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إتمام البيع", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Share as PDF Button
                        OutlinedButton(
                            onClick = {
                                PdfInvoiceHelper.shareInvoiceAsPdf(context, invoiceData)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("finalized_share_pdf_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مشاركة الفاتورة كـ PDF 📄 (واتساب / بريد)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
