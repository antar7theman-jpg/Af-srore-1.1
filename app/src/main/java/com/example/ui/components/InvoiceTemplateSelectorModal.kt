package com.example.ui.components

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.InvoiceStyle
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.SuccessGreen
import com.example.utils.AppLanguage
import com.example.utils.AppStrings

@Composable
fun InvoiceTemplateSelectorModal(
    currentStyle: InvoiceStyle,
    currentPaperWidth: String,
    currentLanguage: AppLanguage,
    onSelectStyle: (InvoiceStyle) -> Unit,
    onSelectPaperWidth: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var previewStyle by remember { mutableStateOf<InvoiceStyle?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp)
                .testTag("invoice_template_selector_modal"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (currentLanguage == AppLanguage.FRENCH) "Modèles de Facture & Reçus" else if (currentLanguage == AppLanguage.ENGLISH) "Invoice & Receipt Templates" else "نماذج الفاتورة والطباعة الحرارية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentLanguage == AppLanguage.FRENCH) "Sélectionnez le modèle par défaut" else if (currentLanguage == AppLanguage.ENGLISH) "Select your default printing style" else "اختر النموذج المعتمد للطباعة والمعاينة",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Paper width selector (58mm vs 80mm)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.FRENCH) "Largeur du papier thermique :" else if (currentLanguage == AppLanguage.ENGLISH) "Thermal Paper Width:" else "عرض رول الورق الحراري:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("58" to "58mm", "80" to "80mm").forEach { (widthVal, label) ->
                                val isSelected = currentPaperWidth == widthVal
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSelectPaperWidth(widthVal) },
                                    label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Templates
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(InvoiceStyle.values()) { style ->
                        val isSelected = currentStyle == style
                        TemplateOptionCard(
                            style = style,
                            isSelected = isSelected,
                            currentLanguage = currentLanguage,
                            onSelect = { onSelectStyle(style) },
                            onPreview = { previewStyle = style }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("invoice_template_done_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentLanguage == AppLanguage.FRENCH) "Appliquer & Enregistrer" else if (currentLanguage == AppLanguage.ENGLISH) "Apply & Save" else "تطبيق وحفظ الاختيار",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Interactive Sample Preview Modal
    previewStyle?.let { styleToPreview ->
        SampleInvoicePreviewModal(
            style = styleToPreview,
            currentLanguage = currentLanguage,
            onDismiss = { previewStyle = null }
        )
    }
}

@Composable
fun TemplateOptionCard(
    style: InvoiceStyle,
    isSelected: Boolean,
    currentLanguage: AppLanguage,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onSelect,
        modifier = modifier
            .fillMaxWidth()
            .testTag("invoice_style_card_${style.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = style.iconEmoji, fontSize = 24.sp)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = style.getDisplayName(currentLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = if (currentLanguage == AppLanguage.FRENCH) "Actif" else if (currentLanguage == AppLanguage.ENGLISH) "Active" else "النموذج المعتمد",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = style.getDescription(currentLanguage),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini visual representation
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (style == InvoiceStyle.THERMAL_POS) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .border(
                        1.dp,
                        if (style == InvoiceStyle.THERMAL_POS) Color(0xFF334155) else Color(0xFFE2E8F0),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    when (style) {
                        InvoiceStyle.DETAILED -> {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🏪 AF store للتجارة", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Text("#10024", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF64748B))
                                }
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFCBD5E1)))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("1× أرز مصري فاخر (10 ق)", fontSize = 8.sp, color = Color(0xFF334155))
                                    Text("325.00 ج.م", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                            }
                        }
                        InvoiceStyle.SIMPLE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• 1× أرز مصري فاخر", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                    Text("32.50 ج.م", fontSize = 9.sp, color = Color(0xFF0F172A))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("• 2× سكر مكرر", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                    Text("56.00 ج.م", fontSize = 9.sp, color = Color(0xFF0F172A))
                                }
                            }
                        }
                        InvoiceStyle.THERMAL_POS -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("--- AF STORE POS ---", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                Text("ARZ MASRI 1KG x2  = 65.00", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFE2E8F0))
                                Text("TOTAL: 65.00 EGP [CASH]", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row inside card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPreview,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (currentLanguage == AppLanguage.FRENCH) "Aperçu de la maquette" else if (currentLanguage == AppLanguage.ENGLISH) "Preview Layout" else "معاينة شكل النموذج 👁️",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onSelect
                )
            }
        }
    }
}

/**
 * Interactive Sample Mock Preview Dialog for the chosen template
 */
@Composable
fun SampleInvoicePreviewModal(
    style: InvoiceStyle,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (style == InvoiceStyle.THERMAL_POS) Color(0xFF0F172A) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${style.iconEmoji} ${style.getDisplayName(currentLanguage)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (style == InvoiceStyle.THERMAL_POS) Color.White else Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = if (style == InvoiceStyle.THERMAL_POS) Color.White else Color.Black
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Render Sample Form
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        when (style) {
                            InvoiceStyle.DETAILED -> {
                                DetailedSampleView(currentLanguage)
                            }
                            InvoiceStyle.SIMPLE -> {
                                SimpleSampleView(currentLanguage)
                            }
                            InvoiceStyle.THERMAL_POS -> {
                                ThermalPosSampleView(currentLanguage)
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (currentLanguage == AppLanguage.FRENCH) "Fermer l'aperçu" else if (currentLanguage == AppLanguage.ENGLISH) "Close Preview" else "إغلاق المعاينة")
                }
            }
        }
    }
}

@Composable
private fun DetailedSampleView(currentLanguage: AppLanguage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text("🏪 AF store للتجارة والتوزيع", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
        Text("فاتورة مبيعات معتمدة - تفصيلية", fontSize = 11.sp, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("رقم الفاتورة: #749210", fontSize = 11.sp, color = Color(0xFF334155))
            Text("التاريخ: 2026/08/25", fontSize = 11.sp, color = Color(0xFF334155))
        }
        Text("العميل: أحمد محمود (نقدي)", fontSize = 11.sp, color = Color(0xFF334155))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Table Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("الصنف", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1.5f))
            Text("الكمية", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
            Text("السعر", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
            Text("الإجمالي", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Sample Items
        listOf(
            Triple("أرز مصري فاخر", "1 📦 (10 ق)", "32.50 = 325.00"),
            Triple("زيت عباد 800مل", "2 ق فردي", "65.00 = 130.00"),
            Triple("شاي العروسة 250جم", "3 ق فردي", "48.00 = 144.00")
        ).forEach { (name, qty, prc) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(name, fontSize = 11.sp, color = Color(0xFF1E293B), modifier = Modifier.weight(1.5f))
                Text(qty, fontSize = 10.sp, color = Color(0xFF475569), modifier = Modifier.weight(1f))
                Text(prc.split(" = ")[0], fontSize = 10.sp, color = Color(0xFF475569), modifier = Modifier.weight(1f))
                Text("${prc.split(" = ")[1]} ج.م", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("المجموع الفرعي:", fontSize = 11.sp, color = Color(0xFF64748B))
            Text("599.00 ج.م", fontSize = 11.sp, color = Color(0xFF0F172A))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("خصم (5%):", fontSize = 11.sp, color = Color(0xFFDC2626))
            Text("-29.95 ج.م", fontSize = 11.sp, color = Color(0xFFDC2626))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("الإجمالي النهائي:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
            Text("569.05 ج.م", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PolishPrimary)
        }
    }
}

@Composable
private fun SimpleSampleView(currentLanguage: AppLanguage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text("🏪 AF store", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
        Text("إيصال مبيعات سريع #749210", fontSize = 11.sp, color = Color(0xFF64748B))
        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

        listOf(
            "10× أرز مصري فاخر" to "325.00 ج.م",
            "2× زيت عباد 800مل" to "130.00 ج.م",
            "3× شاي العروسة" to "144.00 ج.م"
        ).forEach { (item, price) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item, fontSize = 12.sp, color = Color(0xFF0F172A))
                Text(price, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("المبلغ الإجمالي:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("599.00 ج.م", fontWeight = FontWeight.Black, fontSize = 16.sp, color = PolishPrimary)
        }
    }
}

@Composable
private fun ThermalPosSampleView(currentLanguage: AppLanguage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF020617), RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("================================", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF64748B))
        Text("AF STORE POS RECEIPT", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
        Text("INV: #749210 | DATE: 2026/08/25", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF94A3B8))
        Text("--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF64748B))

        listOf(
            "ARZ MASRI 1KG  x10  325.00",
            "ZEIT ABBAD     x2   130.00",
            "SHAY AROUSA    x3   144.00"
        ).forEach { line ->
            Text(line, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFFE2E8F0), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        }

        Text("--------------------------------", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF64748B))
        Text("TOTAL: 599.00 EGP [CASH]", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF4ADE80))
        Text("================================", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color(0xFF64748B))
        Text("THANK YOU FOR YOUR BUSINESS!", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color(0xFF38BDF8))
    }
}
