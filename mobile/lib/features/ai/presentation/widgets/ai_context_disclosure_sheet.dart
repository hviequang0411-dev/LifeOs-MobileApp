import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_tokens.dart';
import '../../../../core/widgets/lifeos_widgets.dart';
import '../../data/ai_api.dart';
import '../../data/ai_models.dart';
import '../../data/ai_providers.dart';

/// Bảng minh bạch ngữ cảnh "AI đã đọc gì?" theo AWP-3.
/// Hiển thị trung thực snapshot payload dữ liệu đã gửi tới nhà cung cấp mô hình sau redaction.
class AiContextDisclosureSheet extends ConsumerWidget {
  const AiContextDisclosureSheet({
    super.key,
    required this.messageId,
  });

  final String messageId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final AiApiClient api = ref.watch(aiApiClientProvider);

    return Container(
      decoration: BoxDecoration(
        color: tokens.bgSurface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(LifeOsTokens.radiusLg),
        ),
      ),
      constraints: BoxConstraints(
        maxHeight: MediaQuery.sizeOf(context).height * 0.85,
      ),
      child: Column(
        children: <Widget>[
          // Header
          Padding(
            padding: const EdgeInsets.fromLTRB(
              LifeOsTokens.space4,
              LifeOsTokens.space3,
              LifeOsTokens.space2,
              LifeOsTokens.space2,
            ),
            child: Row(
              children: <Widget>[
                Icon(
                  Icons.visibility_outlined,
                  color: tokens.accentPrimary,
                  size: 22,
                ),
                const SizedBox(width: LifeOsTokens.space2),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        'AI đã đọc những gì?',
                        style: TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.w700,
                          color: tokens.textPrimary,
                        ),
                      ),
                      Text(
                        'Minh bạch ngữ cảnh dữ liệu cá nhân đã gửi (AWP-3)',
                        style: TextStyle(
                          fontSize: 12,
                          color: tokens.textSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  key: const Key('btn_close_ai_context'),
                  icon: const Icon(Icons.close_rounded),
                  onPressed: () => Navigator.of(context).pop(),
                ),
              ],
            ),
          ),
          const Divider(height: 1),

          // Body
          Expanded(
            child: FutureBuilder<AiContextResponse>(
              future: api.getMessageContext(messageId),
              builder: (BuildContext ctx, AsyncSnapshot<AiContextResponse> snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const Center(
                    child: Padding(
                      padding: EdgeInsets.all(LifeOsTokens.space6),
                      child: CircularProgressIndicator(),
                    ),
                  );
                }

                if (snapshot.hasError) {
                  return Center(
                    child: Padding(
                      padding: const EdgeInsets.all(LifeOsTokens.space4),
                      child: LifeOsErrorBanner(
                        message: 'Không thể tải ngữ cảnh tin nhắn: ${snapshot.error}',
                      ),
                    ),
                  );
                }

                final AiContextResponse? data = snapshot.data;
                if (data == null || data.entities.isEmpty) {
                  return Center(
                    child: Padding(
                      padding: const EdgeInsets.all(LifeOsTokens.space4),
                      child: Text(
                        'Tin nhắn này không đọc thêm dữ liệu cá nhân nào (hoạt động ở chế độ trò chuyện chung không ngữ cảnh).',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 13.5,
                          color: tokens.textSecondary,
                        ),
                      ),
                    ),
                  );
                }

                return ListView(
                  padding: const EdgeInsets.all(LifeOsTokens.space4),
                  children: <Widget>[
                    // Metadata box
                    Container(
                      padding: const EdgeInsets.all(LifeOsTokens.space3),
                      decoration: BoxDecoration(
                        color: tokens.accentPrimary.withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(LifeOsTokens.radiusSm),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: <Widget>[
                          Text(
                            'Thời điểm ghi nhận: ${data.capturedAt}',
                            style: TextStyle(
                              fontSize: 12,
                              color: tokens.textSecondary,
                            ),
                          ),
                          const SizedBox(height: LifeOsTokens.space2),
                          Wrap(
                            spacing: 6,
                            runSpacing: 4,
                            children: data.domains.map((String d) {
                              return Chip(
                                visualDensity: VisualDensity.compact,
                                label: Text(
                                  d,
                                  style: const TextStyle(fontSize: 11),
                                ),
                              );
                            }).toList(),
                          ),
                          if (data.truncated) ...<Widget>[
                            const SizedBox(height: LifeOsTokens.space2),
                            Text(
                              '⚠️ Một số trường dữ liệu đã được cắt giảm theo chính sách tối thiểu hóa dữ liệu.',
                              style: TextStyle(
                                fontSize: 11.5,
                                color: tokens.warning,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                    const SizedBox(height: LifeOsTokens.space3),

                    Text(
                      'Danh sách thực thể đã gửi (${data.entities.length}):',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: tokens.textPrimary,
                      ),
                    ),
                    const SizedBox(height: LifeOsTokens.space2),

                    // Entities
                    ...data.entities.map((AiContextEntity entity) {
                      return Padding(
                        padding: const EdgeInsets.only(bottom: LifeOsTokens.space3),
                        child: LifeOsCard(
                          elevation: 1,
                          padding: const EdgeInsets.all(LifeOsTokens.space3),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: <Widget>[
                              Row(
                                children: <Widget>[
                                  Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 8,
                                      vertical: 3,
                                    ),
                                    decoration: BoxDecoration(
                                      color: tokens.accentPrimary.withValues(alpha: 0.14),
                                      borderRadius: BorderRadius.circular(4),
                                    ),
                                    child: Text(
                                      entity.domain,
                                      style: TextStyle(
                                        fontSize: 11,
                                        fontWeight: FontWeight.w700,
                                        color: tokens.accentPrimary,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  Text(
                                    '${entity.entityType.toUpperCase()} (v${entity.entityVersion ?? 1})',
                                    style: TextStyle(
                                      fontSize: 12.5,
                                      fontWeight: FontWeight.w700,
                                      color: tokens.textPrimary,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: LifeOsTokens.space2),
                              Text(
                                'ID: ${entity.entityId}',
                                style: TextStyle(
                                  fontSize: 11,
                                  fontFamily: 'monospace',
                                  color: tokens.textSecondary,
                                ),
                              ),
                              const Divider(height: 16),
                              // Fields
                              ...entity.fields.entries.map((MapEntry<String, dynamic> entry) {
                                return Padding(
                                  padding: const EdgeInsets.symmetric(vertical: 2),
                                  child: Row(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: <Widget>[
                                      SizedBox(
                                        width: 100,
                                        child: Text(
                                          entry.key,
                                          style: TextStyle(
                                            fontSize: 12,
                                            fontWeight: FontWeight.w600,
                                            color: tokens.textSecondary,
                                          ),
                                        ),
                                      ),
                                      Expanded(
                                        child: Text(
                                          '${entry.value}',
                                          style: TextStyle(
                                            fontSize: 12,
                                            color: tokens.textPrimary,
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                );
                              }),
                            ],
                          ),
                        ),
                      );
                    }),
                  ],
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

