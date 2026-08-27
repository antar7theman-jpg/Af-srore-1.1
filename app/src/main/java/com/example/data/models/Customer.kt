package com.example.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index("name"),
        Index("phone"),
        Index("email"),
        Index("balanceDebt"),
        Index("isFavorite")
    ]
)
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val balanceDebt: Double = 0.0, // Remaining balance owed by customer
    val totalPurchases: Double = 0.0, // Lifetime purchase sum
    val createdAt: Long = System.currentTimeMillis()
)

