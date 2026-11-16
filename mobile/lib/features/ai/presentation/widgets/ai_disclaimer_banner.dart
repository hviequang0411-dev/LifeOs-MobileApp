import 'package:flutter/material.dart';

import '../../../../core/theme/app_tokens.dart';

/// Dòng thông báo cố định trên toàn bộ màn hình Trợ lý AI (AWP-1).
/// "Nội dung do AI tạo có thể chưa chính xác — hãy kiểm tra trước khi làm theo."
class AiDisclaimerBanner extends StatelessWidget {
  const AiDisclaimerBanner({super.key});

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final bool isDark = Theme.of(context).brightness == Brightness.dark;
    final Color bgColor = isDark
        ? const Color(0xFF1E2822)
        : const Color(0xFFEBF3ED);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(
        horizontal: LifeOsTokens.space3,
        vertical: LifeOsTokens.space2,
      ),
      decoration: BoxDecoration(
        color: bgColor,
        border: Border(
          bottom: BorderSide(
            color: tokens.borderSubtle,
            width: 1.0,
          ),
        ),
      ),
      child: Row(
        children: <Widget>[
          Icon(
            Icons.info_outline_rounded,
            size: 16,
            color: tokens.accentPrimary,
          ),
          const SizedBox(width: LifeOsTokens.space2),
          Expanded(
            child: Text(
              'Nội dung do AI tạo có thể chưa chính xác — hãy kiểm tra trước khi làm theo.',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w500,
                color: tokens.textSecondary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

