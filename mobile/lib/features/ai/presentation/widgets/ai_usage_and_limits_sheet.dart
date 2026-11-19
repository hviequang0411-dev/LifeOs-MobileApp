import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/l10n/vi_strings.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../../core/widgets/lifeos_widgets.dart';
import '../../data/ai_api.dart';
import '../../data/ai_models.dart';
import '../../data/ai_providers.dart';

/// Màn hình kiểm tra Hạn mức, Mức sử dụng và Quản lý dữ liệu AI trên server (AWP-5).
class AiUsageAndLimitsSheet extends ConsumerStatefulWidget {
  const AiUsageAndLimitsSheet({super.key});

  @override
  ConsumerState<AiUsageAndLimitsSheet> createState() =>
      _AiUsageAndLimitsSheetState();
}

class _AiUsageAndLimitsSheetState extends ConsumerState<AiUsageAndLimitsSheet> {
  AiLimitsResponse? _limits;
  AiUsageResponse? _usage;
  bool _isLoading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    final AiApiClient api = ref.read(aiApiClientProvider);
    final DateTime now = DateTime.now().toUtc();
    final String today =
        '${now.year.toString().padLeft(4, '0')}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';

    try {
      final List<dynamic> results = await Future.wait<dynamic>(<Future<dynamic>>[
        api.getLimits(),
        api.getUsage(from: today, to: today),
      ]);
      if (mounted) {
        setState(() {
          _limits = results[0] as AiLimitsResponse;
          _usage = results[1] as AiUsageResponse;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        final AiErrorDescriptor err = ViStrings.mapAiError(
          e is LifeOsApiException ? e.errorCode : null,
        );
        setState(() {
          _errorMessage = err.message;
          _isLoading = false;
        });
      }
    }
  }

  void _openDeleteAiDataDialog() {
    showDialog<void>(
      context: context,
      builder: (BuildContext ctx) => const _DeleteAiDataDialog(),
    );
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusLg),
      ),
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.sizeOf(context).height * 0.9,
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
                  Icons.pie_chart_outline_rounded,
                  color: tokens.accentPrimary,
                  size: 22,
                ),
                const SizedBox(width: LifeOsTokens.space2),
                Expanded(
                  child: Text(
                    'Mức dùng & Hạn mức AI',
                    style: TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                      color: tokens.textPrimary,
                    ),
                  ),
                ),
                IconButton(
                  key: const Key('btn_close_ai_usage'),
                  icon: const Icon(Icons.close_rounded),
                  onPressed: () => Navigator.of(context).pop(),
                ),
              ],
            ),
          ),
          const Divider(height: 1),

          // Body
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _errorMessage != null
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(LifeOsTokens.space4),
                          child: LifeOsErrorBanner(message: _errorMessage!),
                        ),
                      )
                    : ListView(
                        padding: const EdgeInsets.all(LifeOsTokens.space4),
                        children: <Widget>[
                          // Trạng thái dịch vụ
                          LifeOsCard(
                            elevation: 1,
                            padding: const EdgeInsets.all(LifeOsTokens.space3),
                            child: Row(
                              children: <Widget>[
                                Container(
                                  width: 12,
                                  height: 12,
                                  decoration: BoxDecoration(
                                    shape: BoxShape.circle,
                                    color: (_limits?.enabled ?? false)
                                        ? tokens.accentPrimary
                                        : tokens.danger,
                                  ),
                                ),
                                const SizedBox(width: LifeOsTokens.space2),
                                Expanded(
                                  child: Text(
                                    (_limits?.enabled ?? false)
                                        ? 'Dịch vụ AI đang sẵn sàng'
                                        : 'Dịch vụ AI đang tạm ngưng',
                                    style: TextStyle(
                                      fontSize: 13.5,
                                      fontWeight: FontWeight.w700,
                                      color: tokens.textPrimary,
                                    ),
                                  ),
                                ),
                                Text(
                                  'Tối đa ${_limits?.rateLimitPerMinute ?? 5} y/cầu / phút',
                                  style: TextStyle(
                                    fontSize: 11.5,
                                    color: tokens.textSecondary,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(height: LifeOsTokens.space3),

                          // Cảnh báo gần hạn mức nếu có
                          if ((_limits?.daily.isNearLimit ?? false) ||
                              (_limits?.monthly.isNearLimit ?? false))
                            Container(
                              margin: const EdgeInsets.only(
                                  bottom: LifeOsTokens.space3),
                              padding: const EdgeInsets.all(LifeOsTokens.space3),
                              decoration: BoxDecoration(
                                color: tokens.warning.withValues(alpha: 0.14),
                                borderRadius:
                                    BorderRadius.circular(LifeOsTokens.radiusSm),
                              ),
                              child: Row(
                                children: <Widget>[
                                  Icon(Icons.warning_amber_rounded,
                                      color: tokens.warning, size: 20),
                                  const SizedBox(width: LifeOsTokens.space2),
                                  Expanded(
                                    child: Text(
                                      'Cảnh báo: Bạn đã sử dụng hơn 80% hạn mức trong kỳ hiện tại.',
                                      style: TextStyle(
                                        fontSize: 12.5,
                                        fontWeight: FontWeight.w600,
                                        color: tokens.textPrimary,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),

                          // Hạn mức Hôm nay (Daily)
                          _buildQuotaCard(
                            title: 'Hạn mức Hôm nay (Ngày)',
                            quota: _limits!.daily,
                            tokens: tokens,
                          ),
                          const SizedBox(height: LifeOsTokens.space3),

                          // Hạn mức Tháng (Monthly)
                          _buildQuotaCard(
                            title: 'Hạn mức Tháng này',
                            quota: _limits!.monthly,
                            tokens: tokens,
                          ),
                          const SizedBox(height: LifeOsTokens.space3),

                          // Thống kê chi phí ước tính
                          LifeOsCard(
                            elevation: 1,
                            padding: const EdgeInsets.all(LifeOsTokens.space3),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: <Widget>[
                                Text(
                                  'Ước tính Chi phí & Token hôm nay',
                                  style: TextStyle(
                                    fontSize: 14,
                                    fontWeight: FontWeight.w700,
                                    color: tokens.textPrimary,
                                  ),
                                ),
                                const SizedBox(height: LifeOsTokens.space2),
                                Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceBetween,
                                  children: <Widget>[
                                    Text('Yêu cầu đã thực hiện:',
                                        style: TextStyle(
                                            color: tokens.textSecondary,
                                            fontSize: 13)),
                                    Text('${_usage?.totals.requests ?? 0}',
                                        style: TextStyle(
                                            fontWeight: FontWeight.w700,
                                            color: tokens.textPrimary,
                                            fontSize: 13)),
                                  ],
                                ),
                                const SizedBox(height: 4),
                                Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceBetween,
                                  children: <Widget>[
                                    Text('Input Tokens:',
                                        style: TextStyle(
                                            color: tokens.textSecondary,
                                            fontSize: 13)),
                                    Text('${_usage?.totals.inputTokens ?? 0}',
                                        style: TextStyle(
                                            fontWeight: FontWeight.w600,
                                            color: tokens.textPrimary,
                                            fontSize: 13)),
                                  ],
                                ),
                                const SizedBox(height: 4),
                                Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceBetween,
                                  children: <Widget>[
                                    Text('Output Tokens:',
                                        style: TextStyle(
                                            color: tokens.textSecondary,
                                            fontSize: 13)),
                                    Text('${_usage?.totals.outputTokens ?? 0}',
                                        style: TextStyle(
                                            fontWeight: FontWeight.w600,
                                            color: tokens.textPrimary,
                                            fontSize: 13)),
                                  ],
                                ),
                                const Divider(height: 16),
                                Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceBetween,
                                  children: <Widget>[
                                    Text('Ước tính chi phí (micro-USD):',
                                        style: TextStyle(
                                            color: tokens.textPrimary,
                                            fontWeight: FontWeight.w600,
                                            fontSize: 13.5)),
                                    Text(
                                      formatMicroUsd(
                                        _usage?.totals.estimatedCostMicrousd ??
                                            0,
                                      ),
                                      style: TextStyle(
                                        fontWeight: FontWeight.w800,
                                        color: tokens.accentPrimary,
                                        fontSize: 14,
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(height: LifeOsTokens.space5),

                          // Vùng nguy hiểm: Xóa dữ liệu AI trên server
                          Text(
                            'Quản trị Dữ liệu AI trên Máy chủ',
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w700,
                              color: tokens.danger,
                            ),
                          ),
                          const SizedBox(height: LifeOsTokens.space2),
                          OutlinedButton.icon(
                            key: const Key('btn_delete_ai_data'),
                            style: OutlinedButton.styleFrom(
                              foregroundColor: tokens.danger,
                              side: BorderSide(color: tokens.danger),
                              minimumSize: const Size.fromHeight(
                                  LifeOsTokens.minTouchTarget),
                            ),
                            onPressed: _openDeleteAiDataDialog,
                            icon: const Icon(Icons.delete_sweep_rounded),
                            label: const Text('Xóa toàn bộ dữ liệu AI trên server'),
                          ),
                        ],
                      ),
          ),
        ],
      ),
    ),
  );
}

  Widget _buildQuotaCard({
    required String title,
    required AiQuotaWindow quota,
    required LifeOsTokens tokens,
  }) {
    final double requestProgress = quota.requestLimit > 0
        ? (quota.requestsUsed / quota.requestLimit).clamp(0.0, 1.0)
        : 0.0;

    return LifeOsCard(
      elevation: 1,
      padding: const EdgeInsets.all(LifeOsTokens.space3),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Text(
                title,
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                  color: tokens.textPrimary,
                ),
              ),
              Text(
                'Làm mới: ${quota.resetsAt.length >= 16 ? '${quota.resetsAt.substring(11, 16)} UTC' : quota.resetsAt}',
                style: TextStyle(
                  fontSize: 11,
                  color: tokens.textSecondary,
                ),
              ),
            ],
          ),
          const SizedBox(height: LifeOsTokens.space2),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Text('Số yêu cầu:',
                  style: TextStyle(fontSize: 13, color: tokens.textSecondary)),
              Text(
                '${quota.requestsUsed} / ${quota.requestLimit}',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                  color: quota.isNearLimit ? tokens.warning : tokens.textPrimary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: requestProgress,
              minHeight: 6,
              backgroundColor: tokens.borderSubtle,
              valueColor: AlwaysStoppedAnimation<Color>(
                quota.isNearLimit ? tokens.warning : tokens.accentPrimary,
              ),
            ),
          ),
          const SizedBox(height: LifeOsTokens.space2),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Text('Tokens sử dụng:',
                  style: TextStyle(fontSize: 12.5, color: tokens.textSecondary)),
              Text(
                '${quota.inputTokensUsed + quota.outputTokensUsed} / ${quota.inputTokenLimit + quota.outputTokenLimit}',
                style: TextStyle(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w600,
                  color: tokens.textPrimary,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

/// Hộp thoại xác nhận kiểu nguy hiểm: Bắt buộc nhập chữ "XOA DU LIEU AI" (AWP-5).
class _DeleteAiDataDialog extends ConsumerStatefulWidget {
  const _DeleteAiDataDialog();

  @override
  ConsumerState<_DeleteAiDataDialog> createState() =>
      _DeleteAiDataDialogState();
}

class _DeleteAiDataDialogState extends ConsumerState<_DeleteAiDataDialog> {
  final TextEditingController _textController = TextEditingController();
  bool _isDeleting = false;
  String? _error;

  static const String _kConfirmKeyword = 'XOA DU LIEU AI';

  bool get _isMatch =>
      _textController.text.trim().toUpperCase() == _kConfirmKeyword;

  Future<void> _handleConfirmDelete() async {
    setState(() {
      _isDeleting = true;
      _error = null;
    });

    try {
      final AiApiClient api = ref.read(aiApiClientProvider);
      await api.deleteAllData();
      ref.read(aiPreferencesProvider.notifier).resetToDefault();
      if (mounted) {
        Navigator.of(context).pop(); // pop dialog
        Navigator.of(context).pop(); // pop sheet
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Đã xóa toàn bộ dữ liệu AI trên server và reset quyền riêng tư về mặc định.',
            ),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = e.toString();
          _isDeleting = false;
        });
      }
    }
  }

  @override
  void dispose() {
    _textController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    return AlertDialog(
      title: Row(
        children: <Widget>[
          Icon(Icons.warning_rounded, color: tokens.danger),
          const SizedBox(width: 8),
          const Expanded(child: Text('Xác nhận xóa dữ liệu AI')),
        ],
      ),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Text(
              'Thao tác này sẽ xóa vĩnh viễn toàn bộ các cuộc hội thoại, lịch sử tin nhắn, '
              'nhật ký ngữ cảnh và đặt lại toàn bộ quyền riêng tư (consent) AI về TẮT trên máy chủ.',
              style: TextStyle(fontSize: 13, color: tokens.textPrimary),
            ),
            const SizedBox(height: LifeOsTokens.space3),
            Container(
              padding: const EdgeInsets.all(LifeOsTokens.space3),
              decoration: BoxDecoration(
                color: tokens.accentPrimary.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(LifeOsTokens.radiusSm),
              ),
              child: Text(
                'Lưu ý an toàn: Toàn bộ công việc, thói quen, tài chính, ghi chú và các dữ liệu đồng bộ '
                'khác của bạn trên máy và máy chủ hoàn toàn KHÔNG bị ảnh hưởng.',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: tokens.textPrimary,
                ),
              ),
            ),
            const SizedBox(height: LifeOsTokens.space3),
            Text(
              'Để xác nhận, vui lòng nhập chính xác chữ: $_kConfirmKeyword',
              style: TextStyle(
                fontSize: 12.5,
                fontWeight: FontWeight.w700,
                color: tokens.danger,
              ),
            ),
            const SizedBox(height: LifeOsTokens.space2),
            TextField(
              key: const Key('input_confirm_delete_ai_data'),
              controller: _textController,
              decoration: const InputDecoration(
                hintText: _kConfirmKeyword,
                border: OutlineInputBorder(),
              ),
              onChanged: (_) => setState(() {}),
            ),
            if (_error != null) ...<Widget>[
              const SizedBox(height: LifeOsTokens.space2),
              Text(
                _error!,
                style: TextStyle(fontSize: 12, color: tokens.danger),
              ),
            ],
          ],
        ),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: _isDeleting ? null : () => Navigator.of(context).pop(),
          child: const Text('Hủy'),
        ),
        FilledButton(
          key: const Key('btn_confirm_delete_ai_data'),
          style: FilledButton.styleFrom(
            backgroundColor: tokens.danger,
          ),
          onPressed: (_isMatch && !_isDeleting) ? _handleConfirmDelete : null,
          child: Text(_isDeleting ? 'Đang xóa...' : 'Xóa vĩnh viễn'),
        ),
      ],
    );
  }
}

