package com.example.data.models

import com.example.utils.AppLanguage

/**
 * Supported invoice layout templates
 */
enum class InvoiceStyle(
    val id: String,
    val iconEmoji: String
) {
    DETAILED("DETAILED", "📋"),
    SIMPLE("SIMPLE", "📄"),
    THERMAL_POS("THERMAL_POS", "🖨️");

    fun getDisplayName(language: AppLanguage): String {
        return when (language) {
            AppLanguage.FRENCH -> when (this) {
                DETAILED -> "Modèle Détaillé"
                SIMPLE -> "Modèle Simple"
                THERMAL_POS -> "Reçu Thermique POS"
            }
            AppLanguage.ENGLISH -> when (this) {
                DETAILED -> "Detailed Invoice"
                SIMPLE -> "Simple Invoice"
                THERMAL_POS -> "Thermal POS Receipt"
            }
            AppLanguage.ARABIC -> when (this) {
                DETAILED -> "نموذج مبيعات مفصل"
                SIMPLE -> "نموذج مبيعات بسيط"
                THERMAL_POS -> "نموذج إيصال حراري (POS)"
            }
        }
    }

    fun getDescription(language: AppLanguage): String {
        return when (language) {
            AppLanguage.FRENCH -> when (this) {
                DETAILED -> "Tableau complet avec prix unitaire, détails cartons/unités, codes-barres et totaux détaillés."
                SIMPLE -> "Mise en page épurée et concise des articles et du montant total sans colonnes superflues."
                THERMAL_POS -> "Format optimisé pour rouleaux thermiques 58mm/80mm (ESC/POS) avec séparateurs en pointillés."
            }
            AppLanguage.ENGLISH -> when (this) {
                DETAILED -> "Full itemized table with unit price, carton/loose units breakdown, barcodes, and subtotal taxes."
                SIMPLE -> "Clean, minimalist layout focusing purely on item names, quantities, and final amount."
                THERMAL_POS -> "Optimized for 58mm/80mm thermal receipt rolls with dashed dividers and monospace alignment."
            }
            AppLanguage.ARABIC -> when (this) {
                DETAILED -> "جدول كامل يحتوي على السعر الفردي، تفصيل العبوات والقطع، الباركود، والخصومات والمجاميع."
                SIMPLE -> "تصميم مبسط ومركّز يعرض أسماء الأصناف والكميات والإجمالي النهائي بدون جداول معقدة."
                THERMAL_POS -> "نمط مخصص لطابعات البلوتوث والرول الحراري (58مم / 80مم) مع فواصل شرطية وتنسيق مدمج."
            }
        }
    }

    companion object {
        fun fromId(id: String?): InvoiceStyle {
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DETAILED
        }
    }
}
