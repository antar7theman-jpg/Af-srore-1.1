package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.utils.AppLanguage

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    fun getDisplayName(lang: AppLanguage): String = when (this) {
        LIGHT -> when (lang) {
            AppLanguage.FRENCH -> "Clair"
            AppLanguage.ENGLISH -> "Light"
            AppLanguage.ARABIC -> "فاتح (نهاري)"
        }
        DARK -> when (lang) {
            AppLanguage.FRENCH -> "Sombre"
            AppLanguage.ENGLISH -> "Dark"
            AppLanguage.ARABIC -> "داكن (ليلي)"
        }
        SYSTEM -> when (lang) {
            AppLanguage.FRENCH -> "Système"
            AppLanguage.ENGLISH -> "System"
            AppLanguage.ARABIC -> "تلقائي (حسب النظام)"
        }
    }
}

enum class AppThemePalette(
    val id: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val accentColor: Color,
    val iconEmoji: String,
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme
) {
    // 1. Royal Purple (Classic Antar)
    PURPLE(
        id = "purple",
        primaryColor = Color(0xFF6750A4),
        secondaryColor = Color(0xFF625B71),
        accentColor = Color(0xFF7D5260),
        iconEmoji = "💜",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF6750A4),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFEADDFF),
            onPrimaryContainer = Color(0xFF21005D),
            secondary = Color(0xFF625B71),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFE8DEF8),
            onSecondaryContainer = Color(0xFF1D192B),
            tertiary = Color(0xFF7D5260),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFFD8E4),
            onTertiaryContainer = Color(0xFF31111D),
            background = Color(0xFFFEF7FF),
            onBackground = Color(0xFF1D1B20),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1D1B20),
            surfaceVariant = Color(0xFFF3EDF7),
            onSurfaceVariant = Color(0xFF49454F),
            outline = Color(0xFFCAC4D0),
            outlineVariant = Color(0xFFE7E0EC)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFFD0BCFF),
            onPrimary = Color(0xFF381E72),
            primaryContainer = Color(0xFF4F378B),
            onPrimaryContainer = Color(0xFFEADDFF),
            secondary = Color(0xFFCCC2DC),
            onSecondary = Color(0xFF332D41),
            secondaryContainer = Color(0xFF4A4458),
            onSecondaryContainer = Color(0xFFE8DEF8),
            tertiary = Color(0xFFEFB8C8),
            onTertiary = Color(0xFF492532),
            tertiaryContainer = Color(0xFF633B48),
            onTertiaryContainer = Color(0xFFFFD8E4),
            background = Color(0xFF141218),
            onBackground = Color(0xFFE6E1E5),
            surface = Color(0xFF1D1B20),
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = Color(0xFF49454F),
            onSurfaceVariant = Color(0xFFCAC4D0),
            outline = Color(0xFF938F99),
            outlineVariant = Color(0xFF49454F)
        )
    ),

    // 2. Sapphire Ocean Blue
    BLUE(
        id = "blue",
        primaryColor = Color(0xFF0284C7),
        secondaryColor = Color(0xFF0EA5E9),
        accentColor = Color(0xFF0369A1),
        iconEmoji = "💙",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF0284C7),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE0F2FE),
            onPrimaryContainer = Color(0xFF03446A),
            secondary = Color(0xFF0284C7),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFBAE6FD),
            onSecondaryContainer = Color(0xFF0C4A6E),
            tertiary = Color(0xFF0369A1),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFDBEAFE),
            onTertiaryContainer = Color(0xFF1E3A8A),
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFFCBD5E1),
            outlineVariant = Color(0xFFE2E8F0)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFF7DD3FC),
            onPrimary = Color(0xFF003554),
            primaryContainer = Color(0xFF004D74),
            onPrimaryContainer = Color(0xFFC2E8FF),
            secondary = Color(0xFF38BDF8),
            onSecondary = Color(0xFF00344F),
            secondaryContainer = Color(0xFF0369A1),
            onSecondaryContainer = Color(0xFFE0F2FE),
            tertiary = Color(0xFF93C5FD),
            onTertiary = Color(0xFF1E3A8A),
            tertiaryContainer = Color(0xFF1D4ED8),
            onTertiaryContainer = Color(0xFFDBEAFE),
            background = Color(0xFF0B1320),
            onBackground = Color(0xFFE2E8F0),
            surface = Color(0xFF111C2E),
            onSurface = Color(0xFFE2E8F0),
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF64748B),
            outlineVariant = Color(0xFF334155)
        )
    ),

    // 3. Emerald Green / Smart Market
    EMERALD(
        id = "emerald",
        primaryColor = Color(0xFF059669),
        secondaryColor = Color(0xFF10B981),
        accentColor = Color(0xFF047857),
        iconEmoji = "💚",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF059669),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFD1FAE5),
            onPrimaryContainer = Color(0xFF064E3B),
            secondary = Color(0xFF0D9488),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFCCFBF1),
            onSecondaryContainer = Color(0xFF134E4A),
            tertiary = Color(0xFF047857),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFA7F3D0),
            onTertiaryContainer = Color(0xFF064E3B),
            background = Color(0xFFF7FDF9),
            onBackground = Color(0xFF062B1D),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF062B1D),
            surfaceVariant = Color(0xFFECFDF5),
            onSurfaceVariant = Color(0xFF374151),
            outline = Color(0xFFA7F3D0),
            outlineVariant = Color(0xFFD1FAE5)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFF6EE7B7),
            onPrimary = Color(0xFF003822),
            primaryContainer = Color(0xFF065F46),
            onPrimaryContainer = Color(0xFFA7F3D0),
            secondary = Color(0xFF5EEAD4),
            onSecondary = Color(0xFF003731),
            secondaryContainer = Color(0xFF115E59),
            onSecondaryContainer = Color(0xFFCCFBF1),
            tertiary = Color(0xFFA7F3D0),
            onTertiary = Color(0xFF064E3B),
            tertiaryContainer = Color(0xFF047857),
            onTertiaryContainer = Color(0xFFD1FAE5),
            background = Color(0xFF0A1A12),
            onBackground = Color(0xFFE6F4ED),
            surface = Color(0xFF11261B),
            onSurface = Color(0xFFE6F4ED),
            surfaceVariant = Color(0xFF1A3828),
            onSurfaceVariant = Color(0xFF9CA3AF),
            outline = Color(0xFF4B5563),
            outlineVariant = Color(0xFF1F2937)
        )
    ),

    // 4. Sunset Amber & Gold
    AMBER(
        id = "amber",
        primaryColor = Color(0xFFD97706),
        secondaryColor = Color(0xFFB45309),
        accentColor = Color(0xFFF59E0B),
        iconEmoji = "🧡",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFFD97706),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFEF3C7),
            onPrimaryContainer = Color(0xFF78350F),
            secondary = Color(0xFFB45309),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFDE68A),
            onSecondaryContainer = Color(0xFF451A03),
            tertiary = Color(0xFFCA8A04),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFEF9C3),
            onTertiaryContainer = Color(0xFF713F12),
            background = Color(0xFFFFFBEB),
            onBackground = Color(0xFF291E0A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF291E0A),
            surfaceVariant = Color(0xFFFEF3C7),
            onSurfaceVariant = Color(0xFF57442D),
            outline = Color(0xFFFCD34D),
            outlineVariant = Color(0xFFFDE68A)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFFFBBF24),
            onPrimary = Color(0xFF451A03),
            primaryContainer = Color(0xFF78350F),
            onPrimaryContainer = Color(0xFFFEF3C7),
            secondary = Color(0xFFF59E0B),
            onSecondary = Color(0xFF451A03),
            secondaryContainer = Color(0xFF92400E),
            onSecondaryContainer = Color(0xFFFDE68A),
            tertiary = Color(0xFFFDE047),
            onTertiary = Color(0xFF713F12),
            tertiaryContainer = Color(0xFFA16207),
            onTertiaryContainer = Color(0xFFFEF9C3),
            background = Color(0xFF1C1306),
            onBackground = Color(0xFFFDF6E2),
            surface = Color(0xFF2B1D0B),
            onSurface = Color(0xFFFDF6E2),
            surfaceVariant = Color(0xFF3F2B13),
            onSurfaceVariant = Color(0xFFD4B895),
            outline = Color(0xFF8C6B3E),
            outlineVariant = Color(0xFF4D371A)
        )
    ),

    // 5. Dynamic Crimson Red
    CRIMSON(
        id = "crimson",
        primaryColor = Color(0xFFDC2626),
        secondaryColor = Color(0xFFB91C1C),
        accentColor = Color(0xFFE11D48),
        iconEmoji = "❤️",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFFDC2626),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFEE2E2),
            onPrimaryContainer = Color(0xFF7F1D1D),
            secondary = Color(0xFFBE123C),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFE4E6),
            onSecondaryContainer = Color(0xFF881337),
            tertiary = Color(0xFFB91C1C),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFFECACA),
            onTertiaryContainer = Color(0xFF7F1D1D),
            background = Color(0xFFFEF2F2),
            onBackground = Color(0xFF260D0D),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF260D0D),
            surfaceVariant = Color(0xFFFEE2E2),
            onSurfaceVariant = Color(0xFF593B3B),
            outline = Color(0xFFFCA5A5),
            outlineVariant = Color(0xFFFECACA)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFFF87171),
            onPrimary = Color(0xFF450A0A),
            primaryContainer = Color(0xFF7F1D1D),
            onPrimaryContainer = Color(0xFFFEE2E2),
            secondary = Color(0xFFFB7185),
            onSecondary = Color(0xFF4C0519),
            secondaryContainer = Color(0xFF881337),
            onSecondaryContainer = Color(0xFFFFE4E6),
            tertiary = Color(0xFFFDA4AF),
            onTertiary = Color(0xFF7F1D1D),
            tertiaryContainer = Color(0xFF991B1B),
            onTertiaryContainer = Color(0xFFFECACA),
            background = Color(0xFF1A0A0A),
            onBackground = Color(0xFFFEE2E2),
            surface = Color(0xFF291010),
            onSurface = Color(0xFFFEE2E2),
            surfaceVariant = Color(0xFF421D1D),
            onSurfaceVariant = Color(0xFFD1A8A8),
            outline = Color(0xFF8F4D4D),
            outlineVariant = Color(0xFF542525)
        )
    ),

    // 6. Turquoise Teal / Aqua
    TEAL(
        id = "teal",
        primaryColor = Color(0xFF0D9488),
        secondaryColor = Color(0xFF0891B2),
        accentColor = Color(0xFF14B8A6),
        iconEmoji = "🩵",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF0D9488),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFCCFBF1),
            onPrimaryContainer = Color(0xFF134E4A),
            secondary = Color(0xFF0891B2),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFCFFAFE),
            onSecondaryContainer = Color(0xFF164E63),
            tertiary = Color(0xFF0F766E),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFF99F6E4),
            onTertiaryContainer = Color(0xFF115E59),
            background = Color(0xFFF0FDFA),
            onBackground = Color(0xFF0D2825),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0D2825),
            surfaceVariant = Color(0xFFE6FFFA),
            onSurfaceVariant = Color(0xFF335C58),
            outline = Color(0xFF99F6E4),
            outlineVariant = Color(0xFFCCFBF1)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFF5EEAD4),
            onPrimary = Color(0xFF003731),
            primaryContainer = Color(0xFF115E59),
            onPrimaryContainer = Color(0xFFCCFBF1),
            secondary = Color(0xFF67E8F9),
            onSecondary = Color(0xFF003640),
            secondaryContainer = Color(0xFF155E75),
            onSecondaryContainer = Color(0xFFCFFAFE),
            tertiary = Color(0xFF99F6E4),
            onTertiary = Color(0xFF115E59),
            tertiaryContainer = Color(0xFF0F766E),
            onTertiaryContainer = Color(0xFFE6FFFA),
            background = Color(0xFF081816),
            onBackground = Color(0xFFE6FFFA),
            surface = Color(0xFF0F2623),
            onSurface = Color(0xFFE6FFFA),
            surfaceVariant = Color(0xFF173834),
            onSurfaceVariant = Color(0xFFA5D8D2),
            outline = Color(0xFF4C7B76),
            outlineVariant = Color(0xFF26504C)
        )
    ),

    // 7. Obsidian Midnight / Titanium Dark
    OBSIDIAN(
        id = "obsidian",
        primaryColor = Color(0xFF475569),
        secondaryColor = Color(0xFF334155),
        accentColor = Color(0xFF64748B),
        iconEmoji = "🖤",
        lightColorScheme = lightColorScheme(
            primary = Color(0xFF334155),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF1F5F9),
            onPrimaryContainer = Color(0xFF0F172A),
            secondary = Color(0xFF475569),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFE2E8F0),
            onSecondaryContainer = Color(0xFF1E293B),
            tertiary = Color(0xFF64748B),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFCBD5E1),
            onTertiaryContainer = Color(0xFF0F172A),
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFF94A3B8),
            outlineVariant = Color(0xFFE2E8F0)
        ),
        darkColorScheme = darkColorScheme(
            primary = Color(0xFFCBD5E1),
            onPrimary = Color(0xFF0F172A),
            primaryContainer = Color(0xFF334155),
            onPrimaryContainer = Color(0xFFF8FAFC),
            secondary = Color(0xFF94A3B8),
            onSecondary = Color(0xFF0F172A),
            secondaryContainer = Color(0xFF475569),
            onSecondaryContainer = Color(0xFFF1F5F9),
            tertiary = Color(0xFFE2E8F0),
            onTertiary = Color(0xFF1E293B),
            tertiaryContainer = Color(0xFF64748B),
            onTertiaryContainer = Color(0xFFF8FAFC),
            background = Color(0xFF090D14),
            onBackground = Color(0xFFF8FAFC),
            surface = Color(0xFF111827),
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFF1F2937),
            onSurfaceVariant = Color(0xFF9CA3AF),
            outline = Color(0xFF6B7280),
            outlineVariant = Color(0xFF374151)
        )
    );

    fun getDisplayName(lang: AppLanguage): String = when (this) {
        PURPLE -> when (lang) {
            AppLanguage.FRENCH -> "Violet Royal"
            AppLanguage.ENGLISH -> "Royal Purple"
            AppLanguage.ARABIC -> "البنفسجي الملكي (الكلاسيكي)"
        }
        BLUE -> when (lang) {
            AppLanguage.FRENCH -> "Bleu Saphir"
            AppLanguage.ENGLISH -> "Sapphire Blue"
            AppLanguage.ARABIC -> "الأزرق الياقوتي (محيطي)"
        }
        EMERALD -> when (lang) {
            AppLanguage.FRENCH -> "Vert Émeraude"
            AppLanguage.ENGLISH -> "Emerald Green"
            AppLanguage.ARABIC -> "الأخضر الزمردي (متجر ذكي)"
        }
        AMBER -> when (lang) {
            AppLanguage.FRENCH -> "Ambre Doré"
            AppLanguage.ENGLISH -> "Amber Gold"
            AppLanguage.ARABIC -> "العنبر الذهبي (غروب فاخر)"
        }
        CRIMSON -> when (lang) {
            AppLanguage.FRENCH -> "Rouge Cramoisi"
            AppLanguage.ENGLISH -> "Crimson Ruby"
            AppLanguage.ARABIC -> "الأحمر القرمزي (طاقة وحيوية)"
        }
        TEAL -> when (lang) {
            AppLanguage.FRENCH -> "Turquoise Océan"
            AppLanguage.ENGLISH -> "Ocean Teal"
            AppLanguage.ARABIC -> "التركواز الفيروزي (انتعاش)"
        }
        OBSIDIAN -> when (lang) {
            AppLanguage.FRENCH -> "Obsidienne Minuit"
            AppLanguage.ENGLISH -> "Obsidian Midnight"
            AppLanguage.ARABIC -> "أوبسيديان الليلي (فاحم تباين)"
        }
    }
}
