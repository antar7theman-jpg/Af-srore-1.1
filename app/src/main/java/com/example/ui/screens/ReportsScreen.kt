package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Sale
import com.example.ui.components.*
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodels.AntarSalesViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class DashboardViewTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DAILY("اليومي واللحظي", Icons.Default.Timeline),
    MONTHLY("المقارنة الشهرية", Icons.Default.CalendarMonth),
    CATEGORIES("الأقسام والمدفوعات", Icons.Default.PieChart)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: AntarSalesViewModel,
    modifier: Modifier = Modifier
) {
    var selectedDashboardTab by remember { mutableStateOf(DashboardViewTab.DAILY) }
    var selectedPreset by remember { mutableStateOf(DateFilterPreset.LAST_7_DAYS) }
    var customStartMillis by remember { mutableStateOf<Long?>(null) }
    var customEndMillis by remember { mutableStateOf<Long?>(null) }
    var showCustomDateDialog by remember { mutableStateOf(false) }

    var chartMode by remember { mutableStateOf(DashboardChartMode.DUAL_BAR) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var selectedMonthIndex by remember { mutableStateOf<Int?>(null) }
    var previewInvoiceSale by remember { mutableStateOf<Sale?>(null) }

    val isLoadingReports by viewModel.isLoadingReports.collectAsState()
    val allSales by viewModel.allSales.collectAsState()
    val products by viewModel.products.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadReports()
    }

    // Compute active date range selection
    val activeDateRange = remember(selectedPreset, customStartMillis, customEndMillis) {
        DateRangeHelper.getRangeForPreset(
            preset = selectedPreset,
            customStart = customStartMillis,
            customEnd = customEndMillis
        )
    }

    // Reset selected chart point when date range changes
    LaunchedEffect(activeDateRange) {
        selectedPointIndex = null
    }

    // Compute daily financial data points
    val dailyPoints: List<DailyFinancialPoint> = remember(allSales, products, activeDateRange) {
        FinancialAnalyticsHelper.computeDailyPoints(
            sales = allSales,
            products = products,
            startMillis = activeDateRange.startMillis,
            endMillis = activeDateRange.endMillis
        )
    }

    // Compute monthly financial data points (12 months of current year)
    val monthlyPoints: List<MonthlyFinancialPoint> = remember(allSales, products) {
        FinancialAnalyticsHelper.computeMonthlyPoints(
            sales = allSales,
            products = products
        )
    }

    // Compute hourly financial data points for today
    val hourlyPoints: List<HourlyFinancialPoint> = remember(allSales, products) {
        FinancialAnalyticsHelper.computeHourlyPointsForToday(
            sales = allSales,
            products = products
        )
    }

    // Compute category financial shares
    val categoryShares: List<CategoryFinancialShare> = remember(allSales, products, activeDateRange) {
        FinancialAnalyticsHelper.computeCategoryShares(
            sales = allSales,
            products = products,
            startMillis = activeDateRange.startMillis,
            endMillis = activeDateRange.endMillis
        )
    }

    // Compute payment method shares
    val paymentShares: List<PaymentMethodShare> = remember(allSales, activeDateRange) {
        FinancialAnalyticsHelper.computePaymentMethodShares(
            sales = allSales,
            startMillis = activeDateRange.startMillis,
            endMillis = activeDateRange.endMillis
        )
    }

    // Compute top profitable products in the active range
    val topProducts: List<ProductPerformance> = remember(allSales, products, activeDateRange) {
        FinancialAnalyticsHelper.computeTopProducts(
            sales = allSales,
            products = products,
            startMillis = activeDateRange.startMillis,
            endMillis = activeDateRange.endMillis
        )
    }

    // Filtered sales in the active date range
    val filteredSales = remember(allSales, activeDateRange) {
        allSales.filter { it.saleDate in activeDateRange.startMillis..activeDateRange.endMillis }
    }

    val productMap = remember(products) { products.associateBy { it.id } }

    val totalSales = remember(filteredSales) { filteredSales.sumOf { it.totalPrice } }
    val totalCost = remember(filteredSales, productMap) {
        filteredSales.sumOf { sale ->
            val p = productMap[sale.productId]
            sale.quantitySold * (p?.purchasePrice ?: 0.0)
        }
    }
    val totalProfit = remember(totalSales, totalCost) { totalSales - totalCost }
    val profitMarginPercent = remember(totalSales, totalProfit) {
        if (totalSales > 0) (totalProfit / totalSales) * 100.0 else 0.0
    }
    val totalInvoices = remember(filteredSales) { filteredSales.size }
    val avgOrderValue = remember(totalSales, totalInvoices) {
        if (totalInvoices > 0) totalSales / totalInvoices else 0.0
    }

    AnimatedContent(
        targetState = isLoadingReports,
        transitionSpec = {
            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                    fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        },
        label = "reports_loading_anim"
    ) { loading ->
        if (loading) {
            ReportsScreenSkeleton(modifier = modifier)
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("reports_screen"),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Header & Title Banner
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "📊 لوحة تحكم أداء المتجر (Dashboard)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "إحصائيات المبيعات اليومية والشهرية والرسوم البيانية التفاعلية",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { viewModel.loadReports() },
                            modifier = Modifier.testTag("refresh_dashboard_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث البيانات",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 2. View Mode Selector Tabs (Daily, Monthly, Categories)
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DashboardViewTab.values().forEach { tab ->
                                val isSelected = selectedDashboardTab == tab
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null,
                                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedDashboardTab = tab }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tab.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                when (selectedDashboardTab) {
                    DashboardViewTab.DAILY -> {
                        // 3. Date Range Filter Row
                        item {
                            DateRangeFilterRow(
                                selectedPreset = selectedPreset,
                                activeRangeLabel = activeDateRange.label,
                                onSelectPreset = { preset ->
                                    selectedPreset = preset
                                },
                                onOpenCustomPicker = {
                                    showCustomDateDialog = true
                                }
                            )
                        }

                        // 4. KPI Summary Cards Grid
                        item {
                            DashboardKpiSection(
                                totalSales = totalSales,
                                totalProfit = totalProfit,
                                totalCost = totalCost,
                                totalInvoices = totalInvoices,
                                avgOrderValue = avgOrderValue,
                                profitMarginPercent = profitMarginPercent
                            )
                        }

                        // 5. Interactive Dual-Series Chart Card (Sales Volume vs Realized Profit)
                        item {
                            DashboardDualChartCard(
                                title = when (selectedPreset) {
                                    DateFilterPreset.TODAY -> "حجم المبيعات والأرباح لليوم"
                                    DateFilterPreset.LAST_7_DAYS -> "المبيعات والأرباح خلال آخر 7 أيام"
                                    DateFilterPreset.LAST_30_DAYS -> "المبيعات والأرباح خلال آخر 30 يوم"
                                    DateFilterPreset.THIS_MONTH -> "المبيعات والأرباح خلال هذا الشهر"
                                    DateFilterPreset.LAST_3_MONTHS -> "المبيعات والأرباح خلال 3 أشهر"
                                    DateFilterPreset.THIS_YEAR -> "المبيعات والأرباح السنوية"
                                    DateFilterPreset.CUSTOM -> "المبيعات والأرباح خلال النطاق المخصص"
                                },
                                points = dailyPoints,
                                chartMode = chartMode,
                                selectedIndex = selectedPointIndex,
                                onChartModeChange = { chartMode = it },
                                onSelectIndex = { selectedPointIndex = it },
                                onRefresh = { viewModel.loadReports() }
                            )
                        }

                        // 6. Hourly Traffic Progression Chart for Today
                        item {
                            DashboardHourlyTrafficCard(hourlyPoints = hourlyPoints)
                        }

                        // 7. Top Profitable Products in this Date Period
                        item {
                            TopProfitableProductsSection(topProducts = topProducts)
                        }

                        // 8. Day-by-Day Financial Breakdown Table
                        item {
                            DailyBreakdownTable(dailyPoints = dailyPoints)
                        }

                        // 9. Recent Sales Transactions in Filtered Period
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⏱️ عمليات البيع المنفذة خلال النطاق (${filteredSales.size} عملية)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        if (filteredSales.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "لا توجد عمليات بيع مسجلة في هذه الفترة",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredSales.take(20), key = { it.saleId }) { sale ->
                                val matchedProd = productMap[sale.productId]
                                val purchaseCost = sale.quantitySold * (matchedProd?.purchasePrice ?: 0.0)
                                val saleProfit = sale.totalPrice - purchaseCost

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { previewInvoiceSale = sale },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.ReceiptLong,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = sale.productName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(sale.saleDate))
                                                Text(
                                                    text = "$dateStr • ${sale.quantitySold} قطعة • ربح: +${String.format(Locale.getDefault(), "%.1f", saleProfit)} ج.م",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = String.format(Locale.getDefault(), "+%.2f ج.م", sale.totalPrice),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ChevronLeft,
                                                contentDescription = "معاينة الفاتورة",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DashboardViewTab.MONTHLY -> {
                        // Monthly Analytics View
                        item {
                            val ytdSales = monthlyPoints.sumOf { it.salesVolume }
                            val ytdProfit = monthlyPoints.sumOf { it.profit }
                            val ytdCost = monthlyPoints.sumOf { it.costVolume }
                            val ytdInvoices = monthlyPoints.sumOf { it.invoicesCount }
                            val ytdMargin = if (ytdSales > 0) (ytdProfit / ytdSales) * 100.0 else 0.0

                            DashboardKpiSection(
                                totalSales = ytdSales,
                                totalProfit = ytdProfit,
                                totalCost = ytdCost,
                                totalInvoices = ytdInvoices,
                                avgOrderValue = if (ytdInvoices > 0) ytdSales / ytdInvoices else 0.0,
                                profitMarginPercent = ytdMargin
                            )
                        }

                        item {
                            DashboardMonthlyChartCard(
                                monthlyPoints = monthlyPoints,
                                selectedMonthIndex = selectedMonthIndex,
                                onSelectMonth = { selectedMonthIndex = it }
                            )
                        }

                        item {
                            MonthlyBreakdownTable(monthlyPoints = monthlyPoints)
                        }
                    }

                    DashboardViewTab.CATEGORIES -> {
                        // Category & Payment Distribution View
                        item {
                            DashboardCategoryDonutCard(categoryShares = categoryShares)
                        }

                        item {
                            DashboardPaymentMethodCard(paymentShares = paymentShares)
                        }

                        item {
                            val totalInventoryCost = products.sumOf { it.purchasePrice * it.stock }
                            val totalInventorySaleValue = products.sumOf { it.price * it.stock }
                            val totalProjectedProfit = totalInventorySaleValue - totalInventoryCost

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "📦 تقييم بضاعة المخزن الإجمالي (شراء vs بيع)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("تكلفة الشراء الكلية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = String.format(Locale.getDefault(), "%.2f ج.م", totalInventoryCost),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("قيمة البيع المتوقعة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = String.format(Locale.getDefault(), "%.2f ج.م", totalInventorySaleValue),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("الربح المتوقع بالمخزن", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = String.format(Locale.getDefault(), "+%.2f ج.م", totalProjectedProfit),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = SuccessGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            TopProfitableProductsSection(topProducts = topProducts)
                        }
                    }
                }
            }
        }
    }

    // Custom Date Range Picker Dialog
    if (showCustomDateDialog) {
        CustomDateRangePickerDialog(
            initialStartMillis = customStartMillis ?: activeDateRange.startMillis,
            initialEndMillis = customEndMillis ?: activeDateRange.endMillis,
            onDismiss = { showCustomDateDialog = false },
            onConfirm = { start, end ->
                customStartMillis = start
                customEndMillis = end
                selectedPreset = DateFilterPreset.CUSTOM
                showCustomDateDialog = false
            }
        )
    }

    // Invoice Preview Dialog for Historical Sale
    previewInvoiceSale?.let { sale ->
        val matchedProd = products.find { it.id == sale.productId }
        val item = InvoicePreviewItem(
            productId = sale.productId,
            name = sale.productName,
            quantity = sale.quantitySold,
            unitPrice = if (sale.quantitySold > 0) sale.totalPrice / sale.quantitySold else sale.totalPrice,
            totalPrice = sale.totalPrice,
            unitsPerCarton = matchedProd?.unitsPerCarton ?: 1,
            barcode = matchedProd?.barcode
        )
        val invoiceNum = (sale.saleDate % 1000000).toString()
        val invoiceData = InvoicePreviewData(
            invoiceNumber = invoiceNum,
            dateMillis = sale.saleDate,
            cashierName = currentUser?.username ?: "المدير",
            items = listOf(item),
            subtotal = sale.totalPrice,
            finalTotal = sale.totalPrice,
            isDraft = false
        )

        InvoicePreviewDialog(
            invoiceData = invoiceData,
            onDismiss = { previewInvoiceSale = null },
            onConfirmAndSave = { previewInvoiceSale = null },
            onPrintSuccess = { viewModel.showMessage("🖨️ تمت إعادة طباعة الفاتورة بنجاح") }
        )
    }
}
