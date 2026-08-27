package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Product
import com.example.data.models.Sale
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.*

enum class DashboardChartMode(val label: String) {
    DUAL_BAR("أعمدة مقارنة"),
    SMOOTH_AREA("منحنى مساحي"),
    MARGIN_TREND("هامش الربح %")
}

data class DailyFinancialPoint(
    val dayKey: String,
    val shortLabel: String,
    val fullDateLabel: String,
    val salesVolume: Double,
    val costVolume: Double,
    val profit: Double,
    val marginPercent: Double,
    val invoicesCount: Int,
    val unitsSold: Int
)

data class MonthlyFinancialPoint(
    val monthKey: String, // e.g. "2026-08"
    val monthIndex: Int,  // 0 to 11
    val shortLabel: String, // "أغسطس"
    val fullMonthLabel: String, // "أغسطس 2026"
    val salesVolume: Double,
    val costVolume: Double,
    val profit: Double,
    val marginPercent: Double,
    val invoicesCount: Int,
    val unitsSold: Int,
    val momGrowthPercent: Double? = null
)

data class HourlyFinancialPoint(
    val hour: Int,
    val label: String, // "09:00"
    val salesVolume: Double,
    val profit: Double,
    val invoicesCount: Int,
    val unitsSold: Int
)

data class CategoryFinancialShare(
    val categoryName: String,
    val totalSales: Double,
    val totalProfit: Double,
    val unitsSold: Int,
    val percentage: Double,
    val color: Color
)

data class PaymentMethodShare(
    val methodName: String,
    val totalAmount: Double,
    val count: Int,
    val percentage: Double,
    val color: Color
)

data class ProductPerformance(
    val productId: Int,
    val productName: String,
    val quantitySold: Int,
    val totalRevenue: Double,
    val totalCost: Double,
    val totalProfit: Double,
    val profitMarginPercent: Double
)

object FinancialAnalyticsHelper {
    private val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val shortDisplayFormat = SimpleDateFormat("MM/dd", Locale.US)
    private val fullDisplayFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ar"))
    private val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val arabicMonthNames = listOf(
        "يناير", "فبراير", "مارس", "إبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    private val chartColors = listOf(
        Color(0xFF2563EB), // Blue
        Color(0xFF10B981), // Emerald
        Color(0xFF8B5CF6), // Purple
        Color(0xFFF59E0B), // Amber
        Color(0xFFEC4899), // Pink
        Color(0xFF06B6D4), // Cyan
        Color(0xFF14B8A6), // Teal
        Color(0xFF6366F1)  // Indigo
    )

    fun computeDailyPoints(
        sales: List<Sale>,
        products: List<Product>,
        startMillis: Long,
        endMillis: Long
    ): List<DailyFinancialPoint> {
        val productMap = products.associateBy { it.id }
        val filteredSales = sales.filter { it.saleDate in startMillis..endMillis }

        // Group sales by dayKey
        val groupedSales = filteredSales.groupBy { sale ->
            dayKeyFormat.format(Date(sale.saleDate))
        }

        // Build continuous calendar days between start and end (capped at 90 days for visual clarity)
        val result = mutableListOf<DailyFinancialPoint>()
        val cal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }

        val maxDays = 90
        var dayCount = 0

        while (!cal.after(endCal) && dayCount < maxDays) {
            val key = dayKeyFormat.format(cal.time)
            val daySales = groupedSales[key] ?: emptyList()

            var dayRevenue = 0.0
            var dayCost = 0.0
            var dayUnits = 0

            for (sale in daySales) {
                dayRevenue += sale.totalPrice
                dayUnits += sale.quantitySold
                val prod = productMap[sale.productId]
                val purchasePrice = prod?.purchasePrice ?: 0.0
                dayCost += (sale.quantitySold * purchasePrice)
            }

            val dayProfit = dayRevenue - dayCost
            val margin = if (dayRevenue > 0) (dayProfit / dayRevenue) * 100.0 else 0.0

            result.add(
                DailyFinancialPoint(
                    dayKey = key,
                    shortLabel = shortDisplayFormat.format(cal.time),
                    fullDateLabel = fullDisplayFormat.format(cal.time),
                    salesVolume = dayRevenue,
                    costVolume = dayCost,
                    profit = dayProfit,
                    marginPercent = margin,
                    invoicesCount = daySales.size,
                    unitsSold = dayUnits
                )
            )

            cal.add(Calendar.DAY_OF_YEAR, 1)
            dayCount++
        }

        return result
    }

    fun computeMonthlyPoints(
        sales: List<Sale>,
        products: List<Product>,
        year: Int = Calendar.getInstance().get(Calendar.YEAR)
    ): List<MonthlyFinancialPoint> {
        val productMap = products.associateBy { it.id }
        val result = mutableListOf<MonthlyFinancialPoint>()

        var previousMonthSales: Double? = null

        for (monthIdx in 0..11) {
            val startCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthIdx)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthIdx)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            val monthSales = sales.filter { it.saleDate in startCal.timeInMillis..endCal.timeInMillis }
            var revenue = 0.0
            var cost = 0.0
            var units = 0

            for (sale in monthSales) {
                revenue += sale.totalPrice
                units += sale.quantitySold
                val prod = productMap[sale.productId]
                val purchasePrice = prod?.purchasePrice ?: 0.0
                cost += (sale.quantitySold * purchasePrice)
            }

            val profit = revenue - cost
            val margin = if (revenue > 0) (profit / revenue) * 100.0 else 0.0

            val momGrowth = if (previousMonthSales != null && previousMonthSales!! > 0) {
                ((revenue - previousMonthSales!!) / previousMonthSales!!) * 100.0
            } else null

            previousMonthSales = if (revenue > 0) revenue else previousMonthSales

            val monthName = arabicMonthNames.getOrElse(monthIdx) { "شهر ${monthIdx + 1}" }
            val monthKey = String.format(Locale.US, "%d-%02d", year, monthIdx + 1)

            result.add(
                MonthlyFinancialPoint(
                    monthKey = monthKey,
                    monthIndex = monthIdx,
                    shortLabel = monthName,
                    fullMonthLabel = "$monthName $year",
                    salesVolume = revenue,
                    costVolume = cost,
                    profit = profit,
                    marginPercent = margin,
                    invoicesCount = monthSales.size,
                    unitsSold = units,
                    momGrowthPercent = momGrowth
                )
            )
        }

        return result
    }

    fun computeHourlyPointsForToday(
        sales: List<Sale>,
        products: List<Product>
    ): List<HourlyFinancialPoint> {
        val productMap = products.associateBy { it.id }
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val todaySales = sales.filter { it.saleDate in startOfToday..endOfToday }
        val result = mutableListOf<HourlyFinancialPoint>()

        // From 8:00 AM to 11:00 PM (typical retail store hours)
        for (hour in 8..23) {
            val hourSales = todaySales.filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.saleDate }
                cal.get(Calendar.HOUR_OF_DAY) == hour
            }

            var rev = 0.0
            var cost = 0.0
            var units = 0

            for (s in hourSales) {
                rev += s.totalPrice
                units += s.quantitySold
                val prod = productMap[s.productId]
                cost += (s.quantitySold * (prod?.purchasePrice ?: 0.0))
            }

            result.add(
                HourlyFinancialPoint(
                    hour = hour,
                    label = String.format(Locale.US, "%02d:00", hour),
                    salesVolume = rev,
                    profit = rev - cost,
                    invoicesCount = hourSales.size,
                    unitsSold = units
                )
            )
        }

        return result
    }

    fun computeCategoryShares(
        sales: List<Sale>,
        products: List<Product>,
        startMillis: Long,
        endMillis: Long
    ): List<CategoryFinancialShare> {
        val productMap = products.associateBy { it.id }
        val filteredSales = sales.filter { it.saleDate in startMillis..endMillis }
        val totalRevenue = filteredSales.sumOf { it.totalPrice }.coerceAtLeast(0.001)

        // If products don't have explicit category field, extract group or default categorization
        val grouped = filteredSales.groupBy { sale ->
            val p = productMap[sale.productId]
            val name = p?.name ?: sale.productName
            // Determine a category from product name or simple heuristics
            when {
                name.contains("كرتون") || name.contains("علبة") || name.contains("طرد") -> "عبوات وكوارين"
                name.contains("زيت") || name.contains("سمن") || name.contains("زبدة") -> "زيوت ودهون"
                name.contains("سكر") || name.contains("أرز") || name.contains("دقيق") || name.contains("مكرونة") -> "مواد تموينية"
                name.contains("شاي") || name.contains("قهوة") || name.contains("بن") || name.contains("عصير") || name.contains("مياه") -> "مشروبات وبن"
                name.contains("بسكويت") || name.contains("شيبس") || name.contains("شوكولاتة") || name.contains("حلوى") -> "سناكس وحلويات"
                name.contains("صابون") || name.contains("منظف") || name.contains("كلور") || name.contains("مسحوق") -> "منظفات وعناية"
                else -> "أصناف عامة"
            }
        }

        var colorIdx = 0
        return grouped.map { (catName, catSales) ->
            val catRev = catSales.sumOf { it.totalPrice }
            val catUnits = catSales.sumOf { it.quantitySold }
            val catCost = catSales.sumOf { s ->
                val p = productMap[s.productId]
                s.quantitySold * (p?.purchasePrice ?: 0.0)
            }
            val color = chartColors[colorIdx % chartColors.size]
            colorIdx++

            CategoryFinancialShare(
                categoryName = catName,
                totalSales = catRev,
                totalProfit = catRev - catCost,
                unitsSold = catUnits,
                percentage = (catRev / totalRevenue) * 100.0,
                color = color
            )
        }.sortedByDescending { it.totalSales }
    }

    fun computePaymentMethodShares(
        sales: List<Sale>,
        startMillis: Long,
        endMillis: Long
    ): List<PaymentMethodShare> {
        val filteredSales = sales.filter { it.saleDate in startMillis..endMillis }
        val totalRevenue = filteredSales.sumOf { it.totalPrice }.coerceAtLeast(0.001)

        val cashSales = filteredSales.filter { it.paymentMethod.equals("CASH", ignoreCase = true) || !it.isDebt }
        val debtSales = filteredSales.filter { it.paymentMethod.equals("DEBT", ignoreCase = true) || it.isDebt }
        val cardSales = filteredSales.filter { it.paymentMethod.equals("CARD", ignoreCase = true) || it.paymentMethod.equals("PARTIAL", ignoreCase = true) }

        val list = mutableListOf<PaymentMethodShare>()

        val cashSum = cashSales.sumOf { it.totalPrice }
        if (cashSum > 0 || filteredSales.isNotEmpty()) {
            list.add(
                PaymentMethodShare(
                    methodName = "💵 نقدياً (كاش)",
                    totalAmount = cashSum,
                    count = cashSales.size,
                    percentage = (cashSum / totalRevenue) * 100.0,
                    color = Color(0xFF10B981) // Green
                )
            )
        }

        val debtSum = debtSales.sumOf { it.totalPrice }
        if (debtSum > 0) {
            list.add(
                PaymentMethodShare(
                    methodName = "📒 آجل / ديون عملاء",
                    totalAmount = debtSum,
                    count = debtSales.size,
                    percentage = (debtSum / totalRevenue) * 100.0,
                    color = Color(0xFFEF4444) // Red
                )
            )
        }

        val cardSum = cardSales.sumOf { it.totalPrice }
        if (cardSum > 0) {
            list.add(
                PaymentMethodShare(
                    methodName = "💳 دفع إلكتروني / شبكة",
                    totalAmount = cardSum,
                    count = cardSales.size,
                    percentage = (cardSum / totalRevenue) * 100.0,
                    color = Color(0xFF3B82F6) // Blue
                )
            )
        }

        return list
    }

    fun computeTopProducts(
        sales: List<Sale>,
        products: List<Product>,
        startMillis: Long,
        endMillis: Long
    ): List<ProductPerformance> {
        val productMap = products.associateBy { it.id }
        val filteredSales = sales.filter { it.saleDate in startMillis..endMillis }

        val grouped = filteredSales.groupBy { it.productId }
        val list = mutableListOf<ProductPerformance>()

        for ((productId, pSales) in grouped) {
            val prod = productMap[productId]
            val name = pSales.firstOrNull()?.productName ?: prod?.name ?: "صنف #$productId"
            val totalQty = pSales.sumOf { it.quantitySold }
            val totalRev = pSales.sumOf { it.totalPrice }
            val purchasePrice = prod?.purchasePrice ?: 0.0
            val totalCost = totalQty * purchasePrice
            val totalProfit = totalRev - totalCost
            val margin = if (totalRev > 0) (totalProfit / totalRev) * 100.0 else 0.0

            list.add(
                ProductPerformance(
                    productId = productId,
                    productName = name,
                    quantitySold = totalQty,
                    totalRevenue = totalRev,
                    totalCost = totalCost,
                    totalProfit = totalProfit,
                    profitMarginPercent = margin
                )
            )
        }

        return list.sortedByDescending { it.totalProfit }
    }
}

@Composable
fun DashboardKpiSection(
    totalSales: Double,
    totalProfit: Double,
    totalCost: Double,
    totalInvoices: Int,
    avgOrderValue: Double,
    profitMarginPercent: Double,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Sales Volume & Realized Profit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Total Sales Volume Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "حجم المبيعات",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "إجمالي المبيعات",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f ج.م", totalSales),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Realized Profit Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SuccessGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SuccessGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%% هامش", profitMarginPercent),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SuccessGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "الأرباح المحققة",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "+%.2f ج.م", totalProfit),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SuccessGreen
                    )
                }
            }
        }

        // Row 2: Cost of Goods & Total Transactions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cost Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text("تكلفة البضاعة المباعة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f ج.م", totalCost),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Invoices & Avg Value Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text("$totalInvoices فاتورة منفذة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format(Locale.getDefault(), "متوسط: %.1f ج.م", avgOrderValue),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardDualChartCard(
    title: String,
    points: List<DailyFinancialPoint>,
    chartMode: DashboardChartMode,
    selectedIndex: Int?,
    onChartModeChange: (DashboardChartMode) -> Unit,
    onSelectIndex: (Int?) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val profitColor = SuccessGreen
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val totalSalesInView = remember(points) { points.sumOf { it.salesVolume } }
    val totalProfitInView = remember(points) { points.sumOf { it.profit } }
    val avgSalesInView = remember(points, totalSalesInView) {
        if (points.isNotEmpty()) totalSalesInView / points.size else 0.0
    }
    val avgProfitInView = remember(points, totalProfitInView) {
        if (points.isNotEmpty()) totalProfitInView / points.size else 0.0
    }
    val peakSalesPoint = remember(points) { points.maxByOrNull { it.salesVolume } }
    val peakProfitPoint = remember(points) { points.maxByOrNull { it.profit } }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📊 $title",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "مقارنة المبيعات اليومية والأرباح المحققة",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalIconToggleButton(
                        checked = chartMode == DashboardChartMode.DUAL_BAR,
                        onCheckedChange = { onChartModeChange(DashboardChartMode.DUAL_BAR) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = "أعمدة", modifier = Modifier.size(18.dp))
                    }

                    FilledTonalIconToggleButton(
                        checked = chartMode == DashboardChartMode.SMOOTH_AREA,
                        onCheckedChange = { onChartModeChange(DashboardChartMode.SMOOTH_AREA) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.ShowChart, contentDescription = "منحنى", modifier = Modifier.size(18.dp))
                    }

                    FilledTonalIconToggleButton(
                        checked = chartMode == DashboardChartMode.MARGIN_TREND,
                        onCheckedChange = { onChartModeChange(DashboardChartMode.MARGIN_TREND) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Percent, contentDescription = "نسبة الهامش", modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chart Legends Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(shape = CircleShape, color = primaryColor, modifier = Modifier.size(8.dp)) {}
                    Text("حجم المبيعات", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(shape = CircleShape, color = profitColor, modifier = Modifier.size(8.dp)) {}
                    Text("الأرباح المحققة", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = profitColor)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(shape = RoundedCornerShape(2.dp), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f), modifier = Modifier.size(12.dp, 2.dp)) {}
                    Text("المتوسط اليومي", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (points.isEmpty() || points.all { it.salesVolume == 0.0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "لا توجد حركات بيع مسجلة خلال هذا النطاق الزمني",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                val maxVal = remember(points) {
                    (points.maxOfOrNull { maxOf(it.salesVolume, it.profit) } ?: 10.0).coerceAtLeast(10.0)
                }

                // Interactive Chart Canvas Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(points) {
                                detectTapGestures { offset ->
                                    val count = points.size
                                    if (count > 0) {
                                        val segWidth = size.width / count
                                        val tappedIndex = (offset.x / segWidth).toInt().coerceIn(0, count - 1)
                                        onSelectIndex(if (selectedIndex == tappedIndex) null else tappedIndex)
                                    }
                                }
                            }
                            .pointerInput(points) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val count = points.size
                                        if (count > 0) {
                                            val segWidth = size.width / count
                                            val draggedIndex = (offset.x / segWidth).toInt().coerceIn(0, count - 1)
                                            onSelectIndex(draggedIndex)
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val count = points.size
                                        if (count > 0) {
                                            val segWidth = size.width / count
                                            val draggedIndex = (change.position.x / segWidth).toInt().coerceIn(0, count - 1)
                                            onSelectIndex(draggedIndex)
                                        }
                                    }
                                )
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height - 22f
                        val pointCount = points.size

                        // 1. Grid Guidelines
                        val gridLineSteps = 4
                        for (g in 0..gridLineSteps) {
                            val gridY = canvasHeight * (g.toFloat() / gridLineSteps)
                            drawLine(
                                color = outlineVariant.copy(alpha = 0.35f),
                                start = Offset(0f, gridY),
                                end = Offset(canvasWidth, gridY),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                        }

                        // 2. Average Benchmark Line
                        if (avgSalesInView > 0 && chartMode != DashboardChartMode.MARGIN_TREND) {
                            val avgY = canvasHeight - ((avgSalesInView / maxVal) * canvasHeight).toFloat()
                            drawLine(
                                color = primaryColor.copy(alpha = 0.45f),
                                start = Offset(0f, avgY),
                                end = Offset(canvasWidth, avgY),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }

                        when (chartMode) {
                            DashboardChartMode.DUAL_BAR -> {
                                val segmentWidth = canvasWidth / pointCount
                                val barWidth = (segmentWidth * 0.38f).coerceIn(6f, 22f)
                                val barSpacing = 2f

                                points.forEachIndexed { i, pt ->
                                    val centerX = (i * segmentWidth) + (segmentWidth / 2f)
                                    val isSelected = selectedIndex == i

                                    // Sales Bar (Left)
                                    val salesHeight = ((pt.salesVolume / maxVal) * canvasHeight).toFloat().coerceAtLeast(3f)
                                    val salesLeft = centerX - barWidth - barSpacing
                                    val salesTop = canvasHeight - salesHeight

                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f)),
                                            startY = salesTop,
                                            endY = canvasHeight
                                        ),
                                        topLeft = Offset(salesLeft, salesTop),
                                        size = Size(barWidth, salesHeight),
                                        cornerRadius = CornerRadius(6f, 6f)
                                    )

                                    // Profit Bar (Right)
                                    val profitHeight = ((pt.profit.coerceAtLeast(0.0) / maxVal) * canvasHeight).toFloat().coerceAtLeast(3f)
                                    val profitLeft = centerX + barSpacing
                                    val profitTop = canvasHeight - profitHeight

                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(profitColor, profitColor.copy(alpha = 0.7f)),
                                            startY = profitTop,
                                            endY = canvasHeight
                                        ),
                                        topLeft = Offset(profitLeft, profitTop),
                                        size = Size(barWidth, profitHeight),
                                        cornerRadius = CornerRadius(6f, 6f)
                                    )

                                    if (isSelected) {
                                        // Focus highlight surrounding both bars
                                        drawRoundRect(
                                            color = onSurfaceColor,
                                            topLeft = Offset(salesLeft - 2f, minOf(salesTop, profitTop) - 2f),
                                            size = Size((barWidth * 2f) + (barSpacing * 2f) + 4f, maxOf(salesHeight, profitHeight) + 4f),
                                            cornerRadius = CornerRadius(8f, 8f),
                                            style = Stroke(width = 2f)
                                        )
                                    }
                                }
                            }

                            DashboardChartMode.SMOOTH_AREA -> {
                                val segmentWidth = canvasWidth / (pointCount.coerceAtLeast(2) - 1)

                                val salesOffsets = points.mapIndexed { i, pt ->
                                    val x = if (pointCount == 1) canvasWidth / 2f else i * segmentWidth
                                    val y = canvasHeight - ((pt.salesVolume / maxVal) * canvasHeight).toFloat().coerceAtLeast(4f)
                                    Offset(x, y)
                                }

                                val profitOffsets = points.mapIndexed { i, pt ->
                                    val x = if (pointCount == 1) canvasWidth / 2f else i * segmentWidth
                                    val y = canvasHeight - ((pt.profit.coerceAtLeast(0.0) / maxVal) * canvasHeight).toFloat().coerceAtLeast(4f)
                                    Offset(x, y)
                                }

                                // 1. Draw Sales Area (Primary)
                                val salesAreaPath = Path().apply {
                                    moveTo(salesOffsets.first().x, canvasHeight)
                                    lineTo(salesOffsets.first().x, salesOffsets.first().y)
                                    for (i in 0 until salesOffsets.size - 1) {
                                        val c1 = Offset(salesOffsets[i].x + (salesOffsets[i + 1].x - salesOffsets[i].x) / 2f, salesOffsets[i].y)
                                        val c2 = Offset(salesOffsets[i].x + (salesOffsets[i + 1].x - salesOffsets[i].x) / 2f, salesOffsets[i + 1].y)
                                        cubicTo(c1.x, c1.y, c2.x, c2.y, salesOffsets[i + 1].x, salesOffsets[i + 1].y)
                                    }
                                    lineTo(salesOffsets.last().x, canvasHeight)
                                    close()
                                }

                                drawPath(
                                    path = salesAreaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(primaryColor.copy(alpha = 0.28f), primaryColor.copy(alpha = 0.02f)),
                                        startY = 0f,
                                        endY = canvasHeight
                                    )
                                )

                                // 2. Draw Profit Area (Green)
                                val profitAreaPath = Path().apply {
                                    moveTo(profitOffsets.first().x, canvasHeight)
                                    lineTo(profitOffsets.first().x, profitOffsets.first().y)
                                    for (i in 0 until profitOffsets.size - 1) {
                                        val c1 = Offset(profitOffsets[i].x + (profitOffsets[i + 1].x - profitOffsets[i].x) / 2f, profitOffsets[i].y)
                                        val c2 = Offset(profitOffsets[i].x + (profitOffsets[i + 1].x - profitOffsets[i].x) / 2f, profitOffsets[i + 1].y)
                                        cubicTo(c1.x, c1.y, c2.x, c2.y, profitOffsets[i + 1].x, profitOffsets[i + 1].y)
                                    }
                                    lineTo(profitOffsets.last().x, canvasHeight)
                                    close()
                                }

                                drawPath(
                                    path = profitAreaPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(profitColor.copy(alpha = 0.35f), profitColor.copy(alpha = 0.03f)),
                                        startY = 0f,
                                        endY = canvasHeight
                                    )
                                )

                                // 3. Draw Stroke Lines
                                val salesLinePath = Path().apply {
                                    moveTo(salesOffsets.first().x, salesOffsets.first().y)
                                    for (i in 0 until salesOffsets.size - 1) {
                                        val c1 = Offset(salesOffsets[i].x + (salesOffsets[i + 1].x - salesOffsets[i].x) / 2f, salesOffsets[i].y)
                                        val c2 = Offset(salesOffsets[i].x + (salesOffsets[i + 1].x - salesOffsets[i].x) / 2f, salesOffsets[i + 1].y)
                                        cubicTo(c1.x, c1.y, c2.x, c2.y, salesOffsets[i + 1].x, salesOffsets[i + 1].y)
                                    }
                                }
                                drawPath(salesLinePath, color = primaryColor, style = Stroke(width = 3f))

                                val profitLinePath = Path().apply {
                                    moveTo(profitOffsets.first().x, profitOffsets.first().y)
                                    for (i in 0 until profitOffsets.size - 1) {
                                        val c1 = Offset(profitOffsets[i].x + (profitOffsets[i + 1].x - profitOffsets[i].x) / 2f, profitOffsets[i].y)
                                        val c2 = Offset(profitOffsets[i].x + (profitOffsets[i + 1].x - profitOffsets[i].x) / 2f, profitOffsets[i + 1].y)
                                        cubicTo(c1.x, c1.y, c2.x, c2.y, profitOffsets[i + 1].x, profitOffsets[i + 1].y)
                                    }
                                }
                                drawPath(profitLinePath, color = profitColor, style = Stroke(width = 3f))

                                // Draw Dots
                                points.indices.forEach { i ->
                                    val isSelected = selectedIndex == i
                                    drawCircle(
                                        color = if (isSelected) surfaceColor else primaryColor,
                                        radius = if (isSelected) 6f else 3.5f,
                                        center = salesOffsets[i]
                                    )
                                    drawCircle(
                                        color = if (isSelected) surfaceColor else profitColor,
                                        radius = if (isSelected) 6f else 3.5f,
                                        center = profitOffsets[i]
                                    )
                                }
                            }

                            DashboardChartMode.MARGIN_TREND -> {
                                val segmentWidth = canvasWidth / (pointCount.coerceAtLeast(2) - 1)
                                val maxMargin = (points.maxOfOrNull { it.marginPercent } ?: 50.0).coerceAtLeast(50.0)

                                val marginOffsets = points.mapIndexed { i, pt ->
                                    val x = if (pointCount == 1) canvasWidth / 2f else i * segmentWidth
                                    val y = canvasHeight - ((pt.marginPercent.coerceIn(0.0, maxMargin) / maxMargin) * canvasHeight).toFloat().coerceAtLeast(4f)
                                    Offset(x, y)
                                }

                                val linePath = Path().apply {
                                    moveTo(marginOffsets.first().x, marginOffsets.first().y)
                                    for (i in 0 until marginOffsets.size - 1) {
                                        val c1 = Offset(marginOffsets[i].x + (marginOffsets[i + 1].x - marginOffsets[i].x) / 2f, marginOffsets[i].y)
                                        val c2 = Offset(marginOffsets[i].x + (marginOffsets[i + 1].x - marginOffsets[i].x) / 2f, marginOffsets[i + 1].y)
                                        cubicTo(c1.x, c1.y, c2.x, c2.y, marginOffsets[i + 1].x, marginOffsets[i + 1].y)
                                    }
                                }
                                drawPath(linePath, color = profitColor, style = Stroke(width = 3.5f))

                                marginOffsets.forEachIndexed { i, offset ->
                                    val isSelected = selectedIndex == i
                                    drawCircle(
                                        color = if (isSelected) surfaceColor else profitColor,
                                        radius = if (isSelected) 7f else 4f,
                                        center = offset
                                    )
                                    if (isSelected) {
                                        drawCircle(color = profitColor, radius = 7f, center = offset, style = Stroke(width = 3f))
                                    }
                                }
                            }
                        }

                        // Cursor Guideline when hovered/selected
                        if (selectedIndex != null && selectedIndex in points.indices) {
                            val cursorX = if (chartMode == DashboardChartMode.DUAL_BAR) {
                                val segWidth = canvasWidth / pointCount
                                (selectedIndex * segWidth) + (segWidth / 2f)
                            } else {
                                val segWidth = canvasWidth / (pointCount.coerceAtLeast(2) - 1)
                                if (pointCount == 1) canvasWidth / 2f else selectedIndex * segWidth
                            }

                            drawLine(
                                color = onSurfaceColor.copy(alpha = 0.4f),
                                start = Offset(cursorX, 0f),
                                end = Offset(cursorX, canvasHeight),
                                strokeWidth = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }
                    }
                }

                // X-Axis Labels Row (Sampled smartly if many days)
                val displayStep = when {
                    points.size > 20 -> 5
                    points.size > 10 -> 2
                    else -> 1
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEachIndexed { idx, pt ->
                        val shouldShow = idx % displayStep == 0 || idx == points.lastIndex || idx == selectedIndex
                        val isSelected = selectedIndex == idx

                        if (shouldShow) {
                            Text(
                                text = pt.shortLabel,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.clickable { onSelectIndex(if (selectedIndex == idx) null else idx) }
                            )
                        }
                    }
                }

                // Interactive Scrubber Details Card
                AnimatedVisibility(
                    visible = selectedIndex != null && selectedIndex in points.indices,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (selectedIndex != null && selectedIndex in points.indices) {
                        val activePt = points[selectedIndex]

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.EventNote,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = activePt.fullDateLabel,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { onSelectIndex(null) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(16.dp))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("حجم المبيعات:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.2f ج.م", activePt.salesVolume),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = primaryColor
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("الأرباح المحققة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = String.format(Locale.getDefault(), "+%.2f ج.م", activePt.profit),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = profitColor
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("هامش الربح:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.1f%%", activePt.marginPercent),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = profitColor
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "📦 تكلفة البضاعة: ${String.format(Locale.getDefault(), "%.2f ج.م", activePt.costVolume)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "🧾 ${activePt.invoicesCount} فواتير (${activePt.unitsSold} قطعة)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopProfitableProductsSection(
    topProducts: List<ProductPerformance>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "🏆 الأصناف الأكثر تحقيقاً للأرباح في هذه الفترة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (topProducts.isEmpty()) {
                Text(
                    text = "لا توجد مبيعات أصناف في هذه الفترة",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxProfit = (topProducts.maxOfOrNull { it.totalProfit } ?: 1.0).coerceAtLeast(1.0)

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    topProducts.take(5).forEachIndexed { index, item ->
                        val ratio = (item.totalProfit.coerceAtLeast(0.0) / maxProfit).toFloat().coerceIn(0.05f, 1f)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Text(
                                        text = item.productName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = String.format(Locale.getDefault(), "+%.2f ج.م ربح", item.totalProfit),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = SuccessGreen
                                )
                            }

                            // Progress Bar Indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(MaterialTheme.colorScheme.primary, SuccessGreen)
                                            )
                                        )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "المبيعات: ${String.format(Locale.getDefault(), "%.2f ج.م", item.totalRevenue)} (${item.quantitySold} قطعة)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "هامش: ${String.format(Locale.getDefault(), "%.1f%%", item.profitMarginPercent)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyBreakdownTable(
    dailyPoints: List<DailyFinancialPoint>,
    modifier: Modifier = Modifier
) {
    val activeDays = remember(dailyPoints) { dailyPoints.filter { it.salesVolume > 0 } }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "📋 السجل المالي اليومي المفصل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (activeDays.isEmpty()) {
                Text(
                    text = "لا توجد مبيعات في الأيام المحددة",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeDays.reversed().forEach { day ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = day.fullDateLabel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${day.invoicesCount} فواتير | ${day.unitsSold} قطعة مباعة",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.2f ج.م مبيعات", day.salesVolume),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "+%.2f ج.م ربح (%.0f%%)", day.profit, day.marginPercent),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = SuccessGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardMonthlyChartCard(
    monthlyPoints: List<MonthlyFinancialPoint>,
    selectedMonthIndex: Int?,
    onSelectMonth: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val profitColor = SuccessGreen
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val maxVal = remember(monthlyPoints) {
        (monthlyPoints.maxOfOrNull { maxOf(it.salesVolume, it.profit) } ?: 10.0).coerceAtLeast(10.0)
    }

    val bestMonth = remember(monthlyPoints) {
        monthlyPoints.maxByOrNull { it.salesVolume }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = primaryColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "📊 الأداء والمقارنة الشهرية (12 شهراً)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "مقارنة المبيعات وصافي الأرباح ونسب النمو",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (bestMonth != null && bestMonth.salesVolume > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7), // Amber highlight
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("⭐", fontSize = 11.sp)
                            Text(
                                text = "أعلى شهر: ${bestMonth.shortLabel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(primaryColor))
                    Text("إجمالي المبيعات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(profitColor))
                    Text("صافي الأرباح", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(monthlyPoints) {
                            detectTapGestures { offset ->
                                val count = monthlyPoints.size
                                if (count > 0) {
                                    val segWidth = size.width / count
                                    val tappedIndex = (offset.x / segWidth).toInt().coerceIn(0, count - 1)
                                    onSelectMonth(if (selectedMonthIndex == tappedIndex) null else tappedIndex)
                                }
                            }
                        }
                        .pointerInput(monthlyPoints) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val count = monthlyPoints.size
                                    if (count > 0) {
                                        val segWidth = size.width / count
                                        val idx = (offset.x / segWidth).toInt().coerceIn(0, count - 1)
                                        onSelectMonth(idx)
                                    }
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val count = monthlyPoints.size
                                    if (count > 0) {
                                        val segWidth = size.width / count
                                        val idx = (change.position.x / segWidth).toInt().coerceIn(0, count - 1)
                                        onSelectMonth(idx)
                                    }
                                }
                            )
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height - 22f
                    val pointCount = monthlyPoints.size

                    // Grid lines
                    val gridLineSteps = 4
                    for (g in 0..gridLineSteps) {
                        val gridY = canvasHeight * (g.toFloat() / gridLineSteps)
                        drawLine(
                            color = outlineVariant.copy(alpha = 0.35f),
                            start = Offset(0f, gridY),
                            end = Offset(canvasWidth, gridY),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    val segmentWidth = canvasWidth / pointCount
                    val barWidth = (segmentWidth * 0.35f).coerceIn(4f, 18f)
                    val barSpacing = 2f

                    monthlyPoints.forEachIndexed { i, pt ->
                        val centerX = (i * segmentWidth) + (segmentWidth / 2f)
                        val isSelected = selectedMonthIndex == i

                        // Sales Bar (Left)
                        val salesHeight = ((pt.salesVolume / maxVal) * canvasHeight).toFloat().coerceAtLeast(3f)
                        val salesLeft = centerX - barWidth - barSpacing
                        val salesTop = canvasHeight - salesHeight

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.75f)),
                                startY = salesTop,
                                endY = canvasHeight
                            ),
                            topLeft = Offset(salesLeft, salesTop),
                            size = Size(barWidth, salesHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        // Profit Bar (Right)
                        val profitHeight = ((pt.profit.coerceAtLeast(0.0) / maxVal) * canvasHeight).toFloat().coerceAtLeast(3f)
                        val profitLeft = centerX + barSpacing
                        val profitTop = canvasHeight - profitHeight

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(profitColor, profitColor.copy(alpha = 0.75f)),
                                startY = profitTop,
                                endY = canvasHeight
                            ),
                            topLeft = Offset(profitLeft, profitTop),
                            size = Size(barWidth, profitHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )

                        if (isSelected) {
                            drawRoundRect(
                                color = onSurfaceColor,
                                topLeft = Offset(salesLeft - 2f, minOf(salesTop, profitTop) - 2f),
                                size = Size((barWidth * 2f) + (barSpacing * 2f) + 4f, maxOf(salesHeight, profitHeight) + 4f),
                                cornerRadius = CornerRadius(8f, 8f),
                                style = Stroke(width = 2f)
                            )
                        }
                    }

                    // Scrubber line
                    if (selectedMonthIndex != null && selectedMonthIndex in monthlyPoints.indices) {
                        val segWidth = canvasWidth / pointCount
                        val cursorX = (selectedMonthIndex * segWidth) + (segWidth / 2f)
                        drawLine(
                            color = onSurfaceColor.copy(alpha = 0.5f),
                            start = Offset(cursorX, 0f),
                            end = Offset(cursorX, canvasHeight),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }
                }
            }

            // Month Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                monthlyPoints.forEachIndexed { idx, pt ->
                    val isSelected = selectedMonthIndex == idx
                    Text(
                        text = pt.shortLabel.take(3), // Take 3 letters for compact view
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable { onSelectMonth(if (selectedMonthIndex == idx) null else idx) }
                    )
                }
            }

            // Selected Month Scrubber Popup
            AnimatedVisibility(
                visible = selectedMonthIndex != null && selectedMonthIndex in monthlyPoints.indices,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (selectedMonthIndex != null && selectedMonthIndex in monthlyPoints.indices) {
                    val mPt = monthlyPoints[selectedMonthIndex]

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📅 ${mPt.fullMonthLabel}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )

                                if (mPt.momGrowthPercent != null) {
                                    val isPos = mPt.momGrowthPercent >= 0
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isPos) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                                    ) {
                                        Text(
                                            text = "${if (isPos) "+" else ""}${String.format(Locale.getDefault(), "%.1f%%", mPt.momGrowthPercent)} نمو شهري",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPos) Color(0xFF065F46) else Color(0xFF991B1B),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onSelectMonth(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق", modifier = Modifier.size(16.dp))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("المبيعات:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.2f ج.م", mPt.salesVolume),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = primaryColor
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("صافي الأرباح:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = String.format(Locale.getDefault(), "+%.2f ج.م", mPt.profit),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = profitColor
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("فواتير / قطع:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${mPt.invoicesCount} فاتورة (${mPt.unitsSold} قطعة)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHourlyTrafficCard(
    hourlyPoints: List<HourlyFinancialPoint>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val peakHour = remember(hourlyPoints) { hourlyPoints.maxByOrNull { it.salesVolume } }
    val maxVal = remember(hourlyPoints) { (hourlyPoints.maxOfOrNull { it.salesVolume } ?: 10.0).coerceAtLeast(10.0) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF97316).copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "⏱️ حركة المبيعات اليومية بالساعات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "توزيع الإقبال والمبيعات من 8 صباحاً حتى 11 مساءً",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (peakHour != null && peakHour.salesVolume > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEE2E2),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "🔥 الذروة: ${peakHour.label}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB91C1C),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Hourly Mini Canvas Bars
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height - 20f
                    val count = hourlyPoints.size
                    val segWidth = canvasWidth / count
                    val barWidth = (segWidth * 0.55f).coerceIn(4f, 16f)

                    hourlyPoints.forEachIndexed { i, pt ->
                        val centerX = (i * segWidth) + (segWidth / 2f)
                        val barHeight = ((pt.salesVolume / maxVal) * canvasHeight).toFloat().coerceAtLeast(2f)
                        val barLeft = centerX - (barWidth / 2f)
                        val barTop = canvasHeight - barHeight

                        val isPeak = pt == peakHour && pt.salesVolume > 0
                        val barColor = if (isPeak) Color(0xFFF97316) else primaryColor

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(barColor, barColor.copy(alpha = 0.6f)),
                                startY = barTop,
                                endY = canvasHeight
                            ),
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
            }

            // Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                hourlyPoints.filterIndexed { index, _ -> index % 3 == 0 || index == hourlyPoints.lastIndex }.forEach { pt ->
                    Text(
                        text = pt.label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardCategoryDonutCard(
    categoryShares: List<CategoryFinancialShare>,
    modifier: Modifier = Modifier
) {
    var selectedCategoryIndex by remember { mutableStateOf<Int?>(null) }
    val totalSales = remember(categoryShares) { categoryShares.sumOf { it.totalSales } }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "🍩 توزيع المبيعات حسب التصنيفات والأقسام",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "نسبة مساهمة كل قسم في إجمالي دخل المتجر",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (categoryShares.isEmpty() || totalSales <= 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد مبيعات مسجلة في هذا النطاق", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Canvas
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val diameter = minOf(size.width, size.height)
                            val strokeWidth = 24f
                            val arcSize = Size(diameter - strokeWidth, diameter - strokeWidth)
                            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                            var currentAngle = -90f

                            categoryShares.forEachIndexed { index, share ->
                                val sweepAngle = ((share.percentage / 100.0) * 360f).toFloat()
                                val isSelected = selectedCategoryIndex == index

                                drawArc(
                                    color = share.color,
                                    startAngle = currentAngle,
                                    sweepAngle = sweepAngle - 2f, // slight gap
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = if (isSelected) strokeWidth + 6f else strokeWidth)
                                )

                                currentAngle += sweepAngle
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "الإجمالي",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.0f ج", totalSales),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Legend List
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryShares.take(4).forEachIndexed { idx, item ->
                            val isSelected = selectedCategoryIndex == idx

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) item.color.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategoryIndex = if (isSelected) null else idx }
                            ) {
                                Row(
                                    modifier = Modifier.padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(item.color)
                                        )
                                        Text(
                                            text = item.categoryName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        text = String.format(Locale.getDefault(), "%.1f%%", item.percentage),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = item.color
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardPaymentMethodCard(
    paymentShares: List<PaymentMethodShare>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "💳 طرق السداد والتحصيل",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            if (paymentShares.isEmpty()) {
                Text("لا توجد مبيعات مسجلة", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                // Multi-color segmented progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        paymentShares.forEach { share ->
                            Box(
                                modifier = Modifier
                                    .weight((share.percentage.coerceAtLeast(1.0)).toFloat())
                                    .fillMaxHeight()
                                    .background(share.color)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    paymentShares.forEach { share ->
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(share.color))
                                Text(share.methodName, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f ج.م (%.0f%%)", share.totalAmount, share.percentage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyBreakdownTable(
    monthlyPoints: List<MonthlyFinancialPoint>,
    modifier: Modifier = Modifier
) {
    val activeMonths = remember(monthlyPoints) { monthlyPoints.filter { it.salesVolume > 0 } }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarViewMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "📑 جدول الأداء الشهري المفصل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (activeMonths.isEmpty()) {
                Text(
                    text = "لا توجد حركات مبيعات مسجلة في الشهور السابقة",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeMonths.reversed().forEach { month ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = month.fullMonthLabel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${month.invoicesCount} فاتورة | ${month.unitsSold} قطعة مباعة",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.2f ج.م مبيعات", month.salesVolume),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "+%.2f ج.م ربح (%.0f%%)", month.profit, month.marginPercent),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = SuccessGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
