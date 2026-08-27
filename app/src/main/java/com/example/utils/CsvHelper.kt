package com.example.utils

import android.content.Context
import android.net.Uri
import com.example.data.models.Customer
import com.example.data.models.Product
import com.example.data.models.Sale
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvHelper {

    fun generateProductsCsv(products: List<Product>): String {
        val sb = StringBuilder()
        sb.append("id,name,purchasePrice,price,stock,unitsPerCarton,barcode,cartonBarcode,isDeleted\n")
        for (p in products) {
            val safeName = p.name.replace("\"", "\"\"")
            val barcode = p.barcode ?: ""
            val cartonBarcode = p.cartonBarcode ?: ""
            sb.append("${p.id},\"$safeName\",${p.purchasePrice},${p.price},${p.stock},${p.unitsPerCarton},\"$barcode\",\"$cartonBarcode\",${p.isDeleted}\n")
        }
        return sb.toString()
    }

    fun parseProductsCsv(csvContent: String): List<Product> {
        val result = mutableListOf<Product>()
        val lines = csvContent.lines()
        for ((index, line) in lines.withIndex()) {
            if (index == 0 || line.isBlank()) continue // skip header
            try {
                val tokens = parseCsvLine(line)
                if (tokens.size >= 4) {
                    val name = tokens[1].trim()
                    val (purchasePrice, price, stock, unitsPerCarton, barcode, cartonBarcode, isDeleted) = if (tokens.size >= 9) {
                        // 9-column format: id, name, purchasePrice, price, stock, unitsPerCarton, barcode, cartonBarcode, isDeleted
                        val cost = tokens[2].trim().toDoubleOrNull() ?: 0.0
                        val sell = tokens[3].trim().toDoubleOrNull() ?: 0.0
                        val stk = tokens[4].trim().toIntOrNull() ?: 0
                        val upc = tokens[5].trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val bc = if (tokens[6].isNotBlank()) tokens[6].trim() else null
                        val cbc = if (tokens[7].isNotBlank()) tokens[7].trim() else null
                        val del = tokens[8].trim().toBoolean()
                        Tuple7(cost, sell, stk, upc, bc, cbc, del)
                    } else if (tokens.size >= 8) {
                        // 8-column format: id, name, purchasePrice, price, stock, unitsPerCarton, barcode, isDeleted
                        val cost = tokens[2].trim().toDoubleOrNull() ?: 0.0
                        val sell = tokens[3].trim().toDoubleOrNull() ?: 0.0
                        val stk = tokens[4].trim().toIntOrNull() ?: 0
                        val upc = tokens[5].trim().toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val bc = if (tokens[6].isNotBlank()) tokens[6].trim() else null
                        val del = tokens[7].trim().toBoolean()
                        Tuple7(cost, sell, stk, upc, bc, null, del)
                    } else if (tokens.size >= 7) {
                        // 7-column format: id, name, purchasePrice, price, stock, barcode, isDeleted
                        val cost = tokens[2].trim().toDoubleOrNull() ?: 0.0
                        val sell = tokens[3].trim().toDoubleOrNull() ?: 0.0
                        val stk = tokens[4].trim().toIntOrNull() ?: 0
                        val bc = if (tokens[5].isNotBlank()) tokens[5].trim() else null
                        val del = tokens[6].trim().toBoolean()
                        Tuple7(cost, sell, stk, 1, bc, null, del)
                    } else {
                        // Older 6-column format: id, name, price, stock, barcode, isDeleted
                        val sell = tokens[2].trim().toDoubleOrNull() ?: 0.0
                        val stk = tokens[3].trim().toIntOrNull() ?: 0
                        val bc = if (tokens.size > 4 && tokens[4].isNotBlank()) tokens[4].trim() else null
                        val del = if (tokens.size > 5) tokens[5].trim().toBoolean() else false
                        Tuple7(0.0, sell, stk, 1, bc, null, del)
                    }

                    if (name.isNotEmpty()) {
                        result.add(
                            Product(
                                id = 0, // new auto-generated
                                name = name,
                                purchasePrice = purchasePrice,
                                price = price,
                                stock = stock,
                                unitsPerCarton = unitsPerCarton,
                                barcode = barcode,
                                cartonBarcode = cartonBarcode,
                                image = null,
                                isDeleted = isDeleted
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                // ignore malformed line
            }
        }
        return result
    }

    private data class Tuple7(
        val purchasePrice: Double,
        val price: Double,
        val stock: Int,
        val unitsPerCarton: Int,
        val barcode: String?,
        val cartonBarcode: String?,
        val isDeleted: Boolean
    )

    fun generateSalesCsv(sales: List<Sale>): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sb.append("saleId,productId,productName,quantitySold,totalPrice,saleDate,customerName,paymentMethod\n")
        for (s in sales) {
            val safeName = s.productName.replace("\"", "\"\"")
            val safeCust = (s.customerName ?: "").replace("\"", "\"\"")
            val dateStr = dateFormat.format(Date(s.saleDate))
            sb.append("${s.saleId},${s.productId},\"$safeName\",${s.quantitySold},${s.totalPrice},\"$dateStr\",\"$safeCust\",${s.paymentMethod}\n")
        }
        return sb.toString()
    }

    fun generateCustomersCsv(customers: List<Customer>): String {
        val sb = StringBuilder()
        sb.append("id,name,phone,email,address,notes,balanceDebt,totalPurchases\n")
        for (c in customers) {
            val safeName = c.name.replace("\"", "\"\"")
            val safePhone = c.phone.replace("\"", "\"\"")
            val safeEmail = c.email.replace("\"", "\"\"")
            val safeAddr = c.address.replace("\"", "\"\"")
            val safeNotes = c.notes.replace("\"", "\"\"")
            sb.append("${c.id},\"$safeName\",\"$safePhone\",\"$safeEmail\",\"$safeAddr\",\"$safeNotes\",${c.balanceDebt},${c.totalPurchases}\n")
        }
        return sb.toString()
    }

    fun parseCustomersCsv(csvContent: String): List<Customer> {
        val result = mutableListOf<Customer>()
        val lines = csvContent.lines()
        for ((index, line) in lines.withIndex()) {
            if (index == 0 || line.isBlank()) continue
            try {
                val tokens = parseCsvLine(line)
                if (tokens.size >= 2) {
                    val name = tokens[1].trim()
                    val phone = if (tokens.size > 2) tokens[2].trim() else ""
                    val email = if (tokens.size > 3 && (tokens[3].contains("@") || tokens.size >= 7)) tokens[3].trim() else ""
                    val address = if (tokens.size > 4) tokens[4].trim() else if (tokens.size > 3 && !tokens[3].contains("@")) tokens[3].trim() else ""
                    val notes = if (tokens.size > 5) tokens[5].trim() else if (tokens.size > 4 && !tokens[4].toDoubleOrNull().isNumeric()) tokens[4].trim() else ""
                    val debtIndex = if (tokens.size >= 8) 6 else if (tokens.size >= 7) 5 else 4
                    val purchasesIndex = debtIndex + 1
                    val debt = if (tokens.size > debtIndex) tokens[debtIndex].trim().toDoubleOrNull() ?: 0.0 else 0.0
                    val purchases = if (tokens.size > purchasesIndex) tokens[purchasesIndex].trim().toDoubleOrNull() ?: 0.0 else 0.0
                    if (name.isNotEmpty()) {
                        result.add(
                            Customer(
                                id = 0,
                                name = name,
                                phone = phone,
                                email = email,
                                address = address,
                                notes = notes,
                                balanceDebt = debt,
                                totalPurchases = purchases
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }
        return result
    }

    private fun Double?.isNumeric(): Boolean = this != null

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var sb = java.lang.StringBuilder()
        var inQuotes = false
        for (i in line.indices) {
            val c = line[i]
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString())
                sb = java.lang.StringBuilder()
            } else {
                sb.append(c)
            }
        }
        tokens.add(sb.toString())
        return tokens
    }

    fun readUriContent(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).readText()
        } ?: ""
    }

    fun writeStringToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
