import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:lifeos_mobile/core/theme/app_tokens.dart';

double _channelLuminance(int c) {
  final double s = c / 255.0;
  return s <= 0.04045 ? s / 12.92 : math.pow((s + 0.055) / 1.055, 2.4).toDouble();
}

double _colorLuminance(Color color) {
  final double r = _channelLuminance((color.r * 255.0).round().clamp(0, 255));
  final double g = _channelLuminance((color.g * 255.0).round().clamp(0, 255));
  final double b = _channelLuminance((color.b * 255.0).round().clamp(0, 255));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

double _contrastRatio(Color c1, Color c2) {
  final double l1 = _colorLuminance(c1);
  final double l2 = _colorLuminance(c2);
  final double lighter = math.max(l1, l2);
  final double darker = math.min(l1, l2);
  return (lighter + 0.05) / (darker + 0.05);
}

void main() {
  group('WCAG 2.x Contrast Verification — Direction A Botanical Premium (Đọc trực tiếp từ LifeOsTokens)', () {
    const LifeOsTokens light = LifeOsTokens.light;
    const LifeOsTokens dark = LifeOsTokens.dark;

    test('Chế độ Sáng (Light) — Đạt chuẩn WCAG AA >= 4.5:1 cho toàn bộ cặp thực tế', () {
      // Văn bản cơ bản & Nền
      expect(_contrastRatio(light.textPrimary, light.bgPrimary), greaterThanOrEqualTo(4.5), reason: 'Chữ chính trên Nền kem');
      expect(_contrastRatio(light.textPrimary, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Chữ chính trên Thẻ trắng');
      expect(_contrastRatio(light.textSecondary, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Chữ phụ trên Thẻ trắng');
      expect(_contrastRatio(light.textSecondary, light.bgPrimary), greaterThanOrEqualTo(4.5), reason: 'Chữ phụ trên Nền kem');
      expect(_contrastRatio(light.textMuted, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Chữ mờ trên Thẻ trắng');

      // Màu Domain Semantic trên nền thẻ/nền chính
      expect(_contrastRatio(light.domainTask, light.bgPrimary), greaterThanOrEqualTo(4.5), reason: 'Domain Task (Xanh rừng) trên Nền kem');
      expect(_contrastRatio(light.domainHabit, light.bgPrimary), greaterThanOrEqualTo(4.5), reason: 'Domain Habit (Hổ phách đậm) trên Nền kem');
      expect(_contrastRatio(light.domainFinance, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Domain Finance (Lục bảo) trên Thẻ trắng');
      expect(_contrastRatio(light.domainCalendar, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Domain Calendar (Chàm) trên Thẻ trắng');
      expect(_contrastRatio(light.domainKnowledge, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Domain Knowledge (Tím mận) trên Thẻ trắng');
      expect(_contrastRatio(light.domainGoal, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Domain Goal (Tím hoàng gia) trên Thẻ trắng');

      // Các trạng thái nút Thói quen (Habit Button States)
      expect(_contrastRatio(light.domainHabitButtonFg, light.domainHabitButtonBg), greaterThanOrEqualTo(4.5), reason: 'Nút điểm danh: Chữ tối trên nền cam');
      expect(_contrastRatio(light.domainTask, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Nút đã điểm danh: Chữ xanh trên nền trắng');
      expect(_contrastRatio(light.textSecondary, light.bgElevated), greaterThanOrEqualTo(4.5), reason: 'Nút bỏ qua: Chữ xám trên nền xám nhạt');

      // Thu/chi Tài chính
      expect(_contrastRatio(light.financeExpense, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Đỏ chi tiêu trên Thẻ trắng');
      expect(_contrastRatio(light.financeIncome, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Xanh thu nhập trên Thẻ trắng');

      // Chip Độ ưu tiên (Priority Chips)
      expect(_contrastRatio(light.priorityLowFg, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Ưu tiên Thấp trên Thẻ trắng');
      expect(_contrastRatio(light.priorityMediumFg, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Ưu tiên Trung bình trên Thẻ trắng');
      expect(_contrastRatio(light.priorityHighFg, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Ưu tiên Cao trên Thẻ trắng');

      // Chip Rủi ro Mục tiêu (Goal Risk Chips)
      expect(_contrastRatio(light.riskOnTrackFg, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Mục tiêu Đúng tiến độ trên Thẻ trắng');
      expect(_contrastRatio(light.riskAtRiskFg, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Mục tiêu Có nguy cơ trên Thẻ trắng');
      expect(_contrastRatio(light.riskOverdueFg, light.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Mục tiêu Quá hạn trên Thẻ trắng');
    });

    test('Chế độ Tối (Dark) — Đạt chuẩn WCAG AA >= 4.5:1 cho toàn bộ cặp thực tế', () {
      // Văn bản cơ bản & Nền
      expect(_contrastRatio(dark.textPrimary, dark.bgPrimary), greaterThanOrEqualTo(4.5), reason: 'Chữ chính trên Nền tối');
      expect(_contrastRatio(dark.textPrimary, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Chữ chính trên Thẻ tối');
      expect(_contrastRatio(dark.textSecondary, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Chữ phụ trên Thẻ tối');
      expect(_contrastRatio(dark.textMuted, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Chữ mờ trên Thẻ tối');

      // Màu Domain Semantic trên nền thẻ tối
      expect(_contrastRatio(dark.domainTask, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Domain Task (Ngọc bích) trên Thẻ tối');
      expect(_contrastRatio(dark.domainHabit, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Domain Habit (Vàng hổ phách) trên Thẻ tối');
      expect(_contrastRatio(dark.domainFinance, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Domain Finance (Lục bảo sáng) trên Thẻ tối');
      expect(_contrastRatio(dark.domainCalendar, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Domain Calendar (Chàm sáng) trên Thẻ tối');
      expect(_contrastRatio(dark.domainKnowledge, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Domain Knowledge (Tím sáng) trên Thẻ tối');
      expect(_contrastRatio(dark.domainGoal, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Domain Goal (Tím hoàng gia sáng) trên Thẻ tối');

      // Các trạng thái nút Thói quen Dark
      expect(_contrastRatio(dark.domainHabitButtonFg, dark.domainHabitButtonBg), greaterThanOrEqualTo(4.5), reason: 'Nút điểm danh Dark: Chữ tối trên nền vàng');
      expect(_contrastRatio(dark.domainTask, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Nút đã điểm danh Dark: Chữ xanh sáng trên thẻ tối');

      // Thu/chi Tài chính Dark
      expect(_contrastRatio(dark.financeExpense, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Đỏ chi tiêu Dark trên Thẻ tối');
      expect(_contrastRatio(dark.financeIncome, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Xanh thu nhập Dark trên Thẻ tối');

      // Chip Độ ưu tiên Dark
      expect(_contrastRatio(dark.priorityLowFg, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Ưu tiên Thấp Dark trên Thẻ tối');
      expect(_contrastRatio(dark.priorityMediumFg, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Ưu tiên Trung bình Dark trên Thẻ tối');
      expect(_contrastRatio(dark.priorityHighFg, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Ưu tiên Cao Dark trên Thẻ tối');

      // Chip Rủi ro Mục tiêu Dark
      expect(_contrastRatio(dark.riskOnTrackFg, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Mục tiêu Đúng hạn Dark trên Thẻ tối');
      expect(_contrastRatio(dark.riskAtRiskFg, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Mục tiêu Có nguy cơ Dark trên Thẻ tối');
      expect(_contrastRatio(dark.riskOverdueFg, dark.bgSurface), greaterThanOrEqualTo(4.5), reason: 'Mục tiêu Quá hạn Dark trên Thẻ tối');
    });
  });
}
