package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.models.Product
import com.example.ui.components.InventoryListSkeleton
import com.example.ui.components.SingleBarcodeCaptureModal
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodels.AntarSalesViewModel
import com.example.utils.AppLanguage
import com.example.utils.AppStrings
import com.example.utils.ImageStorageHelper
import java.io.File
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: AntarSalesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLang by viewModel.currentLanguage.collectAsState()
    val isLoadingProducts by viewModel.isLoadingProducts.collectAsState()
    val products by viewModel.products.collectAsState()
    val deletedProducts by viewModel.deletedProducts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val lowStockAlertsEnabled by viewModel.lowStockAlertsEnabled.collectAsState()
    val lowStockThreshold by viewModel.lowStockThreshold.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Active, 1: Trash Bin
    var filterStatus by remember { mutableStateOf("الكل") } // "الكل", "متوفر", "منخفض", "نفد"
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var priceEditProduct by remember { mutableStateOf<Product?>(null) }
    var stockEditProduct by remember { mutableStateOf<Product?>(null) }
    var deleteConfirmProduct by remember { mutableStateOf<Product?>(null) }

    val filteredActiveList = remember(products, searchQuery, filterStatus, lowStockThreshold) {
        products.filter { p ->
            val matchQuery = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                p.name.lowercase().contains(q) || (p.barcode != null && p.barcode.contains(q))
            }
            val matchFilter = when (filterStatus) {
                "متوفر" -> p.stock > lowStockThreshold
                "منخفض" -> p.stock in 1..lowStockThreshold
                "نفد" -> p.stock <= 0
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Tab Selector: Active Products vs Trash Bin
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = if (currentLang == AppLanguage.FRENCH) "Produits actifs (${products.size})" else if (currentLang == AppLanguage.ENGLISH) "Active Products (${products.size})" else "المنتجات النشطة (${products.size})",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = if (currentLang == AppLanguage.FRENCH) "Corbeille (${deletedProducts.size})" else if (currentLang == AppLanguage.ENGLISH) "Trash Bin (${deletedProducts.size})" else "المحذوفات (${deletedProducts.size})",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> (width * 0.35f).toInt() } + fadeIn(animationSpec = tween(220)))
                            .togetherWith(slideOutHorizontally { width -> (-width * 0.35f).toInt() } + fadeOut(animationSpec = tween(200)))
                    } else {
                        (slideInHorizontally { width -> (-width * 0.35f).toInt() } + fadeIn(animationSpec = tween(220)))
                            .togetherWith(slideOutHorizontally { width -> (width * 0.35f).toInt() } + fadeOut(animationSpec = tween(200)))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "inventory_tabs_transition"
            ) { tabIndex ->
                if (tabIndex == 0) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = {
                                Text(
                                    if (currentLang == AppLanguage.FRENCH) "Rechercher par nom ou code-barres..."
                                    else if (currentLang == AppLanguage.ENGLISH) "Search inventory by name or barcode..."
                                    else "بحث في المخزون بالاسم أو الباركود..."
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("inventory_search_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Filter Status Chips
                        val filterOptions = remember(currentLang, products, lowStockThreshold) {
                            val lowCount = products.count { it.stock in 1..lowStockThreshold }
                            val outCount = products.count { it.stock <= 0 }
                            listOf(
                                "الكل" to (if (currentLang == AppLanguage.FRENCH) "Tous (${products.size})" else if (currentLang == AppLanguage.ENGLISH) "All (${products.size})" else "الكل (${products.size})"),
                                "متوفر" to (if (currentLang == AppLanguage.FRENCH) "En stock" else if (currentLang == AppLanguage.ENGLISH) "In Stock" else "متوفر"),
                                "منخفض" to (if (currentLang == AppLanguage.FRENCH) "Stock faible ($lowCount)" else if (currentLang == AppLanguage.ENGLISH) "Low Stock ($lowCount)" else "منخفض ($lowCount) ⚠️"),
                                "نفد" to (if (currentLang == AppLanguage.FRENCH) "Épuisé ($outCount)" else if (currentLang == AppLanguage.ENGLISH) "Out of Stock ($outCount)" else "نفد ($outCount) 🚫")
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filterOptions.forEach { (key, label) ->
                                FilterChip(
                                    selected = filterStatus == key,
                                    onClick = { filterStatus = key },
                                    label = { Text(label, fontWeight = if (filterStatus == key) FontWeight.Bold else FontWeight.Normal) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (key == "منخفض") WarningOrange.copy(alpha = 0.2f)
                                        else if (key == "نفد") DangerRed.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = if (key == "منخفض") WarningOrange
                                        else if (key == "نفد") DangerRed
                                        else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        // Low Stock Alert Banner
                        val lowStockItems = remember(products, lowStockThreshold) { products.filter { it.stock in 1..lowStockThreshold } }
                        val outOfStockItems = remember(products) { products.filter { it.stock <= 0 } }
                        if (lowStockAlertsEnabled && (lowStockItems.isNotEmpty() || outOfStockItems.isNotEmpty())) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (outOfStockItems.isNotEmpty()) DangerRed.copy(alpha = 0.1f) else WarningOrange.copy(alpha = 0.1f),
                                border = BorderStroke(
                                    1.dp,
                                    if (outOfStockItems.isNotEmpty()) DangerRed.copy(alpha = 0.35f) else WarningOrange.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.fillMaxWidth()
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
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = if (outOfStockItems.isNotEmpty()) DangerRed else WarningOrange,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (currentLang == AppLanguage.ENGLISH) "Stock Alert: Attention Required"
                                                else if (currentLang == AppLanguage.FRENCH) "Alerte Stock: Attention Requise"
                                                else "تنبيه المخزون: أصناف بحاجة لإعادة الطلب",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (outOfStockItems.isNotEmpty()) DangerRed else WarningOrange
                                            )
                                            Text(
                                                text = if (currentLang == AppLanguage.ENGLISH) "${lowStockItems.size} low stock, ${outOfStockItems.size} out of stock"
                                                else if (currentLang == AppLanguage.FRENCH) "${lowStockItems.size} stock faible, ${outOfStockItems.size} épuisé"
                                                else "يوجد ${lowStockItems.size} صنف منخفض و ${outOfStockItems.size} صنف نفد بالكامل",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (filterStatus != "منخفض" && filterStatus != "نفد") {
                                        TextButton(
                                            onClick = { filterStatus = if (lowStockItems.isNotEmpty()) "منخفض" else "نفد" },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (currentLang == AppLanguage.ENGLISH) "View"
                                                else if (currentLang == AppLanguage.FRENCH) "Voir"
                                                else "عرض",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (outOfStockItems.isNotEmpty()) DangerRed else WarningOrange
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Active List with Skeleton Shimmer and smooth AnimatedContent
                        AnimatedContent(
                            targetState = when {
                                isLoadingProducts -> "LOADING"
                                filteredActiveList.isEmpty() -> "EMPTY"
                                else -> "CONTENT"
                            },
                            transitionSpec = {
                                fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            label = "active_products_anim"
                        ) { state ->
                            when (state) {
                                "LOADING" -> {
                                    InventoryListSkeleton(count = 5)
                                }
                                "EMPTY" -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.ProductionQuantityLimits,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(56.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = if (searchQuery.isNotBlank()) "لا توجد منتجات مطابقة للبحث" else "لا توجد منتجات مطابقة في المخزون",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .testTag("inventory_products_list"),
                                        contentPadding = PaddingValues(bottom = 88.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(filteredActiveList, key = { it.id }) { product ->
                                            InventoryProductCard(
                                                product = product,
                                                onEditPrice = { priceEditProduct = product },
                                                onEditStock = { stockEditProduct = product },
                                                onEditAll = { editingProduct = product },
                                                onSoftDelete = { viewModel.softDeleteProduct(product.id) },
                                                lowStockThreshold = lowStockThreshold,
                                                lowStockAlertsEnabled = lowStockAlertsEnabled
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Recycle Bin (Deleted Products)
                    AnimatedContent(
                        targetState = deletedProducts.isEmpty(),
                        transitionSpec = {
                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "deleted_products_anim"
                    ) { isEmpty ->
                        if (isEmpty) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("سلة المحذوفات فارغة", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 88.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(deletedProducts, key = { it.id }) { product ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Product Image or Icon Thumbnail
                                            ProductThumbnail(
                                                imagePath = product.imagePath,
                                                imageBytes = product.image,
                                                productName = product.name,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = product.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = "بيع: ${product.price} ج.م | شراء: ${product.costPrice} ج.م | المخزون: ${product.stock}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { viewModel.restoreProduct(product.id) },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("استعادة", fontSize = 12.sp)
                                                }

                                                IconButton(
                                                    onClick = { deleteConfirmProduct = product }
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteForever,
                                                        contentDescription = "حذف نهائي",
                                                        tint = MaterialTheme.colorScheme.error
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
        }

        // Add Product FAB
        if (selectedTabIndex == 0) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_product_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "إضافة صنف جديد")
            }
        }
    }

    // ===== Add / Edit Product Dialog =====
    if (showAddDialog || editingProduct != null) {
        val isEditing = editingProduct != null
        val initialUnitsPerCarton = (editingProduct?.unitsPerCarton ?: 12).coerceAtLeast(1)
        val initialStock = editingProduct?.stock ?: (if (isEditing) 0 else 12)
        val initialPurchasePrice = editingProduct?.purchasePrice?.takeIf { it > 0 } ?: 0.0
        val initialSellingPrice = editingProduct?.price?.takeIf { it > 0 } ?: 0.0

        var isCartonRegistrationMode by remember {
            mutableStateOf(if (isEditing) (editingProduct?.unitsPerCarton ?: 1) > 1 else true)
        }
        var nameInput by remember { mutableStateOf(editingProduct?.name ?: "") }
        var unitsPerCartonInput by remember {
            mutableStateOf(initialUnitsPerCarton.toString())
        }
        var purchasePriceInput by remember {
            mutableStateOf(if (initialPurchasePrice > 0) String.format(Locale.US, "%.2f", initialPurchasePrice) else "")
        }
        var priceInput by remember {
            mutableStateOf(if (initialSellingPrice > 0) String.format(Locale.US, "%.2f", initialSellingPrice) else "")
        }

        // Carton Stock & Loose Stock
        var cartonStockInput by remember {
            mutableStateOf(if (initialUnitsPerCarton > 0) (initialStock / initialUnitsPerCarton).toString() else "1")
        }
        var looseStockInput by remember {
            mutableStateOf(if (initialUnitsPerCarton > 0) (initialStock % initialUnitsPerCarton).toString() else "0")
        }
        var stockInput by remember {
            mutableStateOf(initialStock.toString())
        }
        var barcodeInput by remember { mutableStateOf(editingProduct?.barcode ?: "") }
        var cartonBarcodeInput by remember { mutableStateOf(editingProduct?.cartonBarcode ?: "") }
        var isScanningUnitBarcode by remember { mutableStateOf(false) }
        var isScanningCartonBarcode by remember { mutableStateOf(false) }
        var currentImagePath by remember { mutableStateOf(editingProduct?.imagePath) }

        // Gallery Launcher
        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                val saved = ImageStorageHelper.saveImageFromUri(context, uri)
                if (saved != null) {
                    currentImagePath = saved
                    viewModel.showMessage("📸 تم حفظ صورة المنتج في ذاكرة الهاتف بنجاح")
                } else {
                    viewModel.showMessage("❌ تعذر حفظ الصورة")
                }
            }
        }

        // Camera Launcher
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->
            if (bitmap != null) {
                val saved = ImageStorageHelper.saveBitmapToInternalStorage(context, bitmap)
                if (saved != null) {
                    currentImagePath = saved
                    viewModel.showMessage("📷 تم التقاط وحفظ صورة المنتج في ذاكرة الهاتف")
                } else {
                    viewModel.showMessage("❌ تعذر حفظ الصورة الملتقطة")
                }
            }
        }

        val parsedUnitsPerCarton = unitsPerCartonInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val currentStockInt = stockInput.toIntOrNull() ?: 0
        val cartonsInStock = if (parsedUnitsPerCarton > 0) currentStockInt / parsedUnitsPerCarton else 0
        val looseInStock = if (parsedUnitsPerCarton > 0) currentStockInt % parsedUnitsPerCarton else 0

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingProduct = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 12.dp),
            title = {
                Text(
                    text = if (isEditing) "تعديل بيانات المنتج" else "إضافة منتج جديد للمخزن",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ====== Image Selection & Storage Card ======
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!currentImagePath.isNullOrBlank() && File(currentImagePath!!).exists()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = File(currentImagePath!!),
                                        contentDescription = "صورة المنتج",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    // Stored locally pill
                                    Surface(
                                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "💾 مخزنة بذاكرة الهاتف",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Remove Image button
                                    IconButton(
                                        onClick = {
                                            ImageStorageHelper.deleteImageFile(currentImagePath)
                                            currentImagePath = null
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                shape = CircleShape
                                            )
                                            .size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "حذف الصورة",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            galleryLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تغيير من المعرض", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                cameraLauncher.launch(null)
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("التقاط صورة", fontSize = 11.sp)
                                    }
                                }
                            } else {
                                // Empty state placeholder for image
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Outlined.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(38.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "صورة المنتج (تُحفظ في ذاكرة الهاتف)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "تسهل التعرف السريع على الصنف في شاشة البيع",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                galleryLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("اختيار من المعرض", fontSize = 11.sp)
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                try {
                                                    cameraLauncher.launch(null)
                                                } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("التقاط بالكاميرا", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("اسم المنتج أو الصنف *") },
                        placeholder = { Text("مثلاً: عصير تفاح، شيبسي، بسكويت...") },
                        leadingIcon = {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Pricing Section with Profit Margin, Percentage, & Carton Auto-Division
                    ProfitPricingEditorSection(
                        purchasePriceText = purchasePriceInput,
                        onPurchasePriceChange = { purchasePriceInput = it },
                        sellingPriceText = priceInput,
                        onSellingPriceChange = { priceInput = it },
                        unitsPerCartonText = unitsPerCartonInput,
                        onUnitsPerCartonChange = { newUnitsText ->
                            unitsPerCartonInput = newUnitsText
                            val upc = newUnitsText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            val cartons = cartonStockInput.toIntOrNull() ?: 0
                            val loose = looseStockInput.toIntOrNull() ?: 0
                            stockInput = ((cartons * upc) + loose).toString()
                        },
                        isCartonRegistrationMode = isCartonRegistrationMode,
                        onToggleCartonRegistrationMode = { isCartonRegistrationMode = it },
                        currentLang = currentLang
                    )

                    // Stock Management Section (Cartons & Loose or Direct Units)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isCartonRegistrationMode) "📦 تسجيل المخزون بالعبوات:" else "🏷️ المخزون بالقطع:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "الإجمالي: $currentStockInt قطعة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (isCartonRegistrationMode) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = cartonStockInput,
                                        onValueChange = { newCartonsText ->
                                            cartonStockInput = newCartonsText
                                            val c = newCartonsText.toIntOrNull() ?: 0
                                            val l = looseStockInput.toIntOrNull() ?: 0
                                            val upc = unitsPerCartonInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                            stockInput = ((c * upc) + l).toString()
                                        },
                                        label = { Text("عدد العبوات 📦") },
                                        placeholder = { Text("مثلاً 5") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )

                                    OutlinedTextField(
                                        value = looseStockInput,
                                        onValueChange = { newLooseText ->
                                            looseStockInput = newLooseText
                                            val c = cartonStockInput.toIntOrNull() ?: 0
                                            val l = newLooseText.toIntOrNull() ?: 0
                                            val upc = unitsPerCartonInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                            stockInput = ((c * upc) + l).toString()
                                        },
                                        label = { Text("+ قطع فردية") },
                                        placeholder = { Text("0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "المخزون المسجل:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "$cartonStockInput عبوة × $parsedUnitsPerCarton قطعة + $looseStockInput فردي = $currentStockInt قطعة",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = stockInput,
                                    onValueChange = { newStockText ->
                                        stockInput = newStockText
                                        val s = newStockText.toIntOrNull() ?: 0
                                        val upc = unitsPerCartonInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                        cartonStockInput = (s / upc).toString()
                                        looseStockInput = (s % upc).toString()
                                    },
                                    label = { Text("إجمالي القطع في المخزن *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // ===== Unit / Piece Barcode =====
                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = { barcodeInput = it },
                        label = { Text("🏷️ باركود القطعة / الوحدة الفردية") },
                        placeholder = { Text("امسح بالكاميرا أو ولّد تلقائياً") },
                        leadingIcon = {
                            IconButton(onClick = { isScanningUnitBarcode = true }) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = "مسح باركود القطعة بالكاميرا",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    barcodeInput = "622" + Random.nextInt(1000000, 9999999)
                                }) {
                                    Icon(Icons.Default.Autorenew, contentDescription = "توليد باركود قطعة تلقائي")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unit_barcode_input")
                    )

                    // ===== Carton Barcode =====
                    OutlinedTextField(
                        value = cartonBarcodeInput,
                        onValueChange = { cartonBarcodeInput = it },
                        label = { Text("📦 باركود العبوة الكبرى (اختياري)") },
                        placeholder = { Text("امسح باركود العبوة للبيع بالجملة") },
                        leadingIcon = {
                            IconButton(onClick = { isScanningCartonBarcode = true }) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = "مسح باركود العبوة بالكاميرا",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    cartonBarcodeInput = "6229" + Random.nextInt(100000, 999999)
                                }) {
                                    Icon(Icons.Default.Autorenew, contentDescription = "توليد باركود عبوة تلقائي")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("carton_barcode_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val c = purchasePriceInput.toDoubleOrNull() ?: 0.0
                        val p = priceInput.toDoubleOrNull() ?: 0.0
                        val s = stockInput.toIntOrNull() ?: 0
                        val upc = unitsPerCartonInput.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        if (nameInput.isNotBlank() && p > 0) {
                            if (isEditing && editingProduct != null) {
                                val updated = editingProduct!!.copy(
                                    name = nameInput.trim(),
                                    purchasePrice = c,
                                    price = p,
                                    stock = s,
                                    unitsPerCarton = upc,
                                    barcode = barcodeInput.trim().ifEmpty { null },
                                    cartonBarcode = cartonBarcodeInput.trim().ifEmpty { null },
                                    imagePath = currentImagePath
                                )
                                viewModel.updateProduct(updated)
                            } else {
                                viewModel.addProduct(
                                    name = nameInput,
                                    purchasePrice = c,
                                    price = p,
                                    stock = s,
                                    unitsPerCarton = upc,
                                    barcode = barcodeInput,
                                    cartonBarcode = cartonBarcodeInput,
                                    image = null,
                                    imagePath = currentImagePath
                                )
                            }
                            showAddDialog = false
                            editingProduct = null
                        }
                    },
                    enabled = nameInput.isNotBlank() && (priceInput.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Text(if (isEditing) "حفظ التعديلات" else "إضافة المنتج")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    editingProduct = null
                }) {
                    Text("إلغاء")
                }
            }
        )

        // Single Barcode Capture Camera Modals for Unit & Carton
        if (isScanningUnitBarcode) {
            SingleBarcodeCaptureModal(
                title = "مسح باركود القطعة / الوحدة",
                subtitle = "وجّه الكاميرا نحو باركود القطعة الفردية لحفظه",
                onBarcodeCaptured = { code ->
                    barcodeInput = code
                    isScanningUnitBarcode = false
                    viewModel.showMessage("✅ تم مسح وحفظ باركود القطعة: $code")
                },
                onDismiss = { isScanningUnitBarcode = false }
            )
        }

        if (isScanningCartonBarcode) {
            SingleBarcodeCaptureModal(
                title = "مسح باركود العبوة الكبرى",
                subtitle = "وجّه الكاميرا نحو باركود العبوة الخارجية لحفظه",
                onBarcodeCaptured = { code ->
                    cartonBarcodeInput = code
                    isScanningCartonBarcode = false
                    viewModel.showMessage("📦 تم مسح وحفظ باركود العبوة: $code")
                },
                onDismiss = { isScanningCartonBarcode = false }
            )
        }
    }

    // ===== Unified Stock & Price Edit Dialog =====
    val activeEditProduct = stockEditProduct ?: priceEditProduct
    activeEditProduct?.let { prod ->
        StockAndPricesEditDialog(
            product = prod,
            currentLang = currentLang,
            onDismiss = {
                stockEditProduct = null
                priceEditProduct = null
            },
            onSave = { newStock, newPurchasePrice, newSellingPrice ->
                viewModel.updateProductStockAndPrices(prod.id, newStock, newPurchasePrice, newSellingPrice)
                stockEditProduct = null
                priceEditProduct = null
            }
        )
    }

    // ===== Permanent Delete Confirmation =====
    deleteConfirmProduct?.let { prod ->
        AlertDialog(
            onDismissRequest = { deleteConfirmProduct = null },
            title = { Text("تأكيد الحذف النهائي", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف \"${prod.name}\" نهائياً؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentDeleteProduct(prod.id)
                        deleteConfirmProduct = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmProduct = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun ProductThumbnail(
    imagePath: String?,
    imageBytes: ByteArray?,
    productName: String,
    modifier: Modifier = Modifier
) {
    val file = remember(imagePath) {
        if (!imagePath.isNullOrBlank()) File(imagePath) else null
    }

    if (file != null && file.exists()) {
        AsyncImage(
            model = file,
            contentDescription = productName,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (imageBytes != null && imageBytes.isNotEmpty()) {
        AsyncImage(
            model = imageBytes,
            contentDescription = productName,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = modifier
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAndPricesEditDialog(
    product: Product,
    currentLang: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (newStock: Int, newPurchasePrice: Double, newSellingPrice: Double) -> Unit
) {
    var newStockText by remember(product) { mutableStateOf(product.stock.toString()) }
    var newPurchasePriceText by remember(product) {
        mutableStateOf(if (product.purchasePrice > 0) String.format(Locale.US, "%.2f", product.purchasePrice) else "")
    }
    var newSellingPriceText by remember(product) {
        mutableStateOf(if (product.price > 0) String.format(Locale.US, "%.2f", product.price) else "")
    }

    val parsedUnitsPerCarton = product.unitsPerCarton.coerceAtLeast(1)

    val currentStock = newStockText.toIntOrNull() ?: 0
    val costPrice = newPurchasePriceText.toDoubleOrNull() ?: 0.0
    val sellPrice = newSellingPriceText.toDoubleOrNull() ?: 0.0

    val unitProfit = sellPrice - costPrice
    val marginPercent = if (costPrice > 0) (unitProfit / costPrice) * 100.0 else 0.0

    val totalStockValuation = (currentStock.coerceAtLeast(0)) * sellPrice
    val totalStockProfit = (currentStock.coerceAtLeast(0)) * unitProfit

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = if (currentLang == AppLanguage.FRENCH) "Modifier Stock & Prix"
                        else if (currentLang == AppLanguage.ENGLISH) "Edit Stock & Prices"
                        else "تعديل المخزون والأسعار",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = product.name,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Product Summary Header Strip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProductThumbnail(
                            imagePath = product.imagePath,
                            imageBytes = product.image,
                            productName = product.name,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "#${product.id}",
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                if (!product.barcode.isNullOrBlank()) {
                                    Text(
                                        text = "🏷️ ${product.barcode}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // ===== Section 1: Stock Quantity Management =====
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (currentLang == AppLanguage.FRENCH) "1. Quantité en stock"
                                else if (currentLang == AppLanguage.ENGLISH) "1. Stock Quantity"
                                else "١. كمية المخزون المتوفرة",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Carton Fast Addition (if product has units per carton > 1)
                        if (parsedUnitsPerCarton > 1) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📦 العبوة = $parsedUnitsPerCarton قطعة | المخزون الحالي: ${product.stock} ق (${product.cartonCount} عبوة + ${product.remainingLooseUnits} فردي)",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            Text(
                                text = "إضافة توريد سريع بالعبوة:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1, 2, 5, 10).forEach { cartonAmount ->
                                    OutlinedButton(
                                        onClick = {
                                            val currentVal = newStockText.toIntOrNull() ?: 0
                                            newStockText = (currentVal + (cartonAmount * parsedUnitsPerCarton)).toString()
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("+$cartonAmount عبوة", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Stock TextField with Stepper Controls
                        OutlinedTextField(
                            value = newStockText,
                            onValueChange = { newStockText = it },
                            label = { Text("إجمالي الكمية (بالقطع)") },
                            placeholder = { Text("0") },
                            leadingIcon = {
                                Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            val cur = newStockText.toIntOrNull() ?: 0
                                            if (cur > 0) newStockText = (cur - 1).toString()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Minus", tint = MaterialTheme.colorScheme.error)
                                    }
                                    IconButton(
                                        onClick = {
                                            val cur = newStockText.toIntOrNull() ?: 0
                                            newStockText = (cur + 1).toString()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Plus", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_stock_input")
                        )

                        if (parsedUnitsPerCarton > 1) {
                            val cartons = if (currentStock >= 0) currentStock / parsedUnitsPerCarton else 0
                            val loose = if (currentStock >= 0) currentStock % parsedUnitsPerCarton else 0
                            Text(
                                text = "💡 يعادل: $cartons عبوة كاملة + $loose قطعة فردية",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // ===== Section 2: Purchase & Selling Price Management =====
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (currentLang == AppLanguage.FRENCH) "2. Prix d'achat & Prix de vente"
                                else if (currentLang == AppLanguage.ENGLISH) "2. Purchase Cost & Selling Price"
                                else "٢. تعديل سعر الشراء وسعر البيع",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Purchase Price & Selling Price Fields
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Purchase Cost Field
                            OutlinedTextField(
                                value = newPurchasePriceText,
                                onValueChange = { newPurchasePriceText = it },
                                label = { Text("سعر الشراء (التكلفة)") },
                                placeholder = { Text("0.00") },
                                leadingIcon = {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                },
                                suffix = { Text("ج.م", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_purchase_price_input")
                            )

                            // Selling Price Field
                            OutlinedTextField(
                                value = newSellingPriceText,
                                onValueChange = { newSellingPriceText = it },
                                label = { Text("سعر البيع للعميل") },
                                placeholder = { Text("0.00") },
                                leadingIcon = {
                                    Icon(Icons.Default.Sell, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                },
                                suffix = { Text("ج.م", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_selling_price_input")
                            )
                        }

                        if (parsedUnitsPerCarton > 1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "تكلفة العبوة: ${String.format(Locale.US, "%.2f", costPrice * parsedUnitsPerCarton)} ج.م",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "بيع العبوة: ${String.format(Locale.US, "%.2f", sellPrice * parsedUnitsPerCarton)} ج.م",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Live Profit & Margin Indicator
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (unitProfit >= 0) SuccessGreen.copy(alpha = 0.08f) else DangerRed.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, if (unitProfit >= 0) SuccessGreen.copy(alpha = 0.3f) else DangerRed.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "صافي الربح / قطعة:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%+.2f ج.م", unitProfit),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (unitProfit >= 0) SuccessGreen else DangerRed
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (unitProfit >= 0) SuccessGreen else DangerRed
                                    ) {
                                        Text(
                                            text = "هامش ${String.format(Locale.US, "%.1f%%", marginPercent)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (currentStock > 0 && sellPrice > 0) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "قيمة المخزون الإجمالية: ${String.format(Locale.US, "%.2f ج.م", totalStockValuation)}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "إجمالي أرباح الدفعة: ${String.format(Locale.US, "%+.2f ج.م", totalStockProfit)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (totalStockProfit >= 0) SuccessGreen else DangerRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = newStockText.toIntOrNull() ?: 0
                    val c = newPurchasePriceText.toDoubleOrNull() ?: 0.0
                    val p = newSellingPriceText.toDoubleOrNull() ?: 0.0
                    if (p > 0 && s >= 0) {
                        onSave(s, c, p)
                    }
                },
                enabled = (newSellingPriceText.toDoubleOrNull() ?: 0.0) > 0 && (newStockText.toIntOrNull() ?: -1) >= 0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (currentLang == AppLanguage.FRENCH) "Enregistrer"
                    else if (currentLang == AppLanguage.ENGLISH) "Save Changes"
                    else "حفظ التعديلات",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.cancel(currentLang))
            }
        }
    )
}

@Composable
fun InventoryProductCard(
    product: Product,
    onEditPrice: () -> Unit,
    onEditStock: () -> Unit,
    onEditAll: () -> Unit,
    onSoftDelete: () -> Unit,
    lowStockThreshold: Int = 5,
    lowStockAlertsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.stock <= 0
    val isLowStock = lowStockAlertsEnabled && (product.stock in 1..lowStockThreshold)

    val statusColor = when {
        isOutOfStock -> DangerRed
        isLowStock -> WarningOrange
        else -> SuccessGreen
    }

    val statusText = when {
        isOutOfStock -> "نفد المخزون"
        isLowStock -> "${product.stock} ق (منخفض)"
        else -> "${product.stock} قطعة"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("inventory_item_${product.id}"),
        shape = RoundedCornerShape(20.dp),
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
                isLowStock -> WarningOrange.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLowStock || isOutOfStock) 3.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Section: Image + Title/Barcodes + Stock Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Product Image Thumbnail with stock dot overlay
                Box {
                    ProductThumbnail(
                        imagePath = product.imagePath,
                        imageBytes = product.image,
                        productName = product.name,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(14.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .clip(CircleShape)
                            .background(statusColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }

                // Name & Barcodes
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Barcodes Badges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!product.barcode.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        Icons.Default.QrCode,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = product.barcode,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (!product.cartonBarcode.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Inventory2,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = product.cartonBarcode,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Status Badge Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            // Structured Financial & Stock Metrics Grid (Clickable to quickly adjust stock & prices)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditStock() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Top Metrics Row: Selling Price | Cost Price | Profit & Margin
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Selling Price Cell
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سعر البيع",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format(Locale.US, "%.2f ج.م", product.price),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Purchase Price Cell
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "التكلفة",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (product.purchasePrice > 0)
                                    String.format(Locale.US, "%.2f ج.م", product.purchasePrice)
                                else "-",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Profit & Margin Cell
                        val profit = product.profitPerUnit
                        val margin = product.profitMarginPercent
                        Column(
                            modifier = Modifier.weight(1.1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "هامش الربح",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            if (product.purchasePrice > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%+.1f", profit),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (profit >= 0) SuccessGreen else DangerRed
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = (if (profit >= 0) SuccessGreen else DangerRed).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%.0f%%", margin),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (profit >= 0) SuccessGreen else DangerRed,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "غير محدد",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Carton Breakdown Details (if unitsPerCarton > 1)
                    if (product.unitsPerCarton > 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.8.dp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${product.unitsPerCarton} ق/عبوة (بيع: ${String.format(Locale.US, "%.1f", product.cartonSellingPrice)} ج.م)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "${product.cartonCount} عبوة + ${product.remainingLooseUnits} فردي",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Buttons Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Unified Stock & Price Edit Button
                FilledTonalButton(
                    onClick = onEditStock,
                    modifier = Modifier
                        .weight(1.8f)
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("المخزون والأسعار", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Full Edit Button
                OutlinedButton(
                    onClick = onEditAll,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("تعديل", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Delete Button
                OutlinedButton(
                    onClick = onSoftDelete,
                    modifier = Modifier
                        .weight(0.9f)
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("حذف", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitPricingEditorSection(
    purchasePriceText: String,
    onPurchasePriceChange: (String) -> Unit,
    sellingPriceText: String,
    onSellingPriceChange: (String) -> Unit,
    unitsPerCartonText: String = "1",
    onUnitsPerCartonChange: ((String) -> Unit)? = null,
    unitsPerCarton: Int = 1,
    isCartonRegistrationMode: Boolean = true,
    onToggleCartonRegistrationMode: ((Boolean) -> Unit)? = null,
    currentLang: AppLanguage = AppLanguage.ARABIC,
    modifier: Modifier = Modifier
) {
    val effectiveUnits = if (onUnitsPerCartonChange != null) {
        unitsPerCartonText.toIntOrNull()?.coerceAtLeast(1) ?: 1
    } else {
        unitsPerCarton.coerceAtLeast(1)
    }

    val costPerUnit = purchasePriceText.toDoubleOrNull() ?: 0.0
    val sellPerUnit = sellingPriceText.toDoubleOrNull() ?: 0.0
    val profitPerUnit = sellPerUnit - costPerUnit
    val marginPercent = if (costPerUnit > 0) (profitPerUnit / costPerUnit) * 100.0 else 0.0

    val cartonCost = costPerUnit * effectiveUnits
    val cartonSell = sellPerUnit * effectiveUnits
    val cartonProfit = profitPerUnit * effectiveUnits

    var cartonCostInput by remember(costPerUnit, effectiveUnits) {
        mutableStateOf(if (costPerUnit > 0) String.format(Locale.US, "%.2f", cartonCost) else "")
    }

    var cartonSellInput by remember(sellPerUnit, effectiveUnits) {
        mutableStateOf(if (sellPerUnit > 0) String.format(Locale.US, "%.2f", cartonSell) else "")
    }

    // Pricing modes: 0 = Carton Price, 1 = Unit Price, 2 = Margin %, 3 = Profit Amount
    var pricingMode by remember { mutableIntStateOf(if (isCartonRegistrationMode) 0 else 1) }

    var percentInput by remember {
        val p = if (costPerUnit > 0 && sellPerUnit > costPerUnit) {
            String.format(Locale.US, "%.1f", marginPercent)
        } else "25"
        mutableStateOf(p)
    }

    var amountInput by remember {
        val a = if (sellPerUnit > costPerUnit) {
            String.format(Locale.US, "%.2f", profitPerUnit)
        } else "5.00"
        mutableStateOf(a)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mode Selector: Register by Box / Carton vs Single Piece
        if (onToggleCartonRegistrationMode != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "📋 طريقة تسجيل وحساب المنتج:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = isCartonRegistrationMode,
                            onClick = {
                                onToggleCartonRegistrationMode(true)
                                pricingMode = 0
                            },
                            label = {
                                Text(
                                    text = "📦 بالعبوة / طرد",
                                    fontSize = 12.sp,
                                    fontWeight = if (isCartonRegistrationMode) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = !isCartonRegistrationMode,
                            onClick = {
                                onToggleCartonRegistrationMode(false)
                                pricingMode = 1
                            },
                            label = {
                                Text(
                                    text = "🏷️ بالقطعة الفردية",
                                    fontSize = 12.sp,
                                    fontWeight = if (!isCartonRegistrationMode) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ===== Section 1: Purchase / Cost Input & Carton Division =====
        if (isCartonRegistrationMode) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "📦 بيانات تكلفة العبوة (شراء بالجملة):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = cartonCostInput,
                            onValueChange = { newCartonCostText ->
                                cartonCostInput = newCartonCostText
                                val cCost = newCartonCostText.toDoubleOrNull() ?: 0.0
                                if (effectiveUnits > 0) {
                                    val computedUnitCost = cCost / effectiveUnits
                                    onPurchasePriceChange(String.format(Locale.US, "%.2f", computedUnitCost))

                                    // Update selling price based on mode
                                    if (pricingMode == 2) { // %
                                        val pct = percentInput.toDoubleOrNull() ?: 0.0
                                        val computedSell = computedUnitCost * (1.0 + pct / 100.0)
                                        onSellingPriceChange(String.format(Locale.US, "%.2f", computedSell))
                                    } else if (pricingMode == 3) { // amount
                                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                                        val computedSell = (computedUnitCost + amt).coerceAtLeast(0.0)
                                        onSellingPriceChange(String.format(Locale.US, "%.2f", computedSell))
                                    }
                                }
                            },
                            label = { Text("تكلفة العبوة (ج.م) *") },
                            placeholder = { Text("مثلاً 240.00") },
                            leadingIcon = {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )

                        if (onUnitsPerCartonChange != null) {
                            OutlinedTextField(
                                value = unitsPerCartonText,
                                onValueChange = { newUnitsText ->
                                    onUnitsPerCartonChange(newUnitsText)
                                    val u = newUnitsText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                    val cCost = cartonCostInput.toDoubleOrNull() ?: (costPerUnit * u)
                                    val computedUnitCost = if (u > 0) cCost / u else 0.0
                                    onPurchasePriceChange(String.format(Locale.US, "%.2f", computedUnitCost))
                                },
                                label = { Text("عدد الوحدات 📦 *") },
                                placeholder = { Text("12") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 🧮 Math division formula highlight card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Calculate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "معادلة تقسيم تكلفة العبوة:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "حساب تلقائي",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            val cVal = cartonCostInput.toDoubleOrNull() ?: (costPerUnit * effectiveUnits)
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.2f", cVal)} ج.م (عبوة) ÷ $effectiveUnits قطعة = ${String.format(Locale.getDefault(), "%.2f", costPerUnit)} ج.م (تكلفة القطعة)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        } else {
            // Direct Single Piece Purchase Price
            OutlinedTextField(
                value = purchasePriceText,
                onValueChange = { newCostText ->
                    onPurchasePriceChange(newCostText)
                    val newCost = newCostText.toDoubleOrNull() ?: 0.0
                    cartonCostInput = String.format(Locale.US, "%.2f", newCost * effectiveUnits)
                    if (pricingMode == 2) { // % mode
                        val pct = percentInput.toDoubleOrNull() ?: 0.0
                        if (newCost > 0) {
                            val computedSell = newCost * (1.0 + pct / 100.0)
                            onSellingPriceChange(String.format(Locale.US, "%.2f", computedSell))
                        }
                    } else if (pricingMode == 3) { // amount mode
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        val computedSell = (newCost + amt).coerceAtLeast(0.0)
                        onSellingPriceChange(String.format(Locale.US, "%.2f", computedSell))
                    }
                },
                label = { Text("سعر الشراء / التكلفة للقطعة (ج.م) *") },
                placeholder = { Text("0.00") },
                leadingIcon = {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ===== Section 2: Pricing & Profit Margin Selector =====
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "🎯 تحديد سعر البيع وهامش الربح عبر:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isCartonRegistrationMode) {
                    // Row 1: Carton Sale vs Piece Sale
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = pricingMode == 0,
                            onClick = { pricingMode = 0 },
                            label = {
                                Text(
                                    text = "📦 بيع العبوة",
                                    fontSize = 11.sp,
                                    fontWeight = if (pricingMode == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = pricingMode == 1,
                            onClick = { pricingMode = 1 },
                            label = {
                                Text(
                                    text = "🏷️ بيع القطعة",
                                    fontSize = 11.sp,
                                    fontWeight = if (pricingMode == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Margin % vs Profit Amount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = pricingMode == 2,
                            onClick = {
                                pricingMode = 2
                                val pct = percentInput.toDoubleOrNull() ?: 20.0
                                if (costPerUnit > 0) {
                                    val computed = costPerUnit * (1.0 + pct / 100.0)
                                    onSellingPriceChange(String.format(Locale.US, "%.2f", computed))
                                    cartonSellInput = String.format(Locale.US, "%.2f", computed * effectiveUnits)
                                }
                            },
                            label = {
                                Text(
                                    text = "📈 نسبة ربح %",
                                    fontSize = 11.sp,
                                    fontWeight = if (pricingMode == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = pricingMode == 3,
                            onClick = {
                                pricingMode = 3
                                val amt = amountInput.toDoubleOrNull() ?: 5.0
                                val computed = (costPerUnit + amt).coerceAtLeast(0.0)
                                onSellingPriceChange(String.format(Locale.US, "%.2f", computed))
                                cartonSellInput = String.format(Locale.US, "%.2f", computed * effectiveUnits)
                            },
                            label = {
                                Text(
                                    text = "💰 مبلغ ربح ثابت",
                                    fontSize = 11.sp,
                                    fontWeight = if (pricingMode == 3) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Single unit: 3 modes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = pricingMode == 1,
                            onClick = { pricingMode = 1 },
                            label = {
                                Text(
                                    text = "🏷️ بيع مباشر",
                                    fontSize = 11.sp,
                                    fontWeight = if (pricingMode == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = pricingMode == 2,
                            onClick = {
                                pricingMode = 2
                                val pct = percentInput.toDoubleOrNull() ?: 20.0
                                if (costPerUnit > 0) {
                                    val computed = costPerUnit * (1.0 + pct / 100.0)
                                    onSellingPriceChange(String.format(Locale.US, "%.2f", computed))
                                    cartonSellInput = String.format(Locale.US, "%.2f", computed * effectiveUnits)
                                }
                            },
                            label = {
                                Text(
                                    text = "📈 نسبة %",
                                    fontSize = 11.sp,
                                    fontWeight = if (pricingMode == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = pricingMode == 3,
                            onClick = {
                                pricingMode = 3
                                val amt = amountInput.toDoubleOrNull() ?: 5.0
                                val computed = (costPerUnit + amt).coerceAtLeast(0.0)
                                onSellingPriceChange(String.format(Locale.US, "%.2f", computed))
                                cartonSellInput = String.format(Locale.US, "%.2f", computed * effectiveUnits)
                            },
                            label = {
                                Text(
                                    text = "💰 مبلغ ربح",
                                    fontSize = 11.sp,
                                    fontWeight = if (pricingMode == 3) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ===== Section 3: Mode-specific Selling Price Controls =====
        when (pricingMode) {
            0 -> {
                // By Carton Selling Price
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = cartonSellInput,
                        onValueChange = { newCartonSellText ->
                            cartonSellInput = newCartonSellText
                            val cSell = newCartonSellText.toDoubleOrNull() ?: 0.0
                            if (effectiveUnits > 0) {
                                val computedUnitSell = cSell / effectiveUnits
                                onSellingPriceChange(String.format(Locale.US, "%.2f", computedUnitSell))
                                if (costPerUnit > 0) {
                                    val newProfit = computedUnitSell - costPerUnit
                                    percentInput = String.format(Locale.US, "%.1f", (newProfit / costPerUnit) * 100.0)
                                    amountInput = String.format(Locale.US, "%.2f", newProfit)
                                }
                            }
                        },
                        label = { Text("سعر بيع العبوة بالكامل (ج.م) *") },
                        placeholder = { Text("مثلاً 300.00") },
                        leadingIcon = {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = SuccessGreen)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 سعر بيع القطعة التلقائي = ${String.format(Locale.getDefault(), "%.2f", sellPerUnit)} ج.م (لكل قطعة)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            1 -> {
                // By Unit Selling Price
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = sellingPriceText,
                        onValueChange = { newSellText ->
                            onSellingPriceChange(newSellText)
                            val newSell = newSellText.toDoubleOrNull() ?: 0.0
                            cartonSellInput = String.format(Locale.US, "%.2f", newSell * effectiveUnits)
                            if (costPerUnit > 0) {
                                val newProfit = newSell - costPerUnit
                                percentInput = String.format(Locale.US, "%.1f", (newProfit / costPerUnit) * 100.0)
                                amountInput = String.format(Locale.US, "%.2f", newProfit)
                            }
                        },
                        label = { Text("سعر بيع القطعة الواحدة (ج.م) *") },
                        placeholder = { Text("مثلاً 25.00") },
                        leadingIcon = {
                            Icon(Icons.Default.PriceCheck, contentDescription = null, tint = SuccessGreen)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (effectiveUnits > 1) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📦 سعر بيع العبوة التلقائي ($effectiveUnits قطع) = ${String.format(Locale.getDefault(), "%.2f", sellPerUnit * effectiveUnits)} ج.م",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            2 -> {
                // By Target Margin Percentage (%)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = percentInput,
                        onValueChange = { newPctText ->
                            percentInput = newPctText
                            val pct = newPctText.toDoubleOrNull() ?: 0.0
                            if (costPerUnit > 0) {
                                val computedSell = costPerUnit * (1.0 + pct / 100.0)
                                onSellingPriceChange(String.format(Locale.US, "%.2f", computedSell))
                                cartonSellInput = String.format(Locale.US, "%.2f", computedSell * effectiveUnits)
                                amountInput = String.format(Locale.US, "%.2f", computedSell - costPerUnit)
                            }
                        },
                        label = { Text("نسبة هامش الربح المستهدفة (%) *") },
                        placeholder = { Text("مثلاً 25") },
                        leadingIcon = {
                            Icon(Icons.Default.Percent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            Text("%", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick percentage chips with horizontal scroll
                    Text(
                        text = "نسب ربح سريعة (مرر للاختيار):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 15, 20, 25, 30, 40, 50, 75, 100).forEach { p ->
                            SuggestionChip(
                                onClick = {
                                    percentInput = p.toString()
                                    if (costPerUnit > 0) {
                                        val computedSell = costPerUnit * (1.0 + p / 100.0)
                                        onSellingPriceChange(String.format(Locale.US, "%.2f", computedSell))
                                        cartonSellInput = String.format(Locale.US, "%.2f", computedSell * effectiveUnits)
                                        amountInput = String.format(Locale.US, "%.2f", computedSell - costPerUnit)
                                    }
                                },
                                label = { Text("+$p%", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
            3 -> {
                // By Profit Amount (EGP)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { newAmtText ->
                            amountInput = newAmtText
                            val amt = newAmtText.toDoubleOrNull() ?: 0.0
                            val computedSell = (costPerUnit + amt).coerceAtLeast(0.0)
                            onSellingPriceChange(String.format(Locale.US, "%.2f", computedSell))
                            cartonSellInput = String.format(Locale.US, "%.2f", computedSell * effectiveUnits)
                            if (costPerUnit > 0) {
                                percentInput = String.format(Locale.US, "%.1f", (amt / costPerUnit) * 100.0)
                            }
                        },
                        label = { Text("مبلغ الربح الصافي للقطعة (ج.م) *") },
                        placeholder = { Text("مثلاً 5.00") },
                        leadingIcon = {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            Text("ج.م", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick amount chips with horizontal scroll
                    Text(
                        text = "مبالغ ربح سريعة للقطعة (مرر للاختيار):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.5, 1.0, 2.0, 3.0, 5.0, 10.0, 15.0, 20.0, 50.0, 100.0).forEach { a ->
                            val labelText = if (a == a.toLong().toDouble()) "+${a.toInt()} ج.م" else "+$a ج.م"
                            SuggestionChip(
                                onClick = {
                                    amountInput = a.toString()
                                    val computedSell = (costPerUnit + a).coerceAtLeast(0.0)
                                    onSellingPriceChange(String.format(Locale.US, "%.2f", computedSell))
                                    cartonSellInput = String.format(Locale.US, "%.2f", computedSell * effectiveUnits)
                                    if (costPerUnit > 0) {
                                        percentInput = String.format(Locale.US, "%.1f", (a / costPerUnit) * 100.0)
                                    }
                                },
                                label = { Text(labelText, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // ===== Section 4: Live Calculation & Comprehensive Profit Preview Card =====
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = when {
                sellPerUnit <= 0 -> MaterialTheme.colorScheme.surfaceVariant
                sellPerUnit < costPerUnit -> DangerRed.copy(alpha = 0.12f)
                sellPerUnit == costPerUnit -> WarningOrange.copy(alpha = 0.12f)
                else -> SuccessGreen.copy(alpha = 0.12f)
            },
            border = BorderStroke(
                width = 1.dp,
                color = when {
                    sellPerUnit <= 0 -> MaterialTheme.colorScheme.outlineVariant
                    sellPerUnit < costPerUnit -> DangerRed.copy(alpha = 0.4f)
                    sellPerUnit == costPerUnit -> WarningOrange.copy(alpha = 0.4f)
                    else -> SuccessGreen.copy(alpha = 0.4f)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header row of the summary card: Unit selling price & Unit profit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "سعر البيع النهائي (قطعة):",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f ج.م", sellPerUnit),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (sellPerUnit > costPerUnit) SuccessGreen else if (sellPerUnit < costPerUnit && sellPerUnit > 0) DangerRed else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "صافي ربح القطعة:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.getDefault(), "%+.2f ج.م", profitPerUnit),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (profitPerUnit >= 0) SuccessGreen else DangerRed
                            )
                            if (costPerUnit > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (profitPerUnit >= 0) SuccessGreen.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%+.1f%%", marginPercent),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (profitPerUnit >= 0) SuccessGreen else DangerRed,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Carton metrics row
                if (effectiveUnits > 1 && sellPerUnit > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📦 بيع العبوة ($effectiveUnits قطع): ${String.format(Locale.getDefault(), "%.2f ج.م", cartonSell)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "ربح العبوة: ${String.format(Locale.getDefault(), "%+.2f ج.م", cartonProfit)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cartonProfit >= 0) SuccessGreen else DangerRed
                        )
                    }
                }

                // Status notice
                if (sellPerUnit > 0 && costPerUnit > 0) {
                    if (sellPerUnit < costPerUnit) {
                        Text(
                            text = "⚠️ تنبيه: سعر البيع أقل من التكلفة (خسارة بمقدار ${String.format(Locale.getDefault(), "%.2f", costPerUnit - sellPerUnit)} ج.م للقطعة)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed
                        )
                    } else if (sellPerUnit == costPerUnit) {
                        Text(
                            text = "ℹ️ سعر البيع مساوٍ للتكلفة تماماً (بدون أي هامش ربح)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WarningOrange
                        )
                    }
                }
            }
        }
    }
}
