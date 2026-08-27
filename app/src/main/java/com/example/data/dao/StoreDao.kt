package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.models.Customer
import com.example.data.models.CustomerTransaction
import com.example.data.models.Product
import com.example.data.models.Sale
import com.example.data.models.User
import kotlinx.coroutines.flow.Flow

data class WeeklySalesReport(
    val day: String,
    val total: Double,
    val invoicesCount: Int = 0
)

data class MonthlySalesReport(
    val month: String,
    val total: Double,
    val invoicesCount: Int = 0
)

data class YearlySalesReport(
    val year: String,
    val total: Double,
    val invoicesCount: Int = 0
)

@Dao
interface StoreDao {

    // ===== Products =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("UPDATE products SET purchasePrice = :purchasePrice, price = :price WHERE id = :productId")
    suspend fun updateProductPrices(productId: Int, purchasePrice: Double, price: Double)

    @Query("UPDATE products SET stock = :stock, purchasePrice = :purchasePrice, price = :price WHERE id = :productId")
    suspend fun updateProductStockAndPrices(productId: Int, stock: Int, purchasePrice: Double, price: Double)

    @Query("UPDATE products SET purchasePrice = :purchasePrice WHERE id = :productId")
    suspend fun updateProductPurchasePrice(productId: Int, purchasePrice: Double)

    @Query("UPDATE products SET price = :price WHERE id = :productId")
    suspend fun updateProductPrice(productId: Int, price: Double)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun permanentDeleteProduct(id: Int)

    @Query("UPDATE products SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteProduct(id: Int)

    @Query("UPDATE products SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreProduct(id: Int)

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE isDeleted = 1 ORDER BY name ASC")
    fun getDeletedProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): Product?

    @Query("SELECT * FROM products WHERE (barcode = :barcode OR cartonBarcode = :barcode) AND isDeleted = 0 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE cartonBarcode = :cartonBarcode AND isDeleted = 0 LIMIT 1")
    suspend fun getProductByCartonBarcode(cartonBarcode: String): Product?

    @Query("SELECT * FROM products WHERE barcode = :unitBarcode AND isDeleted = 0 LIMIT 1")
    suspend fun getProductByUnitBarcode(unitBarcode: String): Product?

    @Query("SELECT * FROM products") // For full export/backup
    suspend fun getAllProductsList(): List<Product>

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Query("UPDATE products SET stock = stock - :qty WHERE id = :productId AND stock >= :qty AND isDeleted = 0")
    suspend fun reduceStock(productId: Int, qty: Int)

    @Query("UPDATE products SET stock = stock + :qty WHERE id = :productId")
    suspend fun increaseStock(productId: Int, qty: Int)

    @Query("SELECT * FROM products WHERE isDeleted = 0 AND (name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' OR cartonBarcode LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<Product>>

    // ===== Sales =====
    @Insert
    suspend fun recordSale(sale: Sale): Long

    @Query("SELECT COALESCE(SUM(totalPrice), 0.0) FROM sales WHERE DATE(saleDate / 1000, 'unixepoch', 'localtime') = DATE('now', 'localtime')")
    suspend fun getTodayTotalSales(): Double

    @Query("SELECT * FROM sales WHERE DATE(saleDate / 1000, 'unixepoch', 'localtime') = DATE('now', 'localtime') ORDER BY saleId DESC")
    fun getTodaySales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY saleId DESC LIMIT 100")
    fun getAllRecentSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY saleDate DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE saleDate >= :startTime AND saleDate <= :endTime ORDER BY saleDate DESC")
    fun getSalesBetweenDates(startTime: Long, endTime: Long): Flow<List<Sale>>

    @Query("SELECT * FROM sales")
    suspend fun getAllSalesList(): List<Sale>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSales(sales: List<Sale>)

    @Query("DELETE FROM sales")
    suspend fun deleteAllSales()

    // ===== Reports =====
    @Query("""
        SELECT DATE(saleDate / 1000, 'unixepoch', 'localtime') as day,
               COALESCE(SUM(totalPrice), 0.0) as total,
               COUNT(*) as invoicesCount
        FROM sales
        WHERE saleDate >= (strftime('%s', 'now', '-6 days', 'localtime') * 1000)
        GROUP BY day ORDER BY day ASC
    """)
    suspend fun getWeeklySalesDetailed(): List<WeeklySalesReport>

    @Query("""
        SELECT strftime('%Y-%m', saleDate/1000, 'unixepoch', 'localtime') as month,
               COALESCE(SUM(totalPrice), 0.0) as total,
               COUNT(*) as invoicesCount
        FROM sales
        WHERE saleDate >= (strftime('%s', 'now', '-11 months', 'localtime') * 1000)
        GROUP BY month ORDER BY month ASC
    """)
    suspend fun getMonthlySalesReport(): List<MonthlySalesReport>

    @Query("""
        SELECT strftime('%Y', saleDate/1000, 'unixepoch', 'localtime') as year,
               COALESCE(SUM(totalPrice), 0.0) as total,
               COUNT(*) as invoicesCount
        FROM sales
        GROUP BY year ORDER BY year ASC
    """)
    suspend fun getYearlySalesReport(): List<YearlySalesReport>

    // ===== Users =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addUser(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Int)

    // ===== Customers =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: Int)

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Int): Customer?

    @Query("SELECT * FROM customers ORDER BY isFavorite DESC, name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE balanceDebt > 0 ORDER BY balanceDebt DESC")
    fun getCustomersWithDebt(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' ORDER BY isFavorite DESC, name ASC")
    fun searchCustomers(query: String): Flow<List<Customer>>

    @Query("UPDATE customers SET isFavorite = :isFavorite WHERE id = :customerId")
    suspend fun setCustomerFavorite(customerId: Int, isFavorite: Boolean)

    @Query("UPDATE customers SET balanceDebt = balanceDebt + :debtChange, totalPurchases = totalPurchases + :purchasesChange WHERE id = :customerId")
    suspend fun updateCustomerBalances(customerId: Int, debtChange: Double, purchasesChange: Double)

    @Query("UPDATE customers SET balanceDebt = MAX(0.0, balanceDebt - :paymentAmount) WHERE id = :customerId")
    suspend fun payCustomerDebt(customerId: Int, paymentAmount: Double)

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersList(): List<Customer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()

    // ===== Customer Transactions =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerTransaction(tx: CustomerTransaction): Long

    @Query("SELECT * FROM customer_transactions WHERE customerId = :customerId ORDER BY date DESC")
    fun getCustomerTransactions(customerId: Int): Flow<List<CustomerTransaction>>

    @Query("SELECT * FROM customer_transactions ORDER BY date DESC LIMIT 100")
    fun getAllRecentCustomerTransactions(): Flow<List<CustomerTransaction>>

    @Query("DELETE FROM customer_transactions WHERE customerId = :customerId")
    suspend fun deleteCustomerTransactions(customerId: Int)

    @Query("DELETE FROM customer_transactions")
    suspend fun deleteAllCustomerTransactions()

    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY saleDate DESC")
    fun getSalesByCustomer(customerId: Int): Flow<List<Sale>>
}
