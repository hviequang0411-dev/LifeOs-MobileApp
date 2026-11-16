import 'package:flutter/material.dart';

import '../theme/app_tokens.dart';

/// Thẻ giao diện chuẩn Botanical Premium có độ nổi 3 lớp (Layered Depth)
class LifeOsCard extends StatelessWidget {
  const LifeOsCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(LifeOsTokens.space4),
    this.margin = EdgeInsets.zero,
    this.elevation = 1,
    this.onTap,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  final EdgeInsetsGeometry margin;
  final int elevation;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final List<BoxShadow> shadows = elevation == 3
        ? LifeOsTokens.shadowElevation3
        : elevation == 2
            ? LifeOsTokens.shadowElevation2
            : LifeOsTokens.shadowElevation1;

    Widget cardBody = Padding(
      padding: padding,
      child: child,
    );

    if (onTap != null) {
      cardBody = InkWell(
        borderRadius: BorderRadius.circular(LifeOsTokens.radiusLg),
        onTap: onTap,
        child: cardBody,
      );
    }

    return Container(
      margin: margin,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(LifeOsTokens.radiusLg),
        boxShadow: shadows,
      ),
      child: Material(
        color: tokens.bgSurface,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(LifeOsTokens.radiusLg),
          side: BorderSide(color: tokens.borderSubtle),
        ),
        clipBehavior: Clip.antiAlias,
        child: cardBody,
      ),
    );
  }
}

/// Trạng thái đang tải giả lập Shimmer Skeleton (Loading State)
class LifeOsShimmerSkeleton extends StatelessWidget {
  const LifeOsShimmerSkeleton({
    super.key,
    this.width = double.infinity,
    this.height = 48.0,
    this.borderRadius = LifeOsTokens.radiusMd,
  });

  final double width;
  final double height;
  final double borderRadius;

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: tokens.bgElevated,
        borderRadius: BorderRadius.circular(borderRadius),
        border: Border.all(color: tokens.borderSubtle),
      ),
    );
  }
}

/// Trạng thái rỗng chuẩn kèm gợi ý và nút hành động (Empty State)
class LifeOsEmptyState extends StatelessWidget {
  const LifeOsEmptyState({
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
    return Padding(
      padding: const EdgeInsets.all(LifeOsTokens.space8),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: <Widget>[
          Container(
            padding: const EdgeInsets.all(LifeOsTokens.space4),
            decoration: BoxDecoration(
              color: tokens.bgElevated,
              shape: BoxShape.circle,
            ),
            child: Icon(icon, size: 40, color: tokens.textSecondary),
          ),
          const SizedBox(height: LifeOsTokens.space3),
          Text(
            title,
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w700,
              color: tokens.textPrimary,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            subtitle,
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 13,
              color: tokens.textSecondary,
              height: 1.4,
            ),
          ),
          if (actionLabel != null && onAction != null) ...<Widget>[
            const SizedBox(height: LifeOsTokens.space4),
            FilledButton.icon(
              onPressed: onAction,
              style: FilledButton.styleFrom(
                backgroundColor: tokens.domainTask,
                foregroundColor: Theme.of(context).brightness == Brightness.dark
                    ? tokens.bgPrimary
                    : Colors.white,
                minimumSize: const Size(160, LifeOsTokens.minTouchTarget),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                ),
              ),
              icon: const Icon(Icons.add_rounded, size: 18),
              label: Text(actionLabel!),
            ),
          ],
        ],
      ),
    );
  }
}

/// Khối thông báo lỗi kèm nút thử lại (Error State Banner)
class LifeOsErrorBanner extends StatelessWidget {
  const LifeOsErrorBanner({
    super.key,
    required this.message,
    this.onRetry,
  });

  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    return Container(
      margin: const EdgeInsets.all(LifeOsTokens.space4),
      padding: const EdgeInsets.all(LifeOsTokens.space3),
      decoration: BoxDecoration(
        color: tokens.danger.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
        border: Border.all(color: tokens.danger),
      ),
      child: Row(
        children: <Widget>[
          Icon(Icons.error_outline_rounded, color: tokens.danger, size: 20),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              message,
              style: TextStyle(
                fontSize: 12.5,
                fontWeight: FontWeight.w500,
                color: tokens.danger,
              ),
            ),
          ),
          if (onRetry != null)
            TextButton(
              onPressed: onRetry,
              child: const Text('Thử lại'),
            ),
        ],
      ),
    );
  }
}

