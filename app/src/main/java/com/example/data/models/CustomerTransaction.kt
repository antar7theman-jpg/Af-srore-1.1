package com.example.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customer_transactions",
    indices = [
        Index("customerId"),
        Index("date")
    ]
)
data class CustomerTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerId: Int,
    val customerName: String,
    val type: String, // "SALE_DEBT" (دين مبيعات), "PAYMENT" (سداد دفعة), "MANUAL_DEBT" (إضافة دين يدوي)
    val amount: Double,
    val notes: String = "",
    val date: Long = System.currentTimeMillis()
)
