package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.InvoiceStyle
import com.example.data.models.User
import com.example.ui.components.BluetoothPrinterModal
import com.example.ui.components.InvoiceTemplateSelectorModal
import com.example.ui.components.UsbPrinterModal
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppThemePalette
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.utils.AppLanguage
import com.example.utils.AppStrings
import com.example.utils.BluetoothPrinterManager
import com.example.utils.PdfInvoiceHelper
import com.example.utils.UsbAndBuiltInPrinterManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AntarSalesViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val visibleUsers = remember(allUsers) {
        allUsers.filter { user ->
            !user.username.matches(Regex("(?i)^admin\\d+$"))
        }
    }
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val currentThemePalette by viewModel.currentThemePalette.collectAsState()
    val currentThemeMode by viewModel.currentThemeMode.collectAsState()
    val currentInvoiceStyle by viewModel.invoiceStyle.collectAsState()
    val currentPaperWidth by viewModel.thermalPaperWidth.collectAsState()
    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val lowStockAlertsEnabled by viewModel.lowStockAlertsEnabled.collectAsState()
    val lowStockThreshold by viewModel.lowStockThreshold.collectAsState()
    val connectedPrinter by BluetoothPrinterManager.connectedDevice.collectAsState()
    val connectedUsbPrinter by UsbAndBuiltInPrinterManager.connectedUsbDevice.collectAsState()
    val isBuiltInPrinterMode by UsbAndBuiltInPrinterManager.isBuiltInPrinterMode.collectAsState()
    val usbPaperWidth by UsbAndBuiltInPrinterManager.paperWidth.collectAsState()
    val usbAutoCut by UsbAndBuiltInPrinterManager.autoCutEnabled.collectAsState()
    val usbDrawerKick by UsbAndBuiltInPrinterManager.drawerKickEnabled.collectAsState()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var showUsbPrinterDialog by remember { mutableStateOf(false) }
    var showInvoiceTemplatesModal by remember { mutableStateOf(false) }
    var showEditStoreDialog by remember { mutableStateOf(false) }
    var showCustomCurrencyDialog by remember { mutableStateOf(false) }
    var isTestingPrint by remember { mutableStateOf(false) }
    var isTestingUsbPrint by remember { mutableStateOf(false) }
    var isKickingDrawerQuick by remember { mutableStateOf(false) }

    // CSV Launchers
    val exportProductsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportProductsToCsv(context, it) }
    }

    val importProductsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importProductsFromCsv(context, it) }
    }

    val exportSalesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportSalesToCsv(context, it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // User Info Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.username ?: "المستخدم",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (currentUser?.role == "admin") AppStrings.roleAdmin(currentLanguage) else AppStrings.roleSeller(currentLanguage),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    FilledTonalButton(
                        onClick = { showLogoutDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.logout(currentLanguage), fontSize = 12.sp)
                    }
                }
            }
        }

        // 🏪 Store Identity & Currency Section
        item {
            SectionHeader(title = AppStrings.storeSettingsSectionTitle(currentLanguage))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Store Name Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = AppStrings.storeNameTitle(currentLanguage),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = storeName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { showEditStoreDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("edit_store_name_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.FRENCH) "Modifier" else if (currentLanguage == AppLanguage.ENGLISH) "Edit" else "تعديل",
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Currency Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Paid,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = AppStrings.currencyTitle(currentLanguage),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = AppStrings.currencyDesc(currentLanguage),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = currencySymbol,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Currency Chips
                    val popularCurrencies = listOf("ج.م", "ر.س", "د.إ", "د.ك", "$", "€", "د.ج", "د.ت")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        popularCurrencies.forEach { curr ->
                            val isSelected = currencySymbol == curr
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setCurrencySymbol(curr) },
                                label = { Text(curr, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        FilterChip(
                            selected = !popularCurrencies.contains(currencySymbol),
                            onClick = { showCustomCurrencyDialog = true },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(if (currentLanguage == AppLanguage.FRENCH) "Autre..." else if (currentLanguage == AppLanguage.ENGLISH) "Custom..." else "عملة مخصصة...")
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // 🔔 Low Stock Monitoring & Alerts Section
        item {
            SectionHeader(title = AppStrings.stockAlertsSectionTitle(currentLanguage))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stock_alerts_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (lowStockAlertsEnabled) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (lowStockAlertsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = if (lowStockAlertsEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.padding(end = 8.dp)) {
                                Text(
                                    text = AppStrings.lowStockAlertsToggle(currentLanguage),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = AppStrings.lowStockAlertsDesc(currentLanguage),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Switch(
                            checked = lowStockAlertsEnabled,
                            onCheckedChange = { viewModel.setLowStockAlertsEnabled(it) },
                            modifier = Modifier.testTag("low_stock_alerts_switch")
                        )
                    }

                    if (lowStockAlertsEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = AppStrings.lowStockThresholdTitle(currentLanguage),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (currentLanguage == AppLanguage.FRENCH) "Alerte si stock ≤ $lowStockThreshold unités"
                                    else if (currentLanguage == AppLanguage.ENGLISH) "Alert when stock ≤ $lowStockThreshold units"
                                    else "يتم التنبيه عند وصول الرصيد إلى $lowStockThreshold قطع أو أقل",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        if (lowStockThreshold > 1) viewModel.setLowStockThreshold(lowStockThreshold - 1)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "$lowStockThreshold",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        if (lowStockThreshold < 50) viewModel.setLowStockThreshold(lowStockThreshold + 1)
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Language Section (AR, FR, EN)
        item {
            SectionHeader(title = AppStrings.languageSectionTitle(currentLanguage))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (currentLanguage) {
                            AppLanguage.FRENCH -> "Choisissez la langue de l'application :"
                            AppLanguage.ENGLISH -> "Select interface language:"
                            AppLanguage.ARABIC -> "اختر لغة الواجهة المفضلة:"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.values().forEach { lang ->
                            val isSelected = currentLanguage == lang
                            OutlinedCard(
                                onClick = { viewModel.setLanguage(lang) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(text = lang.flagEmoji, fontSize = 14.sp)
                                            Text(
                                                text = lang.symbol,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = lang.nativeName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Appearance & Multi-Theme Section
        item {
            SectionHeader(title = AppStrings.appearanceSectionTitle(currentLanguage))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 1. Display Mode (Light / Dark / System)
                    Text(
                        text = AppStrings.displayModeTitle(currentLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.values().forEach { mode ->
                            val isSelected = currentThemeMode == mode
                            val modeIcon = when (mode) {
                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                AppThemeMode.DARK -> Icons.Default.DarkMode
                                AppThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                            }
                            OutlinedCard(
                                onClick = { viewModel.setThemeMode(mode) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = mode.getDisplayName(currentLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Color Palette & Brand Themes Grid
                    Text(
                        text = AppStrings.themePaletteTitle(currentLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = AppStrings.themePaletteDesc(currentLanguage),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // List of themes
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemePalette.values().forEach { palette ->
                            val isSelected = currentThemePalette == palette
                            OutlinedCard(
                                onClick = { viewModel.setThemePalette(palette) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSelected) palette.primaryColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) palette.primaryColor else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(text = palette.iconEmoji, fontSize = 20.sp)
                                        Column {
                                            Text(
                                                text = palette.getDisplayName(currentLanguage),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            // Color Preview Badges
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(palette.primaryColor)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(palette.secondaryColor)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(palette.accentColor)
                                                )
                                            }
                                        }
                                    }

                                    if (isSelected) {
                                        Surface(
                                            shape = CircleShape,
                                            color = palette.primaryColor,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
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
        }

        // Invoice Templates & Thermal Printing Section
        item {
            val templatesSectionTitle = when (currentLanguage) {
                AppLanguage.FRENCH -> "Modèles de Facture & Reçus Thermiques"
                AppLanguage.ENGLISH -> "Invoice & Thermal Receipt Templates"
                AppLanguage.ARABIC -> "نماذج الفاتورة والطباعة الحرارية"
            }
            SectionHeader(title = templatesSectionTitle)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("invoice_templates_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (currentLanguage == AppLanguage.FRENCH) "Modèle actif" else if (currentLanguage == AppLanguage.ENGLISH) "Active Template" else "النموذج المعتمد حالياً",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${currentInvoiceStyle.iconEmoji} ${currentInvoiceStyle.getDisplayName(currentLanguage)} (${currentPaperWidth}mm)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { showInvoiceTemplatesModal = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("change_invoice_template_btn")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.FRENCH) "Changer" else if (currentLanguage == AppLanguage.ENGLISH) "Change" else "تغيير النمط",
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 Quick Selection Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InvoiceStyle.values().forEach { style ->
                            val isSelected = currentInvoiceStyle == style
                            OutlinedCard(
                                onClick = { viewModel.setInvoiceStyle(style) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = style.iconEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = style.getDisplayName(currentLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bluetooth Printer Section
        item {
            val printerDisplayName = connectedPrinter?.name ?: BluetoothPrinterManager.getSavedPrinterName(context)
            SectionHeader(title = AppStrings.printerSectionTitle(currentLanguage))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (connectedPrinter != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Bluetooth,
                                        contentDescription = null,
                                        tint = if (connectedPrinter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(AppStrings.connectedPrinter(currentLanguage), fontWeight = FontWeight.SemiBold)
                                Text(
                                    printerDisplayName,
                                    fontSize = 12.sp,
                                    color = if (connectedPrinter != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showPrinterDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("بحث واتصال", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            isTestingPrint = true
                            BluetoothPrinterManager.printTestReceipt(context, coroutineScope) { success, msg ->
                                isTestingPrint = false
                                viewModel.showMessage(msg)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isTestingPrint
                    ) {
                        if (isTestingPrint) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(AppStrings.testPrint(currentLanguage))
                        }
                    }
                }
            }
        }

        // USB & Built-in Thermal POS Printer Section
        item {
            val isUsbActive = isBuiltInPrinterMode || connectedUsbPrinter != null
            val usbDisplayName = if (isBuiltInPrinterMode) {
                "الطابعة المدمجة في جهاز الكاشير (نشطة)"
            } else if (connectedUsbPrinter != null) {
                connectedUsbPrinter?.displayName ?: "طابعة USB متصلة"
            } else {
                "غير محددة (اضغط للإعداد والتوصيل)"
            }

            SectionHeader(title = AppStrings.usbPrinterSectionTitle(currentLanguage))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("usb_printer_settings_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isUsbActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isBuiltInPrinterMode) Icons.Default.PointOfSale else Icons.Default.Usb,
                                        contentDescription = null,
                                        tint = if (isUsbActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isBuiltInPrinterMode) "طابعة الكاشير المدمجة" else "طابعة حرارية USB / OTG",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = usbDisplayName,
                                    fontSize = 12.sp,
                                    color = if (isUsbActive) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isUsbActive) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        Button(
                            onClick = { showUsbPrinterDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إعداد وفحص", fontSize = 12.sp)
                        }
                    }

                    // Configuration Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("مقاس $usbPaperWidth مم", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(13.dp), tint = if (usbAutoCut) SuccessGreen else MaterialTheme.colorScheme.outline)
                                Text("قص تلقائي: ${if (usbAutoCut) "نعم" else "لا"}", fontSize = 11.sp)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(13.dp), tint = if (usbDrawerKick) SuccessGreen else MaterialTheme.colorScheme.outline)
                                Text("درج نقود: ${if (usbDrawerKick) "نعم" else "لا"}", fontSize = 11.sp)
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isTestingUsbPrint = true
                                UsbAndBuiltInPrinterManager.printTestReceipt(context, coroutineScope, storeName) { success, msg ->
                                    isTestingUsbPrint = false
                                    viewModel.showMessage(msg)
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isTestingUsbPrint
                        ) {
                            if (isTestingUsbPrint) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة تجريبية (USB/مدمجة)", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                isKickingDrawerQuick = true
                                UsbAndBuiltInPrinterManager.kickCashDrawer(context, coroutineScope) { success, msg ->
                                    isKickingDrawerQuick = false
                                    viewModel.showMessage(msg)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isKickingDrawerQuick
                        ) {
                            if (isKickingDrawerQuick) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فتح الدرج", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Data Management (CSV & Backup) Section
        item {
            SectionHeader(title = AppStrings.dataBackupSectionTitle(currentLanguage))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    SettingActionRow(
                        icon = Icons.Default.FileDownload,
                        title = AppStrings.exportProductsCsv(currentLanguage),
                        subtitle = if (currentLanguage == AppLanguage.FRENCH) "Sauvegarder articles, prix et stock" else "حفظ نسخة من قائمة الأصناف والأسعار والمخزون",
                        onClick = { exportProductsLauncher.launch("antar_products_${System.currentTimeMillis()}.csv") }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                    SettingActionRow(
                        icon = Icons.Default.FileUpload,
                        title = AppStrings.importProductsCsv(currentLanguage),
                        subtitle = if (currentLanguage == AppLanguage.FRENCH) "Importer de nouveaux articles automatiquement" else "إضافة أصناف جديدة تلقائياً من ملف CSV",
                        onClick = { importProductsLauncher.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values")) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                    SettingActionRow(
                        icon = Icons.Default.ReceiptLong,
                        title = AppStrings.exportSalesCsv(currentLanguage),
                        subtitle = if (currentLanguage == AppLanguage.FRENCH) "Exporter le journal des ventes et encaissements" else "استخراج أرشيف العمليات المحاسبية",
                        onClick = { exportSalesLauncher.launch("antar_sales_${System.currentTimeMillis()}.csv") }
                    )

                    if (currentUser?.role == "admin") {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                        SettingActionRow(
                            icon = Icons.Default.Restore,
                            title = AppStrings.resetDatabase(currentLanguage),
                            subtitle = if (currentLanguage == AppLanguage.FRENCH) "Réinitialiser les données aux valeurs initiales" else "تفريغ العمليات واستعادة بيانات البداية",
                            isDestructive = true,
                            onClick = { showResetConfirmDialog = true }
                        )
                    }
                }
            }
        }

        // Users Management Section (Visible to Admin)
        if (currentUser?.role == "admin") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(title = AppStrings.userManagementSectionTitle(currentLanguage))
                    TextButton(onClick = { showAddUserDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.addUser(currentLanguage), fontSize = 13.sp)
                    }
                }
            }

            if (visibleUsers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.GroupAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.FRENCH) "Aucun utilisateur supplémentaire"
                                else if (currentLanguage == AppLanguage.ENGLISH) "No additional users added"
                                else "لا يوجد مستخدمون أو بائعون إضافيون",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (currentLanguage == AppLanguage.FRENCH) "Cliquez sur 'Ajouter un utilisateur' pour créer des comptes pour vos vendeurs ou caissiers."
                                else if (currentLanguage == AppLanguage.ENGLISH) "Click 'Add User' to create accounts for your sellers or cashiers."
                                else "يمكنك إضافة حسابات مخصصة للبائعين والمحاسبين بالضغط على «إضافة مستخدم».",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(visibleUsers, key = { it.id }) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = user.username, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = if (user.role == "admin") "مدير (Admin)" else "بائع (Seller)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (user.id != currentUser?.id) {
                                IconButton(onClick = { viewModel.deleteUser(user.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف المستخدم", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // About Info
        item {
            SectionHeader(title = "ℹ️ معلومات النظام")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("نظام AF store للإدارة والمبيعات", fontWeight = FontWeight.Bold)
                    Text("الإصدار: 2.0.0 - نسخة متكاملة غير متصلة بالإنترنت (Offline First)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("يدعم طباعة الفواتير عبر البلوتوث، تصدير ملفات PDF، وإدارة شاملة للمخزون وتقارير المبيعات.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // ===== Add User Dialog =====
    if (showAddUserDialog) {
        var newUsername by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var newRole by remember { mutableStateOf("seller") }

        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("إضافة مستخدم جديد", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("اسم المستخدم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("كلمة المرور") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("الصلاحية والنوع:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = newRole == "seller",
                            onClick = { newRole = "seller" },
                            label = { Text("بائع (نقطة بيع)") }
                        )
                        FilterChip(
                            selected = newRole == "admin",
                            onClick = { newRole = "admin" },
                            label = { Text("مدير نظام") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUsername.isNotBlank() && newPassword.isNotBlank()) {
                            viewModel.addNewUser(newUsername, newPassword, newRole)
                            showAddUserDialog = false
                        }
                    },
                    enabled = newUsername.isNotBlank() && newPassword.length >= 3
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (currentLanguage == AppLanguage.FRENCH) "Déconnexion"
                    else if (currentLanguage == AppLanguage.ENGLISH) "Log Out"
                    else "تسجيل الخروج",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (currentLanguage == AppLanguage.FRENCH) "Êtes-vous sûr de vouloir vous déconnecter de votre compte ?"
                    else if (currentLanguage == AppLanguage.ENGLISH) "Are you sure you want to log out of your account?"
                    else "هل أنت متأكد من رغبتك في تسجيل الخروج من الحساب الحالي؟",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.FRENCH) "Se déconnecter"
                        else if (currentLanguage == AppLanguage.ENGLISH) "Log Out"
                        else "نعم، خروج"
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(AppStrings.cancel(currentLanguage))
                }
            }
        )
    }

    // ===== Bluetooth Thermal Printer Modal =====
    if (showPrinterDialog) {
        BluetoothPrinterModal(
            onDismiss = { showPrinterDialog = false },
            onPrinterConnected = { printerName ->
                viewModel.showMessage("تم تعيين والاتصال بالطابعة: $printerName")
            }
        )
    }

    // ===== USB & Built-in Thermal Printer Modal =====
    if (showUsbPrinterDialog) {
        UsbPrinterModal(
            currentLang = currentLanguage,
            onDismiss = { showUsbPrinterDialog = false },
            onPrinterConfigured = { printerName ->
                viewModel.showMessage("تم ضبط طابعة $printerName بنجاح")
            }
        )
    }

    // ===== Invoice Template & Thermal POS Selector Modal =====
    if (showInvoiceTemplatesModal) {
        InvoiceTemplateSelectorModal(
            currentStyle = currentInvoiceStyle,
            currentPaperWidth = currentPaperWidth,
            currentLanguage = currentLanguage,
            onSelectStyle = { style ->
                viewModel.setInvoiceStyle(style)
            },
            onSelectPaperWidth = { width ->
                viewModel.setThermalPaperWidth(width)
            },
            onDismiss = { showInvoiceTemplatesModal = false }
        )
    }

    // ===== Reset Database Confirm =====
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("تأكيد إعادة ضبط البيانات", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من مسح جميع المنتجات وسجلات المبيعات وإعادة تهيئة متجر AF store؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، إعادة ضبط")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // ===== Edit Store Name & Details Dialog =====
    if (showEditStoreDialog) {
        var tempStoreName by remember { mutableStateOf(storeName) }
        var tempStorePhone by remember { mutableStateOf(storePhone) }

        AlertDialog(
            onDismissRequest = { showEditStoreDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentLanguage == AppLanguage.FRENCH) "Modifier les infos du magasin"
                        else if (currentLanguage == AppLanguage.ENGLISH) "Edit Store Information"
                        else "تعديل بيانات واسم المتجر",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempStoreName,
                        onValueChange = { tempStoreName = it },
                        label = {
                            Text(
                                if (currentLanguage == AppLanguage.FRENCH) "Nom du magasin *"
                                else if (currentLanguage == AppLanguage.ENGLISH) "Store Name *"
                                else "اسم المتجر / النشاط التجاري *"
                            )
                        },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("store_name_input")
                    )

                    OutlinedTextField(
                        value = tempStorePhone,
                        onValueChange = { tempStorePhone = it },
                        label = {
                            Text(
                                if (currentLanguage == AppLanguage.FRENCH) "Numéro de téléphone"
                                else if (currentLanguage == AppLanguage.ENGLISH) "Store Phone Number"
                                else "رقم هاتف المتجر (يظهر في الفاتورة)"
                            )
                        },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("store_phone_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempStoreName.isNotBlank()) {
                            viewModel.setStoreName(tempStoreName)
                            viewModel.setStorePhone(tempStorePhone)
                            showEditStoreDialog = false
                        }
                    },
                    enabled = tempStoreName.isNotBlank(),
                    modifier = Modifier.testTag("save_store_name_btn")
                ) {
                    Text(AppStrings.save(currentLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditStoreDialog = false }) {
                    Text(AppStrings.cancel(currentLanguage))
                }
            }
        )
    }

    // ===== Custom Currency Dialog =====
    if (showCustomCurrencyDialog) {
        var tempCurrency by remember { mutableStateOf(currencySymbol) }

        AlertDialog(
            onDismissRequest = { showCustomCurrencyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Paid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentLanguage == AppLanguage.FRENCH) "Définir une devise personnalisée"
                        else if (currentLanguage == AppLanguage.ENGLISH) "Set Custom Currency"
                        else "تحديد رمز عملة مخصص",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.FRENCH) "Entrez le symbole ou code de la devise (ex: USD, SAR, د.إ, DT) :"
                        else if (currentLanguage == AppLanguage.ENGLISH) "Enter currency symbol or code (e.g. USD, SAR, AED, €) :"
                        else "أدخل رمز أو اختصار العملة المُراد اعتمادها في الحسابات (مثال: ج.م، ر.س، د.إ، USD، EUR) :",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempCurrency,
                        onValueChange = { tempCurrency = it },
                        label = {
                            Text(
                                if (currentLanguage == AppLanguage.FRENCH) "Symbole de devise"
                                else if (currentLanguage == AppLanguage.ENGLISH) "Currency Symbol"
                                else "رمز العملة"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_currency_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempCurrency.isNotBlank()) {
                            viewModel.setCurrencySymbol(tempCurrency)
                            showCustomCurrencyDialog = false
                        }
                    },
                    enabled = tempCurrency.isNotBlank(),
                    modifier = Modifier.testTag("save_custom_currency_btn")
                ) {
                    Text(AppStrings.save(currentLanguage))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCurrencyDialog = false }) {
                    Text(AppStrings.cancel(currentLanguage))
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
