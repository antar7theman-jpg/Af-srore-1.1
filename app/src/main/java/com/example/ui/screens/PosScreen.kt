package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.models.Customer
import com.example.data.models.InvoiceStyle
import com.example.data.models.Product
import com.example.data.models.Sale
import com.example.data.models.User
import com.example.ui.components.BluetoothPrinterModal
import com.example.ui.components.CameraBarcodeScannerModal
import com.example.ui.components.InvoicePreviewData
import com.example.ui.components.InvoicePreviewDialog
import com.example.ui.components.InvoicePreviewItem
import com.example.ui.components.PosProductsGridSkeleton
import com.example.ui.components.SaleConfirmationModal
import com.example.ui.theme.*
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.utils.AppLanguage
import com.example.utils.AppStrings
import com.example.utils.PdfInvoiceHelper
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: AntarSalesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isLoadingProducts by viewModel.isLoadingProducts.collectAsState()
    val products by viewModel.filteredProducts.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val selectedCustomer by viewModel.selectedPosCustomer.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val todayTotal by viewModel.todayTotal.collectAsState()
    val todaySales by viewModel.todaySales.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentInvoiceStyle by viewModel.invoiceStyle.collectAsState()

    var showCartSheet by remember { mutableStateOf(false) }
    var showBarcodeScannerDialog by remember { mutableStateOf(false) }
    var showQuickSellDialog by remember { mutableStateOf<Product?>(null) }
    var showCustomerPickerModal by remember { mutableStateOf(false) }
    var showAddCustomerQuickDialog by remember { mutableStateOf(false) }
    var showPosPrinterModal by remember { mutableStateOf(false) }
    var isCartDebtSale by remember { mutableStateOf(false) }
    var completedReceiptSales by remember { mutableStateOf<List<Sale>?>(null) }
    var previewInvoiceData by remember { mutableStateOf<InvoicePreviewData?>(null) }
    var isPrinterSimulating by remember { mutableStateOf(false) }

    // Quick direct sale state
    var directSellQty by remember { mutableIntStateOf(1) }
    var directSellDiscount by remember { mutableDoubleStateOf(0.0) }

    // Cart discount state
    var cartDiscountPercent by remember { mutableDoubleStateOf(0.0) }

    val totalCartCount = cartItems.sumOf { it.quantity }
    val cartSubtotal = cartItems.sumOf { it.subtotal }
    val cartDiscountFactor = (100.0 - cartDiscountPercent.coerceIn(0.0, 100.0)) / 100.0
    val cartFinalTotal = cartSubtotal * cartDiscountFactor

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

        if (isTablet) {
            // ===== Tablet / Expanded POS Layout (Catalog on Left, Live Register Pane on Right) =====
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Search, Barcode, Customer, Products Grid
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Search Bar and Barcode Scanner Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text(AppStrings.searchPlaceholder(currentLanguage)) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            },
                            trailingIcon = {
                                AnimatedVisibility(
                                    visible = searchQuery.isNotEmpty(),
                                    enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.8f),
                                    exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f)
                                ) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pos_search_input_tablet")
                        )

                        FilledTonalIconButton(
                            onClick = { showBarcodeScannerDialog = true },
                            modifier = Modifier
                                .size(54.dp)
                                .testTag("barcode_scanner_button_tablet"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "ماسح الباركود",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Animated Products Grid Container
                    AnimatedContent(
                        targetState = when {
                            isLoadingProducts -> "LOADING"
                            products.isEmpty() -> "EMPTY"
                            else -> "CONTENT"
                        },
                        transitionSpec = {
                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        label = "products_grid_tablet_anim"
                    ) { state ->
                        when (state) {
                            "LOADING" -> {
                                PosProductsGridSkeleton()
                            }
                            "EMPTY" -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (searchQuery.isNotBlank()) "لا توجد منتجات مطابقة للبحث" else "لا توجد منتجات مسجلة بعد",
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            else -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 145.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("pos_products_grid_tablet"),
                                    contentPadding = PaddingValues(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(products, key = { it.id }) { product ->
                                        ProductPosCard(
                                            product = product,
                                            onAddToCart = { viewModel.addToCart(product, 1) },
                                            onAddCartonToCart = if (product.unitsPerCarton > 1) {
                                                { viewModel.addToCart(product, product.unitsPerCarton) }
                                            } else null,
                                            onQuickSell = {
                                                showQuickSellDialog = product
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Column: Dedicated POS Register Sidebar Card for Tablet
                Card(
                    modifier = Modifier
                        .width(380.dp)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Register Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "سجل المبيعات (${cartItems.size})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (cartItems.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.clearCart() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("تفريغ", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Customer Strip in Register Pane
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showCustomerPickerModal = true },
                            color = if (selectedCustomer != null) {
                                if (selectedCustomer!!.balanceDebt > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(
                                1.dp,
                                if (selectedCustomer != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (selectedCustomer != null) Icons.Default.Person else Icons.Default.PersonOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (selectedCustomer != null) selectedCustomer!!.name else "زبون نقدي عام",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (selectedCustomer != null && selectedCustomer!!.balanceDebt > 0) {
                                            Text(
                                                text = String.format(Locale.getDefault(), "دين: %.2f ج.م", selectedCustomer!!.balanceDebt),
                                                fontSize = 10.sp,
                                                color = DangerRed,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                TextButton(
                                    onClick = { showCustomerPickerModal = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = if (selectedCustomer == null) "تحديد" else "تغيير",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Scrollable Cart Items List
                        if (cartItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.AddShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Text(
                                        text = "السلة فارغة، اضغط على المنتجات لإضافتها",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(cartItems, key = { it.product.id }) { item ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.product.name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = String.format(Locale.getDefault(), "%.2f × %d = %.2f ج.م", item.product.price, item.quantity, item.subtotal),
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { viewModel.updateCartItemQuantity(item.product.id, item.quantity - 1) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = "إنقاص", modifier = Modifier.size(14.dp))
                                                }

                                                Text(
                                                    text = "${item.quantity}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )

                                                IconButton(
                                                    onClick = { viewModel.updateCartItemQuantity(item.product.id, item.quantity + 1) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(14.dp))
                                                }

                                                IconButton(
                                                    onClick = { viewModel.removeFromCart(item.product.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Discount Quick Selector Chips in Tablet
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الخصم:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            listOf(0.0, 5.0, 10.0, 15.0).forEach { disc ->
                                FilterChip(
                                    selected = cartDiscountPercent == disc,
                                    onClick = { cartDiscountPercent = disc },
                                    label = { Text("${disc.toInt()}%", fontSize = 10.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Payment mode selection (Cash vs Debt)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = !isCartDebtSale,
                                onClick = { isCartDebtSale = false },
                                label = { Text("💵 نقدي", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            FilterChip(
                                selected = isCartDebtSale,
                                onClick = {
                                    if (selectedCustomer == null) {
                                        showCustomerPickerModal = true
                                    }
                                    isCartDebtSale = true
                                },
                                label = { Text("⚠️ دين آجل", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Totals Summary Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (cartDiscountPercent > 0) {
                                    val discountVal = cartSubtotal * (cartDiscountPercent / 100.0)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("خصم (${cartDiscountPercent.toInt()}%):", fontSize = 11.sp, color = DangerRed)
                                        Text(String.format(Locale.getDefault(), "-%.2f ج.م", discountVal), fontSize = 11.sp, color = DangerRed)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("المجموع النهائي:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.2f ج.م", cartFinalTotal),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Checkout Action Buttons (Preview & Quick Checkout)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (cartItems.isEmpty()) return@Button
                                    if (isCartDebtSale && selectedCustomer == null) {
                                        showCustomerPickerModal = true
                                        return@Button
                                    }
                                    val previewItems = cartItems.map { item ->
                                        val itemDiscFactor = (100.0 - cartDiscountPercent.coerceIn(0.0, 100.0)) / 100.0
                                        InvoicePreviewItem(
                                            productId = item.product.id,
                                            name = item.product.name,
                                            quantity = item.quantity,
                                            unitPrice = item.product.price,
                                            totalPrice = item.subtotal * itemDiscFactor,
                                            unitsPerCarton = item.product.unitsPerCarton,
                                            barcode = item.product.barcode,
                                            cartonBarcode = item.product.cartonBarcode
                                        )
                                    }
                                    val invoiceNum = (System.currentTimeMillis() % 1000000).toString()
                                    val discAmt = cartSubtotal * (cartDiscountPercent / 100.0)
                                    previewInvoiceData = InvoicePreviewData(
                                        invoiceNumber = invoiceNum,
                                        dateMillis = System.currentTimeMillis(),
                                        cashierName = currentUser?.username ?: "المدير",
                                        customerName = selectedCustomer?.name,
                                        isDebt = isCartDebtSale,
                                        paymentMethod = if (isCartDebtSale) "DEBT" else "CASH",
                                        items = previewItems,
                                        subtotal = cartSubtotal,
                                        discountPercent = cartDiscountPercent,
                                        discountAmount = discAmt,
                                        finalTotal = cartFinalTotal,
                                        isDraft = true
                                    )
                                },
                                enabled = cartItems.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("معاينة الفاتورة 👁️", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = {
                                    if (cartItems.isEmpty()) return@FilledTonalButton
                                    if (isCartDebtSale && selectedCustomer == null) {
                                        showCustomerPickerModal = true
                                        return@FilledTonalButton
                                    }
                                    coroutineScope.launch {
                                        val results = viewModel.checkoutCart(
                                            discountPercent = cartDiscountPercent,
                                            customer = selectedCustomer,
                                            isDebt = isCartDebtSale,
                                            paymentMethod = if (isCartDebtSale) "DEBT" else "CASH"
                                        )
                                        if (results != null) {
                                            completedReceiptSales = results
                                            cartDiscountPercent = 0.0
                                            isCartDebtSale = false
                                        }
                                    }
                                },
                                enabled = cartItems.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCartDebtSale) "إتمام بيع آجل (دين) ⚠️" else "إتمام البيع المباشر ✅",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // ===== Phone Layout (Single Column with Floating Cart Bar & Bottom Sheet) =====
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar and Barcode Scanner Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text(AppStrings.searchPlaceholder(currentLanguage)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = searchQuery.isNotEmpty(),
                            enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.8f),
                            exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f)
                        ) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pos_search_input")
                )

                FilledTonalIconButton(
                    onClick = { showBarcodeScannerDialog = true },
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("barcode_scanner_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "ماسح الباركود",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Animated search active filter badge
            AnimatedVisibility(
                visible = searchQuery.isNotBlank(),
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = if (currentLanguage == AppLanguage.FRENCH) "Résultats pour \"$searchQuery\" (${products.size})" else if (currentLanguage == AppLanguage.ENGLISH) "Results for \"$searchQuery\" (${products.size})" else "نتائج البحث عن \"$searchQuery\" (${products.size} صنف)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        TextButton(
                            onClick = { viewModel.updateSearchQuery("") },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.FRENCH) "Effacer ✕" else if (currentLanguage == AppLanguage.ENGLISH) "Clear ✕" else "إلغاء الفلترة ✕",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Animated background color for customer strip
            val customerStripBgColor by animateColorAsState(
                targetValue = if (selectedCustomer != null) {
                    if (selectedCustomer!!.balanceDebt > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "customer_strip_bg"
            )
            val customerStripBorderColor by animateColorAsState(
                targetValue = if (selectedCustomer != null) {
                    if (selectedCustomer!!.balanceDebt > 0) DangerRed.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                } else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "customer_strip_border"
            )

            // POS Customer Selector Strip with subtle animations
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showCustomerPickerModal = true }
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                color = customerStripBgColor,
                border = BorderStroke(1.dp, customerStripBorderColor),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (selectedCustomer != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AnimatedContent(
                                    targetState = selectedCustomer != null,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                                    },
                                    label = "customer_icon_anim"
                                ) { hasCustomer ->
                                    Icon(
                                        imageVector = if (hasCustomer) Icons.Default.Person else Icons.Default.PersonOutline,
                                        contentDescription = null,
                                        tint = if (hasCustomer) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        AnimatedContent(
                            targetState = selectedCustomer,
                            transitionSpec = {
                                (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + slideInVertically { it / 3 })
                                    .togetherWith(fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + slideOutVertically { -it / 3 })
                            },
                            label = "customer_info_anim"
                        ) { targetCust ->
                            Column {
                                if (targetCust != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = targetCust.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (targetCust.phone.isNotBlank()) {
                                            Text(
                                                text = "(${targetCust.phone})",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (targetCust.balanceDebt > 0) {
                                        Text(
                                            text = String.format(Locale.getDefault(), "⚠️ عليه دين سابق: %.2f ج.م", targetCust.balanceDebt),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DangerRed
                                        )
                                    } else {
                                        Text(
                                            text = "✅ حسابه خالص (لا يوجد ديون)",
                                            fontSize = 11.sp,
                                            color = SuccessGreen
                                        )
                                    }
                                } else {
                                    Text(
                                        text = AppStrings.generalCustomer(currentLanguage),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (currentLanguage == AppLanguage.FRENCH) "Cliquez pour sélectionner un client ou activer le crédit" else if (currentLanguage == AppLanguage.ENGLISH) "Tap to select customer or enable credit sale" else "اضغط لتحديد زبون مسجل أو تفعيل البيع الآجل",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AnimatedVisibility(
                            visible = selectedCustomer != null,
                            enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.8f),
                            exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.selectPosCustomer(null)
                                    isCartDebtSale = false
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "إلغاء تحديد الزبون",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        FilledTonalButton(
                            onClick = { showCustomerPickerModal = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("pos_select_customer_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (selectedCustomer == null) AppStrings.selectCustomerQuick(currentLanguage) else AppStrings.edit(currentLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = { showAddCustomerQuickDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("pos_create_customer_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = AppStrings.newCustomerQuick(currentLanguage),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Products Grid with smooth skeleton loading and empty/content animated transitions
            AnimatedContent(
                targetState = when {
                    isLoadingProducts -> "LOADING"
                    products.isEmpty() -> "EMPTY"
                    else -> "CONTENT"
                },
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "products_grid_anim"
            ) { state ->
                when (state) {
                    "LOADING" -> {
                        PosProductsGridSkeleton()
                    }
                    "EMPTY" -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "لا توجد منتجات مطابقة للبحث" else "لا توجد منتجات مسجلة بعد",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("pos_products_grid"),
                            contentPadding = PaddingValues(bottom = 88.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(products, key = { it.id }) { product ->
                                ProductPosCard(
                                    product = product,
                                    onAddToCart = { viewModel.addToCart(product, 1) },
                                    onAddCartonToCart = if (product.unitsPerCarton > 1) {
                                        { viewModel.addToCart(product, product.unitsPerCarton) }
                                    } else null,
                                    onQuickSell = {
                                        showQuickSellDialog = product
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Cart Bar / Button with animated entrance, ticker badge, and spring transitions
        AnimatedVisibility(
            visible = totalCartCount > 0,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clickable { showCartSheet = true }
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                    .testTag("floating_cart_bar"),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.onPrimary,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            AnimatedContent(
                                targetState = totalCartCount,
                                transitionSpec = {
                                    (slideInVertically { -it } + fadeIn())
                                        .togetherWith(slideOutVertically { it } + fadeOut())
                                },
                                label = "cart_count_badge_anim"
                            ) { count ->
                                Text(
                                    text = "$count",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (currentLanguage == AppLanguage.FRENCH) "Panier en cours" else if (currentLanguage == AppLanguage.ENGLISH) "Active Cart" else "سلة المبيعات الحالية",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedContent(
                            targetState = cartFinalTotal,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                            },
                            label = "cart_total_anim"
                        ) { total ->
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f ج.م", total),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "عرض السلة",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
}

    // ===== Quick Sell Single Product Modal =====
    showQuickSellDialog?.let { prod ->
        QuickSellModal(
            product = prod,
            currentUser = currentUser,
            selectedCustomer = selectedCustomer,
            currentLanguage = currentLanguage,
            onSelectCustomerClick = { showCustomerPickerModal = true },
            onAddNewCustomerClick = { showAddCustomerQuickDialog = true },
            onDismiss = { showQuickSellDialog = null },
            onPreviewInvoice = { qty, discountPercent, isDebt ->
                val sub = prod.price * qty
                val discAmt = sub * (discountPercent / 100.0)
                val fin = sub - discAmt
                val previewItem = InvoicePreviewItem(
                    productId = prod.id,
                    name = prod.name,
                    quantity = qty,
                    unitPrice = prod.price,
                    totalPrice = fin,
                    unitsPerCarton = prod.unitsPerCarton,
                    barcode = prod.barcode,
                    cartonBarcode = prod.cartonBarcode
                )
                val invoiceNum = (System.currentTimeMillis() % 1000000).toString()
                previewInvoiceData = InvoicePreviewData(
                    invoiceNumber = invoiceNum,
                    dateMillis = System.currentTimeMillis(),
                    cashierName = currentUser?.username ?: "المدير",
                    customerName = selectedCustomer?.name,
                    isDebt = isDebt,
                    paymentMethod = if (isDebt) "DEBT" else "CASH",
                    items = listOf(previewItem),
                    subtotal = sub,
                    discountPercent = discountPercent,
                    discountAmount = discAmt,
                    finalTotal = fin,
                    isDraft = true
                )
                showQuickSellDialog = null
            },
            onConfirmSale = { qty, discountPercent, isDebt ->
                val singleSale = Sale(
                    productId = prod.id,
                    productName = if (discountPercent > 0) "${prod.name} (خصم ${discountPercent.toInt()}%)" else prod.name,
                    quantitySold = qty,
                    totalPrice = (prod.price * qty) * ((100.0 - discountPercent) / 100.0),
                    saleDate = System.currentTimeMillis(),
                    customerId = selectedCustomer?.id,
                    customerName = selectedCustomer?.name,
                    isDebt = isDebt,
                    paymentMethod = if (isDebt) "DEBT" else "CASH"
                )
                viewModel.sellProductDirect(
                    productId = prod.id,
                    quantity = qty,
                    discountPercent = discountPercent,
                    customer = selectedCustomer,
                    isDebt = isDebt,
                    paymentMethod = if (isDebt) "DEBT" else "CASH"
                )
                completedReceiptSales = listOf(singleSale)
                showQuickSellDialog = null
            },
            onContinueSelling = { qty, discountPercent ->
                // Add to active cart with specified quantity and discount, keeping the POS session open for more items
                viewModel.addToCart(prod, quantity = qty, discountPercent = discountPercent)
                showQuickSellDialog = null
            }
        )
    }

    // ===== Cart Bottom Sheet =====
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سلة المشتريات (${cartItems.size} صنف)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.clearCart() }) {
                        Text("تفريغ السلة", color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Quick Scan Barcode Button inside Cart Sheet
                OutlinedButton(
                    onClick = {
                        showBarcodeScannerDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مسح باركود إضافي بالكاميرا (سريع) 📷", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // POS Customer Selector Strip in Cart
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showCustomerPickerModal = true },
                    color = if (selectedCustomer != null) {
                        if (selectedCustomer!!.balanceDebt > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (selectedCustomer != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (selectedCustomer != null) Icons.Default.Person else Icons.Default.PersonOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = if (selectedCustomer != null) "الزبون: ${selectedCustomer!!.name}" else "الزبون: زبون نقدي عام",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (selectedCustomer != null && selectedCustomer!!.balanceDebt > 0) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "دين سابق: %.2f ج.م", selectedCustomer!!.balanceDebt),
                                        fontSize = 10.sp,
                                        color = DangerRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = { showCustomerPickerModal = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(if (selectedCustomer == null) AppStrings.selectCustomerQuick(currentLanguage) else AppStrings.edit(currentLanguage), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showAddCustomerQuickDialog = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(AppStrings.newCustomerQuick(currentLanguage), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartItems, key = { it.product.id }) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val cartImgFile = remember(item.product.imagePath) {
                                    if (!item.product.imagePath.isNullOrBlank()) File(item.product.imagePath) else null
                                }
                                if (cartImgFile != null && cartImgFile.exists()) {
                                    AsyncImage(
                                        model = cartImgFile,
                                        contentDescription = item.product.name,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else if (item.product.image != null && item.product.image.isNotEmpty()) {
                                    AsyncImage(
                                        model = item.product.image,
                                        contentDescription = item.product.name,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.product.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.2f × %d = %.2f ج.م", item.product.price, item.quantity, item.subtotal),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (item.product.unitsPerCarton > 1 && item.quantity >= item.product.unitsPerCarton) {
                                        Text(
                                            text = "📦 يعادل: ${item.quantity / item.product.unitsPerCarton} عبوة + ${item.quantity % item.product.unitsPerCarton} فردي",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.updateCartItemQuantity(item.product.id, item.quantity - 1) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "إنقاص قطعة", modifier = Modifier.size(16.dp))
                                        }

                                        Text(
                                            text = "${item.quantity} ق",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )

                                        IconButton(
                                            onClick = { viewModel.updateCartItemQuantity(item.product.id, item.quantity + 1) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "زيادة قطعة", modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = { viewModel.removeFromCart(item.product.id) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    if (item.product.unitsPerCarton > 1) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            FilledTonalButton(
                                                onClick = {
                                                    val newQty = (item.quantity - item.product.unitsPerCarton).coerceAtLeast(0)
                                                    if (newQty == 0) {
                                                        viewModel.removeFromCart(item.product.id)
                                                    } else {
                                                        viewModel.updateCartItemQuantity(item.product.id, newQty)
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                modifier = Modifier.height(26.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("-1 📦", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            FilledTonalButton(
                                                onClick = {
                                                    viewModel.updateCartItemQuantity(item.product.id, item.quantity + item.product.unitsPerCarton)
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                modifier = Modifier.height(26.dp),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("+1 📦", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Mode Selector (نقدي vs بيع آجل)
                Text("طريقة الدفع والحساب:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isCartDebtSale,
                        onClick = { isCartDebtSale = false },
                        label = { Text("💵 دفع نقدي (كاش)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isCartDebtSale,
                        onClick = {
                            if (selectedCustomer == null) {
                                showCustomerPickerModal = true
                            }
                            isCartDebtSale = true
                        },
                        label = { Text("⚠️ بيع آجل (دين)") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Discount Selector
                Text("تطبيق خصم عام على الفاتورة:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.0, 5.0, 10.0, 15.0, 20.0).forEach { disc ->
                        FilterChip(
                            selected = cartDiscountPercent == disc,
                            onClick = { cartDiscountPercent = disc },
                            label = { Text("${disc.toInt()}%") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Totals Breakdown
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المجموع الفرعي:", fontSize = 13.sp)
                        Text(String.format(Locale.getDefault(), "%.2f ج.م", cartSubtotal), fontSize = 13.sp)
                    }
                    if (cartDiscountPercent > 0) {
                        val discountVal = cartSubtotal * (cartDiscountPercent / 100.0)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("قيمة الخصم (${cartDiscountPercent.toInt()}%):", fontSize = 13.sp, color = DangerRed)
                            Text(String.format(Locale.getDefault(), "-%.2f ج.م", discountVal), fontSize = 13.sp, color = DangerRed)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المبلغ النهائي المستحق:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            String.format(Locale.getDefault(), "%.2f ج.م", cartFinalTotal),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cart Action Buttons (Preview Invoice & Direct Checkout)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Preview Invoice Button (Primary Feature)
                    Button(
                        onClick = {
                            if (isCartDebtSale && selectedCustomer == null) {
                                showCustomerPickerModal = true
                                return@Button
                            }
                            val previewItems = cartItems.map { item ->
                                val itemDiscFactor = (100.0 - cartDiscountPercent.coerceIn(0.0, 100.0)) / 100.0
                                InvoicePreviewItem(
                                    productId = item.product.id,
                                    name = item.product.name,
                                    quantity = item.quantity,
                                    unitPrice = item.product.price,
                                    totalPrice = item.subtotal * itemDiscFactor,
                                    unitsPerCarton = item.product.unitsPerCarton,
                                    barcode = item.product.barcode,
                                    cartonBarcode = item.product.cartonBarcode
                                )
                            }
                            val invoiceNum = (System.currentTimeMillis() % 1000000).toString()
                            val discAmt = cartSubtotal * (cartDiscountPercent / 100.0)
                            previewInvoiceData = InvoicePreviewData(
                                invoiceNumber = invoiceNum,
                                dateMillis = System.currentTimeMillis(),
                                cashierName = currentUser?.username ?: "المدير",
                                customerName = selectedCustomer?.name,
                                isDebt = isCartDebtSale,
                                paymentMethod = if (isCartDebtSale) "DEBT" else "CASH",
                                items = previewItems,
                                subtotal = cartSubtotal,
                                discountPercent = cartDiscountPercent,
                                discountAmount = discAmt,
                                finalTotal = cartFinalTotal,
                                isDraft = true
                            )
                            showCartSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("preview_invoice_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("معاينة الفاتورة قبل الطباعة 👁️", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    // Direct Checkout Button
                    FilledTonalButton(
                        onClick = {
                            if (isCartDebtSale && selectedCustomer == null) {
                                showCustomerPickerModal = true
                                return@FilledTonalButton
                            }
                            coroutineScope.launch {
                                val results = viewModel.checkoutCart(
                                    discountPercent = cartDiscountPercent,
                                    customer = selectedCustomer,
                                    isDebt = isCartDebtSale,
                                    paymentMethod = if (isCartDebtSale) "DEBT" else "CASH"
                                )
                                if (results != null) {
                                    completedReceiptSales = results
                                    showCartSheet = false
                                    cartDiscountPercent = 0.0
                                    isCartDebtSale = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("checkout_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCartDebtSale) "إتمام البيع الآجل (تسجيل دين) ⚠️" else "إتمام البيع المباشر ✅",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ===== Camera Barcode Scanner Modal =====
    if (showBarcodeScannerDialog) {
        CameraBarcodeScannerModal(
            products = products,
            cartItemCount = totalCartCount,
            cartTotalAmount = cartFinalTotal,
            cartItems = cartItems,
            onProductScanned = { product ->
                viewModel.addToCart(product, 1)
            },
            onCartonScanned = { product ->
                viewModel.addToCart(product, product.unitsPerCarton)
            },
            onUpdateCartQuantity = { prodId, qty ->
                viewModel.updateCartItemQuantity(prodId, qty)
            },
            onRemoveFromCart = { prodId ->
                viewModel.removeFromCart(prodId)
            },
            onOpenCartSheet = {
                showCartSheet = true
            },
            onPreviewInvoice = {
                val previewItems = cartItems.map { item ->
                    val itemDiscFactor = (100.0 - cartDiscountPercent.coerceIn(0.0, 100.0)) / 100.0
                    InvoicePreviewItem(
                        productId = item.product.id,
                        name = item.product.name,
                        quantity = item.quantity,
                        unitPrice = item.product.price,
                        totalPrice = item.subtotal * itemDiscFactor,
                        unitsPerCarton = item.product.unitsPerCarton,
                        barcode = item.product.barcode,
                        cartonBarcode = item.product.cartonBarcode
                    )
                }
                val invoiceNum = (System.currentTimeMillis() % 1000000).toString()
                val discAmt = cartSubtotal * (cartDiscountPercent / 100.0)
                previewInvoiceData = InvoicePreviewData(
                    invoiceNumber = invoiceNum,
                    dateMillis = System.currentTimeMillis(),
                    cashierName = currentUser?.username ?: "المدير",
                    customerName = selectedCustomer?.name,
                    isDebt = isCartDebtSale,
                    paymentMethod = if (isCartDebtSale) "DEBT" else "CASH",
                    items = previewItems,
                    subtotal = cartSubtotal,
                    discountPercent = cartDiscountPercent,
                    discountAmount = discAmt,
                    finalTotal = cartFinalTotal,
                    isDraft = true
                )
            },
            onManualCodeSubmit = { code ->
                val trimmed = code.trim()
                val matchedCarton = products.find { it.cartonBarcode != null && it.cartonBarcode == trimmed }
                val matched = matchedCarton ?: products.find { it.barcode != null && it.barcode == trimmed }
                if (matched != null) {
                    val isCarton = matchedCarton != null && matched.unitsPerCarton > 1
                    if (isCarton) {
                        val cartonUnits = matched.unitsPerCarton
                        if (matched.stock >= cartonUnits) {
                            viewModel.addToCart(matched, cartonUnits)
                            viewModel.showMessage("📦 تم إدخال باركود عبوة: ${matched.name} ($cartonUnits ق) ✅")
                        } else if (matched.stock > 0) {
                            viewModel.showMessage("⚠️ المتبقي في المخزون (${matched.stock} قطعة) أقل من عبوة كاملة")
                        } else {
                            viewModel.showMessage("⚠️ عبوة '${matched.name}' نفدت من المخزون!")
                        }
                    } else {
                        if (matched.stock > 0) {
                            viewModel.addToCart(matched, 1)
                            viewModel.showMessage("🏷️ تم إدخال باركود قطعة: ${matched.name} ✅")
                        } else {
                            viewModel.showMessage("⚠️ المنتج '${matched.name}' نفد من المخزون!")
                        }
                    }
                } else {
                    viewModel.showMessage("❌ لا يوجد منتج مسجل بالباركود: $trimmed")
                }
            },
            onDismiss = { showBarcodeScannerDialog = false }
        )
    }

    // ===== Customer Picker Dialog =====
    if (showCustomerPickerModal) {
        CustomerPickerModal(
            customers = customers,
            selectedCustomer = selectedCustomer,
            currentLanguage = currentLanguage,
            onCustomerSelected = { customer ->
                viewModel.selectPosCustomer(customer)
                showCustomerPickerModal = false
            },
            onAddNewCustomerClick = {
                showCustomerPickerModal = false
                showAddCustomerQuickDialog = true
            },
            onDismiss = { showCustomerPickerModal = false }
        )
    }

    // ===== Quick Add New Customer Dialog =====
    if (showAddCustomerQuickDialog) {
        QuickAddCustomerDialog(
            currentLanguage = currentLanguage,
            onDismiss = { showAddCustomerQuickDialog = false },
            onCustomerCreated = { name, phone, email, address, notes, isFav, initDebt ->
                viewModel.addCustomer(
                    name = name,
                    phone = phone,
                    email = email,
                    address = address,
                    notes = notes,
                    isFavorite = isFav,
                    initialDebt = initDebt,
                    andSelectForPos = true
                )
                showAddCustomerQuickDialog = false
            }
        )
    }

    // ===== Draft / Interactive Invoice Preview Dialog =====
    previewInvoiceData?.let { preview ->
        InvoicePreviewDialog(
            invoiceData = preview,
            onDismiss = { previewInvoiceData = null },
            onConfirmAndSave = {
                if (preview.isDraft) {
                    if (preview.items.size == 1 && cartItems.none { it.product.id == preview.items.first().productId }) {
                        // Quick single item sale
                        val item = preview.items.first()
                        viewModel.sellProductDirect(
                            productId = item.productId,
                            quantity = item.quantity,
                            discountPercent = preview.discountPercent,
                            customer = selectedCustomer,
                            isDebt = preview.isDebt,
                            paymentMethod = preview.paymentMethod
                        )
                        previewInvoiceData = preview.copy(isDraft = false)
                    } else {
                        // Cart items checkout
                        coroutineScope.launch {
                            val results = viewModel.checkoutCart(
                                discountPercent = preview.discountPercent,
                                customer = selectedCustomer,
                                isDebt = preview.isDebt,
                                paymentMethod = preview.paymentMethod
                            )
                            if (results != null) {
                                previewInvoiceData = preview.copy(isDraft = false)
                                cartDiscountPercent = 0.0
                                isCartDebtSale = false
                            }
                        }
                    }
                } else {
                    previewInvoiceData = null
                }
            },
            onPrintSuccess = {
                viewModel.showMessage("🖨️ تمت الطباعة بنجاح على طابعة البلوتوث")
            }
        )
    }

    // ===== Sale Confirmation Modal (Success Badge + Direct Thermal POS Print, No PDF share dialog) =====
    completedReceiptSales?.let { sales ->
        val invoiceItems = sales.map { s ->
            val matchingProd = products.find { it.id == s.productId }
            InvoicePreviewItem(
                productId = s.productId,
                name = s.productName,
                quantity = s.quantitySold,
                unitPrice = if (s.quantitySold > 0) s.totalPrice / s.quantitySold else s.totalPrice,
                totalPrice = s.totalPrice,
                unitsPerCarton = matchingProd?.unitsPerCarton ?: 1,
                barcode = matchingProd?.barcode,
                cartonBarcode = matchingProd?.cartonBarcode
            )
        }
        val totalAmt = sales.sumOf { it.totalPrice }
        val invoiceNum = (System.currentTimeMillis() % 1000000).toString()
        val firstSale = sales.firstOrNull()
        val finalizedData = InvoicePreviewData(
            invoiceNumber = invoiceNum,
            dateMillis = firstSale?.saleDate ?: System.currentTimeMillis(),
            cashierName = currentUser?.username ?: "المدير",
            customerName = firstSale?.customerName ?: selectedCustomer?.name,
            isDebt = sales.any { it.isDebt },
            paymentMethod = firstSale?.paymentMethod ?: if (sales.any { it.isDebt }) "DEBT" else "CASH",
            items = invoiceItems,
            subtotal = totalAmt,
            finalTotal = totalAmt,
            isDraft = false
        )

        SaleConfirmationModal(
            invoiceData = finalizedData,
            invoiceStyle = currentInvoiceStyle,
            currentLanguage = currentLanguage,
            onDismiss = { completedReceiptSales = null },
            onOpenPreview = {
                previewInvoiceData = finalizedData
                completedReceiptSales = null
            },
            onOpenPrinterSettings = {
                showPosPrinterModal = true
            }
        )
    }

    // ===== Bluetooth Printer Modal for POS Screen =====
    if (showPosPrinterModal) {
        BluetoothPrinterModal(
            onDismiss = { showPosPrinterModal = false },
            onPrinterConnected = { printerName ->
                viewModel.showMessage("تم تعيين والاتصال بالطابعة: $printerName")
            }
        )
    }
}

@Composable
fun ProductPosCard(
    product: Product,
    onAddToCart: () -> Unit,
    onAddCartonToCart: (() -> Unit)? = null,
    onQuickSell: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.stock <= 0
    val isLowStock = product.stock in 1..5

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_pos_card_${product.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOutOfStock -> DangerRed.copy(alpha = 0.04f)
                isLowStock -> WarningOrange.copy(alpha = 0.04f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            when {
                isOutOfStock -> DangerRed.copy(alpha = 0.5f)
                isLowStock -> WarningOrange.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stock Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isOutOfStock -> DangerRed.copy(alpha = 0.15f)
                        isLowStock -> WarningOrange.copy(alpha = 0.15f)
                        else -> SuccessGreen.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = if (isOutOfStock) "نفد" else if (product.unitsPerCarton > 1) "${product.stock} ق (${product.cartonCount} ك)" else "${product.stock} ق",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isOutOfStock -> DangerRed
                            isLowStock -> WarningOrange
                            else -> SuccessGreen
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (product.unitsPerCarton > 1) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "📦 ${product.unitsPerCarton}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                } else if (product.barcode != null) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "باركود",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val imgFile = remember(product.imagePath) {
                if (!product.imagePath.isNullOrBlank()) File(product.imagePath) else null
            }
            if (imgFile != null && imgFile.exists()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    AsyncImage(
                        model = imgFile,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            } else if (product.image != null && product.image.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    AsyncImage(
                        model = product.image,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 40.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "سعر البيع",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.2f ج.م", product.price),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (product.unitsPerCarton > 1) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "عبوة (${product.unitsPerCarton} ق)",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f ج.م", product.cartonSellingPrice),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilledTonalButton(
                    onClick = onQuickSell,
                    enabled = !isOutOfStock,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("بيع", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (product.unitsPerCarton > 1 && onAddCartonToCart != null) {
                    FilledTonalButton(
                        onClick = onAddCartonToCart,
                        enabled = !isOutOfStock && product.stock >= product.unitsPerCarton,
                        modifier = Modifier.height(38.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+1 📦", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onAddToCart,
                    enabled = !isOutOfStock,
                    modifier = Modifier.size(38.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddShoppingCart,
                        contentDescription = "إضافة للسلة",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickSellModal(
    product: Product,
    currentUser: User?,
    selectedCustomer: Customer?,
    currentLanguage: AppLanguage = AppLanguage.ARABIC,
    onSelectCustomerClick: () -> Unit,
    onAddNewCustomerClick: () -> Unit = {},
    onDismiss: () -> Unit,
    onPreviewInvoice: (qty: Int, discountPercent: Double, isDebt: Boolean) -> Unit,
    onConfirmSale: (qty: Int, discountPercent: Double, isDebt: Boolean) -> Unit,
    onContinueSelling: (qty: Int, discountPercent: Double) -> Unit
) {
    val unitsPerCarton = product.unitsPerCarton.coerceAtLeast(1)
    val maxCartonsAvailable = product.stock / unitsPerCarton

    var cartonInput by remember { mutableStateOf(if (unitsPerCarton > 1 && product.stock >= unitsPerCarton) "1" else "0") }
    var looseInput by remember { mutableStateOf(if (unitsPerCarton > 1 && product.stock >= unitsPerCarton) "0" else "1") }
    var directSellDiscount by remember { mutableDoubleStateOf(0.0) }
    var isDebtSale by remember { mutableStateOf(false) }

    val cartons = cartonInput.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val loose = looseInput.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val calculatedTotalQty = if (unitsPerCarton > 1) {
        (cartons * unitsPerCarton) + loose
    } else {
        loose
    }

    val isValidQty = calculatedTotalQty in 1..product.stock

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(vertical = 12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PointOfSale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "بيع مباشر: ${product.name}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "المخزون المتوفر: ${product.stock} قطعة (${product.cartonCount} عبوة + ${product.remainingLooseUnits} فردي)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Customer selector pill inside quick sell
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedCustomer != null) {
                        if (selectedCustomer.balanceDebt > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectCustomerClick() }
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (selectedCustomer != null) "الزبون: ${selectedCustomer.name}" else "الزبون: نقدي عام (افتراضي)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            TextButton(
                                onClick = onSelectCustomerClick,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (selectedCustomer == null) AppStrings.selectCustomerQuick(currentLanguage) else AppStrings.edit(currentLanguage),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = onAddNewCustomerClick,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = "إضافة زبون جديد",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (unitsPerCarton > 1) {
                    // Carton specification box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📦 تحديد وادخال عدد العبوات:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "العبوة = $unitsPerCarton قطعة",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Carton Input Row with Stepper
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        val cur = cartonInput.toIntOrNull() ?: 0
                                        if (cur > 0) cartonInput = (cur - 1).toString()
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "إنقاص عبوة")
                                }

                                OutlinedTextField(
                                    value = cartonInput,
                                    onValueChange = { cartonInput = it },
                                    label = { Text("عدد العبوات 📦") },
                                    placeholder = { Text("0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                )

                                FilledTonalIconButton(
                                    onClick = {
                                        val cur = cartonInput.toIntOrNull() ?: 0
                                        if ((cur + 1) * unitsPerCarton + loose <= product.stock) {
                                             cartonInput = (cur + 1).toString()
                                        }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "زيادة عبوة")
                                }
                            }

                            // Quick Carton Presets
                            Text("أزرار سريعة للعبوات:", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(1, 2, 5, 10).forEach { num ->
                                    val fits = (num * unitsPerCarton) <= product.stock
                                    OutlinedButton(
                                        onClick = {
                                            cartonInput = num.toString()
                                        },
                                        enabled = fits,
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("$num ع", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (maxCartonsAvailable > 0) {
                                    OutlinedButton(
                                        onClick = {
                                            cartonInput = maxCartonsAvailable.toString()
                                            looseInput = (product.stock % unitsPerCarton).toString()
                                        },
                                        modifier = Modifier.weight(1.1f),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("الكل ($maxCartonsAvailable)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Loose units row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        val cur = looseInput.toIntOrNull() ?: 0
                                        if (cur > 0) looseInput = (cur - 1).toString()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "إنقاص قطعة")
                                }

                                OutlinedTextField(
                                    value = looseInput,
                                    onValueChange = { looseInput = it },
                                    label = { Text("قطع فردية إضافية 🔲") },
                                    placeholder = { Text("0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 14.sp)
                                )

                                FilledTonalIconButton(
                                    onClick = {
                                        val cur = looseInput.toIntOrNull() ?: 0
                                        if (calculatedTotalQty + 1 <= product.stock) {
                                            looseInput = (cur + 1).toString()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "زيادة قطعة")
                                }
                            }
                        }
                    }
                } else {
                    // Single unit product
                    OutlinedTextField(
                        value = looseInput,
                        onValueChange = { looseInput = it },
                        label = { Text("الكمية المطلوبة (بالقطع)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    )
                }

                // Total quantity summary banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إجمالي الكمية للبيع:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (unitsPerCarton > 1) "$calculatedTotalQty قطعة ($cartons عبوة + $loose فردي)" else "$calculatedTotalQty قطعة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Payment Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = !isDebtSale,
                        onClick = { isDebtSale = false },
                        label = { Text("💵 نقدي (كاش)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isDebtSale,
                        onClick = {
                            if (selectedCustomer == null) {
                                onSelectCustomerClick()
                            }
                            isDebtSale = true
                        },
                        label = { Text("⚠️ بيع آجل (دين)") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }

                // Discount row
                Text("نسبة الخصم (%):", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.0, 3.0, 5.0, 10.0, 15.0, 20.0, 25.0).forEach { disc ->
                        FilterChip(
                            selected = directSellDiscount == disc,
                            onClick = { directSellDiscount = disc },
                            label = { Text("${disc.toInt()}%", fontWeight = if (directSellDiscount == disc) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Total Price Calculation Card
                val sub = product.price * calculatedTotalQty
                val fin = sub * ((100.0 - directSellDiscount) / 100.0)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("المبلغ الإجمالي المستحق:", fontSize = 12.sp)
                            if (directSellDiscount > 0) {
                                Text(
                                    text = String.format(Locale.getDefault(), "قبل الخصم: %.2f ج.م", sub),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f ج.م", fin),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (calculatedTotalQty > product.stock) {
                    Text(
                        text = "⚠️ الكمية المطلوبة ($calculatedTotalQty) تتجاوز المخزون المتاح (${product.stock})",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            onContinueSelling(calculatedTotalQty, directSellDiscount)
                        },
                        enabled = isValidQty,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مواصلة البيع 🛒", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (isDebtSale && selectedCustomer == null) {
                                onSelectCustomerClick()
                            } else {
                                onConfirmSale(calculatedTotalQty, directSellDiscount, isDebtSale)
                            }
                        },
                        enabled = isValidQty,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isDebtSale) "تسجيل دين ⚠️" else "إنهاء وتأكيد",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        if (isDebtSale && selectedCustomer == null) {
                            onSelectCustomerClick()
                        } else {
                            onPreviewInvoice(calculatedTotalQty, directSellDiscount, isDebtSale)
                        }
                    },
                    enabled = isValidQty,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("معاينة الفاتورة قبل الطباعة")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun CustomerPickerModal(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    currentLanguage: AppLanguage = AppLanguage.ARABIC,
    onCustomerSelected: (Customer?) -> Unit,
    onAddNewCustomerClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) {
            customers.sortedByDescending { it.isFavorite }
        } else {
            customers.filter {
                it.name.contains(searchQuery.trim(), ignoreCase = true) ||
                it.phone.contains(searchQuery.trim()) ||
                it.email.contains(searchQuery.trim(), ignoreCase = true) ||
                it.address.contains(searchQuery.trim(), ignoreCase = true)
            }.sortedByDescending { it.isFavorite }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (currentLanguage == AppLanguage.FRENCH) "Sélectionner un client" else if (currentLanguage == AppLanguage.ENGLISH) "Select Customer" else "تحديد زبون لعملية البيع",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search row + Add new customer button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (currentLanguage == AppLanguage.FRENCH) "Rechercher..." else if (currentLanguage == AppLanguage.ENGLISH) "Search name/phone..." else "بحث بالاسم أو الهاتف...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = onAddNewCustomerClick,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("picker_add_new_customer_btn")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.newCustomerQuick(currentLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Option: General Cash Customer (Default)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedCustomer == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (selectedCustomer == null) MaterialTheme.colorScheme.primary else Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCustomerSelected(null) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(AppStrings.generalCustomer(currentLanguage), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    if (currentLanguage == AppLanguage.FRENCH) "Paiement direct au comptoir" else if (currentLanguage == AppLanguage.ENGLISH) "Walk-in cash payment" else "دفع نقدي كاش عادي بدون تسجيل دين",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (selectedCustomer == null) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentLanguage == AppLanguage.FRENCH) "Clients enregistrés (${filtered.size}):" else if (currentLanguage == AppLanguage.ENGLISH) "Registered Customers (${filtered.size}):" else "الزبائن المسجلون (${filtered.size}):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) {
                                    if (currentLanguage == AppLanguage.FRENCH) "Aucun client enregistré pour le moment" else if (currentLanguage == AppLanguage.ENGLISH) "No registered customers yet" else "لا يوجد زبائن مسجلين حالياً"
                                } else {
                                    if (currentLanguage == AppLanguage.FRENCH) "Aucun client ne correspond à la recherche" else if (currentLanguage == AppLanguage.ENGLISH) "No customer found" else "لم يتم العثور على زبون مطابق للبحث"
                                },
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = onAddNewCustomerClick,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        if (currentLanguage == AppLanguage.FRENCH) "Créer le client \"$searchQuery\"" else if (currentLanguage == AppLanguage.ENGLISH) "Create \"$searchQuery\"" else "إنشاء زبون باسم \"$searchQuery\""
                                    } else {
                                        AppStrings.addCustomer(currentLanguage)
                                    },
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered, key = { it.id }) { customer ->
                            val isSelected = selectedCustomer?.id == customer.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else if (customer.balanceDebt > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCustomerSelected(customer) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (customer.balanceDebt > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = customer.name.firstOrNull()?.toString() ?: "ز",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                            if (customer.isFavorite) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = WarningOrange,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .align(Alignment.BottomEnd)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Star,
                                                            contentDescription = "Favorite",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(9.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                if (customer.isFavorite) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(Icons.Default.Star, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(13.dp))
                                                }
                                            }
                                            if (customer.phone.isNotBlank()) {
                                                Text(customer.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (customer.email.isNotBlank()) {
                                                Text(customer.email, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        if (customer.balanceDebt > 0) {
                                            Text(
                                                text = String.format(Locale.getDefault(), "دين: %.2f ج.م", customer.balanceDebt),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = DangerRed
                                            )
                                        } else {
                                            Text(
                                                text = if (currentLanguage == AppLanguage.FRENCH) "Réglé" else if (currentLanguage == AppLanguage.ENGLISH) "Clear" else "خالص",
                                                fontSize = 11.sp,
                                                color = SuccessGreen,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.close(currentLanguage))
            }
        }
    )
}

@Composable
fun QuickAddCustomerDialog(
    currentLanguage: AppLanguage = AppLanguage.ARABIC,
    onDismiss: () -> Unit,
    onCustomerCreated: (name: String, phone: String, email: String, address: String, notes: String, isFavorite: Boolean, initialDebt: Double) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var isFavoriteInput by remember { mutableStateOf(false) }
    var initialDebtInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.addCustomer(currentLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("${AppStrings.customerName(currentLanguage)} *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_customer_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text(AppStrings.phone(currentLanguage)) },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_customer_phone_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text(AppStrings.email(currentLanguage)) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_customer_email_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    label = { Text(AppStrings.address(currentLanguage)) },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isFavoriteInput = !isFavoriteInput }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = if (isFavoriteInput) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (isFavoriteInput) WarningOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = AppStrings.favoriteCustomer(currentLanguage),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Switch(
                            checked = isFavoriteInput,
                            onCheckedChange = { isFavoriteInput = it }
                        )
                    }
                }

                OutlinedTextField(
                    value = initialDebtInput,
                    onValueChange = { initialDebtInput = it },
                    label = { Text(if (currentLanguage == AppLanguage.FRENCH) "Solde débiteur initial (optionnel)" else if (currentLanguage == AppLanguage.ENGLISH) "Initial Debt (optional)" else "رصيد دين سابق/افتتاحي (اختياري)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text(AppStrings.notes(currentLanguage)) },
                    leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        val initDebt = initialDebtInput.toDoubleOrNull() ?: 0.0
                        onCustomerCreated(
                            nameInput.trim(),
                            phoneInput.trim(),
                            emailInput.trim(),
                            addressInput.trim(),
                            notesInput.trim(),
                            isFavoriteInput,
                            initDebt
                        )
                    }
                },
                enabled = nameInput.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_and_select_customer_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.createCustomerAndSelect(currentLanguage))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.cancel(currentLanguage))
            }
        }
    )
}
