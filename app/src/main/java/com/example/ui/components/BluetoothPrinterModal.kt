package com.example.ui.components

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.utils.BluetoothPrinterDevice
import com.example.utils.BluetoothPrinterManager
import com.example.utils.PrinterConnectionStatus
import kotlinx.coroutines.launch

/**
 * Modern, full-featured Bluetooth Thermal POS Printer Modal.
 * Provides live device discovery, pairing, direct SPP socket connection,
 * paper width toggle (58mm/80mm), and instant test printing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothPrinterModal(
    onDismiss: () -> Unit,
    onPrinterConnected: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pairedDevices by BluetoothPrinterManager.pairedPrinters.collectAsState()
    val discoveredDevices by BluetoothPrinterManager.discoveredPrinters.collectAsState()
    val isScanning by BluetoothPrinterManager.isScanning.collectAsState()
    val connectionStatus by BluetoothPrinterManager.connectionStatus.collectAsState()
    val connectedDevice by BluetoothPrinterManager.connectedDevice.collectAsState()
    val lastMessage by BluetoothPrinterManager.lastMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPaperWidth by remember { mutableIntStateOf(BluetoothPrinterManager.getSavedPaperWidth(context)) }
    var isTestingPrint by remember { mutableStateOf(false) }
    var connectingAddress by remember { mutableStateOf<String?>(null) }
    var hasPermissions by remember { mutableStateOf(BluetoothPrinterManager.hasRequiredPermissions(context)) }
    var isBtEnabled by remember { mutableStateOf(BluetoothPrinterManager.isBluetoothEnabled(context)) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        hasPermissions = allGranted
        if (allGranted) {
            BluetoothPrinterManager.loadPairedDevices(context)
            BluetoothPrinterManager.startDiscovery(context)
        }
    }

    // Refresh state and load devices upon opening
    LaunchedEffect(Unit) {
        BluetoothPrinterManager.init(context)
        hasPermissions = BluetoothPrinterManager.hasRequiredPermissions(context)
        isBtEnabled = BluetoothPrinterManager.isBluetoothEnabled(context)
        if (hasPermissions) {
            BluetoothPrinterManager.loadPairedDevices(context)
        } else {
            permissionLauncher.launch(BluetoothPrinterManager.getPermissionsToRequest())
        }
    }

    // Clean up scanner when dismissed
    DisposableEffect(Unit) {
        onDispose {
            BluetoothPrinterManager.stopDiscovery(context)
        }
    }

    // Pulsing animation for scanning/active connection
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Dialog(
        onDismissRequest = {
            BluetoothPrinterManager.stopDiscovery(context)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp)
                .padding(vertical = 16.dp)
                .testTag("bluetooth_printer_modal"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // ===== Header =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Print,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "إعدادات الطابعة الحرارية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "البحث والاتصال بطابعات البلوتوث (ESC/POS)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = {
                        BluetoothPrinterManager.stopDiscovery(context)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ===== Connection & Status Card =====
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (connectedDevice != null)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (connectedDevice != null) SuccessGreen else WarningOrange)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (connectedDevice != null) "الطابعة المتصلة الحالية:" else "لا توجد طابعة متصلة حالياً",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (connectedDevice != null) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Paper width chip
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = selectedPaperWidth == 58,
                                    onClick = {
                                        selectedPaperWidth = 58
                                        BluetoothPrinterManager.setSavedPaperWidth(context, 58)
                                    },
                                    label = { Text("58 مم", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.height(30.dp)
                                )
                                FilterChip(
                                    selected = selectedPaperWidth == 80,
                                    onClick = {
                                        selectedPaperWidth = 80
                                        BluetoothPrinterManager.setSavedPaperWidth(context, 80)
                                    },
                                    label = { Text("80 مم", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }

                        if (connectedDevice != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = connectedDevice?.name ?: "طابعة حرارية",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "MAC: ${connectedDevice?.address}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            isTestingPrint = true
                                            BluetoothPrinterManager.printTestReceipt(context, coroutineScope) { _, _ ->
                                                isTestingPrint = false
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(36.dp),
                                        enabled = !isTestingPrint
                                    ) {
                                        if (isTestingPrint) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("اختبار", fontSize = 11.sp)
                                        }
                                    }

                                    TextButton(
                                        onClick = { BluetoothPrinterManager.disconnect() },
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("قطع", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                // Status message banner
                lastMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ===== Bluetooth / Permission Warnings =====
                if (!hasPermissions) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("إذن البلوتوث مطلوب للبحث عن الطابعات", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("يرجى منح الإذن للوصول إلى طابعات الفواتير", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { permissionLauncher.launch(BluetoothPrinterManager.getPermissionsToRequest()) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("منح الإذن", fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // ===== Tabs for Paired vs Discovery =====
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BluetoothConnected, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("الأجهزة المقترنة (${pairedDevices.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            if (hasPermissions && !isScanning) {
                                BluetoothPrinterManager.startDiscovery(context)
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.BluetoothSearching,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isScanning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("بحث عن طابعات (${discoveredDevices.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                if (isScanning && selectedTab == 1) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ===== Tab Contents =====
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        // Paired Devices List
                        if (pairedDevices.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "لا توجد أجهزة مقترنة حالياً",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "انتقل لتبويب 'بحث عن طابعات' للعثور على الطابعة والاتصال بها",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = {
                                    selectedTab = 1
                                    BluetoothPrinterManager.startDiscovery(context)
                                }) {
                                    Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("بدء البحث عن طابعات")
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(pairedDevices, key = { it.address }) { dev ->
                                    val isCurrent = connectedDevice?.address == dev.address
                                    val isConnectingThis = connectingAddress == dev.address

                                    PrinterDeviceRow(
                                        device = dev,
                                        isConnected = isCurrent,
                                        isConnecting = isConnectingThis,
                                        onConnect = {
                                            connectingAddress = dev.address
                                            BluetoothPrinterManager.connectToDevice(
                                                context = context,
                                                device = dev,
                                                coroutineScope = coroutineScope
                                            ) { success, _ ->
                                                connectingAddress = null
                                                if (success) {
                                                    onPrinterConnected(dev.name)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Live Discovery List
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isScanning) "جاري البحث عن أجهزة بلوتوث قريبة..." else "انقر لبدء البحث عن طابعات جديدة",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Button(
                                    onClick = {
                                        if (isScanning) {
                                            BluetoothPrinterManager.stopDiscovery(context)
                                        } else {
                                            if (hasPermissions) {
                                                BluetoothPrinterManager.startDiscovery(context)
                                            } else {
                                                permissionLauncher.launch(BluetoothPrinterManager.getPermissionsToRequest())
                                            }
                                        }
                                    },
                                    colors = if (isScanning) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    if (isScanning) {
                                        Text("إيقاف البحث", fontSize = 11.sp)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("بحث جديد", fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (discoveredDevices.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("جاري استكشاف الطابعات القريبة...", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("تأكد من تشغيل الطابعة الحرارية وتفعيل البلوتوث بها", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Icon(
                                            Icons.Default.BluetoothSearching,
                                            contentDescription = null,
                                            modifier = Modifier.size(44.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("لم يتم العثور على أجهزة بعد", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("اضغط على 'بحث جديد' للبدء في اكتشاف الطابعات", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(discoveredDevices, key = { it.address }) { dev ->
                                        val isCurrent = connectedDevice?.address == dev.address
                                        val isConnectingThis = connectingAddress == dev.address

                                        PrinterDeviceRow(
                                            device = dev,
                                            isConnected = isCurrent,
                                            isConnecting = isConnectingThis,
                                            onConnect = {
                                                connectingAddress = dev.address
                                                BluetoothPrinterManager.connectToDevice(
                                                    context = context,
                                                    device = dev,
                                                    coroutineScope = coroutineScope
                                                ) { success, _ ->
                                                    connectingAddress = null
                                                    if (success) {
                                                        onPrinterConnected(dev.name)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ===== Footer Controls =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                context.startActivity(intent)
                            } catch (ignored: Exception) {
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعدادات البلوتوث بالنظام", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            BluetoothPrinterManager.stopDiscovery(context)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تم والعودة")
                    }
                }
            }
        }
    }
}

/**
 * Individual row item for Bluetooth device list
 */
@Composable
private fun PrinterDeviceRow(
    device: BluetoothPrinterDevice,
    isConnected: Boolean,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isConnecting) { onConnect() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isConnected)
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (device.isLikelyPrinter)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (device.isLikelyPrinter) Icons.Default.Print else Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = if (device.isLikelyPrinter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        if (device.isLikelyPrinter) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "طابعة POS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "MAC: ${device.address}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isConnected) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SuccessGreen.copy(alpha = 0.15f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "متصلة",
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            } else if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                FilledTonalButton(
                    onClick = onConnect,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("اتصال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
