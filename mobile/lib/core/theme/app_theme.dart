import 'package:flutter/material.dart';
import 'app_tokens.dart';

/// Xây dựng `ThemeData` Light & Dark cho Hướng A — Calm Botanical,
/// tối ưu chiều cao dòng (`height: 1.4`–`1.5`) để không cắt dấu phụ tiếng Việt (`ẫ, ệ, ử, ợ`).
class LifeOsTheme {
  LifeOsTheme._();

  static ThemeData light() => _build(
        brightness: Brightness.light,
        tokens: LifeOsTokens.light,
      );

  static ThemeData dark() => _build(
        brightness: Brightness.dark,
        tokens: LifeOsTokens.dark,
      );

  static ThemeData buildWithTokens({
    required Brightness brightness,
    required LifeOsTokens tokens,
  }) =>
      _build(
        brightness: brightness,
        tokens: tokens,
      );

  static ThemeData _build({
    required Brightness brightness,
    required LifeOsTokens tokens,
  }) {
    final bool isDark = brightness == Brightness.dark;
    final ColorScheme colorScheme = ColorScheme(
      brightness: brightness,
      primary: tokens.accentPrimary,
      onPrimary: isDark ? const Color(0xFF0A1410) : Colors.white,
      secondary: tokens.accentSecondary,
      onSecondary: const Color(0xFF141A17),
      secondaryContainer:
          isDark ? const Color(0xFF1D2E27) : const Color(0xFFE5EFEA),
      onSecondaryContainer:
          isDark ? const Color(0xFF6EE7B7) : tokens.accentPrimary,
      error: tokens.danger,
      onError: Colors.white,
      surface: tokens.bgSurface,
      onSurface: tokens.textPrimary,
      surfaceContainerHighest: tokens.bgElevated,
      outline: tokens.borderSubtle,
    );

    final TextTheme textTheme = TextTheme(
      displaySmall: TextStyle(
        fontSize: 28,
        fontWeight: FontWeight.w700,
        height: 1.35,
        color: tokens.textPrimary,
      ),
      headlineMedium: TextStyle(
        fontSize: 22,
        fontWeight: FontWeight.w700,
        height: 1.35,
        color: tokens.textPrimary,
      ),
      titleLarge: TextStyle(
        fontSize: 18,
        fontWeight: FontWeight.w700,
        height: 1.4,
        color: tokens.textPrimary,
      ),
      titleMedium: TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w600,
        height: 1.4,
        color: tokens.textPrimary,
      ),
      bodyLarge: TextStyle(
        fontSize: 15,
        fontWeight: FontWeight.w400,
        height: 1.5,
        color: tokens.textPrimary,
      ),
      bodyMedium: TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.w400,
        height: 1.45,
        color: tokens.textPrimary,
      ),
      labelLarge: TextStyle(
        fontSize: 14,
        fontWeight: FontWeight.w600,
        height: 1.4,
        color: tokens.textPrimary,
      ),
      labelMedium: TextStyle(
        fontSize: 12.5,
        fontWeight: FontWeight.w600,
        height: 1.35,
        color: tokens.textPrimary,
      ),
      labelSmall: TextStyle(
        fontSize: 11.5,
        fontWeight: FontWeight.w500,
        height: 1.35,
        color: tokens.textSecondary,
      ),
      bodySmall: TextStyle(
        fontSize: 12.5,
        fontWeight: FontWeight.w500,
        height: 1.4,
        color: tokens.textSecondary,
      ),
    ).apply(
      fontFamily: 'LifeOsSans',
      fontFamilyFallback: const <String>['Roboto', 'Segoe UI', 'sans-serif'],
    );

    return ThemeData(
      useMaterial3: true,
      fontFamily: 'LifeOsSans',
      brightness: brightness,
      scaffoldBackgroundColor: tokens.bgPrimary,
      colorScheme: colorScheme,
      textTheme: textTheme,
      extensions: <ThemeExtension<dynamic>>[tokens],
      floatingActionButtonTheme: FloatingActionButtonThemeData(
        backgroundColor: tokens.accentPrimary,
        foregroundColor: isDark ? const Color(0xFF0A1410) : Colors.white,
        elevation: 0,
        focusElevation: 0,
        hoverElevation: 0,
        highlightElevation: 0,
        disabledElevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
          side: BorderSide.none,
        ),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: tokens.bgPrimary,
        foregroundColor: tokens.textPrimary,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        titleTextStyle: textTheme.titleLarge,
      ),
      cardTheme: CardThemeData(
        color: tokens.bgSurface,
        elevation: isDark ? 0 : 1,
        shadowColor: Colors.black.withValues(alpha: 0.06),
        margin: EdgeInsets.zero,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(LifeOsTokens.radiusLg),
          side: BorderSide(color: tokens.borderSubtle, width: 1),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: tokens.bgSurface,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: LifeOsTokens.space4,
          vertical: LifeOsTokens.space3,
        ),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
          borderSide: BorderSide(color: tokens.borderSubtle),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
          borderSide: BorderSide(color: tokens.borderSubtle),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
          borderSide: BorderSide(color: tokens.accentPrimary, width: 1.8),
        ),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          minimumSize: const Size(
            LifeOsTokens.minTouchTarget,
            LifeOsTokens.minTouchTarget,
          ),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(
            LifeOsTokens.minTouchTarget,
            LifeOsTokens.minTouchTarget,
          ),
          side: BorderSide(color: tokens.borderSubtle),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
          ),
        ),
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: tokens.bgSurface,
        indicatorColor: tokens.accentPrimary.withValues(alpha: 0.16),
        height: 68,
        iconTheme: WidgetStateProperty.resolveWith((Set<WidgetState> states) {
          final bool selected = states.contains(WidgetState.selected);
          return IconThemeData(
            color: selected ? tokens.accentPrimary : tokens.textSecondary,
          );
        }),
        labelTextStyle: WidgetStateProperty.resolveWith((Set<WidgetState> states) {
          final bool selected = states.contains(WidgetState.selected);
          return TextStyle(
            fontFamily: 'LifeOsSans',
            fontSize: 12,
            fontWeight: selected ? FontWeight.w700 : FontWeight.w600,
            color: selected ? tokens.accentPrimary : tokens.textPrimary,
          );
        }),
      ),
      navigationRailTheme: NavigationRailThemeData(
        backgroundColor: tokens.bgSurface,
        indicatorColor: tokens.accentPrimary.withValues(alpha: 0.16),
        selectedIconTheme: IconThemeData(color: tokens.accentPrimary),
        unselectedIconTheme: IconThemeData(color: tokens.textSecondary),
        selectedLabelTextStyle: TextStyle(
          fontFamily: 'LifeOsSans',
          fontSize: 12.5,
          fontWeight: FontWeight.w700,
          color: tokens.accentPrimary,
        ),
        unselectedLabelTextStyle: TextStyle(
          fontFamily: 'LifeOsSans',
          fontSize: 12,
          fontWeight: FontWeight.w500,
          color: tokens.textSecondary,
        ),
      ),
      dividerTheme: DividerThemeData(
        color: tokens.borderSubtle,
        thickness: 1,
        space: 1,
      ),
    );
  }
}

