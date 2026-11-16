import 'dart:ui';

import 'package:flutter/material.dart';

import '../database/app_database.dart';
import '../l10n/vi_strings.dart';
import '../money/decimal10k.dart';
import '../theme/app_tokens.dart';

export 'lifeos_card_and_states.dart';

/// Chỉ báo trạng thái đồng bộ & cam kết "Đã lưu trên máy" (Zero Data-Loss Anxiety — §5.6).
class SyncStatusPill extends StatelessWidget {
  const SyncStatusPill({
    super.key,
    required this.outboxItems,
    required this.syncPaused,
    this.isOffline = false,
    this.onTap,
  });

  final List<OutboxMutationRecord> outboxItems;
  final bool syncPaused;
  final bool isOffline;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final int conflictCount = outboxItems
        .where((OutboxMutationRecord m) => m.status == 'NEEDS_USER_ACTION')
        .length;
    final int pendingCount = outboxItems
        .where((OutboxMutationRecord m) =>
            m.status == 'PENDING' || m.status == 'IN_FLIGHT')
        .length;

    late final Color dotColor;
    late final IconData icon;
    late final String label;

    if (conflictCount > 0) {
      dotColor = tokens.danger;
      icon = Icons.warning_amber_rounded;
      label = ViStrings.syncConflictCount(conflictCount);
    } else if (syncPaused) {
      dotColor = tokens.warning;
      icon = Icons.pause_circle_outline_rounded;
      label = ViStrings.syncPaused;
    } else if (isOffline || pendingCount > 0) {
      dotColor = tokens.accentSecondary;
      icon = Icons.cloud_upload_outlined;
      label = isOffline
          ? ViStrings.syncOfflinePending(pendingCount)
          : '${ViStrings.savedLocallyBadge} · ${ViStrings.syncPendingCount(pendingCount)}';
    } else {
      dotColor = tokens.success;
      icon = Icons.check_circle_outline_rounded;
      label = ViStrings.syncSynced;
    }

    return Semantics(
      button: true,
      label: 'Trạng thái đồng bộ: $label',
      child: InkWell(
        borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
        onTap: onTap,
        child: ConstrainedBox(
          constraints: const BoxConstraints(
            minHeight: LifeOsTokens.minTouchTarget,
          ),
          child: Container(
            padding: const EdgeInsets.symmetric(
              horizontal: LifeOsTokens.space3,
              vertical: LifeOsTokens.space2,
            ),
            decoration: BoxDecoration(
              color: tokens.bgElevated,
              borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
              border: Border.all(color: tokens.borderSubtle),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: <Widget>[
                Icon(icon, size: 16, color: dotColor),
                const SizedBox(width: 6),
                Flexible(
                  child: Text(
                    label,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: tokens.textPrimary,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Trạng thái rỗng (Empty State) ấm áp với lời mời hành động rõ ràng bằng tiếng Việt (§5.3).
class EmptyStateCard extends StatelessWidget {
  const EmptyStateCard({
    super.key,
    required this.icon,
    required this.title,
    required this.subtitle,
    this.actionLabel,
    this.onAction,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(LifeOsTokens.space6),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: <Widget>[
            Container(
              width: 56,
              height: 56,
              decoration: BoxDecoration(
                color: tokens.accentPrimary.withValues(alpha: 0.12),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, size: 28, color: tokens.accentPrimary),
            ),
            const SizedBox(height: LifeOsTokens.space3),
            Text(
              title,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: LifeOsTokens.space1),
            Text(
              subtitle,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodySmall,
            ),
            if (actionLabel != null && onAction != null) ...<Widget>[
              const SizedBox(height: LifeOsTokens.space4),
              FilledButton.icon(
                onPressed: onAction,
                icon: const Icon(Icons.add_rounded, size: 18),
                label: Text(actionLabel!),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Khung xương (Skeleton Loading) tôn trọng cài đặt `disableAnimations` (Reduce Motion — §5.4).
class SkeletonPlaceholder extends StatelessWidget {
  const SkeletonPlaceholder({
    super.key,
    this.height = 68,
    this.itemCount = 3,
  });

  final double height;
  final int itemCount;

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    return Column(
      children: List<Widget>.generate(itemCount, (int index) {
        return Padding(
          padding: const EdgeInsets.only(bottom: LifeOsTokens.space3),
          child: Container(
            height: height,
            decoration: BoxDecoration(
              color: tokens.bgElevated,
              borderRadius: BorderRadius.circular(LifeOsTokens.radiusLg),
              border: Border.all(color: tokens.borderSubtle),
            ),
          ),
        );
      }),
    );
  }
}

/// Hộp thoại xác nhận thao tác nguy hiểm nêu rõ tên mục và hệ quả (§5.5).
class DestructiveConfirmDialog extends StatelessWidget {
  const DestructiveConfirmDialog({
    super.key,
    required this.title,
    required this.consequenceMessage,
    required this.confirmLabel,
  });

  final String title;
  final String consequenceMessage;
  final String confirmLabel;

  static Future<bool> show(
    BuildContext context, {
    required String title,
    required String consequenceMessage,
    String confirmLabel = ViStrings.deleteAction,
  }) async {
    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => DestructiveConfirmDialog(
        title: title,
        consequenceMessage: consequenceMessage,
        confirmLabel: confirmLabel,
      ),
    );
    return confirmed ?? false;
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    return AlertDialog(
      title: Text(title),
      content: Text(consequenceMessage),
      actions: <Widget>[
        TextButton(
          onPressed: () => Navigator.of(context).pop(false),
          child: const Text(ViStrings.cancelAction),
        ),
        FilledButton(
          style: FilledButton.styleFrom(
            backgroundColor: tokens.danger,
            foregroundColor: Colors.white,
          ),
          onPressed: () => Navigator.of(context).pop(true),
          child: Text(confirmLabel),
        ),
      ],
    );
  }
}

/// Bàn phím số tùy chỉnh (Custom Numeric Keypad) cho nhập liệu Tài chính nhanh 1 tay (§5.5),
/// xuất trực tiếp `Decimal10k` mà không dùng `double`.
class CustomFinanceNumpad extends StatelessWidget {
  const CustomFinanceNumpad({
    super.key,
    required this.rawDigits,
    required this.onChanged,
  });

  final String rawDigits;
  final ValueChanged<String> onChanged;

  void _append(String token) {
    final String next = (rawDigits == '0' ? '' : rawDigits) + token;
    if (next.length <= 12) {
      onChanged(next.isEmpty ? '0' : next);
    }
  }

  void _backspace() {
    if (rawDigits.length <= 1) {
      onChanged('0');
    } else {
      onChanged(rawDigits.substring(0, rawDigits.length - 1));
    }
  }

  @override
  Widget build(BuildContext context) {
    final List<String> keys = <String>[
      '1', '2', '3',
      '4', '5', '6',
      '7', '8', '9',
      '000', '0', 'Xóa',
    ];
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        mainAxisSpacing: 8,
        crossAxisSpacing: 8,
        mainAxisExtent: 48,
      ),
      itemCount: keys.length,
      itemBuilder: (BuildContext context, int index) {
        final String k = keys[index];
        return OutlinedButton(
          onPressed: () {
            if (k == 'Xóa') {
              _backspace();
            } else {
              _append(k);
            }
          },
          child: Text(
            k,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w700,
              fontFeatures: <FontFeature>[FontFeature.tabularFigures()],
            ),
          ),
        );
      },
    );
  }

  static Decimal10k toDecimal10k(String digits) {
    final String clean = digits.replaceAll(RegExp(r'[^0-9]'), '');
    if (clean.isEmpty) return Decimal10k.zero;
    return Decimal10k.parseJsonString(clean);
  }
}

/// Biểu đồ cột nhẹ bằng `CustomPainter` đồng bộ tuyệt đối với Design Tokens (Quyết định #7 §11).
class SimpleBarTrendChart extends StatelessWidget {
  const SimpleBarTrendChart({
    super.key,
    required this.values,
    required this.labels,
    this.height = 100,
  });

  final List<int> values;
  final List<String> labels;
  final double height;

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: <Widget>[
        SizedBox(
          height: height,
          width: double.infinity,
          child: CustomPaint(
            painter: _BarChartPainter(
              values: values,
              barColor: tokens.accentPrimary,
              trackColor: tokens.bgElevated,
            ),
          ),
        ),
        if (labels.isNotEmpty) ...<Widget>[
          const SizedBox(height: LifeOsTokens.space2),
          Row(
            children: List<Widget>.generate(
              labels.length,
              (int i) => Expanded(
                child: Center(
                  child: Text(
                    labels[i],
                    style: TextStyle(
                      fontFamily: 'LifeOsSans',
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: tokens.textSecondary,
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ],
    );
  }
}

class _BarChartPainter extends CustomPainter {
  const _BarChartPainter({
    required this.values,
    required this.barColor,
    required this.trackColor,
  });

  final List<int> values;
  final Color barColor;
  final Color trackColor;

  @override
  void paint(Canvas canvas, Size size) {
    if (values.isEmpty) return;
    int maxVal = 1;
    for (final int v in values) {
      if (v > maxVal) maxVal = v;
    }

    final double slotWidth = size.width / values.length;
    final double barWidth = (slotWidth * 0.56).clamp(8.0, 36.0);
    final Paint trackPaint = Paint()..color = trackColor;
    final Paint barPaint = Paint()..color = barColor;

    for (int i = 0; i < values.length; i++) {
      final double cx = i * slotWidth + slotWidth / 2;
      final RRect trackRect = RRect.fromRectAndRadius(
        Rect.fromCenter(
          center: Offset(cx, size.height / 2),
          width: barWidth,
          height: size.height,
        ),
        const Radius.circular(6),
      );
      canvas.drawRRect(trackRect, trackPaint);

      final double ratio = (values[i] / maxVal).clamp(0.06, 1.0);
      final double barHeight = size.height * ratio;
      final RRect fillRect = RRect.fromRectAndRadius(
        Rect.fromLTWH(
          cx - barWidth / 2,
          size.height - barHeight,
          barWidth,
          barHeight,
        ),
        const Radius.circular(6),
      );
      canvas.drawRRect(fillRect, barPaint);
    }
  }

  @override
  bool shouldRepaint(covariant _BarChartPainter oldDelegate) {
    return oldDelegate.values != values || oldDelegate.barColor != barColor;
  }
}

/// Lớp phủ làm mờ màn hình khi chuyển ứng dụng (App Switcher Blur — §5.9)
/// bảo vệ thông tin tài chính và ghi chú nhạy cảm.
class AppSwitcherPrivacyBlurWrapper extends StatefulWidget {
  const AppSwitcherPrivacyBlurWrapper({
    super.key,
    required this.enabled,
    required this.child,
  });

  final bool enabled;
  final Widget child;

  @override
  State<AppSwitcherPrivacyBlurWrapper> createState() =>
      _AppSwitcherPrivacyBlurWrapperState();
}

class _AppSwitcherPrivacyBlurWrapperState
    extends State<AppSwitcherPrivacyBlurWrapper> with WidgetsBindingObserver {
  bool _obscured = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (!widget.enabled) return;
    final bool shouldBlur = state == AppLifecycleState.inactive ||
        state == AppLifecycleState.paused ||
        state == AppLifecycleState.hidden;
    if (shouldBlur != _obscured && mounted) {
      setState(() => _obscured = shouldBlur);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: <Widget>[
        widget.child,
        if (widget.enabled && _obscured)
          Positioned.fill(
            child: BackdropFilter(
              filter: ImageFilter.blur(sigmaX: 16, sigmaY: 16),
              child: Container(
                color: Colors.black.withValues(alpha: 0.35),
                alignment: Alignment.center,
                child: const Icon(
                  Icons.lock_outline_rounded,
                  size: 48,
                  color: Colors.white,
                ),
              ),
            ),
          ),
      ],
    );
  }
}


