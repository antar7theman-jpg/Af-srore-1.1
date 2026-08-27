package com.example.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.InvoiceStyle
import com.example.data.models.Sale
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.utils.AppLanguage
import com.example.utils.AppStrings
import com.example.utils.BluetoothPrinterManager
import com.example.utils.PdfInvoiceHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Sale Confirmation Modal displayed immediately upon completing a sale.
 * Replaces the unwanted PDF share dialog with a direct confirmation badge,
 * order summary, and seamless Bluetooth thermal printer dispatch.
 */
@Composable
fun SaleConfirmationModal(
    invoiceData: InvoicePreviewData,
    invoiceStyle: InvoiceStyle,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onOpenPreview: () -> Unit,
    onOpenPrinterSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }
    var printStatusMessage by remember { mutableStateOf<String?>(null) }
    var printSuccess by remember { mutableStateOf(false) }

    val savedPrinterName = remember { BluetoothPrinterManager.getSavedPrinterName(context) }
    val savedPaperWidth = remember { BluetoothPrinterManager.getSavedPaperWidth(context) }
    var selectedStyle by remember {
        mutableStateOf(BluetoothPrinterManager.getSavedInvoiceStyle(context) ?: invoiceStyle)
    }

    // Pulsing animation for confirmation icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(invoiceData.dateMillis) { dateFormat.format(Date(invoiceData.dateMillis)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("sale_confirmation_modal"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Success Confirmation Badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                ) {
                    // Outer glow circle
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.18f))
                    )
                    // Inner solid badge
                    Surface(
                        shape = CircleShape,
                        color = SuccessGreen,
                        modifier = Modifier.size(68.dp),
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "تأكيد البيع",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }

                // Confirmation Title
                Text(
                    text = when (currentLanguage) {
                        AppLanguage.FRENCH -> "Vente Validée avec Succès !"
                        AppLanguage.ENGLISH -> "Sale Confirmed Successfully!"
                        AppLanguage.ARABIC -> "تم تأكيد البيع بنجاح ✅"
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Invoice Number & Date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "#${invoiceData.invoiceNumber}",
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Transaction Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Amount Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (currentLanguage) {
                                    AppLanguage.FRENCH -> "Montant total payé"
                                    AppLanguage.ENGLISH -> "Total Amount"
                                    AppLanguage.ARABIC -> "المبلغ الإجمالي"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%,.2f %s", invoiceData.finalTotal, AppStrings.currency(currentLanguage)),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 1.dp
                        )

                        // Payment Mode & Customer Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Payment method badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (invoiceData.isDebt) DangerRed.copy(alpha = 0.15f) else SuccessGreen.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (invoiceData.isDebt) Icons.Default.Warning else Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = if (invoiceData.isDebt) DangerRed else SuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (invoiceData.isDebt) {
                                            if (currentLanguage == AppLanguage.FRENCH) "À Crédit (Dette)" else if (currentLanguage == AppLanguage.ENGLISH) "On Credit (Debt)" else "بيع آجل (دين)"
                                        } else {
                                            if (currentLanguage == AppLanguage.FRENCH) "Comptant (Cash)" else if (currentLanguage == AppLanguage.ENGLISH) "Cash" else "دفع نقدي (كاش)"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (invoiceData.isDebt) DangerRed else SuccessGreen
                                    )
                                }
                            }

                            // Customer Name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = invoiceData.customerName ?: if (currentLanguage == AppLanguage.FRENCH) "Client Général" else if (currentLanguage == AppLanguage.ENGLISH) "General Customer" else "زبون نقدي عام",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Items Count & Template Selector Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${invoiceData.totalItemsCount} ${if (currentLanguage == AppLanguage.FRENCH) "articles" else if (currentLanguage == AppLanguage.ENGLISH) "items" else "أصناف"} (${invoiceData.totalQuantitySold} ${if (currentLanguage == AppLanguage.FRENCH) "unités" else if (currentLanguage == AppLanguage.ENGLISH) "units" else "قطعة"})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (invoiceData.totalCartonsCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "${invoiceData.totalCartonsCount} ${if (currentLanguage == AppLanguage.FRENCH) "cartons" else if (currentLanguage == AppLanguage.ENGLISH) "boxes" else "عبوة"} 📦",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Quick Invoice Style Selector Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "نموذج الفاتورة:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                InvoiceStyle.values().forEach { style ->
                                    val isSelected = selectedStyle == style
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1)),
                                        modifier = Modifier.clickable {
                                            selectedStyle = style
                                            BluetoothPrinterManager.saveInvoiceStyle(context, style)
                                        }
                                    ) {
                                        Text(
                                            text = "${style.iconEmoji} ${style.getDisplayName(currentLanguage)}",
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Thermal Printing Action Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (printSuccess) SuccessGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (printSuccess) SuccessGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Printer name & paper size indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = savedPrinterName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${savedPaperWidth}mm",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onOpenPrinterSettings,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsBluetooth,
                                    contentDescription = "إعدادات الطابعة",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Big Print Thermal Receipt Button
                        Button(
                            onClick = {
                                isPrinting = true
                                printStatusMessage = "جاري إرسال الإيصال للطابعة الحرارية..."
                                BluetoothPrinterManager.printInvoice(
                                    context = context,
                                    invoice = invoiceData,
                                    coroutineScope = coroutineScope,
                                    style = selectedStyle
                                ) { success, msg ->
                                    isPrinting = false
                                    printSuccess = success
                                    printStatusMessage = msg
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("print_thermal_receipt_btn"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isPrinting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (printSuccess) SuccessGreen else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isPrinting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (currentLanguage == AppLanguage.FRENCH) "Impression..." else if (currentLanguage == AppLanguage.ENGLISH) "Printing..." else "جاري الطباعة...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = if (printSuccess) Icons.Default.CheckCircle else Icons.Default.Print,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (printSuccess) {
                                        if (currentLanguage == AppLanguage.FRENCH) "Réimprimer le Reçu 🖨️" else if (currentLanguage == AppLanguage.ENGLISH) "Reprint Receipt 🖨️" else "إعادة طباعة الفاتورة 🖨️"
                                    } else {
                                        if (currentLanguage == AppLanguage.FRENCH) "Imprimer le Reçu Thermique 🖨️" else if (currentLanguage == AppLanguage.ENGLISH) "Print Thermal Receipt 🖨️" else "طبع الفاتورة الحرارية 🖨️"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Share as PDF Button
                        OutlinedButton(
                            onClick = {
                                PdfInvoiceHelper.shareInvoiceAsPdf(context, invoiceData)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("confirmation_share_pdf_btn"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.FRENCH) "Partager en PDF 📄" else if (currentLanguage == AppLanguage.ENGLISH) "Share as PDF 📄" else "مشاركة الفاتورة كـ PDF 📄",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Print Status Text Feedback
                        printStatusMessage?.let { status ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = status,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (printSuccess) SuccessGreen else MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Bottom Action Buttons (Done / New Sale & Optional Preview)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Preview Details (Optional)
                    OutlinedButton(
                        onClick = onOpenPreview,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("confirmation_preview_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.FRENCH) "Aperçu" else if (currentLanguage == AppLanguage.ENGLISH) "Preview" else "معاينة",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Done & Next Sale Button (Primary Dismiss)
                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                            .testTag("confirmation_done_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.FRENCH) "Terminer (Nouveau)" else if (currentLanguage == AppLanguage.ENGLISH) "Done (New Sale)" else "تم (بيع جديد)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
