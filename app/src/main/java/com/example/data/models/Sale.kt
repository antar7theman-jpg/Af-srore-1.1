package com.example.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    indices = [
        Index("saleDate"),
        Index("customerId"),
        Index("productId")
    ]
)
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val saleId: Int = 0,
    val productId: Int,
    val productName: String,
    val quantitySold: Int,
    val totalPrice: Double,
    val saleDate: Long = System.currentTimeMillis(),
    val customerId: Int? = null,
    val customerName: String? = null,
    val isDebt: Boolean = false,
    val paymentMethod: String = "CASH" // "CASH", "DEBT", "PARTIAL"
)
