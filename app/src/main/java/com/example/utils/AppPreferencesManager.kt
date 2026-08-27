package com.example.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.models.InvoiceStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "antar_store_settings")

class AppPreferencesManager(private val context: Context) {

    companion object {
        val KEY_INVOICE_STYLE = stringPreferencesKey("key_invoice_style")
        val KEY_THERMAL_PAPER_WIDTH = stringPreferencesKey("key_thermal_paper_width") // "58" or "80"
        val KEY_STORE_NAME = stringPreferencesKey("key_store_name")
        val KEY_STORE_PHONE = stringPreferencesKey("key_store_phone")
        val KEY_STORE_FOOTER_NOTE = stringPreferencesKey("key_store_footer_note")
        val KEY_CURRENCY_SYMBOL = stringPreferencesKey("key_currency_symbol")
        val KEY_LOW_STOCK_ALERTS = booleanPreferencesKey("key_low_stock_alerts")
        val KEY_LOW_STOCK_THRESHOLD = intPreferencesKey("key_low_stock_threshold")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("key_is_logged_in")
        val KEY_LOGGED_IN_USERNAME = stringPreferencesKey("key_logged_in_username")
        val KEY_APP_LANGUAGE = stringPreferencesKey("key_app_language")
        val KEY_THEME_PALETTE = stringPreferencesKey("key_theme_palette")
        val KEY_THEME_MODE = stringPreferencesKey("key_theme_mode")
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_LOGGED_IN] ?: false
    }

    val loggedInUsernameFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_LOGGED_IN_USERNAME]
    }

    val appLanguageFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_APP_LANGUAGE]
    }

    val themePaletteFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_PALETTE]
    }

    val themeModeFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE]
    }

    val invoiceStyleFlow: Flow<InvoiceStyle> = context.dataStore.data.map { preferences ->
        val styleId = preferences[KEY_INVOICE_STYLE]
        InvoiceStyle.fromId(styleId)
    }

    val thermalPaperWidthFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THERMAL_PAPER_WIDTH] ?: "80"
    }

    val storeNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_STORE_NAME] ?: "AF store"
    }

    val storePhoneFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_STORE_PHONE] ?: "01012345678"
    }

    val storeFooterNoteFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_STORE_FOOTER_NOTE] ?: "شكراً لتعاملكم معنا ونرحب بزيارتكم دائماً!"
    }

    val currencySymbolFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CURRENCY_SYMBOL] ?: "ج.م"
    }

    val lowStockAlertsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_LOW_STOCK_ALERTS] ?: true
    }

    val lowStockThresholdFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_LOW_STOCK_THRESHOLD] ?: 5
    }

    suspend fun setInvoiceStyle(style: InvoiceStyle) {
        context.dataStore.edit { preferences ->
            preferences[KEY_INVOICE_STYLE] = style.id
        }
    }

    suspend fun setThermalPaperWidth(width: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THERMAL_PAPER_WIDTH] = width
        }
    }

    suspend fun setStoreName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STORE_NAME] = name
        }
    }

    suspend fun setStorePhone(phone: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STORE_PHONE] = phone
        }
    }

    suspend fun setStoreFooterNote(note: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STORE_FOOTER_NOTE] = note
        }
    }

    suspend fun setCurrencySymbol(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CURRENCY_SYMBOL] = currency
        }
    }

    suspend fun setLowStockAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LOW_STOCK_ALERTS] = enabled
        }
    }

    suspend fun setLowStockThreshold(threshold: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LOW_STOCK_THRESHOLD] = threshold
        }
    }

    suspend fun saveUserSession(username: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = true
            preferences[KEY_LOGGED_IN_USERNAME] = username
        }
    }

    suspend fun clearUserSession() {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = false
            preferences.remove(KEY_LOGGED_IN_USERNAME)
        }
    }

    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_LANGUAGE] = languageCode
        }
    }

    suspend fun setThemePalette(paletteName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_PALETTE] = paletteName
        }
    }

    suspend fun setThemeMode(modeName: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = modeName
        }
    }
}
