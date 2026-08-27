package com.example.data

import com.example.data.dao.MonthlySalesReport
import com.example.data.dao.StoreDao
import com.example.data.dao.WeeklySalesReport
import com.example.data.dao.YearlySalesReport
import com.example.data.models.Customer
import com.example.data.models.CustomerTransaction
import com.example.data.models.Product
import com.example.data.models.Sale
import com.example.data.models.User
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class AntarRepository(private val dao: StoreDao) {

    // ===== Products =====
    suspend fun addProduct(product: Product): Long = dao.addProduct(product)
    suspend fun updateProduct(product: Product) = dao.updateProduct(product)
    suspend fun updateProductPrices(productId: Int, purchasePrice: Double, price: Double) = dao.updateProductPrices(productId, purchasePrice, price)
    suspend fun updateProductStockAndPrices(productId: Int, stock: Int, purchasePrice: Double, price: Double) = dao.updateProductStockAndPrices(productId, stock, purchasePrice, price)
    suspend fun updateProductPurchasePrice(productId: Int, purchasePrice: Double) = dao.updateProductPurchasePrice(productId, purchasePrice)
    suspend fun updateProductPrice(productId: Int, price: Double) = dao.updateProductPrice(productId, price)
    suspend fun getProductById(id: Int): Product? = dao.getProductById(id)
    suspend fun getProductByBarcode(barcode: String): Product? = dao.getProductByBarcode(barcode)
    suspend fun getProductByCartonBarcode(cartonBarcode: String): Product? = dao.getProductByCartonBarcode(cartonBarcode)
    suspend fun getProductByUnitBarcode(unitBarcode: String): Product? = dao.getProductByUnitBarcode(unitBarcode)
    suspend fun softDeleteProduct(id: Int) = dao.softDeleteProduct(id)
    suspend fun permanentDeleteProduct(id: Int) = dao.permanentDeleteProduct(id)
    suspend fun restoreProduct(id: Int) = dao.restoreProduct(id)
    fun getAllProducts(): Flow<List<Product>> = dao.getAllProducts()
    fun getDeletedProducts(): Flow<List<Product>> = dao.getDeletedProducts()
    suspend fun getAllProductsList(): List<Product> = dao.getAllProductsList()
    suspend fun deleteAllProducts() = dao.deleteAllProducts()
    suspend fun insertProducts(products: List<Product>) = dao.insertProducts(products)
    fun searchProducts(query: String): Flow<List<Product>> = dao.searchProducts(query)

    // ===== Sales =====
    suspend fun sellProduct(
        productId: Int,
        quantity: Int,
        customerId: Int? = null,
        customerName: String? = null,
        isDebt: Boolean = false,
        paymentMethod: String = "CASH"
    ): Boolean {
        val product = dao.getProductById(productId) ?: return false
        if (product.stock < quantity) return false
        dao.reduceStock(productId, quantity)
        val totalPrice = product.price * quantity
        dao.recordSale(
            Sale(
                productId = product.id,
                productName = product.name,
                quantitySold = quantity,
                totalPrice = totalPrice,
                saleDate = System.currentTimeMillis(),
                customerId = customerId,
                customerName = customerName,
                isDebt = isDebt,
                paymentMethod = paymentMethod
            )
        )
        if (customerId != null) {
            val debtToAdd = if (isDebt) totalPrice else 0.0
            dao.updateCustomerBalances(customerId, debtToAdd, totalPrice)
            if (isDebt) {
                dao.insertCustomerTransaction(
                    CustomerTransaction(
                        customerId = customerId,
                        customerName = customerName ?: "",
                        type = "SALE_DEBT",
                        amount = totalPrice,
                        notes = "فاتورة بيع آجل: ${product.name} (عدد $quantity)"
                    )
                )
            }
        }
        return true
    }

    suspend fun sellProductWithDiscount(
        productId: Int,
        quantity: Int,
        discountPercent: Double,
        customerId: Int? = null,
        customerName: String? = null,
        isDebt: Boolean = false,
        paymentMethod: String = "CASH"
    ): Boolean {
        val product = dao.getProductById(productId) ?: return false
        if (product.stock < quantity) return false
        dao.reduceStock(productId, quantity)
        val discountFactor = (100.0 - discountPercent.coerceIn(0.0, 100.0)) / 100.0
        val finalPrice = (product.price * quantity) * discountFactor
        val discountLabel = if (discountPercent > 0) {
            String.format(Locale.getDefault(), " (خصم %.0f%%)", discountPercent)
        } else ""

        dao.recordSale(
            Sale(
                productId = product.id,
                productName = "${product.name}$discountLabel",
                quantitySold = quantity,
                totalPrice = finalPrice,
                saleDate = System.currentTimeMillis(),
                customerId = customerId,
                customerName = customerName,
                isDebt = isDebt,
                paymentMethod = paymentMethod
            )
        )
        if (customerId != null) {
            val debtToAdd = if (isDebt) finalPrice else 0.0
            dao.updateCustomerBalances(customerId, debtToAdd, finalPrice)
            if (isDebt) {
                dao.insertCustomerTransaction(
                    CustomerTransaction(
                        customerId = customerId,
                        customerName = customerName ?: "",
                        type = "SALE_DEBT",
                        amount = finalPrice,
                        notes = "فاتورة بيع آجل: ${product.name} (عدد $quantity$discountLabel)"
                    )
                )
            }
        }
        return true
    }

    suspend fun recordDirectSale(sale: Sale): Long {
        val id = dao.recordSale(sale)
        if (sale.customerId != null) {
            val debtToAdd = if (sale.isDebt) sale.totalPrice else 0.0
            dao.updateCustomerBalances(sale.customerId, debtToAdd, sale.totalPrice)
            if (sale.isDebt) {
                dao.insertCustomerTransaction(
                    CustomerTransaction(
                        customerId = sale.customerId,
                        customerName = sale.customerName ?: "",
                        type = "SALE_DEBT",
                        amount = sale.totalPrice,
                        notes = "فاتورة بيع آجل: ${sale.productName}"
                    )
                )
            }
        }
        return id
    }

    suspend fun getTodayTotal(): Double = dao.getTodayTotalSales()
    fun getTodaySales(): Flow<List<Sale>> = dao.getTodaySales()
    fun getAllRecentSales(): Flow<List<Sale>> = dao.getAllRecentSales()
    fun getAllSales(): Flow<List<Sale>> = dao.getAllSales()
    fun getSalesBetweenDates(startTime: Long, endTime: Long): Flow<List<Sale>> = dao.getSalesBetweenDates(startTime, endTime)
    suspend fun getAllSalesList(): List<Sale> = dao.getAllSalesList()
    suspend fun insertSales(sales: List<Sale>) = dao.insertSales(sales)
    suspend fun deleteAllSales() = dao.deleteAllSales()

    // ===== Customers =====
    suspend fun addCustomer(customer: Customer): Long = dao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = dao.updateCustomer(customer)
    suspend fun deleteCustomer(id: Int) {
        dao.deleteCustomer(id)
        dao.deleteCustomerTransactions(id)
    }
    suspend fun getCustomerById(id: Int): Customer? = dao.getCustomerById(id)
    fun getAllCustomers(): Flow<List<Customer>> = dao.getAllCustomers()
    fun getFavoriteCustomers(): Flow<List<Customer>> = dao.getFavoriteCustomers()
    fun getCustomersWithDebt(): Flow<List<Customer>> = dao.getCustomersWithDebt()
    fun searchCustomers(query: String): Flow<List<Customer>> = dao.searchCustomers(query)
    suspend fun setCustomerFavorite(id: Int, isFavorite: Boolean) = dao.setCustomerFavorite(id, isFavorite)
    suspend fun getAllCustomersList(): List<Customer> = dao.getAllCustomersList()
    suspend fun insertCustomers(customers: List<Customer>) = dao.insertCustomers(customers)
    suspend fun deleteAllCustomers() {
        dao.deleteAllCustomers()
        dao.deleteAllCustomerTransactions()
    }

    // ===== Customer Transactions =====
    suspend fun recordCustomerPayment(customerId: Int, customerName: String, amount: Double, notes: String): Long {
        dao.payCustomerDebt(customerId, amount)
        return dao.insertCustomerTransaction(
            CustomerTransaction(
                customerId = customerId,
                customerName = customerName,
                type = "PAYMENT",
                amount = amount,
                notes = if (notes.isBlank()) "سداد دفعة نقدية من الحساب" else notes
            )
        )
    }

    suspend fun addManualCustomerDebt(customerId: Int, customerName: String, amount: Double, notes: String): Long {
        dao.updateCustomerBalances(customerId, debtChange = amount, purchasesChange = 0.0)
        return dao.insertCustomerTransaction(
            CustomerTransaction(
                customerId = customerId,
                customerName = customerName,
                type = "MANUAL_DEBT",
                amount = amount,
                notes = if (notes.isBlank()) "إضافة رصيد مدين يدوي" else notes
            )
        )
    }

    fun getCustomerTransactions(customerId: Int): Flow<List<CustomerTransaction>> =
        dao.getCustomerTransactions(customerId)

    fun getAllRecentCustomerTransactions(): Flow<List<CustomerTransaction>> =
        dao.getAllRecentCustomerTransactions()

    fun getSalesByCustomer(customerId: Int): Flow<List<Sale>> =
        dao.getSalesByCustomer(customerId)

    // ===== Users =====
    suspend fun addUser(user: User): Long = dao.addUser(user)
    suspend fun getUserByUsername(username: String): User? = dao.getUserByUsername(username)
    fun getAllUsers(): Flow<List<User>> = dao.getAllUsers()
    suspend fun deleteUser(userId: Int) = dao.deleteUser(userId)

    // ===== Reports =====
    suspend fun getWeeklyReport(): List<WeeklySalesReport> = dao.getWeeklySalesDetailed()
    suspend fun getMonthlyReport(): List<MonthlySalesReport> = dao.getMonthlySalesReport()
    suspend fun getYearlyReport(): List<YearlySalesReport> = dao.getYearlySalesReport()
}
