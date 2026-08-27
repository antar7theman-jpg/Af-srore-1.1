package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.models.Product
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodels.CartItem
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

@Composable
fun CameraBarcodeScannerModal(
    products: List<Product>,
    cartItemCount: Int,
    cartTotalAmount: Double,
    cartItems: List<CartItem> = emptyList(),
    onProductScanned: (Product) -> Unit,
    onCartonScanned: ((Product) -> Unit)? = null,
    onUpdateCartQuantity: ((Int, Int) -> Unit)? = null,
    onRemoveFromCart: ((Int) -> Unit)? = null,
    onOpenCartSheet: (() -> Unit)? = null,
    onPreviewInvoice: (() -> Unit)? = null,
    onManualCodeSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isSoundEnabled by remember { mutableStateOf(true) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("camera_barcode_scanner_modal")
        ) {
            if (hasCameraPermission) {
                CameraScannerContent(
                    products = products,
                    cartItemCount = cartItemCount,
                    cartTotalAmount = cartTotalAmount,
                    cartItems = cartItems,
                    isSoundEnabled = isSoundEnabled,
                    onToggleSound = { isSoundEnabled = !isSoundEnabled },
                    onProductScanned = { product ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isSoundEnabled) playBeepSound()
                        onProductScanned(product)
                    },
                    onCartonScanned = if (onCartonScanned != null) {
                        { product ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isSoundEnabled) playBeepSound()
                            onCartonScanned(product)
                        }
                    } else null,
                    onUpdateCartQuantity = onUpdateCartQuantity,
                    onRemoveFromCart = onRemoveFromCart,
                    onOpenCartSheet = onOpenCartSheet,
                    onPreviewInvoice = onPreviewInvoice,
                    onManualCodeSubmit = onManualCodeSubmit,
                    onDismiss = onDismiss
                )
            } else {
                CameraPermissionFallback(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun CameraScannerContent(
    products: List<Product>,
    cartItemCount: Int,
    cartTotalAmount: Double,
    cartItems: List<CartItem>,
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onProductScanned: (Product) -> Unit,
    onCartonScanned: ((Product) -> Unit)? = null,
    onUpdateCartQuantity: ((Int, Int) -> Unit)? = null,
    onRemoveFromCart: ((Int) -> Unit)? = null,
    onOpenCartSheet: (() -> Unit)? = null,
    onPreviewInvoice: (() -> Unit)? = null,
    onManualCodeSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isTorchOn by remember { mutableStateOf(false) }
    var cameraLensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var cameraControlInstance by remember { mutableStateOf<CameraControl?>(null) }
    var autoAddMode by remember { mutableStateOf(true) }

    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScannedProduct by remember { mutableStateOf<Product?>(null) }
    var isLastScannedCarton by remember { mutableStateOf(false) }
    var lastScanStatusMessage by remember { mutableStateOf<String?>(null) }
    var lastScanTimestamp by remember { mutableLongStateOf(0L) }
    var scanSuccessGlow by remember { mutableStateOf(false) }

    var isLiveCartExpanded by remember { mutableStateOf(false) }
    var manualCodeInput by remember { mutableStateOf("") }
    var isManualInputExpanded by remember { mutableStateOf(false) }

    // Laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    // Reset scan glow effect after delay
    LaunchedEffect(scanSuccessGlow) {
        if (scanSuccessGlow) {
            delay(500)
            scanSuccessGlow = false
        }
    }

    // Camera preview and ML Kit Analyzer
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build()
                    val barcodeScanner = BarcodeScanning.getClient(options)

                    @OptIn(ExperimentalGetImage::class)
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                val rawValue = barcode.rawValue?.trim()
                                                if (!rawValue.isNullOrBlank()) {
                                                    val now = System.currentTimeMillis()
                                                    // Debounce: 1000ms
                                                    if (rawValue != lastScannedCode || now - lastScanTimestamp > 1000) {
                                                        lastScannedCode = rawValue
                                                        lastScanTimestamp = now

                                                        // Check if matches carton barcode first, then unit barcode
                                                        val matchedCarton = products.find { it.cartonBarcode != null && it.cartonBarcode == rawValue }
                                                        val matchingProduct = matchedCarton ?: products.find { it.barcode != null && it.barcode == rawValue }
                                                        val isCarton = matchedCarton != null && matchingProduct != null && matchingProduct.unitsPerCarton > 1

                                                        lastScannedProduct = matchingProduct
                                                        isLastScannedCarton = isCarton
                                                        scanSuccessGlow = true

                                                        if (matchingProduct != null) {
                                                            if (isCarton) {
                                                                val cartonUnits = matchingProduct.unitsPerCarton
                                                                if (matchingProduct.stock >= cartonUnits) {
                                                                    lastScanStatusMessage = "📦 تم مسح عبوة: ${matchingProduct.name} ($cartonUnits ق)"
                                                                    if (autoAddMode) {
                                                                        if (onCartonScanned != null) {
                                                                            onCartonScanned(matchingProduct)
                                                                        } else {
                                                                            onProductScanned(matchingProduct)
                                                                        }
                                                                    }
                                                                } else if (matchingProduct.stock > 0) {
                                                                    lastScanStatusMessage = "⚠️ المتبقي (${matchingProduct.stock} ق) أقل من عبوة كاملة!"
                                                                } else {
                                                                    lastScanStatusMessage = "⚠️ عبوة ${matchingProduct.name} نفدت من المخزون!"
                                                                }
                                                            } else {
                                                                if (matchingProduct.stock > 0) {
                                                                    lastScanStatusMessage = "🏷️ تم مسح قطعة: ${matchingProduct.name}"
                                                                    if (autoAddMode) {
                                                                        onProductScanned(matchingProduct)
                                                                    }
                                                                } else {
                                                                    lastScanStatusMessage = "⚠️ المنتج ${matchingProduct.name} نفد من المخزون!"
                                                                }
                                                            }
                                                        } else {
                                                            lastScanStatusMessage = "❌ لا يوجد منتج مسجل بالباركود: $rawValue"
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(cameraLensFacing)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        cameraControlInstance = camera.cameraControl
                    } catch (e: Exception) {
                        Log.e("CameraScanner", "Use case binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            update = {
                cameraControlInstance?.enableTorch(isTorchOn)
            }
        )

        // Overlay with scanning reticle and controls
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .testTag("scanner_close_button")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White
                    )
                }

                // Live Cart Header Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.5.dp, if (cartItemCount > 0) SuccessGreen else Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable {
                        if (cartItems.isNotEmpty()) isLiveCartExpanded = !isLiveCartExpanded
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = if (cartItemCount > 0) SuccessGreen else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "$cartItemCount أصناف",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "|",
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f ج.م", cartTotalAmount),
                            color = SuccessGreen,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Controls (Sound & Torch)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .background(
                                if (isSoundEnabled) Color.Black.copy(alpha = 0.6f) else DangerRed.copy(alpha = 0.6f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "صوت التنبيه",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            isTorchOn = !isTorchOn
                            cameraControlInstance?.enableTorch(isTorchOn)
                        },
                        modifier = Modifier
                            .background(
                                if (isTorchOn) WarningOrange.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.6f),
                                CircleShape
                            )
                            .testTag("scanner_torch_button")
                    ) {
                        Icon(
                            if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "الفلاش",
                            tint = if (isTorchOn) Color.Black else Color.White
                        )
                    }
                }
            }

            // Scanning Viewfinder & Laser Center Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                // Viewfinder frame
                val reticleBorderColor = if (scanSuccessGlow) SuccessGreen else Color(0xFF00E676)
                Box(
                    modifier = Modifier
                        .size(260.dp, 220.dp)
                        .border(
                            width = if (scanSuccessGlow) 3.dp else 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    reticleBorderColor,
                                    Color.White,
                                    reticleBorderColor
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Animated Laser Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .offset(y = (220 * laserPosition).dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color(0xFF00E676),
                                            Color.White,
                                            Color(0xFF00E676),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }

                // Instruction tag above / below
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Text(
                            text = "وجه الكاميرا نحو باركود المنتج (قطعة أو عبوة)",
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Bottom Floating Action & Feedback Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status / Last Scanned Toast Card
                AnimatedVisibility(
                    visible = lastScanStatusMessage != null,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (lastScannedProduct != null && lastScannedProduct!!.stock > 0)
                                Color(0xFF1E3A2F)
                            else
                                Color(0xFF3E2723)
                        ),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val prodImgFile = remember(lastScannedProduct?.imagePath) {
                                if (!lastScannedProduct?.imagePath.isNullOrBlank()) File(lastScannedProduct!!.imagePath!!) else null
                            }
                            if (prodImgFile != null && prodImgFile.exists()) {
                                AsyncImage(
                                    model = prodImgFile,
                                    contentDescription = lastScannedProduct?.name,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lastScanStatusMessage ?: "",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (lastScannedProduct != null) {
                                    Text(
                                        text = "السعر: ${lastScannedProduct!!.price} ج.م | المخزون: ${lastScannedProduct!!.stock} قطعة",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Quick Add buttons for manual review or extra taps
                            if (lastScannedProduct != null && lastScannedProduct!!.stock > 0) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (isLastScannedCarton && lastScannedProduct!!.unitsPerCarton > 1) {
                                        Button(
                                            onClick = {
                                                if (onCartonScanned != null) {
                                                    onCartonScanned(lastScannedProduct!!)
                                                } else {
                                                    onProductScanned(lastScannedProduct!!)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text("+1 📦", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            onProductScanned(lastScannedProduct!!)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("+1 ق", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Mini-Cart Drawer (Expandable)
                if (cartItems.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isLiveCartExpanded = !isLiveCartExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "المنتجات الممسوحة بالسلة (${cartItems.size} أصناف)",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.2f ج.م", cartTotalAmount),
                                        color = SuccessGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Icon(
                                        if (isLiveCartExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Expanded Items List with Quick Stepper
                            AnimatedVisibility(visible = isLiveCartExpanded) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 160.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(cartItems, key = { it.product.id }) { item ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color.White.copy(alpha = 0.08f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = item.product.name,
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${item.quantity} ق × ${item.product.price} = ${String.format(Locale.getDefault(), "%.2f", item.subtotal)} ج.م",
                                                            color = Color.White.copy(alpha = 0.7f),
                                                            fontSize = 11.sp
                                                        )
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        IconButton(
                                                            onClick = {
                                                                if (onUpdateCartQuantity != null) {
                                                                    onUpdateCartQuantity(item.product.id, item.quantity - 1)
                                                                }
                                                            },
                                                            modifier = Modifier.size(26.dp)
                                                        ) {
                                                            Icon(Icons.Default.Remove, contentDescription = "-", tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }
                                                        Text(
                                                            text = "${item.quantity}",
                                                            color = SuccessGreen,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp)
                                                        )
                                                        IconButton(
                                                            onClick = {
                                                                if (onUpdateCartQuantity != null) {
                                                                    onUpdateCartQuantity(item.product.id, item.quantity + 1)
                                                                }
                                                            },
                                                            modifier = Modifier.size(26.dp)
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = "+", tint = Color.White, modifier = Modifier.size(14.dp))
                                                        }
                                                        if (onRemoveFromCart != null) {
                                                            IconButton(
                                                                onClick = { onRemoveFromCart(item.product.id) },
                                                                modifier = Modifier.size(26.dp)
                                                            ) {
                                                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = DangerRed, modifier = Modifier.size(14.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Action buttons row: Preview Invoice & Open Checkout Sheet
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (onPreviewInvoice != null) {
                                            OutlinedButton(
                                                onClick = {
                                                    onDismiss()
                                                    onPreviewInvoice()
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                            ) {
                                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("معاينة الفاتورة", fontSize = 12.sp)
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                onDismiss()
                                                if (onOpenCartSheet != null) {
                                                    onOpenCartSheet()
                                                }
                                            },
                                            modifier = Modifier.weight(1.2f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إتمام الدفع (${String.format(Locale.getDefault(), "%.2f", cartTotalAmount)})", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Mode Selector Bar (Auto-Add vs Manual Review)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                if (autoAddMode) Icons.Default.FlashAuto else Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = if (autoAddMode) SuccessGreen else WarningOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (autoAddMode) "إضافة فورية مستمرة بالسلة" else "فحص وتأكيد يدوي",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = autoAddMode,
                                onClick = { autoAddMode = true },
                                label = { Text("تلقائي ⚡", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SuccessGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                            FilterChip(
                                selected = !autoAddMode,
                                onClick = { autoAddMode = false },
                                label = { Text("يدوي 👆", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WarningOrange,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }

                // Manual Barcode Input Fallback Toggle
                AnimatedVisibility(visible = isManualInputExpanded) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.95f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            OutlinedTextField(
                                value = manualCodeInput,
                                onValueChange = { manualCodeInput = it },
                                label = { Text("أدخل رمز الباركود بالأرقام", color = Color.White.copy(alpha = 0.7f)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (manualCodeInput.isNotBlank()) {
                                        onManualCodeSubmit(manualCodeInput.trim())
                                        manualCodeInput = ""
                                    }
                                }),
                                trailingIcon = {
                                    if (manualCodeInput.isNotBlank()) {
                                        IconButton(onClick = {
                                            onManualCodeSubmit(manualCodeInput.trim())
                                            manualCodeInput = ""
                                        }) {
                                            Icon(Icons.Default.Send, contentDescription = "بحث وإضافة", tint = SuccessGreen)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_barcode_input_scanner")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "أكواد تجريبية سريعة:",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("6221001001", "6221001003", "6221001005").forEach { sample ->
                                    SuggestionChip(
                                        onClick = {
                                            onManualCodeSubmit(sample)
                                        },
                                        label = { Text(sample, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { isManualInputExpanded = !isManualInputExpanded },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        if (isManualInputExpanded) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isManualInputExpanded) "إخفاء لوحة الإدخال اليدوي" else "إدخال كود يدوي / باركود تالف",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionFallback(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = DangerRed.copy(alpha = 0.15f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "إذن الكاميرا مطلوب",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "يتطلب تطبيق AF store إذن استخدام الكاميرا لمسح باركود المنتجات وتسهيل عمليات البيع السريعة بدقة واحترافية.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("منح إذن الكاميرا")
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onDismiss) {
            Text("إلغاء والعودة", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

private fun playBeepSound() {
    try {
        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    } catch (_: Exception) {}
}

/**
 * Dedicated lightweight camera scanner modal to scan a single barcode (for unit or carton field in inventory)
 */
@Composable
fun SingleBarcodeCaptureModal(
    title: String = "مسح رمز الباركود",
    subtitle: String = "وجّه الكاميرا نحو الباركود لحفظه تلقائياً",
    onBarcodeCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("single_barcode_capture_modal")
        ) {
            if (hasCameraPermission) {
                SingleScannerCaptureContent(
                    title = title,
                    subtitle = subtitle,
                    onBarcodeCaptured = { code ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        playBeepSound()
                        onBarcodeCaptured(code)
                    },
                    onDismiss = onDismiss
                )
            } else {
                CameraPermissionFallback(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun SingleScannerCaptureContent(
    title: String,
    subtitle: String,
    onBarcodeCaptured: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControlInstance by remember { mutableStateOf<CameraControl?>(null) }
    var capturedCode by remember { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "single_laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val barcodeScanner = BarcodeScanning.getClient(
                        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
                    )
                    @OptIn(ExperimentalGetImage::class)
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null && capturedCode == null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                val rawValue = barcode.rawValue?.trim()
                                                if (!rawValue.isNullOrBlank() && capturedCode == null) {
                                                    capturedCode = rawValue
                                                    onBarcodeCaptured(rawValue)
                                                    break
                                                }
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        cameraControlInstance = camera.cameraControl
                    } catch (e: Exception) {
                        Log.e("SingleScanner", "Failed binding", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            update = {
                cameraControlInstance?.enableTorch(isTorchOn)
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = { isTorchOn = !isTorchOn },
                    modifier = Modifier.background(
                        if (isTorchOn) WarningOrange.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
                ) {
                    Icon(
                        if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "الفلاش",
                        tint = Color.White
                    )
                }
            }

            // Central Reticle
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.CenterHorizontally)
                    .border(2.dp, SuccessGreen, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .offset(y = (240 * laserPosition).dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0xFF00E676), Color.White, Color(0xFF00E676), Color.Transparent)
                            )
                        )
                )
            }

            // Subtitle instructions
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = subtitle,
                    color = Color.White,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}
