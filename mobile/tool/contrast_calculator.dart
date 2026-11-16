// ignore_for_file: avoid_print, prefer_const_constructors
import 'dart:math' as math;

class WcagColor {
  const WcagColor(this.hex, this.name);
  final String hex;
  final String name;

  int get value => int.parse(hex.replaceFirst('#', ''), radix: 16);
  int get r => (value >> 16) & 0xFF;
  int get g => (value >> 8) & 0xFF;
  int get b => value & 0xFF;

  double get luminance {
    double toLinear(int c) {
      final double s = c / 255.0;
      return s <= 0.04045 ? s / 12.92 : math.pow((s + 0.055) / 1.055, 2.4).toDouble();
    }

    return 0.2126 * toLinear(r) + 0.7152 * toLinear(g) + 0.0722 * toLinear(b);
  }

  static double ratio(WcagColor fg, WcagColor bg) {
    final double l1 = fg.luminance;
    final double l2 = bg.luminance;
    final double lighter = math.max(l1, l2);
    final double darker = math.min(l1, l2);
    return (lighter + 0.05) / (darker + 0.05);
  }
}

class ContrastPair {
  const ContrastPair({
    required this.direction,
    required this.mode,
    required this.component,
    required this.fg,
    required this.bg,
    required this.minRatio,
    this.note = '',
  });

  final String direction;
  final String mode;
  final String component;
  final WcagColor fg;
  final WcagColor bg;
  final double minRatio;
  final String note;

  double get ratio => WcagColor.ratio(fg, bg);
  bool get passes => ratio >= minRatio;
}

void main() {
  print('=== WCAG 2.x Contrast Verification Script ===\n');

  // Verify User Prompt Discrepancies first
  print('--- 1. Verification of User Test Cases ---');
  final List<ContrastPair> verifyCases = <ContrastPair>[
    ContrastPair(
      direction: 'A (cũ)',
      mode: 'Light',
      component: 'Hổ phách cũ trên nền kem',
      fg: const WcagColor('#C8681A', 'Hổ phách v1'),
      bg: const WcagColor('#F8F7F3', 'Nền kem v1'),
      minRatio: 4.5,
    ),
    ContrastPair(
      direction: 'A (cũ)',
      mode: 'Light',
      component: 'Hổ phách cũ trên nền trắng',
      fg: const WcagColor('#C8681A', 'Hổ phách v1'),
      bg: const WcagColor('#FFFFFF', 'Nền trắng'),
      minRatio: 4.5,
    ),
    ContrastPair(
      direction: 'A (cũ)',
      mode: 'Light',
      component: 'Chữ mờ cũ trên nền kem',
      fg: const WcagColor('#707C76', 'Chữ mờ v1'),
      bg: const WcagColor('#F8F7F3', 'Nền kem v1'),
      minRatio: 4.5,
    ),
    ContrastPair(
      direction: 'B (cũ)',
      mode: 'Light',
      component: 'San hô cũ trên sương sớm',
      fg: const WcagColor('#F43F5E', 'San hô v1'),
      bg: const WcagColor('#F4F6FB', 'Sương sớm v1'),
      minRatio: 4.5,
    ),
    ContrastPair(
      direction: 'B (cũ)',
      mode: 'Light',
      component: 'Xanh lam cũ trên sương sớm',
      fg: const WcagColor('#0284C7', 'Lam v1'),
      bg: const WcagColor('#F4F6FB', 'Sương sớm v1'),
      minRatio: 4.5,
    ),
    ContrastPair(
      direction: 'Chung (cũ)',
      mode: 'Button',
      component: 'Chữ trắng trên nút hổ phách cũ',
      fg: const WcagColor('#FFFFFF', 'Chữ trắng'),
      bg: const WcagColor('#C8681A', 'Hổ phách v1'),
      minRatio: 4.5,
    ),
  ];

  for (final ContrastPair c in verifyCases) {
    print('${c.component}: ${c.fg.hex} vs ${c.bg.hex} -> ${c.ratio.toStringAsFixed(2)}:1 (Đạt: ${c.passes ? "ĐẠT" : "KHÔNG ĐẠT"})');
  }

  print('\n--- 2. Comprehensive Test for 3 Directions (v2 Optimized Colors) ---');
  // Define refined palettes for A, B, C (all >= 4.5:1 for normal text, >= 3.0:1 for large/UI)
  
  // DIRECTION A - Botanical Premium
  final WcagColor aLightBg = const WcagColor('#F8F7F3', 'Kem mỹ thuật');
  final WcagColor aLightSurface = const WcagColor('#FFFFFF', 'Trắng bề mặt');
  final WcagColor aLightTextPrimary = const WcagColor('#141A17', 'Mực than sẫm');
  final WcagColor aLightTextSecondary = const WcagColor('#3D4843', 'Xám rêu vừa'); // L=0.067 -> ~9.8:1
  final WcagColor aLightTextMuted = const WcagColor('#505C56', 'Xám mờ đạt chuẩn'); // L=0.106 -> ~6.3:1
  final WcagColor aLightForest = const WcagColor('#135E44', 'Xanh thông rừng'); // L=0.093 -> ~7.1:1
  final WcagColor aLightAmberText = const WcagColor('#8A3C01', 'Hổ phách đậm (chữ)'); // L=0.076 -> ~8.5:1
  final WcagColor aLightAmberBtnBg = const WcagColor('#E58E26', 'Cam hổ phách nút'); // dùng với chữ tối
  final WcagColor aLightSuccess = const WcagColor('#15803D', 'Xanh lá chuẩn');
  final WcagColor aLightDanger = const WcagColor('#B91C1C', 'Đỏ gạch chuẩn');
  final WcagColor aLightGoal = const WcagColor('#9333EA', 'Tím hoàng gia (Goal)'); // Tách biệt khỏi info/calendar
  final WcagColor aLightCalendar = const WcagColor('#3730A3', 'Chàm lịch');

  final WcagColor aDarkBg = const WcagColor('#0E1311', 'Đá phiến ngọc');
  final WcagColor aDarkSurface = const WcagColor('#161E1A', 'Nhung sẫm');
  final WcagColor aDarkTextPrimary = const WcagColor('#EDF2EE', 'Trắng ngọc');
  final WcagColor aDarkTextSecondary = const WcagColor('#9BA9A1', 'Xám bạc');
  const WcagColor aDarkTextMuted = WcagColor('#7E8F87', 'Xám mờ');
  const WcagColor aDarkForest = WcagColor('#34D399', 'Ngọc bích');
  const WcagColor aDarkAmber = WcagColor('#FBBF24', 'Vàng hổ phách');
  const WcagColor aDarkGoal = WcagColor('#C084FC', 'Tím sáng (Goal)');

  // DIRECTION B - Midnight Aurora
  const WcagColor bLightBg = WcagColor('#F4F6FB', 'Sương sớm lam');
  const WcagColor bLightSurface = WcagColor('#FFFFFF', 'Trắng tinh thể');
  const WcagColor bLightTextPrimary = WcagColor('#0B132B', 'Xanh đêm sâu');
  const WcagColor bLightTextSecondary = WcagColor('#334155', 'Xám đá lam');
  const WcagColor bLightCyan = WcagColor('#0369A1', 'Lam ngọc đậm'); // đã chỉnh tối hơn để đạt >=4.5:1 trên F4F6FB
  const WcagColor bLightCoral = WcagColor('#BE123C', 'San hô đậm'); // đã chỉnh tối hơn để đạt >=4.5:1
  const WcagColor bLightGoal = WcagColor('#7C3AED', 'Tím cực quang (Goal)');

  const WcagColor bDarkSurface = WcagColor('#131B2E', 'Bề mặt tinh thể');
  const WcagColor bDarkTextPrimary = WcagColor('#F8FAFC', 'Trắng ánh lam');
  const WcagColor bDarkTextSecondary = WcagColor('#94A3B8', 'Xám bạc lam');
  const WcagColor bDarkCyan = WcagColor('#38BDF8', 'Cyan Neon');
  const WcagColor bDarkCoral = WcagColor('#FB7185', 'San hô Neon');
  const WcagColor bDarkGoal = WcagColor('#A78BFA', 'Tím Neon');

  // DIRECTION C - Ivory & Ink
  const WcagColor cLightBg = WcagColor('#FAF9F6', 'Ngà voi');
  const WcagColor cLightSurface = WcagColor('#FFFFFF', 'Trắng tinh khiết');
  const WcagColor cLightTextPrimary = WcagColor('#111111', 'Mực carbon');
  const WcagColor cLightTextSecondary = WcagColor('#444444', 'Xám chì đậm');
  const WcagColor cLightEmerald = WcagColor('#0F5132', 'Ngọc lục bảo Thụy Sĩ');
  const WcagColor cLightGoal = WcagColor('#6B21A8', 'Thạch anh sẫm (Goal)');

  const WcagColor cDarkSurface = WcagColor('#1E1E1E', 'Bề mặt than');
  const WcagColor cDarkTextPrimary = WcagColor('#F5F5F5', 'Trắng mực in');
  const WcagColor cDarkTextSecondary = WcagColor('#A3A3A3', 'Xám bạc');
  const WcagColor cDarkEmerald = WcagColor('#4EAE7C', 'Ngọc lục bảo sáng');
  const WcagColor cDarkGoal = WcagColor('#C084FC', 'Thạch anh sáng');

  final List<ContrastPair> fullPairs = <ContrastPair>[
    // HƯỚNG A - LIGHT
    ContrastPair(direction: 'A. Botanical', mode: 'Light', component: 'Chữ chính trên Nền kem', fg: aLightTextPrimary, bg: aLightBg, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Light', component: 'Chữ chính trên Thẻ trắng', fg: aLightTextPrimary, bg: aLightSurface, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Light', component: 'Chữ phụ trên Thẻ trắng', fg: aLightTextSecondary, bg: aLightSurface, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Light', component: 'Chữ phụ trên Nền kem', fg: aLightTextSecondary, bg: aLightBg, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Light', component: 'Chữ mờ trên Thẻ trắng', fg: aLightTextMuted, bg: aLightSurface, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Light', component: 'Xanh rừng (Task) trên Nền kem', fg: aLightForest, bg: aLightBg, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Light', component: 'Hổ phách chữ (Habit) trên Nền kem', fg: aLightAmberText, bg: aLightBg, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Light', component: 'Nút điểm danh: Chữ tối trên Nền cam', fg: aLightTextPrimary, bg: aLightAmberBtnBg, minRatio: 4.5, note: 'Khắc phục triệt để lỗi chữ trắng'),
    ContrastPair(direction: 'A. Botanical', mode: 'Light', component: 'Nút đã điểm danh: Chữ xanh trên Nền trắng', fg: aLightForest, bg: aLightSurface, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Light', component: 'Màu Mục tiêu (Tím) trên Thẻ trắng', fg: aLightGoal, bg: aLightSurface, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Light', component: 'Màu Lịch (Chàm) trên Thẻ trắng', fg: aLightCalendar, bg: aLightSurface, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Light', component: 'Đỏ chi tiêu trên Thẻ trắng', fg: aLightDanger, bg: aLightSurface, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Light', component: 'Xanh thu nhập trên Thẻ trắng', fg: aLightSuccess, bg: aLightSurface, minRatio: 4.5),

    // HƯỚNG A - DARK
    ContrastPair(direction: 'A. Botanical', mode: 'Dark', component: 'Chữ chính trên Nền tối', fg: aDarkTextPrimary, bg: aDarkBg, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Dark', component: 'Chữ chính trên Thẻ tối', fg: aDarkTextPrimary, bg: aDarkSurface, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Dark', component: 'Chữ phụ trên Thẻ tối', fg: aDarkTextSecondary, bg: aDarkSurface, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Dark', component: 'Chữ mờ trên Thẻ tối', fg: aDarkTextMuted, bg: aDarkSurface, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Dark', component: 'Ngọc bích (Task) trên Thẻ tối', fg: aDarkForest, bg: aDarkSurface, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Dark', component: 'Vàng hổ phách (Habit) trên Thẻ tối', fg: aDarkAmber, bg: aDarkSurface, minRatio: 4.5),
    ContrastPair(direction: 'A. Botanical', mode: 'Dark', component: 'Mục tiêu (Tím sáng) trên Thẻ tối', fg: aDarkGoal, bg: aDarkSurface, minRatio: 4.5),

    // HƯỚNG B - LIGHT
    ContrastPair(direction: 'B. Aurora', mode: 'Light', component: 'Chữ chính trên Nền sương lam', fg: bLightTextPrimary, bg: bLightBg, minRatio: 4.5),
    ContrastPair(direction: 'B. Aurora', mode: 'Light', component: 'Chữ phụ trên Thẻ trắng', fg: bLightTextSecondary, bg: bLightSurface, minRatio: 4.5),
    ContrastPair(direction: 'B. Aurora', mode: 'Light', component: 'Lam ngọc (Cyan) trên Nền sương lam', fg: bLightCyan, bg: bLightBg, minRatio: 4.5),
    ContrastPair(direction: 'B. Aurora', mode: 'Light', component: 'San hô (Coral) trên Nền sương lam', fg: bLightCoral, bg: bLightBg, minRatio: 4.5),
    ContrastPair(direction: 'B. Aurora', mode: 'Light', component: 'Tím cực quang (Goal) trên Thẻ trắng', fg: bLightGoal, bg: bLightSurface, minRatio: 4.5),

    // HƯỚNG B - DARK
    ContrastPair(direction: 'B. Aurora', mode: 'Dark', component: 'Chữ chính trên Thẻ đêm', fg: bDarkTextPrimary, bg: bDarkSurface, minRatio: 4.5),
    ContrastPair(direction: 'B. Aurora', mode: 'Dark', component: 'Chữ phụ trên Thẻ đêm', fg: bDarkTextSecondary, bg: bDarkSurface, minRatio: 4.5),
    ContrastPair(direction: 'B. Aurora', mode: 'Dark', component: 'Cyan Neon trên Thẻ đêm', fg: bDarkCyan, bg: bDarkSurface, minRatio: 4.5),
    ContrastPair(direction: 'B. Aurora', mode: 'Dark', component: 'San hô Neon trên Thẻ đêm', fg: bDarkCoral, bg: bDarkSurface, minRatio: 4.5),
    ContrastPair(direction: 'B. Aurora', mode: 'Dark', component: 'Tím Neon trên Thẻ đêm', fg: bDarkGoal, bg: bDarkSurface, minRatio: 4.5),

    // HƯỚNG C - LIGHT
    ContrastPair(direction: 'C. Ivory & Ink', mode: 'Light', component: 'Mực carbon trên Nền ngà voi', fg: cLightTextPrimary, bg: cLightBg, minRatio: 4.5),
    ContrastPair(direction: 'C. Ivory & Ink', mode: 'Light', component: 'Chữ phụ trên Thẻ trắng', fg: cLightTextSecondary, bg: cLightSurface, minRatio: 4.5),
    ContrastPair(direction: 'C. Ivory & Ink', mode: 'Light', component: 'Ngọc lục bảo Thụy Sĩ trên Nền ngà', fg: cLightEmerald, bg: cLightBg, minRatio: 4.5),
    ContrastPair(direction: 'C. Ivory & Ink', mode: 'Light', component: 'Mục tiêu (Thạch anh) trên Thẻ trắng', fg: cLightGoal, bg: cLightSurface, minRatio: 4.5),

    // HƯỚNG C - DARK
    ContrastPair(direction: 'C. Ivory & Ink', mode: 'Dark', component: 'Chữ chính trên Thẻ than', fg: cDarkTextPrimary, bg: cDarkSurface, minRatio: 4.5),
    ContrastPair(direction: 'C. Ivory & Ink', mode: 'Dark', component: 'Chữ phụ trên Thẻ than', fg: cDarkTextSecondary, bg: cDarkSurface, minRatio: 4.5),
    ContrastPair(direction: 'C. Ivory & Ink', mode: 'Dark', component: 'Ngọc lục bảo sáng trên Thẻ than', fg: cDarkEmerald, bg: cDarkSurface, minRatio: 4.5),
    ContrastPair(direction: 'C. Ivory & Ink', mode: 'Dark', component: 'Mục tiêu sáng trên Thẻ than', fg: cDarkGoal, bg: cDarkSurface, minRatio: 4.5),
  ];

  print('\n| Hướng | Chế độ | Thành phần UI | Màu chữ/thành phần (FG) | Màu nền (BG) | Tỷ lệ (Script) | Ngưỡng WCAG AA | Kết quả |');
  print('|---|---|---|---|---|:---:|:---:|:---:|');
  for (final ContrastPair p in fullPairs) {
    print('| ${p.direction} | ${p.mode} | ${p.component} | `${p.fg.hex}` (${p.fg.name}) | `${p.bg.hex}` (${p.bg.name}) | **${p.ratio.toStringAsFixed(2)}:1** | >= ${p.minRatio.toStringAsFixed(1)}:1 | ${p.passes ? "✅ ĐẠT" : "❌ KHÔNG ĐẠT"} |');
  }
}

