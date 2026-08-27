package com.example.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index("barcode"),
        Index("cartonBarcode"),
        Index("name"),
        Index("isDeleted")
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    var purchasePrice: Double = 0.0, // سعر الشراء للوحدة
    var price: Double = 0.0,         // سعر البيع للوحدة
    var stock: Int = 0,              // إجمالي الوحدات في المخزن
    var unitsPerCarton: Int = 1,     // عدد الوحدات في العبوة (مثلاً 6 أو 12)
    val barcode: String? = null,     // باركود القطعة / الوحدة الفردية
    val cartonBarcode: String? = null, // باركود العبوة الكبرى بالكامل
    val image: ByteArray? = null,
    val imagePath: String? = null,    // مسار حفظ الصورة في ذاكرة الهاتف
    var isDeleted: Boolean = false
) {
    val costPrice: Double
        get() = purchasePrice

    val profitPerUnit: Double
        get() = price - purchasePrice

    val profitMarginPercent: Double
        get() = if (purchasePrice > 0) ((price - purchasePrice) / purchasePrice) * 100.0 else 0.0

    // حسابات العبوة
    val cartonCount: Int
        get() = if (unitsPerCarton > 0) stock / unitsPerCarton else 0

    val remainingLooseUnits: Int
        get() = if (unitsPerCarton > 0) stock % unitsPerCarton else stock

    val cartonPurchasePrice: Double
        get() = purchasePrice * unitsPerCarton

    val cartonSellingPrice: Double
        get() = price * unitsPerCarton

    val cartonProfit: Double
        get() = profitPerUnit * unitsPerCarton

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Product

        if (id != other.id) return false
        if (name != other.name) return false
        if (purchasePrice != other.purchasePrice) return false
        if (price != other.price) return false
        if (stock != other.stock) return false
        if (unitsPerCarton != other.unitsPerCarton) return false
        if (barcode != other.barcode) return false
        if (cartonBarcode != other.cartonBarcode) return false
        if (imagePath != other.imagePath) return false
        if (isDeleted != other.isDeleted) return false
        if (image != null) {
            if (other.image == null) return false
            if (!image.contentEquals(other.image)) return false
        } else if (other.image != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + purchasePrice.hashCode()
        result = 31 * result + price.hashCode()
        result = 31 * result + stock
        result = 31 * result + unitsPerCarton
        result = 31 * result + (barcode?.hashCode() ?: 0)
        result = 31 * result + (cartonBarcode?.hashCode() ?: 0)
        result = 31 * result + (imagePath?.hashCode() ?: 0)
        result = 31 * result + (image?.contentHashCode() ?: 0)
        result = 31 * result + isDeleted.hashCode()
        return result
    }
}

