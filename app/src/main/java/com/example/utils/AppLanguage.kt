package com.example.utils

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val symbol: String,
    val flag: String,
    val isRtl: Boolean
) {
    ARABIC("ar", "العربية", "العربية", "AR", "🇪🇬", true),
    FRENCH("fr", "Français", "Français", "FR", "🇫🇷", false),
    ENGLISH("en", "English", "English", "EN", "🇬🇧", false);

    val flagEmoji: String get() = flag
    val displayCode: String get() = code.lowercase()
    val uppercaseCode: String get() = code.uppercase()
}
