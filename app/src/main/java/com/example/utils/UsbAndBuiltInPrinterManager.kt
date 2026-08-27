package com.example.utils

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import com.example.data.models.InvoiceStyle
import com.example.ui.components.InvoicePreviewData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Model representing a detected USB thermal printer device.
 */
data class UsbPrinterDevice(
    val deviceId: Int,
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val manufacturerName: String = "طابعة USB",
    val productName: String = "POS Thermal Printer",
    val isLikelyPrinter: Boolean = true,
    val hasPermission: Boolean = false
) {
    val displayName: String
        get() {
            val name = if (productName.isNotBlank() && productName != "POS Thermal Printer") {
                productName
            } else if (manufacturerName.isNotBlank() && manufacturerName != "طابعة USB") {
                "$manufacturerName USB"
            } else {
                "طابعة حرارية USB (VID:$vendorId)"
            }
            return name
        }
}

/**
 * Manager for Built-in POS Printers & USB OTG Thermal Receipt Printers.
 * Supports ESC/POS raw USB transfer, Android PrintManager integration,
 * paper cut, cash drawer kick, and terminal hardware detection.
 */
object UsbAndBuiltInPrinterManager {

    private const val TAG = "UsbBuiltInPrinterMgr"
    private const val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"

    private const val PREFS_NAME = "af_store_usb_printer_prefs"
    private const val KEY_BUILTIN_ENABLED = "builtin_pos_printer_enabled"
    private const val KEY_SELECTED_USB_ID = "selected_usb_device_id"
    private const val KEY_SELECTED_USB_NAME = "selected_usb_device_name"
    private const val KEY_PAPER_WIDTH = "usb_paper_width_mm"
    private const val KEY_AUTO_CUT = "usb_auto_cut_paper"
    private const val KEY_DRAWER_KICK = "usb_drawer_kick_on_sale"

    private val _connectedUsbDevice = MutableStateFlow<UsbPrinterDevice?>(null)
    val connectedUsbDevice: StateFlow<UsbPrinterDevice?> = _connectedUsbDevice.asStateFlow()

    private val _discoveredUsbPrinters = MutableStateFlow<List<UsbPrinterDevice>>(emptyList())
    val discoveredUsbPrinters: StateFlow<List<UsbPrinterDevice>> = _discoveredUsbPrinters.asStateFlow()

    private val _isBuiltInPrinterMode = MutableStateFlow(false)
    val isBuiltInPrinterMode: StateFlow<Boolean> = _isBuiltInPrinterMode.asStateFlow()

    private val _autoCutEnabled = MutableStateFlow(true)
    val autoCutEnabled: StateFlow<Boolean> = _autoCutEnabled.asStateFlow()

    private val _drawerKickEnabled = MutableStateFlow(true)
    val drawerKickEnabled: StateFlow<Boolean> = _drawerKickEnabled.asStateFlow()

    private val _paperWidth = MutableStateFlow(58)
    val paperWidth: StateFlow<Int> = _paperWidth.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var activeUsbConnection: UsbDeviceConnection? = null
    private var activeUsbInterface: UsbInterface? = null
    private var activeUsbEndpoint: UsbEndpoint? = null

    private var isReceiverRegistered = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        }

                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        if (granted && device != null && context != null) {
                            _statusMessage.value = "✅ تم منح إذن الوصول لطابعة USB"
                            scanAndConnectUsbDevice(context, device.deviceId)
                        } else {
                            _statusMessage.value = "⚠️ تم رفض إذن USB من قبل المستخدم"
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    context?.let { scanUsbDevices(it) }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    context?.let { scanUsbDevices(it) }
                }
            }
        }
    }

    /**
     * Initializes preferences and registers USB listeners.
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isBuiltInPrinterMode.value = prefs.getBoolean(KEY_BUILTIN_ENABLED, false)
        _autoCutEnabled.value = prefs.getBoolean(KEY_AUTO_CUT, true)
        _drawerKickEnabled.value = prefs.getBoolean(KEY_DRAWER_KICK, true)
        _paperWidth.value = prefs.getInt(KEY_PAPER_WIDTH, 58)

        val savedName = prefs.getString(KEY_SELECTED_USB_NAME, null)
        val savedId = prefs.getInt(KEY_SELECTED_USB_ID, -1)

        if (savedName != null && savedId != -1) {
            _connectedUsbDevice.value = UsbPrinterDevice(
                deviceId = savedId,
                deviceName = savedName,
                vendorId = 0,
                productId = 0,
                productName = savedName,
                hasPermission = true
            )
        }

        registerReceiver(context)
        scanUsbDevices(context)
    }

    private fun registerReceiver(context: Context) {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(ACTION_USB_PERMISSION)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(usbReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    /**
     * Scans for all connected USB devices and filters potential POS thermal receipt printers.
     */
    fun scanUsbDevices(context: Context) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return
        val deviceList = usbManager.deviceList

        val printers = mutableListOf<UsbPrinterDevice>()

        for ((_, device) in deviceList) {
            var isPrinter = false
            // Check interface classes for USB_CLASS_PRINTER (7)
            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                if (iface.interfaceClass == UsbConstants.USB_CLASS_PRINTER ||
                    iface.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC ||
                    iface.interfaceClass == UsbConstants.USB_CLASS_COMM ||
                    iface.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA
                ) {
                    isPrinter = true
                    break
                }
            }

            // Also check known thermal printer VID/PIDs (Xprinter, Rongta, Epson, Bixolon, Goojprt, Sunmi, etc.)
            val knownPrinter = isPrinter || isKnownPrinterVendor(device.vendorId)

            val usbDev = UsbPrinterDevice(
                deviceId = device.deviceId,
                deviceName = device.deviceName,
                vendorId = device.vendorId,
                productId = device.productId,
                manufacturerName = device.manufacturerName ?: "طابعة USB",
                productName = device.productName ?: "POS USB Thermal Printer",
                isLikelyPrinter = knownPrinter,
                hasPermission = usbManager.hasPermission(device)
            )
            printers.add(usbDev)
        }

        _discoveredUsbPrinters.value = printers

        // Auto-select if single printer found and not yet selected
        if (_connectedUsbDevice.value == null && printers.isNotEmpty()) {
            val best = printers.firstOrNull { it.isLikelyPrinter } ?: printers.first()
            _connectedUsbDevice.value = best
        }
    }

    private fun isKnownPrinterVendor(vid: Int): Boolean {
        return vid in listOf(
            0x0416, // Winbond / Xprinter
            0x0fe6, // ICS
            0x04b8, // Seiko Epson
            0x1504, // Bixolon
            0x0dd4, // Custom Engineering
            0x1fc9, // NXP / Thermal
            0x6868, // Pos-58 / Rongta
            0x28e9, // Gprinter
            0x1a86, // QinHeng Electronics (USB to Serial / POS)
            0x10c4, // Silicon Labs (CP210x POS)
            0x0403  // FTDI POS
        )
    }

    /**
     * Request permission for USB device if not yet granted.
     */
    fun requestUsbPermission(context: Context, usbDeviceModel: UsbPrinterDevice) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return
        val rawDevice = usbManager.deviceList.values.firstOrNull { it.deviceId == usbDeviceModel.deviceId }

        if (rawDevice == null) {
            _statusMessage.value = "⚠️ لم يتم العثور على جهاز USB متصل"
            return
        }

        if (usbManager.hasPermission(rawDevice)) {
            _statusMessage.value = "✅ الإذن ممنوح بالفعل لطابعة USB"
            scanAndConnectUsbDevice(context, rawDevice.deviceId)
        } else {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                flags
            )
            usbManager.requestPermission(rawDevice, permissionIntent)
            _statusMessage.value = "جاري طلب إذن USB من النظام..."
        }
    }

    /**
     * Connects to USB device, claims interface and locates OUT endpoint.
     */
    fun scanAndConnectUsbDevice(context: Context, deviceId: Int): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return false
        val device = usbManager.deviceList.values.firstOrNull { it.deviceId == deviceId } ?: return false

        if (!usbManager.hasPermission(device)) {
            return false
        }

        try {
            closeUsbConnection()

            val connection = usbManager.openDevice(device) ?: return false
            var printerInterface: UsbInterface? = null
            var outEndpoint: UsbEndpoint? = null

            for (i in 0 until device.interfaceCount) {
                val iface = device.getInterface(i)
                for (j in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(j)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK && ep.direction == UsbConstants.USB_DIR_OUT) {
                        printerInterface = iface
                        outEndpoint = ep
                        break
                    }
                }
                if (outEndpoint != null) break
            }

            if (printerInterface != null && outEndpoint != null) {
                connection.claimInterface(printerInterface, true)
                activeUsbConnection = connection
                activeUsbInterface = printerInterface
                activeUsbEndpoint = outEndpoint

                val devModel = UsbPrinterDevice(
                    deviceId = device.deviceId,
                    deviceName = device.deviceName,
                    vendorId = device.vendorId,
                    productId = device.productId,
                    manufacturerName = device.manufacturerName ?: "طابعة USB",
                    productName = device.productName ?: "طابعة إيصالات حرارية USB",
                    isLikelyPrinter = true,
                    hasPermission = true
                )
                _connectedUsbDevice.value = devModel

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putInt(KEY_SELECTED_USB_ID, device.deviceId)
                    .putString(KEY_SELECTED_USB_NAME, devModel.displayName)
                    .apply()

                _statusMessage.value = "✅ متصل بطابعة USB: ${devModel.displayName}"
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to USB device", e)
            _statusMessage.value = "خطأ في الاتصال بطابعة USB: ${e.message}"
        }
        return false
    }

    private fun closeUsbConnection() {
        try {
            activeUsbInterface?.let { activeUsbConnection?.releaseInterface(it) }
            activeUsbConnection?.close()
        } catch (ignored: Exception) {
        } finally {
            activeUsbConnection = null
            activeUsbInterface = null
            activeUsbEndpoint = null
        }
    }

    /**
     * Toggles Built-in POS Terminal Printer Mode.
     */
    fun setBuiltInPrinterMode(context: Context, enabled: Boolean) {
        _isBuiltInPrinterMode.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BUILTIN_ENABLED, enabled).apply()
        _statusMessage.value = if (enabled) "✅ تم تفعيل وضع الطابعة المدمجة في جهاز الكاشير" else "تم إلغاء تفعيل الطابعة المدمجة"
    }

    fun setPaperWidth(context: Context, width: Int) {
        _paperWidth.value = width
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_PAPER_WIDTH, width).apply()
    }

    fun setAutoCut(context: Context, enabled: Boolean) {
        _autoCutEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_CUT, enabled).apply()
    }

    fun setDrawerKick(context: Context, enabled: Boolean) {
        _drawerKickEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DRAWER_KICK, enabled).apply()
    }

    /**
     * Sends ESC/POS Cash Drawer Kick command (ESC p 0 25 250).
     */
    fun kickCashDrawer(context: Context, coroutineScope: CoroutineScope, onResult: ((Boolean, String) -> Unit)? = null) {
        coroutineScope.launch {
            val drawerBytes = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())
            val success = sendRawBytes(context, drawerBytes)
            withContext(Dispatchers.Main) {
                if (success) {
                    onResult?.invoke(true, "✅ تم إرسال أمر فتح درج الكاشير بنجاح")
                } else {
                    onResult?.invoke(false, "⚠️ تعذر إرسال نبضة الدرج (تأكد من توصيل درج النقود بالطابعة)")
                }
            }
        }
    }

    /**
     * Creates and prints a Test Receipt for Built-in & USB thermal printer.
     */
    fun printTestReceipt(
        context: Context,
        coroutineScope: CoroutineScope,
        storeName: String = "AF store",
        onResult: (Boolean, String) -> Unit
    ) {
        coroutineScope.launch {
            val widthMm = _paperWidth.value
            val widthChars = if (widthMm == 80) 48 else 32
            val sep = "=".repeat(widthChars)
            val dash = "-".repeat(widthChars)
            val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())

            val bytes = mutableListOf<Byte>()

            // ESC @ (Initialize)
            bytes.addAll(listOf(0x1B.toByte(), 0x40.toByte()))

            // Open cash drawer if enabled
            if (_drawerKickEnabled.value) {
                bytes.addAll(listOf(0x1B.toByte(), 0x70.toByte(), 0x00.toByte(), 0x19.toByte(), 0xFA.toByte()))
            }

            // Align Center
            bytes.addAll(listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte()))

            // Header in Double Size & Bold
            bytes.addAll(listOf(0x1B.toByte(), 0x45.toByte(), 0x01.toByte()))
            bytes.addAll(listOf(0x1D.toByte(), 0x21.toByte(), 0x11.toByte()))
            bytes.addAll("$storeName\n".toByteArray(Charset.forName("UTF-8")).toList())

            // Normal size
            bytes.addAll(listOf(0x1D.toByte(), 0x21.toByte(), 0x00.toByte()))
            bytes.addAll(listOf(0x1B.toByte(), 0x45.toByte(), 0x00.toByte()))
            bytes.addAll("إيصال تجريبي - طابعة USB / مدمجة\n".toByteArray(Charset.forName("UTF-8")).toList())
            bytes.addAll("USB & Built-in Thermal POS Receipt\n".toByteArray(Charset.forName("UTF-8")).toList())
            bytes.addAll("$sep\n".toByteArray(Charset.forName("UTF-8")).toList())

            // Align Left Details
            bytes.addAll(listOf(0x1B.toByte(), 0x61.toByte(), 0x00.toByte()))
            bytes.addAll("التاريخ: $dateStr\n".toByteArray(Charset.forName("UTF-8")).toList())
            bytes.addAll("نوع الطابعة: ${if (_isBuiltInPrinterMode.value) "طابعة جهاز الكاشير المدمجة" else "طابعة USB OTG خارجية"}\n".toByteArray(Charset.forName("UTF-8")).toList())
            bytes.addAll("عرض الورق: ${widthMm}mm (${widthChars} حرف)\n".toByteArray(Charset.forName("UTF-8")).toList())
            bytes.addAll("حالة الاتصال: متصل وجاهز للطباعة OK\n".toByteArray(Charset.forName("UTF-8")).toList())
            bytes.addAll("خاصية درج الكاشير: ${if (_drawerKickEnabled.value) "مفعلة ✓" else "معطلة"}\n".toByteArray(Charset.forName("UTF-8")).toList())
            bytes.addAll("خاصية قص الورق: ${if (_autoCutEnabled.value) "مفعلة ✓" else "معطلة"}\n".toByteArray(Charset.forName("UTF-8")).toList())
            bytes.addAll("$dash\n".toByteArray(Charset.forName("UTF-8")).toList())

            // Align Center
            bytes.addAll(listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte()))
            bytes.addAll("نظام إدارة المبيعات والمخزون\n".toByteArray(Charset.forName("UTF-8")).toList())
            bytes.addAll("AF STORE POS SYSTEM\n".toByteArray(Charset.forName("UTF-8")).toList())
            bytes.addAll("جاهز لطباعة فواتير المبيعات فورياً\n".toByteArray(Charset.forName("UTF-8")).toList())
            bytes.addAll("$sep\n\n\n\n".toByteArray(Charset.forName("UTF-8")).toList())

            // Auto cut
            if (_autoCutEnabled.value) {
                bytes.addAll(listOf(0x1D.toByte(), 0x56.toByte(), 0x41.toByte(), 0x10.toByte()))
            }

            val success = sendRawBytes(context, bytes.toByteArray())

            withContext(Dispatchers.Main) {
                if (success) {
                    onResult(true, "✅ تمت الطباعة التجريبية بنجاح على طابعة USB / المدمجة")
                } else {
                    // Fallback to Android System Print if no physical USB endpoint opened
                    printViaSystemPrintService(context, storeName, widthMm)
                    onResult(true, "✅ تم إرسال أمر الطباعة التجريبية إلى خدمة طباعة النظام")
                }
            }
        }
    }

    /**
     * Prints an actual customer sales invoice on USB or Built-in thermal printer.
     */
    fun printInvoice(
        context: Context,
        invoice: InvoicePreviewData,
        coroutineScope: CoroutineScope,
        style: InvoiceStyle = InvoiceStyle.DETAILED,
        onResult: (Boolean, String) -> Unit
    ) {
        coroutineScope.launch {
            val widthMm = _paperWidth.value
            var rawBytes = try {
                val bitmap = ThermalBitmapRenderer.renderInvoiceToBitmap(context, invoice, widthMm, style)
                ThermalBitmapRenderer.convertBitmapToEscPosBytes(bitmap)
            } catch (e: Exception) {
                BluetoothPrinterManager.createInvoiceBytes(invoice, widthMm, style)
            }

            // Prepend drawer kick if enabled
            if (_drawerKickEnabled.value) {
                val kick = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())
                rawBytes = kick + rawBytes
            }

            val success = sendRawBytes(context, rawBytes)
            withContext(Dispatchers.Main) {
                if (success) {
                    onResult(true, "✅ تم طباعة الفاتورة #${invoice.invoiceNumber} عبر طابعة USB / المدمجة")
                } else {
                    // Fallback to System Print if USB transfer fails
                    val pdfFile = PdfInvoiceHelper.createPdfFromInvoiceData(context, invoice)
                    if (pdfFile != null) {
                        PdfInvoiceHelper.printPdfViaSystem(context, pdfFile, "فاتورة #${invoice.invoiceNumber}")
                        onResult(true, "تم فتح نافذة الطباعة للفاتورة #${invoice.invoiceNumber}")
                    } else {
                        onResult(false, "تعذر إنشاء ملف الطباعة")
                    }
                }
            }
        }
    }

    private suspend fun sendRawBytes(context: Context, bytes: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (activeUsbConnection != null && activeUsbEndpoint != null) {
                    val transferred = activeUsbConnection?.bulkTransfer(activeUsbEndpoint, bytes, bytes.size, 5000) ?: -1
                    if (transferred >= 0) return@withContext true
                }

                // If not connected, try finding and connecting to first available USB printer
                val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
                val rawDevices = usbManager?.deviceList?.values?.toList() ?: emptyList()
                val printerDev = rawDevices.firstOrNull { isKnownPrinterVendor(it.vendorId) || it.interfaceCount > 0 }

                if (printerDev != null && usbManager != null && usbManager.hasPermission(printerDev)) {
                    val conn = usbManager.openDevice(printerDev)
                    if (conn != null) {
                        for (i in 0 until printerDev.interfaceCount) {
                            val iface = printerDev.getInterface(i)
                            for (j in 0 until iface.endpointCount) {
                                val ep = iface.getEndpoint(j)
                                if (ep.direction == UsbConstants.USB_DIR_OUT) {
                                    conn.claimInterface(iface, true)
                                    val sent = conn.bulkTransfer(ep, bytes, bytes.size, 4000)
                                    conn.releaseInterface(iface)
                                    conn.close()
                                    if (sent >= 0) return@withContext true
                                }
                            }
                        }
                        conn.close()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending USB raw bytes", e)
            }
            false
        }
    }

    private fun printViaSystemPrintService(context: Context, title: String, paperWidthMm: Int) {
        try {
            val previewData = InvoicePreviewData(
                invoiceNumber = "TEST-POS-001",
                dateMillis = System.currentTimeMillis(),
                cashierName = "مدير النظام",
                customerName = "إيصال تجريبي",
                items = listOf(
                    com.example.ui.components.InvoicePreviewItem(
                        productId = 1,
                        name = "صنف تجريبي رقم 1",
                        quantity = 2,
                        unitPrice = 50.0,
                        totalPrice = 100.0
                    ),
                    com.example.ui.components.InvoicePreviewItem(
                        productId = 2,
                        name = "صنف تجريبي رقم 2",
                        quantity = 1,
                        unitPrice = 150.0,
                        totalPrice = 150.0
                    )
                ),
                subtotal = 250.0,
                discountPercent = 0.0,
                discountAmount = 0.0,
                finalTotal = 250.0,
                isDebt = false,
                storeName = title
            )
            val pdfFile = PdfInvoiceHelper.createPdfFromInvoiceData(context, previewData)
            if (pdfFile != null) {
                PdfInvoiceHelper.printPdfViaSystem(context, pdfFile, "إيصال تجريبي - $title")
            }
        } catch (e: Exception) {
            Log.e(TAG, "PrintManager fallback error", e)
        }
    }
}
