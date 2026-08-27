package com.example.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AntarRepository
import com.example.data.dao.MonthlySalesReport
import com.example.data.dao.WeeklySalesReport
import com.example.data.dao.YearlySalesReport
import com.example.data.models.Customer
import com.example.data.models.CustomerTransaction
import com.example.data.models.InvoiceStyle
import com.example.data.models.Product
import com.example.data.models.Sale
import com.example.data.models.User
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.AppThemePalette
import com.example.utils.AppLanguage
import com.example.utils.AppPreferencesManager
import com.example.utils.CsvHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val subtotal: Double get() = product.price * quantity
}

class AntarSalesViewModel(
    private val repository: AntarRepository,
    private val preferencesManager: AppPreferencesManager? = null
) : ViewModel() {

    // Products
    val products: StateFlow<List<Product>> = repository.getAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedProducts: StateFlow<List<Product>> = repository.getDeletedProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val filteredProducts: StateFlow<List<Product>> = combine(products, _searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            val q = query.trim().lowercase()
            list.filter {
                it.name.lowercase().contains(q) ||
                        (it.barcode != null && it.barcode.contains(q)) ||
                        (it.cartonBarcode != null && it.cartonBarcode.contains(q))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sales & Totals
    private val _todayTotal = MutableStateFlow(0.0)
    val todayTotal: StateFlow<Double> = _todayTotal.asStateFlow()

    val todaySales: StateFlow<List<Sale>> = repository.getTodaySales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSales: StateFlow<List<Sale>> = repository.getAllRecentSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSales: StateFlow<List<Sale>> = repository.getAllSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User & Authentication
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    val allUsers: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // POS Cart
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val cartTotal: StateFlow<Double> = _cartItems.combine(_cartItems) { items, _ ->
        items.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Customers & POS Debt State
    val customers: StateFlow<List<Customer>> = repository.getAllCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteCustomers: StateFlow<List<Customer>> = repository.getFavoriteCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customersWithDebt: StateFlow<List<Customer>> = repository.getCustomersWithDebt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCustomerDebt: StateFlow<Double> = customers.map { list ->
        list.sumOf { it.balanceDebt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _selectedPosCustomer = MutableStateFlow<Customer?>(null)
    val selectedPosCustomer: StateFlow<Customer?> = _selectedPosCustomer.asStateFlow()

    fun selectPosCustomer(customer: Customer?) {
        _selectedPosCustomer.value = customer
    }

    // Reports
    private val _weeklyReport = MutableStateFlow<List<WeeklySalesReport>>(emptyList())
    val weeklyReport: StateFlow<List<WeeklySalesReport>> = _weeklyReport.asStateFlow()

    private val _monthlyReport = MutableStateFlow<List<MonthlySalesReport>>(emptyList())
    val monthlyReport: StateFlow<List<MonthlySalesReport>> = _monthlyReport.asStateFlow()

    private val _yearlyReport = MutableStateFlow<List<YearlySalesReport>>(emptyList())
    val yearlyReport: StateFlow<List<YearlySalesReport>> = _yearlyReport.asStateFlow()

    // Loading States for Skeleton Shimmer UI
    private val _isLoadingProducts = MutableStateFlow(true)
    val isLoadingProducts: StateFlow<Boolean> = _isLoadingProducts.asStateFlow()

    private val _isLoadingReports = MutableStateFlow(true)
    val isLoadingReports: StateFlow<Boolean> = _isLoadingReports.asStateFlow()

    // Language State
    private val _currentLanguage = MutableStateFlow(AppLanguage.ARABIC)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        viewModelScope.launch {
            preferencesManager?.setAppLanguage(language.name)
        }
        val msg = when (language) {
            AppLanguage.FRENCH -> "[FR] Langue changée en Français"
            AppLanguage.ENGLISH -> "[EN] Language changed to English"
            AppLanguage.ARABIC -> "[AR] تم تغيير لغة التطبيق إلى العربية"
        }
        showMessage(msg)
    }

    // Multiple Theme Palette & Mode State
    private val _currentThemePalette = MutableStateFlow(AppThemePalette.PURPLE)
    val currentThemePalette: StateFlow<AppThemePalette> = _currentThemePalette.asStateFlow()

    private val _currentThemeMode = MutableStateFlow(AppThemeMode.LIGHT)
    val currentThemeMode: StateFlow<AppThemeMode> = _currentThemeMode.asStateFlow()

    fun setThemePalette(palette: AppThemePalette) {
        _currentThemePalette.value = palette
        viewModelScope.launch {
            preferencesManager?.setThemePalette(palette.name)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _currentThemeMode.value = mode
        viewModelScope.launch {
            preferencesManager?.setThemeMode(mode.name)
        }
    }

    fun toggleDarkMode(isDark: Boolean) {
        val mode = if (isDark) AppThemeMode.DARK else AppThemeMode.LIGHT
        _currentThemeMode.value = mode
        viewModelScope.launch {
            preferencesManager?.setThemeMode(mode.name)
        }
    }

    // Invoice Style & Thermal Printer Preferences (persisted in DataStore)
    private val _invoiceStyle = MutableStateFlow(InvoiceStyle.DETAILED)
    val invoiceStyle: StateFlow<InvoiceStyle> = _invoiceStyle.asStateFlow()

    private val _thermalPaperWidth = MutableStateFlow("80")
    val thermalPaperWidth: StateFlow<String> = _thermalPaperWidth.asStateFlow()

    private val _storeName = MutableStateFlow("AF store")
    val storeName: StateFlow<String> = _storeName.asStateFlow()

    private val _storePhone = MutableStateFlow("01012345678")
    val storePhone: StateFlow<String> = _storePhone.asStateFlow()

    private val _currencySymbol = MutableStateFlow("ج.م")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _lowStockAlertsEnabled = MutableStateFlow(true)
    val lowStockAlertsEnabled: StateFlow<Boolean> = _lowStockAlertsEnabled.asStateFlow()

    private val _lowStockThreshold = MutableStateFlow(5)
    val lowStockThreshold: StateFlow<Int> = _lowStockThreshold.asStateFlow()

    fun setStoreName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            _storeName.value = trimmed
            viewModelScope.launch {
                preferencesManager?.setStoreName(trimmed)
            }
            showMessage("🏪 تم حفظ اسم المتجر: $trimmed")
        }
    }

    fun setStorePhone(phone: String) {
        val trimmed = phone.trim()
        _storePhone.value = trimmed
        viewModelScope.launch {
            preferencesManager?.setStorePhone(trimmed)
        }
        showMessage("📞 تم حفظ رقم هاتف المتجر")
    }

    fun setCurrencySymbol(currency: String) {
        val trimmed = currency.trim()
        if (trimmed.isNotBlank()) {
            _currencySymbol.value = trimmed
            viewModelScope.launch {
                preferencesManager?.setCurrencySymbol(trimmed)
            }
            showMessage("💱 تم ضبط العملة المعتمدة: $trimmed")
        }
    }

    fun setLowStockAlertsEnabled(enabled: Boolean) {
        _lowStockAlertsEnabled.value = enabled
        viewModelScope.launch {
            preferencesManager?.setLowStockAlertsEnabled(enabled)
        }
        val msg = if (enabled) "🔔 تم تفعيل تنبيهات نقص المخزون" else "🔕 تم تعطيل تنبيهات نقص المخزون"
        showMessage(msg)
    }

    fun setLowStockThreshold(threshold: Int) {
        val safeThreshold = threshold.coerceIn(1, 100)
        _lowStockThreshold.value = safeThreshold
        viewModelScope.launch {
            preferencesManager?.setLowStockThreshold(safeThreshold)
        }
        showMessage("⚠️ تم ضبط حد التنبيه لنقص المخزون: $safeThreshold قطع")
    }

    fun setInvoiceStyle(style: InvoiceStyle) {
        _invoiceStyle.value = style
        viewModelScope.launch {
            preferencesManager?.setInvoiceStyle(style)
        }
        val msg = when (_currentLanguage.value) {
            AppLanguage.FRENCH -> "[FR] Modèle de facture défini : ${style.getDisplayName(AppLanguage.FRENCH)}"
            AppLanguage.ENGLISH -> "[EN] Invoice template set: ${style.getDisplayName(AppLanguage.ENGLISH)}"
            AppLanguage.ARABIC -> "✅ تم ضبط نموذج الفاتورة المعتمد: ${style.getDisplayName(AppLanguage.ARABIC)}"
        }
        showMessage(msg)
    }

    fun setThermalPaperWidth(width: String) {
        _thermalPaperWidth.value = width
        viewModelScope.launch {
            preferencesManager?.setThermalPaperWidth(width)
        }
        showMessage("🖨️ تم ضبط عرض ورق الطابعة الحرارية: ${width}مم")
    }

    // Status / Messages
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        // Collect saved preferences from DataStore
        if (preferencesManager != null) {
            viewModelScope.launch {
                preferencesManager.invoiceStyleFlow.collect { savedStyle ->
                    _invoiceStyle.value = savedStyle
                }
            }
            viewModelScope.launch {
                preferencesManager.thermalPaperWidthFlow.collect { savedWidth ->
                    _thermalPaperWidth.value = savedWidth
                }
            }
            viewModelScope.launch {
                preferencesManager.storeNameFlow.collect { savedName ->
                    _storeName.value = savedName
                }
            }
            viewModelScope.launch {
                preferencesManager.storePhoneFlow.collect { savedPhone ->
                    _storePhone.value = savedPhone
                }
            }
            viewModelScope.launch {
                preferencesManager.currencySymbolFlow.collect { savedCurrency ->
                    _currencySymbol.value = savedCurrency
                }
            }
            viewModelScope.launch {
                preferencesManager.lowStockAlertsEnabledFlow.collect { savedEnabled ->
                    _lowStockAlertsEnabled.value = savedEnabled
                }
            }
            viewModelScope.launch {
                preferencesManager.lowStockThresholdFlow.collect { savedThreshold ->
                    _lowStockThreshold.value = savedThreshold
                }
            }
            viewModelScope.launch {
                preferencesManager.appLanguageFlow.collect { savedLang ->
                    if (savedLang != null) {
                        try {
                            _currentLanguage.value = AppLanguage.valueOf(savedLang)
                        } catch (_: Exception) {}
                    }
                }
            }
            viewModelScope.launch {
                preferencesManager.themePaletteFlow.collect { savedPalette ->
                    if (savedPalette != null) {
                        try {
                            _currentThemePalette.value = AppThemePalette.valueOf(savedPalette)
                        } catch (_: Exception) {}
                    }
                }
            }
            viewModelScope.launch {
                preferencesManager.themeModeFlow.collect { savedMode ->
                    if (savedMode != null) {
                        try {
                            _currentThemeMode.value = AppThemeMode.valueOf(savedMode)
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        // Refresh today's total on change
        viewModelScope.launch {
            repository.getTodaySales().collect {
                _todayTotal.value = repository.getTodayTotal()
            }
        }

        // Initialize default user, restore logged-in session, and starter products
        viewModelScope.launch {
            _isLoadingProducts.value = true
            ensureDefaultAccounts()
            
            // Restore persistent user session if logged in previously
            if (preferencesManager != null) {
                try {
                    val isLogged = preferencesManager.isLoggedInFlow.first()
                    val savedUsername = preferencesManager.loggedInUsernameFlow.first()
                    if (isLogged && !savedUsername.isNullOrBlank()) {
                        val user = repository.getUserByUsername(savedUsername)
                        if (user != null) {
                            _currentUser.value = user
                            _isLoggedIn.value = true
                        }
                    }
                } catch (_: Exception) {}
            }

            seedInitialProductsIfEmpty()
            kotlinx.coroutines.delay(450)
            _isLoadingProducts.value = false
        }

        viewModelScope.launch {
            loadReports(showLoading = true)
        }
    }

    private suspend fun ensureDefaultAccounts() {
        val defaultTenUsers = listOf(
            Pair("admin1", "atr1"),
            Pair("admin2", "atr2"),
            Pair("admin3", "atr3"),
            Pair("admin4", "Atr4"),
            Pair("admin5", "atr5"),
            Pair("admin6", "atr6"),
            Pair("admin7", "atr7"),
            Pair("admin8", "atr8"),
            Pair("admin9", "atr9"),
            Pair("admin10", "atr10")
        )

        for ((uname, pwd) in defaultTenUsers) {
            val existing = repository.getUserByUsername(uname)
            if (existing == null) {
                repository.addUser(
                    User(
                        username = uname,
                        passwordHash = hashPassword(pwd),
                        role = "admin"
                    )
                )
            } else {
                val expectedHash = hashPassword(pwd)
                if (existing.passwordHash != expectedHash) {
                    repository.addUser(
                        existing.copy(passwordHash = expectedHash, role = "admin")
                    )
                }
            }
        }
    }

    private suspend fun seedInitialProductsIfEmpty() {
        val currentList = repository.getAllProductsList()
        if (currentList.isEmpty()) {
            val starterItems = listOf(
                Product(name = "أرز مصري فاخر (1 كجم)", purchasePrice = 25.00, price = 32.50, stock = 50, unitsPerCarton = 10, barcode = "6221001001", cartonBarcode = "6229001001"),
                Product(name = "سكر ناعم مكرر (1 كجم)", purchasePrice = 22.00, price = 28.00, stock = 60, unitsPerCarton = 10, barcode = "6221001002", cartonBarcode = "6229001002"),
                Product(name = "زيت عباد الشمس نقي (800 مل)", purchasePrice = 52.00, price = 65.00, stock = 36, unitsPerCarton = 12, barcode = "6221001003", cartonBarcode = "6229001003"),
                Product(name = "شاي العروسة ناعم (250 جم)", purchasePrice = 38.00, price = 48.00, stock = 72, unitsPerCarton = 24, barcode = "6221001004", cartonBarcode = "6229001004"),
                Product(name = "لبن جهينة كامل الدسم (1 لتر)", purchasePrice = 34.00, price = 42.00, stock = 36, unitsPerCarton = 12, barcode = "6221001005", cartonBarcode = "6229001005"),
                Product(name = "مكرونة روجينا أصابع (400 جم)", purchasePrice = 14.50, price = 18.50, stock = 60, unitsPerCarton = 20, barcode = "6221001006", cartonBarcode = "6229001006"),
                Product(name = "جبنة بيضاء دومتي (500 جم)", purchasePrice = 30.00, price = 38.00, stock = 24, unitsPerCarton = 12, barcode = "6221001007", cartonBarcode = "6229001007"),
                Product(name = "تونة صن شاين قطع (185 جم)", purchasePrice = 42.00, price = 52.00, stock = 48, unitsPerCarton = 24, barcode = "6221001008", cartonBarcode = "6229001008"),
                Product(name = "مسحوق غسيل أوتوماتيك (2.5 كجم)", purchasePrice = 115.00, price = 145.00, stock = 16, unitsPerCarton = 4, barcode = "6221001009", cartonBarcode = "6229001009"),
                Product(name = "بسكويت سادة دايجستف (150 جم)", purchasePrice = 12.00, price = 16.00, stock = 48, unitsPerCarton = 12, barcode = "6221001010", cartonBarcode = "6229001010")
            )
            repository.insertProducts(starterItems)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun clearMessage() {
        _message.value = null
    }

    fun showMessage(msg: String) {
        _message.value = msg
    }

    // ===== Cart Operations =====
    fun addToCart(product: Product, quantity: Int = 1, discountPercent: Double = 0.0) {
        if (product.stock <= 0) {
            showMessage("⚠️ هذا المنتج نفد من المخزون!")
            return
        }
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            val newQty = existing.quantity + quantity
            if (newQty > product.stock) {
                showMessage("⚠️ الكمية المطلوبة تتجاوز المخزون المتوفر (${product.stock})")
                return
            }
            current[index] = existing.copy(quantity = newQty)
        } else {
            if (quantity > product.stock) {
                showMessage("⚠️ الكمية المطلوبة تتجاوز المخزون المتوفر (${product.stock})")
                return
            }
            current.add(CartItem(product, quantity))
        }
        _cartItems.value = current
        showMessage("🛒 تمت إضافة ($quantity قطعة) من ${product.name} إلى السلة")
    }

    fun addToCartByCarton(product: Product, cartonCount: Int = 1) {
        val qty = cartonCount * (if (product.unitsPerCarton > 0) product.unitsPerCarton else 1)
        if (qty > product.stock) {
            showMessage("⚠️ المخزون المتوفر لا يكفي لـ $cartonCount عبوة (مطلوب $qty قطعة، المتوفر ${product.stock})")
            return
        }
        addToCart(product, qty)
    }

    fun updateCartItemQuantity(productId: Int, newQuantity: Int) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (newQuantity <= 0) {
                current.removeAt(index)
            } else {
                val item = current[index]
                if (newQuantity > item.product.stock) {
                    showMessage("⚠️ الكمية المطلوبة تتجاوز المخزون (${item.product.stock})")
                    return
                }
                current[index] = item.copy(quantity = newQuantity)
            }
            _cartItems.value = current
        }
    }

    fun removeFromCart(productId: Int) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    // ===== Camera Barcode Scanner Support =====
    suspend fun getProductByBarcode(barcode: String): Product? {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return null
        return repository.getProductByBarcode(trimmed)
    }

    suspend fun scanBarcodeAndAddToCart(barcode: String): Product? {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return null
        val product = repository.getProductByBarcode(trimmed)
        if (product != null) {
            val isCarton = product.cartonBarcode != null && product.cartonBarcode == trimmed
            if (isCarton && product.unitsPerCarton > 1) {
                // Scanned a whole carton!
                val cartonUnits = product.unitsPerCarton
                if (product.stock >= cartonUnits) {
                    addToCart(product, cartonUnits)
                    showMessage("📦 تم مسح باركود عبوة: ${product.name} (عدد $cartonUnits قطعة) وإضافتها للسلة ✅")
                } else if (product.stock > 0) {
                    showMessage("⚠️ تم مسح عبوة '${product.name}' ولكن المتبقي في المخزون (${product.stock} قطعة فقط)")
                } else {
                    showMessage("⚠️ تم مسح عبوة '${product.name}' ولكن المنتج نفد من المخزون!")
                }
            } else {
                // Scanned a single unit / piece
                if (product.stock > 0) {
                    addToCart(product, 1)
                    showMessage("🏷️ تم مسح باركود قطعة: ${product.name} وإضافتها للسلة ✅")
                } else {
                    showMessage("⚠️ تم مسح '${product.name}' ولكن المنتج نفد من المخزون!")
                }
            }
            return product
        } else {
            showMessage("❌ لم يتم العثور على منتج مسجل بالباركود: $trimmed")
            return null
        }
    }

    suspend fun checkoutCart(
        discountPercent: Double = 0.0,
        customer: Customer? = _selectedPosCustomer.value,
        isDebt: Boolean = false,
        paymentMethod: String = if (isDebt) "DEBT" else "CASH"
    ): List<Sale>? {
        val items = _cartItems.value
        if (items.isEmpty()) return null

        val completedSales = mutableListOf<Sale>()
        var successCount = 0

        for (item in items) {
            val success = if (discountPercent > 0.0) {
                repository.sellProductWithDiscount(
                    productId = item.product.id,
                    quantity = item.quantity,
                    discountPercent = discountPercent,
                    customerId = customer?.id,
                    customerName = customer?.name,
                    isDebt = isDebt,
                    paymentMethod = paymentMethod
                )
            } else {
                repository.sellProduct(
                    productId = item.product.id,
                    quantity = item.quantity,
                    customerId = customer?.id,
                    customerName = customer?.name,
                    isDebt = isDebt,
                    paymentMethod = paymentMethod
                )
            }
            if (success) {
                successCount++
                val discountFactor = (100.0 - discountPercent.coerceIn(0.0, 100.0)) / 100.0
                completedSales.add(
                    Sale(
                        productId = item.product.id,
                        productName = if (discountPercent > 0) "${item.product.name} (خصم ${discountPercent.toInt()}%)" else item.product.name,
                        quantitySold = item.quantity,
                        totalPrice = (item.product.price * item.quantity) * discountFactor,
                        saleDate = System.currentTimeMillis(),
                        customerId = customer?.id,
                        customerName = customer?.name,
                        isDebt = isDebt,
                        paymentMethod = paymentMethod
                    )
                )
            }
        }

        if (successCount > 0) {
            _cartItems.value = emptyList()
            _todayTotal.value = repository.getTodayTotal()
            loadReports()
            val debtMsg = if (isDebt && customer != null) " (تم تسجيل الدين على حساب ${customer.name})" else ""
            showMessage("✅ تم إتمام عملية البيع بنجاح ($successCount أصناف)$debtMsg")
            return completedSales
        } else {
            showMessage("❌ تعذر إتمام البيع، تحقق من الكميات في المخزن")
            return null
        }
    }

    // ===== Single Product Operations =====
    fun addProduct(
        name: String,
        purchasePrice: Double,
        price: Double,
        stock: Int,
        unitsPerCarton: Int = 1,
        barcode: String?,
        cartonBarcode: String? = null,
        image: ByteArray? = null,
        imagePath: String? = null
    ) {
        if (name.isBlank() || price <= 0) {
            showMessage("⚠️ يرجى إدخال اسم وسعر بيع صحيحين للمنتج")
            return
        }
        viewModelScope.launch {
            repository.addProduct(
                Product(
                    name = name.trim(),
                    purchasePrice = purchasePrice.coerceAtLeast(0.0),
                    price = price,
                    stock = stock.coerceAtLeast(0),
                    unitsPerCarton = unitsPerCarton.coerceAtLeast(1),
                    barcode = barcode?.trim()?.ifEmpty { null },
                    cartonBarcode = cartonBarcode?.trim()?.ifEmpty { null },
                    image = image,
                    imagePath = imagePath?.trim()?.ifEmpty { null },
                    isDeleted = false
                )
            )
            showMessage("✅ تمت إضافة المنتج بنجاح: $name (العبوة: $unitsPerCarton قطعة)")
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
            showMessage("✅ تم تحديث بيانات المنتج: ${product.name}")
        }
    }

    fun addStockCartons(productId: Int, cartonsToAdd: Int) {
        viewModelScope.launch {
            val product = repository.getProductById(productId) ?: return@launch
            val unitsToAdd = cartonsToAdd * product.unitsPerCarton
            val newStock = product.stock + unitsToAdd
            product.stock = newStock
            repository.updateProduct(product)
            showMessage("📦 تم توريد $cartonsToAdd عبوة (+$unitsToAdd قطعة) لـ ${product.name}")
        }
    }

    fun updateProductPrices(productId: Int, newPurchasePrice: Double, newSellingPrice: Double) {
        viewModelScope.launch {
            val product = repository.getProductById(productId) ?: return@launch
            product.purchasePrice = newPurchasePrice.coerceAtLeast(0.0)
            product.price = newSellingPrice.coerceAtLeast(0.0)
            repository.updateProduct(product)
            showMessage("✅ تم تحديث أسعار ${product.name} (شراء: $newPurchasePrice | بيع: $newSellingPrice ج.م)")
        }
    }

    fun updateProductStockAndPrices(productId: Int, newStock: Int, newPurchasePrice: Double, newSellingPrice: Double) {
        viewModelScope.launch {
            val product = repository.getProductById(productId) ?: return@launch
            product.stock = newStock.coerceAtLeast(0)
            product.purchasePrice = newPurchasePrice.coerceAtLeast(0.0)
            product.price = newSellingPrice.coerceAtLeast(0.0)
            repository.updateProduct(product)
            showMessage("✅ تم حفظ المخزون ($newStock قطعة) والأسعار (شراء: $newPurchasePrice | بيع: $newSellingPrice ج.م) لـ ${product.name}")
        }
    }

    fun updateProductPrice(productId: Int, newPrice: Double) {
        viewModelScope.launch {
            val product = repository.getProductById(productId) ?: return@launch
            product.price = newPrice
            repository.updateProduct(product)
            showMessage("✅ تم تحديث سعر بيع ${product.name} إلى $newPrice ج.م")
        }
    }

    fun updateProductStock(productId: Int, newStock: Int) {
        viewModelScope.launch {
            val product = repository.getProductById(productId) ?: return@launch
            product.stock = newStock.coerceAtLeast(0)
            repository.updateProduct(product)
            showMessage("✅ تم تعديل مخزون ${product.name} إلى $newStock")
        }
    }

    fun softDeleteProduct(productId: Int) {
        viewModelScope.launch {
            repository.softDeleteProduct(productId)
            showMessage("🗑️ تم نقل المنتج إلى سلة المحذوفات")
        }
    }

    fun restoreProduct(productId: Int) {
        viewModelScope.launch {
            repository.restoreProduct(productId)
            showMessage("♻️ تمت استعادة المنتج إلى القائمة النشطة")
        }
    }

    fun permanentDeleteProduct(productId: Int) {
        viewModelScope.launch {
            repository.permanentDeleteProduct(productId)
            showMessage("❌ تم الحذف النهائي للمنتج")
        }
    }

    fun sellProductDirect(
        productId: Int,
        quantity: Int,
        discountPercent: Double = 0.0,
        customer: Customer? = null,
        isDebt: Boolean = false,
        paymentMethod: String = if (isDebt) "DEBT" else "CASH"
    ) {
        viewModelScope.launch {
            val success = if (discountPercent > 0.0) {
                repository.sellProductWithDiscount(
                    productId = productId,
                    quantity = quantity,
                    discountPercent = discountPercent,
                    customerId = customer?.id,
                    customerName = customer?.name,
                    isDebt = isDebt,
                    paymentMethod = paymentMethod
                )
            } else {
                repository.sellProduct(
                    productId = productId,
                    quantity = quantity,
                    customerId = customer?.id,
                    customerName = customer?.name,
                    isDebt = isDebt,
                    paymentMethod = paymentMethod
                )
            }
            if (success) {
                _todayTotal.value = repository.getTodayTotal()
                loadReports()
                val debtMsg = if (isDebt && customer != null) " (دين على ${customer.name})" else ""
                showMessage("✅ تم تسجيل البيع بنجاح$debtMsg")
            } else {
                showMessage("⚠️ رصيد المخزن لا يكفي لهذه الكمية!")
            }
        }
    }

    // ===== Customer Operations =====
    fun addCustomer(
        name: String,
        phone: String = "",
        email: String = "",
        address: String = "",
        notes: String = "",
        isFavorite: Boolean = false,
        creditLimit: Double = 0.0,
        initialDebt: Double = 0.0,
        andSelectForPos: Boolean = false
    ) {
        if (name.isBlank()) {
            showMessage("⚠️ يرجى إدخال اسم الزبون")
            return
        }
        viewModelScope.launch {
            val customer = Customer(
                name = name.trim(),
                phone = phone.trim(),
                email = email.trim(),
                address = address.trim(),
                notes = notes.trim(),
                isFavorite = isFavorite,
                balanceDebt = initialDebt.coerceAtLeast(0.0),
                totalPurchases = initialDebt.coerceAtLeast(0.0)
            )
            val id = repository.addCustomer(customer)
            if (initialDebt > 0) {
                repository.addManualCustomerDebt(
                    customerId = id.toInt(),
                    customerName = name.trim(),
                    amount = initialDebt,
                    notes = "رصيد افتتاحي سابق"
                )
            }
            val created = customer.copy(id = id.toInt())
            if (andSelectForPos) {
                _selectedPosCustomer.value = created
                showMessage("✅ تم إنشاء الزبون وتحديده للبيع: $name")
            } else {
                showMessage("✅ تمت إضافة الزبون بنجاح: $name")
            }
        }
    }

    fun toggleCustomerFavorite(customer: Customer) {
        viewModelScope.launch {
            val newFav = !customer.isFavorite
            repository.setCustomerFavorite(customer.id, newFav)
            if (_selectedPosCustomer.value?.id == customer.id) {
                _selectedPosCustomer.value = _selectedPosCustomer.value?.copy(isFavorite = newFav)
            }
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            showMessage("✅ تم تحديث بيانات الزبون: ${customer.name}")
        }
    }

    fun deleteCustomer(id: Int) {
        viewModelScope.launch {
            repository.deleteCustomer(id)
            if (_selectedPosCustomer.value?.id == id) {
                _selectedPosCustomer.value = null
            }
            showMessage("🗑️ تم حذف الزبون وسجل معاملاته بنجاح")
        }
    }

    fun recordCustomerPayment(customerId: Int, customerName: String, amount: Double, notes: String) {
        if (amount <= 0) {
            showMessage("⚠️ المبلغ يجب أن يكون أكبر من الصفر")
            return
        }
        viewModelScope.launch {
            repository.recordCustomerPayment(customerId, customerName, amount, notes)
            showMessage("💵 تم تسجيل سداد مبلغ $amount ج.م بنجاح للزبون $customerName")
        }
    }

    fun addCustomerDebt(customerId: Int, customerName: String, amount: Double, notes: String) {
        if (amount <= 0) {
            showMessage("⚠️ المبلغ يجب أن يكون أكبر من الصفر")
            return
        }
        viewModelScope.launch {
            repository.addManualCustomerDebt(customerId, customerName, amount, notes)
            showMessage("⚠️ تمت إضافة دين $amount ج.م على حساب $customerName")
        }
    }

    fun getCustomerTransactions(customerId: Int): Flow<List<CustomerTransaction>> =
        repository.getCustomerTransactions(customerId)

    fun exportCustomersToCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.getAllCustomersList()
            val csvText = CsvHelper.generateCustomersCsv(list)
            val success = CsvHelper.writeStringToUri(context, uri, csvText)
            withContext(Dispatchers.Main) {
                if (success) showMessage("✅ تم تصدير بيانات ${list.size} زبون إلى CSV بنجاح")
                else showMessage("❌ فشل تصدير ملف زبائن CSV")
            }
        }
    }

    fun importCustomersFromCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val csvContent = CsvHelper.readUriContent(context, uri)
            val parsed = CsvHelper.parseCustomersCsv(csvContent)
            if (parsed.isNotEmpty()) {
                repository.insertCustomers(parsed)
                withContext(Dispatchers.Main) {
                    showMessage("✅ تم استيراد ${parsed.size} زبون بنجاح!")
                }
            } else {
                withContext(Dispatchers.Main) {
                    showMessage("⚠️ لم يتم العثور على بيانات زبائن صالحة في ملف CSV")
                }
            }
        }
    }

    // ===== Auth & Users =====
    suspend fun login(username: String, password: String): Boolean {
        val cleanUsername = username.trim()
        val user = repository.getUserByUsername(cleanUsername)
        if (user != null) {
            val passHash = hashPassword(password)
            val isValidPassword = user.passwordHash == passHash ||
                (cleanUsername.equals("admin4", ignoreCase = true) && (passHash == hashPassword("atr4") || passHash == hashPassword("Atr4")))
            if (isValidPassword) {
                _currentUser.value = user
                _isLoggedIn.value = true
                preferencesManager?.saveUserSession(user.username)
                showMessage("👋 مرحباً بك، ${user.username} (مدير النظام)")
                return true
            }
        }
        showMessage("❌ اسم المستخدم أو كلمة المرور غير صحيحة")
        return false
    }

    fun logout() {
        _currentUser.value = null
        _isLoggedIn.value = false
        _cartItems.value = emptyList()
        viewModelScope.launch {
            preferencesManager?.clearUserSession()
        }
        showMessage("🔒 تم تسجيل الخروج")
    }

    fun addNewUser(username: String, password: String, role: String) {
        if (username.isBlank() || password.length < 3) {
            showMessage("⚠️ يرجى إدخال اسم مستخدم وكلمة مرور صحيحة (3 أحرف على الأقل)")
            return
        }
        viewModelScope.launch {
            val existing = repository.getUserByUsername(username.trim())
            if (existing != null) {
                showMessage("⚠️ اسم المستخدم مسجل بالفعل!")
                return@launch
            }
            repository.addUser(
                User(
                    username = username.trim(),
                    passwordHash = hashPassword(password),
                    role = role
                )
            )
            showMessage("✅ تم إنشاء حساب المستخدم: $username ($role)")
        }
    }

    fun deleteUser(userId: Int) {
        viewModelScope.launch {
            repository.deleteUser(userId)
            showMessage("✅ تم حذف حساب المستخدم")
        }
    }

    fun refreshProducts() {
        viewModelScope.launch {
            _isLoadingProducts.value = true
            kotlinx.coroutines.delay(400)
            _isLoadingProducts.value = false
        }
    }

    // ===== Reports =====
    fun loadReports(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) {
                _isLoadingReports.value = true
            }
            _weeklyReport.value = repository.getWeeklyReport()
            _monthlyReport.value = repository.getMonthlyReport()
            _yearlyReport.value = repository.getYearlyReport()
            _todayTotal.value = repository.getTodayTotal()
            if (showLoading || _isLoadingReports.value) {
                kotlinx.coroutines.delay(400)
                _isLoadingReports.value = false
            }
        }
    }

    // ===== CSV & Backup =====
    fun exportProductsToCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val productsList = repository.getAllProductsList()
            val csvText = CsvHelper.generateProductsCsv(productsList)
            val success = CsvHelper.writeStringToUri(context, uri, csvText)
            withContext(Dispatchers.Main) {
                if (success) showMessage("✅ تم تصدير ${productsList.size} صنف إلى ملف CSV بنجاح")
                else showMessage("❌ فشل تصدير ملف CSV")
            }
        }
    }

    fun importProductsFromCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val csvContent = CsvHelper.readUriContent(context, uri)
            val parsedProducts = CsvHelper.parseProductsCsv(csvContent)
            if (parsedProducts.isNotEmpty()) {
                repository.insertProducts(parsedProducts)
                withContext(Dispatchers.Main) {
                    showMessage("✅ تم استيراد ${parsedProducts.size} منتج بنجاح!")
                }
            } else {
                withContext(Dispatchers.Main) {
                    showMessage("⚠️ لم يتم العثور على بيانات صالحة في ملف CSV")
                }
            }
        }
    }

    fun exportSalesToCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val salesList = repository.getAllSalesList()
            val csvText = CsvHelper.generateSalesCsv(salesList)
            val success = CsvHelper.writeStringToUri(context, uri, csvText)
            withContext(Dispatchers.Main) {
                if (success) showMessage("✅ تم تصدير سجل المبيعات (${salesList.size} عملية) بنجاح")
                else showMessage("❌ فشل تصدير سجل المبيعات")
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.deleteAllProducts()
            repository.deleteAllSales()
            repository.deleteAllCustomers()
            _todayTotal.value = 0.0
            _cartItems.value = emptyList()
            _selectedPosCustomer.value = null
            loadReports()
            seedInitialProductsIfEmpty()
            showMessage("🔄 تم إعادة ضبط قاعدة البيانات بنجاح")
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

class AntarSalesViewModelFactory(
    private val repository: AntarRepository,
    private val preferencesManager: AppPreferencesManager? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AntarSalesViewModel::class.java)) {
            return AntarSalesViewModel(repository, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
